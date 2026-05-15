package com.remodex.mobile.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalViewModel(
    private val profileRepository: TerminalProfileRepository,
    private val trustedHostRepository: TerminalTrustedHostRepository,
    private val clientFactory: () -> TerminalClient = { SshjTerminalClient(trustedHostRepository) },
    private val newSessionId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {
    constructor(
        profileRepository: TerminalProfileRepository,
        trustedHostRepository: TerminalTrustedHostRepository,
        client: TerminalClient,
    ) : this(profileRepository, trustedHostRepository, clientFactory = { client })
    private data class RuntimeSession(
        val client: TerminalClient,
        val output: MutableSharedFlow<ByteArray>,
    )

    private val runtimeSessions = mutableMapOf<String, RuntimeSession>()
    private val _state = MutableStateFlow(TerminalState(profiles = profileRepository.loadProfiles()))
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    fun outputForSession(sessionId: String?): Flow<ByteArray> =
        sessionId?.let { runtimeSessions[it]?.output?.asSharedFlow() } ?: emptyFlow()

    fun startCreateProfile() {
        _state.update {
            it.copy(
                surface = TerminalSurface.Editor,
                editor = TerminalProfileDraft(),
                editorErrorMessage = null,
            )
        }
    }

    fun showProfiles() {
        _state.update { it.copy(surface = TerminalSurface.Profiles) }
    }

    fun startEditProfile(id: String) {
        val profile = _state.value.profiles.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                surface = TerminalSurface.Editor,
                editor =
                    TerminalProfileDraft(
                        id = profile.id,
                        label = profile.label.orEmpty(),
                        host = profile.host,
                        port = profile.port.toString(),
                        username = profile.username,
                        privateKey = profile.privateKey,
                        allowUnencryptedKey = profile.allowUnencryptedKey,
                    ),
                editorErrorMessage = null,
            )
        }
    }

    fun cancelEditor() {
        _state.update { it.copy(surface = TerminalSurface.Profiles, editor = null, editorErrorMessage = null) }
    }

    fun updateEditorLabel(value: String) = updateEditor { it.copy(label = value) }
    fun updateEditorHost(value: String) = updateEditor { it.copy(host = value) }
    fun updateEditorPort(value: String) = updateEditor { it.copy(port = value.filter(Char::isDigit).take(5)) }
    fun updateEditorUsername(value: String) = updateEditor { it.copy(username = value) }
    fun updateEditorPrivateKey(value: String) = updateEditor { it.copy(privateKey = value) }
    fun updateEditorAllowUnencryptedKey(value: Boolean) = updateEditor { it.copy(allowUnencryptedKey = value) }

    fun saveEditor() {
        val editor = _state.value.editor ?: return
        profileRepository.saveProfile(editor)
            .onSuccess {
                _state.update {
                    it.copy(
                        surface = TerminalSurface.Profiles,
                        profiles = profileRepository.loadProfiles(),
                        editor = null,
                        editorErrorMessage = null,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(editorErrorMessage = error.userFacingMessage()) }
            }
    }

    fun deleteProfile(id: String) {
        profileRepository.deleteProfile(id)
        val sessionIdsToClose = _state.value.sessions.filter { it.profileId == id }.map { it.id }
        sessionIdsToClose.forEach(::closeSession)
        _state.update { it.copy(profiles = profileRepository.loadProfiles()) }
    }

    fun openProfile(id: String) {
        if (_state.value.profiles.none { it.id == id }) return
        val sessionId = newSessionId()
        val runtime =
            RuntimeSession(
                client = clientFactory(),
                output =
                    MutableSharedFlow(
                        extraBufferCapacity = 128,
                        onBufferOverflow = BufferOverflow.DROP_OLDEST,
                    ),
            )
        runtimeSessions[sessionId] = runtime
        bindClient(sessionId, runtime)
        _state.update {
            it.copy(
                surface = TerminalSurface.Session,
                sessions = it.sessions + TerminalSessionSummary(id = sessionId, profileId = id),
                selectedSessionId = sessionId,
            )
        }
    }

    fun selectSession(id: String) {
        if (_state.value.sessions.any { it.id == id }) {
            _state.update { it.copy(surface = TerminalSurface.Session, selectedSessionId = id) }
        }
    }

    fun closeSelectedSession() {
        _state.value.selectedSessionId?.let(::closeSession)
    }

    fun closeSession(id: String) {
        val runtime = runtimeSessions.remove(id)
        viewModelScope.launch { runtime?.client?.disconnect() }
        _state.update { current ->
            val sessions = current.sessions.filterNot { it.id == id }
            current.copy(
                surface = if (sessions.isEmpty()) TerminalSurface.Profiles else TerminalSurface.Session,
                sessions = sessions,
                selectedSessionId =
                    when {
                        current.selectedSessionId != id -> current.selectedSessionId
                        else -> sessions.lastOrNull()?.id
                    },
            )
        }
    }

    fun updatePassphrase(value: String) = updateSelectedSession { it.copy(passphrase = value, errorMessage = null) }

    fun updateSelectedProfileAllowUnencryptedKey(value: Boolean) {
        val profileId = _state.value.selectedSession?.profileId ?: return
        profileRepository.setAllowUnencryptedKey(profileId, value)
        _state.update { it.copy(profiles = profileRepository.loadProfiles()) }
    }

    fun connect() {
        val session = _state.value.selectedSession ?: return
        val config = connectionConfigOrNull(session) ?: run {
            updateSelectedSession {
                it.copy(
                    status = TerminalStatus.Error,
                    errorMessage =
                        if (_state.value.selectedProfile?.allowUnencryptedKey == true) {
                            "Select a profile before connecting."
                        } else {
                            "Enter the passphrase before connecting."
                        },
                )
            }
            return
        }
        val runtime = runtimeSessions[session.id] ?: return
        viewModelScope.launch {
            runCatching {
                runtime.client.connect(config, session.size)
                profileRepository.markUsed(config.profileId)
                _state.update { it.copy(profiles = profileRepository.loadProfiles()) }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                when (val unknownHost = error.findCause<UnknownTerminalHostKeyException>()) {
                    null -> Unit
                    else -> {
                        updateSession(session.id) {
                            it.copy(
                                status = TerminalStatus.Disconnected,
                                pendingHostTrust =
                                    PendingTerminalHostTrust(
                                        host = unknownHost.host,
                                        port = unknownHost.port,
                                        fingerprint = unknownHost.fingerprint,
                                        encodedHostKey = unknownHost.encodedHostKey,
                                        replacesExistingTrust = false,
                                    ),
                                errorMessage = "Unknown host. Confirm the fingerprint before connecting.",
                            )
                        }
                        return@onFailure
                    }
                }
                when (val changedHost = error.findCause<ChangedTerminalHostKeyException>()) {
                    null -> Unit
                    else -> {
                        updateSession(session.id) {
                            it.copy(
                                status = TerminalStatus.Disconnected,
                                pendingHostTrust =
                                    PendingTerminalHostTrust(
                                        host = changedHost.host,
                                        port = changedHost.port,
                                        fingerprint = changedHost.fingerprint,
                                        encodedHostKey = changedHost.encodedHostKey,
                                        replacesExistingTrust = true,
                                    ),
                                errorMessage = "Host fingerprint changed. Re-confirm the host before connecting.",
                            )
                        }
                        return@onFailure
                    }
                }
                updateSession(session.id) {
                    it.copy(status = TerminalStatus.Error, errorMessage = error.userFacingMessage())
                }
            }
        }
    }

    fun trustPendingHostAndConnect() {
        val pending = _state.value.selectedSession?.pendingHostTrust ?: return
        trustedHostRepository.trust(pending.host, pending.port, pending.fingerprint)
        updateSelectedSession { it.copy(pendingHostTrust = null, errorMessage = null) }
        connect()
    }

    fun disconnect() {
        val sessionId = _state.value.selectedSessionId ?: return
        val runtime = runtimeSessions[sessionId] ?: return
        viewModelScope.launch { runtime.client.disconnect() }
    }

    fun send(bytes: ByteArray) {
        val session = _state.value.selectedSession ?: return
        if (bytes.isEmpty() || session.status != TerminalStatus.Connected) return
        val runtime = runtimeSessions[session.id] ?: return
        viewModelScope.launch { runCatching { runtime.client.write(bytes) } }
    }

    fun resize(size: TerminalSize) {
        val session = _state.value.selectedSession ?: return
        val normalized = size.normalized
        if (session.size == normalized) return
        updateSelectedSession { it.copy(size = normalized) }
        if (session.status != TerminalStatus.Connected) return
        val runtime = runtimeSessions[session.id] ?: return
        viewModelScope.launch { runCatching { runtime.client.resize(normalized) } }
    }

    fun clearTerminal() {
        val sessionId = _state.value.selectedSessionId ?: return
        runtimeSessions[sessionId]?.output?.tryEmit("\u001Bc".encodeToByteArray())
    }

    override fun onCleared() {
        runtimeSessions.values.forEach { runtime ->
            viewModelScope.launch { runtime.client.disconnect() }
        }
        super.onCleared()
    }

    private fun bindClient(
        sessionId: String,
        runtime: RuntimeSession,
    ) {
        viewModelScope.launch {
            runtime.client.events.collect { event ->
                when (event) {
                    is TerminalEvent.Output -> runtime.output.tryEmit(event.bytes)
                    is TerminalEvent.StatusChanged -> updateSession(sessionId) { it.copy(status = event.status) }
                    is TerminalEvent.Error -> updateSession(sessionId) { it.copy(status = TerminalStatus.Error, errorMessage = event.message) }
                }
            }
        }
    }

    private fun updateEditor(transform: (TerminalProfileDraft) -> TerminalProfileDraft) {
        _state.update { current ->
            current.copy(
                editor = current.editor?.let(transform),
                editorErrorMessage = null,
            )
        }
    }

    private fun updateSelectedSession(transform: (TerminalSessionSummary) -> TerminalSessionSummary) {
        _state.value.selectedSessionId?.let { updateSession(it, transform) }
    }

    private fun updateSession(
        id: String,
        transform: (TerminalSessionSummary) -> TerminalSessionSummary,
    ) {
        _state.update { current ->
            current.copy(sessions = current.sessions.map { if (it.id == id) transform(it) else it })
        }
    }

    private fun connectionConfigOrNull(session: TerminalSessionSummary): TerminalConnectionConfig? {
        val profile = _state.value.profiles.firstOrNull { it.id == session.profileId } ?: return null
        return TerminalConnectionConfig(
            profileId = profile.id,
            host = profile.host,
            port = profile.port,
            username = profile.username,
            privateKey = profile.privateKey,
            passphrase = session.passphrase,
            allowUnencryptedKey = profile.allowUnencryptedKey,
        ).takeIf { it.isComplete }
    }

    private fun Throwable.userFacingMessage(): String =
        when {
            findCause<MissingTerminalPassphraseException>() != null -> "Passphrase is required for every terminal connection."
            findCause<UnencryptedTerminalPrivateKeyException>() != null -> "Only encrypted SSH private keys are supported."
            findCause<TerminalPrivateKeyDecryptionException>() != null -> "The private key could not be unlocked with this passphrase."
            findCause<TerminalPublicKeyAuthenticationException>() != null ->
                findCause<TerminalPublicKeyAuthenticationException>()!!.message.orEmpty()
            findCause<TerminalNetworkConnectionException>() != null -> "Could not reach the SSH host."
            message?.contains("password", ignoreCase = true) == true ||
                message?.contains("decrypt", ignoreCase = true) == true -> "The passphrase is incorrect."
            message?.contains("auth", ignoreCase = true) == true -> "SSH authentication failed."
            else -> message?.takeIf(String::isNotBlank) ?: javaClass.simpleName
        }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }

    class Factory(
        private val profileRepository: TerminalProfileRepository,
        private val trustedHostRepository: TerminalTrustedHostRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TerminalViewModel(profileRepository, trustedHostRepository) as T
    }
}
