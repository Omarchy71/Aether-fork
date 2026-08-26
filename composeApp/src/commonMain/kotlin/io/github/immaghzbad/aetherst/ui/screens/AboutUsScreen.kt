package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.ui.components.IosActionRow
import io.github.immaghzbad.aetherst.shared.ui.components.SectionCard
import io.github.immaghzbad.aetherst.shared.ui.components.AppDivider
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import io.github.immaghzbad.aetherst.shared.ui.theme.appColors

private val IosActiveBlue = AppPalette.accent
private val IosActiveGreen = AppPalette.statusConnected
private val IosPurple = AppPalette.accentVariant

private const val UserGithubUrl = "https://github.com/immaghzbad"
private const val AetherRepositoryUrl = "https://github.com/CluvexStudio/Aether"
private const val HevRepositoryUrl = "https://github.com/heiher/hev-socks5-tunnel"
private const val DeveloperTelegramUrl = "https://t.me/PowerSigma"

@Composable
fun AboutUsScreen(
    appVersion: String = "1.0.0",
    bottomContentPadding: Dp = 0.dp
) {
    val uriHandler = LocalUriHandler.current
    val colors = appColors()
    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceSunken)) {
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + if (isDesktop) 16.dp else 12.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = topPadding,
                bottom = bottomContentPadding + 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { AboutHero(appVersion = appVersion) }
            item {
                SectionCard {
                    SectionTitle("Overview")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "AetherST is a modern client for the Aether tunnel ecosystem. It turns advanced protocol management into a calm, focused experience — real-time stats, intelligent presets, and zero clutter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle("Why AetherST")
                    InfoRow(
                        title = "Censorship-Resistant",
                        description = "Engineered to bypass DPI and protocol-based filtering."
                    )
                    AppDivider()
                    InfoRow(
                        title = "Hybrid Transports",
                        description = "Support for MASQUE (HTTP/2 & HTTP/3), WireGuard, and cascaded tunnels."
                    )
                    AppDivider()
                    InfoRow(
                        title = "Gateway Validation",
                        description = "Verifies gateway health and integrity before routing any data."
                    )
                    AppDivider()
                    InfoRow(
                        title = "Fast Recovery",
                        description = "Automatic reconnection logic that adapts to network changes."
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle("Architecture")
                    InfoRow(
                        title = "The Aether Core",
                        description = "Open-source proxy core built for stability in restricted networks, using dynamic gateway discovery and traffic obfuscation."
                    )
                    AppDivider()
                    InfoRow(
                        title = "Native HEV Stack",
                        description = "A specialized SOCKS5 tunnel written in C that bridges Android's TUN interface with the Aether core for minimal overhead."
                    )
                    AppDivider()
                    InfoRow(
                        title = "SocksTunBridge",
                        description = "A custom Kotlin bridge managing traffic between the system TUN and the proxy core with high compatibility."
                    )
                }
            }
            item {
                SectionCard {
                    SectionTitle("Links & Source")
                    IosActionRow(
                        iconBg = IosActiveBlue.copy(alpha = 0.16f),
                        title = "Project Maintainer",
                        subtitle = "github.com/immaghzbad",
                        onClick = { uriHandler.openUri(UserGithubUrl) }
                    )
                    AppDivider()
                    IosActionRow(
                        iconBg = IosActiveBlue.copy(alpha = 0.16f),
                        title = "Telegram Channel",
                        subtitle = "Support, chat & dev updates",
                        onClick = { uriHandler.openUri(DeveloperTelegramUrl) }
                    )
                    AppDivider()
                    IosActionRow(
                        iconBg = IosActiveGreen.copy(alpha = 0.16f),
                        title = "Aether Repository",
                        subtitle = "Engine source & protocol",
                        onClick = { uriHandler.openUri(AetherRepositoryUrl) }
                    )
                    AppDivider()
                    IosActionRow(
                        iconBg = IosPurple.copy(alpha = 0.16f),
                        title = "HEV Stack Source",
                        subtitle = "Native C TUN-to-SOCKS bridge",
                        onClick = { uriHandler.openUri(HevRepositoryUrl) }
                    )
                }
            }
            item { AboutFooter() }
        }
    }
}

@Composable
private fun AboutHero(appVersion: String) {
    val colors = appColors()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AetherST",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            fontSize = 30.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Advanced Secure Tunneling Client",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionChip(label = "APP", value = appVersion)
            VersionChip(label = "AETHER", value = "1.7.0")
            VersionChip(label = "HEV", value = "2.17.1")
        }
    }
}

@Composable
private fun VersionChip(label: String, value: String) {
    val colors = appColors()
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceRaised,
        border = BorderStroke(0.5.dp, colors.divider)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = colors.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoRow(title: String, description: String) {
    val colors = appColors()
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colors = appColors()
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = colors.textSecondary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun AboutFooter() {
    val colors = appColors()
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Built with ",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF375F),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = " by PowerSigma Team",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "AetherST is an independent client project. The Aether core is developed by CluvexStudio and distributed under its own open-source license.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
