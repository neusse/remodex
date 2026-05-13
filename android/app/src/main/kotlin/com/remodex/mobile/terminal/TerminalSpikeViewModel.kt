package com.remodex.mobile.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remodex.mobile.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalSpikeViewModel : ViewModel() {
    private val knownHostStore = TerminalKnownHostStore()
    private val client: TerminalClient = SshjTerminalClient(knownHostStore)

    private val _state =
        MutableStateFlow(
            TerminalSpikeState(
                host = BuildConfig.TERMINAL_SPIKE_HOST,
                port = BuildConfig.TERMINAL_SPIKE_PORT.toString(),
                username = BuildConfig.TERMINAL_SPIKE_USERNAME,
            ),
        )
    val state: StateFlow<TerminalSpikeState> = _state.asStateFlow()

    private val _output =
        MutableSharedFlow<ByteArray>(
            extraBufferCapacity = 128,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val output: SharedFlow<ByteArray> = _output.asSharedFlow()

    init {
        viewModelScope.launch {
            client.events.collect { event ->
                when (event) {
                    is TerminalEvent.Output -> _output.tryEmit(event.bytes)
                    is TerminalEvent.StatusChanged ->
                        _state.update { current -> current.copy(status = event.status) }
                    is TerminalEvent.Error ->
                        _state.update { current ->
                            current.copy(
                                status = TerminalStatus.Error,
                                errorMessage = event.message,
                            )
                        }
                }
            }
        }
    }

    fun updateHost(value: String) {
        _state.update { it.copy(host = value, errorMessage = null) }
    }

    fun updatePort(value: String) {
        _state.update { it.copy(port = value.filter(Char::isDigit).take(5), errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _state.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePrivateKey(value: String) {
        _state.update { it.copy(privateKey = value, errorMessage = null) }
    }

    fun updatePassphrase(value: String) {
        _state.update { it.copy(passphrase = value, errorMessage = null) }
    }

    fun connect() {
        val config = connectionConfigOrNull()
        if (config == null) {
            _state.update {
                it.copy(
                    status = TerminalStatus.Error,
                    errorMessage = "Host, port, username, and private key are required.",
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching {
                client.connect(config, _state.value.size)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                val unknownHostKey = error.findCause<UnknownTerminalHostKeyException>()
                if (unknownHostKey != null) {
                    _state.update {
                        it.copy(
                            status = TerminalStatus.Disconnected,
                            pendingHostFingerprint = unknownHostKey.fingerprint,
                            pendingHostKey = unknownHostKey.encodedHostKey,
                            errorMessage = "Trust this host fingerprint before connecting.",
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            status = TerminalStatus.Error,
                            errorMessage = error.message?.takeIf(String::isNotBlank) ?: error.javaClass.simpleName,
                        )
                    }
                }
            }
        }
    }

    fun trustPendingHostAndConnect() {
        val current = _state.value
        val fingerprint = current.pendingHostFingerprint ?: return
        val port = current.port.toIntOrNull()?.coerceIn(1, 65535) ?: 22
        knownHostStore.trust(current.host, port, fingerprint)
        _state.update {
            it.copy(
                pendingHostFingerprint = null,
                pendingHostKey = null,
                errorMessage = null,
            )
        }
        connect()
    }

    fun disconnect() {
        viewModelScope.launch {
            client.disconnect()
        }
    }

    fun send(bytes: ByteArray) {
        if (bytes.isEmpty() || _state.value.status != TerminalStatus.Connected) return
        viewModelScope.launch {
            runCatching { client.write(bytes) }
        }
    }

    fun resize(size: TerminalSize) {
        val normalized = size.normalized
        if (_state.value.size == normalized) return
        _state.update { it.copy(size = normalized) }
        if (_state.value.status != TerminalStatus.Connected) return
        viewModelScope.launch {
            runCatching { client.resize(normalized) }
        }
    }

    fun clearTerminal() {
        _output.tryEmit("\u001Bc".encodeToByteArray())
    }

    override fun onCleared() {
        viewModelScope.launch {
            client.disconnect()
        }
        super.onCleared()
    }

    private fun connectionConfigOrNull(): TerminalConnectionConfig? {
        val current = _state.value
        val config =
            TerminalConnectionConfig(
                host = current.host.trim(),
                port = current.port.toIntOrNull()?.coerceIn(1, 65535) ?: return null,
                username = current.username.trim(),
                privateKey = current.privateKey,
                passphrase = current.passphrase,
        )
        return config.takeIf { it.isComplete }
    }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}
