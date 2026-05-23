package com.remodex.mobile.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

object RemodexPopupChrome {
    val PopupShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val PanelShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

    @Composable
    fun surfaceColor(): Color =
        if (isAgentLightChrome()) {
            Color(0xFFFAF8F2).copy(alpha = 0.97f).compositeOver(MaterialTheme.colorScheme.background)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                .compositeOver(MaterialTheme.colorScheme.background)
        }

    @Composable
    fun borderWidth(): Dp =
        if (isAgentLightChrome()) {
            1.dp
        } else {
            0.5.dp
        }

    @Composable
    fun borderColor(accent: Boolean = false): Color =
        when {
            accent -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f)
            isAgentLightChrome() -> AgentLightColors.Border
            else -> Color.White.copy(alpha = 0.12f)
        }

    @Composable
    fun borderStroke(accent: Boolean = false): BorderStroke =
        BorderStroke(borderWidth(), borderColor(accent))

    @Composable
    fun shadowElevation(panel: Boolean = false) =
        if (isAgentLightChrome()) {
            if (panel) 10.dp else 8.dp
        } else {
            if (panel) 4.dp else 3.dp
        }

    @Composable
    fun tonalElevation() = if (isAgentLightChrome()) 0.dp else 2.dp

    /** Subtle drop shadow for composer, env pills, and header icon buttons. */
    @Composable
    fun elevatedControlShadowElevation(): Dp =
        if (isAgentLightChrome()) {
            4.dp
        } else {
            2.dp
        }

    @Composable
    fun elevatedControlShadowColor(): Color =
        Color.Black.copy(alpha = if (isAgentLightChrome()) 0.055f else 0.085f)
}

@Composable
fun RemodexPopupSurface(
    modifier: Modifier = Modifier,
    panel: Boolean = false,
    accentBorder: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = if (panel) RemodexPopupChrome.PanelShape else RemodexPopupChrome.PopupShape
    Surface(
        modifier = modifier.border(RemodexPopupChrome.borderStroke(accentBorder), shape),
        shape = shape,
        color = RemodexPopupChrome.surfaceColor(),
        tonalElevation = RemodexPopupChrome.tonalElevation(),
        shadowElevation = RemodexPopupChrome.shadowElevation(panel),
        content = content,
    )
}

@Composable
fun RemodexDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        properties = properties,
        shape = RemodexPopupChrome.PopupShape,
        containerColor = RemodexPopupChrome.surfaceColor(),
        tonalElevation = RemodexPopupChrome.tonalElevation(),
        shadowElevation = RemodexPopupChrome.shadowElevation(),
        border = RemodexPopupChrome.borderStroke(),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemodexModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    sheetGesturesEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val panelShape = RemodexPopupChrome.PanelShape
    // Border must not live on the sheet modifier: M3 sizes that node to the expanded slot,
    // so a shaped border draws a taller outline than the visible sheet (especially in dark mode).
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = panelShape,
        containerColor = RemodexPopupChrome.surfaceColor(),
        tonalElevation = RemodexPopupChrome.tonalElevation(),
        content = content,
    )
}
