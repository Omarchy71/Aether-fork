package io.github.immaghzbad.aetherst.shared.core

import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds

class AetherProcessRunner(private val context: PlatformContext) {
    private val systemUtils = getSystemUtils(context)
    private var process: PlatformProcess? = null
    private var tunProcess: PlatformProcess? = null
    private var runnerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val currentAttemptId = AtomicLong(0)
    private var goolOuterValidated = false
    private var remoteEndpointIp: String? = null
    private var originalGateway: String? = null
    private var lastBindAddress: String = "127.0.0.1:1819"
    private var lastTunIndex: String? = null

    private suspend fun runCommand(vararg command: String): Int = withContext(Dispatchers.IO) {
        LogRepository.d("Executing command: ${command.joinToString(" ")}")
        try {
            val pb = ProcessBuilder(*command)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            val output = StringBuilder()
            proc.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
            }
            val exitCode = proc.waitFor()
            val finalOutput = output.toString().trim()
            if (finalOutput.isNotEmpty()) {
                LogRepository.i("Command '${command[0]}' output:\n$finalOutput")
            }
            if (exitCode != 0) {
                LogRepository.w("Command '${command[0]}' failed with exit code $exitCode")
            }
            exitCode
        } catch (e: Exception) {
            LogRepository.e("Critical error executing ${command[0]}: ${e.message}")
            -1
        }
    }

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.STOPPED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun start(config: AetherConfig, bindAddress: String, onCodeRequired: () -> Unit = {}, inputProvider: suspend () -> String = { "" }) {
        if (runnerJob?.isActive == true) return
        LogRepository.currentAppLogLevel = config.appLogLevel
        LogRepository.currentCoreLogLevel = config.coreLogLevel
        val attemptId = currentAttemptId.incrementAndGet()
        runnerJob = scope.launch {
            var retryCount = 0
            while (isActive && (currentAttemptId.get() == attemptId)) {
                if (config.smartReconnect && retryCount >= config.reconnectRetryLimit) {
                    LogRepository.e("Smart Reconnect limit reached ($retryCount). Stopping...")
                    updateState(ConnectionStatus.ERROR, attemptId)
                    break
                }
                if (retryCount > 0) {
                    val waitTime = (retryCount * 1000L).coerceAtMost(10000L)
                    LogRepository.i("Recovering connection (Retry $retryCount)...")
                    updateState(ConnectionStatus.RECONNECTING, attemptId)
                    delay(waitTime.milliseconds)
                } else {
                    LogRepository.i("Starting system core at $bindAddress")
                    lastBindAddress = bindAddress
                    updateState(ConnectionStatus.STARTING, attemptId)
                }
                try {
                    val success = runBinary(config, attemptId, bindAddress, onCodeRequired, inputProvider)
                    if (currentAttemptId.get() != attemptId) break
                    if (!success) {
                        LogRepository.e("Core execution failed or terminated unexpectedly.")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    LogRepository.e("Execution cycle critical error: ${e.message}")
                }
                retryCount++
                if (connectionStatus.value == ConnectionStatus.RUNNING) {
                    retryCount = 1
                }
            }
        }
    }

    private suspend fun runBinary(config: AetherConfig, attemptId: Long, bindAddress: String, onCodeRequired: () -> Unit, inputProvider: suspend () -> String): Boolean = coroutineScope {
        val proc = PlatformProcess()
        try {
            val binaryPath = getBinaryManager(context).prepareBinary()
            val command = mutableListOf(binaryPath, "--bind", bindAddress)
            if (config.protocol == AetherProtocol.ZERO_TRUST) {
                command.add("--team")
                command.add(config.teamName.ifEmpty { "unnamed" })
            }
            command.add(when (config.ipMode) {
                AetherIpMode.IPV4 -> "-4"
                AetherIpMode.IPV6 -> "-6"
                AetherIpMode.DUAL -> "--dual"
            })
            if (config.h2Mode) command.add("--h2")
            if (config.echEnabled) {
                command.add("--ech")
                command.add("auto")
            }
            if (config.httpProxyEnabled || config.connectionMode == ConnectionMode.SYSTEM_PROXY) {
                val httpBindHost = bindAddress.substringBefore(':')
                command.add("--http-proxy")
                command.add("$httpBindHost:${config.httpPort}")
            }
            if (config.h2Fragment) {
                command.add("--fragment")
                command.add("--fragment-size")
                command.add(config.fragmentSize)
                command.add("--fragment-delay")
                command.add(config.fragmentDelay)
            }
            if (config.noDataCheck) command.add("--no-data-check")
            if (config.quickReconnect) command.add("--quick-reconnect") else command.add("--no-quick-reconnect")
            if (config.peer.isNotEmpty()) {
                command.add("--peer")
                command.add(config.peer)
            }
            if (config.protocol == AetherProtocol.WG || config.protocol == AetherProtocol.GOOL) {
                command.add("--keepalive")
                command.add(config.keepalive.toString())
            }
            if (config.tlsGroups.isNotEmpty()) {
                command.add("--tls-groups")
                command.add(config.tlsGroups)
            }
            command.add("--validate-secs")
            command.add(config.validateSecs.toString())
            command.add("--reconnect-secs")
            command.add(config.reconnectSecs.toString())
            if (config.noProfileRetry) command.add("--no-profile-retry")
            if (config.accessEmail.isNotEmpty()) {
                command.add("--access-email")
                command.add(config.accessEmail)
            }
            if (config.accessId.isNotEmpty()) {
                command.add("--access-id")
                command.add(config.accessId)
            }
            if (config.accessSecret.isNotEmpty()) {
                command.add("--access-secret")
                command.add(config.accessSecret)
            }
            if (config.accessToken.isNotEmpty()) {
                command.add("--access-token")
                command.add(config.accessToken)
            }

            val directRules = config.routingRules.filter { it.mode == RoutingMode.DIRECT }.map { formatRoutingPattern(it.pattern) }.joinToString(",")
            val blockRules = config.routingRules.filter { it.mode == RoutingMode.BLOCK }.map { formatRoutingPattern(it.pattern) }.joinToString(",")
            if (directRules.isNotEmpty()) {
                command.add("--route-direct")
                command.add(directRules)
            }
            if (blockRules.isNotEmpty()) {
                command.add("--route-block")
                command.add(blockRules)
            }

            if (config.useGateway) command.add("--gateway")
            if (config.dnsList.isNotEmpty()) {
                command.add("--dns")
                command.add(config.dnsList)
            }
            if (config.upstreamProxy.isNotEmpty()) {
                command.add("--upstream")
                command.add(config.upstreamProxy)
            }
            val env = mutableMapOf<String, String>()
            env["AETHER_PROTOCOL"] = config.protocol.rawValue
            env["AETHER_NOIZE"] = config.noise.rawValue
            env["AETHER_SCAN"] = config.scanMode.rawValue
            env["AETHER_IP"] = config.ipMode.rawValue
            env["AETHER_SOCKS"] = bindAddress
            env["AETHER_LOG_LEVEL"] = config.coreLogLevel.rawValue
            env["AETHER_PERF_PROFILE"] = config.perfProfile.rawValue
            if (config.h2Mode) env["AETHER_MASQUE_HTTP2"] = "1"
            if (config.echEnabled) env["AETHER_ECH"] = "auto"
            if (config.httpProxyEnabled) {
                val httpBindHost = bindAddress.substringBefore(':')
                env["AETHER_HTTP_PROXY"] = "$httpBindHost:${config.httpPort}"
            }
            if (config.h2Fragment) {
                env["AETHER_MASQUE_H2_FRAGMENT"] = "1"
                env["AETHER_MASQUE_H2_FRAGMENT_SIZE"] = config.fragmentSize
                env["AETHER_MASQUE_H2_FRAGMENT_DELAY"] = config.fragmentDelay
            }
            if (config.noDataCheck) {
                env["AETHER_MASQUE_NO_DATA_CHECK"] = "1"
                env["AETHER_WG_NO_DATA_CHECK"] = "1"
            }
            if (config.quickReconnect) env["AETHER_QUICK_RECONNECT"] = "1" else env["AETHER_QUICK_RECONNECT"] = "0"
            if (config.peer.isNotEmpty()) {
                env["AETHER_PEER"] = config.peer
                env["AETHER_WG_PEER"] = config.peer
            }
            env["AETHER_WG_KEEPALIVE"] = config.keepalive.toString()
            env["AETHER_MASQUE_VALIDATE_SECS"] = config.validateSecs.toString()
            env["AETHER_MASQUE_RECONNECT_SECS"] = config.reconnectSecs.toString()
            env["AETHER_WG_RECONNECT_SECS"] = config.reconnectSecs.toString()
            if (config.noProfileRetry) env["AETHER_WG_NO_PROFILE_RETRY"] = "1"
            if (config.tlsGroups.isNotEmpty()) env["AETHER_TLS_GROUPS"] = config.tlsGroups
            if (config.accessEmail.isNotEmpty()) env["AETHER_ACCESS_EMAIL"] = config.accessEmail
            if (config.accessId.isNotEmpty()) env["AETHER_ACCESS_ID"] = config.accessId
            if (config.accessSecret.isNotEmpty()) env["AETHER_ACCESS_SECRET"] = config.accessSecret
            if (config.accessToken.isNotEmpty()) env["AETHER_ACCESS_TOKEN"] = config.accessToken
            if (config.useGateway) env["AETHER_GATEWAY"] = "1"
            
            val directRulesEnv = config.routingRules.filter { it.mode == RoutingMode.DIRECT }.map { formatRoutingPattern(it.pattern) }.joinToString(",")
            val blockRulesEnv = config.routingRules.filter { it.mode == RoutingMode.BLOCK }.map { formatRoutingPattern(it.pattern) }.joinToString(",")
            if (directRulesEnv.isNotEmpty()) env["AETHER_ROUTE_DIRECT"] = directRulesEnv
            if (blockRulesEnv.isNotEmpty()) env["AETHER_ROUTE_BLOCK"] = blockRulesEnv

            if (config.dnsList.isNotEmpty()) env["AETHER_DNS"] = config.dnsList
            if (config.upstreamProxy.isNotEmpty()) env["AETHER_UPSTREAM"] = config.upstreamProxy
            env["AETHER_ROUTE_SNIFF"] = if (config.routeSniffing) "1" else "0"
            env["AETHER_ROUTE_SNIFF_MS"] = config.sniffingTimeoutMs.toString()
            env["AETHER_REPROVISION"] = if (config.reprovision) "1" else "0"
            LogRepository.d("Executing: ${command.joinToString(" ")}")
            if (!proc.start(command, systemUtils.getFilesDir(), env)) {
                LogRepository.e("Failed to start process: $binaryPath. Check if file exists and is executable.")
                return@coroutineScope false
            }
            process = proc
            val inputJob = launch {
                while (isActive) {
                    val text = inputProvider()
                    if (text.isNotEmpty()) {
                        proc.writeLine(text)
                        LogRepository.d("Sent input to binary")
                    }
                }
            }
            while (isActive && currentAttemptId.get() == attemptId) {
                val line = proc.readLine() ?: break
                parseOutputLine(line, attemptId, config.protocol, onCodeRequired)
            }
            inputJob.cancel()
            val exitCode = proc.waitFor()
            LogRepository.i("Core process terminated (Exit code: $exitCode)")
            exitCode == 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (currentAttemptId.get() == attemptId) {
                LogRepository.e("Binary execution runtime error: ${e.message}")
            }
            false
        } finally {
            proc.destroy()
            tunProcess?.destroy()
            tunProcess = null
        }
    }

    private fun startTunWindows(config: AetherConfig, bindAddress: String) {
        if (tunProcess != null) return
        scope.launch(Dispatchers.IO) {
            try {
                LogRepository.i("Starting Advanced Windows TUN Diagnostics...")
                if (!config.tunnelAllApps) {
                    LogRepository.w("Warning: 'Tunnel Whole Device' is OFF. Per-app tunneling is not yet supported on Windows. Force-tunneling entire system.")
                }
                val tunBinaryPath = getBinaryManager(context).prepareBinary("tun2socks")
                val binFile = java.io.File(tunBinaryPath)
                val workingDir = binFile.parentFile?.absolutePath ?: systemUtils.getFilesDir()
                val wintunDll = java.io.File(workingDir, "wintun.dll")
                LogRepository.i("Binary Path: $tunBinaryPath (Exists: ${binFile.exists()})")
                LogRepository.i("Wintun DLL Path: ${wintunDll.absolutePath} (Exists: ${wintunDll.exists()})")
                val configFile = java.io.File(workingDir, "tun-config.yaml")
                val configContent = """
                    tunnel:
                      name: AetherTun
                      mtu: ${config.mtu}
                      ipv4: 198.18.0.2
                      gateway: $tunGateway
                      mask: 255.255.255.0
                    socks:
                      address: 127.0.0.1
                      port: ${bindAddress.substringAfter(':')}
                      udp: udp
                """.trimIndent()
                configFile.writeText(configContent)
                LogRepository.i("Generated TUN Config:\n$configContent")
                runCommand("powershell", "-Command", "Get-NetIPInterface -AddressFamily IPv4 | Select-Object InterfaceAlias, InterfaceMetric, ConnectionState | Format-Table | Out-String")
                val tunProc = PlatformProcess()
                val command = listOf(tunBinaryPath, configFile.absolutePath)
                if (tunProc.start(command, workingDir, emptyMap())) {
                    tunProcess = tunProc
                    launch {
                        while (isActive) {
                            val line = tunProc.readLine() ?: break
                            LogRepository.i(line, "TunEngine")
                        }
                    }
                    LogRepository.i("Windows TUN engine launched. Waiting for adapter initialization...")
                    delay(5000.milliseconds) 
                    val ifIndex = try {
                        val pb = ProcessBuilder("powershell", "-Command", "(Get-NetAdapter | Where-Object { \$_.Name -like 'AetherTun*' } | Sort-Object -Property InterfaceIndex -Descending | Select-Object -First 1).ifIndex")
                        pb.start().inputStream.bufferedReader().readText().trim()
                    } catch(e: Exception) { 
                        LogRepository.e("Failed to detect AetherTun ifIndex: ${e.message}")
                        null 
                    }
                    LogRepository.i("Detected AetherTun ifIndex: ${ifIndex ?: "Unknown"}")
                    lastTunIndex = ifIndex
                    val gateway = try {
                        val pb = ProcessBuilder("powershell", "-Command", "(Get-NetRoute -DestinationPrefix '0.0.0.0/0' | Sort-Object RouteMetric | Select-Object -First 1).NextHop")
                        pb.start().inputStream.bufferedReader().readText().trim()
                    } catch(e: Exception) { 
                        LogRepository.e("Failed to detect original gateway: ${e.message}")
                        null 
                    }
                    originalGateway = gateway
                    LogRepository.i("Detected original gateway: ${gateway ?: "Unknown"}")
                    val remoteIp = remoteEndpointIp
                    LogRepository.i("Remote endpoint IP: ${remoteIp ?: "Unknown"}")
                    if (!remoteIp.isNullOrEmpty() && !gateway.isNullOrEmpty()) {
                        runCommand("powershell", "-Command", "if (-not (Get-NetRoute -DestinationPrefix '$remoteIp/32' -ErrorAction SilentlyContinue)) { route add $remoteIp $gateway metric 1 }")
                    }
                    val dns = config.dnsList.split(",").firstOrNull()?.trim() ?: "1.1.1.1"
                    LogRepository.i("Configuring network interface AetherTun...")
                    if (!ifIndex.isNullOrEmpty()) {
                        runCommand("powershell", "-Command", "Enable-NetAdapter -Name (Get-NetAdapter -InterfaceIndex $ifIndex).Name -Confirm:\$false")
                        runCommand("powershell", "-Command", "Get-NetIPAddress -InterfaceIndex $ifIndex -AddressFamily IPv4 -ErrorAction SilentlyContinue | Remove-NetIPAddress -Confirm:\$false")
                        runCommand("powershell", "-Command", "New-NetIPAddress -InterfaceIndex $ifIndex -IPAddress 198.18.0.2 -PrefixLength 24 -DefaultGateway $tunGateway -Confirm:\$false")
                        runCommand("powershell", "-Command", "Set-NetIPInterface -InterfaceIndex $ifIndex -InterfaceMetric 1 -DadTransmits 0 -Confirm:\$false")
                        runCommand("powershell", "-Command", "Get-NetIPInterface | Where-Object { \$_.InterfaceAlias -notlike 'AetherTun*' -and \$_.InterfaceAlias -ne 'Loopback Pseudo-Interface 1' } | Set-NetIPInterface -InterfaceMetric 500 -Confirm:\$false")
                        runCommand("powershell", "-Command", "Disable-NetAdapterBinding -Name (Get-NetAdapter -InterfaceIndex $ifIndex).Name -ComponentID ms_tcpip6 -Confirm:\$false")
                        runCommand("powershell", "-Command", "Set-DnsClientServerAddress -InterfaceIndex $ifIndex -ServerAddresses $dns")
                    } else {
                        runCommand("netsh", "interface", "ip", "set address AetherTun static 198.18.0.2 255.255.255.0 $tunGateway")
                        runCommand("netsh", "interface", "ip", "set dnsserver AetherTun static $dns validate=no")
                    }
                    LogRepository.i("Applying global routing...")
                    val gw = tunGateway
                    if (!ifIndex.isNullOrEmpty()) {
                        runCommand("powershell", "-Command", "if (-not (Get-NetRoute -DestinationPrefix '0.0.0.0/1' -ErrorAction SilentlyContinue)) { route add 0.0.0.0 mask 128.0.0.0 $gw metric 1 if $ifIndex }")
                        runCommand("powershell", "-Command", "if (-not (Get-NetRoute -DestinationPrefix '128.0.0.0/1' -ErrorAction SilentlyContinue)) { route add 128.0.0.0 mask 128.0.0.0 $gw metric 1 if $ifIndex }")
                    } else {
                        runCommand("route", "add", "0.0.0.0", "mask", "128.0.0.0", gw, "metric", "1")
                        runCommand("route", "add", "128.0.0.0", "mask", "128.0.0.0", gw, "metric", "1")
                    }
                    LogRepository.i("Windows TUN routing configured successfully.")
                    runCommand("powershell", "-Command", "Get-NetIPAddress -InterfaceIndex $ifIndex -AddressFamily IPv4 | Select-Object IPAddress, AddressState | Format-Table | Out-String")
                } else {
                    LogRepository.e("Failed to launch tun2socks binary.")
                }
            } catch (e: Exception) {
                LogRepository.e("Error in TUN lifecycle: ${e.message}")
            }
        }
    }

    private fun parseOutputLine(line: String, attemptId: Long, protocol: AetherProtocol, onCodeRequired: () -> Unit) {
        if (currentAttemptId.get() != attemptId) return
        val lower = line.lowercase()
        if (lower.contains("using cloudflare edge") || lower.contains("connecting tcp to")) {
            val match = Regex("""(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})""").find(line)
            if (match != null) remoteEndpointIp = match.value
        }
        when {
            lower.contains(" error ") || lower.contains("[error]") -> LogRepository.e(line, "AetherCore")
            lower.contains(" warn ") || lower.contains("[warn]") -> LogRepository.w(line, "AetherCore")
            else -> LogRepository.i(line, "AetherCore")
        }
        if (lower.contains("enter code:") || lower.contains("login code required")) {
            onCodeRequired()
            return
        }
        when {
            lower.contains("scanning") -> {
                goolOuterValidated = false
                updateState(ConnectionStatus.STARTING, attemptId)
            }
            lower.contains("validating") -> updateState(ConnectionStatus.VALIDATING, attemptId)
            (protocol == AetherProtocol.MASQUE && (lower.contains("tunnel validated") || lower.contains("connect-ip status: 200"))) ||
            (protocol == AetherProtocol.WG && (lower.contains("wireguard tunnel validated") || lower.contains("handshake complete"))) -> {
                updateState(ConnectionStatus.RUNNING, attemptId)
                val isWindows = try { System.getProperty("os.name").lowercase().contains("win") } catch(_: Throwable) { false }
                val config = io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository.getInstance(io.github.immaghzbad.aetherst.platform.getSettings(context)).config.value
                if (isWindows && config.connectionMode == ConnectionMode.TUNNEL && tunProcess == null) {
                    startTunWindows(config, lastBindAddress)
                }
            }
            protocol == AetherProtocol.GOOL -> {
                if (lower.contains("outer") && lower.contains("tunnel validated")) goolOuterValidated = true
                if (lower.contains("inner") && lower.contains("tunnel validated") && goolOuterValidated) {
                    updateState(ConnectionStatus.RUNNING, attemptId)
                }
            }
            lower.contains("reconnecting") || lower.contains("tunnel lost") || lower.contains("handshake timeout") -> {
                goolOuterValidated = false
                updateState(ConnectionStatus.RECONNECTING, attemptId)
            }
        }
    }

    private fun updateState(state: ConnectionStatus, attemptId: Long) {
        if (currentAttemptId.get() == attemptId) _connectionStatus.value = state
    }

    private fun formatRoutingPattern(pattern: String): String {
        val trimmed = pattern.trim()
        if (trimmed.startsWith("domain:") || trimmed.startsWith("ip:") || 
            trimmed.startsWith("keyword:") || trimmed.startsWith("regexp:") ||
            trimmed == "private") {
            return trimmed
        }

        val isIp = trimmed.all { it.isDigit() || it == '.' || it == ':' || it == '/' || (it.lowercaseChar() in 'a'..'f') } &&
                (trimmed.contains('.') || trimmed.contains(':'))
        
        return if (isIp) "ip:$trimmed" else "domain:$trimmed"
    }

    private val tunGateway = "198.18.0.1"

    fun stop() {
        currentAttemptId.incrementAndGet()
        _connectionStatus.value = ConnectionStatus.STOPPED
        val isWindows = try { System.getProperty("os.name").lowercase().contains("win") } catch(_: Throwable) { false }
        if (isWindows && tunProcess != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    originalGateway?.let { gw ->
                        remoteEndpointIp?.let { ip ->
                            runCommand("route", "delete", ip, gw)
                        }
                    }
                    runCommand("route", "delete", "0.0.0.0", "mask", "128.0.0.0", tunGateway)
                    runCommand("route", "delete", "128.0.0.0", "mask", "128.0.0.0", tunGateway)
                    runCommand("powershell", "-Command", "Get-NetIPInterface | Where-Object { \$_.InterfaceAlias -notlike 'AetherTun*' -and \$_.InterfaceAlias -ne 'Loopback Pseudo-Interface 1' } | Set-NetIPInterface -InterfaceMetric 25 -Confirm:\$false")
                    if (!lastTunIndex.isNullOrEmpty()) {
                        runCommand("powershell", "-Command", "Set-DnsClientServerAddress -InterfaceIndex $lastTunIndex -ResetServerAddresses")
                    } else {
                        runCommand("netsh", "interface", "ip", "set dnsserver \"AetherTun\" source=dhcp")
                    }
                } catch (e: Exception) {
                    LogRepository.e("Error during TUN cleanup: ${e.message}")
                }
            }
        }
        remoteEndpointIp = null
        originalGateway = null
        lastTunIndex = null
        runnerJob?.cancel()
        process?.destroy()
        tunProcess?.destroy()
        tunProcess = null
        LogRepository.i("System core shutdown initiated.")
    }
}
