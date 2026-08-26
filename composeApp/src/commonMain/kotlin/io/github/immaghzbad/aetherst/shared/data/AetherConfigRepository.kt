package io.github.immaghzbad.aetherst.shared.data

import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.platform.Settings
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.platform.isWindows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AetherConfigRepository private constructor(private val settings: Settings) {

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<AetherConfig> = _config.asStateFlow()

    private val _isOnboardingComplete = MutableStateFlow(value = settings.getBoolean("onboarding_complete", false))
    val isOnboardingComplete: StateFlow<Boolean> = _isOnboardingComplete.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: AetherConfigRepository? = null

        fun getInstance(settings: Settings): AetherConfigRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AetherConfigRepository(settings).also { INSTANCE = it }
            }
        }
    }

    init {
        LogRepository.currentAppLogLevel = _config.value.appLogLevel
        LogRepository.currentCoreLogLevel = _config.value.coreLogLevel
        PsiphonEgressRegistry.setAvailableRegions(loadCachedEgressRegions())
    }

    private fun loadCachedEgressRegions(): List<String> {
        return settings.getString("psiphon_egress_regions_cache", "")
            .split(",")
            .map { it.trim().uppercase() }
            .filter { it.matches(Regex("^[A-Z]{2}$")) }
            .distinct()
            .sorted()
    }

    fun cacheEgressRegions(regions: List<String>) {
        val normalized = regions.map { it.trim().uppercase() }.filter { it.matches(Regex("^[A-Z]{2}$")) }.distinct().sorted()
        settings.putString("psiphon_egress_regions_cache", normalized.joinToString(","))
        PsiphonEgressRegistry.setAvailableRegions(normalized)
    }

    private fun loadConfig(): AetherConfig {
        migrateCoreLoggingDefault()
        return readFromSettings("")
    }

    private fun migrateCoreLoggingDefault() {
        if (settings.getBoolean("core_logging_default_v2", false)) return
        val current = settings.getString("core_log_level", "")
        val manual = settings.getString("manual_core_log_level", "")
        
        if (current.isEmpty() || (current == AetherLogLevel.OFF.name)) {
            settings.putString("core_log_level", AetherLogLevel.INFO.name)
        }
        if (manual.isEmpty() || (manual == AetherLogLevel.OFF.name)) {
            settings.putString("manual_core_log_level", AetherLogLevel.INFO.name)
        }
        settings.putBoolean("core_logging_default_v2", true)
    }

    private fun loadManualConfig(): AetherConfig {
        return readFromSettings("manual_")
    }

    private fun readFromSettings(prefix: String): AetherConfig {
        val protocolStr = settings.getString("${prefix}protocol", AetherProtocol.MASQUE.name)
        val noiseStr = settings.getString("${prefix}noise", AetherNoise.FIREWALL.name)
        val scanModeStr = settings.getString("${prefix}scan_mode", AetherScanMode.BALANCED.name)
        val ipModeStr = settings.getString("${prefix}ip_mode", AetherIpMode.IPV4.name)
        val appLogLevelStr = settings.getString("${prefix}app_log_level", AetherLogLevel.INFO.name)
        val coreLogLevelStr = settings.getString("${prefix}core_log_level", AetherLogLevel.INFO.name)
        val perfProfileStr = settings.getString("${prefix}perf_profile", AetherPerfProfile.MEDIUM.name)
        val connectionModeStr = settings.getString("${prefix}connection_mode", "")
        val legacyProxyOnly = settings.getBoolean("${prefix}proxy_only", false)
        
        val connectionMode = if (connectionModeStr.isNotEmpty()) {
            runCatching { ConnectionMode.valueOf(connectionModeStr) }.getOrDefault(ConnectionMode.TUNNEL)
        } else {
            if (legacyProxyOnly) ConnectionMode.PROXY_ONLY else if (isWindows) ConnectionMode.SYSTEM_PROXY else ConnectionMode.TUNNEL
        }

        val protocol = runCatching { AetherProtocol.valueOf(protocolStr) }.getOrDefault(AetherProtocol.MASQUE)
        val finalConnectionMode = connectionMode

        val presetId = settings.getString("${prefix}preset_id", "custom")
        val socksHost = settings.getString("${prefix}socks_host", "127.0.0.1")
        val cleanHost = if (socksHost == "198.18.0.1") "127.0.0.1" else socksHost
        
        return AetherConfig(
            presetId = presetId,
            protocol = protocol,
            noise = runCatching { AetherNoise.valueOf(noiseStr) }.getOrDefault(AetherNoise.FIREWALL),
            scanMode = runCatching { AetherScanMode.valueOf(scanModeStr) }.getOrDefault(AetherScanMode.BALANCED),
            ipMode = runCatching { AetherIpMode.valueOf(ipModeStr) }.getOrDefault(AetherIpMode.IPV4),
            echEnabled = settings.getBoolean("${prefix}ech_enabled", false),
            httpProxyEnabled = settings.getBoolean("${prefix}http_proxy_enabled", false),
            perfProfile = runCatching { AetherPerfProfile.valueOf(perfProfileStr) }.getOrDefault(AetherPerfProfile.MEDIUM),
            h2Mode = settings.getBoolean("${prefix}h2_mode", true),
            h2Fragment = settings.getBoolean("${prefix}h2_fragment", false),
            fragmentSize = settings.getString("${prefix}fragment_size", "16-32"),
            fragmentDelay = settings.getString("${prefix}fragment_delay", "2-10"),
            noDataCheck = settings.getBoolean("${prefix}no_data_check", false),
            quickReconnect = settings.getBoolean("${prefix}quick_reconnect", true),
            socksHost = cleanHost,
            socksPort = settings.getString("${prefix}socks_port", "1819"),
            httpPort = settings.getString("${prefix}http_port", "1820"),
            appLogLevel = runCatching { AetherLogLevel.valueOf(appLogLevelStr) }.getOrDefault(AetherLogLevel.INFO),
            coreLogLevel = runCatching { AetherLogLevel.valueOf(coreLogLevelStr) }.getOrDefault(AetherLogLevel.INFO),
            peer = settings.getString("${prefix}peer", ""),
            keepaliveEnabled = settings.getBoolean("${prefix}keepalive_enabled", true),
            keepalive = settings.getInt("${prefix}keepalive", 5),
            validateSecs = settings.getInt("${prefix}validate_secs", 10),
            reconnectSecs = settings.getInt("${prefix}reconnect_secs", 2),
            noProfileRetry = settings.getBoolean("${prefix}no_profile_retry", false),
            tlsGroups = settings.getString("${prefix}tls_groups", ""),
            mtu = settings.getInt("${prefix}mtu", 1100),
            connectionMode = finalConnectionMode,
            routingRules = settings.getString("${prefix}routing_rules", "").let {
                if (it.isEmpty()) emptyList() else runCatching { Json.decodeFromString<List<RoutingRule>>(it) }.getOrDefault(emptyList())
            },
            teamName = settings.getString("${prefix}team_name", ""),
            accessEmail = settings.getString("${prefix}access_email", ""),
            accessId = settings.getString("${prefix}access_id", ""),
            accessSecret = settings.getString("${prefix}access_secret", ""),
            accessToken = settings.getString("${prefix}access_token", ""),
            useGateway = settings.getBoolean("${prefix}use_gateway", false),
            smartReconnect = settings.getBoolean("${prefix}smart_reconnect", true),
            reconnectRetryLimit = settings.getInt("${prefix}reconnect_retry_limit", 10),
            dnsList = settings.getString("${prefix}dns_list", "1.1.1.1,1.0.0.1"),
            shareHotspot = settings.getBoolean("${prefix}share_hotspot", false),
            upstreamProxy = settings.getString("${prefix}upstream_proxy", ""),
            upstreamProxyEnabled = settings.getBoolean("${prefix}upstream_proxy_enabled", false),
            routeSniffing = settings.getBoolean("${prefix}route_sniffing", true),
            sniffingTimeoutMs = settings.getInt("${prefix}sniffing_timeout_ms", 100),
            reprovision = settings.getBoolean("${prefix}reprovision", true),
            hevLogLevel = sanitizeHevLogLevel(settings.getString("${prefix}hev_log_level", "warn")),
            hevConnectTimeoutMs = settings.getInt("${prefix}hev_connect_timeout_ms", 5000),
            hevReadWriteTimeoutMs = settings.getInt("${prefix}hev_read_write_timeout_ms", 60000),
            hevMaxSessionCount = settings.getInt("${prefix}hev_max_session_count", 0),
            hevMapdnsCacheSize = settings.getInt("${prefix}hev_mapdns_cache_size", 10000),
            hevUdpMode = sanitizeHevUdpMode(settings.getString("${prefix}hev_udp_mode", "udp")),
            cloakEnabled = settings.getBoolean("${prefix}cloak_enabled", false),
            cloakSniList = settings.getString("${prefix}cloak_sni_list", "www.hcaptcha.com,www.speedtest.net,www.bing.com"),
            cloakTtlList = settings.getString("${prefix}cloak_ttl_list", "4,5,6,8"),
            cloakJitterMin = settings.getInt("${prefix}cloak_jitter_min", 20),
            cloakJitterMax = settings.getInt("${prefix}cloak_jitter_max", 80),
            cloakFragment = settings.getBoolean("${prefix}cloak_fragment", false),
            cloakAdaptive = settings.getBoolean("${prefix}cloak_adaptive", true),
            cloakFallbackPorts = settings.getString("${prefix}cloak_fallback_ports", "443,2053,2083,2087,2096,8443"),
            cloakLogLevel = sanitizeHevLogLevel(settings.getString("${prefix}cloak_log_level", "info")),
            cloakRandomizeSniCase = settings.getBoolean("${prefix}cloak_randomize_sni_case", false),
            psiphonEnabled = settings.getBoolean("${prefix}psiphon_enabled", false),
            psiphonChainOuter = settings.getString("${prefix}psiphon_chain_outer", "masque"),
            psiphonSocksPort = settings.getString("${prefix}psiphon_socks_port", "3080"),
            psiphonEgressRegion = sanitizePsiphonEgressRegion(settings.getString("${prefix}psiphon_egress_region", "")),
            ztStaySignedIn = settings.getBoolean("${prefix}zt_stay_signed_in", true),
            ztTokenExpiry = settings.getString("${prefix}zt_token_expiry", "0").toLongOrNull() ?: 0,
            connectButtonStyle = sanitizeConnectButtonStyle(settings.getString("${prefix}connect_button_style", "swipe")),
            tunnelAllApps = settings.getBoolean("${prefix}tunnel_all_apps", true),
            excludedPackages = settings.getStringSet("${prefix}excluded_packages", emptySet()),
            blockedPackages = settings.getStringSet("${prefix}blocked_packages", emptySet()),
            tunneledPackages = settings.getStringSet("${prefix}tunneled_packages", emptySet()),
        )
    }

    private fun sanitizeHevLogLevel(value: String): String {
        return if (value in setOf("error", "warn", "info", "debug")) value else "warn"
    }

    private fun sanitizeHevUdpMode(value: String): String {
        val v = value.lowercase().trim()
        return if (v in setOf("udp", "icmp", "off", "false")) v else "udp"
    }

    private fun sanitizePsiphonEgressRegion(value: String): String {
        val v = value.trim().uppercase()
        return if (v.isEmpty() || v.matches(Regex("^[A-Z]{2}$"))) v else ""
    }

    private fun sanitizeConnectButtonStyle(value: String): String {
        val v = value.trim().lowercase()
        return if (v == "capsule" || v == "swipe") v else "swipe"
    }

    fun updateConfig(newConfig: AetherConfig) {
        val oldConfig = _config.value
        val sanitized = newConfig
        val manualConfig = sanitized.copy(presetId = "custom")
        
        val finalConfig = if (oldConfig.protocol != manualConfig.protocol) {
            saveProtocolSettings(oldConfig)
            loadProtocolSettings(manualConfig.protocol, manualConfig)
        } else {
            saveProtocolSettings(manualConfig)
            manualConfig
        }

        saveToSettings("", finalConfig)
        saveToSettings("manual_", finalConfig)
        LogRepository.currentAppLogLevel = finalConfig.appLogLevel
        LogRepository.currentCoreLogLevel = finalConfig.coreLogLevel
        _config.value = finalConfig
    }

    fun applyDetectedConfig(newConfig: AetherConfig) {
        val oldConfig = _config.value
        val manualConfig = newConfig.copy(presetId = "custom")

        if (oldConfig.protocol != manualConfig.protocol) {
            saveProtocolSettings(oldConfig)
        }
        saveProtocolSettings(manualConfig)

        saveToSettings("", manualConfig)
        saveToSettings("manual_", manualConfig)
        LogRepository.currentAppLogLevel = manualConfig.appLogLevel
        LogRepository.currentCoreLogLevel = manualConfig.coreLogLevel
        _config.value = manualConfig
    }

    fun setOnboardingComplete(complete: Boolean) {
        settings.putBoolean("onboarding_complete", complete)
        _isOnboardingComplete.value = complete
    }

    fun getOnboardingStep(): OnboardingStep {
        val name = settings.getString("onboarding_step_name", OnboardingStep.WELCOME.name)
        return try { OnboardingStep.valueOf(name) } catch (_: Exception) { OnboardingStep.WELCOME }
    }

    fun setOnboardingStep(step: OnboardingStep) {
        settings.putString("onboarding_step_name", step.name)
    }

    private fun saveToSettings(prefix: String, cfg: AetherConfig) {
        settings.putString("${prefix}preset_id", cfg.presetId)
        settings.putString("${prefix}protocol", cfg.protocol.name)
        settings.putString("${prefix}noise", cfg.noise.name)
        settings.putString("${prefix}scan_mode", cfg.scanMode.name)
        settings.putString("${prefix}ip_mode", cfg.ipMode.name)
        settings.putBoolean("${prefix}ech_enabled", cfg.echEnabled)
        settings.putBoolean("${prefix}http_proxy_enabled", cfg.httpProxyEnabled)
        settings.putString("${prefix}perf_profile", cfg.perfProfile.name)
        settings.putBoolean("${prefix}h2_mode", cfg.h2Mode)
        settings.putBoolean("${prefix}h2_fragment", cfg.h2Fragment)
        settings.putString("${prefix}fragment_size", cfg.fragmentSize)
        settings.putString("${prefix}fragment_delay", cfg.fragmentDelay)
        settings.putBoolean("${prefix}no_data_check", cfg.noDataCheck)
        settings.putBoolean("${prefix}quick_reconnect", cfg.quickReconnect)
        settings.putString("${prefix}socks_host", cfg.socksHost)
        settings.putString("${prefix}socks_port", cfg.socksPort)
        settings.putString("${prefix}http_port", cfg.httpPort)
        settings.putString("${prefix}app_log_level", cfg.appLogLevel.name)
        settings.putString("${prefix}core_log_level", cfg.coreLogLevel.name)
        settings.putString("${prefix}peer", cfg.peer)
        settings.putBoolean("${prefix}keepalive_enabled", cfg.keepaliveEnabled)
        settings.putInt("${prefix}keepalive", cfg.keepalive)
        settings.putInt("${prefix}validate_secs", cfg.validateSecs)
        settings.putInt("${prefix}reconnect_secs", cfg.reconnectSecs)
        settings.putBoolean("${prefix}no_profile_retry", cfg.noProfileRetry)
        settings.putString("${prefix}tls_groups", cfg.tlsGroups)
        settings.putInt("${prefix}mtu", cfg.mtu)
        settings.putString("${prefix}connection_mode", cfg.connectionMode.name)
        settings.putString("${prefix}routing_rules", Json.encodeToString(cfg.routingRules))
        settings.putString("${prefix}team_name", cfg.teamName)
        settings.putString("${prefix}access_email", cfg.accessEmail)
        settings.putString("${prefix}access_id", cfg.accessId)
        settings.putString("${prefix}access_secret", cfg.accessSecret)
        settings.putString("${prefix}access_token", cfg.accessToken)
        settings.putBoolean("${prefix}use_gateway", cfg.useGateway)
        settings.putBoolean("${prefix}smart_reconnect", cfg.smartReconnect)
        settings.putInt("${prefix}reconnect_retry_limit", cfg.reconnectRetryLimit)
        settings.putString("${prefix}dns_list", cfg.dnsList)
        settings.putBoolean("${prefix}share_hotspot", cfg.shareHotspot)
        settings.putString("${prefix}upstream_proxy", cfg.upstreamProxy)
        settings.putBoolean("${prefix}upstream_proxy_enabled", cfg.upstreamProxyEnabled)
        settings.putBoolean("${prefix}route_sniffing", cfg.routeSniffing)
        settings.putInt("${prefix}sniffing_timeout_ms", cfg.sniffingTimeoutMs)
        settings.putBoolean("${prefix}reprovision", cfg.reprovision)
        settings.putString("${prefix}hev_log_level", sanitizeHevLogLevel(cfg.hevLogLevel))
        settings.putInt("${prefix}hev_connect_timeout_ms", cfg.hevConnectTimeoutMs.coerceIn(500, 120000))
        settings.putInt("${prefix}hev_read_write_timeout_ms", cfg.hevReadWriteTimeoutMs.coerceIn(1000, 600000))
        settings.putInt("${prefix}hev_max_session_count", cfg.hevMaxSessionCount.coerceIn(0, 200000))
        settings.putInt("${prefix}hev_mapdns_cache_size", cfg.hevMapdnsCacheSize.coerceIn(100, 1000000))
        settings.putString("${prefix}hev_udp_mode", sanitizeHevUdpMode(cfg.hevUdpMode))
        settings.putBoolean("${prefix}cloak_enabled", cfg.cloakEnabled)
        settings.putString("${prefix}cloak_sni_list", cfg.cloakSniList)
        settings.putString("${prefix}cloak_ttl_list", cfg.cloakTtlList)
        settings.putInt("${prefix}cloak_jitter_min", cfg.cloakJitterMin.coerceIn(5, 500))
        settings.putInt("${prefix}cloak_jitter_max", cfg.cloakJitterMax.coerceIn(10, 1000))
        settings.putBoolean("${prefix}cloak_fragment", cfg.cloakFragment)
        settings.putBoolean("${prefix}cloak_adaptive", cfg.cloakAdaptive)
        settings.putString("${prefix}cloak_fallback_ports", cfg.cloakFallbackPorts)
        settings.putString("${prefix}cloak_log_level", sanitizeHevLogLevel(cfg.cloakLogLevel))
        settings.putBoolean("${prefix}cloak_randomize_sni_case", cfg.cloakRandomizeSniCase)
        settings.putBoolean("${prefix}psiphon_enabled", cfg.psiphonEnabled)
        settings.putString("${prefix}psiphon_chain_outer", cfg.psiphonChainOuter)
        settings.putString("${prefix}psiphon_socks_port", cfg.psiphonSocksPort)
        settings.putString("${prefix}psiphon_egress_region", sanitizePsiphonEgressRegion(cfg.psiphonEgressRegion))
        settings.putBoolean("${prefix}zt_stay_signed_in", cfg.ztStaySignedIn)
        settings.putString("${prefix}zt_token_expiry", cfg.ztTokenExpiry.toString())
        settings.putString("${prefix}connect_button_style", sanitizeConnectButtonStyle(cfg.connectButtonStyle))
        settings.putBoolean("${prefix}tunnel_all_apps", cfg.tunnelAllApps)
        settings.putStringSet("${prefix}excluded_packages", cfg.excludedPackages)
        settings.putStringSet("${prefix}blocked_packages", cfg.blockedPackages)
        settings.putStringSet("${prefix}tunneled_packages", cfg.tunneledPackages)
    }

    fun resetToDefaults() {
        val defaultConfig = AetherConfig()
        updateConfig(defaultConfig)
        LogRepository.i("System reset: All settings restored to factory defaults")
    }

    fun getFullConfigJson(): String {
        return Json.encodeToString(_config.value)
    }

    fun restoreFullConfig(json: String): Boolean {
        return try {
            val restored = Json.decodeFromString<AetherConfig>(json)
            updateConfig(restored)
            LogRepository.i("Full configuration restored from backup")
            true
        } catch (_: Exception) {
            false
        }
    }

    fun applyPreset(presetId: String) {
        val current = _config.value
        var updated = when (presetId) {
            "custom" -> loadManualConfig()
            "bypass_udp" -> current.copy(
                presetId = "bypass_udp",
                protocol = AetherProtocol.MASQUE,
                noise = AetherNoise.FIREWALL,
                scanMode = AetherScanMode.BALANCED,
                h2Mode = true,
                h2Fragment = true,
                echEnabled = true,
                fragmentSize = "16-32",
                fragmentDelay = "2-10",
                noDataCheck = false,
                tlsGroups = "",
                mtu = 1100,
                connectionMode = ConnectionMode.TUNNEL
            )
            "ironclad_stealth" -> current.copy(
                presetId = "ironclad_stealth",
                protocol = AetherProtocol.MASQUE,
                noise = AetherNoise.GFW,
                scanMode = AetherScanMode.IRONCLAD,
                echEnabled = true,
                h2Mode = false,
                h2Fragment = false,
                noDataCheck = false,
                tlsGroups = "",
                mtu = 1100,
                connectionMode = ConnectionMode.TUNNEL
            )
            "turbo_wg" -> current.copy(
                presetId = "turbo_wg",
                protocol = AetherProtocol.WG,
                noise = AetherNoise.BALANCED,
                scanMode = AetherScanMode.TURBO,
                noDataCheck = true,
                h2Mode = false,
                h2Fragment = false,
                mtu = 1100,
                connectionMode = ConnectionMode.TUNNEL
            )
            else -> current
        }
        LogRepository.i("Configuration profile applied: $presetId")
        saveToSettings("", updated)
        saveProtocolSettings(updated)
        LogRepository.currentAppLogLevel = updated.appLogLevel
        LogRepository.currentCoreLogLevel = updated.coreLogLevel
        _config.value = updated
    }

    private fun saveProtocolSettings(cfg: AetherConfig) {
        val p = "protocol_${cfg.protocol.name}_"
        settings.putString("${p}noise", cfg.noise.name)
        settings.putString("${p}scan_mode", cfg.scanMode.name)
        settings.putString("${p}ip_mode", cfg.ipMode.name)
        settings.putBoolean("${p}ech_enabled", cfg.echEnabled)
        settings.putBoolean("${p}h2_mode", cfg.h2Mode)
        settings.putBoolean("${p}h2_fragment", cfg.h2Fragment)
        settings.putString("${p}fragment_size", cfg.fragmentSize)
        settings.putString("${p}fragment_delay", cfg.fragmentDelay)
        settings.putBoolean("${p}no_data_check", cfg.noDataCheck)
        settings.putBoolean("${p}quick_reconnect", cfg.quickReconnect)
        settings.putString("${p}peer", cfg.peer)
        settings.putBoolean("${p}keepalive_enabled", cfg.keepaliveEnabled)
        settings.putInt("${p}keepalive", cfg.keepalive)
        settings.putInt("${p}validate_secs", cfg.validateSecs)
        settings.putInt("${p}reconnect_secs", cfg.reconnectSecs)
        settings.putBoolean("${p}no_profile_retry", cfg.noProfileRetry)
        settings.putString("${p}tls_groups", cfg.tlsGroups)
        settings.putInt("${p}mtu", cfg.mtu)
        settings.putString("${p}team_name", cfg.teamName)
        settings.putString("${p}access_email", cfg.accessEmail)
        settings.putString("${p}access_id", cfg.accessId)
        settings.putString("${p}access_secret", cfg.accessSecret)
        settings.putString("${p}access_token", cfg.accessToken)
        settings.putBoolean("${p}use_gateway", cfg.useGateway)
        settings.putString("${p}upstream_proxy", cfg.upstreamProxy)
        settings.putBoolean("${p}route_sniffing", cfg.routeSniffing)
        settings.putInt("${p}sniffing_timeout_ms", cfg.sniffingTimeoutMs)
        settings.putBoolean("${p}reprovision", cfg.reprovision)
        settings.putBoolean("${p}initialized", true)
    }

    private fun loadProtocolSettings(protocol: AetherProtocol, base: AetherConfig): AetherConfig {
        val p = "protocol_${protocol.name}_"
        if (!settings.getBoolean("${p}initialized", false)) {
            return when (protocol) {
                AetherProtocol.MASQUE -> base.copy(protocol = protocol, noise = AetherNoise.FIREWALL, scanMode = AetherScanMode.BALANCED)
                AetherProtocol.WG -> base.copy(protocol = protocol, noise = AetherNoise.BALANCED, scanMode = AetherScanMode.TURBO, noDataCheck = true)
                AetherProtocol.GOOL -> base.copy(protocol = protocol, noise = AetherNoise.BALANCED, scanMode = AetherScanMode.BALANCED)
                AetherProtocol.ZERO_TRUST -> base.copy(protocol = protocol, noise = AetherNoise.OFF, scanMode = AetherScanMode.BALANCED)
            }
        }
        return base.copy(
            protocol = protocol,
            noise = runCatching { AetherNoise.valueOf(settings.getString("${p}noise", "")) }.getOrDefault(base.noise),
            scanMode = runCatching { AetherScanMode.valueOf(settings.getString("${p}scan_mode", "")) }.getOrDefault(base.scanMode),
            ipMode = runCatching { AetherIpMode.valueOf(settings.getString("${p}ip_mode", "")) }.getOrDefault(base.ipMode),
            echEnabled = settings.getBoolean("${p}ech_enabled", false),
            h2Mode = settings.getBoolean("${p}h2_mode", true),
            h2Fragment = settings.getBoolean("${p}h2_fragment", false),
            fragmentSize = settings.getString("${p}fragment_size", "16-32"),
            fragmentDelay = settings.getString("${p}fragment_delay", "2-10"),
            noDataCheck = settings.getBoolean("${p}no_data_check", false),
            quickReconnect = settings.getBoolean("${p}quick_reconnect", true),
            peer = settings.getString("${p}peer", ""),
            keepaliveEnabled = settings.getBoolean("${p}keepalive_enabled", true),
            keepalive = settings.getInt("${p}keepalive", 5),
            validateSecs = settings.getInt("${p}validate_secs", 10),
            reconnectSecs = settings.getInt("${p}reconnect_secs", 2),
            noProfileRetry = settings.getBoolean("${p}no_profile_retry", false),
            tlsGroups = settings.getString("${p}tls_groups", ""),
            mtu = settings.getInt("${p}mtu", 1100),
            teamName = settings.getString("${p}team_name", ""),
            accessEmail = settings.getString("${p}access_email", ""),
            accessId = settings.getString("${p}access_id", ""),
            accessSecret = settings.getString("${p}access_secret", ""),
            accessToken = settings.getString("${p}access_token", ""),
            ztStaySignedIn = settings.getBoolean("${p}zt_stay_signed_in", true),
            ztTokenExpiry = settings.getString("${p}zt_token_expiry", "0").toLongOrNull() ?: 0,
            useGateway = settings.getBoolean("${p}use_gateway", false),
            upstreamProxy = settings.getString("${p}upstream_proxy", ""),
            upstreamProxyEnabled = settings.getBoolean("${p}upstream_proxy_enabled", false),
            routeSniffing = settings.getBoolean("${p}route_sniffing", true),
            sniffingTimeoutMs = settings.getInt("${p}sniffing_timeout_ms", 100),
            reprovision = settings.getBoolean("${p}reprovision", true)
        )
    }
}
