package io.github.immaghzbad.aetherst.core

import android.content.Context
import android.net.TrafficStats
import android.os.Process
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.shared.platform.Bridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class ConnectionController private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runner = AetherProcessRunner(appContext)
    private val mutex = Mutex()
    private val activeAttemptId = AtomicLong(0)
    private val loginCodeChannel = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)

    private var timerJob: Job? = null
    private var durationSeconds = 0L
    private var baseTx = 0L
    private var baseRx = 0L
    private var isManualTraffic = false
    private var lastManualTx = 0L
    private var lastManualRx = 0L

    companion object {
        const val ACTION_STATUS_CHANGED = "io.github.immaghzbad.aetherst.STATUS_CHANGED"

        @Volatile
        private var INSTANCE: ConnectionController? = null

        private val _status = MutableStateFlow(ConnectionStatus.STOPPED)
        val status: StateFlow<ConnectionStatus> = _status.asStateFlow()
        
        @Volatile
        var lastKnownStatus: ConnectionStatus = ConnectionStatus.STOPPED
            private set

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _sessionTraffic = MutableStateFlow(SessionTraffic())
        val sessionTraffic: StateFlow<SessionTraffic> = _sessionTraffic.asStateFlow()

        private val _isWaitingForCode = MutableStateFlow(false)
        val isWaitingForCode: StateFlow<Boolean> = _isWaitingForCode.asStateFlow()

        private fun notifyStatusChanged(context: Context, newStatus: ConnectionStatus) {
            lastKnownStatus = newStatus
            _status.value = newStatus
            Bridge.statusOverride.value = newStatus
            val intent = android.content.Intent(ACTION_STATUS_CHANGED)
            intent.putExtra("status", newStatus.name)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }

        fun getInstance(context: Context): ConnectionController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionController(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun updateIsWaitingForCode(waiting: Boolean) {
            _isWaitingForCode.value = waiting
        }
    }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            runner.connectionStatus.collect { coreStatus ->
                handleCoreStatus(coreStatus)
            }
        }
    }


    fun submitLoginCode(code: String) {
        updateIsWaitingForCode(false)
        loginCodeChannel.trySend(code)
    }

    suspend fun start() = mutex.withLock {
        if (_status.value == ConnectionStatus.RUNNING || _status.value == ConnectionStatus.VALIDATING) {
            return@withLock
        }

        val attemptId = System.currentTimeMillis()
        activeAttemptId.set(attemptId)
        notifyStatusChanged(appContext, ConnectionStatus.STARTING)

        try {
            val config = AetherConfigRepository.getInstance(getSettings(PlatformContext(appContext))).config.value
            val bindHost = if (config.shareHotspot) "0.0.0.0" else "127.0.0.1"
            val bindAddress = "$bindHost:${config.socksPort}"

            isManualTraffic = false
            baseTx = TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0)
            baseRx = TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0)

            LogRepository.i("[Controller] Starting core at $bindAddress")
            runner.start(config, bindAddress, onCodeRequired = {
                updateIsWaitingForCode(true)
            }, inputProvider = {
                loginCodeChannel.receive()
            })

            val proxyPort = config.socksPort.toIntOrNull() ?: 1819
            val startupTimeoutSeconds = when (config.scanMode) {
                AetherScanMode.TURBO -> 90L
                AetherScanMode.BALANCED -> 120L
                AetherScanMode.THOROUGH -> 180L
                AetherScanMode.STEALTH -> 240L
                AetherScanMode.IRONCLAD -> 240L
            } + config.validateSecs.coerceAtLeast(0)

            val ready = withTimeoutOrNull(startupTimeoutSeconds.seconds) {
                while (currentCoroutineContext().isActive) {
                    if (runner.connectionStatus.value == ConnectionStatus.RUNNING) return@withTimeoutOrNull true
                    if (isPortListening("127.0.0.1", proxyPort)) return@withTimeoutOrNull true
                    delay(500.milliseconds)
                }
                false
            } ?: false

            if (!ready && runner.connectionStatus.value != ConnectionStatus.RUNNING) {
                LogRepository.w("[Controller] Startup taking longer than expected. Continuing in background...")
            }

            if (!verifyPortListening("127.0.0.1", proxyPort) && runner.connectionStatus.value != ConnectionStatus.RUNNING) {
                LogRepository.w("[Controller] Port $proxyPort not responding yet. Continuing...")
            }

            notifyStatusChanged(appContext, ConnectionStatus.RUNNING)
            
            startTimer()
            LogRepository.i("[Controller] Core is active and validated")
        } catch (e: Exception) {
            if (runner.connectionStatus.value != ConnectionStatus.RUNNING) {
                LogRepository.e("[Controller] Startup initial check: ${e.localizedMessage}")
                cleanup(attemptId)
                notifyStatusChanged(appContext, ConnectionStatus.ERROR)
            }
        }
    }

    suspend fun stop() = mutex.withLock {
        if (_status.value == ConnectionStatus.STOPPED) {
            return@withLock
        }

        val attemptId = activeAttemptId.get()
        notifyStatusChanged(appContext, ConnectionStatus.STOPPING)
        LogRepository.i("[Controller] Stopping core")

        stopTimer()
        cleanup(attemptId)

        notifyStatusChanged(appContext, ConnectionStatus.STOPPED)
        LogRepository.i("[Controller] Core stopped")
    }

    private suspend fun cleanup(attemptId: Long) {
        if (activeAttemptId.get() == attemptId) {
            activeAttemptId.set(0)
        }
        runner.stop()
        updateIsWaitingForCode(false)
        delay(500.milliseconds)
    }

    private fun handleCoreStatus(coreStatus: ConnectionStatus) {
        _status.update { current ->
            if (current == ConnectionStatus.STOPPING && coreStatus != ConnectionStatus.STOPPED) {
                return@update current
            }

            val next = when (coreStatus) {
                ConnectionStatus.ERROR -> {
                    if (current == ConnectionStatus.STARTING || current == ConnectionStatus.VALIDATING) {
                        current
                    } else {
                        LogRepository.e("[Controller] Core reported error")
                        stopTimer()
                        ConnectionStatus.ERROR
                    }
                }
                ConnectionStatus.STOPPED -> {
                    if (current == ConnectionStatus.RUNNING || current == ConnectionStatus.RECONNECTING) {
                        LogRepository.w("[Controller] Core stopped unexpectedly")
                        stopTimer()
                        ConnectionStatus.ERROR
                    } else if (current == ConnectionStatus.STARTING || current == ConnectionStatus.VALIDATING) {
                        current
                    } else if (current == ConnectionStatus.STOPPING) {
                        ConnectionStatus.STOPPED
                    } else {
                        current
                    }
                }
                ConnectionStatus.RECONNECTING -> {
                    if (current != ConnectionStatus.RECONNECTING) {
                        ConnectionStatus.RECONNECTING
                    } else {
                        current
                    }
                }
                ConnectionStatus.RUNNING -> {
                    if (current != ConnectionStatus.RUNNING) {
                        startTimer()
                        ConnectionStatus.RUNNING
                    } else {
                        current
                    }
                }
                else -> current
            }
            
            if (next != current) {
                notifyStatusChanged(appContext, next)
            }
            next
        }
    }

    private suspend fun isPortListening(host: String, port: Int): Boolean {
        return withContext(Dispatchers.IO) {
            runCatching {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), 1000)
                    true
                }
            }.getOrDefault(false)
        }
    }

    private suspend fun verifyPortListening(host: String, port: Int): Boolean {
        val deadline = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < deadline) {
            if (isPortListening(host, port)) return true
            delay(500.milliseconds)
        }
        return false
    }

    private fun startTimer() {
        if (timerJob?.isActive == true) return
        durationSeconds = 0L
        _elapsedSeconds.value = 0L
        timerJob = scope.launch {
            while (isActive) {
                delay(1000.milliseconds)
                durationSeconds++
                _elapsedSeconds.value = durationSeconds
                Bridge.elapsedOverride.value = durationSeconds
                if (!isManualTraffic) {
                    updateTrafficFromStats()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        durationSeconds = 0L
        _elapsedSeconds.value = 0L
        Bridge.elapsedOverride.value = 0L
        _sessionTraffic.value = SessionTraffic()
        Bridge.trafficOverride.value = SessionTraffic()
        isManualTraffic = false
        lastManualTx = 0L
        lastManualRx = 0L
    }

    private fun updateTrafficFromStats() {
        val currentTx = TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0)
        val currentRx = TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0)
        
        val diffTx = (currentTx - baseTx).coerceAtLeast(0)
        val diffRx = (currentRx - baseRx).coerceAtLeast(0)
        
        _sessionTraffic.value = SessionTraffic(diffTx, diffRx)
        Bridge.trafficOverride.value = SessionTraffic(diffTx, diffRx)
    }

    fun setTraffic(tx: Long, rx: Long) {
        if (tx > lastManualTx || rx > lastManualRx || (tx == 0L && rx == 0L && !isManualTraffic)) {
            isManualTraffic = true
            lastManualTx = tx.coerceAtLeast(lastManualTx)
            lastManualRx = rx.coerceAtLeast(lastManualRx)
            val traffic = SessionTraffic(lastManualTx, lastManualRx)
            _sessionTraffic.value = traffic
            Bridge.trafficOverride.value = traffic
        }
    }
}
