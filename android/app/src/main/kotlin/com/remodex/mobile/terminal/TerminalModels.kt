package com.remodex.mobile.terminal

import kotlinx.coroutines.flow.Flow

data class TerminalSize(
    val cols: Int,
    val rows: Int,
) {
    val normalized: TerminalSize
        get() =
            TerminalSize(
                cols = cols.coerceIn(2, 400),
                rows = rows.coerceIn(2, 200),
            )
}

enum class TerminalStatus {
    Idle,
    Connecting,
    Connected,
    Disconnected,
    Error,
}

data class TerminalConnectionConfig(
    val host: String,
    val port: Int,
    val username: String,
    val privateKey: String,
    val passphrase: String = "",
) {
    val isComplete: Boolean
        get() =
            host.isNotBlank() &&
                port in 1..65535 &&
                username.isNotBlank() &&
                privateKey.isNotBlank()
}

sealed interface TerminalEvent {
    data class Output(val bytes: ByteArray) : TerminalEvent {
        override fun equals(other: Any?): Boolean =
            other is Output && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data class StatusChanged(val status: TerminalStatus) : TerminalEvent
    data class Error(val message: String, val throwable: Throwable? = null) : TerminalEvent
}

interface TerminalClient {
    val events: Flow<TerminalEvent>

    suspend fun connect(
        config: TerminalConnectionConfig,
        initialSize: TerminalSize,
    )

    suspend fun write(bytes: ByteArray)

    suspend fun resize(size: TerminalSize)

    suspend fun disconnect()
}

data class TerminalSpikeState(
    val status: TerminalStatus = TerminalStatus.Idle,
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val privateKey: String = "",
    val passphrase: String = "",
    val size: TerminalSize = TerminalSize(cols = 80, rows = 24),
    val errorMessage: String? = null,
    val pendingHostFingerprint: String? = null,
    val pendingHostKey: String? = null,
)
