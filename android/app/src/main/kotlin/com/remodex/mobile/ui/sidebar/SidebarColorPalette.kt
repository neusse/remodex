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
                background = Color(0xFF000000),
                surface = Color(0xFF1C1C1E),
                selectedRow = Color(0xFF1C1C1E),
                border = Color(0xFF2C2C2E),
                primaryText = Color(0xFFFFFFFF),
                secondaryText = Color(0xFFAEAEB2),
                mutedText = Color(0xFF8E8E93),
                accent = accent,
                onAccent = onAccent,
                green = Color(0xFF55D979),
                red = Color(0xFFFF6B6B),
            )
        } else {
            SidebarColorPalette(
                background = Color(0xFFF6F5F2),
                surface = Color(0xFFEEEDE9),
                selectedRow = Color(0xFFEDEBE5),
                border = Color(0xFFD8D5CD),
                primaryText = Color(0xFF1E1F21),
                secondaryText = Color(0xFF5F6266),
                mutedText = Color(0xFF8A8D91),
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
