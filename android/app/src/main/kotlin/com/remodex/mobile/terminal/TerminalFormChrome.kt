package com.remodex.mobile.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remodex.mobile.ui.sidebar.SidebarColorPalette
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette
import com.remodex.mobile.ui.sidebar.remodexFlatControlChrome
import com.remodex.mobile.ui.theme.RemodexPopupChrome
import com.remodex.mobile.ui.theme.isAgentLightChrome

internal val TerminalFieldShape = RoundedCornerShape(14.dp)
internal val TerminalViewportShape = RoundedCornerShape(12.dp)
private val TerminalPrimaryShape = RoundedCornerShape(999.dp)
private val TerminalAccessoryKeyShape = RoundedCornerShape(999.dp)

@Composable
fun rememberTerminalViewportBackground(): Color {
    val lightChrome = isAgentLightChrome()
    return remember(lightChrome) {
        if (lightChrome) {
            Color(0xFF0C0C0E)
        } else {
            Color(0xFF000000)
        }
    }
}

@Composable
fun TerminalSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Text(
        text = text,
        modifier = modifier,
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        color = colors.mutedText,
    )
}

@Composable
fun TerminalTopBarAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: Boolean = false,
    destructive: Boolean = false,
    colors: SidebarColorPalette = rememberSidebarColorPalette(),
) {
    Text(
        text = text,
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = if (emphasis) FontWeight.SemiBold else FontWeight.Medium,
            ),
        color =
            when {
                destructive -> colors.red
                emphasis -> colors.accent
                else -> colors.secondaryText
            },
    )
}

@Composable
fun TerminalTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val colors = rememberSidebarColorPalette()
    Text(
        text = text,
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
        color = if (destructive) colors.red else colors.secondaryText,
    )
}

@Composable
fun TerminalEmptyState(
    text: String,
    modifier: Modifier = Modifier,
) {
    TerminalInfoCard(text = text, modifier = modifier)
}

@Composable
fun TerminalGuideStepCard(
    title: String,
    command: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
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
            text = command,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .remodexFlatControlChrome(TerminalFieldShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            style =
                MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                ),
            color = colors.secondaryText,
        )
    }
}

@Composable
fun TerminalViewportFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .remodexFlatControlChrome(TerminalFieldShape)
                .padding(3.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(TerminalViewportShape),
        ) {
            content()
        }
    }
}

@Composable
fun TerminalFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    fieldHeight: Modifier = Modifier.height(48.dp),
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val colors = rememberSidebarColorPalette()
    val textStyle =
        MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            color = colors.primaryText,
            lineHeight = 20.sp,
        )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            color = colors.secondaryText,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(fieldHeight)
                    .remodexFlatControlChrome(TerminalFieldShape)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = textStyle,
            visualTransformation = visualTransformation,
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    innerTextField()
                }
            },
        )
    }
}

@Composable
fun TerminalFormCheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = colors.accent,
                    uncheckedColor = colors.border,
                    checkmarkColor = colors.onAccent,
                ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = colors.primaryText,
        )
    }
}

@Composable
fun TerminalPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fullWidth: Boolean = true,
) {
    val colors = rememberSidebarColorPalette()
    val elevation = RemodexPopupChrome.elevatedControlShadowElevation()
    val shadowColor = RemodexPopupChrome.elevatedControlShadowColor()
    val compact = !fullWidth
    val buttonHeight = if (compact) 40.dp else 48.dp
    val horizontalPadding = if (compact) 16.dp else 0.dp
    Box(
        modifier =
            modifier
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .height(buttonHeight)
                .shadow(
                    elevation = if (enabled) elevation else 0.dp,
                    shape = TerminalPrimaryShape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor,
                )
                .clip(TerminalPrimaryShape)
                .background(if (enabled) colors.accent else colors.surface)
                .border(
                    width = 1.dp,
                    color = if (enabled) colors.accent else colors.border,
                    shape = TerminalPrimaryShape,
                )
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = horizontalPadding),
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 13.sp else 15.sp,
                ),
            color = if (enabled) colors.onAccent else colors.mutedText,
        )
    }
}

@Composable
fun TerminalSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = rememberSidebarColorPalette()
    Box(
        modifier =
            modifier
                .height(40.dp)
                .remodexFlatControlChrome(TerminalPrimaryShape)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                ),
            color = if (enabled) colors.primaryText else colors.mutedText,
        )
    }
}

@Composable
fun TerminalProfileCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    val colors = rememberSidebarColorPalette()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .remodexFlatControlChrome(TerminalFieldShape)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = colors.primaryText,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = colors.secondaryText,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            actions()
        }
    }
}

@Composable
fun TerminalInfoCard(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .remodexFlatControlChrome(TerminalFieldShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        color = colors.secondaryText,
    )
}

@Composable
fun TerminalAccessoryKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
) {
    val colors = rememberSidebarColorPalette()
    Row(
        modifier =
            modifier
                .height(34.dp)
                .remodexFlatControlChrome(TerminalAccessoryKeyShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        leadingIcon?.invoke()
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
            color = if (selected) colors.accent else colors.primaryText,
            maxLines = 1,
        )
    }
}
