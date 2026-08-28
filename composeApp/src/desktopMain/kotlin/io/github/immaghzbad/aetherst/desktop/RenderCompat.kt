package io.github.immaghzbad.aetherst.desktop

import io.github.immaghzbad.aetherst.shared.desktop.DesktopLogger
import java.io.File
import java.util.concurrent.TimeUnit

object RenderCompat {

    private const val SOFTWARE_API = "SOFTWARE_FAST"

    private val BARE_INTEL_HD = Regex("""intel\(r\)\s*hd\s*graphics\s*$""", RegexOption.IGNORE_CASE)

    fun apply(args: Array<String>) {
        try {
            val envRenderApi = System.getenv("SKIKO_RENDER_API")
            if (!envRenderApi.isNullOrBlank()) {
                log("SKIKO_RENDER_API='$envRenderApi' present -> respecting user choice")
                return
            }
            if (args.any { it.equals("--force-gpu", true) || it.equals("--hardware-render", true) }) {
                log("CLI --force-gpu -> keeping hardware rendering")
                return
            }
            if (args.any { it.equals("--force-software", true) || it.equals("--software-render", true) }) {
                forceSoftware("CLI --force-software override")
                return
            }

            val isWindows = System.getProperty("os.name")?.contains("win", true) == true
            if (!isWindows) return

            val adapters = enumerateGpus()
            if (adapters.isEmpty()) {
                log("GPU enumeration returned nothing -> keeping hardware rendering")
                return
            }
            log("Detected GPUs: " + adapters.joinToString("; ") { "${it.name} (drv=${it.driverDate?.year ?: "?"})" })

            if (evaluate(adapters)) {
                forceSoftware("Legacy/incompatible GPU detected: " + adapters.joinToString { it.name })
            } else {
                log("GPUs compatible with hardware rendering -> keeping hardware")
            }
        } catch (t: Throwable) {
            log("Detection error (${t.message}) -> keeping hardware rendering")
        }
    }

    private fun forceSoftware(reason: String) {
        runCatching { System.setProperty("skiko.renderApi", SOFTWARE_API) }
        log("Forcing Skiko software renderer ($SOFTWARE_API): $reason")
    }

    private fun evaluate(adapters: List<GpuAdapter>): Boolean {
        val names = adapters.map { it.name.lowercase() }
        val hasIntel = names.any { it.contains("intel") }
        val hasNvidia = names.any { it.contains("nvidia") }

        for (n in names) {
            if (isLegacyNvidia(n) || isLegacyIntel(n)) return true
        }

        if (hasIntel && hasNvidia) {
            val intelDate = adapters.firstOrNull { it.name.contains("intel", true) }?.driverDate
            if (intelDate != null && intelDate.year < 2016) return true
        }
        return false
    }

    private fun isLegacyNvidia(n: String): Boolean =
        n.contains("nvs") || n.contains("quadro nvs")

    private fun isLegacyIntel(n: String): Boolean {
        if (n.contains("hd graphics 2000") || n.contains("hd graphics 2500") ||
            n.contains("hd graphics 3000") || n.contains("hd graphics 4000")
        ) return true
        if (n.contains("intel(r) hd graphics family")) return true
        if (BARE_INTEL_HD.containsMatchIn(n)) return true
        return false
    }

    private data class GpuAdapter(
        val name: String,
        val driverDate: DriverDate?,
        val driverVersion: String?
    )

    private data class DriverDate(val year: Int)

    private fun enumerateGpus(): List<GpuAdapter> {
        val wmic = runCommand(
            listOf("wmic", "path", "Win32_VideoController", "get", "Name,DriverDate,DriverVersion", "/value"),
            8
        )
        val lines = if (wmic.isNotEmpty()) wmic else runCommand(
            listOf(
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "Get-CimInstance Win32_VideoController | ForEach-Object { ${'$'}(\${'$'}_.Name)|${'$'}(\${'$'}_.DriverDate)|${'$'}(\${'$'}_.DriverVersion) }"
            ),
            10
        )
        if (lines.isEmpty()) return emptyList()
        return if (lines.any { it.contains("=") }) parseWmic(lines) else parsePowershell(lines)
    }

    private fun parseWmic(lines: List<String>): List<GpuAdapter> {
        val result = mutableListOf<GpuAdapter>()
        var name: String? = null
        var date: String? = null
        var ver: String? = null
        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) {
                if (!name.isNullOrBlank()) result += GpuAdapter(name, parseDriverDate(date ?: ""), ver)
                name = null
                date = null
                ver = null
                continue
            }
            val eq = line.indexOf('=')
            if (eq < 0) continue
            val key = line.substring(0, eq).trim().lowercase()
            val value = line.substring(eq + 1).trim()
            when (key) {
                "name" -> name = value
                "driverdate" -> date = value
                "driverversion" -> ver = value.ifEmpty { null }
            }
        }
        if (!name.isNullOrBlank()) result += GpuAdapter(name, parseDriverDate(date ?: ""), ver)
        return result.filter { it.name.isNotBlank() }
    }

    private fun parsePowershell(lines: List<String>): List<GpuAdapter> =
        lines.mapNotNull { line ->
            val parts = line.split("|")
            val name = parts.getOrNull(0)?.trim().orEmpty()
            if (name.isEmpty()) null
            else GpuAdapter(
                name,
                parseDriverDate(parts.getOrNull(1)?.trim().orEmpty()),
                parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            )
        }

    private fun parseDriverDate(raw: String): DriverDate? {
        if (raw.isEmpty()) return null
        val digits = raw.filter { it.isDigit() }
        if (digits.length >= 4) {
            val y = digits.take(4).toIntOrNull()
            if (y != null && y in 1990..2100) return DriverDate(y)
        }
        return null
    }

    private fun runCommand(command: List<String>, timeoutSeconds: Long): List<String> {
        return try {
            val proc = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return emptyList()
            }
            proc.inputStream.bufferedReader().readText().lineSequence()
                .map { it.trim() }.filter { it.isNotEmpty() }.toList()
        } catch (t: Throwable) {
            log("runCommand failed (${command.firstOrNull()}): ${t.message}")
            emptyList()
        }
    }

    private fun log(msg: String) {
        try { DesktopLogger.i("RenderCompat", msg) } catch (_: Throwable) {}
        try {
            File(System.getProperty("java.io.tmpdir"), "AetherST/aetherst-boot.log")
                .appendText("[RENDER] $msg\n")
        } catch (_: Throwable) {}
    }
}
