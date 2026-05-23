package com.remodex.mobile.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remodex.mobile.AppContainer
import com.remodex.mobile.ui.sidebar.RemodexCircleIconButton
import com.remodex.mobile.ui.sidebar.rememberSidebarColorPalette
import com.remodex.mobile.ui.sidebar.remodexFlatControlChrome
import com.remodex.mobile.ui.theme.RemodexPopupSurface
import kotlinx.coroutines.launch

@Composable
fun TerminalRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TerminalViewModel =
        viewModel(
            factory =
                TerminalViewModel.Factory(
                    profileRepository = AppContainer.terminalProfileRepository,
                    trustedHostRepository = AppContainer.terminalTrustedHostRepository,
                ),
        )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val missionScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        AppContainer.betaEngagementRepository.recordMissionEvent(
            eventType = "terminal_opened",
            screen = "terminal",
        )
    }
    TerminalScreen(
        state = state,
        output = viewModel.outputForSession(state.selectedSessionId),
        onCreateProfile = viewModel::startCreateProfile,
        onEditProfile = viewModel::startEditProfile,
        onDeleteProfile = viewModel::deleteProfile,
        onOpenProfile = viewModel::openProfile,
        onShowProfiles = viewModel::showProfiles,
        onSelectSession = viewModel::selectSession,
        onCloseSession = viewModel::closeSession,
        onCancelEditor = viewModel::cancelEditor,
        onSaveEditor = {
            if (viewModel.saveEditor()) {
                missionScope.launch {
                    AppContainer.betaEngagementRepository.recordMissionEvent(
                        eventType = "terminal_profile_saved",
                        screen = "terminal",
                    )
                }
            }
        },
        onEditorLabelChange = viewModel::updateEditorLabel,
        onEditorHostChange = viewModel::updateEditorHost,
        onEditorPortChange = viewModel::updateEditorPort,
        onEditorUsernameChange = viewModel::updateEditorUsername,
        onEditorPrivateKeyChange = viewModel::updateEditorPrivateKey,
        onEditorAllowUnencryptedKeyChange = viewModel::updateEditorAllowUnencryptedKey,
        onPassphraseChange = viewModel::updatePassphrase,
        onSelectedProfileAllowUnencryptedKeyChange = viewModel::updateSelectedProfileAllowUnencryptedKey,
        onConnect = {
            viewModel.connect()
            missionScope.launch {
                AppContainer.betaEngagementRepository.recordMissionEvent(
                    eventType = "terminal_connect_started",
                    screen = "terminal",
                )
            }
        },
        onTrustHost = viewModel::trustPendingHostAndConnect,
        onDisconnect = viewModel::disconnect,
        onCloseSelectedSession = viewModel::closeSelectedSession,
        onSend = viewModel::send,
        onResize = viewModel::resize,
        onClear = viewModel::clearTerminal,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    state: TerminalState,
    output: kotlinx.coroutines.flow.Flow<ByteArray>,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onShowProfiles: () -> Unit,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onCancelEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    onEditorLabelChange: (String) -> Unit,
    onEditorHostChange: (String) -> Unit,
    onEditorPortChange: (String) -> Unit,
    onEditorUsernameChange: (String) -> Unit,
    onEditorPrivateKeyChange: (String) -> Unit,
    onEditorAllowUnencryptedKeyChange: (Boolean) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onSelectedProfileAllowUnencryptedKeyChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onTrustHost: () -> Unit,
    onDisconnect: () -> Unit,
    onCloseSelectedSession: () -> Unit,
    onSend: (ByteArray) -> Unit,
    onResize: (TerminalSize) -> Unit,
    onClear: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedProfile = state.selectedProfile
    val selectedSession = state.selectedSession
    val connected = selectedSession?.status == TerminalStatus.Connected
    var showSessions by remember { mutableStateOf(false) }
    var showWindowsHelp by remember { mutableStateOf(false) }
    val colors = rememberSidebarColorPalette()
    val title =
        when (state.surface) {
            TerminalSurface.Profiles -> "Terminal"
            TerminalSurface.Editor -> if (state.editor?.id == null) "New profile" else "Edit profile"
            TerminalSurface.Session -> selectedProfile?.displayLabel ?: "Terminal"
        }
    val onBack =
        when (state.surface) {
            TerminalSurface.Profiles -> onNavigateBack
            TerminalSurface.Editor -> onCancelEditor
            TerminalSurface.Session -> onCloseSelectedSession
        }

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                            color = colors.primaryText,
                        )
                        if (state.surface == TerminalSurface.Session && selectedProfile != null) {
                            Text(
                                text = "${selectedSession?.status?.name ?: TerminalStatus.Idle.name} - ${selectedProfile.username}@${selectedProfile.host}:${selectedProfile.port}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryText,
                            )
                        }
                    }
                },
                navigationIcon = {
                    RemodexCircleIconButton(
                        onClick = onBack,
                        contentDescription = "Back",
                        colors = colors,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.primaryText,
                        )
                    }
                },
                actions = {
                    if (state.surface == TerminalSurface.Profiles) {
                        TerminalTopBarAction(
                            text = "Help",
                            onClick = { showWindowsHelp = true },
                        )
                    }
                    if (state.surface == TerminalSurface.Session) {
                        TerminalTopBarAction(
                            text = "Help",
                            onClick = { showWindowsHelp = true },
                        )
                        TerminalTopBarAction(
                            text = "Sessions",
                            onClick = { showSessions = !showSessions },
                            emphasis = showSessions,
                        )
                        if (connected || selectedSession?.status == TerminalStatus.Connecting) {
                            TerminalTopBarAction(
                                text = "Disconnect",
                                onClick = onDisconnect,
                                destructive = true,
                            )
                        } else {
                            TerminalTopBarAction(
                                text = "Connect",
                                onClick = onConnect,
                                emphasis = true,
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background,
                        scrolledContainerColor = colors.background,
                    ),
            )
        },
    ) { innerPadding ->
        when (state.surface) {
            TerminalSurface.Profiles ->
                TerminalProfilesContent(
                    profiles = state.profiles,
                    onCreateProfile = onCreateProfile,
                    onEditProfile = onEditProfile,
                    onDeleteProfile = onDeleteProfile,
                    onOpenProfile = onOpenProfile,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            TerminalSurface.Editor ->
                TerminalProfileEditorContent(
                    editor = state.editor ?: TerminalProfileDraft(),
                    errorMessage = state.editorErrorMessage,
                    onLabelChange = onEditorLabelChange,
                    onHostChange = onEditorHostChange,
                    onPortChange = onEditorPortChange,
                    onUsernameChange = onEditorUsernameChange,
                    onPrivateKeyChange = onEditorPrivateKeyChange,
                    onAllowUnencryptedKeyChange = onEditorAllowUnencryptedKeyChange,
                    onSave = onSaveEditor,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                )
            TerminalSurface.Session ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                ) {
                    if (showSessions) {
                        TerminalSessionSidebar(
                            sessions = state.sessions,
                            profiles = state.profiles,
                            selectedSessionId = state.selectedSessionId,
                            onSelectSession = {
                                onSelectSession(it)
                                showSessions = false
                            },
                            onCloseSession = onCloseSession,
                            onShowProfiles = {
                                showSessions = false
                                onShowProfiles()
                            },
                        )
                    }
                    TerminalSessionContent(
                        state = state,
                        output = output,
                        onPassphraseChange = onPassphraseChange,
                        onAllowUnencryptedKeyChange = onSelectedProfileAllowUnencryptedKeyChange,
                        onConnect = onConnect,
                        onTrustHost = onTrustHost,
                        onSend = onSend,
                        onResize = onResize,
                        onClear = onClear,
                        modifier = Modifier.weight(1f).fillMaxSize(),
                    )
                }
        }
    }

    if (showWindowsHelp) {
        Dialog(
            onDismissRequest = { showWindowsHelp = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            RemodexPopupSurface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                panel = true,
            ) {
                Column(
                    modifier =
                        Modifier
                            .padding(18.dp)
                            .heightIn(max = 560.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TerminalWindowsSetupGuide(scrollable = true)
                    TerminalPrimaryButton(
                        text = "Close",
                        onClick = { showWindowsHelp = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalSessionSidebar(
    sessions: List<TerminalSessionSummary>,
    profiles: List<TerminalProfile>,
    selectedSessionId: String?,
    onSelectSession: (String) -> Unit,
    onCloseSession: (String) -> Unit,
    onShowProfiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Column(
        modifier =
            modifier
                .width(120.dp)
                .fillMaxSize()
                .background(colors.background)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TerminalSectionTitle(text = "Sessions")
        sessions.forEach { session ->
            val profile = profiles.firstOrNull { it.id == session.profileId }
            val selected = session.id == selectedSessionId
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .remodexFlatControlChrome(TerminalFieldShape)
                        .then(
                            if (selected) {
                                Modifier.border(1.5.dp, colors.accent, TerminalFieldShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = profile?.displayLabel ?: "Terminal",
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    color = colors.primaryText,
                    maxLines = 2,
                )
                Text(
                    text = session.status.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = if (selected) colors.accent else colors.mutedText,
                )
                TerminalTextAction(
                    text = "Close",
                    onClick = { onCloseSession(session.id) },
                    destructive = true,
                )
            }
        }
        TerminalSecondaryButton(
            text = "+",
            onClick = onShowProfiles,
            enabled = profiles.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TerminalProfilesContent(
    profiles: List<TerminalProfile>,
    onCreateProfile: () -> Unit,
    onEditProfile: (String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TerminalPrimaryButton(
            text = "Create profile",
            onClick = onCreateProfile,
        )
        if (profiles.isEmpty()) {
            TerminalEmptyState(text = "No terminal profiles yet.")
        } else {
            TerminalSectionTitle(text = "Saved profiles")
        }
        profiles.forEach { profile ->
            TerminalProfileCard(
                title = profile.displayLabel,
                subtitle = "${profile.username}@${profile.host}:${profile.port}",
            ) {
                TerminalPrimaryButton(
                    text = "Open",
                    onClick = { onOpenProfile(profile.id) },
                    fullWidth = false,
                )
                TerminalSecondaryButton(text = "Edit", onClick = { onEditProfile(profile.id) })
                TerminalTextAction(
                    text = "Delete",
                    onClick = { onDeleteProfile(profile.id) },
                    destructive = true,
                )
            }
        }
    }
}

@Composable
private fun TerminalProfileEditorContent(
    editor: TerminalProfileDraft,
    errorMessage: String?,
    onLabelChange: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onAllowUnencryptedKeyChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberSidebarColorPalette()
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TerminalFormTextField(
            value = editor.label,
            onValueChange = onLabelChange,
            label = "Profile name (optional)",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TerminalFormTextField(
                value = editor.host,
                onValueChange = onHostChange,
                label = "Host",
                modifier = Modifier.weight(1f),
            )
            TerminalFormTextField(
                value = editor.port,
                onValueChange = onPortChange,
                label = "Port",
                modifier = Modifier.weight(0.42f),
            )
        }
        TerminalFormTextField(
            value = editor.username,
            onValueChange = onUsernameChange,
            label = "Username",
        )
        TerminalFormTextField(
            value = editor.privateKey,
            onValueChange = onPrivateKeyChange,
            label = if (editor.allowUnencryptedKey) "Private key" else "Encrypted private key",
            singleLine = false,
            minLines = 5,
            maxLines = 8,
            fieldHeight = Modifier.heightIn(min = 132.dp),
        )
        TerminalFormCheckboxRow(
            checked = editor.allowUnencryptedKey,
            onCheckedChange = onAllowUnencryptedKeyChange,
            label = "Allow unencrypted key",
        )
        Spacer(modifier = Modifier.height(4.dp))
        TerminalPrimaryButton(
            text = "Save profile",
            onClick = onSave,
        )
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = colors.red,
            )
        }
    }
}

@Composable
private fun TerminalSessionContent(
    state: TerminalState,
    output: kotlinx.coroutines.flow.Flow<ByteArray>,
    onPassphraseChange: (String) -> Unit,
    onAllowUnencryptedKeyChange: (Boolean) -> Unit,
    onConnect: () -> Unit,
    onTrustHost: () -> Unit,
    onSend: (ByteArray) -> Unit,
    onResize: (TerminalSize) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingModifier by remember { mutableStateOf<TerminalModifier?>(null) }
    val context = LocalContext.current
    val selectedSession = state.selectedSession ?: return
    val connected = selectedSession.status == TerminalStatus.Connected
    val colors = rememberSidebarColorPalette()

    Column(modifier = modifier) {
        if (!connected) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.selectedProfile?.let { profile ->
                    TerminalInfoCard(
                        text = "${profile.displayLabel}\n${profile.username}@${profile.host}:${profile.port}",
                    )
                }
                TerminalFormTextField(
                    value = selectedSession.passphrase,
                    onValueChange = onPassphraseChange,
                    label =
                        if (state.selectedProfile?.allowUnencryptedKey == true) {
                            "Passphrase (optional)"
                        } else {
                            "Passphrase"
                        },
                    visualTransformation = PasswordVisualTransformation(),
                )
                TerminalFormCheckboxRow(
                    checked = state.selectedProfile?.allowUnencryptedKey == true,
                    onCheckedChange = onAllowUnencryptedKeyChange,
                    label = "Allow unencrypted key for this profile",
                )
                selectedSession.pendingHostTrust?.let { pending ->
                    TerminalInfoCard(
                        text =
                            if (pending.replacesExistingTrust) {
                                "Changed host fingerprint: ${pending.fingerprint}"
                            } else {
                                "Host fingerprint: ${pending.fingerprint}"
                            },
                    )
                    TerminalPrimaryButton(
                        text =
                            if (pending.replacesExistingTrust) {
                                "Trust new host and connect"
                            } else {
                                "Trust host and connect"
                            },
                        onClick = onTrustHost,
                    )
                }
                if (selectedSession.pendingHostTrust == null) {
                    TerminalPrimaryButton(
                        text =
                            if (selectedSession.status == TerminalStatus.Connecting) {
                                "Connecting"
                            } else {
                                "Connect"
                            },
                        onClick = onConnect,
                        enabled = selectedSession.status != TerminalStatus.Connecting,
                    )
                }
                selectedSession.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = colors.red,
                    )
                }
            }
            return@Column
        }
        TerminalViewportFrame(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            TermuxTerminalSurface(
                output = output,
                onInput = onSend,
                onResize = onResize,
                modifier = Modifier.fillMaxSize(),
            )
        }
        TerminalAccessoryBar(
            pendingModifier = pendingModifier,
            onModifier = { modifierValue ->
                pendingModifier = if (pendingModifier == modifierValue) null else modifierValue
            },
            onSendText = { text ->
                onSend(TerminalInputEncoder.text(text, pendingModifier))
                pendingModifier = null
            },
            onDirection = { direction ->
                onSend(TerminalInputEncoder.direction(direction, pendingModifier))
                pendingModifier = null
            },
            onClear = onClear,
            onPaste = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                if (text.isNotEmpty()) onSend(text.encodeToByteArray())
            },
        )
    }
}

@Composable
private fun TerminalAccessoryBar(
    pendingModifier: TerminalModifier?,
    onModifier: (TerminalModifier) -> Unit,
    onSendText: (String) -> Unit,
    onDirection: (TerminalDirection) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
) {
    val colors = rememberSidebarColorPalette()
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.background)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalAccessoryKey(label = "Esc", onClick = { onSendText("\u001B") })
        TerminalAccessoryKey(
            label = if (pendingModifier == TerminalModifier.Ctrl) "Ctrl*" else "Ctrl",
            selected = pendingModifier == TerminalModifier.Ctrl,
            onClick = { onModifier(TerminalModifier.Ctrl) },
        )
        TerminalAccessoryKey(
            label = if (pendingModifier == TerminalModifier.Alt) "Alt*" else "Alt",
            selected = pendingModifier == TerminalModifier.Alt,
            onClick = { onModifier(TerminalModifier.Alt) },
        )
        TerminalAccessoryKey(label = "Tab", onClick = { onSendText("\t") })
        TerminalAccessoryKey(label = "Enter", onClick = { onSendText("\r") })
        listOf("/", "|", "~", "-").forEach { value ->
            TerminalAccessoryKey(label = value, onClick = { onSendText(value) })
        }
        TerminalAccessoryKey(
            label = "Up",
            leadingIcon = {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primaryText,
                )
            },
            onClick = { onDirection(TerminalDirection.Up) },
        )
        TerminalAccessoryKey(
            label = "Down",
            leadingIcon = {
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primaryText,
                )
            },
            onClick = { onDirection(TerminalDirection.Down) },
        )
        TerminalAccessoryKey(
            label = "Left",
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primaryText,
                )
            },
            onClick = { onDirection(TerminalDirection.Left) },
        )
        TerminalAccessoryKey(
            label = "Right",
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.primaryText,
                )
            },
            onClick = { onDirection(TerminalDirection.Right) },
        )
        TerminalAccessoryKey(label = "Clear", onClick = onClear)
        TerminalAccessoryKey(label = "Paste", onClick = onPaste)
    }
}
