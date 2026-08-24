package io.github.immaghzbad.aetherst

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.UninstallCleanup
import io.github.immaghzbad.aetherst.shared.App
import io.github.immaghzbad.aetherst.shared.core.ConnectionController
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.net.ServerSocket
import kotlin.system.exitProcess
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont

private var lockSocket: ServerSocket? = null
private var requestShowWindow: (() -> Unit)? = null

@Volatile
private var windowShown = false

private fun startupLogFile(): File {
    val dir = System.getProperty("jpackage.app.dir")
        ?: ProcessHandle.current().info().command().orElse(null)?.let { File(it).parent }
        ?: System.getProperty("java.io.tmpdir")
    return File(dir, "startup_error.log")
}

private fun appendStartupLog(message: String) {
    try {
        startupLogFile().appendText("[${java.time.LocalDateTime.now()}] $message\n")
    } catch (_: Exception) {}
}

private fun signalRunningInstance(): Boolean = try {
    java.net.Socket("127.0.0.1", 18195).use { socket ->
        socket.getOutputStream().write(1)
        socket.getOutputStream().flush()
    }
    true
} catch (_: Exception) {
    false
}

private fun killStaleCoreProcesses() {
    try {
        val isWindows = System.getProperty("os.name")?.lowercase()?.contains("win") == true
        if (!isWindows) return
        val processes = listOf("aether-core.exe", "tun2socks.exe", "aether-core", "tun2socks")
        for (proc in processes) {
            ProcessBuilder("taskkill", "/F", "/IM", proc).start().waitFor()
        }
    } catch (_: Exception) {}
}

private fun cleanTempFiles() {
    try {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "AetherST")
        if (tempDir.exists()) tempDir.walkBottomUp().forEach { it.delete() }
        val routingFile = File(System.getProperty("java.io.tmpdir"), "routing.ast")
        if (routingFile.exists()) routingFile.delete()
    } catch (_: Exception) {}
}

fun main(args: Array<String>) {
    val jvmArgs = System.getProperty("sun.java.command")?.split(" ") ?: emptyList()

    if (jvmArgs.contains("--cleanup")) {
        UninstallCleanup.performManualCleanup()
        cleanTempFiles()
        exitProcess(0)
    }

    val softwareFallbackTried = args.contains("--software-render")
    if (softwareFallbackTried) {
        System.setProperty("skia.renderPipeline", "software")
    }

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        appendStartupLog("Uncaught exception on thread '${thread.name}': ${throwable.stackTraceToString()}")
    }

    try {
        UninstallCleanup.handleStartupCleanup()
    } catch (e: Exception) {
        appendStartupLog("Startup cleanup failed: ${e.stackTraceToString()}")
    }
    killStaleCoreProcesses()

    try {
        lockSocket = ServerSocket(18195)
    } catch (_: Exception) {
        signalRunningInstance()
        exitProcess(0)
    }

    Thread({
        while (true) {
            val client = try { lockSocket?.accept() ?: break } catch (_: Exception) { break }
            runCatching { client.close() }
            try { requestShowWindow?.invoke() } catch (_: Exception) {}
        }
    }, "Aether-SingleInstance").apply { isDaemon = true }.start()

    Thread({
        try {
            Thread.sleep(20000)
        } catch (_: InterruptedException) {
            return@Thread
        }
        if (!windowShown) {
            appendStartupLog(
                "Window did not appear within 20 seconds. softwareFallbackTried=$softwareFallbackTried. " +
                        "os=${System.getProperty("os.name")} java=${System.getProperty("java.version")}"
            )
            if (!softwareFallbackTried) {
                appendStartupLog("Relaunching with software rendering fallback...")
                try {
                    val exePath = ProcessHandle.current().info().command().orElse(null)
                    if (exePath != null) {
                        ProcessBuilder(exePath, "--software-render")
                            .apply { environment()["JAVA_TOOL_OPTIONS"] = "-Dskia.renderPipeline=software" }
                            .start()
                        exitProcess(0)
                    } else {
                        appendStartupLog("Relaunch skipped: own executable path unavailable")
                    }
                } catch (e: Exception) {
                    appendStartupLog("Relaunch failed: ${e.stackTraceToString()}")
                }
            }
        }
    }, "Aether-StartupWatchdog").apply { isDaemon = false }.start()

    application {
        val viewModelStoreOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }

        var isVisible by remember { mutableStateOf(true) }
        var showExitDialog by remember { mutableStateOf(false) }

        val windowState = rememberWindowState(
            width = 420.dp,
            height = 860.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )

        remember {
            requestShowWindow = {
                isVisible = true
                windowState.isMinimized = false
            }
            true
        }

        val traySupported = remember {
            runCatching { java.awt.SystemTray.isSupported() }.getOrDefault(false)
        }

        if (traySupported) {
            val trayIcon = remember {
                val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
                val g = image.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                g.color = AwtColor(0, 122, 255)
                g.fillOval(2, 2, 28, 28)
                g.color = AwtColor.WHITE
                g.font = AwtFont("Arial", AwtFont.BOLD, 18)
                g.drawString("A", 10, 22)
                g.dispose()
                image.toPainter()
            }

            Tray(
                icon = trayIcon,
                tooltip = "AetherST Tunnel",
                onAction = { isVisible = true },
                menu = {
                    Item("Open AetherST", onClick = { isVisible = true })
                    Separator()
                    Item("Exit", onClick = {
                        try {
                            ConnectionController.getInstance(PlatformContext()).stop()
                        } catch (_: Exception) {}
                        cleanTempFiles()
                        lockSocket?.close()
                        lockSocket = null
                        exitApplication()
                    })
                }
            )
        }

        if (isVisible) {
            Window(
                onCloseRequest = { showExitDialog = true },
                title = "AetherST Tunnel",
                state = windowState,
                resizable = false,
                undecorated = true,
                transparent = true,
                visible = isVisible
            ) {
                windowShown = true
                CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            WindowDraggableArea {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .background(Color(0xFF1C1C1E))
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(Color(0xFF007AFF), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "A",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "AetherST Tunnel",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .clickable { windowState.isMinimized = true }
                                                .background(Color.White.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Minimize",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .clickable { showExitDialog = true }
                                                .background(Color(0xFFFF3B30).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close",
                                                tint = Color(0xFFFF3B30),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                App(PlatformContext())
                            }

                            if (showExitDialog) {
                                ExitDialog(
                                    onHide = {
                                        isVisible = false
                                        showExitDialog = false
                                    },
                                    onExit = {
                                        try {
                                            ConnectionController.getInstance(PlatformContext()).stop()
                                        } catch (_: Exception) {}
                                        cleanTempFiles()
                                        lockSocket?.close()
                                        lockSocket = null
                                        exitApplication()
                                    },
                                    onDismiss = { showExitDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExitDialog(
    onHide: () -> Unit,
    onExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Exit Completely", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onHide) {
                Text("Hide to Tray", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                "Exit AetherST?",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                "Would you like to keep the tunnel running in the background or exit the application entirely?",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        },
        containerColor = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    )
}
