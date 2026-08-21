package io.github.immaghzbad.aetherst.shared.data

import io.github.immaghzbad.aetherst.shared.core.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlinx.serialization.json.*
import java.net.InetSocketAddress
import java.net.Proxy
import kotlin.time.Duration.Companion.milliseconds

object IpInfoRepository {
    private val _ipInfo = MutableStateFlow(IpInfo())
    val ipInfo: StateFlow<IpInfo> = _ipInfo.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = kotlinx.coroutines.sync.Mutex()

    suspend fun fetchIpInfo(socksHost: String = "127.0.0.1", socksPort: Int = 1819, useProxy: Boolean = true) {
        if (!mutex.tryLock()) return
        
        try {
            _ipInfo.value = _ipInfo.value.copy(isLoading = true)

            withContext(Dispatchers.Default) {
                if (!useProxy) {
                    LogRepository.i("Querying public IP endpoint...", "IpWhois")
                    if (tryFetchDirectIpSb() || tryFetchDirectIpify()) {
                        return@withContext
                    }
                } else {
                    delay(2500.milliseconds)

                    for (attempt in 1..3) {
                        LogRepository.i("Querying public IP via tunnel ($socksHost:$socksPort)...", "IpWhois")

                        val success = tryFetchFromIpSb(socksHost, socksPort) || 
                                      tryFetchFromIpify(socksHost, socksPort) ||
                                      tryFetchFromIfconfig(socksHost, socksPort) ||
                                      tryFetchFromAmazon(socksHost, socksPort)
                        
                        if (success) return@withContext

                        if (attempt < 3) {
                            delay(2000.milliseconds)
                        }
                    }
                }

                LogRepository.w("${if (useProxy) "SOCKS proxy" else "Direct"} IP lookup failed.", "IpWhois")
                _ipInfo.value = _ipInfo.value.copy(
                    isLoading = false,
                    error = if (useProxy) "Proxy Lookup Failed" else "Direct Lookup Failed"
                )
            }
        } finally {
            mutex.unlock()
        }
    }

    private fun tryFetchFromIpSb(socksHost: String, socksPort: Int): Boolean {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.ip.sb/geoip")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return false
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                    val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        _ipInfo.value = IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false)
                        LogRepository.i("Geo-data synchronized (ip.sb): $ip ($country)", "IpWhois")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            LogRepository.w("ip.sb via SOCKS error: ${e.message}", "IpWhois")
            false
        }
    }

    private fun tryFetchFromIpify(socksHost: String, socksPort: Int): Boolean {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return false
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        _ipInfo.value = IpInfo(ip, "Unknown", "", "🌐", false)
                        LogRepository.i("IP discovered via ipify: $ip", "IpWhois")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            LogRepository.w("ipify via SOCKS error: ${e.message}", "IpWhois")
            false
        }
    }

    private fun tryFetchFromIfconfig(socksHost: String, socksPort: Int): Boolean {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://ifconfig.me/all.json")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return false
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip_addr"]?.jsonPrimitive?.content ?: ""
                    val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""

                    if (ip.isNotEmpty()) {
                        _ipInfo.value = IpInfo(ip, "Unknown", countryCode, getFlagEmoji(countryCode), false)
                        LogRepository.i("IP discovered via ifconfig: $ip", "IpWhois")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            LogRepository.w("ifconfig.me via SOCKS error: ${e.message}", "IpWhois")
            false
        }
    }

    private fun tryFetchFromAmazon(socksHost: String, socksPort: Int): Boolean {
        return try {
            val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(socksHost, socksPort))
            val client = NetworkClient.instance.newBuilder()
                .proxy(proxy)
                .connectTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(12000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            val request = Request.Builder()
                .url("https://checkip.amazonaws.com")
                .header("User-Agent", "curl/7.64.1")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val ip = response.body?.string()?.trim() ?: return false
                    if (ip.isNotEmpty()) {
                        _ipInfo.value = IpInfo(ip, "Unknown", "", "🌐", false)
                        LogRepository.i("IP discovered via Amazon: $ip", "IpWhois")
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            LogRepository.w("Amazon IP check failed: ${e.message}", "IpWhois")
            false
        }
    }

    private fun tryFetchDirectIpSb(): Boolean {
        return try {
            val request = Request.Builder().url("https://api.ip.sb/geoip").build()
            NetworkClient.instance.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return false
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) {
                        val country = root["country"]?.jsonPrimitive?.content ?: "Unknown"
                        val countryCode = root["country_code"]?.jsonPrimitive?.content ?: ""
                        _ipInfo.value = IpInfo(ip, country, countryCode, getFlagEmoji(countryCode), false)
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
    }

    private fun tryFetchDirectIpify(): Boolean {
        return try {
            val request = Request.Builder().url("https://api.ipify.org?format=json").build()
            NetworkClient.instance.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val jsonStr = response.body?.string() ?: return false
                    val root = json.parseToJsonElement(jsonStr).jsonObject
                    val ip = root["ip"]?.jsonPrimitive?.content ?: ""
                    if (ip.isNotEmpty()) {
                        _ipInfo.value = IpInfo(ip, "Unknown", "", "🌐", false)
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) { false }
    }

    fun reset() { _ipInfo.value = IpInfo() }

    private fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🌐"
        val firstLetter = countryCode[0].uppercaseChar().code - 'A'.code + 0x1F1E6
        val secondLetter = countryCode[1].uppercaseChar().code - 'A'.code + 0x1F1E6
        return codePointToString(firstLetter) + codePointToString(secondLetter)
    }

    private fun codePointToString(codePoint: Int): String {
        return if (codePoint <= 0xFFFF) {
            codePoint.toChar().toString()
        } else {
            val high = ((codePoint - 0x10000) shr 10) + 0xD800
            val low = ((codePoint - 0x10000) and 0x3FF) + 0xDC00
            high.toChar().toString() + low.toChar().toString()
        }
    }
}
