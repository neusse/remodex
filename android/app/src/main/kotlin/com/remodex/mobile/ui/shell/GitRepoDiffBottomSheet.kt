package com.remodex.mobile.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import android.content.ClipData
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.remodex.mobile.R
import com.remodex.mobile.core.model.AIUnifiedPatchParser
import com.remodex.mobile.core.model.GitRepoSyncResult
import com.remodex.mobile.data.RepoDiffLastTurnFileRow
import com.remodex.mobile.ui.agent.truncatePathMiddle
import com.remodex.mobile.ui.sidebar.RemodexCircleIconButton
import com.remodex.mobile.ui.sidebar.SidebarColorPalette
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette
import com.remodex.mobile.ui.theme.RemodexModalBottomSheet
import kotlinx.coroutines.launch
import com.remodex.mobile.ui.turn.RepoMarkdownFileLink
import kotlinx.coroutines.delay

enum class GitRepoDiffScope {
    LastTurn,
    FullWorkingTree,
}

private data class GitRepoDiffRenderableRow(
    val stableKey: String,
    val displayPath: String,
    val chunk: String,
)

private enum class GitRepoDiffUiTab(
    val stringRes: Int,
) {
    Summary(R.string.git_repo_diff_tab_summary),
    Review(R.string.git_repo_diff_tab_review),
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun GitRepoDiffBottomSheet(
    visible: Boolean,
    scope: GitRepoDiffScope,
    onScopeChange: (GitRepoDiffScope) -> Unit,
    lastTurnRows: List<RepoDiffLastTurnFileRow>,
    fullTreePatch: String,
    isFullTreeLoading: Boolean,
    fullTreeError: String?,
    gitStatus: GitRepoSyncResult?,
    focusPathQuery: String?,
    onFocusPathQueryConsumed: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(sheetState) {
        sheetState.expand()
    }
    RemodexModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        var selectedTabIx by remember { mutableIntStateOf(GitRepoDiffUiTab.Review.ordinal) }
        val selectedTab =
            GitRepoDiffUiTab.entries.getOrElse(selectedTabIx) { GitRepoDiffUiTab.Review }
        var pathCopyDialogPath by remember { mutableStateOf<String?>(null) }
        val clipboard = LocalClipboard.current
        val copyScope = rememberCoroutineScope()

        val trimmedFull = remember(fullTreePatch) { fullTreePatch.trim() }
        val rows =
            remember(scope, trimmedFull, lastTurnRows) {
                when (scope) {
                    GitRepoDiffScope.LastTurn ->
                        lastTurnRows.map {
                            GitRepoDiffRenderableRow(
                                stableKey = it.stableKey,
                                displayPath = it.path,
                                chunk = it.chunk,
                            )
                        }
                    GitRepoDiffScope.FullWorkingTree ->
                        if (trimmedFull.isBlank()) {
                            emptyList()
                        } else {
                            AIUnifiedPatchParser.splitUnifiedPatchIntoFileChunks(trimmedFull).mapIndexed {
                                    i,
                                    pair,
                                ->
                                GitRepoDiffRenderableRow(
                                    stableKey = "full:$i:${pair.first}",
                                    displayPath = pair.first,
                                    chunk = pair.second,
                                )
                            }
                        }
                }
            }

        val reviewLazyListState = rememberLazyListState()
        var markdownExpandRowStableKey by remember { mutableStateOf<String?>(null) }
        val onFocusConsumedUpdated = rememberUpdatedState(onFocusPathQueryConsumed)

        LaunchedEffect(visible) {
            if (!visible) {
                markdownExpandRowStableKey = null
            }
        }

        LaunchedEffect(focusPathQuery, rows, trimmedFull, isFullTreeLoading, scope) {
            val q = focusPathQuery?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect

            selectedTabIx = GitRepoDiffUiTab.Review.ordinal

            val waitingPatch =
                scope == GitRepoDiffScope.FullWorkingTree &&
                    trimmedFull.isBlank() &&
                    isFullTreeLoading
            if (waitingPatch) {
                return@LaunchedEffect
            }

            delay(48)

            val ix =
                rows.indexOfFirst {
                    RepoMarkdownFileLink.rowMatchesQuery(it.displayPath, q)
                }
            if (ix >= 0) {
                markdownExpandRowStableKey = rows[ix].stableKey
                reviewLazyListState.animateScrollToItem(ix)
                delay(50)
            } else {
                markdownExpandRowStableKey = null
            }
            onFocusConsumedUpdated.value()
        }

        val editSessionKey =
            remember(scope, rows) {
                buildString {
                    append(scope.name)
                    rows.forEach { row ->
                        append('|')
                        append(row.stableKey)
                        append(':')
                        append(row.chunk.hashCode())
                    }
                }
            }
        val edits = remember { mutableStateMapOf<String, TextFieldValue>() }
        LaunchedEffect(editSessionKey) {
            edits.clear()
        }

        val colors = rememberSidebarColorPalette()

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.git_repo_diff_sheet_title),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f),
                )
                RemodexCircleIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(android.R.string.ok),
                    colors = colors,
                ) {
                    Icon(
                        painter = painterResource(LucideR.drawable.lucide_ic_x),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.primaryText,
                    )
                }
            }

            GitRepoDiffTabRow(
                selectedTab = selectedTab,
                onSelected = { selectedTabIx = it.ordinal },
                colors = colors,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                GitRepoDiffScopeChip(
                    selected = scope == GitRepoDiffScope.LastTurn,
                    onClick = {
                        edits.clear()
                        onScopeChange(GitRepoDiffScope.LastTurn)
                    },
                    iconRes = LucideR.drawable.lucide_ic_clock,
                    label = stringResource(R.string.git_repo_diff_scope_last_turn),
                    colors = colors,
                )
                GitRepoDiffScopeChip(
                    selected = scope == GitRepoDiffScope.FullWorkingTree,
                    onClick = {
                        edits.clear()
                        onScopeChange(GitRepoDiffScope.FullWorkingTree)
                    },
                    iconRes = LucideR.drawable.lucide_ic_git_branch,
                    label = stringResource(R.string.git_repo_diff_scope_full_tree),
                    colors = colors,
                )
            }

            when {
                scope == GitRepoDiffScope.FullWorkingTree && fullTreeError != null ->
                    GitRepoDiffMessage(
                        text = fullTreeError,
                        isError = true,
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )

                scope == GitRepoDiffScope.FullWorkingTree && isFullTreeLoading && fullTreePatch.isBlank() ->
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                            color = colors.accent,
                        )
                    }

                rows.isEmpty() && scope == GitRepoDiffScope.LastTurn ->
                    GitRepoDiffMessage(
                        text = stringResource(R.string.git_repo_diff_empty_last_turn),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )

                rows.isEmpty() ->
                    GitRepoDiffMessage(
                        text = stringResource(R.string.git_repo_diff_empty),
                        colors = colors,
                        modifier = Modifier.weight(1f),
                    )

                else ->
                    GitRepoDiffContent(
                        uiTab = selectedTab,
                        rows = rows,
                        gitStatus = gitStatus,
                        edits = edits,
                        reviewLazyListState = reviewLazyListState,
                        markdownExpandRowStableKey = markdownExpandRowStableKey,
                        onFilePathClick = { pathCopyDialogPath = it },
                        colors = colors,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                    )
            }
        }

        pathCopyDialogPath?.let { fullPath ->
            GitRepoDiffPathCopyDialog(
                fullPath = fullPath,
                onDismiss = { pathCopyDialogPath = null },
                onCopy = {
                    copyScope.launch {
                        clipboard.setClipEntry(
                            ClipData.newPlainText("repo-diff-file-path", fullPath).toClipEntry(),
                        )
                        pathCopyDialogPath = null
                    }
                },
            )
        }
    }
}

@Composable
private fun GitRepoDiffTabRow(
    selectedTab: GitRepoDiffUiTab,
    onSelected: (GitRepoDiffUiTab) -> Unit,
    colors: SidebarColorPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GitRepoDiffUiTab.entries.forEach { tab ->
            GitRepoDiffTabChip(
                label = stringResource(tab.stringRes),
                selected = tab == selectedTab,
                colors = colors,
                onClick = { onSelected(tab) },
            )
        }
    }
}

@Composable
private fun GitRepoDiffTabChip(
    label: String,
    selected: Boolean,
    colors: SidebarColorPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier =
            modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) colors.accent else colors.surface)
                .border(1.dp, if (selected) colors.accent else colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            color = if (selected) colors.onAccent else colors.primaryText,
        )
    }
}

@Composable
private fun GitRepoDiffScopeChip(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier =
            modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) colors.selectedRow else colors.surface)
                .border(1.dp, if (selected) colors.accent.copy(alpha = 0.55f) else colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (selected) colors.accent else colors.secondaryText,
        )
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 12.sp,
                ),
            color = if (selected) colors.primaryText else colors.secondaryText,
            maxLines = 1,
        )
    }
}

@Composable
private fun GitRepoDiffMessage(
    text: String,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color =
                if (isError) {
                    colors.red
                } else {
                    colors.secondaryText
                },
        )
    }
}

@Composable
private fun GitRepoDiffContent(
    uiTab: GitRepoDiffUiTab,
    rows: List<GitRepoDiffRenderableRow>,
    gitStatus: GitRepoSyncResult?,
    edits: SnapshotStateMap<String, TextFieldValue>,
    reviewLazyListState: LazyListState,
    markdownExpandRowStableKey: String?,
    onFilePathClick: (String) -> Unit,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    when (uiTab) {
        GitRepoDiffUiTab.Summary ->
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.stableKey }) { row ->
                    GitRepoDiffSummaryRow(
                        path = row.displayPath,
                        chunk = row.chunk,
                        gitStatus = gitStatus,
                        onFilePathClick = onFilePathClick,
                        colors = colors,
                    )
                }
            }
        GitRepoDiffUiTab.Review ->
            LazyColumn(
                modifier = modifier,
                state = reviewLazyListState,
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.stableKey }) { row ->
                    GitRepoDiffExpandableFile(
                        rowKey = row.stableKey,
                        path = row.displayPath,
                        chunk = row.chunk,
                        gitStatus = gitStatus,
                        edits = edits,
                        onFilePathClick = onFilePathClick,
                        colors = colors,
                        expandForMarkdownFocus =
                            markdownExpandRowStableKey != null &&
                                row.stableKey == markdownExpandRowStableKey,
                    )
                }
            }
    }
}

@Composable
private fun GitRepoDiffSummaryRow(
    path: String,
    chunk: String,
    gitStatus: GitRepoSyncResult?,
    onFilePathClick: (String) -> Unit,
    colors: SidebarColorPalette,
) {
    val (adds, dels) =
        remember(chunk) { AIUnifiedPatchParser.additionsDeletionsForDisplay(chunk) }
    val gitFile = remember(path, gitStatus) { findGitStatusForPatchPath(path, gitStatus?.files.orEmpty()) }
    val staging = gitFile?.let { GitPathStagingUi.fromPorcelain(it.status) }
    val cardShape = RoundedCornerShape(12.dp)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.border, cardShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        GitRepoDiffFileTile(colors = colors)
        GitRepoDiffFilePathBlock(
            path = path,
            onPathClick = onFilePathClick,
            colors = colors,
            modifier = Modifier.weight(1f),
        )
        staging?.let { GitRepoDiffStagingBadge(staging = it, colors = colors) }
        GitRepoDiffStatText(text = "+$adds", positive = true, colors = colors)
        GitRepoDiffStatText(text = "-$dels", positive = false, colors = colors)
    }
}

@Composable
private fun GitRepoDiffFileTile(colors: SidebarColorPalette) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier =
            Modifier
                .size(36.dp)
                .clip(shape)
                .background(colors.background)
                .border(1.dp, colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(LucideR.drawable.lucide_ic_file_diff),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = colors.secondaryText,
        )
    }
}

@Composable
private fun GitRepoDiffStatText(
    text: String,
    positive: Boolean,
    colors: SidebarColorPalette,
) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        color = if (positive) colors.green else colors.red,
        maxLines = 1,
    )
}

@Composable
private fun GitRepoDiffFilePathBlock(
    path: String,
    onPathClick: (String) -> Unit,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    val fileName = remember(path) { fileNameFromRepoPath(path) }
    Column(
        modifier =
            modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onPathClick(path) },
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = fileName,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
            color = colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = truncatePathMiddle(path),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    lineHeight = 13.sp,
                    fontSize = 11.sp,
                ),
            fontFamily = FontFamily.Monospace,
            color = colors.mutedText,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis,
        )
    }
}

@Composable
private fun GitRepoDiffPathCopyDialog(
    fullPath: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.turn_thread_path_dialog_title)) },
        text = {
            SelectionContainer {
                Text(
                    text = fullPath,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.turn_thread_path_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

private fun fileNameFromRepoPath(path: String): String =
    path
        .replace('\\', '/')
        .substringAfterLast('/')
        .ifBlank { path }

@Composable
private fun GitRepoDiffStagingBadge(
    staging: GitPathStagingUi,
    colors: SidebarColorPalette,
) {
    val label =
        when {
            staging.isUntracked -> stringResource(R.string.git_repo_diff_staging_untracked)
            staging.staged && staging.unstaged -> stringResource(R.string.git_repo_diff_staging_mixed)
            staging.staged && !staging.unstaged -> stringResource(R.string.git_repo_diff_staging_staged_only)
            !staging.staged && staging.unstaged -> stringResource(R.string.git_repo_diff_staging_unstaged_only)
            else -> null
        } ?: return
    val shape = RoundedCornerShape(999.dp)
    Text(
        text = label,
        style =
            MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
        color = colors.secondaryText,
        modifier =
            Modifier
                .clip(shape)
                .background(colors.background)
                .border(1.dp, colors.border, shape)
                .padding(horizontal = 7.dp, vertical = 3.dp),
        maxLines = 1,
    )
}

@Composable
private fun GitRepoDiffExpandableFile(
    rowKey: String,
    path: String,
    chunk: String,
    gitStatus: GitRepoSyncResult?,
    edits: SnapshotStateMap<String, TextFieldValue>,
    onFilePathClick: (String) -> Unit,
    colors: SidebarColorPalette,
    expandForMarkdownFocus: Boolean = false,
) {
    var expanded by remember(rowKey) { mutableStateOf(false) }
    LaunchedEffect(expandForMarkdownFocus) {
        if (expandForMarkdownFocus) expanded = true
    }
    /** When [chunkLooksLikeUnifiedDiff], Preview shows [GitPatchHighlightedBlock]; Edit is a plain text patch editor. */
    var diffPatchEditMode by remember(rowKey) { mutableStateOf(false) }
    val patchBodyForStats = edits[rowKey]?.text ?: chunk
    val (adds, dels) =
        remember(patchBodyForStats) {
            AIUnifiedPatchParser.additionsDeletionsForDisplay(patchBodyForStats)
        }
    val gitFile = remember(path, gitStatus) { findGitStatusForPatchPath(path, gitStatus?.files.orEmpty()) }
    val staging = gitFile?.let { GitPathStagingUi.fromPorcelain(it.status) }
    val cardShape = RoundedCornerShape(12.dp)
    val patchPanelShape = RoundedCornerShape(10.dp)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.border, cardShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GitRepoDiffFileTile(colors = colors)
            GitRepoDiffFilePathBlock(
                path = path,
                onPathClick = onFilePathClick,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
            staging?.let { GitRepoDiffStagingBadge(staging = it, colors = colors) }
            GitRepoDiffStatText(text = "+$adds", positive = true, colors = colors)
            GitRepoDiffStatText(text = "-$dels", positive = false, colors = colors)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = colors.mutedText,
                modifier = Modifier.size(18.dp),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                when {
                    chunkIsTimelinePlaceholderEcho(chunk) ->
                        Text(
                            text = stringResource(R.string.git_repo_diff_timeline_no_real_patch),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = colors.secondaryText,
                            modifier = Modifier.padding(4.dp),
                        )
                    chunkLooksLikeUnifiedDiff(chunk) ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                GitRepoDiffPatchModeButton(
                                    selected = !diffPatchEditMode,
                                    onClick = { diffPatchEditMode = false },
                                    iconRes = LucideR.drawable.lucide_ic_eye,
                                    label = stringResource(R.string.git_repo_diff_mode_preview),
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                )
                                GitRepoDiffPatchModeButton(
                                    selected = diffPatchEditMode,
                                    onClick = { diffPatchEditMode = true },
                                    iconRes = LucideR.drawable.lucide_ic_square_pen,
                                    label = stringResource(R.string.git_repo_diff_mode_edit),
                                    colors = colors,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            val patchText = edits[rowKey]?.text ?: chunk
                            if (!diffPatchEditMode) {
                                GitPatchHighlightedBlock(
                                    patch = patchText,
                                    verticalScrollEnabled = false,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(patchPanelShape)
                                            .border(1.dp, colors.border, patchPanelShape)
                                            .background(colors.background),
                                )
                            } else {
                                val fieldValue =
                                    edits.getOrPut(rowKey) { TextFieldValue(chunk) }
                                DiffPatchTextEditor(
                                    value = fieldValue,
                                    onValueChange = { edits[rowKey] = it },
                                    colors = colors,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 360.dp)
                                            .clip(patchPanelShape)
                                            .border(1.dp, colors.border, patchPanelShape)
                                            .background(colors.background),
                                )
                            }
                        }
                    else -> {
                        val initial = remember(rowKey, chunk) { TextFieldValue(chunk) }
                        val fieldValue = edits[rowKey] ?: initial
                        DiffPatchTextEditor(
                            value = fieldValue,
                            onValueChange = { edits[rowKey] = it },
                            colors = colors,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 360.dp)
                                    .clip(patchPanelShape)
                                    .border(1.dp, colors.border, patchPanelShape)
                                    .background(colors.background),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GitRepoDiffPatchModeButton(
    selected: Boolean,
    onClick: () -> Unit,
    iconRes: Int,
    label: String,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Row(
        modifier =
            modifier
                .height(32.dp)
                .clip(shape)
                .background(if (selected) colors.accent else colors.surface)
                .border(1.dp, if (selected) colors.accent else colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (selected) colors.onAccent else colors.secondaryText,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                ),
            color = if (selected) colors.onAccent else colors.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DiffPatchTextEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    colors: SidebarColorPalette,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle =
            TextStyle(
                fontFamily = FontFamily.Monospace,
                color = colors.primaryText,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
            ),
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(8.dp),
    )
}

private fun chunkLooksLikeUnifiedDiff(chunk: String): Boolean {
    val t = chunk.trim().replace("\r\n", "\n")
    if (t.isEmpty()) return false
    if (chunkIsTimelinePlaceholderEcho(t)) return false
    return t.startsWith("diff --git ") ||
        t.lineSequence().any { it.startsWith("@@ ") } ||
        (t.contains("--- ") && t.contains("+++ "))
}

private fun chunkIsTimelinePlaceholderEcho(chunk: String): Boolean {
    val lines =
        chunk
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
    return lines.isNotEmpty() && lines.all { it == "[file change]" }
}
