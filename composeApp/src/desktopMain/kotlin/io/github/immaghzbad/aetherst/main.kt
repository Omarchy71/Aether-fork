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
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.shared.App
import io.github.immaghzbad.aetherst.shared.core.ConnectionController
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.ServerSocket
import kotlin.system.exitProcess
import java.awt.Color as AwtColor
import java.awt.Font as AwtFont

private var lockSocket: ServerSocket? = null

fun main() {
    try {
        lockSocket = ServerSocket(18195)
    } catch (_: Exception) {
        exitProcess(0)
    }

    application {
        val viewModelStoreOwner = remember {
            object : ViewModelStoreOwner {
                override val viewModelStore = ViewModelStore()
            }
        }

        var isVisible by remember { mutableStateOf(true) }
        var showExitDialog by remember { mutableStateOf(false) }

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

        val windowState = rememberWindowState(
            width = 420.dp,
            height = 860.dp
        )

        Tray(
            icon = trayIcon,
            tooltip = "AetherST Tunnel",
            onAction = { isVisible = true },
            menu = {
                Item("Open AetherST", onClick = { isVisible = true })
                Separator()
                Item("Exit", onClick = {
                    ConnectionController.getInstance(PlatformContext()).stop()
                    exitApplication()
                })
            }
        )

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
                                        ConnectionController.getInstance(PlatformContext()).stop()
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
