package com.remodex.mobile.ui.turn

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.ui.theme.RemodexGitAddition
import com.remodex.mobile.ui.theme.RemodexPopupChrome
import com.remodex.mobile.ui.theme.isAgentLightChrome

/** Compact footnote / unified chip icon size. */
internal val ComposerFootnoteIconDp = 13.dp

internal fun compactModelStickerForUnifiedChip(option: TurnComposerRuntimeOption): String {
    val t = option.label.trim()
    val lowered = t.lowercase()
    if (lowered.startsWith("gpt-")) {
        val rest = t.drop(4).trimStart()
        return rest.substringBefore(' ').substringBefore('|').takeIf { it.isNotBlank() } ?: t
    }
    return if (t.length <= 16) {
        t
    } else {
        t.take(14) + "\u2026"
    }
}

internal fun reasoningOrSpeedCompactCaption(runtimeControls: TurnComposerRuntimeControlsState): String =
    when {
        runtimeControls.reasoningEffort.enabled -> runtimeControls.reasoningEffort.selected.label
        else -> runtimeControls.serviceTier.selected.label
    }.trim()

internal fun unifiedModelRuntimeChipLabel(runtimeControls: TurnComposerRuntimeControlsState): String {
    val m = compactModelStickerForUnifiedChip(runtimeControls.model.selected)
    val r = reasoningOrSpeedCompactCaption(runtimeControls)
    return if (r.isBlank()) m else "$m $r"
}

private enum class ComposerRuntimePopupTab {
    Model,
    Reasoning,
}

private data class ComposerRuntimeModelEntry(
    val id: String,
    val label: String,
)

private val ComposerRuntimeModelEntries =
    listOf(
        ComposerRuntimeModelEntry("gpt-5.5", "GPT-5.5"),
        ComposerRuntimeModelEntry("gpt-5.4", "gpt-5.4"),
        ComposerRuntimeModelEntry("gpt-5.4-mini", "GPT-5.4-Mini"),
        ComposerRuntimeModelEntry("gpt-5.3-codex", "gpt-5.3-codex"),
        ComposerRuntimeModelEntry("gpt-5.2", "gpt-5.2"),
    )

private val ComposerRuntimeReasoningOrder = listOf("low", "medium", "high", "xhigh")

/** Single pill + menu (Codex desktop: model caption + reasoning + speed). */
@Composable
internal fun UnifiedComposerModelRuntimeChip(
    runtimeControls: TurnComposerRuntimeControlsState,
    runtimeToolbarMenu: TurnComposerToolbarMenu,
    enabled: Boolean,
    onSelectModel: (TurnComposerRuntimeOption) -> Unit,
    onSelectReasoningEffort: (TurnComposerRuntimeOption) -> Unit,
    onSelectServiceTier: (TurnComposerRuntimeOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(ComposerRuntimePopupTab.Model) }

    val modelState = runtimeControls.model
    val modelOptions =
        remember(modelState.selected, modelState.options) {
            if (modelState.options.any { it.id == modelState.selected.id }) {
                modelState.options
            } else {
                listOf(modelState.selected) + modelState.options
            }
        }
    val popupModelOptions =
        remember(modelOptions) {
            ComposerRuntimeModelEntries.map { entry ->
                modelOptions.firstOrNull { it.matchesRuntimeModelEntry(entry) }
                    ?.copy(label = entry.label)
                    ?: TurnComposerRuntimeOption(
                        id = entry.id,
                        label = entry.label,
                        enabled = false,
                    )
            }
        }
    val reasoningOptions =
        remember(runtimeControls.reasoningEffort.options) {
            runtimeControls.reasoningEffort.options
                .filter { it.id != TURN_COMPOSER_RUNTIME_AUTO_ID }
                .sortedBy { option ->
                    val key = option.id.reasoningSortKey()
                    ComposerRuntimeReasoningOrder.indexOf(key).takeIf { it >= 0 } ?: Int.MAX_VALUE
                }
        }
    val normalTierOption =
        runtimeControls.serviceTier.options.firstOrNull { it.id == TURN_COMPOSER_RUNTIME_AUTO_ID }
    val fastTierOption =
        runtimeControls.serviceTier.options.firstOrNull {
            it.id != TURN_COMPOSER_RUNTIME_AUTO_ID && it.enabled
        }
    val fastModeEnabled = runtimeControls.serviceTier.selected.id != TURN_COMPOSER_RUNTIME_AUTO_ID
    val canToggleFast =
        enabled &&
            runtimeControls.serviceTier.enabled &&
            normalTierOption != null &&
            fastTierOption != null

    val hasModelChoice = popupModelOptions.any { it.enabled }
    val hasReasoningChoice = reasoningOptions.any { it.enabled }

    val canOpenMenu =
        enabled &&
            (hasModelChoice || hasReasoningChoice || canToggleFast || runtimeToolbarMenu.sections.any { it.items.any { item -> item.enabled } })

    Box {
        val lightChrome = isAgentLightChrome()
        val showFastModeIcon = runtimeControls.serviceTier.selected.id != TURN_COMPOSER_RUNTIME_AUTO_ID
        Surface(
            modifier =
                modifier
                    .wrapContentWidth()
                    .widthIn(max = 136.dp)
                    .border(RemodexPopupChrome.borderStroke(), RoundedCornerShape(999.dp))
                    .clickable(enabled = canOpenMenu) { expanded = true },
            shape = RoundedCornerShape(999.dp),
            color = RemodexPopupChrome.surfaceColor().copy(alpha = if (lightChrome) 0.92f else 0.72f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (showFastModeIcon) {
                        Icon(
                            painter = painterResource(LucideR.drawable.lucide_ic_zap),
                            contentDescription = null,
                            modifier = Modifier.size(ComposerFootnoteIconDp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = unifiedModelRuntimeChipLabel(runtimeControls),
                        modifier = Modifier.widthIn(max = 94.dp),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    painter = painterResource(LucideR.drawable.lucide_ic_chevron_down),
                    contentDescription = null,
                    modifier = Modifier.size(ComposerFootnoteIconDp),
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.BottomStart,
                offset = IntOffset(x = 0, y = -24),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                ComposerRuntimePopup(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    modelOptions = popupModelOptions,
                    selectedModelId = modelState.selected.id,
                    reasoningOptions = reasoningOptions,
                    selectedReasoningId = runtimeControls.reasoningEffort.selected.id,
                    fastModeEnabled = fastModeEnabled,
                    fastModeToggleEnabled = canToggleFast,
                    onSelectModel = {
                        expanded = false
                        onSelectModel(it)
                    },
                    onSelectReasoningEffort = {
                        expanded = false
                        onSelectReasoningEffort(it)
                    },
                    onSetFastMode = { checked ->
                        when {
                            checked -> fastTierOption?.let(onSelectServiceTier)
                            else -> normalTierOption?.let(onSelectServiceTier)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ComposerRuntimePopup(
    selectedTab: ComposerRuntimePopupTab,
    onSelectTab: (ComposerRuntimePopupTab) -> Unit,
    modelOptions: List<TurnComposerRuntimeOption>,
    selectedModelId: String,
    reasoningOptions: List<TurnComposerRuntimeOption>,
    selectedReasoningId: String,
    fastModeEnabled: Boolean,
    fastModeToggleEnabled: Boolean,
    onSelectModel: (TurnComposerRuntimeOption) -> Unit,
    onSelectReasoningEffort: (TurnComposerRuntimeOption) -> Unit,
    onSetFastMode: (Boolean) -> Unit,
) {
    val panelShape = RemodexPopupChrome.PopupShape
    val panelColor = RemodexPopupChrome.surfaceColor()
    val border = RemodexPopupChrome.borderStroke()
    val listHeight = 28.5.dp * ComposerRuntimeModelEntries.size
    Column(
        modifier = Modifier.width(198.dp),
    ) {
        Surface(
            modifier = Modifier
                .border(border, panelShape),
            shape = panelShape,
            color = panelColor,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = RemodexPopupChrome.tonalElevation(),
            shadowElevation = RemodexPopupChrome.shadowElevation(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.5.dp, vertical = 9.dp),
            ) {
                ComposerRuntimeSegmentedTabs(
                    selectedTab = selectedTab,
                    onSelectTab = onSelectTab,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 9.dp, bottom = 4.5.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                    thickness = 0.5.dp,
                )
                Column(
                    modifier = Modifier.height(listHeight),
                ) {
                    when (selectedTab) {
                        ComposerRuntimePopupTab.Model ->
                            modelOptions.forEach { option ->
                                ComposerRuntimePopupRow(
                                    label = option.label,
                                    selected = option.id == selectedModelId,
                                    enabled = option.enabled,
                                    onClick = { onSelectModel(option) },
                                )
                            }

                        ComposerRuntimePopupTab.Reasoning ->
                            reasoningOptions.forEach { option ->
                                ComposerRuntimePopupRow(
                                    label = option.reasoningPopupLabel(),
                                    selected = option.id == selectedReasoningId,
                                    enabled = option.enabled,
                                    onClick = { onSelectReasoningEffort(option) },
                                )
                            }
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 6.dp, bottom = 7.5.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.26f),
                    thickness = 0.5.dp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "FAST MODE",
                        modifier = Modifier.weight(1f),
                        style =
                            MaterialTheme.typography.labelLarge.copy(
                                fontSize = 9.75.sp,
                                lineHeight = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.sp,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    CompactFastModeSwitch(
                        checked = fastModeEnabled,
                        enabled = fastModeToggleEnabled,
                        onCheckedChange = onSetFastMode,
                    )
                }
            }
        }
        ComposerRuntimePopupPointer(
            color = panelColor,
            modifier = Modifier.padding(start = 31.5.dp),
        )
    }
}

@Composable
private fun ComposerRuntimeSegmentedTabs(
    selectedTab: ComposerRuntimePopupTab,
    onSelectTab: (ComposerRuntimePopupTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .width(133.5.dp)
            .height(22.5.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = if (isAgentLightChrome()) 0.10f else 0.08f))
            .padding(1.5.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
    ) {
        ComposerRuntimeSegmentTab(
            label = "Model",
            selected = selectedTab == ComposerRuntimePopupTab.Model,
            onClick = { onSelectTab(ComposerRuntimePopupTab.Model) },
            modifier = Modifier.weight(1f),
        )
        ComposerRuntimeSegmentTab(
            label = "Reasoning",
            selected = selectedTab == ComposerRuntimePopupTab.Reasoning,
            onClick = { onSelectTab(ComposerRuntimePopupTab.Reasoning) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ComposerRuntimeSegmentTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .height(19.5.dp)
            .clip(shape)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontSize = 9.5.sp,
                    lineHeight = 11.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
        )
    }
}

@Composable
private fun ComposerRuntimePopupRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.5.dp)
            .clip(RoundedCornerShape(7.5.dp))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 11.25.sp,
                    lineHeight = 13.5.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    letterSpacing = 0.sp,
                ),
            color =
                MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (enabled) 1f else 0.38f,
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Spacer(modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun CompactFastModeSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    val trackColor =
        when {
            checked -> RemodexGitAddition
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
        }
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(19.5.dp)
            .clip(shape)
            .background(trackColor.copy(alpha = if (enabled) 1f else 0.42f))
            .border(
                width = 0.5.dp,
                color =
                    if (checked) {
                        Color.White.copy(alpha = 0.36f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.36f)
                    },
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                onCheckedChange(!checked)
            }
            .padding(2.25.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = if (checked) 16.5.dp else 0.dp)
                .size(15.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

@Composable
private fun ComposerRuntimePopupPointer(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .width(16.5.dp)
            .height(7.5.dp),
    ) {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
                close()
            }
        drawPath(path = path, color = color)
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(0f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.10f),
            start = Offset(size.width, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = 1f,
        )
    }
}

private fun TurnComposerRuntimeOption.matchesRuntimeModelEntry(
    entry: ComposerRuntimeModelEntry,
): Boolean {
    val normalizedEntry = entry.id.lowercase()
    return id.lowercase() == normalizedEntry ||
        label.lowercase() == normalizedEntry ||
        label.lowercase() == entry.label.lowercase()
}

private fun String.reasoningSortKey(): String =
    trim()
        .lowercase()
        .replace('-', '_')
        .replace(' ', '_')

private fun TurnComposerRuntimeOption.reasoningPopupLabel(): String =
    when (id.reasoningSortKey()) {
        "low" -> "Low"
        "medium", "mid" -> "Mid"
        "high" -> "High"
        "xhigh", "extra_high" -> "XHigh"
        else -> label
    }
