package io.github.immaghzbad.aetherst.shared.ui.components

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
actual fun LogsVerticalScrollbar(
    state: LazyListState,
    modifier: Modifier
) {
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(state),
        modifier = modifier,
        style = defaultScrollbarStyle().copy(
            unhoverColor = Color.White.copy(alpha = 0.35f),
            hoverColor = Color.White.copy(alpha = 0.6f),
            thickness = 6.dp
        )
    )
}

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
}
