package com.remodex.mobile.terminal

import com.remodex.mobile.core.security.SecureStore
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface TerminalKeyValueStore {
    fun readString(key: String): String?
    fun writeString(key: String, value: String)
    fun deleteValue(key: String)
}

class SecureTerminalKeyValueStore(
    private val secureStore: SecureStore,
) : TerminalKeyValueStore {
    override fun readString(key: String): String? = secureStore.readString(key)

    override fun writeString(
        key: String,
        value: String,
    ) = secureStore.writeString(key, value)

    override fun deleteValue(key: String) = secureStore.deleteValue(key)
}

class TerminalProfileRepository(
    private val store: TerminalKeyValueStore,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) {
    fun loadProfiles(): List<TerminalProfile> =
        decode<StoredTerminalProfiles>(PROFILES_KEY)?.profiles.orEmpty()
            .sortedByDescending { it.lastUsedAtEpochMs ?: Long.MIN_VALUE }

    fun saveProfile(draft: TerminalProfileDraft): Result<TerminalProfile> =
        runCatching {
            val port = draft.port.toIntOrNull()?.takeIf { it in 1..65535 }
                ?: error("Port must be between 1 and 65535.")
            val key = draft.privateKey.trim()
            if (!draft.allowUnencryptedKey && !TerminalPrivateKeyInspector.isEncrypted(key)) {
                throw UnencryptedTerminalPrivateKeyException()
            }
            val host = draft.host.trim().takeIf(String::isNotEmpty) ?: error("Host is required.")
            val username = draft.username.trim().takeIf(String::isNotEmpty) ?: error("Username is required.")
            val current = loadProfiles()
            val existing = draft.id?.let { id -> current.firstOrNull { it.id == id } }
            val profile =
                TerminalProfile(
                    id = existing?.id ?: newId(),
                    label = draft.label.trim().takeIf(String::isNotEmpty),
                    host = host,
                    port = port,
                    username = username,
                    privateKey = key,
                    allowUnencryptedKey = draft.allowUnencryptedKey,
                    lastUsedAtEpochMs = existing?.lastUsedAtEpochMs,
                )
            persist(current.filterNot { it.id == profile.id } + profile)
            profile
        }

    fun deleteProfile(id: String) {
        persist(loadProfiles().filterNot { it.id == id })
    }

    fun markUsed(id: String) {
        val next =
            loadProfiles().map { profile ->
                if (profile.id == id) profile.copy(lastUsedAtEpochMs = nowEpochMs()) else profile
            }
        persist(next)
    }

    fun setAllowUnencryptedKey(
        id: String,
        allow: Boolean,
    ) {
        val next =
            loadProfiles().map { profile ->
                if (profile.id == id) profile.copy(allowUnencryptedKey = allow) else profile
            }
        persist(next)
    }

    private fun persist(profiles: List<TerminalProfile>) {
        store.writeString(PROFILES_KEY, json.encodeToString(StoredTerminalProfiles(profiles)))
    }

    private inline fun <reified T> decode(key: String): T? =
        store.readString(key)?.let { raw -> runCatching { json.decodeFromString<T>(raw) }.getOrNull() }

    @Serializable
    private data class StoredTerminalProfiles(
        val profiles: List<TerminalProfile> = emptyList(),
    )

    companion object {
        private const val PROFILES_KEY = "terminal.profiles"
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
    }
}

class TerminalTrustedHostRepository(
    private val store: TerminalKeyValueStore,
) {
    fun loadHosts(): List<TrustedTerminalHost> =
        decode<StoredTrustedTerminalHosts>(HOSTS_KEY)?.hosts.orEmpty()

    fun trustedFingerprint(
        host: String,
        port: Int,
    ): String? =
        loadHosts()
            .firstOrNull { it.key == normalizedKey(host, port) }
            ?.fingerprint

    fun trust(
        host: String,
        port: Int,
        fingerprint: String,
    ) {
        val next =
            loadHosts().filterNot { it.key == normalizedKey(host, port) } +
                TrustedTerminalHost(host = host.trim(), port = port, fingerprint = fingerprint)
        persist(next)
    }

    private fun persist(hosts: List<TrustedTerminalHost>) {
        store.writeString(HOSTS_KEY, json.encodeToString(StoredTrustedTerminalHosts(hosts)))
    }

    private inline fun <reified T> decode(key: String): T? =
        store.readString(key)?.let { raw -> runCatching { json.decodeFromString<T>(raw) }.getOrNull() }

    private val TrustedTerminalHost.key: String
        get() = normalizedKey(host, port)

    @Serializable
    private data class StoredTrustedTerminalHosts(
        val hosts: List<TrustedTerminalHost> = emptyList(),
    )

    companion object {
        private const val HOSTS_KEY = "terminal.trusted_hosts"
        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        internal fun normalizedKey(
            host: String,
            port: Int,
        ): String = "${host.trim().lowercase()}:$port"
    }
}
