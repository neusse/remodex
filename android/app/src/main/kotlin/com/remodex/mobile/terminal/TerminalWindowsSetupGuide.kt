package com.remodex.mobile.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remodex.mobile.R
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette

@Composable
fun TerminalWindowsSetupGuide(
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.terminal_windows_guide_title),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            color = colors.primaryText,
        )
        Text(
            text = stringResource(R.string.terminal_windows_guide_intro),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = colors.secondaryText,
        )
        TerminalGuideStepCard(
            title = stringResource(R.string.terminal_windows_guide_step_1_title),
            command = stringResource(R.string.terminal_windows_guide_step_1_command),
        )
        TerminalGuideStepCard(
            title = stringResource(R.string.terminal_windows_guide_step_2_title),
            command = stringResource(R.string.terminal_windows_guide_step_2_command),
        )
        TerminalGuideStepCard(
            title = stringResource(R.string.terminal_windows_guide_step_3_title),
            command = stringResource(R.string.terminal_windows_guide_step_3_command),
        )
        TerminalGuideStepCard(
            title = stringResource(R.string.terminal_windows_guide_step_4_title),
            command = stringResource(R.string.terminal_windows_guide_step_4_command),
        )
        TerminalGuideStepCard(
            title = stringResource(R.string.terminal_windows_guide_step_5_title),
            command = stringResource(R.string.terminal_windows_guide_step_5_command),
        )
        TerminalInfoCard(text = stringResource(R.string.terminal_windows_guide_note))
    }
}
