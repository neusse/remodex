package com.remodex.mobile.ui.agent

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.annotation.DrawableRes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R
import com.remodex.mobile.core.model.GitDiffTotals
import com.remodex.mobile.core.model.TurnGitActionKind
import com.remodex.mobile.ui.theme.AgentLightColors
import com.remodex.mobile.ui.theme.RemodexGitAddition
import com.remodex.mobile.ui.theme.RemodexDropdownMenu
import com.remodex.mobile.ui.sidebar.RemodexCircleIconButton
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette
import com.remodex.mobile.ui.theme.isAgentLightChrome

/** Truncate long filesystem paths for the header subtitle (middle ellipsis). */
fun truncatePathMiddle(
    path: String,
    maxLen: Int = 44,
): String {
    if (path.length <= maxLen) return path
    val ellipsis = "…"
    val inner = maxLen - ellipsis.length
    val head = inner / 2
    val tail = inner - head
    return path.take(head) + ellipsis + path.takeLast(tail)
}

/**
 * Main turn toolbar: title, repo path, running pill, git actions menu (incl. repository diff),
 * overflow (handoff / stop).
 * SwiftUI reference: [TurnToolbarContent](CodexMobile/CodexMobile/Views/Turn/TurnToolbarContent.swift).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHeader(
    title: String,
    pathSubtitle: String?,
    onPathClick: (() -> Unit)?,
    showRunningPill: Boolean,
    repoDiffTotals: GitDiffTotals?,
    isLoadingRepoDiff: Boolean,
    canViewRepoDiff: Boolean,
    showGitActions: Boolean,
    onGitAction: ((TurnGitActionKind) -> Unit)?,
    gitActionsBusy: Boolean,
    showsDiscardRuntimeRecovery: Boolean,
    isGitActionEnabled: Boolean,
    isGitInitialized: Boolean,
    showDesktopHandoff: Boolean,
    handingOffToDesktop: Boolean,
    showWorktreeHandoff: Boolean,
    handingOffWorktree: Boolean,
    isWorktreeProject: Boolean,
    showTurnStop: Boolean,
    onOpenDrawer: () -> Unit,
    onContinueDesktop: () -> Unit,
    onWorktreeHandoff: () -> Unit,
    onStopTurn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var overflowExpanded by remember { mutableStateOf(false) }
    var gitMenuExpanded by remember { mutableStateOf(false) }
    val showOverflow = showDesktopHandoff || showWorktreeHandoff || showTurnStop
    val gitMenuCd = stringResource(R.string.cd_git_actions_menu)
    val chrome = isAgentLightChrome()
    val pathColor =
        if (chrome) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.84f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f)
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(108.dp),
    ) {
        HeaderAtmosphere(
            lightChrome = chrome,
            modifier =
                Modifier
                    .matchParentSize()
                    .statusBarsPadding(),
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HeaderMenuButton(onOpenDrawer = onOpenDrawer)
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .then(
                            if (pathSubtitle != null && onPathClick != null) {
                                Modifier
                                    .clickable(onClick = onPathClick)
                                    .semantics {
                                        contentDescription = pathSubtitle
                                        role = Role.Button
                                    }
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Column {
                    Text(
                        text = title,
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                lineHeight = 22.sp,
                            ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pathSubtitle != null) {
                        Text(
                            text = pathSubtitle,
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    lineHeight = 13.sp,
                                ),
                            fontFamily = FontFamily.Monospace,
                            color = pathColor,
                            maxLines = 1,
                            overflow = TextOverflow.MiddleEllipsis,
                        )
                    }
                }
            }
            if (showRunningPill) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.turn_top_bar_thinking),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
            HeaderActions(
                repoDiffTotals = repoDiffTotals,
                isLoadingRepoDiff = isLoadingRepoDiff,
                canViewRepoDiff = canViewRepoDiff,
                showGitActions = showGitActions,
                onGitAction = onGitAction,
                gitActionsBusy = gitActionsBusy,
                showsDiscardRuntimeRecovery = showsDiscardRuntimeRecovery,
                isGitActionEnabled = isGitActionEnabled,
                isGitInitialized = isGitInitialized,
                gitMenuCd = gitMenuCd,
                showOverflow = showOverflow,
                overflowExpanded = overflowExpanded,
                onSetOverflowExpanded = { overflowExpanded = it },
                gitMenuExpanded = gitMenuExpanded,
                onSetGitMenuExpanded = { gitMenuExpanded = it },
                showDesktopHandoff = showDesktopHandoff,
                handingOffToDesktop = handingOffToDesktop,
                showWorktreeHandoff = showWorktreeHandoff,
                handingOffWorktree = handingOffWorktree,
                isWorktreeProject = isWorktreeProject,
                showTurnStop = showTurnStop,
                onContinueDesktop = onContinueDesktop,
                onWorktreeHandoff = onWorktreeHandoff,
                onStopTurn = onStopTurn,
            )
        }
    }
}

@Composable
private fun HeaderAtmosphere(
    lightChrome: Boolean,
    modifier: Modifier = Modifier,
) {
    val base = if (lightChrome) AgentLightColors.ScreenBg else MaterialTheme.colorScheme.background
    val topAlpha = if (lightChrome) 0.86f else 0.74f
    Box(
        modifier =
            modifier
                .clipToBounds(),
    ) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize(),
        ) {
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colorStops =
                            arrayOf(
                                0f to base.copy(alpha = topAlpha),
                                0.62f to base.copy(alpha = topAlpha * 0.44f),
                                1f to base.copy(alpha = 0f),
                            ),
                    ),
            )
        }
    }
}

@Composable
private fun HeaderMenuButton(onOpenDrawer: () -> Unit) {
    val colors = rememberSidebarColorPalette()
    RemodexCircleIconButton(
        onClick = onOpenDrawer,
        contentDescription = stringResource(R.string.cd_open_navigation_drawer),
        colors = colors,
    ) {
        Icon(
            painter = painterResource(LucideR.drawable.lucide_ic_menu),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = colors.primaryText,
        )
    }
}

@Composable
private fun HeaderActions(
    repoDiffTotals: GitDiffTotals?,
    isLoadingRepoDiff: Boolean,
    canViewRepoDiff: Boolean,
    showGitActions: Boolean,
    onGitAction: ((TurnGitActionKind) -> Unit)?,
    gitActionsBusy: Boolean,
    showsDiscardRuntimeRecovery: Boolean,
    isGitActionEnabled: Boolean,
    isGitInitialized: Boolean,
    gitMenuCd: String,
    showOverflow: Boolean,
    overflowExpanded: Boolean,
    onSetOverflowExpanded: (Boolean) -> Unit,
    gitMenuExpanded: Boolean,
    onSetGitMenuExpanded: (Boolean) -> Unit,
    showDesktopHandoff: Boolean,
    handingOffToDesktop: Boolean,
    showWorktreeHandoff: Boolean,
    handingOffWorktree: Boolean,
    isWorktreeProject: Boolean,
    showTurnStop: Boolean,
    onContinueDesktop: () -> Unit,
    onWorktreeHandoff: () -> Unit,
    onStopTurn: () -> Unit,
) {
    val colors = rememberSidebarColorPalette()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showGitActions && onGitAction != null) {
            Box {
                RemodexCircleIconButton(
                    onClick = { onSetGitMenuExpanded(true) },
                    enabled = (canViewRepoDiff || isGitActionEnabled) && !gitActionsBusy,
                    contentDescription = gitMenuCd,
                    colors = colors,
                ) {
                    val tint = colors.primaryText.copy(alpha = if ((canViewRepoDiff || isGitActionEnabled) && !gitActionsBusy) 1f else 0.42f)
                    if (gitActionsBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = tint,
                        )
                    } else {
                        Box(modifier = Modifier.size(20.dp)) {
                            GitNodeConnectorIcon(
                                tint = tint,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
                GitActionsDropdown(
                    expanded = gitMenuExpanded,
                    onDismissRequest = { onSetGitMenuExpanded(false) },
                    repoDiffTotals = repoDiffTotals,
                    isLoadingRepoDiff = isLoadingRepoDiff,
                    canViewRepoDiff = canViewRepoDiff,
                    showsDiscardRuntimeRecovery = showsDiscardRuntimeRecovery,
                    isGitActionEnabled = isGitActionEnabled,
                    isGitInitialized = isGitInitialized,
                    onSelect = {
                        onSetGitMenuExpanded(false)
                        onGitAction(it)
                    },
                )
            }
        }

        if (showOverflow) {
            Box {
                RemodexCircleIconButton(
                    onClick = { onSetOverflowExpanded(true) },
                    contentDescription = stringResource(R.string.turn_top_bar_thread_actions_cd),
                    colors = colors,
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_ellipsis_vertical),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.primaryText,
                    )
                }
                RemodexDropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { onSetOverflowExpanded(false) },
                ) {
                    if (showDesktopHandoff) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (handingOffToDesktop) {
                                        stringResource(R.string.turn_open_desktop_opening)
                                    } else {
                                        stringResource(R.string.turn_open_desktop)
                                    },
                                )
                            },
                            onClick = {
                                onSetOverflowExpanded(false)
                                if (!handingOffToDesktop) onContinueDesktop()
                            },
                            enabled = !handingOffToDesktop,
                        )
                    }
                    if (showWorktreeHandoff) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (handingOffWorktree) {
                                        stringResource(R.string.composer_worktree_preparing)
                                    } else if (isWorktreeProject) {
                                        stringResource(R.string.composer_runtime_menu_handoff_local)
                                    } else {
                                        stringResource(R.string.composer_runtime_menu_worktree_handoff)
                                    },
                                )
                            },
                            onClick = {
                                onSetOverflowExpanded(false)
                                if (!handingOffWorktree) onWorktreeHandoff()
                            },
                            enabled = !handingOffWorktree,
                        )
                    }
                    if (showTurnStop) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.turn_stop),
                                    color = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                onSetOverflowExpanded(false)
                                onStopTurn()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitActionsDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    repoDiffTotals: GitDiffTotals?,
    isLoadingRepoDiff: Boolean,
    canViewRepoDiff: Boolean,
    showsDiscardRuntimeRecovery: Boolean,
    isGitActionEnabled: Boolean,
    isGitInitialized: Boolean,
    onSelect: (TurnGitActionKind) -> Unit,
) {
    RemodexDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier.widthIn(min = 272.dp),
    ) {
        GitActionMenuSectionHeader(stringResource(R.string.git_action_section_changes))
        GitActionChangesMenuItem(
            totals = repoDiffTotals,
            isLoading = isLoadingRepoDiff,
            enabled = canViewRepoDiff,
            onClick = { onSelect(TurnGitActionKind.viewRepositoryDiff) },
        )
        HorizontalDivider()
        GitActionMenuSectionHeader(stringResource(R.string.git_action_section_write))
        GitActionMenuItem(
            label = TurnGitActionKind.commit.title,
            useGitNodeIcon = true,
            onClick = { onSelect(TurnGitActionKind.commit) },
            enabled = isGitActionEnabled,
        )
        GitActionMenuItem(
            label = TurnGitActionKind.push.title,
            iconRes = LucideR.drawable.lucide_ic_arrow_up,
            onClick = { onSelect(TurnGitActionKind.push) },
            enabled = isGitActionEnabled,
        )
        GitActionMenuItem(
            label = TurnGitActionKind.commitAndPush.title,
            iconRes = LucideR.drawable.lucide_ic_cloud,
            onClick = { onSelect(TurnGitActionKind.commitAndPush) },
            enabled = isGitActionEnabled,
        )
        GitActionMenuItem(
            label = stringResource(R.string.git_action_commit_push_pr),
            iconRes = LucideR.drawable.lucide_ic_github,
            onClick = { onSelect(TurnGitActionKind.commitPushAndPullRequest) },
            enabled = isGitActionEnabled,
        )
        GitActionMenuItem(
            label = TurnGitActionKind.createPR.title,
            iconRes = LucideR.drawable.lucide_ic_github,
            onClick = { onSelect(TurnGitActionKind.createPR) },
            enabled = isGitActionEnabled,
        )
        HorizontalDivider()
        GitActionMenuSectionHeader(stringResource(R.string.git_action_section_update))
        GitActionMenuItem(
            label = TurnGitActionKind.syncNow.title,
            iconRes = LucideR.drawable.lucide_ic_refresh_cw,
            onClick = { onSelect(TurnGitActionKind.syncNow) },
            enabled = isGitActionEnabled,
        )
        if (!isGitInitialized) {
            GitActionMenuItem(
                label = TurnGitActionKind.initialize.title,
                iconRes = LucideR.drawable.lucide_ic_git_branch,
                onClick = { onSelect(TurnGitActionKind.initialize) },
                enabled = isGitActionEnabled,
            )
        }
        if (showsDiscardRuntimeRecovery) {
            HorizontalDivider()
            GitActionMenuSectionHeader(stringResource(R.string.git_action_section_recovery))
            GitActionMenuItem(
                label = TurnGitActionKind.discardRuntimeChangesAndSync.title,
                iconRes = LucideR.drawable.lucide_ic_trash_2,
                onClick = { onSelect(TurnGitActionKind.discardRuntimeChangesAndSync) },
                enabled = isGitActionEnabled,
                labelColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun GitActionMenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun GitActionMenuItem(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    @DrawableRes iconRes: Int? = null,
    useGitNodeIcon: Boolean = false,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = if (enabled) labelColor else labelColor.copy(alpha = 0.42f),
            )
        },
        leadingIcon = {
            GitActionMenuLeadingIcon(
                iconRes = iconRes,
                useGitNodeIcon = useGitNodeIcon,
                enabled = enabled,
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun GitActionMenuLeadingIcon(
    @DrawableRes iconRes: Int?,
    useGitNodeIcon: Boolean,
    enabled: Boolean,
) {
    val tint =
        MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (enabled) 0.92f else 0.42f,
        )
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        when {
            useGitNodeIcon ->
                GitNodeConnectorIcon(
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            iconRes != null ->
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
        }
    }
}

@Composable
private fun GitActionChangesMenuItem(
    totals: GitDiffTotals?,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else if (totals != null) {
                    GitActionDiffTotalsRow(totals = totals, enabled = enabled)
                }
            }
        },
        leadingIcon = {
            Icon(
                painter = painterResource(LucideR.drawable.lucide_ic_file_diff),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint =
                    MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (enabled) 0.92f else 0.42f,
                    ),
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
private fun GitActionDiffTotalsRow(
    totals: GitDiffTotals,
    enabled: Boolean,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val labelAlpha = if (enabled) 1f else 0.42f
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+${totals.additions}",
            style = MaterialTheme.typography.labelLarge,
            color = RemodexGitAddition.copy(alpha = labelAlpha),
        )
        Text(
            text = "-${totals.deletions}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error.copy(alpha = labelAlpha),
        )
        if (totals.binaryFiles > 0) {
            Text(
                text = "B${totals.binaryFiles}",
                style = MaterialTheme.typography.labelMedium,
                color = muted.copy(alpha = labelAlpha),
            )
        }
    }
}
