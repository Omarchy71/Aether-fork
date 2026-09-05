package io.github.immaghzbad.aetherst.core

import android.content.Context
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.service.AetherVpnService
import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.model.AutoConnectSettings
import io.github.immaghzbad.aetherst.shared.model.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object AutoConnectManager {

    private const val PREF_PREFIX = "auto_connect_"
    private const val PREF_AUTO_CONNECT_ON_START = "${PREF_PREFIX}on_start"
    private const val PREF_AUTO_CONNECT_ON_BOOT = "${PREF_PREFIX}on_boot"
    private const val PREF_AUTO_CONNECT_ON_NETWORK = "${PREF_PREFIX}on_network"
    private const val PREF_AUTO_RESTART_ON_CRASH = "${PREF_PREFIX}restart_crash"
    private const val PREF_AUTO_CONNECT_AFTER_CRASH = "${PREF_PREFIX}after_crash"
    private const val PREF_MANUAL_DISCONNECT = "${PREF_PREFIX}manual_disconnect"
    private const val PREF_CRASH_COUNT = "${PREF_PREFIX}crash_count"
    private const val PREF_CRASH_WINDOW_START = "${PREF_PREFIX}crash_window_start"

    private const val MAX_CRASH_RETRIES = 3
    private const val CRASH_WINDOW_MS = 60_000L
    private const val NETWORK_DEBOUNCE_MS = 3000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var networkDebounceJob: Job? = null

    fun loadSettings(context: Context): AutoConnectSettings {
        val settings = getSettings(PlatformContext(context))
        return AutoConnectSettings(
            autoConnectOnStart = settings.getBoolean(PREF_AUTO_CONNECT_ON_START, false),
            autoConnectOnBoot = settings.getBoolean(PREF_AUTO_CONNECT_ON_BOOT, false),
            autoConnectOnNetwork = settings.getBoolean(PREF_AUTO_CONNECT_ON_NETWORK, false),
            autoRestartOnCrash = settings.getBoolean(PREF_AUTO_RESTART_ON_CRASH, false),
            autoConnectAfterCrash = settings.getBoolean(PREF_AUTO_CONNECT_AFTER_CRASH, false)
        )
    }

    fun saveSettings(context: Context, s: AutoConnectSettings) {
        val prefs = getSettings(PlatformContext(context))
        prefs.putBoolean(PREF_AUTO_CONNECT_ON_START, s.autoConnectOnStart)
        prefs.putBoolean(PREF_AUTO_CONNECT_ON_BOOT, s.autoConnectOnBoot)
        prefs.putBoolean(PREF_AUTO_CONNECT_ON_NETWORK, s.autoConnectOnNetwork)
        prefs.putBoolean(PREF_AUTO_RESTART_ON_CRASH, s.autoRestartOnCrash)
        prefs.putBoolean(PREF_AUTO_CONNECT_AFTER_CRASH, s.autoConnectAfterCrash)
    }

    fun setManualDisconnect(context: Context, manual: Boolean) {
        val prefs = getSettings(PlatformContext(context))
        prefs.putBoolean(PREF_MANUAL_DISCONNECT, manual)
    }

    fun isManualDisconnect(context: Context): Boolean {
        val prefs = getSettings(PlatformContext(context))
        return prefs.getBoolean(PREF_MANUAL_DISCONNECT, false)
    }

    fun handleAppStart(context: Context) {
        val s = loadSettings(context)
        if (!s.autoConnectOnStart) return
        if (isManualDisconnect(context)) {
            LogRepository.i("[AutoConnect] Skipping: manual disconnect active")
            return
        }
        val status = ConnectionController.status.value
        if (status == ConnectionStatus.RUNNING || status == ConnectionStatus.TUN_ACTIVE) return
        LogRepository.i("[AutoConnect] Auto-connecting on app start")
        AetherVpnService.startVpn(context)
    }

    fun handleBootCompleted(context: Context) {
        val s = loadSettings(context)
        if (!s.autoConnectOnBoot) return
        if (isManualDisconnect(context)) {
            LogRepository.i("[AutoConnect] Skipping boot: manual disconnect active")
            return
        }
        LogRepository.i("[AutoConnect] Auto-connecting on boot")
        AetherVpnService.startVpn(context)
    }

    fun handleNetworkChange(context: Context) {
        val s = loadSettings(context)
        if (!s.autoConnectOnNetwork) return
        if (isManualDisconnect(context)) return
        val status = ConnectionController.status.value
        if (status == ConnectionStatus.RUNNING || status == ConnectionStatus.TUN_ACTIVE) return
        if (status == ConnectionStatus.CONNECTING || status == ConnectionStatus.RECONNECTING) return

        networkDebounceJob?.cancel()
        networkDebounceJob = scope.launch {
            delay(NETWORK_DEBOUNCE_MS)
            val cur = ConnectionController.status.value
            if (cur == ConnectionStatus.RUNNING || cur == ConnectionStatus.TUN_ACTIVE) return@launch
            if (cur == ConnectionStatus.CONNECTING || cur == ConnectionStatus.RECONNECTING) return@launch
            if (isManualDisconnect(context)) return@launch
            LogRepository.i("[AutoConnect] Auto-connecting on network change")
            AetherVpnService.startVpn(context)
        }
    }

    fun shouldRecoverFromCrash(context: Context): Boolean {
        val s = loadSettings(context)
        if (!s.autoRestartOnCrash) return false

        val prefs = getSettings(PlatformContext(context))
        val now = System.currentTimeMillis()
        var crashCount = prefs.getInt(PREF_CRASH_COUNT, 0)
        var windowStart = prefs.getLong(PREF_CRASH_WINDOW_START, 0L)

        if (now - windowStart > CRASH_WINDOW_MS) {
            windowStart = now
            crashCount = 1
        } else {
            crashCount++
        }

        prefs.putInt(PREF_CRASH_COUNT, crashCount)
        prefs.putLong(PREF_CRASH_WINDOW_START, windowStart)

        if (crashCount > MAX_CRASH_RETRIES) {
            LogRepository.e("[AutoConnect] Crash loop detected ($crashCount in window). Disabling auto-restart.")
            prefs.putBoolean(PREF_AUTO_RESTART_ON_CRASH, false)
            prefs.putInt(PREF_CRASH_COUNT, 0)
            return false
        }

        LogRepository.i("[AutoConnect] Crash recovery: attempt $crashCount/$MAX_CRASH_RETRIES")
        return true
    }

    fun shouldAutoConnectAfterCrash(context: Context): Boolean {
        return loadSettings(context).autoConnectAfterCrash
    }

    fun clearCrashCount(context: Context) {
        val prefs = getSettings(PlatformContext(context))
        prefs.putInt(PREF_CRASH_COUNT, 0)
        prefs.putLong(PREF_CRASH_WINDOW_START, 0L)
    }

    fun clearManualDisconnect(context: Context) {
        setManualDisconnect(context, false)
    }
}
