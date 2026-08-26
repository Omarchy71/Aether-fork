package io.github.immaghzbad.aetherst.shared.core

import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.getTrafficProvider
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.shared.platform.Bridge
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

actual object ConnectionController {
    private val _isWaitingForCode = MutableStateFlow(false)
    actual val isWaitingForCode: StateFlow<Boolean> = _isWaitingForCode.asStateFlow()

    private val _status = MutableStateFlow(ConnectionStatus.STOPPED)
    actual val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    actual val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _sessionTraffic = MutableStateFlow(SessionTraffic())
    actual val sessionTraffic: StateFlow<SessionTraffic> = _sessionTraffic.asStateFlow()

    actual fun markStatus(status: ConnectionStatus) {
        _status.value = status
    }

    @Volatile
    private var INSTANCE: ControllerImpl? = null

    actual fun getInstance(context: PlatformContext) {
        if (INSTANCE == null) {
            synchronized(this) {
                if (INSTANCE == null) INSTANCE = ControllerImpl(context)
            }
        }
    }

    fun submitLoginCode(code: String) {
        _isWaitingForCode.value = false
        INSTANCE?.submitLoginCode(code)
    }

    fun getImpl(context: PlatformContext): ControllerImpl {
        getInstance(context)
        return INSTANCE!!
    }

    class ControllerImpl(private val context: PlatformContext) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val runner = AetherProcessRunner(context)
        private val trafficProvider = getTrafficProvider(context)
        private val loginCodeChannel = Channel<String>(Channel.UNLIMITED)

        private var timerJob: Job? = null
        private var baseTx = 0L
        private var baseRx = 0L
        private var prevTx = 0L
        private var prevRx = 0L
        @Volatile private var socksProxy: LocalSocksProxyServer? = null
        @Volatile private var httpProxy: LocalHttpProxyServer? = null
        @Volatile private var tunnelModeStarted = false
        private var routingEngine: RoutingEngine? = null
        private var statusJob: Job? = null
        private var modeJob: Job? = null

        init {
            scope.launch {
                Bridge.statusOverride.collect { s ->
                    if (s != null) {
                        _status.value = s
                        if (s == ConnectionStatus.STOPPED || s == ConnectionStatus.ERROR) {
                            stopTimer()
                        }
                    }
                }
            }
            scope.launch {
                Bridge.trafficOverride.collect {
                    if (it != null) _sessionTraffic.value = it
                }
            }
            scope.launch {
                Bridge.elapsedOverride.collect {
                    if (it != null) _elapsedSeconds.value = it
                }
            }
        }

        fun start() {
            LogRepository.i("Initializing connection process...", "AetherSystem")
            if (statusJob == null) {
                statusJob = scope.launch {
                    runner.connectionStatus.collect { _status.value = it }
                }
            }
            val config = AetherConfigRepository.getInstance(getSettings(context)).config.value.effectiveZeroTrustConfig()
            baseTx = trafficProvider.getTxBytes()
            baseRx = trafficProvider.getRxBytes()

            getSystemUtils(context).clearSystemProxy()

            runner.start(
                config = config,
                bindAddress = "127.0.0.1:${config.socksPort}",
                onCodeRequired = { _isWaitingForCode.value = true },
                inputProvider = { loginCodeChannel.receive() }
            )

            val coreSocksPort = config.socksPort.toIntOrNull() ?: 1819
            routingEngine = RoutingEngine(config.routingRules)
            socksProxy = LocalSocksProxyServer(
                listenHost = "127.0.0.1",
                listenPort = 10808,
                targetHost = "127.0.0.1",
                targetPort = coreSocksPort,
                routingEngine = routingEngine!!
            ).apply { start() }
            httpProxy = LocalHttpProxyServer(
                listenHost = "127.0.0.1",
                listenPort = 10809,
                targetHost = "127.0.0.1",
                targetPort = coreSocksPort,
                routingEngine = routingEngine!!
            ).apply { start() }
            LogRepository.i("[Controller] Counting proxies started (socks=10808, http=10809) -> core $coreSocksPort")

            modeJob?.cancel()
            if (config.connectionMode == ConnectionMode.TUNNEL) {
                modeJob = scope.launch {
                    status.collect { s ->
                        if (s == ConnectionStatus.RUNNING) {
                            delay(500.milliseconds)
                            startTunnelMode(config)
                        }
                    }
                }
            } else if (config.connectionMode == ConnectionMode.SYSTEM_PROXY) {
                modeJob = scope.launch {
                    status.collect { s ->
                        if (s == ConnectionStatus.RUNNING) {
                            delay(500.milliseconds)
                            getSystemUtils(context).setSystemProxy("127.0.0.1", 10809)
                        }
                    }
                }
            }

            startTimer()
        }

        private fun startTunnelMode(config: AetherConfig) {
            if (tunnelModeStarted) return
            tunnelModeStarted = true

            if (socksProxy == null) {
                val coreSocksPort = config.socksPort.toIntOrNull() ?: 1819
                if (routingEngine == null) routingEngine = RoutingEngine(config.routingRules)
                socksProxy = LocalSocksProxyServer(
                    listenHost = "127.0.0.1",
                    listenPort = 10808,
                    targetHost = "127.0.0.1",
                    targetPort = coreSocksPort,
                    routingEngine = routingEngine!!
                ).apply { start() }
            }
            LogRepository.i("[Controller] Local SOCKS bridge listening on 127.0.0.1:10808")

            TunHelper.start(config.socksPort.toIntOrNull() ?: 1819)
            LogRepository.i("[Controller] TUN helper started")
        }

        private val stopLock = Any()

        fun stop() {
            synchronized(stopLock) {
                runner.stop()
                stopTimer()
                modeJob?.cancel()
                modeJob = null
                getSystemUtils(context).clearSystemProxy()
                socksProxy?.stop()
                socksProxy = null
                httpProxy?.stop()
                httpProxy = null
                tunnelModeStarted = false
                TunHelper.stop()
                routingEngine = null
                statusJob?.cancel()
                statusJob = null
            }
        }

        private fun startTimer() {
            timerJob?.cancel()
            _elapsedSeconds.value = 0
            timerJob = scope.launch {
                var seconds = 0L
                while (isActive) {
                    delay(1000.milliseconds)
                    seconds++
                    _elapsedSeconds.value = seconds
                    updateTraffic()
                }
            }
        }

        private fun stopTimer() {
            timerJob?.cancel()
            _elapsedSeconds.value = 0L
            _sessionTraffic.value = SessionTraffic()
        }

        private fun updateTraffic() {
            val socksStats = socksProxy?.getStats()
            val httpStats = httpProxy?.getStats()
            if (socksStats != null || httpStats != null) {
                val totalTx = (socksStats?.txBytes ?: 0L) + (httpStats?.txBytes ?: 0L)
                val totalRx = (socksStats?.rxBytes ?: 0L) + (httpStats?.rxBytes ?: 0L)
                val uploadSpeed = (totalTx - prevTx).toDouble().coerceAtLeast(0.0)
                val downloadSpeed = (totalRx - prevRx).toDouble().coerceAtLeast(0.0)
                prevTx = totalTx
                prevRx = totalRx
                _sessionTraffic.value = SessionTraffic(
                    uploadedBytes = totalTx,
                    downloadedBytes = totalRx,
                    uploadSpeedBps = uploadSpeed,
                    downloadSpeedBps = downloadSpeed
                )
            } else {
                val currentTx = trafficProvider.getTxBytes()
                val currentRx = trafficProvider.getRxBytes()
                val totalTx = currentTx - baseTx
                val totalRx = currentRx - baseRx
                val uploadSpeed = (currentTx - prevTx).toDouble().coerceAtLeast(0.0)
                val downloadSpeed = (currentRx - prevRx).toDouble().coerceAtLeast(0.0)
                prevTx = currentTx
                prevRx = currentRx
                _sessionTraffic.value = SessionTraffic(
                    uploadedBytes = totalTx,
                    downloadedBytes = totalRx,
                    uploadSpeedBps = uploadSpeed,
                    downloadSpeedBps = downloadSpeed
                )
            }
        }

        fun submitLoginCode(code: String) {
            _isWaitingForCode.value = false
            loginCodeChannel.trySend(code)
        }
    }
}
