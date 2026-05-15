package com.remodex.mobile.terminal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.remodex.mobile.R

@Composable
fun TerminalWindowsSetupGuide(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.terminal_windows_guide_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.terminal_windows_guide_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TerminalGuideStep(
            title = stringResource(R.string.terminal_windows_guide_step_1_title),
            command = stringResource(R.string.terminal_windows_guide_step_1_command),
        )
        TerminalGuideStep(
            title = stringResource(R.string.terminal_windows_guide_step_2_title),
            command = stringResource(R.string.terminal_windows_guide_step_2_command),
        )
        TerminalGuideStep(
            title = stringResource(R.string.terminal_windows_guide_step_3_title),
            command = stringResource(R.string.terminal_windows_guide_step_3_command),
        )
        TerminalGuideStep(
            title = stringResource(R.string.terminal_windows_guide_step_4_title),
            command = stringResource(R.string.terminal_windows_guide_step_4_command),
        )
        TerminalGuideStep(
            title = stringResource(R.string.terminal_windows_guide_step_5_title),
            command = stringResource(R.string.terminal_windows_guide_step_5_command),
        )
        Text(
            text = stringResource(R.string.terminal_windows_guide_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TerminalGuideStep(
    title: String,
    command: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = command,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
