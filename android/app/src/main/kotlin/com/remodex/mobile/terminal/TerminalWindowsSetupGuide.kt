package com.remodex.mobile.terminal

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette
import com.remodex.mobile.ui.sidebar.remodexFlatControlChrome
import kotlinx.coroutines.launch

@Composable
fun TerminalWindowsSetupGuide(
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    val colors = rememberSidebarColorPalette()
    val contentModifier =
        if (scrollable) {
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        } else {
            modifier.fillMaxWidth()
        }
    Column(
        modifier = contentModifier,
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
        TerminalWindowsGuideStep(
            number = "1",
            title = stringResource(R.string.terminal_windows_guide_step_1_title),
            body = stringResource(R.string.terminal_windows_guide_step_1_body),
            command = stringResource(R.string.terminal_windows_guide_step_1_command),
        )
        TerminalWindowsGuideStep(
            number = "2",
            title = stringResource(R.string.terminal_windows_guide_step_2_title),
            body = stringResource(R.string.terminal_windows_guide_step_2_body),
            command = stringResource(R.string.terminal_windows_guide_step_2_command),
        )
        TerminalWindowsGuideStep(
            number = "3",
            title = stringResource(R.string.terminal_windows_guide_step_3_title),
            body = stringResource(R.string.terminal_windows_guide_step_3_body),
            command = stringResource(R.string.terminal_windows_guide_step_3_command),
        )
        TerminalWindowsGuideStep(
            number = "4",
            title = stringResource(R.string.terminal_windows_guide_step_4_title),
            body = stringResource(R.string.terminal_windows_guide_step_4_body),
            command = stringResource(R.string.terminal_windows_guide_step_4_command),
        )
        TerminalWindowsGuideStep(
            number = "5",
            title = stringResource(R.string.terminal_windows_guide_step_5_title),
            body = stringResource(R.string.terminal_windows_guide_step_5_body),
            command = stringResource(R.string.terminal_windows_guide_step_5_command),
        )
        TerminalInfoCard(text = stringResource(R.string.terminal_windows_guide_note))
        TerminalWindowsGuideHelp(
            title = stringResource(R.string.terminal_windows_guide_help_title),
            body = stringResource(R.string.terminal_windows_guide_help_body),
            prompt = stringResource(R.string.terminal_windows_guide_help_prompt),
            copyContentDescription = stringResource(R.string.terminal_windows_guide_help_copy_cd),
        )
    }
}

@Composable
private fun TerminalWindowsGuideStep(
    number: String,
    title: String,
    body: String,
    command: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(26.dp)
                    .remodexFlatControlChrome(TerminalFieldShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = colors.primaryText,
            )
            Text(
                text = body,
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                color = colors.secondaryText,
            )
            Text(
                text = command,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .remodexFlatControlChrome(TerminalFieldShape)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                color = colors.secondaryText,
            )
        }
    }
}

@Composable
private fun TerminalWindowsGuideHelp(
    title: String,
    body: String,
    prompt: String,
    copyContentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            color = colors.primaryText,
        )
        Text(
            text = body,
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                ),
            color = colors.secondaryText,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .remodexFlatControlChrome(TerminalFieldShape)
                    .padding(start = 10.dp, end = 2.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = prompt,
                modifier = Modifier.weight(1f),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    ),
                color = colors.secondaryText,
            )
            IconButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("windows-ssh-setup-prompt", prompt).toClipEntry(),
                        )
                    }
                },
            ) {
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_copy),
                    contentDescription = copyContentDescription,
                    tint = colors.secondaryText,
                )
            }
        }
    }
}
