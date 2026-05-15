package com.remodex.mobile.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.remodex.mobile.AppContainer
import com.remodex.mobile.ui.theme.RemodexPopupSurface

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
        onSaveEditor = viewModel::saveEditor,
        onEditorLabelChange = viewModel::updateEditorLabel,
        onEditorHostChange = viewModel::updateEditorHost,
        onEditorPortChange = viewModel::updateEditorPort,
        onEditorUsernameChange = viewModel::updateEditorUsername,
        onEditorPrivateKeyChange = viewModel::updateEditorPrivateKey,
        onEditorAllowUnencryptedKeyChange = viewModel::updateEditorAllowUnencryptedKey,
        onPassphraseChange = viewModel::updatePassphrase,
        onSelectedProfileAllowUnencryptedKeyChange = viewModel::updateSelectedProfileAllowUnencryptedKey,
        onConnect = viewModel::connect,
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
    val title =
        when (state.surface) {
            TerminalSurface.Profiles -> "Terminal"
            TerminalSurface.Editor -> if (state.editor?.id == null) "New profile" else "Edit profile"
            TerminalSurface.Session -> selectedProfile?.displayLabel ?: "Terminal"
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title)
                        if (state.surface == TerminalSurface.Session && selectedProfile != null) {
                            Text(
                                text = "${selectedSession?.status?.name ?: TerminalStatus.Idle.name} - ${selectedProfile.username}@${selectedProfile.host}:${selectedProfile.port}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick =
                            when (state.surface) {
                                TerminalSurface.Profiles -> onNavigateBack
                                TerminalSurface.Editor -> onCancelEditor
                                TerminalSurface.Session -> onCloseSelectedSession
                            },
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.surface == TerminalSurface.Profiles) {
                        TextButton(onClick = { showWindowsHelp = true }) { Text("Help") }
                    }
                    if (state.surface == TerminalSurface.Session) {
                        TextButton(onClick = { showWindowsHelp = true }) { Text("Help") }
                        TextButton(onClick = { showSessions = !showSessions }) { Text("Sessions") }
                        if (connected || selectedSession?.status == TerminalStatus.Connecting) {
                            TextButton(onClick = onDisconnect) { Text("Disconnect") }
                        } else {
                            TextButton(onClick = onConnect) { Text("Connect") }
                        }
                    }
                },
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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TerminalWindowsSetupGuide()
                    TextButton(
                        onClick = { showWindowsHelp = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Close")
                    }
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
    Column(
        modifier =
            modifier
                .width(92.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sessions.forEach { session ->
            val profile = profiles.firstOrNull { it.id == session.profileId }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (session.id == selectedSessionId) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        )
                        .clickable { onSelectSession(session.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(
                    text = profile?.displayLabel ?: "Terminal",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                )
                Text(
                    text = session.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { onCloseSession(session.id) }) { Text("Close") }
            }
        }
        TextButton(
            onClick = onShowProfiles,
            enabled = profiles.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("+")
        }
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
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = onCreateProfile, modifier = Modifier.fillMaxWidth()) {
            Text("Create profile")
        }
        if (profiles.isEmpty()) {
            Text(
                text = "No terminal profiles yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        profiles.forEach { profile ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.displayLabel, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${profile.username}@${profile.host}:${profile.port}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onOpenProfile(profile.id) }) { Text("Open") }
                    OutlinedButton(onClick = { onEditProfile(profile.id) }) { Text("Edit") }
                    TextButton(onClick = { onDeleteProfile(profile.id) }) { Text("Delete") }
                }
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
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = editor.label,
            onValueChange = onLabelChange,
            label = { Text("Profile name (optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = editor.host,
                onValueChange = onHostChange,
                label = { Text("Host") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = editor.port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.weight(0.42f),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = editor.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = editor.privateKey,
            onValueChange = onPrivateKeyChange,
            label = { Text(if (editor.allowUnencryptedKey) "Private key" else "Encrypted private key") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(132.dp),
            minLines = 4,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = editor.allowUnencryptedKey,
                onCheckedChange = onAllowUnencryptedKeyChange,
            )
            Text(
                text = "Allow unencrypted key",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text("Save profile")
        }
        errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
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

    Column(modifier = modifier) {
        if (!connected) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = selectedSession.passphrase,
                    onValueChange = onPassphraseChange,
                    label = {
                        Text(
                            if (state.selectedProfile?.allowUnencryptedKey == true) {
                                "Passphrase (optional)"
                            } else {
                                "Passphrase"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Checkbox(
                        checked = state.selectedProfile?.allowUnencryptedKey == true,
                        onCheckedChange = onAllowUnencryptedKeyChange,
                    )
                    Text(
                        text = "Allow unencrypted key for this profile",
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                selectedSession.pendingHostTrust?.let { pending ->
                    Text(
                        text =
                            if (pending.replacesExistingTrust) {
                                "Changed host fingerprint: ${pending.fingerprint}"
                            } else {
                                "Host fingerprint: ${pending.fingerprint}"
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onTrustHost, modifier = Modifier.fillMaxWidth()) {
                        Text(if (pending.replacesExistingTrust) "Trust new host and connect" else "Trust host and connect")
                    }
                }
                if (selectedSession.pendingHostTrust == null) {
                    Button(
                        onClick = onConnect,
                    enabled = selectedSession.status != TerminalStatus.Connecting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (selectedSession.status == TerminalStatus.Connecting) "Connecting" else "Connect")
                    }
                }
                selectedSession.errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            return@Column
        }
        TermuxTerminalSurface(
            output = output,
            onInput = onSend,
            onResize = onResize,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
        )
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TerminalKey("Esc") { onSendText("\u001B") }
        TerminalKey(if (pendingModifier == TerminalModifier.Ctrl) "Ctrl*" else "Ctrl") {
            onModifier(TerminalModifier.Ctrl)
        }
        TerminalKey(if (pendingModifier == TerminalModifier.Alt) "Alt*" else "Alt") {
            onModifier(TerminalModifier.Alt)
        }
        TerminalKey("Tab") { onSendText("\t") }
        TerminalKey("Enter") { onSendText("\r") }
        listOf("/", "|", "~", "-").forEach { value ->
            TerminalKey(value) { onSendText(value) }
        }
        TerminalIconKey(
            label = "Up",
            icon = { Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = null) },
            onClick = { onDirection(TerminalDirection.Up) },
        )
        TerminalIconKey(
            label = "Down",
            icon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
            onClick = { onDirection(TerminalDirection.Down) },
        )
        TerminalIconKey(
            label = "Left",
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null) },
            onClick = { onDirection(TerminalDirection.Left) },
        )
        TerminalIconKey(
            label = "Right",
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            onClick = { onDirection(TerminalDirection.Right) },
        )
        TerminalKey("Clear", onClick = onClear)
        TerminalKey("Paste", onClick = onPaste)
    }
}

@Composable
private fun TerminalKey(
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) { Text(label) }
}

@Composable
private fun TerminalIconKey(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        icon()
        Text(label)
    }
}
