package io.github.immaghzbad.aetherst.platform

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.immaghzbad.aetherst.shared.model.AppInfo
import io.github.immaghzbad.aetherst.shared.core.ConnectionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess

actual class PlatformContext

class DesktopVpnController(private val context: PlatformContext) : VpnController {
    private val connectionController get() = ConnectionController.getInstance(context)

    override fun startVpn() {
        connectionController.start()
    }

    override fun stopVpn() {
        connectionController.stop()
    }

    override fun startProxy() {
        connectionController.start()
    }

    override fun stopProxy() {
        connectionController.stop()
    }

    override fun submitLoginCode(code: String) {
        connectionController.submitLoginCode(code)
    }

    override fun prepareVpn(onPermissionRequired: () -> Unit): Boolean = true

    override fun isVpnPrepared(): Boolean = true
}

class DesktopTrafficProvider : TrafficProvider {
    private var cachedTx = 0L
    private var cachedRx = 0L

    override fun getTxBytes(): Long {
        updateStats()
        return cachedTx
    }
    override fun getRxBytes(): Long {
        updateStats()
        return cachedRx
    }

    private fun updateStats() {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (isWindows) {
                val process = ProcessBuilder("netstat", "-e").start()
                val reader = process.inputStream.bufferedReader()
                reader.useLines { lines ->
                    for (line in lines) {
                        val parts = line.trim().split(Regex("\\s+"))
                        if (parts.size >= 3) {
                            val rxStr = parts[1].replace(Regex("[^0-9]"), "")
                            val txStr = parts[2].replace(Regex("[^0-9]"), "")
                            if (rxStr.isNotEmpty() && txStr.isNotEmpty()) {
                                val rx = rxStr.toLongOrNull() ?: 0L
                                val tx = txStr.toLongOrNull() ?: 0L
                                if (rx > 0 || tx > 0) {
                                    cachedRx = rx
                                    cachedTx = tx
                                    break
                                }
                            }
                        }
                    }
                }
            } else {
                val proc = ProcessBuilder("cat", "/proc/net/dev").start()
                val reader = proc.inputStream.bufferedReader()
                var totalRx = 0L
                var totalTx = 0L
                reader.useLines { lines ->
                    lines.drop(2).forEach { line ->
                        val parts = line.trim().split(Regex(":?\\s+"))
                        if (parts.size >= 10) {
                            totalRx += parts[1].toLongOrNull() ?: 0L
                            totalTx += parts[9].toLongOrNull() ?: 0L
                        }
                    }
                }
                cachedRx = totalRx
                cachedTx = totalTx
            }
        } catch (_: Exception) {}
    }
}

class DesktopAppInfoProvider : AppInfoProvider {
    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = mutableListOf<AppInfo>()
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (!isWindows) return@withContext emptyList()

            
            val startMenuPaths = listOf(
                File(System.getenv("ProgramData") ?: "C:\\ProgramData", "Microsoft\\Windows\\Start Menu\\Programs"),
                File(System.getProperty("user.home"), "AppData\\Roaming\\Microsoft\\Windows\\Start Menu\\Programs")
            )

            for (path in startMenuPaths) {
                if (path.exists()) {
                    path.walkTopDown().filter { it.extension.lowercase() == "lnk" }.forEach { file ->
                        val name = file.nameWithoutExtension
                        if (!name.lowercase().contains("uninstall") && !name.lowercase().contains("help")) {
                            apps.add(AppInfo(name, file.absolutePath, null, false))
                        }
                    }
                }
            }

            
            if (apps.isEmpty()) {
                val commonPaths = listOf(
                    System.getenv("ProgramFiles"),
                    System.getenv("ProgramFiles(x86)")
                ).filterNotNull()

                for (rootPath in commonPaths) {
                    val root = File(rootPath)
                    if (root.exists()) {
                        root.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                            apps.add(AppInfo(dir.name, dir.absolutePath, null, false))
                        }
                    }
                }
            }

            
            try {
                val process = ProcessBuilder("reg", "query", "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall", "/s", "/v", "DisplayName").start()
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.contains("DisplayName")) {
                            val name = line.split("REG_SZ").lastOrNull()?.trim()
                            if (!name.isNullOrBlank() && apps.none { it.name.equals(name, ignoreCase = true) }) {
                                apps.add(AppInfo(name, name, null, false))
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

        } catch (_: Exception) {}
        
        if (apps.isEmpty()) {
            apps.add(AppInfo("Web Browser (Default)", "browser", null, false))
            apps.add(AppInfo("System Proxy (Global)", "all", null, true))
        }
        
        apps.distinctBy { it.name.lowercase() }.sortedBy { it.name.lowercase() }
    }
}

class DesktopSystemUtils : SystemUtils {
    override fun isBatteryOptimized(): Boolean = false
    override fun isNotificationPermissionGranted(): Boolean = true
    override fun getFilesDir(): String {
        val appName = "AetherST-Tunnel"
        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val baseDir = if (isWindows) {
            System.getenv("AppData") ?: System.getProperty("user.home")
        } else {
            System.getProperty("user.home")
        }
        val dir = File(baseDir, appName)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }
    override fun getCacheDir(): String = System.getProperty("java.io.tmpdir")
    override fun getPackageName(): String = "io.github.immaghzbad.aetherst"
    override fun getAppVersion(): String {
        return try {
            val props = java.util.Properties()
            val stream = this::class.java.classLoader.getResourceAsStream("app.properties")
            if (stream != null) {
                stream.use { props.load(it) }
                props.getProperty("app.version", "1.0.2")
            } else "1.0.2"
        } catch (_: Exception) { "1.0.2" }
    }
    override fun exitApp() {
        try {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "AetherST")
            if (tempDir.exists()) tempDir.walkBottomUp().forEach { it.delete() }
        } catch (_: Exception) {}
        exitProcess(0)
    }

    override fun readLastCrashLog(): String? {
        return try {
            val file = File(System.getProperty("java.io.tmpdir"), "last_crash.log")
            if (file.exists()) file.readText() else null
        } catch (_: Exception) {
            null
        }
    }

    override fun clearCrashLog() {
        try {
            File(System.getProperty("java.io.tmpdir"), "last_crash.log").delete()
        } catch (_: Exception) {
        }
    }

    override fun copyToClipboard(text: String) {
        val selection = java.awt.datatransfer.StringSelection(text)
        java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }

    override fun requestNotificationPermission() {}
    override fun requestBatteryOptimization() {}

    override fun exportFile(fileName: String, content: String, onResult: (Boolean) -> Unit) {
        try {
            val fd = java.awt.FileDialog(null as java.awt.Frame?, "Save Backup", java.awt.FileDialog.SAVE)
            fd.file = fileName
            fd.isVisible = true
            val dir = fd.directory
            val file = fd.file
            if (dir != null && file != null) {
                File(dir, file).writeText(content)
                onResult(true)
            } else {
                onResult(false)
            }
        } catch (_: Exception) {
            onResult(false)
        }
    }

    override fun importFile(onResult: (String?) -> Unit) {
        try {
            val fd = java.awt.FileDialog(null as java.awt.Frame?, "Select Backup File", java.awt.FileDialog.LOAD)
            fd.isVisible = true
            val dir = fd.directory
            val file = fd.file
            if (dir != null && file != null) {
                val content = File(dir, file).readText()
                onResult(content)
            } else {
                onResult(null)
            }
        } catch (_: Exception) {
            onResult(null)
        }
    }

    override fun shareFile(fileName: String, content: String) {
        exportFile(fileName, content) {}
    }

    override fun readInternalAsset(fileName: String): String? {
        return try {
            val classLoader = Thread.currentThread().contextClassLoader ?: this::class.java.classLoader
            classLoader.getResourceAsStream(fileName)?.bufferedReader()?.use { it.readText() }
        } catch (_: Exception) {
            null
        }
    }

    override fun setSystemProxy(host: String, port: Int) {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (!isWindows) return

            val proxyStr = "$host:$port"
            val regPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"
            
            ProcessBuilder("reg", "add", regPath, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", "1", "/f").start().waitFor()
            ProcessBuilder("reg", "add", regPath, "/v", "ProxyServer", "/t", "REG_SZ", "/d", proxyStr, "/f").start().waitFor()
            ProcessBuilder("reg", "add", regPath, "/v", "ProxyOverride", "/t", "REG_SZ", "/d", "<local>", "/f").start().waitFor()
            
            
            val psCommand = "[System.Runtime.InteropServices.Marshal]::GetLastWin32Error(); " +
                           "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class WinInet { [DllImport(\"wininet.dll\")] public static extern bool InternetSetOption(IntPtr hInternet, int dwOption, IntPtr lpBuffer, int dwBufferLength); }'; " +
                           "[WinInet]::InternetSetOption([IntPtr]::Zero, 39, [IntPtr]::Zero, 0); [WinInet]::InternetSetOption([IntPtr]::Zero, 37, [IntPtr]::Zero, 0)"
            ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-Command", psCommand).start()
        } catch (_: Exception) {}
    }

    override fun clearSystemProxy() {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (!isWindows) return

            val regPath = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings"
            ProcessBuilder("reg", "add", regPath, "/v", "ProxyEnable", "/t", "REG_DWORD", "/d", "0", "/f").start().waitFor()
            
            val psCommand = "Add-Type -TypeDefinition 'using System; using System.Runtime.InteropServices; public class WinInet { [DllImport(\"wininet.dll\")] public static extern bool InternetSetOption(IntPtr hInternet, int dwOption, IntPtr lpBuffer, int dwBufferLength); }'; " +
                           "[WinInet]::InternetSetOption([IntPtr]::Zero, 39, [IntPtr]::Zero, 0); [WinInet]::InternetSetOption([IntPtr]::Zero, 37, [IntPtr]::Zero, 0)"
            ProcessBuilder("powershell", "-WindowStyle", "Hidden", "-Command", psCommand).start()
        } catch (_: Exception) {}
    }

    override fun isAdministrator(): Boolean {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (!isWindows) return true
            
            val process = ProcessBuilder("net", "session").start()
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    override fun relaunchAsAdmin() {
        try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            if (!isWindows) return

            val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java.exe"
            val jarFile = File(System.getProperty("user.dir")).listFiles()?.find { it.extension == "jar" }?.absolutePath
                ?: (System.getProperty("user.dir") + File.separator + "AetherST-Tunnel.exe")

            val target = if (jarFile.endsWith(".jar")) javaBin else jarFile
            val args = if (jarFile.endsWith(".jar")) listOf("-jar", jarFile) else emptyList()

            val cmdExe = File("C:\\Windows\\System32\\cmd.exe")
            if (cmdExe.exists()) {
                val cmdArgs = mutableListOf("/c", "start", "", "/wait")
                cmdArgs.addAll(listOf("powershell", "-Command", "Start-Process '$target' -ArgumentList '${args.joinToString("', '")}' -Verb RunAs"))
                ProcessBuilder(cmdArgs).start()
            } else {
                ProcessBuilder("powershell", "-Command", "Start-Process '$target' -Verb RunAs").start()
            }
            exitApp()
        } catch (_: Exception) {}
    }

    override fun getInterfaceMtu(): Int {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces().asSequence().firstOrNull {
                it.isUp && !it.isLoopback && !it.isVirtual
            }?.mtu ?: 1500
        } catch (_: Exception) {
            1500
        }
    }

    override fun execPing(host: String, size: Int, timeoutMs: Int, dontFragment: Boolean): Boolean {
        return try {
            val isWindows = System.getProperty("os.name").lowercase().contains("win")
            val pb = if (isWindows) {
                if (dontFragment) {
                    ProcessBuilder("ping", "-n", "1", "-l", size.toString(), "-f", "-w", timeoutMs.toString(), host)
                } else {
                    ProcessBuilder("ping", "-n", "1", "-l", size.toString(), "-w", timeoutMs.toString(), host)
                }
            } else {
                if (dontFragment) {
                    ProcessBuilder("ping", "-c", "1", "-s", size.toString(), "-M", "do", "-W", (timeoutMs / 1000).toString(), host)
                } else {
                    ProcessBuilder("ping", "-c", "1", "-s", size.toString(), "-W", (timeoutMs / 1000).toString(), host)
                }
            }
            pb.redirectErrorStream(true)
            val proc = pb.start()
            proc.waitFor(timeoutMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS) && proc.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }
}

actual fun getVpnController(context: PlatformContext): VpnController = DesktopVpnController(context)
actual fun getTrafficProvider(context: PlatformContext): TrafficProvider = DesktopTrafficProvider()
actual fun getAppInfoProvider(context: PlatformContext): AppInfoProvider = DesktopAppInfoProvider()
actual fun getSystemUtils(context: PlatformContext): SystemUtils = DesktopSystemUtils()

actual fun getCurrentTimestamp(): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
    return formatter.format(java.util.Date())
}

actual val isDesktop: Boolean = true

actual fun getDeviceModel(): String {
    return try {
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osArch = System.getProperty("os.arch") ?: ""
        val computerName = try {
            if (osName.lowercase().contains("win")) {
                val process = ProcessBuilder("hostname").start()
                val name = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                if (name.isNotEmpty()) name else null
            } else null
        } catch (_: Exception) { null }

        val base = computerName ?: osName
        if (osArch.isNotEmpty()) "$base ($osArch)" else base
    } catch (_: Exception) { "Unknown PC" }
}

actual fun getOsVersion(): String {
    return try {
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osVersion = System.getProperty("os.version") ?: ""
        if (osVersion.isNotEmpty()) "$osName $osVersion" else osName
    } catch (_: Exception) { "Unknown" }
}

@Composable
actual fun AppIcon(app: AppInfo, modifier: Modifier) {
    val iconPath = app.icon
    val bitmap = remember(iconPath) {
        if (iconPath != null && File(iconPath).exists()) {
            try {
                when {
                    iconPath.lowercase().endsWith(".lnk") -> {
                        val target = resolveLnkTarget(iconPath)
                        if (target != null && File(target).exists() && target.lowercase().endsWith(".exe")) {
                            extractExeIcon(target)
                        } else null
                    }
                    iconPath.lowercase().endsWith(".exe") -> extractExeIcon(iconPath)
                    iconPath.lowercase().endsWith(".ico") -> {
                        val bytes = File(iconPath).readBytes()
                        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    }
                    else -> {
                        val bytes = File(iconPath).readBytes()
                        org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                    }
                }
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.Black.copy(alpha = 0.3f))
        )
    }
}

private fun resolveLnkTarget(lnkPath: String): String? {
    return try {
        val process = ProcessBuilder(
            "powershell", "-NoProfile", "-Command",
            "(New-Object -ComObject WScript.Shell).CreateShortcut('$lnkPath').TargetPath"
        ).start()
        val result = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        if (result.isNotEmpty() && File(result).exists()) result else null
    } catch (_: Exception) {
        null
    }
}

private fun extractExeIcon(exePath: String): androidx.compose.ui.graphics.ImageBitmap? {
    return try {
        val process = ProcessBuilder(
            "powershell", "-NoProfile", "-Command",
            "[System.Drawing.Icon]::ExtractAssociatedIcon('$exePath').ToBitmap().Save('%TEMP%\\icon_extract.png')"
        ).start()
        process.waitFor()
        val tempIcon = File(System.getenv("TEMP"), "icon_extract.png")
        if (tempIcon.exists()) {
            val bytes = tempIcon.readBytes()
            tempIcon.delete()
            org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } else null
    } catch (_: Exception) {
        null
    }
}
