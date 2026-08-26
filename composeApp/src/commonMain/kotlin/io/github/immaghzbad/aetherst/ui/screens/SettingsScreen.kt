package io.github.immaghzbad.aetherst.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.immaghzbad.aetherst.platform.isDesktop
import io.github.immaghzbad.aetherst.shared.core.NetworkUtils
import io.github.immaghzbad.aetherst.shared.data.PsiphonEgressRegistry
import io.github.immaghzbad.aetherst.shared.model.AetherConfig
import io.github.immaghzbad.aetherst.shared.model.AetherIpMode
import io.github.immaghzbad.aetherst.shared.model.AetherLogLevel
import io.github.immaghzbad.aetherst.shared.model.AetherNoise
import io.github.immaghzbad.aetherst.shared.model.AetherPerfProfile
import io.github.immaghzbad.aetherst.shared.model.AetherProtocol
import io.github.immaghzbad.aetherst.shared.model.AetherScanMode
import io.github.immaghzbad.aetherst.shared.model.ConnectionMode
import io.github.immaghzbad.aetherst.shared.model.TunnelEngine
import io.github.immaghzbad.aetherst.shared.ui.components.AppDivider
import io.github.immaghzbad.aetherst.shared.ui.components.IosActionRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosConfirmationDialog
import io.github.immaghzbad.aetherst.shared.ui.components.IosGroupCard
import io.github.immaghzbad.aetherst.shared.ui.components.IosIconBadge
import io.github.immaghzbad.aetherst.shared.ui.components.IosInputField
import io.github.immaghzbad.aetherst.shared.ui.components.IosInputFieldRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosPickerRow
import io.github.immaghzbad.aetherst.shared.ui.components.IosPresetItem
import io.github.immaghzbad.aetherst.shared.ui.components.IosSwitchRow
import io.github.immaghzbad.aetherst.shared.ui.theme.AppPalette
import io.github.immaghzbad.aetherst.shared.util.CountryNames

private val IosCardBg = AppPalette.surfaceRaised
private val IosGroupBg = AppPalette.divider
private val IosSecondaryLabel = AppPalette.textSecondary
private val IosActiveBlue = AppPalette.accent
private val IosDividerColor = AppPalette.divider
private val IosActiveGreen = AppPalette.statusConnected

enum class SettingsPage(val title: String) {
    PRESETS("Configuration Profiles"),
    CONNECTION("Connection & Tunneling"),
    PROTOCOL("Protocol & Transport"),
    ZEROTRUST("Cloudflare Zero Trust"),
    NETWORK("Network Parameters"),
    SECURITY("Security & Reliability"),
    DIAGNOSTICS("Diagnostics & Core"),
    PSIPHON("Psiphon Chain"),
    SYSTEM("System & Maintenance"),
    HEV_ENGINE("HEV Engine")
}

@Composable
fun SettingsScreen(
    config: AetherConfig,
    isBatteryOptimized: Boolean,
    onUpdateConfig: (AetherConfig) -> Unit,
    onUpdateTunnelEngine: (TunnelEngine) -> Unit,
    onApplyPreset: (String) -> Unit,
    onOpenSplitTunneling: () -> Unit,
    onOpenRoutingRules: () -> Unit,
    onOpenAutoDetect: () -> Unit = {},
    onOpenSpeedTest: () -> Unit = {},
    onRequestBatteryOptimization: () -> Unit,
    onOpenVpnSettings: () -> Unit = {},
    onResetAll: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onOptimizeMtu: () -> Unit,
    isOptimizingMtu: Boolean = false,
    onShowToast: (String, Boolean) -> Unit = { _, _ -> },
    initialPage: SettingsPage? = null,
    onSubPageClosed: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    var currentPage by remember { mutableStateOf(initialPage) }

    if (currentPage != null) {
        SettingsSubPage(
            page = currentPage!!,
            config = config,
            isBatteryOptimized = isBatteryOptimized,
            onBack = {
                currentPage = null
                onSubPageClosed()
            },
            onUpdateConfig = onUpdateConfig,
            onUpdateTunnelEngine = onUpdateTunnelEngine,
            onApplyPreset = onApplyPreset,
            onOpenSplitTunneling = onOpenSplitTunneling,
            onOpenRoutingRules = onOpenRoutingRules,
            onRequestBatteryOptimization = onRequestBatteryOptimization,
            onOpenVpnSettings = onOpenVpnSettings,
            onResetAll = onResetAll,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onOptimizeMtu = onOptimizeMtu,
            isOptimizingMtu = isOptimizingMtu,
            onShowToast = onShowToast,
            bottomContentPadding = bottomContentPadding
        )
        return
    }

    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = if (isDesktop) 12.dp else 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text("AetherST Settings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 26.sp, lineHeight = 30.sp)
                Text("Configure engine protocols, obfuscation & transport", color = IosSecondaryLabel, fontSize = 12.sp)
            }
        }
        item {
            IosGroupCard {
                Column {
                    IosActionRow(icon = Icons.Default.Speed, iconBg = AppPalette.statusScanning, title = "Internet Speed Test", subtitle = "Measure download, upload, ping & jitter", onClick = onOpenSpeedTest)
                    HorizontalDivider(color = IosDividerColor, thickness = 0.5.dp)
                    IosActionRow(icon = Icons.Default.Radar, iconBg = AppPalette.accent, title = "Smart Auto-Detect", subtitle = "Detect the best protocol & settings for your network", onClick = onOpenAutoDetect)
                }
            }
        }
        item { CategoryCard(icon = Icons.Default.Tune, iconBg = AppPalette.textSecondary, title = "Configuration Profiles", subtitle = "Presets & manual tweaks", onClick = { currentPage = SettingsPage.PRESETS }) }
        item { CategoryCard(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected, title = "Connection & Tunneling", subtitle = "Mode, engine, split tunneling, routing", onClick = { currentPage = SettingsPage.CONNECTION }) }
        if (isAndroid) {
            item { CategoryCard(icon = Icons.Default.Shield, iconBg = AppPalette.accentVariant, title = "Psiphon Chain", subtitle = "Route via Psiphon for non-Iran IP", onClick = { currentPage = SettingsPage.PSIPHON }) }
        }
        item { CategoryCard(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = "Protocol & Transport", subtitle = "MASQUE, H2, ECH, obfuscation, MTU", onClick = { currentPage = SettingsPage.PROTOCOL }) }
        if (config.protocol == AetherProtocol.ZERO_TRUST) {
            item { CategoryCard(icon = Icons.Default.Business, iconBg = AppPalette.accentVariant, title = "Cloudflare Zero Trust", subtitle = "Team, gateway & authentication", onClick = { currentPage = SettingsPage.ZEROTRUST }) }
        }
        item { CategoryCard(icon = Icons.Default.Language, iconBg = IosActiveBlue, title = "Network Parameters", subtitle = "SOCKS5, HTTP, ports, DNS, peer", onClick = { currentPage = SettingsPage.NETWORK }) }
        item { CategoryCard(icon = Icons.Default.Lock, iconBg = AppPalette.statusError, title = "Security & Reliability", subtitle = "Kill switch, IPv6 leak, reconnect", onClick = { currentPage = SettingsPage.SECURITY }) }
        item { CategoryCard(icon = Icons.Default.BugReport, iconBg = AppPalette.debugCyan, title = "Diagnostics & Core", subtitle = "Logging, perf, upstream proxy", onClick = { currentPage = SettingsPage.DIAGNOSTICS }) }
        if (isAndroid) {
            item { CategoryCard(icon = Icons.Default.Memory, iconBg = AppPalette.accentVariantAlt, title = "HEV Engine", subtitle = "Log level, timeouts, session limits (Advanced)", onClick = { currentPage = SettingsPage.HEV_ENGINE }) }
        }
        item { CategoryCard(icon = Icons.Default.Settings, iconBg = AppPalette.textSecondary, title = "System & Maintenance", subtitle = "Backup, restore, reset", onClick = { currentPage = SettingsPage.SYSTEM }) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CategoryCard(icon: ImageVector, iconBg: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = IosCardBg)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IosIconBadge(icon = icon, backgroundColor = iconBg)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp); Text(subtitle, color = IosSecondaryLabel, fontSize = 12.sp) }
            Icon(Icons.Default.ChevronRight, null, tint = IosSecondaryLabel, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SettingsSubPage(page: SettingsPage, config: AetherConfig, isBatteryOptimized: Boolean, onBack: () -> Unit, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onApplyPreset: (String) -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit, onRequestBatteryOptimization: () -> Unit, onOpenVpnSettings: () -> Unit, onResetAll: () -> Unit, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onOptimizeMtu: () -> Unit, isOptimizingMtu: Boolean, onShowToast: (String, Boolean) -> Unit, bottomContentPadding: Dp) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showAdvancedZt by remember { mutableStateOf(false) }
    val isAndroid = remember { try { Class.forName("android.os.Build"); true } catch(_: Throwable) { false } }
    val focusManager = LocalFocusManager.current

    io.github.immaghzbad.aetherst.shared.ui.components.PlatformBackHandler(enabled = true, onBack = onBack)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { focusManager.clearFocus() }) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = if (isDesktop) 12.dp else 36.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            Text(page.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomContentPadding + 12.dp, top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                SettingsPage.PRESETS -> item { PresetPage(config, onApplyPreset, onShowToast) }
                SettingsPage.CONNECTION -> item { ConnectionPage(config, isAndroid, onUpdateConfig, onUpdateTunnelEngine, onOpenSplitTunneling, onOpenRoutingRules) }
                SettingsPage.PROTOCOL -> item { ProtocolPage(config, onUpdateConfig, onOptimizeMtu, isOptimizingMtu) }
                SettingsPage.ZEROTRUST -> item { ZeroTrustPage(config, showAdvancedZt, onUpdateConfig) { showAdvancedZt = it } }
                SettingsPage.NETWORK -> item { NetworkPage(config, isAndroid, onUpdateConfig) }
                SettingsPage.SECURITY -> item { SecurityPage(config, isAndroid, isBatteryOptimized, onUpdateConfig, onRequestBatteryOptimization) }
                SettingsPage.DIAGNOSTICS -> item { DiagnosticsPage(config, onUpdateConfig) }
                SettingsPage.PSIPHON -> if (isAndroid) item { PsiphonPage(config, onUpdateConfig) }
                SettingsPage.HEV_ENGINE -> item { HevEnginePage(config, onUpdateConfig) }
                SettingsPage.SYSTEM -> item { SystemPage(isAndroid, onExportBackup, onImportBackup, onOpenVpnSettings) { showResetDialog = true } }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
    if (showResetDialog) IosConfirmationDialog(title = "Reset All Settings?", message = "This will restore all protocols, engine tweaks, and security settings to their factory defaults. This action cannot be undone.", confirmText = "Reset Everything", confirmColor = AppPalette.statusError, onConfirm = { onResetAll(); showResetDialog = false; onShowToast("System restored to defaults", false) }, onDismiss = { showResetDialog = false })
}

@Composable private fun PresetPage(config: AetherConfig, onApplyPreset: (String) -> Unit, onShowToast: (String, Boolean) -> Unit) {
    IosGroupCard { Column {
        IosPresetItem(icon = Icons.Default.Tune, iconBg = AppPalette.textSecondary, title = "Custom Manual Tweaks", subtitle = "Your own independent configuration", isActive = config.presetId == "custom", onClick = { onApplyPreset("custom"); onShowToast("Applied manual configuration", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Lock, iconBg = AppPalette.accentVariant, title = "Bypass UDP / TLS", subtitle = "MASQUE + H2 Fallback + Fragmentation", isActive = config.presetId == "bypass_udp", onClick = { onApplyPreset("bypass_udp"); onShowToast("Applied UDP/TLS Bypass preset", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Shield, iconBg = IosActiveBlue, title = "Ironclad Stealth", subtitle = "MASQUE + GFW Noise + Ironclad Probe", isActive = config.presetId == "ironclad_stealth", onClick = { onApplyPreset("ironclad_stealth"); onShowToast("Applied Ironclad Stealth preset", false) })
        AppDivider(); IosPresetItem(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, title = "Turbo Speed", subtitle = "WireGuard + Balanced Noise + Turbo Scan", isActive = config.presetId == "turbo_wg", onClick = { onApplyPreset("turbo_wg"); onShowToast("Applied Turbo Speed preset", false) })
    } }
}

@Composable private fun ConnectionPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onUpdateTunnelEngine: (TunnelEngine) -> Unit, onOpenSplitTunneling: () -> Unit, onOpenRoutingRules: () -> Unit) {
    IosGroupCard {
        IosPickerRow(
            icon = Icons.Default.TouchApp,
            iconBg = IosActiveBlue,
            title = "Connect Button Style",
            value = if (config.connectButtonStyle == "capsule") "Tap (Capsule)" else "Swipe",
            options = listOf("Swipe", "Tap (Capsule)"),
            onOptionSelected = { idx -> onUpdateConfig(config.copy(connectButtonStyle = if (idx == 0) "swipe" else "capsule")) }
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    IosGroupCard { Column {
        val opts = if (isAndroid) listOf("Tunnel", "Proxy Only") else if (isDesktop) listOf("TUN Mode (Global)", "System Proxy", "Proxy Only") else listOf("TUN Mode (Global)", "System Proxy", "Proxy Only")
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected,             title = "Connection Mode", value = when (config.connectionMode) { ConnectionMode.TUNNEL -> if (isAndroid) "Tunnel" else "TUN Mode (Global)"; ConnectionMode.SYSTEM_PROXY -> "System Proxy"; else -> "Proxy Only" }, options = opts, onOptionSelected = { val m = if (isAndroid) { if (it == 0) ConnectionMode.TUNNEL else ConnectionMode.PROXY_ONLY } else if (isDesktop) { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } } else { when (it) { 0 -> ConnectionMode.TUNNEL; 1 -> ConnectionMode.SYSTEM_PROXY; else -> ConnectionMode.PROXY_ONLY } }; onUpdateConfig(config.copy(connectionMode = m)) })
        if (config.connectionMode == ConnectionMode.TUNNEL) {
            AppDivider(); IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.accentVariant, title = "Tunnel Engine", value = config.tunnelEngine.displayName, options = TunnelEngine.entries.map { it.displayName }, onOptionSelected = { onUpdateTunnelEngine(TunnelEngine.entries[it]) })
            if (!isDesktop) {
                AppDivider(); IosSwitchRow(icon = Icons.Default.AllInclusive, iconBg = IosActiveBlue, title = "Tunnel Whole Device", subtitle = "Route all application traffic through VPN", checked = config.tunnelAllApps, onCheckedChange = { onUpdateConfig(config.copy(tunnelAllApps = it)) }, testTag = "switch_tunnel_all"); AppDivider(); IosPickerRow(icon = Icons.Default.Tune, iconBg = AppPalette.accentVariant, title = "Split Tunneling", value = if (config.tunnelAllApps) "All Apps Tunneled" else "${config.tunneledPackages.size + config.blockedPackages.size} Apps • Default Bypass", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenSplitTunneling, enabled = !config.tunnelAllApps)
            }
            AppDivider()
        }
        IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = IosActiveBlue, title = "Domain & IP Routing", value = "${config.routingRules.size} Rules", options = emptyList(), onOptionSelected = {}, onClickOverride = onOpenRoutingRules)
        if (isAndroid) { AppDivider(); IosSwitchRow(icon = Icons.Default.Share, iconBg = AppPalette.accentVariantAlt, title = "Share via Hotspot", subtitle = "Allow other devices to connect to proxy", checked = config.shareHotspot, onCheckedChange = { onUpdateConfig(config.copy(shareHotspot = it)) }, testTag = "switch_share_hotspot"); if (config.shareHotspot) HotspotInfo(config) }
    } }
}

@Composable private fun HotspotInfo(config: AetherConfig) {
    Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp)) {
        var localIp by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) { localIp = NetworkUtils.getLocalIpAddress() }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { Icon(if (localIp != null) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (localIp != null) IosActiveGreen else AppPalette.statusScanning, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(if (localIp != null) "Hotspot Active" else "Hotspot Inactive", color = if (localIp != null) IosActiveGreen else AppPalette.statusScanning, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            IconButton(onClick = { localIp = NetworkUtils.getLocalIpAddress() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, null, tint = IosActiveBlue, modifier = Modifier.size(18.dp)) }
        }
        if (localIp != null) { Spacer(modifier = Modifier.height(10.dp)); Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), color = Color.Black.copy(alpha = 0.3f)) { Column(modifier = Modifier.padding(12.dp)) { Text("PROXY ADDRESS", color = IosSecondaryLabel, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp); Spacer(modifier = Modifier.height(6.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("$localIp:${config.socksPort}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) } } } }
    }
}

@Composable private fun ProtocolPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit, onOptimizeMtu: () -> Unit, isOptimizingMtu: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IosGroupCard { Column {
            IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosActiveBlue, title = "Transport Protocol", value = config.protocol.displayName, options = AetherProtocol.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(protocol = AetherProtocol.entries[it])) })
            if (config.protocol == AetherProtocol.MASQUE) { AppDivider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "HTTP/2 Fallback Mode", subtitle = "Force MASQUE over TCP/TLS instead of QUIC", checked = config.h2Mode, onCheckedChange = { onUpdateConfig(config.copy(h2Mode = it)) }, testTag = "switch_h2_mode"); AppDivider(); IosSwitchRow(icon = Icons.Default.VerticalSplit, iconBg = AppPalette.accentVariant, title = "Packet Fragmentation", subtitle = "Bypass SNI filters (H2 mode only)", checked = config.h2Fragment, onCheckedChange = { onUpdateConfig(config.copy(h2Fragment = it)) }, testTag = "switch_fragment"); if (config.h2Fragment) { IosInputFieldRow(icon = Icons.Default.Straighten, iconBg = IosSecondaryLabel, label = "Fragment Size (Bytes)", value = config.fragmentSize, onValueChange = { onUpdateConfig(config.copy(fragmentSize = it)) }, placeholder = "16-32", testTag = "fragment_size_input"); AppDivider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "Fragment Delay (ms)", value = config.fragmentDelay, onValueChange = { onUpdateConfig(config.copy(fragmentDelay = it)) }, placeholder = "2-10", testTag = "fragment_delay_input"); AppDivider() }; IosSwitchRow(icon = Icons.Default.EnhancedEncryption, iconBg = IosActiveGreen, title = "Encrypted Client Hello (ECH)", subtitle = "Hide SNI from network observers", checked = config.echEnabled, onCheckedChange = { onUpdateConfig(config.copy(echEnabled = it)) }, testTag = "switch_ech_enabled"); AppDivider() }
            IosSwitchRow(icon = Icons.Default.DataUsage, iconBg = AppPalette.statusScanning, title = "Disable Data Verification", subtitle = "Skip waiting for initial packet exchange", checked = config.noDataCheck, onCheckedChange = { onUpdateConfig(config.copy(noDataCheck = it)) }, testTag = "switch_no_data_check"); AppDivider()
            val availNoise = if (config.protocol == AetherProtocol.MASQUE) listOf(AetherNoise.FIREWALL, AetherNoise.GFW, AetherNoise.OFF) else listOf(AetherNoise.BALANCED, AetherNoise.AGGRESSIVE, AetherNoise.LIGHT, AetherNoise.OFF)
            IosPickerRow(icon = Icons.Default.Tune, iconBg = AppPalette.accentVariantAlt, title = "Bypass Obfuscation", value = config.noise.displayName.substringBefore(" ("), options = availNoise.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(noise = availNoise[it])) }); AppDivider()
            IosPickerRow(icon = Icons.Default.NetworkCheck, iconBg = AppPalette.statusScanning, title = "Speed Strategy", value = config.scanMode.name.lowercase().replaceFirstChar { it.uppercase() }, options = AetherScanMode.entries.map { "${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }} (${it.description})" }, onOptionSelected = { onUpdateConfig(config.copy(scanMode = AetherScanMode.entries[it])) }); AppDivider()
            IosPickerRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariant, title = "Network Stack", value = config.ipMode.rawValue, options = AetherIpMode.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(ipMode = AetherIpMode.entries[it])) }); AppDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.Bottom) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Tune, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "Custom MTU Size", value = config.mtu.toString(), onValueChange = { onUpdateConfig(config.copy(mtu = it.toIntOrNull() ?: 1100)) }, modifier = Modifier.weight(1f), placeholder = "1100", keyboardType = KeyboardType.Number, testTag = "mtu_input") }; Spacer(modifier = Modifier.width(8.dp)); Button(onClick = onOptimizeMtu, enabled = !isOptimizingMtu, modifier = Modifier.height(46.dp), shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = IosActiveBlue.copy(alpha = 0.15f), contentColor = IosActiveBlue, disabledContainerColor = IosActiveBlue.copy(alpha = 0.05f), disabledContentColor = IosActiveBlue.copy(alpha = 0.3f)), contentPadding = PaddingValues(horizontal = 16.dp)) { if (isOptimizingMtu) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = IosActiveBlue, strokeWidth = 2.dp) else Text("Optimize", fontSize = 13.sp, fontWeight = FontWeight.Bold) } }
        } }
        Text("CLOAK OBFUSCATION (Native - MASQUE H2 Only)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = IosSecondaryLabel, fontSize = 11.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(start = 8.dp, top = 4.dp))
        IosGroupCard { Column {
            IosSwitchRow(icon = Icons.Default.Security, iconBg = AppPalette.accentVariant, title = "Cloak Decoy (Native)", subtitle = if (config.cloakEnabled) "Enabled - TTL decoy for DPI bypass" else "Disabled - Native JNI libcloak.so", checked = config.cloakEnabled, onCheckedChange = { onUpdateConfig(config.copy(cloakEnabled = it)) }, testTag = "switch_cloak_enabled")
            if (config.cloakEnabled) {
                AppDivider()
                if (config.protocol != AetherProtocol.MASQUE || !config.h2Mode) {
                    Row(modifier = Modifier.fillMaxWidth().background(AppPalette.statusScanning.copy(alpha = 0.12f)).padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = AppPalette.statusScanning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloak works only with MASQUE + H2 Fallback. Current: ${config.protocol.displayName} ${if (config.h2Mode) "H2" else "H3"}", color = AppPalette.statusScanning, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                    AppDivider()
                }
                IosInputFieldRow(icon = Icons.Default.Public, iconBg = IosActiveGreen, label = "Decoy SNI List", value = config.cloakSniList, onValueChange = { onUpdateConfig(config.copy(cloakSniList = it)) }, placeholder = "www.bing.com,www.hcaptcha.com", testTag = "cloak_sni_input"); AppDivider()
                IosInputFieldRow(icon = Icons.Default.Timer, iconBg = AppPalette.statusScanning, label = "TTL List", value = config.cloakTtlList, onValueChange = { onUpdateConfig(config.copy(cloakTtlList = it)) }, placeholder = "4,5,6,8", testTag = "cloak_ttl_input"); AppDivider()
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Timer, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "Jitter Min (ms)", value = config.cloakJitterMin.toString(), onValueChange = { onUpdateConfig(config.copy(cloakJitterMin = it.toIntOrNull() ?: 20)) }, placeholder = "20", keyboardType = KeyboardType.Number, testTag = "cloak_jitter_min") }
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosInputField(label = "Jitter Max (ms)", value = config.cloakJitterMax.toString(), onValueChange = { onUpdateConfig(config.copy(cloakJitterMax = it.toIntOrNull() ?: 80)) }, placeholder = "80", keyboardType = KeyboardType.Number, testTag = "cloak_jitter_max") }
                }; AppDivider()
                IosSwitchRow(icon = Icons.Default.VerticalSplit, iconBg = AppPalette.accentVariant, title = "Fragment Real Hello", subtitle = "Split SNI across packets (PattNG style)", checked = config.cloakFragment, onCheckedChange = { onUpdateConfig(config.copy(cloakFragment = it)) }, testTag = "switch_cloak_fragment"); AppDivider()
                IosSwitchRow(icon = Icons.Default.Refresh, iconBg = IosActiveGreen, title = "Adaptive Stats", subtitle = "Remember best TTL/server via cloak.stats", checked = config.cloakAdaptive, onCheckedChange = { onUpdateConfig(config.copy(cloakAdaptive = it)) }, testTag = "switch_cloak_adaptive"); AppDivider()
                IosSwitchRow(icon = Icons.Default.FontDownload, iconBg = IosSecondaryLabel, title = "Randomize SNI Case", subtitle = "wWw.BiNg.CoM to evade exact match", checked = config.cloakRandomizeSniCase, onCheckedChange = { onUpdateConfig(config.copy(cloakRandomizeSniCase = it)) }, testTag = "switch_cloak_randomize"); AppDivider()
                IosInputFieldRow(icon = Icons.Default.Dns, iconBg = IosActiveBlue, label = "Fallback Ports", value = config.cloakFallbackPorts, onValueChange = { onUpdateConfig(config.copy(cloakFallbackPorts = it)) }, placeholder = "443,2053,2083,2087,2096,8443", testTag = "cloak_fallback_input"); AppDivider()
                IosPickerRow(icon = Icons.Default.BugReport, iconBg = IosSecondaryLabel, title = "Cloak Log Level", value = config.cloakLogLevel, options = listOf("error", "warn", "info", "debug"), onOptionSelected = { idx -> val lvl = listOf("error", "warn", "info", "debug")[idx]; onUpdateConfig(config.copy(cloakLogLevel = lvl)) })
            }
        } }
        IosGroupCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("About Cloak", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cloak is a native decoy system for MASQUE over H2 (TCP/TLS). Before the real TLS handshake it sends a few low-TTL decoy ClientHellos with common SNIs — for example www.bing.com — that expire in transit and are seen by DPI, followed by the real handshake with its actual SNI. This hides the real SNI from SNI-based filters. It works only with MASQUE + H2 Fallback over TCP and is useful when direct MASQUE fails due to SNI inspection while the underlying network path is still reachable. When direct MASQUE already connects, Cloak adds only extra packets and jitter with no speed benefit. Logs appear on the Logs screen with tag CloakCore.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable private fun ZeroTrustPage(config: AetherConfig, showAdvanced: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onToggleAdvanced: (Boolean) -> Unit) {
    val isZt = config.protocol == AetherProtocol.ZERO_TRUST
    val ztError = if (isZt) config.zeroTrustError() else null
    val hasAuth = config.teamName.isNotBlank() &&
        (config.accessEmail.isNotBlank() || config.accessId.isNotBlank() || config.accessSecret.isNotBlank() || config.accessToken.isNotBlank())
    IosGroupCard { Column {
        IosInputFieldRow(icon = Icons.Default.Business, iconBg = AppPalette.accentVariant, label = if (isZt) "Organization Team Name *" else "Organization Team Name", value = config.teamName, onValueChange = { onUpdateConfig(config.copy(teamName = it)) }, placeholder = "e.g. my-org", testTag = "zt_team_input"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Language, iconBg = IosActiveBlue, label = if (isZt) "Cloudflare Access Email" else "Cloudflare Access Email", value = config.accessEmail, onValueChange = { onUpdateConfig(config.copy(accessEmail = it)) }, placeholder = "user@example.com", testTag = "zt_email_input"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Shield, iconBg = IosActiveGreen, title = "Gateway Filtering Proxy", subtitle = "Route via org Gateway for filtering & logs (off by default)", checked = config.useGateway, onCheckedChange = { onUpdateConfig(config.copy(useGateway = it)) }, testTag = "switch_zt_gateway"); AppDivider()
        IosSwitchRow(icon = Icons.Default.CheckCircle, iconBg = IosActiveBlue, title = "Stay Signed In", subtitle = "Keep this device enrolled (reuse token until it expires)", checked = config.ztStaySignedIn, onCheckedChange = { onUpdateConfig(config.copy(ztStaySignedIn = it)) }, testTag = "switch_zt_stay_signed_in"); AppDivider()
        if (hasAuth) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onUpdateConfig(config.copy(teamName = "", accessEmail = "", accessId = "", accessSecret = "", accessToken = "", ztTokenExpiry = 0, ztStaySignedIn = false)) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.AutoMirrored.Filled.Logout, backgroundColor = AppPalette.statusError); Spacer(modifier = Modifier.width(12.dp)); Text("Sign Out", fontWeight = FontWeight.Medium, color = AppPalette.statusError, fontSize = 15.sp) } }
        }
        Row(modifier = Modifier.fillMaxWidth().clickable { onToggleAdvanced(!showAdvanced) }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Lock, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); Text("Advanced Authentication", fontWeight = FontWeight.Medium, color = Color.White, fontSize = 15.sp) }; Icon(if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = IosSecondaryLabel, modifier = Modifier.size(18.dp)) }
        AnimatedVisibility(visible = showAdvanced, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column(modifier = Modifier.fillMaxWidth().background(IosGroupBg.copy(alpha = 0.4f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Choose exactly one sign-in method.", color = IosSecondaryLabel, fontSize = 12.sp); IosInputField(label = if (isZt) "Access Client ID" else "Access Client ID", value = config.accessId, onValueChange = { onUpdateConfig(config.copy(accessId = it)) }, placeholder = "Required for Service Tokens", testTag = "zt_access_id"); IosInputField(label = if (isZt) "Access Client Secret" else "Access Client Secret", value = config.accessSecret, onValueChange = { onUpdateConfig(config.copy(accessSecret = it)) }, placeholder = "Required for Service Tokens", testTag = "zt_access_secret"); IosInputField(label = if (isZt) "Manual JWT Access Token" else "Manual JWT Access Token", value = config.accessToken, onValueChange = { onUpdateConfig(config.copy(accessToken = it, ztTokenExpiry = config.parseJwtExpiry(it))) }, placeholder = "Existing token you already hold", testTag = "zt_access_token") } }
    } }
    if (ztError != null) {
        Text(
            text = ztError,
            color = AppPalette.statusError,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
        )
    }
}

@Composable private fun NetworkPage(config: AetherConfig, isAndroid: Boolean, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Language, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "SOCKS5 Host", value = config.socksHost, onValueChange = { onUpdateConfig(config.copy(socksHost = it)) }, modifier = Modifier.weight(1f), placeholder = "127.0.0.1", testTag = "socks_host_input"); Spacer(modifier = Modifier.width(10.dp)); IosInputField(label = "SOCKS Port", value = config.socksPort, onValueChange = { onUpdateConfig(config.copy(socksPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1819", keyboardType = KeyboardType.Number, testTag = "socks_port_input"); Spacer(modifier = Modifier.width(8.dp)); IosInputField(label = "HTTP Port", value = config.httpPort, onValueChange = { onUpdateConfig(config.copy(httpPort = it)) }, modifier = Modifier.width(75.dp), placeholder = "1820", keyboardType = KeyboardType.Number, testTag = "http_port_input") }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Http, iconBg = IosActiveBlue, title = "Internal HTTP Proxy", subtitle = "Expose an HTTP CONNECT proxy alongside SOCKS5", checked = config.httpProxyEnabled, onCheckedChange = { onUpdateConfig(config.copy(httpProxyEnabled = it)) }, testTag = "switch_http_proxy_enabled"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Code, iconBg = IosSecondaryLabel, label = "TLS Key Groups", value = config.tlsGroups, onValueChange = { onUpdateConfig(config.copy(tlsGroups = it)) }, placeholder = "P-256:X25519:P-384", testTag = "tls_groups_input"); AppDivider()
        if (isAndroid) { IosInputFieldRow(icon = Icons.Default.Dns, iconBg = IosActiveBlue, label = "Tunnel DNS Servers", value = config.dnsList, onValueChange = { onUpdateConfig(config.copy(dnsList = it.replace(Regex("\\s*,\\s*"), ","))) }, placeholder = "1.1.1.1,1.0.0.1", testTag = "dns_list_input"); AppDivider() }
        IosInputFieldRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariant, label = "Forced Peer IP", value = config.peer, onValueChange = { onUpdateConfig(config.copy(peer = it)) }, placeholder = "e.g. 1.2.3.4:443", testTag = "peer_input"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, title = "Keepalive Packets", subtitle = if (config.keepaliveEnabled) "Maintain NAT binding with periodic pings" else "Disabled — no keepalive (battery saver)", checked = config.keepaliveEnabled, onCheckedChange = { onUpdateConfig(config.copy(keepaliveEnabled = it)) }, testTag = "switch_keepalive_enabled"); AppDivider()
        AnimatedVisibility(visible = config.keepaliveEnabled, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) { Column { IosInputFieldRow(icon = Icons.Default.Bolt, iconBg = AppPalette.statusScanning, label = "Keepalive Interval (Secs)", value = config.keepalive.toString(), onValueChange = { onUpdateConfig(config.copy(keepalive = it.toIntOrNull() ?: 5)) }, placeholder = "5", keyboardType = KeyboardType.Number, testTag = "keepalive_input"); AppDivider() } }
        IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "Validation Interval (Secs)", value = config.validateSecs.toString(), onValueChange = { onUpdateConfig(config.copy(validateSecs = it.toIntOrNull() ?: 10)) }, placeholder = "10", keyboardType = KeyboardType.Number, testTag = "validate_secs_input")
    } }
}

@Composable private fun SecurityPage(config: AetherConfig, isAndroid: Boolean, isBatteryOptimized: Boolean, onUpdateConfig: (AetherConfig) -> Unit, onRequestBatteryOptimization: () -> Unit) {
    IosGroupCard { Column {
        IosSwitchRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.accentVariant, title = "Strict Kill Switch", subtitle = "Prevent any leak even during manual stop", checked = config.strictKillSwitch, onCheckedChange = { onUpdateConfig(config.copy(strictKillSwitch = it)) }, testTag = "switch_strict_kill_switch"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Lock, iconBg = AppPalette.statusError, title = "Kill Switch", subtitle = "Block traffic when VPN is disconnected", checked = config.killSwitch, onCheckedChange = { onUpdateConfig(config.copy(killSwitch = it)) }, testTag = "switch_kill_switch"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Security, iconBg = AppPalette.accentVariant, title = "IPv6 Leak Protection", subtitle = "Force all IPv6 traffic through tunnel", checked = config.ipv6Leak, onCheckedChange = { onUpdateConfig(config.copy(ipv6Leak = it)) }, testTag = "switch_ipv6_leak"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "Smart Reconnect", subtitle = "Attempt auto-recovery on network failure", checked = config.smartReconnect, onCheckedChange = { onUpdateConfig(config.copy(smartReconnect = it)) }, testTag = "switch_smart_reconnect")
        if (config.smartReconnect) { AppDivider(); Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosIconBadge(icon = Icons.Default.Repeat, backgroundColor = IosSecondaryLabel); Spacer(modifier = Modifier.width(12.dp)); IosInputField(label = "Max Retries", value = config.reconnectRetryLimit.toString(), onValueChange = { onUpdateConfig(config.copy(reconnectRetryLimit = it.toIntOrNull() ?: 10)) }, placeholder = "10", keyboardType = KeyboardType.Number, testTag = "reconnect_limit_input") }; Spacer(modifier = Modifier.width(12.dp)); Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) { IosInputField(label = "Delay (Secs)", value = config.reconnectSecs.toString(), onValueChange = { onUpdateConfig(config.copy(reconnectSecs = it.toIntOrNull() ?: 2)) }, placeholder = "2", keyboardType = KeyboardType.Number, testTag = "reconnect_secs_input") } } }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Sync, iconBg = IosActiveGreen, title = "Cloudflare Reprovision", subtitle = "Auto-register fresh device on identity loss", checked = config.reprovision, onCheckedChange = { onUpdateConfig(config.copy(reprovision = it)) }, testTag = "switch_reprovision")
        if (isAndroid) { AppDivider(); IosSwitchRow(icon = Icons.Default.BatteryAlert, iconBg = AppPalette.statusError, title = "Battery Optimization", subtitle = "Allow AetherST to run without restrictions", checked = isBatteryOptimized, enabled = !isBatteryOptimized, onCheckedChange = { if (it) onRequestBatteryOptimization() }, testTag = "switch_battery_opt") }
    } }
}

private data class ParsedUpstreamProxy(val scheme: String, val host: String, val port: String, val user: String, val pass: String)

private fun parseUpstreamProxy(raw: String): ParsedUpstreamProxy {
    if (raw.isBlank()) return ParsedUpstreamProxy("socks5", "127.0.0.1", "", "", "")
    val scheme = if (raw.startsWith("http://", ignoreCase = true)) "http" else "socks5"
    val body = raw.removePrefix("socks5://").removePrefix("http://")
    val authHost = if (body.contains("@")) body.substringBefore("@") to body.substringAfter("@") else "" to body
    val (userRaw, passRaw) = if (authHost.first.contains(":")) authHost.first.substringBefore(":") to authHost.first.substringAfter(":") else authHost.first to ""
    val hostPort = authHost.second
    val (host, port) = if (hostPort.startsWith("[")) {
        val end = hostPort.indexOf("]")
        hostPort.substring(0, end + 1) to hostPort.substringAfter("]:").substringBefore("/")
    } else {
        val i = hostPort.lastIndexOf(":")
        if (i < 0) hostPort to "" else hostPort.substring(0, i) to hostPort.substring(i + 1)
    }
    return ParsedUpstreamProxy(scheme, host, port, decodeUpstreamCredential(userRaw), decodeUpstreamCredential(passRaw))
}

private fun buildUpstreamProxy(p: ParsedUpstreamProxy): String {
    if (p.host.isBlank() || p.port.isBlank()) return ""
    val auth = if (p.user.isBlank()) "" else "${encodeUpstreamCredential(p.user)}:${encodeUpstreamCredential(p.pass)}@"
    return "${p.scheme}://$auth${p.host}:${p.port}"
}

private fun decodeUpstreamCredential(s: String): String = s.replace("%40", "@").replace("%3A", ":")
private fun encodeUpstreamCredential(s: String): String = s.replace("@", "%40").replace(":", "%3A")

@Composable private fun DiagnosticsPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    IosGroupCard { Column {
        IosPickerRow(icon = Icons.Default.BugReport, iconBg = AppPalette.debugCyan, title = "App System Logging", value = config.appLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(appLogLevel = AetherLogLevel.entries[it])) }); AppDivider()
        IosPickerRow(icon = Icons.Default.VpnLock, iconBg = IosSecondaryLabel, title = "Aether Core Logging", value = config.coreLogLevel.displayName.substringBefore(" ("), options = AetherLogLevel.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(coreLogLevel = AetherLogLevel.entries[it])) }); AppDivider()
        IosPickerRow(icon = Icons.Default.Speed, iconBg = IosActiveGreen, title = "Core Performance Profile", value = config.perfProfile.displayName, options = AetherPerfProfile.entries.map { it.displayName }, onOptionSelected = { onUpdateConfig(config.copy(perfProfile = AetherPerfProfile.entries[it])) }); AppDivider()
        Column {
            IosSwitchRow(icon = Icons.AutoMirrored.Filled.AltRoute, iconBg = AppPalette.accentVariantAlt, title = "Chain External Proxy", subtitle = "Route through a local SOCKS/HTTP proxy (another app)", checked = config.upstreamProxyEnabled, onCheckedChange = { onUpdateConfig(config.copy(upstreamProxyEnabled = it, upstreamProxy = if (it) config.upstreamProxy.ifBlank { "socks5://127.0.0.1:1080" } else "")) }, testTag = "switch_upstream_proxy")
            if (config.upstreamProxyEnabled) {
                AppDivider()
                val up = remember(config.upstreamProxy) { parseUpstreamProxy(config.upstreamProxy) }
                val updateUpstream: (ParsedUpstreamProxy) -> Unit = { onUpdateConfig(config.copy(upstreamProxy = buildUpstreamProxy(it))) }
                IosPickerRow(icon = Icons.Default.Shuffle, iconBg = AppPalette.accentVariantAlt, title = "Proxy Type", value = up.scheme.uppercase(), options = listOf("SOCKS5", "HTTP"), onOptionSelected = { idx -> updateUpstream(up.copy(scheme = if (idx == 0) "socks5" else "http")) })
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Dns, iconBg = AppPalette.textSecondary, label = "Host", value = up.host, onValueChange = { updateUpstream(up.copy(host = it)) }, placeholder = "127.0.0.1", testTag = "upstream_proxy_host")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Numbers, iconBg = AppPalette.textSecondary, label = "Port", value = up.port, onValueChange = { updateUpstream(up.copy(port = it.filter { c -> c.isDigit() }.take(5))) }, placeholder = "1080", keyboardType = KeyboardType.Number, testTag = "upstream_proxy_port")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Person, iconBg = AppPalette.textSecondary, label = "Username (optional)", value = up.user, onValueChange = { updateUpstream(up.copy(user = it)) }, placeholder = "user", testTag = "upstream_proxy_user")
                AppDivider()
                IosInputFieldRow(icon = Icons.Default.Lock, iconBg = AppPalette.textSecondary, label = "Password (optional)", value = up.pass, onValueChange = { updateUpstream(up.copy(pass = it)) }, placeholder = "password", testTag = "upstream_proxy_pass")
            }
        }; AppDivider()
        IosSwitchRow(icon = Icons.AutoMirrored.Filled.Rule, iconBg = IosActiveBlue, title = "Domain Sniffing", subtitle = "Sniff SNI/Host for domain routing rules", checked = config.routeSniffing, onCheckedChange = { onUpdateConfig(config.copy(routeSniffing = it)) }, testTag = "switch_route_sniffing")
        if (config.routeSniffing) { AppDivider(); IosInputFieldRow(icon = Icons.Default.Timer, iconBg = IosSecondaryLabel, label = "Sniffing Timeout (ms)", value = config.sniffingTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(sniffingTimeoutMs = it.toIntOrNull() ?: 100)) }, placeholder = "100", keyboardType = KeyboardType.Number, testTag = "sniffing_timeout_input") }
        AppDivider(); IosSwitchRow(icon = Icons.Default.Restore, iconBg = IosActiveGreen, title = "Quick Reconnect Strategy", subtitle = "Optimize session recovery timing", checked = config.quickReconnect, onCheckedChange = { onUpdateConfig(config.copy(quickReconnect = it)) }, testTag = "switch_quick_reconnect"); AppDivider()
        IosSwitchRow(icon = Icons.Default.Block, iconBg = AppPalette.statusError, title = "Strict Profile Lock", subtitle = "Disable fallback to other profiles", checked = config.noProfileRetry, onCheckedChange = { onUpdateConfig(config.copy(noProfileRetry = it)) }, testTag = "switch_no_profile_retry")
    } }
}

@Composable private fun PsiphonPage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IosGroupCard { Column {
            IosSwitchRow(icon = Icons.Default.Shield, iconBg = AppPalette.accentVariant, title = "Psiphon Chain", subtitle = if (config.psiphonEnabled) "Enabled - route via Psiphon for non-Iran IP" else "Disabled", checked = config.psiphonEnabled, onCheckedChange = { onUpdateConfig(config.copy(psiphonEnabled = it, psiphonChainOuter = "masque")) }, testTag = "switch_psiphon_enabled")
            if (config.psiphonEnabled) {
                AppDivider()
                IosPickerRow(icon = Icons.Default.VpnLock, iconBg = AppPalette.statusConnected, title = "Chain Outer Protocol", value = "masque", options = listOf("masque"), onOptionSelected = { onUpdateConfig(config.copy(psiphonChainOuter = "masque")) })
                AppDivider()
                val availableRegions by PsiphonEgressRegistry.availableRegions.collectAsStateWithLifecycle()
                val selectedRegion = config.psiphonEgressRegion.trim().uppercase()
                val regionCodes = buildList {
                    add("")
                    addAll(availableRegions)
                    if (selectedRegion.isNotEmpty() && selectedRegion !in availableRegions) add(selectedRegion)
                }
                val regionOptions = regionCodes.map { CountryNames.label(it) }
                IosPickerRow(icon = Icons.Default.Public, iconBg = Color(0xFF30B0C7), title = "Exit Location", value = CountryNames.label(selectedRegion), options = regionOptions, onOptionSelected = { idx -> onUpdateConfig(config.copy(psiphonEgressRegion = regionCodes[idx])) })
            }
        } }
        IosGroupCard {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("About Psiphon Chain", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Psiphon is chained as inner tunnel over the selected outer protocol (for example masque). Outer handles censorship bypass, inner provides non-Iran exit. Enable the switch on Dashboard or here, select outer protocol, then connect. Works only on Android. When disabled, connection uses single aether core.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable private fun HevEnginePage(config: AetherConfig, onUpdateConfig: (AetherConfig) -> Unit) {
    val hevLevels = listOf("error", "warn", "info", "debug")
    val levelLabels = mapOf("error" to "Error", "warn" to "Warn (Default)", "info" to "Info", "debug" to "Debug (Verbose)")
    val currentLevel = if (config.hevLogLevel in hevLevels) config.hevLogLevel else "warn"
    val hevUdpOptions = listOf("udp", "icmp", "off")
    val hevUdpLabels = mapOf(
        "udp" to "UDP (ASSOCIATE)",
        "icmp" to "UDP (ICMP-in-TCP)",
        "off" to "Disabled"
    )
    val hevUdpMode = if (config.hevUdpMode in hevUdpOptions) config.hevUdpMode else "udp"

    IosGroupCard { Column {
        IosPickerRow(
            icon = Icons.Default.BugReport,
            iconBg = AppPalette.accentVariantAlt,
            title = "HEV Log Level",
            value = levelLabels[currentLevel] ?: "Warn (Default)",
            options = hevLevels.map { levelLabels[it]!! },
            onOptionSelected = { index -> onUpdateConfig(config.copy(hevLogLevel = hevLevels[index])) }
        ); AppDivider()
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.Timer, backgroundColor = IosActiveBlue); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "Connect Timeout (ms)", value = config.hevConnectTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevConnectTimeoutMs = it.toIntOrNull()?.coerceIn(500, 120000) ?: 5000)) }, modifier = Modifier.weight(1f), placeholder = "5000", keyboardType = KeyboardType.Number, testTag = "hev_connect_timeout_input")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IosIconBadge(icon = Icons.Default.SwapHoriz, backgroundColor = IosActiveGreen); Spacer(modifier = Modifier.width(12.dp))
                IosInputField(label = "RW Timeout (ms)", value = config.hevReadWriteTimeoutMs.toString(), onValueChange = { onUpdateConfig(config.copy(hevReadWriteTimeoutMs = it.toIntOrNull()?.coerceIn(1000, 600000) ?: 60000)) }, modifier = Modifier.weight(1f), placeholder = "60000", keyboardType = KeyboardType.Number, testTag = "hev_rw_timeout_input")
            }
        }
        AppDivider()
        IosInputFieldRow(icon = Icons.Default.Layers, iconBg = AppPalette.accentVariant, label = "Max Sessions (0 = Unlimited)", value = config.hevMaxSessionCount.toString(), onValueChange = { onUpdateConfig(config.copy(hevMaxSessionCount = it.toIntOrNull()?.coerceIn(0, 200000) ?: 0)) }, placeholder = "0", keyboardType = KeyboardType.Number, testTag = "hev_max_sessions_input"); AppDivider()
        IosInputFieldRow(icon = Icons.Default.Storage, iconBg = AppPalette.statusScanning, label = "MapDNS Cache Size", value = config.hevMapdnsCacheSize.toString(), onValueChange = { onUpdateConfig(config.copy(hevMapdnsCacheSize = it.toIntOrNull()?.coerceIn(100, 1000000) ?: 10000)) }, placeholder = "10000", keyboardType = KeyboardType.Number, testTag = "hev_mapdns_cache_input")
        AppDivider()
        IosPickerRow(
            icon = Icons.Default.SwapVert,
            iconBg = Color(0xFF30B0C7),
            title = "UDP Forwarding Mode",
            value = hevUdpLabels[hevUdpMode] ?: "UDP (ASSOCIATE)",
            options = hevUdpOptions.map { hevUdpLabels[it]!! },
            onOptionSelected = { index -> onUpdateConfig(config.copy(hevUdpMode = hevUdpOptions[index])) }
        )
    } }
    Spacer(modifier = Modifier.height(8.dp))
    IosGroupCard {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text("About HEV Engine", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text("These values configure the native HEV tun2socks engine used in Tunnel mode on Android. Changes apply after the VPN reconnects. Timeouts are clamped to safe ranges; Max Sessions 0 means unlimited.", color = IosSecondaryLabel, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable private fun SystemPage(isAndroid: Boolean, onExportBackup: () -> Unit, onImportBackup: () -> Unit, onOpenVpnSettings: () -> Unit, onResetClick: () -> Unit) {
    IosGroupCard { Column {
        if (isAndroid) {
            IosActionRow(icon = Icons.Default.Lock, iconBg = Color(0xFF0A84FF), title = "Always-on VPN", subtitle = "Open system VPN settings to keep AetherST always connected", onClick = onOpenVpnSettings); AppDivider()
        }
        IosActionRow(icon = Icons.Default.CloudUpload, iconBg = AppPalette.accentVariant, title = "Full Configuration Backup", subtitle = "Export all settings to .astf file", onClick = onExportBackup); AppDivider()
        IosActionRow(icon = Icons.Default.CloudDownload, iconBg = IosActiveGreen, title = "Restore from Backup", subtitle = "Import settings from an .astf file", onClick = onImportBackup); AppDivider()
        IosActionRow(icon = Icons.Default.DeleteForever, iconBg = AppPalette.statusError, title = "Reset to Factory Defaults", subtitle = "Wipe all custom tweaks and restart", onClick = onResetClick, titleColor = AppPalette.statusError)
    } }
}
