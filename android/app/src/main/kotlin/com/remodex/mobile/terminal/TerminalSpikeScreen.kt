package com.remodex.mobile.terminal

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TerminalSpikeRoute(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: TerminalSpikeViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    TerminalSpikeScreen(
        state = state,
        output = viewModel.output,
        onHostChange = viewModel::updateHost,
        onPortChange = viewModel::updatePort,
        onUsernameChange = viewModel::updateUsername,
        onPrivateKeyChange = viewModel::updatePrivateKey,
        onPassphraseChange = viewModel::updatePassphrase,
        onConnect = viewModel::connect,
        onTrustHost = viewModel::trustPendingHostAndConnect,
        onDisconnect = viewModel::disconnect,
        onSend = viewModel::send,
        onResize = viewModel::resize,
        onClear = viewModel::clearTerminal,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSpikeScreen(
    state: TerminalSpikeState,
    output: kotlinx.coroutines.flow.Flow<ByteArray>,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onConnect: () -> Unit,
    onTrustHost: () -> Unit,
    onDisconnect: () -> Unit,
    onSend: (ByteArray) -> Unit,
    onResize: (TerminalSize) -> Unit,
    onClear: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingModifier by remember { mutableStateOf<TerminalModifier?>(null) }
    val context = LocalContext.current
    val connected = state.status == TerminalStatus.Connected

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Terminal")
                        Text(
                            text = "${state.status.name} - ${state.host.ifBlank { "no host" }}:${state.port}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (connected || state.status == TerminalStatus.Connecting) {
                        TextButton(onClick = onDisconnect) { Text("Disconnect") }
                    } else {
                        TextButton(onClick = onConnect) { Text("Connect") }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (!connected) {
                TerminalConnectionForm(
                    state = state,
                    onHostChange = onHostChange,
                    onPortChange = onPortChange,
                    onUsernameChange = onUsernameChange,
                    onPrivateKeyChange = onPrivateKeyChange,
                    onPassphraseChange = onPassphraseChange,
                    onConnect = onConnect,
                    onTrustHost = onTrustHost,
                    modifier = Modifier.fillMaxSize(),
                )
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
                onModifier = { modifier ->
                    pendingModifier = if (pendingModifier == modifier) null else modifier
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
}

@Composable
private fun TerminalConnectionForm(
    state: TerminalSpikeState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onPassphraseChange: (String) -> Unit,
    onConnect: () -> Unit,
    onTrustHost: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.host,
                onValueChange = onHostChange,
                label = { Text("Host") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.port,
                onValueChange = onPortChange,
                label = { Text("Port") },
                modifier = Modifier.weight(0.42f),
                singleLine = true,
            )
        }
        OutlinedTextField(
            value = state.username,
            onValueChange = onUsernameChange,
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.privateKey,
            onValueChange = onPrivateKeyChange,
            label = { Text("Private key (not saved)") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(112.dp),
            minLines = 3,
        )
        OutlinedTextField(
            value = state.passphrase,
            onValueChange = onPassphraseChange,
            label = { Text("Passphrase (not saved)") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
        )
        state.pendingHostFingerprint?.let { fingerprint ->
            Text(
                text = "Host fingerprint: $fingerprint",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onTrustHost) {
                Text("Trust and connect")
            }
        }
        if (state.pendingHostFingerprint == null) {
            Button(
                onClick = onConnect,
                enabled = state.status != TerminalStatus.Connecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.status == TerminalStatus.Connecting) "Connecting" else "Connect")
            }
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
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
            onClick = {
                onDirection(TerminalDirection.Up)
            },
        )
        TerminalIconKey(
            label = "Down",
            icon = { Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null) },
            onClick = {
                onDirection(TerminalDirection.Down)
            },
        )
        TerminalIconKey(
            label = "Left",
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = null) },
            onClick = {
                onDirection(TerminalDirection.Left)
            },
        )
        TerminalIconKey(
            label = "Right",
            icon = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null) },
            onClick = {
                onDirection(TerminalDirection.Right)
            },
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
    TextButton(onClick = onClick) {
        Text(label)
    }
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
