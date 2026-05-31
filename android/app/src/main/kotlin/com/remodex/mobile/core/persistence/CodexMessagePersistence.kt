package com.remodex.mobile.core.persistence

import android.content.Context
import com.remodex.mobile.core.model.CodexMessage
import com.remodex.mobile.core.model.CodexMessageKind
import com.remodex.mobile.core.security.CodexSecureKeys
import com.remodex.mobile.core.security.SecureStore
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Per-thread message timelines: encrypted per-thread cache + v6 bin / plaintext legacy fallbacks.
 * Mirrors [CodexMessagePersistence.swift](CodexMobile/CodexMobile/Services/CodexMessagePersistence.swift).
 */
class CodexMessagePersistence(
    private val context: Context,
    private val secureStore: SecureStore,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val mapSerializer =
        MapSerializer(String.serializer(), ListSerializer(CodexMessage.serializer()))
    private val listSerializer = ListSerializer(CodexMessage.serializer())

    private val primaryName = "codex-message-history-v6.bin"
    private val perThreadDirName = "codex-message-history-v7"
    private val macScopedDirName = "codex-message-history-mac-v1"
    private val tailCacheName = "codex-message-tail-v1.bin"
    private val threadIndexName = "codex-message-thread-index-v1.bin"
    private val migrationMarkerName = "codex-message-v7-migrated.marker"
    private val legacyNames =
        listOf(
            "codex-message-history-v5.json",
            "codex-message-history-v4.json",
            "codex-message-history-v3.json",
            "codex-message-history-v2.json",
            "codex-message-history.json",
        )

    fun loadInitialThreadTail(
        lastActiveThreadId: String?,
        tailLimit: Int,
        macDeviceId: String? = null,
    ): Map<String, List<CodexMessage>> {
        val tid = lastActiveThreadId?.trim()?.takeIf { it.isNotEmpty() } ?: return emptyMap()
        val limit = tailLimit.coerceAtLeast(1)
        loadThread(tid, macDeviceId)?.let { messages ->
            return mapOf(tid to messages.takeLast(limit))
        }
        loadTailCache(macDeviceId)[tid]?.let { messages ->
            return mapOf(tid to messages.takeLast(limit))
        }
        return emptyMap()
    }

    fun load(macDeviceId: String? = null): Map<String, List<CodexMessage>> {
        val perThread = loadPerThreadStore(macDeviceId)
        if (perThread.isNotEmpty()) return perThread
        for (file in storeFiles(macDeviceId)) {
            if (!file.exists() || !file.isFile || file.length() == 0L) continue
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: continue
            val decoded: Map<String, List<CodexMessage>>? =
                if (file.name == primaryName) {
                    val key = messageHistoryKey()
                    val plain = MessageHistoryAesGcm.decrypt(bytes, key) ?: continue
                    runCatching { json.decodeFromString(mapSerializer, plain.decodeToString()) }.getOrNull()
                } else {
                    runCatching { json.decodeFromString(mapSerializer, bytes.decodeToString()) }.getOrNull()
                }
            if (decoded != null) {
                return sanitizedForPersistence(decoded)
            }
        }
        return emptyMap()
    }

    fun save(
        value: Map<String, List<CodexMessage>>,
        macDeviceId: String? = null,
    ) {
        val sanitized = sanitizedForPersistence(value)
        savePerThreadStore(sanitized, macDeviceId)
        saveTailCache(sanitized, macDeviceId)
    }

    fun migrateLegacyMonolithToPerThreadIfNeeded() {
        val marker = storeDir().resolve(migrationMarkerName)
        if (marker.exists()) return
        val legacy = loadLegacyMonolith()
        if (legacy.isEmpty()) {
            runCatching { marker.writeText("empty") }
            return
        }
        val existingThreadIds = loadThreadIndex().values.toSet()
        val missing = legacy.filterKeys { it !in existingThreadIds }
        if (missing.isNotEmpty()) {
            savePerThreadStore(missing)
        }
        if (loadTailCache().isEmpty()) {
            saveTailCache(legacy)
        }
        runCatching { marker.writeText("done") }
    }

    private fun loadLegacyMonolith(): Map<String, List<CodexMessage>> {
        for (file in storeFiles()) {
            if (!file.exists() || !file.isFile || file.length() == 0L) continue
            val bytes = runCatching { file.readBytes() }.getOrNull() ?: continue
            val decoded: Map<String, List<CodexMessage>>? =
                if (file.name == primaryName) {
                    val key = messageHistoryKey()
                    val plain = MessageHistoryAesGcm.decrypt(bytes, key) ?: continue
                    runCatching { json.decodeFromString(mapSerializer, plain.decodeToString()) }.getOrNull()
                } else {
                    runCatching { json.decodeFromString(mapSerializer, bytes.decodeToString()) }.getOrNull()
                }
            if (decoded != null) {
                return sanitizedForPersistence(decoded)
            }
        }
        return emptyMap()
    }

    private fun loadPerThreadStore(macDeviceId: String?): Map<String, List<CodexMessage>> =
        perThreadDir(macDeviceId)
            .listFiles()
            .orEmpty()
            .asSequence()
            .filter { it.isFile && it.extension == "bin" && it.length() > 0L }
            .mapNotNull { file ->
                val threadId = threadIdFromFile(file, macDeviceId) ?: return@mapNotNull null
                val messages = loadThreadFile(file) ?: return@mapNotNull null
                threadId to messages
            }
            .toMap()

    private fun loadThread(
        threadId: String,
        macDeviceId: String?,
    ): List<CodexMessage>? {
        val tid = threadId.trim().takeIf { it.isNotEmpty() } ?: return null
        return loadThreadFile(threadFile(tid, macDeviceId))
    }

    private fun loadThreadFile(file: File): List<CodexMessage>? {
        if (!file.exists() || !file.isFile || file.length() == 0L) return null
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val key = messageHistoryKey()
        val plain = MessageHistoryAesGcm.decrypt(bytes, key) ?: return null
        return runCatching {
            json.decodeFromString(listSerializer, plain.decodeToString())
        }.getOrNull()?.let { sanitizedForPersistence(mapOf("_" to it))["_"].orEmpty() }
    }

    private fun savePerThreadStore(
        value: Map<String, List<CodexMessage>>,
        macDeviceId: String? = null,
    ) {
        val dir = perThreadDir(macDeviceId)
        dir.mkdirs()
        val index = loadThreadIndex(macDeviceId).toMutableMap()
        for ((threadId, messages) in value) {
            val tid = threadId.trim()
            if (tid.isEmpty()) continue
            val plaintext =
                runCatching { json.encodeToString(listSerializer, messages) }
                    .getOrNull()
                    ?.toByteArray(Charsets.UTF_8)
                    ?: continue
            val encrypted =
                runCatching { MessageHistoryAesGcm.encrypt(plaintext, messageHistoryKey()) }.getOrNull()
                    ?: continue
            threadFile(tid, macDeviceId).writeBytes(encrypted)
            index[hashedThreadId(tid)] = tid
        }
        saveThreadIndex(index, macDeviceId)
    }

    private fun loadTailCache(macDeviceId: String? = null): Map<String, List<CodexMessage>> {
        val file = storeDir(macDeviceId).resolve(tailCacheName)
        if (!file.exists() || !file.isFile || file.length() == 0L) return emptyMap()
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return emptyMap()
        val plain = MessageHistoryAesGcm.decrypt(bytes, messageHistoryKey()) ?: return emptyMap()
        return runCatching {
            json.decodeFromString(mapSerializer, plain.decodeToString())
        }.getOrNull()?.let(::sanitizedForPersistence).orEmpty()
    }

    private fun saveTailCache(
        value: Map<String, List<CodexMessage>>,
        macDeviceId: String? = null,
    ) {
        val tails = value.mapValues { (_, messages) -> messages.takeLast(DEFAULT_TAIL_CACHE_LIMIT) }
        val plaintext =
            runCatching { json.encodeToString(mapSerializer, tails) }
                .getOrNull()
                ?.toByteArray(Charsets.UTF_8)
                ?: return
        val key = messageHistoryKey()
        val encrypted =
            runCatching { MessageHistoryAesGcm.encrypt(plaintext, key) }.getOrNull()
                ?: return
        val file = storeDir(macDeviceId).resolve(tailCacheName)
        file.parentFile?.mkdirs()
        file.writeBytes(encrypted)
    }

    private fun storeDir(macDeviceId: String? = null): File {
        val dir = File(context.filesDir, context.packageName)
        dir.mkdirs()
        val device = macDeviceId?.trim()?.takeIf { it.isNotEmpty() } ?: return dir
        return dir.resolve(macScopedDirName).resolve(hashedValue(device)).apply { mkdirs() }
    }

    private fun storeFiles(macDeviceId: String? = null): List<File> {
        val dir = storeDir(macDeviceId)
        return listOf(dir.resolve(primaryName)) + legacyNames.map { dir.resolve(it) }
    }

    private fun perThreadDir(macDeviceId: String?): File = storeDir(macDeviceId).resolve(perThreadDirName)

    private fun threadFile(
        threadId: String,
        macDeviceId: String?,
    ): File = perThreadDir(macDeviceId).resolve("${hashedThreadId(threadId)}.bin")

    private fun threadIdFromFile(
        file: File,
        macDeviceId: String?,
    ): String? {
        val hash = file.nameWithoutExtension
        loadThreadIndex(macDeviceId)[hash]?.let { return it }
        return loadTailCache(macDeviceId).entries.firstOrNull { hashedThreadId(it.key) == hash }?.key
    }

    private fun loadThreadIndex(macDeviceId: String? = null): Map<String, String> {
        val file = storeDir(macDeviceId).resolve(threadIndexName)
        if (!file.exists() || !file.isFile || file.length() == 0L) return emptyMap()
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return emptyMap()
        val plain = MessageHistoryAesGcm.decrypt(bytes, messageHistoryKey()) ?: return emptyMap()
        val serializer = MapSerializer(String.serializer(), String.serializer())
        return runCatching {
            json.decodeFromString(serializer, plain.decodeToString())
        }.getOrNull().orEmpty()
    }

    private fun saveThreadIndex(
        index: Map<String, String>,
        macDeviceId: String?,
    ) {
        val serializer = MapSerializer(String.serializer(), String.serializer())
        val plaintext =
            runCatching { json.encodeToString(serializer, index) }
                .getOrNull()
                ?.toByteArray(Charsets.UTF_8)
                ?: return
        val encrypted =
            runCatching { MessageHistoryAesGcm.encrypt(plaintext, messageHistoryKey()) }.getOrNull()
                ?: return
        storeDir(macDeviceId).resolve(threadIndexName).writeBytes(encrypted)
    }

    private fun hashedThreadId(threadId: String): String {
        return hashedValue(threadId)
    }

    private fun hashedValue(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun messageHistoryKey(): ByteArray {
        secureStore.readData(CodexSecureKeys.messageHistoryKey)?.let {
            if (it.size == 32) return it
        }
        val newKey = MessageHistoryAesGcm.generateKey()
        secureStore.writeData(CodexSecureKeys.messageHistoryKey, newKey)
        return newKey
    }

    private fun sanitizedForPersistence(value: Map<String, List<CodexMessage>>): Map<String, List<CodexMessage>> =
        value.mapValues { (_, messages) ->
            messages.filter {
                it.kind != CodexMessageKind.userInputPrompt &&
                    it.kind != CodexMessageKind.pendingApproval
            }
        }

    private companion object {
        const val DEFAULT_TAIL_CACHE_LIMIT = 48
    }
}
