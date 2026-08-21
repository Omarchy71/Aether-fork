package io.github.immaghzbad.aetherst.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    primaryContainer = ElegantPrimaryContainer,
    onPrimaryContainer = ElegantOnPrimaryContainer,
    secondary = ElegantSecondary,
    background = ElegantBackground,
    onBackground = ElegantTextPrimary,
    surface = ElegantSurface,
    onSurface = ElegantTextPrimary,
    surfaceVariant = ElegantSurfaceCard,
    onSurfaceVariant = ElegantTextSecondary,
    outline = ElegantOutline
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}
