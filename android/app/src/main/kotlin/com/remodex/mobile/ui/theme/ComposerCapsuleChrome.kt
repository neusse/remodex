package com.remodex.mobile.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.remodex.mobile.ui.sidebar.remodexFlatControlChrome

/**
 * Light “agent minimal” chrome: warm background + frosted controls.
 */
@Composable
internal fun isAgentLightChrome(): Boolean =
    MaterialTheme.colorScheme.background.luminance() >= 0.43f

/** Main turn composer shell — flat fill/border aligned with env pills and header icon buttons. */
@Composable
internal fun RemodexComposerCapsuleChrome(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.remodexFlatControlChrome(RemodexComposerCapsuleShape)) {
        content()
    }
}
