package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.immaghzbad.aetherst.platform.PlatformContext
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.data.AutoDetectRepository
import io.github.immaghzbad.aetherst.shared.model.*

private val IosCardBg = Color(0xFF1C1C1E)
private val IosGroupBg = Color(0xFF2C2C2E)
private val IosSecondaryLabel = Color(0xFF8E8E93)
private val IosActiveGreen = Color(0xFF34C759)
private val IosActiveBlue = Color(0xFF007AFF)
private val IosErrorRed = Color(0xFFFF3B30)
private val IosAmber = Color(0xFFFF9500)
private val IosPurple = Color(0xFFAF52DE)

@Composable
fun AutoDetectScreen(
    onBack: () -> Unit,
    onApplyResult: (AutoDetectResult) -> Unit,
    onApplyManualProtocol: (AutoDetectResult) -> Unit,
    platformContext: PlatformContext,
    bottomContentPadding: Dp = 0.dp
) {
    val state by AutoDetectRepository.state.collectAsState()

    val retest: () -> Unit = {
        AutoDetectRepository.reset()
        AutoDetectRepository.startDetection(platformContext)
    }

    LaunchedEffect(Unit) {
        AutoDetectRepository.startDetection(platformContext)
    }

    DisposableEffect(Unit) {
        onDispose { AutoDetectRepository.cancel() }
    }

    val scaleFactor = remember { 1f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = (8 * scaleFactor).dp,
                    end = (12 * scaleFactor).dp,
                    top = if (isDesktop) 12.dp else 36.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                AutoDetectRepository.cancel()
                onBack()
            }, modifier = Modifier.size((40 * scaleFactor).dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size((24 * scaleFactor).dp)
                )
            }
            Spacer(modifier = Modifier.width((4 * scaleFactor).dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Smart Auto-Detect",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = (26 * scaleFactor).sp,
                    lineHeight = (30 * scaleFactor).sp
                )
                Text(
                    text = "Scan network & find optimal configuration",
                    style = MaterialTheme.typography.bodySmall,
                    color = IosSecondaryLabel,
                    fontSize = (12 * scaleFactor).sp
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = (16 * scaleFactor).dp,
                end = (16 * scaleFactor).dp,
                bottom = bottomContentPadding + (24 * scaleFactor).dp,
                top = (4 * scaleFactor).dp
            ),
            verticalArrangement = Arrangement.spacedBy((12 * scaleFactor).dp)
        ) {
            if (state.phase != AutoDetectPhase.IDLE && state.phase != AutoDetectPhase.COMPLETE && state.phase != AutoDetectPhase.ERROR) {
                item {
                    AutoDetectProgressCard(state, scaleFactor)
                }
            }

            if (state.currentStep.isNotEmpty() && state.phase != AutoDetectPhase.COMPLETE) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = IosActiveBlue.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding((14 * scaleFactor).dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size((18 * scaleFactor).dp),
                                color = IosActiveBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                            Text(
                                text = state.currentStep,
                                color = IosActiveBlue,
                                fontSize = (13 * scaleFactor).sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (state.protocolResults.isNotEmpty() || state.phase == AutoDetectPhase.COMPLETE) {
                item {
                    SectionHeader("NETWORK ENVIRONMENT", scaleFactor)
                    NetworkFingerprintCard(state, scaleFactor)
                }
            }

            if (state.protocolResults.isNotEmpty()) {
                item {
                    SectionHeader("PROTOCOL LATENCY TEST", scaleFactor)
                }
                items(state.protocolResults) { result ->
                    ProtocolProbeRow(result, scaleFactor)
                }
            }

            if (state.mtuResult.status != ProbeStatus.IDLE) {
                item {
                    SectionHeader("MTU DISCOVERY", scaleFactor)
                    MtuProbeRow(state.mtuResult, scaleFactor)
                }
            }

            if (state.noiseResults.isNotEmpty()) {
                item {
                    SectionHeader("OBFUSCATION MODES", scaleFactor)
                }
                items(state.noiseResults) { result ->
                    NoiseProbeRow(result, scaleFactor)
                }
            }

            if (state.scanModeResults.isNotEmpty()) {
                item {
                    SectionHeader("SCAN STRATEGIES", scaleFactor)
                }
                items(state.scanModeResults) { result ->
                    ScanModeProbeRow(result, scaleFactor)
                }
            }

            if (state.phase == AutoDetectPhase.COMPLETE && state.finalResult != null) {
                item {
                    SectionHeader("RECOMMENDED CONFIGURATION", scaleFactor)
                    AutoDetectFinalResult(state.finalResult!!, onApplyResult, retest, scaleFactor)
                }
            }

            if (state.phase == AutoDetectPhase.ERROR) {
                item {
                    ErrorCard(state.error, retest, scaleFactor)
                }
            }

            item {
                SectionHeader("MANUAL PROTOCOL SELECTION", scaleFactor)
                Text(
                    text = "Choose a protocol manually with its recommended settings",
                    color = IosSecondaryLabel,
                    fontSize = (11 * scaleFactor).sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = (8 * scaleFactor).dp)
                )
            }
            item {
                ManualProtocolCard(
                    protocol = AetherProtocol.MASQUE,
                    title = "MASQUE",
                    subtitle = "HTTP/2 & HTTP/3 tunneling — Best for DPI bypass",
                    icon = Icons.Default.Shield,
                    iconColor = IosActiveBlue,
                    recommendedNoise = AetherNoise.FIREWALL,
                    recommendedScan = AetherScanMode.BALANCED,
                    extraSettings = listOf(
                        "HTTP/2 Fallback" to "Enabled",
                        "ECH (Encrypted Client Hello)" to "Optional",
                        "Packet Fragmentation" to "For strict DPI"
                    ),
                    onApply = {
                        onApplyManualProtocol(AutoDetectResult(
                            recommendedProtocol = AetherProtocol.MASQUE,
                            recommendedNoise = AetherNoise.FIREWALL,
                            recommendedScanMode = AetherScanMode.BALANCED,
                            recommendedMtu = 1100,
                            recommendedIpMode = AetherIpMode.IPV4,
                            recommendedH2Mode = true,
                            recommendedEch = false,
                            recommendedFragment = false,
                            confidence = 0.9f
                        ))
                    },
                    scaleFactor = scaleFactor
                )
            }
            item {
                ManualProtocolCard(
                    protocol = AetherProtocol.WG,
                    title = "WireGuard",
                    subtitle = "High-speed modern VPN — Best for raw performance",
                    icon = Icons.Default.Speed,
                    iconColor = IosActiveGreen,
                    recommendedNoise = AetherNoise.BALANCED,
                    recommendedScan = AetherScanMode.TURBO,
                    extraSettings = listOf(
                        "Keepalive" to "5s",
                        "No Data Check" to "Enabled",
                        "Reconnect" to "Smart"
                    ),
                    onApply = {
                        onApplyManualProtocol(AutoDetectResult(
                            recommendedProtocol = AetherProtocol.WG,
                            recommendedNoise = AetherNoise.BALANCED,
                            recommendedScanMode = AetherScanMode.TURBO,
                            recommendedMtu = 1100,
                            recommendedIpMode = AetherIpMode.IPV4,
                            recommendedH2Mode = false,
                            recommendedEch = false,
                            recommendedFragment = false,
                            confidence = 0.9f
                        ))
                    },
                    scaleFactor = scaleFactor
                )
            }
            item {
                ManualProtocolCard(
                    protocol = AetherProtocol.GOOL,
                    title = "Gool (WG-in-WG)",
                    subtitle = "Double WireGuard tunnel — Best for heavy censorship",
                    icon = Icons.Default.AllInclusive,
                    iconColor = IosPurple,
                    recommendedNoise = AetherNoise.BALANCED,
                    recommendedScan = AetherScanMode.BALANCED,
                    extraSettings = listOf(
                        "Double Encryption" to "Active",
                        "Outer + Inner Tunnel" to "Nested WG",
                        "Best for" to "Strict firewalls"
                    ),
                    onApply = {
                        onApplyManualProtocol(AutoDetectResult(
                            recommendedProtocol = AetherProtocol.GOOL,
                            recommendedNoise = AetherNoise.BALANCED,
                            recommendedScanMode = AetherScanMode.BALANCED,
                            recommendedMtu = 1100,
                            recommendedIpMode = AetherIpMode.IPV4,
                            recommendedH2Mode = false,
                            recommendedEch = false,
                            recommendedFragment = false,
                            confidence = 0.9f
                        ))
                    },
                    scaleFactor = scaleFactor
                )
            }
            item {
                Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, scaleFactor: Float) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = IosSecondaryLabel,
        fontSize = (10 * scaleFactor).sp,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = (4 * scaleFactor).dp)
    )
}

@Composable
private fun AutoDetectProgressCard(state: AutoDetectState, scaleFactor: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column(modifier = Modifier.padding((16 * scaleFactor).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Scanning...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
                Text("${state.progressPercent}%", color = IosActiveBlue, fontWeight = FontWeight.ExtraBold, fontSize = (14 * scaleFactor).sp)
            }
            Spacer(modifier = Modifier.height((10 * scaleFactor).dp))
            LinearProgressIndicator(
                progress = { state.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height((6 * scaleFactor).dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = IosActiveBlue,
                trackColor = IosGroupBg
            )
        }
    }
}

@Composable
private fun NetworkFingerprintCard(state: AutoDetectState, scaleFactor: Float) {
    val fingerprint = state.finalResult?.networkFingerprint
    val isComplete = state.phase == AutoDetectPhase.COMPLETE && fingerprint != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Column(modifier = Modifier.padding((14 * scaleFactor).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
            ) {
                InfoPill(
                    label = "NETWORK",
                    value = if (isComplete) fingerprint!!.networkType.uppercase() else "—",
                    color = if (isComplete && fingerprint!!.supportsDPI) IosAmber else IosActiveGreen,
                    modifier = Modifier.weight(1f),
                    scaleFactor = scaleFactor
                )
                InfoPill(
                    label = "DPI DETECTED",
                    value = if (isComplete) if (fingerprint!!.supportsDPI) "YES" else "NO" else "—",
                    color = if (isComplete && fingerprint!!.supportsDPI) IosErrorRed else IosActiveGreen,
                    modifier = Modifier.weight(1f),
                    scaleFactor = scaleFactor
                )
            }
            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy((8 * scaleFactor).dp)
            ) {
                InfoPill(
                    label = "IPv6",
                    value = if (isComplete) if (fingerprint!!.supportsIPv6) "YES" else "NO" else "—",
                    color = if (isComplete && fingerprint!!.supportsIPv6) IosActiveGreen else IosSecondaryLabel,
                    modifier = Modifier.weight(1f),
                    scaleFactor = scaleFactor
                )
                InfoPill(
                    label = "ISP / IP",
                    value = if (isComplete) {
                        val isp = fingerprint!!.carrierOrIsp
                        if (isp.length > 16) isp.take(16) + "…" else isp
                    } else "—",
                    color = IosActiveBlue,
                    modifier = Modifier.weight(1f),
                    scaleFactor = scaleFactor
                )
            }
        }
    }
}

@Composable
private fun InfoPill(label: String, value: String, color: Color, modifier: Modifier = Modifier, scaleFactor: Float) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = IosGroupBg
    ) {
        Column(
            modifier = Modifier.padding(horizontal = (10 * scaleFactor).dp, vertical = (8 * scaleFactor).dp)
        ) {
            Text(
                text = label,
                color = IosSecondaryLabel,
                fontSize = (8 * scaleFactor).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = color,
                fontSize = (12 * scaleFactor).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProtocolProbeRow(result: ProtocolProbeResult, scaleFactor: Float) {
    val statusColor = when (result.status) {
        ProbeStatus.SUCCESS -> IosActiveGreen
        ProbeStatus.FAILED -> IosErrorRed
        ProbeStatus.RUNNING -> IosAmber
        ProbeStatus.SKIPPED -> IosSecondaryLabel
        ProbeStatus.IDLE -> IosSecondaryLabel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((14 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size((10 * scaleFactor).dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                Column {
                    Text(
                        text = result.protocol.displayName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (14 * scaleFactor).sp
                    )
                    when (result.status) {
                        ProbeStatus.SUCCESS -> Text(
                            text = "RTT: ${result.latencyMs}ms median (${result.latencyMs}ms avg)",
                            color = IosActiveGreen,
                            fontSize = (11 * scaleFactor).sp
                        )
                        ProbeStatus.FAILED -> Text(
                            text = result.error ?: "Connection failed",
                            color = IosErrorRed,
                            fontSize = (11 * scaleFactor).sp
                        )
                        ProbeStatus.RUNNING -> Text(
                            text = "Measuring latency...",
                            color = IosAmber,
                            fontSize = (11 * scaleFactor).sp
                        )
                        ProbeStatus.SKIPPED -> Text(
                            text = result.error ?: "Skipped",
                            color = IosSecondaryLabel,
                            fontSize = (11 * scaleFactor).sp
                        )
                        else -> {}
                    }
                }
            }

            when (result.status) {
                ProbeStatus.SUCCESS -> Icon(
                    Icons.Default.CheckCircle, null,
                    tint = IosActiveGreen,
                    modifier = Modifier.size((20 * scaleFactor).dp)
                )
                ProbeStatus.FAILED -> Icon(
                    Icons.Default.Error, null,
                    tint = IosErrorRed,
                    modifier = Modifier.size((20 * scaleFactor).dp)
                )
                ProbeStatus.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size((20 * scaleFactor).dp),
                    strokeWidth = 2.dp, color = IosAmber
                )
                ProbeStatus.SKIPPED -> Text(
                    "SKIP", color = IosSecondaryLabel,
                    fontSize = (10 * scaleFactor).sp, fontWeight = FontWeight.Bold
                )
                ProbeStatus.IDLE -> {}
            }
        }
    }
}

@Composable
private fun MtuProbeRow(result: MtuProbeResult, scaleFactor: Float) {
    val statusColor = when (result.status) {
        ProbeStatus.SUCCESS -> IosActiveGreen
        ProbeStatus.FAILED -> IosErrorRed
        else -> IosSecondaryLabel
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((14 * scaleFactor).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size((10 * scaleFactor).dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                Column {
                    Text("Path MTU Discovery", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (14 * scaleFactor).sp)
                    if (result.status == ProbeStatus.SUCCESS) {
                        Text(
                            text = "Optimal: ${result.discoveredMtu} bytes (Path: ${result.rawPathMtu})",
                            color = IosActiveGreen, fontSize = (11 * scaleFactor).sp
                        )
                    } else if (result.status == ProbeStatus.FAILED) {
                        Text("Using safe default: 1280", color = IosErrorRed, fontSize = (11 * scaleFactor).sp)
                    }
                }
            }
            when (result.status) {
                ProbeStatus.SUCCESS -> Icon(Icons.Default.CheckCircle, null, tint = IosActiveGreen, modifier = Modifier.size((20 * scaleFactor).dp))
                ProbeStatus.FAILED -> Icon(Icons.Default.Error, null, tint = IosErrorRed, modifier = Modifier.size((20 * scaleFactor).dp))
                else -> {}
            }
        }
    }
}

@Composable
private fun NoiseProbeRow(result: NoiseProbeResult, scaleFactor: Float) {
    val statusColor = if (result.status == ProbeStatus.SUCCESS && result.effective) IosActiveGreen
    else if (result.status == ProbeStatus.FAILED) IosErrorRed
    else IosSecondaryLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IosCardBg)
            .padding(horizontal = (14 * scaleFactor).dp, vertical = (10 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size((8 * scaleFactor).dp).clip(CircleShape).background(statusColor))
            Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
            Text(result.noise.displayName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = (13 * scaleFactor).sp)
        }
        if (result.effective) {
            Text("EFFECTIVE", color = IosActiveGreen, fontWeight = FontWeight.ExtraBold, fontSize = (9 * scaleFactor).sp, letterSpacing = 0.5.sp)
        } else if (result.status == ProbeStatus.SUCCESS) {
            Text("WEAK", color = IosSecondaryLabel, fontWeight = FontWeight.Medium, fontSize = (9 * scaleFactor).sp)
        }
    }
}

@Composable
private fun ScanModeProbeRow(result: ScanModeProbeResult, scaleFactor: Float) {
    val statusColor = if (result.status == ProbeStatus.SUCCESS && result.gatewayFound) IosActiveGreen else IosSecondaryLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IosCardBg)
            .padding(horizontal = (14 * scaleFactor).dp, vertical = (10 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size((8 * scaleFactor).dp).clip(CircleShape).background(statusColor))
            Spacer(modifier = Modifier.width((10 * scaleFactor).dp))
            Text(result.scanMode.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.Medium, fontSize = (13 * scaleFactor).sp)
        }
        if (result.gatewayFound) {
            Text("VERIFIED", color = IosActiveGreen, fontWeight = FontWeight.ExtraBold, fontSize = (9 * scaleFactor).sp, letterSpacing = 0.5.sp)
        }
    }
}

@Composable
private fun AutoDetectFinalResult(
    result: AutoDetectResult,
    onApplyResult: (AutoDetectResult) -> Unit,
    onRetest: () -> Unit,
    scaleFactor: Float
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = (8 * scaleFactor).dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = IosActiveGreen.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke((1.5 * scaleFactor).dp, IosActiveGreen.copy(alpha = glowAlpha))
    ) {
        Column(
            modifier = Modifier.padding((20 * scaleFactor).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size((56 * scaleFactor).dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(IosActiveGreen.copy(alpha = 0.3f), IosActiveGreen.copy(alpha = 0.05f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = IosActiveGreen, modifier = Modifier.size((28 * scaleFactor).dp))
            }

            Spacer(modifier = Modifier.height((14 * scaleFactor).dp))

            Text(
                "Optimal Configuration Found",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = (18 * scaleFactor).sp
            )

            Spacer(modifier = Modifier.height((4 * scaleFactor).dp))

            val confidencePercent = (result.confidence * 100).toInt()
            Text(
                "Confidence: $confidencePercent%",
                color = if (result.confidence > 0.7f) IosActiveGreen else IosAmber,
                fontWeight = FontWeight.Bold,
                fontSize = (13 * scaleFactor).sp
            )

            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = IosGroupBg)
            ) {
                Column(modifier = Modifier.padding((14 * scaleFactor).dp)) {
                    RecommendationRow("Protocol", result.recommendedProtocol.displayName, scaleFactor)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                    RecommendationRow("Obfuscation", result.recommendedNoise.displayName, scaleFactor)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                    RecommendationRow("Scan Mode", result.recommendedScanMode.name.lowercase().replaceFirstChar { it.uppercase() }, scaleFactor)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                    RecommendationRow("MTU", "${result.recommendedMtu} bytes", scaleFactor)
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                    RecommendationRow("Network Stack", result.recommendedIpMode.displayName, scaleFactor)
                    if (result.recommendedH2Mode) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                        RecommendationRow("HTTP/2 Fallback", "Enabled", scaleFactor)
                    }
                    if (result.recommendedEch) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                        RecommendationRow("ECH (Encrypted Client Hello)", "Enabled", scaleFactor)
                    }
                    if (result.recommendedFragment) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                        RecommendationRow("Packet Fragmentation", "Enabled", scaleFactor)
                    }
                }
            }

            Spacer(modifier = Modifier.height((20 * scaleFactor).dp))

            Button(
                onClick = { onApplyResult(result) },
                modifier = Modifier.fillMaxWidth().height((52 * scaleFactor).dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IosActiveGreen, contentColor = Color.White)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Configuration", fontWeight = FontWeight.Bold, fontSize = (15 * scaleFactor).sp)
                }
            }

            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))

            OutlinedButton(
                onClick = onRetest,
                modifier = Modifier.fillMaxWidth().height((44 * scaleFactor).dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IosSecondaryLabel),
                border = BorderStroke(1.dp, IosSecondaryLabel.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-Test Network", fontWeight = FontWeight.SemiBold, fontSize = (13 * scaleFactor).sp)
            }
        }
    }
}

@Composable
private fun RecommendationRow(label: String, value: String, scaleFactor: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = IosSecondaryLabel, fontSize = (12 * scaleFactor).sp, fontWeight = FontWeight.Medium)
        Text(value, color = Color.White, fontSize = (13 * scaleFactor).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ErrorCard(error: String?, onRetry: () -> Unit, scaleFactor: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = IosErrorRed.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding((16 * scaleFactor).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Error, contentDescription = null, tint = IosErrorRed, modifier = Modifier.size((32 * scaleFactor).dp))
            Spacer(modifier = Modifier.height((12 * scaleFactor).dp))
            Text("Detection Failed", color = IosErrorRed, fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
            Spacer(modifier = Modifier.height((8 * scaleFactor).dp))
            Text(error ?: "Unknown error occurred", color = IosSecondaryLabel, fontSize = (13 * scaleFactor).sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height((16 * scaleFactor).dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = IosErrorRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height((48 * scaleFactor).dp)
            ) {
                Text("Retry Detection", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun ManualProtocolCard(
    protocol: AetherProtocol,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    recommendedNoise: AetherNoise,
    recommendedScan: AetherScanMode,
    extraSettings: List<Pair<String, String>>,
    onApply: () -> Unit,
    scaleFactor: Float
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = IosCardBg),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding((16 * scaleFactor).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size((40 * scaleFactor).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size((22 * scaleFactor).dp))
                }
                Spacer(modifier = Modifier.width((12 * scaleFactor).dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (16 * scaleFactor).sp)
                    Text(subtitle, color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp, lineHeight = (14 * scaleFactor).sp)
                }
            }

            Spacer(modifier = Modifier.height((12 * scaleFactor).dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = IosGroupBg)
            ) {
                Column(modifier = Modifier.padding((12 * scaleFactor).dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Obfuscation", color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp)
                        Text(recommendedNoise.displayName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (11 * scaleFactor).sp)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Scan Mode", color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp)
                        Text(recommendedScan.name.lowercase().replaceFirstChar { it.uppercase() }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = (11 * scaleFactor).sp)
                    }
                    extraSettings.forEach { (label, value) ->
                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, color = IosSecondaryLabel, fontSize = (11 * scaleFactor).sp)
                            Text(value, color = iconColor, fontWeight = FontWeight.SemiBold, fontSize = (11 * scaleFactor).sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height((12 * scaleFactor).dp))

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth().height((44 * scaleFactor).dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iconColor, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply $title Settings", fontWeight = FontWeight.Bold, fontSize = (14 * scaleFactor).sp)
            }
        }
    }
}
