package io.github.immaghzbad.aetherst.shared.core

import io.github.immaghzbad.aetherst.shared.data.AetherConfigRepository
import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.model.SessionTraffic
import io.github.immaghzbad.aetherst.shared.model.*
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.getSettings
import io.github.immaghzbad.aetherst.platform.getTrafficProvider
import io.github.immaghzbad.aetherst.platform.getSystemUtils
import io.github.immaghzbad.aetherst.shared.platform.Bridge
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

class ConnectionController private constructor(private val context: PlatformContext) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val runner = AetherProcessRunner(context)
    private val trafficProvider = getTrafficProvider(context)
    private val loginCodeChannel = Channel<String>(Channel.UNLIMITED)
    
    private var timerJob: Job? = null
    private var baseTx = 0L
    private var baseRx = 0L
    private var prevTx = 0L
    private var prevRx = 0L

    companion object {
        @Volatile
        private var INSTANCE: ConnectionController? = null

        private val _isWaitingForCode = MutableStateFlow(false)
        val isWaitingForCode: StateFlow<Boolean> = _isWaitingForCode.asStateFlow()

        private val _status = MutableStateFlow(ConnectionStatus.STOPPED)
        val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

        private val _elapsedSeconds = MutableStateFlow(0L)
        val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

        private val _sessionTraffic = MutableStateFlow(SessionTraffic())
        val sessionTraffic: StateFlow<SessionTraffic> = _sessionTraffic.asStateFlow()

        fun getInstance(context: PlatformContext): ConnectionController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConnectionController(context).also { INSTANCE = it }
            }
        }

        fun markStatus(status: ConnectionStatus) {
            _status.value = status
        }
    }

    private var statusJob: Job? = null

    init {
        scope.launch {
            Bridge.statusOverride.collect { status ->
                if (status != null) {
                    _status.value = status
                    if (status == ConnectionStatus.STOPPED || status == ConnectionStatus.ERROR) {
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
        LogRepository.i("Initializing connection process (Common)...", "AetherSystem")
        if (statusJob == null) {
            statusJob = scope.launch {
                runner.connectionStatus.collect { _status.value = it }
            }
        }
        val config = AetherConfigRepository.getInstance(getSettings(context)).config.value
        baseTx = trafficProvider.getTxBytes()
        baseRx = trafficProvider.getRxBytes()
        
        getSystemUtils(context).clearSystemProxy()

        runner.start(
            config = config,
            bindAddress = "127.0.0.1:${config.socksPort}",
            onCodeRequired = { _isWaitingForCode.value = true },
            inputProvider = { loginCodeChannel.receive() }
        )
        
        if (config.connectionMode == ConnectionMode.SYSTEM_PROXY) {
            scope.launch {
                status.collect { s ->
                    if (s == ConnectionStatus.RUNNING) {
                        delay(500.milliseconds)
                        getSystemUtils(context).setSystemProxy("127.0.0.1", config.httpPort.toIntOrNull() ?: 1820)
                    }
                }
            }
        }
        
        startTimer()
    }

    private val stopLock = Any()

    fun stop() {
        synchronized(stopLock) {
            runner.stop()
            stopTimer()
            getSystemUtils(context).clearSystemProxy()
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

    fun submitLoginCode(code: String) {
        _isWaitingForCode.value = false
        loginCodeChannel.trySend(code)
    }
}
