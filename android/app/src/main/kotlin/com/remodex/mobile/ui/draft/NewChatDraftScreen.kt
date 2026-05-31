package com.remodex.mobile.ui.draft

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.remodex.mobile.R
import com.remodex.mobile.core.model.CodexAccessMode
import com.remodex.mobile.core.model.CodexCollaborationModeKind
import com.remodex.mobile.core.model.CodexServiceTier
import com.remodex.mobile.core.transport.ConnectionState
import com.remodex.mobile.data.CodexRepository
import com.remodex.mobile.data.WorktreeNewChatDefaults
import com.remodex.mobile.services.GitActionsService
import com.remodex.mobile.services.ProjectFolderService
import com.remodex.mobile.services.SubscriptionService
import com.remodex.mobile.ui.agent.truncatePathMiddle
import com.remodex.mobile.ui.sidebar.SidebarProjectPickerSheet
import com.remodex.mobile.ui.theme.remodexScreenTopAppBarColors
import com.remodex.mobile.ui.turn.TurnComposerAttachment
import com.remodex.mobile.ui.turn.TurnComposerBar
import com.remodex.mobile.ui.turn.TurnComposerEvent
import com.remodex.mobile.ui.turn.TurnComposerModel
import com.remodex.mobile.ui.turn.TurnComposerReducer
import com.remodex.mobile.ui.turn.ReasoningEffortTitleStrings
import com.remodex.mobile.ui.turn.TurnVoicePhase
import com.remodex.mobile.ui.turn.buildRuntimeControlsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatDraftScreen(
    route: NewChatDraftRoute,
    repository: CodexRepository,
    subscriptionService: SubscriptionService,
    onNavigateBack: () -> Unit,
    onThreadStarted: () -> Unit,
    onOpenTerminal: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onNavigateBack)
    val scope = rememberCoroutineScope()
    val ready by repository.isSessionReady.collectAsStateWithLifecycle()
    val conn by repository.connectionState.collectAsStateWithLifecycle()
    val threads by repository.threads.collectAsStateWithLifecycle()
    val activeThreadId by repository.activeThreadId.collectAsStateWithLifecycle()
    val availableModels by repository.availableModels.collectAsStateWithLifecycle()
    val isLoadingModels by repository.isLoadingModels.collectAsStateWithLifecycle()
    val selectedModelId by repository.selectedModelId.collectAsStateWithLifecycle()
    val selectedReasoningEffort by repository.selectedReasoningEffort.collectAsStateWithLifecycle()
    val selectedAccessMode by repository.selectedAccessMode.collectAsStateWithLifecycle()
    val selectedServiceTier by repository.selectedServiceTier.collectAsStateWithLifecycle()
    val projectFolderService = remember(repository) { ProjectFolderService(repository) }

    var draft by remember(route.id) { mutableStateOf("") }
    var attachments by remember(route.id) { mutableStateOf<List<TurnComposerAttachment>>(emptyList()) }
    var isPlanModeEnabled by remember(route.id) { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }
    var gitStatusMessage by remember { mutableStateOf<String?>(null) }
    var isLoadingGitStatus by remember { mutableStateOf(false) }
    var selectedProjectPath by remember(route.id) {
        mutableStateOf(
            when {
                route.source.isFromGeneralChat && route.preferredProjectPath == null -> null
                else -> route.preferredProjectPath ?: WorktreeNewChatDefaults.baseProjectPath(activeThreadId, threads)
            },
        )
    }
    var showProjectPicker by remember { mutableStateOf(false) }
    var showThreadMenu by remember { mutableStateOf(false) }

    val hasSelectedProject = !selectedProjectPath.isNullOrBlank()
    val isConnected = conn is ConnectionState.Connected
    val canUseGitActions =
        NewChatDraftLogic.gitActionsEnabled(
            projectPath = selectedProjectPath,
            isConnected = isConnected,
        )
    val pathSubtitle =
        remember(selectedProjectPath) {
            selectedProjectPath?.let { truncatePathMiddle(it) }
        }
    val runtimeLoadingLabel = stringResource(R.string.turn_runtime_loading)

    LaunchedEffect(ready, conn) {
        if (ready && conn is ConnectionState.Connected) {
            runCatching { repository.refreshModels() }
        }
    }

    LaunchedEffect(selectedProjectPath) {
        gitStatusMessage = null
    }

    val runtimeControls =
        remember(
            availableModels,
            isLoadingModels,
            selectedModelId,
            selectedReasoningEffort,
            selectedAccessMode,
            selectedServiceTier,
        ) {
            buildRuntimeControlsState(
                models = availableModels,
                isLoadingModels = isLoadingModels,
                selectedModelId = selectedModelId,
                selectedReasoningEffort = selectedReasoningEffort,
                selectedAccessMode = selectedAccessMode,
                selectedServiceTier = selectedServiceTier,
                loadingLabel = runtimeLoadingLabel,
                modelFallbackLabel = "Model",
                noModelsLabel = "No models",
                autoLabel = "Auto",
                normalTierLabel = "Normal",
                reasoningEffortTitles =
                    ReasoningEffortTitleStrings(
                        low = "Low",
                        medium = "Medium",
                        high = "High",
                        xhigh = "Extra high",
                    ),
            )
        }

    val composerModel =
        remember(ready, sending, draft, attachments) {
            listOf(
                TurnComposerEvent.SetEnabled(ready),
                TurnComposerEvent.SetSending(sending),
                TurnComposerEvent.SetDraftText(draft),
                TurnComposerEvent.SetReadyAttachmentCount(0),
                TurnComposerEvent.SetHasBlockingAttachments(false),
                TurnComposerEvent.SetVoicePhase(TurnVoicePhase.Idle),
                TurnComposerEvent.SetThreadRunning(false),
                TurnComposerEvent.SetTranscribing(false),
            ).fold(TurnComposerModel()) { state, event ->
                TurnComposerReducer.reduce(state, event)
            }
        }

    fun sendDraft() {
        if (!composerModel.derive().canSend || sending) return
        if (subscriptionService.isConfigured && !subscriptionService.hasFreeSendAccess) {
            sendError = "Subscription required to send more messages."
            return
        }
        val text = draft.trim()
        if (text.isEmpty()) return
        sending = true
        sendError = null
        scope.launch {
            runCatching {
                val requestedCwd =
                    selectedProjectPath?.trim()?.takeIf { it.isNotEmpty() }
                        ?: if (route.source.isFromGeneralChat) {
                            projectFolderService.createRootlessChatRoot(text)
                        } else {
                            null
                        }
                val thread =
                    repository.startThread(
                        cwd = requestedCwd,
                    )
                repository.startTurn(
                    threadId = thread.id,
                    text = text,
                    collaborationMode =
                        if (isPlanModeEnabled) {
                            CodexCollaborationModeKind.plan
                        } else {
                            null
                        },
                )
                repository.setActiveThreadId(thread.id)
                if (subscriptionService.isConfigured) {
                    subscriptionService.recordFreeSendAttempt()
                }
                onThreadStarted()
            }.onFailure { error ->
                sendError = error.message?.takeIf { it.isNotBlank() } ?: error.javaClass.simpleName
                sending = false
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.new_chat_draft_title))
                        pathSubtitle?.let { subtitle ->
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                colors = remodexScreenTopAppBarColors(),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!route.source.isFromGeneralChat && hasSelectedProject) {
                        TextButton(onClick = { showProjectPicker = true }) {
                            Text(stringResource(R.string.new_chat_draft_change_project))
                        }
                    }
                    Box {
                        IconButton(onClick = { showThreadMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showThreadMenu,
                            onDismissRequest = { showThreadMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_chat_draft_open_terminal)) },
                                onClick = {
                                    showThreadMenu = false
                                    onOpenTerminal(selectedProjectPath)
                                },
                                enabled = hasSelectedProject,
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_chat_draft_git_status)) },
                                onClick = {
                                    showThreadMenu = false
                                    if (!canUseGitActions || isLoadingGitStatus) return@DropdownMenuItem
                                    val projectPath = selectedProjectPath
                                    isLoadingGitStatus = true
                                    gitStatusMessage = null
                                    scope.launch {
                                        runCatching {
                                            GitActionsService(repository, projectPath).status()
                                        }.onSuccess { status ->
                                            gitStatusMessage = NewChatDraftLogic.gitStatusSummary(status)
                                        }.onFailure { error ->
                                            gitStatusMessage =
                                                error.message?.takeIf { it.isNotBlank() }
                                                    ?: error.javaClass.simpleName
                                        }
                                        isLoadingGitStatus = false
                                    }
                                },
                                enabled = canUseGitActions && !isLoadingGitStatus,
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding(),
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.new_chat_draft_prompt),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (route.source.isFromGeneralChat && !hasSelectedProject) {
                        Text(
                            text = stringResource(R.string.new_chat_draft_quick_chat_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    sendError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    gitStatusMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            TurnComposerBar(
                draft = draft,
                attachments = attachments,
                model = composerModel,
                isPlanModeEnabled = isPlanModeEnabled,
                runtimeControls = runtimeControls,
                onDraftChange = { draft = it },
                onPickImages = {},
                onPickFiles = {},
                onTakePhoto = {},
                onSetPlanModeEnabled = { isPlanModeEnabled = it },
                onSelectModel = { option -> scope.launch { repository.setSelectedModelId(option.id) } },
                onSelectReasoningEffort = { option -> scope.launch { repository.setSelectedReasoningEffort(option.id) } },
                onSelectAccessMode = { option ->
                    scope.launch {
                        repository.setSelectedAccessMode(
                            CodexAccessMode.entries.firstOrNull { it.name == option.id }
                                ?: CodexAccessMode.onRequest,
                        )
                    }
                },
                onSelectServiceTier = { option ->
                    scope.launch {
                        repository.setSelectedServiceTier(
                            CodexServiceTier.entries.firstOrNull { it.name == option.id },
                        )
                    }
                },
                onRemoveAttachment = { id -> attachments = attachments.filterNot { it.id == id } },
                onSend = ::sendDraft,
                composerEnvironment = {
                    if (hasSelectedProject) {
                        Text(
                            text = pathSubtitle.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }

    SidebarProjectPickerSheet(
        repository = repository,
        visible = showProjectPicker,
        initialPath = selectedProjectPath ?: WorktreeNewChatDefaults.baseProjectPath(activeThreadId, threads),
        onDismiss = { showProjectPicker = false },
        onStartBusyChange = {},
        onStartThread = { cwd ->
            selectedProjectPath = cwd?.trim()?.takeIf { it.isNotEmpty() }
            showProjectPicker = false
        },
        initialFoldersCollapsed = route.source == NewChatDraftSource.folderChat,
        threads = threads,
        activeThreadId = activeThreadId,
        activeChatMetadata = null,
        onThreadStarted = { showProjectPicker = false },
    )
}
