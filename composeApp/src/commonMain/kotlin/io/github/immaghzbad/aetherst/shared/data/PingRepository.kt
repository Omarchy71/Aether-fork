package io.github.immaghzbad.aetherst.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import kotlin.time.Duration.Companion.milliseconds

object PingRepository {
    private val _pingState = MutableStateFlow(PingState())
    val pingState: StateFlow<PingState> = _pingState.asStateFlow()
    
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun runPing(socksHost: String = "127.0.0.1", socksPort: Int = 1819, useProxy: Boolean = true) {
        if (!mutex.tryLock()) return
        
        try {
            _pingState.value = _pingState.value.copy(isPinging = true, error = null)

            withContext(Dispatchers.Default) {
                var lastError: Exception? = null
                val maxAttempts = if (useProxy) 3 else 1

                for (attempt in 1..maxAttempts) {
                    val startTime = System.currentTimeMillis()
                    try {
                        val socket = if (useProxy) {
                            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
                            Socket(proxy)
                        } else {
                            Socket()
                        }

                        socket.use { s ->
                            s.connect(InetSocketAddress("1.1.1.1", 53), 5000)
                        }
                        val endTime = System.currentTimeMillis()
                        val duration = endTime - startTime

                        _pingState.value = PingState(ms = duration, isPinging = false)
                        LogRepository.i("Ping success: ${duration}ms (Proxy: $useProxy)", "Ping")
                        return@withContext
                    } catch (e: Exception) {
                        lastError = e
                        if (useProxy && attempt < maxAttempts) {
                            delay(1000.milliseconds)
                        }
                    }
                }

                LogRepository.w("Ping failed (Proxy: $useProxy): ${lastError?.message}", "Ping")
                _pingState.value = PingState(ms = -1, isPinging = false, error = "Timeout")
            }
        } finally {
            mutex.unlock()
        }
    }
}
