package com.remodex.mobile.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.remodex.mobile.ui.LocalUserBubbleColor
import com.remodex.mobile.ui.theme.RemodexPopupChrome
import com.remodex.mobile.ui.theme.accentBackground
import com.remodex.mobile.ui.theme.accentForeground
import com.remodex.mobile.ui.theme.isAgentLightChrome

@Immutable
data class SidebarColorPalette(
    val background: Color,
    val surface: Color,
    val selectedRow: Color,
    val border: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val accent: Color,
    val onAccent: Color,
    val green: Color,
    val red: Color,
)

@Composable
fun rememberSidebarColorPalette(
    darkTheme: Boolean = !isAgentLightChrome(),
): SidebarColorPalette {
    val userBubbleColor = LocalUserBubbleColor.current
    val colorScheme = MaterialTheme.colorScheme
    val isLightChrome = isAgentLightChrome()
    return remember(darkTheme, userBubbleColor, colorScheme, isLightChrome) {
        val accent = userBubbleColor.accentBackground(colorScheme, isLightChrome)
        val onAccent = userBubbleColor.accentForeground(colorScheme, isLightChrome)
        if (darkTheme) {
            SidebarColorPalette(
                background = colorScheme.background,
                surface = colorScheme.surface,
                selectedRow = colorScheme.surfaceVariant,
                border = colorScheme.outline,
                primaryText = colorScheme.onBackground,
                secondaryText = colorScheme.onSurfaceVariant,
                mutedText = colorScheme.outlineVariant,
                accent = accent,
                onAccent = onAccent,
                green = Color(0xFF55D979),
                red = Color(0xFFFF6B6B),
            )
        } else {
            SidebarColorPalette(
                background = colorScheme.background,
                surface = colorScheme.surfaceVariant,
                selectedRow = colorScheme.surfaceVariant,
                border = colorScheme.outline,
                primaryText = colorScheme.onBackground,
                secondaryText = colorScheme.onSurfaceVariant,
                mutedText = colorScheme.outlineVariant,
                accent = accent,
                onAccent = onAccent,
                green = Color(0xFF18A957),
                red = Color(0xFFC4484D),
            )
        }
    }
}

/** Warm frosted chrome for composer, env pills, and header icon buttons — light shadow + hairline border. */
@Composable
fun Modifier.remodexFlatControlChrome(shape: Shape): Modifier {
    val fill = RemodexPopupChrome.surfaceColor()
    val border = RemodexPopupChrome.borderStroke()
    val elevation = RemodexPopupChrome.elevatedControlShadowElevation()
    val shadowColor = RemodexPopupChrome.elevatedControlShadowColor()
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor,
        )
        .clip(shape)
        .background(fill)
        .border(border, shape)
}
