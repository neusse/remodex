package com.remodex.mobile.terminal

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

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

@Serializable
data class TerminalProfile(
    val id: String,
    val label: String? = null,
    val host: String,
    val port: Int,
    val username: String,
    val privateKey: String,
    val allowUnencryptedKey: Boolean = false,
    val lastUsedAtEpochMs: Long? = null,
) {
    val displayLabel: String
        get() = label?.trim()?.takeIf(String::isNotEmpty) ?: "$username@$host:$port"
}

@Serializable
data class TrustedTerminalHost(
    val host: String,
    val port: Int,
    val fingerprint: String,
)

data class TerminalProfileDraft(
    val id: String? = null,
    val label: String = "",
    val host: String = "",
    val port: String = "22",
    val username: String = "",
    val privateKey: String = "",
    val allowUnencryptedKey: Boolean = false,
)

data class PendingTerminalHostTrust(
    val host: String,
    val port: Int,
    val fingerprint: String,
    val encodedHostKey: String,
    val replacesExistingTrust: Boolean,
)

data class TerminalConnectionConfig(
    val profileId: String,
    val host: String,
    val port: Int,
    val username: String,
    val privateKey: String,
    val passphrase: String,
    val allowUnencryptedKey: Boolean,
) {
    val isComplete: Boolean
        get() =
            profileId.isNotBlank() &&
                host.isNotBlank() &&
                port in 1..65535 &&
                username.isNotBlank() &&
                privateKey.isNotBlank() &&
                (allowUnencryptedKey || passphrase.isNotBlank())
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

sealed class TerminalConnectionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class TerminalPrivateKeyDecryptionException(cause: Throwable) :
    TerminalConnectionException("The private key could not be unlocked with this passphrase.", cause)

class TerminalPublicKeyAuthenticationException(
    val username: String,
    cause: Throwable,
) : TerminalConnectionException(
        "Public-key authentication was rejected for $username. Check the username and authorized_keys on the server.",
        cause,
    )

class TerminalNetworkConnectionException(cause: Throwable) :
    TerminalConnectionException("Could not reach the SSH host.", cause)

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

enum class TerminalSurface {
    Profiles,
    Editor,
    Session,
}

data class TerminalState(
    val surface: TerminalSurface = TerminalSurface.Profiles,
    val profiles: List<TerminalProfile> = emptyList(),
    val editor: TerminalProfileDraft? = null,
    val editorErrorMessage: String? = null,
    val sessions: List<TerminalSessionSummary> = emptyList(),
    val selectedSessionId: String? = null,
) {
    val selectedSession: TerminalSessionSummary?
        get() = sessions.firstOrNull { it.id == selectedSessionId }

    val selectedProfile: TerminalProfile?
        get() = profiles.firstOrNull { it.id == selectedSession?.profileId }
}

data class TerminalSessionSummary(
    val id: String,
    val profileId: String,
    val passphrase: String = "",
    val status: TerminalStatus = TerminalStatus.Idle,
    val size: TerminalSize = TerminalSize(cols = 80, rows = 24),
    val errorMessage: String? = null,
    val pendingHostTrust: PendingTerminalHostTrust? = null,
)
