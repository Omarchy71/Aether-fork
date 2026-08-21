package io.github.immaghzbad.aetherst.shared.data

import io.github.immaghzbad.aetherst.shared.data.LogRepository
import io.github.immaghzbad.aetherst.shared.core.NetworkClient
import io.github.immaghzbad.aetherst.shared.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

object SpeedTestRepository {
    private val _state = MutableStateFlow(SpeedTestState())
    val state: StateFlow<SpeedTestState> = _state.asStateFlow()

    private var testJob: Job? = null
    private val isCancelled = AtomicBoolean(false)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private var settings: io.github.immaghzbad.aetherst.platform.Settings? = null

    private const val TAG = "SpeedTest"
    private const val PREFIX = "speed_test_"

    fun initialize(settings: io.github.immaghzbad.aetherst.platform.Settings) {
        this.settings = settings
        val savedConfig = loadConfig(settings)
        _state.value = SpeedTestState(config = savedConfig)
    }

    private fun loadConfig(s: io.github.immaghzbad.aetherst.platform.Settings): SpeedTestConfig {
        return SpeedTestConfig(
            selectedServer = runCatching { SpeedTestServer.valueOf(s.getString("${PREFIX}server", SpeedTestServer.CLOUDFLARE.name)) }.getOrDefault(SpeedTestServer.CLOUDFLARE),
            showBits = s.getBoolean("${PREFIX}show_bits", false),
            downloadSizeMb = s.getInt("${PREFIX}download_size", 10),
            uploadSizeMb = s.getInt("${PREFIX}upload_size", 10),
            pingSamples = s.getInt("${PREFIX}ping_samples", 20),
            customServerUrl = s.getString("${PREFIX}custom_url", "")
        )
    }

    private fun saveConfig(config: SpeedTestConfig) {
        val s = settings ?: return
        s.putString("${PREFIX}server", config.selectedServer.name)
        s.putBoolean("${PREFIX}show_bits", config.showBits)
        s.putInt("${PREFIX}download_size", config.downloadSizeMb)
        s.putInt("${PREFIX}upload_size", config.uploadSizeMb)
        s.putInt("${PREFIX}ping_samples", config.pingSamples)
        s.putString("${PREFIX}custom_url", config.customServerUrl)
    }


    fun startTest() {
        if (testJob?.isActive == true) return
        if (!mutex.tryLock()) return

        isCancelled.set(false)
        testJob = CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                val config = _state.value.config
                val server = config.selectedServer
                val serverUrl = resolveServerUrl(server, config)

                LogRepository.i("Speed test started: Server=${server.displayName}, URL=$serverUrl", TAG)
                updateState(_state.value.copy(
                    phase = SpeedTestPhase.PING,
                    currentStep = "Measuring ping & jitter...",
                    progress = 0f,
                    error = null,
                    downloadSpeedHistory = emptyList(),
                    uploadSpeedHistory = emptyList()
                ))

                val pingResult = measurePingAndJitter(serverUrl, config.pingSamples)
                if (isCancelled.get()) return@launch
                updateState(_state.value.copy(
                    result = _state.value.result.copy(
                        pingMs = pingResult.first,
                        jitterMs = pingResult.second,
                        pingSamples = pingResult.third,
                        serverName = server.displayName
                    ),
                    progress = 0.25f,
                    currentStep = "Ping: ${"%.1f".format(pingResult.first)}ms | Jitter: ${"%.1f".format(pingResult.second)}ms"
                ))

                updateState(_state.value.copy(
                    phase = SpeedTestPhase.DOWNLOAD,
                    currentStep = "Testing download speed...",
                    progress = 0.30f
                ))
                val dlResult = measureDownload(serverUrl, config.downloadSizeMb)
                if (isCancelled.get()) return@launch
                updateState(_state.value.copy(
                    result = _state.value.result.copy(
                        downloadBps = dlResult.first,
                        downloadMbps = dlResult.first * 8.0 / (1024.0 * 1024.0)
                    ),
                    downloadSpeedHistory = dlResult.second,
                    progress = 0.65f,
                    currentStep = "Download: ${formatSpeed(dlResult.first, config)}"
                ))

                updateState(_state.value.copy(
                    phase = SpeedTestPhase.UPLOAD,
                    currentStep = "Testing upload speed...",
                    progress = 0.70f
                ))
                val ulResult = measureUpload(serverUrl, config.uploadSizeMb)
                if (isCancelled.get()) return@launch
                updateState(_state.value.copy(
                    result = _state.value.result.copy(
                        uploadBps = ulResult.first,
                        uploadMbps = ulResult.first * 8.0 / (1024.0 * 1024.0)
                    ),
                    uploadSpeedHistory = ulResult.second,
                    progress = 1.0f
                ))

                val finalResult = _state.value.result
                updateState(_state.value.copy(
                    phase = SpeedTestPhase.COMPLETE,
                    currentStep = "Test complete",
                    progress = 1.0f
                ))

                LogRepository.i(
                    "Speed test complete: Ping=${"%.1f".format(finalResult.pingMs)}ms " +
                    "Jitter=${"%.1f".format(finalResult.jitterMs)}ms " +
                    "DL=${"%.2f".format(finalResult.downloadMbps)}Mbps " +
                    "UL=${"%.2f".format(finalResult.uploadMbps)}Mbps",
                    TAG
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogRepository.e("Speed test failed: ${e.message}", TAG)
                updateState(_state.value.copy(
                    phase = SpeedTestPhase.ERROR,
                    currentStep = "Test failed",
                    error = e.message ?: "Unknown error"
                ))
            } finally {
                mutex.unlock()
            }
        }
    }

    fun cancelTest() {
        isCancelled.set(true)
        testJob?.cancel()
        testJob = null
        updateState(_state.value.copy(
            phase = SpeedTestPhase.CANCELLED,
            currentStep = "Test cancelled",
            progress = 0f
        ))
    }

    fun reset() {
        testJob?.cancel()
        testJob = null
        isCancelled.set(false)
        _state.value = SpeedTestState()
    }

    fun updateConfig(config: SpeedTestConfig) {
        updateState(_state.value.copy(config = config))
        saveConfig(config)
    }


    private fun resolveServerUrl(server: SpeedTestServer, config: SpeedTestConfig): String {
        return when (server) {
            SpeedTestServer.CLOUDFLARE -> config.customServerUrl.ifEmpty { "https://speed.cloudflare.com" }
            SpeedTestServer.OFAKIN -> "https://ofakino.pishtazan.dev"
            SpeedTestServer.CUSTOM -> config.customServerUrl.ifEmpty { "https://speed.cloudflare.com" }
        }
    }


    private suspend fun measurePingAndJitter(
        baseUrl: String,
        sampleCount: Int
    ): Triple<Double, Double, List<Long>> {
        return withContext(Dispatchers.IO) {
            val samples = mutableListOf<Long>()
            val pingUrl = "$baseUrl/__down?bytes=0"

            val phaseStart = System.currentTimeMillis()

            repeat(sampleCount) { i ->
                if (isCancelled.get()) return@withContext Triple(-1.0, -1.0, emptyList())

                try {
                    val startTime = System.nanoTime()
                    val conn = URL(pingUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    conn.connect()

                    val code = conn.responseCode
                    conn.disconnect()

                    val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                    if (code == 200 || code == 206) {
                        samples.add(elapsed)

                        val sortedSoFar = samples.sorted()
                        val avgSoFar = samples.average()
                        updateState(_state.value.copy(
                            progress = ((i + 1).toFloat() / sampleCount) * 0.25f,
                            currentStep = "Ping sample ${i + 1}/$sampleCount",
                            livePingMs = elapsed,
                            livePingMin = sortedSoFar.first(),
                            livePingMax = sortedSoFar.last(),
                            livePingAvg = avgSoFar,
                            livePingCount = samples.size,
                            livePhaseElapsed = (System.currentTimeMillis() - phaseStart) / 1000
                        ))
                    }
                } catch (_: Exception) {
                }

                delay(80)
            }

            if (samples.isEmpty()) {
                return@withContext Triple(-1.0, -1.0, emptyList())
            }

            val sorted = samples.sorted()
            val medianPing = sorted[sorted.size / 2].toDouble()

            val mean = samples.average()
            val variance = samples.map { (it - mean) * (it - mean) }.average()
            val jitter = sqrt(variance)

            Triple(medianPing, jitter, samples.toList())
        }
    }


    private suspend fun measureDownload(
        baseUrl: String,
        sizeMb: Int
    ): Pair<Double, List<Double>> {
        return withContext(Dispatchers.IO) {
            val history = mutableListOf<Double>()
            val totalBytes = sizeMb.toLong() * 1024L * 1024L
            val chunkSize = 1024 * 1024 // 1MB chunks
            var totalRead = 0L

            val downloadUrl = "$baseUrl/__down?bytes=${chunkSize}"

            val startTime = System.nanoTime()
            val downloadStartTime = System.currentTimeMillis()

            try {
                var chunkIndex = 0
                while (totalRead < totalBytes && !isCancelled.get()) {
                    val conn = URL(downloadUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.requestMethod = "GET"
                    conn.connect()

                    if (conn.responseCode != 200 && conn.responseCode != 206) {
                        conn.disconnect()
                        break
                    }

                    val inputStream: InputStream = conn.inputStream
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int

                    val chunkStart = System.nanoTime()
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        totalRead += bytesRead
                    }
                    inputStream.close()
                    conn.disconnect()

                    val chunkElapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - chunkStart)
                    var instantSpeed = 0.0
                    if (chunkElapsed > 0) {
                        instantSpeed = chunkSize.toDouble() / chunkElapsed
                        history.add(instantSpeed)
                    }

                    chunkIndex++
                    val progress = (totalRead.toDouble() / totalBytes).coerceIn(0.0, 1.0)
                    val elapsed = (System.currentTimeMillis() - downloadStartTime) / 1000
                    updateState(_state.value.copy(
                        progress = 0.30f + (progress * 0.35f).toFloat(),
                        currentStep = "Downloading ${formatBytes(totalRead)} / ${formatBytes(totalBytes)}",
                        downloadSpeedHistory = history.toList(),
                        liveDownloadBps = instantSpeed,
                        liveDownloadTotal = totalRead,
                        livePhaseElapsed = elapsed
                    ))

                    delay(50)
                }
            } catch (e: Exception) {
                LogRepository.w("Download chunk error: ${e.message}", TAG)
            }

            val totalElapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime)
            val avgSpeed = if (totalElapsed > 0) totalRead.toDouble() / totalElapsed else 0.0

            Pair(avgSpeed, history)
        }
    }


    private suspend fun measureUpload(
        baseUrl: String,
        sizeMb: Int
    ): Pair<Double, List<Double>> {
        return withContext(Dispatchers.IO) {
            val history = mutableListOf<Double>()
            val totalBytes = sizeMb.toLong() * 1024L * 1024L
            val chunkSize = 512 * 1024 // 512KB chunks for upload
            var totalWritten = 0L

            val uploadUrl = "$baseUrl/__up"
            val uploadData = ByteArray(chunkSize) { (it % 256).toByte() }

            val startTime = System.nanoTime()
            val uploadStartTime = System.currentTimeMillis()

            try {
                while (totalWritten < totalBytes && !isCancelled.get()) {
                    val conn = URL(uploadUrl).openConnection() as HttpURLConnection
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.requestMethod = "POST"
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/octet-stream")
                    conn.setRequestProperty("Content-Length", uploadData.size.toString())
                    conn.connect()

                    val chunkStart = System.nanoTime()
                    val outputStream = conn.outputStream
                    outputStream.write(uploadData)
                    outputStream.flush()
                    outputStream.close()

                    val code = conn.responseCode
                    conn.disconnect()

                    val chunkElapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - chunkStart)
                    totalWritten += chunkSize

                    var instantSpeed = 0.0
                    if (chunkElapsed > 0 && (code == 200 || code == 204)) {
                        instantSpeed = chunkSize.toDouble() / chunkElapsed
                        history.add(instantSpeed)
                    }

                    val progress = (totalWritten.toDouble() / totalBytes).coerceIn(0.0, 1.0)
                    val elapsed = (System.currentTimeMillis() - uploadStartTime) / 1000
                    updateState(_state.value.copy(
                        progress = 0.70f + (progress * 0.30f).toFloat(),
                        currentStep = "Uploading ${formatBytes(totalWritten)} / ${formatBytes(totalBytes)}",
                        uploadSpeedHistory = history.toList(),
                        liveUploadBps = instantSpeed,
                        liveUploadTotal = totalWritten,
                        livePhaseElapsed = elapsed
                    ))

                    delay(50)
                }
            } catch (e: Exception) {
                LogRepository.w("Upload chunk error: ${e.message}", TAG)
            }

            val totalElapsed = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime)
            val avgSpeed = if (totalElapsed > 0) totalWritten.toDouble() / totalElapsed else 0.0

            Pair(avgSpeed, history)
        }
    }


    private fun formatSpeed(bytesPerSec: Double, config: SpeedTestConfig): String {
        return if (config.showBits) formatBitsPerSecond(bytesPerSec) else formatBytesPerSecond(bytesPerSec)
    }

    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024L * 1024L * 1024L * 1024L * 1024L -> "${smartFormat(bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0))} PB"
            bytes >= 1024L * 1024L * 1024L * 1024L -> "${smartFormat(bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0))} TB"
            bytes >= 1024L * 1024L * 1024L -> "${smartFormat(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
            bytes >= 1024L * 1024L -> "${smartFormat(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024L -> "${smartFormat(bytes / 1024.0)} KB"
            else -> "$bytes B"
        }
    }

    fun formatBitsPerSecond(bytesPerSec: Double): String {
        val bps = bytesPerSec * 8.0
        return when {
            bps >= 1e15 -> "${"%.2f".format(bps / 1e15)} Pb/s"
            bps >= 1e12 -> "${"%.2f".format(bps / 1e12)} Tb/s"
            bps >= 1e9 -> "${"%.2f".format(bps / 1e9)} Gb/s"
            bps >= 1e6 -> "${"%.2f".format(bps / 1e6)} Mb/s"
            bps >= 1e3 -> "${"%.1f".format(bps / 1e3)} Kb/s"
            else -> "${"%.0f".format(bps)} b/s"
        }
    }

    fun formatBytesPerSecond(bytesPerSec: Double): String {
        return when {
            bytesPerSec >= 1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0 -> "${"%.2f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0 * 1024.0 * 1024.0))} PB/s"
            bytesPerSec >= 1024.0 * 1024.0 * 1024.0 * 1024.0 -> "${"%.2f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0 * 1024.0))} TB/s"
            bytesPerSec >= 1024.0 * 1024.0 * 1024.0 -> "${"%.2f".format(bytesPerSec / (1024.0 * 1024.0 * 1024.0))} GB/s"
            bytesPerSec >= 1024.0 * 1024.0 -> "${"%.2f".format(bytesPerSec / (1024.0 * 1024.0))} MB/s"
            bytesPerSec >= 1024.0 -> "${"%.1f".format(bytesPerSec / 1024.0)} KB/s"
            else -> "${"%.0f".format(bytesPerSec)} B/s"
        }
    }

    /**
     * Smart formatter: shows up to 2 decimal places, drops trailing zeros.
     * e.g. 1.50 -> "1.5", 2.00 -> "2", 3.14 -> "3.14"
     */
    private fun smartFormat(value: Double): String {
        val formatted = "%.2f".format(value)
        return formatted.trimEnd('0').trimEnd('.')
    }

    private fun updateState(newState: SpeedTestState) {
        _state.value = newState
    }
}
