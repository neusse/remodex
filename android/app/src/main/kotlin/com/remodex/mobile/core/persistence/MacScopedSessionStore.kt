package com.remodex.mobile.core.persistence

import android.content.Context
import com.remodex.mobile.core.model.CodexCollaborationModeKind
import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.CodexThreadSyncState
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Mac-scoped local UI/session state. Keys are namespaced by [macDeviceId] so multi-Mac switching
 * can save/load cached threads, active thread, renames, and runtime selections independently.
 */
class MacScopedSessionStore(
    private val sessionPersistence: SessionPersistence,
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            "remodex_mac_scoped_state",
            Context.MODE_PRIVATE,
        )

    fun normalizedMacDeviceId(macDeviceId: String?): String? =
        macDeviceId?.trim()?.takeIf { it.isNotEmpty() }

    fun scopedKey(
        baseKey: String,
        macDeviceId: String?,
    ): String = formatScopedKey(baseKey, macDeviceId)

    fun loadCachedThreads(macDeviceId: String?): List<CodexThread> =
        decodeCachedThreads(readScopedString(KEY_CACHED_THREADS, macDeviceId))
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadCachedThreads() else emptyList()

    fun saveCachedThreads(
        macDeviceId: String?,
        threads: List<CodexThread>,
    ) {
        writeScopedString(KEY_CACHED_THREADS, macDeviceId, encodeCachedThreads(threads))
        markLegacyMigrated()
    }

    fun loadLastActiveThreadId(macDeviceId: String?): String? =
        readScopedString(KEY_LAST_ACTIVE_THREAD, macDeviceId)
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadLastActiveThreadId() else null

    fun saveLastActiveThreadId(
        macDeviceId: String?,
        threadId: String?,
    ) {
        writeScopedString(KEY_LAST_ACTIVE_THREAD, macDeviceId, threadId?.trim()?.takeIf { it.isNotEmpty() })
        markLegacyMigrated()
    }

    fun loadThreadRenames(macDeviceId: String?): Map<String, String> =
        decodeStringMap(readScopedStringSet(KEY_THREAD_RENAMES, macDeviceId))
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadThreadRenames() else emptyMap()

    fun saveThreadRenames(
        macDeviceId: String?,
        renames: Map<String, String>,
    ) {
        writeScopedStringSet(KEY_THREAD_RENAMES, macDeviceId, encodeStringMap(renames))
        markLegacyMigrated()
    }

    fun loadAssociatedManagedWorktreePaths(macDeviceId: String?): Map<String, String> =
        decodeStringMap(readScopedStringSet(KEY_ASSOCIATED_WORKTREES, macDeviceId))
            ?: if (shouldLoadLegacyFallback(macDeviceId)) {
                sessionPersistence.loadAssociatedManagedWorktreePaths()
            } else {
                emptyMap()
            }

    fun saveAssociatedManagedWorktreePaths(
        macDeviceId: String?,
        paths: Map<String, String>,
    ) {
        writeScopedStringSet(KEY_ASSOCIATED_WORKTREES, macDeviceId, encodeStringMap(paths))
        markLegacyMigrated()
    }

    fun loadRuntimeSelection(macDeviceId: String?): RuntimeSelectionSnapshot =
        decodeRuntimeSelection(readScopedStringMap(KEY_RUNTIME_SELECTION, macDeviceId))
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadRuntimeSelection()
            else RuntimeSelectionSnapshot()

    fun saveRuntimeSelection(
        macDeviceId: String?,
        snapshot: RuntimeSelectionSnapshot,
    ) {
        writeScopedStringMap(KEY_RUNTIME_SELECTION, macDeviceId, encodeRuntimeSelection(snapshot))
        markLegacyMigrated()
    }

    fun loadComposerDrafts(macDeviceId: String?): Map<String, String> =
        decodeDraftMap(readScopedStringSet(KEY_COMPOSER_DRAFTS, macDeviceId)).orEmpty()

    fun saveComposerDraft(
        macDeviceId: String?,
        threadId: String,
        draft: String,
    ) {
        val tid = threadId.trim()
        if (tid.isEmpty()) return
        val next = loadComposerDrafts(macDeviceId).toMutableMap()
        val value = draft
        if (value.isEmpty()) {
            next.remove(tid)
        } else {
            next[tid] = value
        }
        writeScopedStringSet(KEY_COMPOSER_DRAFTS, macDeviceId, encodeStringMap(next))
        markLegacyMigrated()
    }

    fun clearComposerDraft(
        macDeviceId: String?,
        threadId: String,
    ) {
        saveComposerDraft(macDeviceId, threadId, "")
    }

    fun loadLocallyDeletedThreadIds(macDeviceId: String?): Set<String> =
        readScopedStringSet(KEY_LOCALLY_DELETED, macDeviceId)?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadLocallyDeletedThreadIds()
            else emptySet()

    fun saveLocallyDeletedThreadIds(
        macDeviceId: String?,
        ids: Set<String>,
    ) {
        writeScopedStringSet(KEY_LOCALLY_DELETED, macDeviceId, ids.map { it.trim() }.filter { it.isNotEmpty() }.toSet())
        markLegacyMigrated()
    }

    fun loadLocallyArchivedThreadIds(macDeviceId: String?): Set<String> =
        readScopedStringSet(KEY_LOCALLY_ARCHIVED, macDeviceId)?.map { it.trim() }?.filter { it.isNotEmpty() }?.toSet()
            ?: if (shouldLoadLegacyFallback(macDeviceId)) sessionPersistence.loadLocallyArchivedThreadIds()
            else emptySet()

    fun saveLocallyArchivedThreadIds(
        macDeviceId: String?,
        ids: Set<String>,
    ) {
        writeScopedStringSet(KEY_LOCALLY_ARCHIVED, macDeviceId, ids.map { it.trim() }.filter { it.isNotEmpty() }.toSet())
        markLegacyMigrated()
    }

    fun clearDevice(macDeviceId: String?) {
        val device = normalizedMacDeviceId(macDeviceId) ?: return
        val editor = prefs.edit()
        ALL_SCOPED_KEYS.forEach { baseKey ->
            editor.remove(scopedKey(baseKey, device))
        }
        editor.apply()
    }

    fun shouldLoadLegacyFallback(macDeviceId: String?): Boolean {
        if (normalizedMacDeviceId(macDeviceId) == null) return false
        return !prefs.getBoolean(KEY_LEGACY_MIGRATED, false)
    }

    private fun markLegacyMigrated() {
        prefs.edit().putBoolean(KEY_LEGACY_MIGRATED, true).apply()
    }

    private fun readScopedString(
        baseKey: String,
        macDeviceId: String?,
    ): String? = prefs.getString(scopedKey(baseKey, macDeviceId), null)?.trim()?.takeIf { it.isNotEmpty() }

    private fun writeScopedString(
        baseKey: String,
        macDeviceId: String?,
        value: String?,
    ) {
        val key = scopedKey(baseKey, macDeviceId)
        val editor = prefs.edit()
        if (value.isNullOrBlank()) editor.remove(key) else editor.putString(key, value.trim())
        editor.apply()
    }

    private fun readScopedStringSet(
        baseKey: String,
        macDeviceId: String?,
    ): Set<String>? = prefs.getStringSet(scopedKey(baseKey, macDeviceId), null)

    private fun writeScopedStringSet(
        baseKey: String,
        macDeviceId: String?,
        values: Set<String>,
    ) {
        val key = scopedKey(baseKey, macDeviceId)
        val editor = prefs.edit()
        if (values.isEmpty()) editor.remove(key) else editor.putStringSet(key, values)
        editor.apply()
    }

    private fun readScopedStringMap(
        baseKey: String,
        macDeviceId: String?,
    ): Map<String, String>? = decodeStringMap(readScopedStringSet(baseKey, macDeviceId))

    private fun writeScopedStringMap(
        baseKey: String,
        macDeviceId: String?,
        values: Map<String, String>,
    ) {
        writeScopedStringSet(baseKey, macDeviceId, encodeStringMap(values))
    }

    private fun encodeStringMap(values: Map<String, String>): Set<String> =
        values.map { (key, value) -> "${urlEncode(key)}\t${urlEncode(value)}" }.toSet()

    private fun decodeStringMap(raw: Set<String>?): Map<String, String>? {
        if (raw == null) return null
        return raw.mapNotNull { entry ->
            val parts = entry.split('\t', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val key = runCatching { urlDecode(parts[0]) }.getOrNull()?.trim().orEmpty()
            val value = runCatching { urlDecode(parts[1]) }.getOrNull()?.trim().orEmpty()
            if (key.isEmpty() || value.isEmpty()) return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun decodeDraftMap(raw: Set<String>?): Map<String, String>? {
        if (raw == null) return null
        return raw.mapNotNull { entry ->
            val parts = entry.split('\t', limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val key = runCatching { urlDecode(parts[0]) }.getOrNull()?.trim().orEmpty()
            val value = runCatching { urlDecode(parts[1]) }.getOrNull().orEmpty()
            if (key.isEmpty() || value.isEmpty()) return@mapNotNull null
            key to value
        }.toMap()
    }

    private fun encodeRuntimeSelection(snapshot: RuntimeSelectionSnapshot): Map<String, String> =
        buildMap {
            snapshot.selectedModelId?.let { put("model", it) }
            snapshot.selectedReasoningEffort?.let { put("reasoning", it) }
            snapshot.selectedAccessMode?.let { put("access", it) }
            snapshot.selectedServiceTier?.let { put("tier", it) }
        }

    private fun decodeRuntimeSelection(raw: Map<String, String>?): RuntimeSelectionSnapshot? {
        if (raw == null) return null
        return RuntimeSelectionSnapshot(
            selectedModelId = raw["model"],
            selectedReasoningEffort = raw["reasoning"],
            selectedAccessMode = raw["access"],
            selectedServiceTier = raw["tier"],
        )
    }

    private fun encodeCachedThreads(threads: List<CodexThread>): String =
        encodeCachedThreadSnapshot(threads)

    private fun decodeCachedThreads(raw: String?): List<CodexThread>? {
        if (raw.isNullOrBlank()) return emptyList()
        return decodeCachedThreadSnapshot(raw)
    }

    companion object {
        fun formatScopedKey(
            baseKey: String,
            macDeviceId: String?,
        ): String {
            val normalized = macDeviceId?.trim()?.takeIf { it.isNotEmpty() } ?: return baseKey
            return "mac.$normalized.$baseKey"
        }

        private const val KEY_CACHED_THREADS = "codex.thread.cachedThreads"
        const val KEY_LAST_ACTIVE_THREAD = "codex.ui.lastActiveThreadId"
        const val KEY_THREAD_RENAMES = "codex.thread.renamedThreadNames"
        const val KEY_ASSOCIATED_WORKTREES = "codex.thread.associatedManagedWorktrees"
        const val KEY_RUNTIME_SELECTION = "codex.runtime.selection"
        const val KEY_COMPOSER_DRAFTS = "codex.composer.draftsByThread"
        const val KEY_LOCALLY_DELETED = "codex.locallyDeletedThreadIDs"
        const val KEY_LOCALLY_ARCHIVED = "codex.locallyArchivedThreadIDs"
        const val KEY_LEGACY_MIGRATED = "codex.macScoped.legacyMigrated"

        private val ALL_SCOPED_KEYS =
            listOf(
                KEY_CACHED_THREADS,
                KEY_LAST_ACTIVE_THREAD,
                KEY_THREAD_RENAMES,
                KEY_ASSOCIATED_WORKTREES,
                KEY_RUNTIME_SELECTION,
                KEY_COMPOSER_DRAFTS,
                KEY_LOCALLY_DELETED,
                KEY_LOCALLY_ARCHIVED,
            )

        internal fun encodeCachedThreadSnapshot(threads: List<CodexThread>): String =
            threads.joinToString("\n") { thread ->
                listOf(
                    thread.id,
                    thread.title.orEmpty(),
                    thread.name.orEmpty(),
                    thread.preview.orEmpty(),
                    thread.createdAt?.toString().orEmpty(),
                    thread.updatedAt?.toString().orEmpty(),
                    thread.cwd.orEmpty(),
                    thread.syncState.name,
                    thread.forkedFromThreadId.orEmpty(),
                    thread.parentThreadId.orEmpty(),
                    thread.agentId.orEmpty(),
                    thread.agentNickname.orEmpty(),
                    thread.agentRole.orEmpty(),
                    thread.model.orEmpty(),
                    thread.modelProvider.orEmpty(),
                    thread.collaborationMode.name,
                ).joinToString("\t") { urlEncode(it) }
            }

        internal fun decodeCachedThreadSnapshot(raw: String): List<CodexThread> =
            raw.lineSequence().mapNotNull { line ->
                val parts =
                    line.split('\t')
                        .map { runCatching { urlDecode(it) }.getOrNull() ?: return@mapNotNull null }
                val id = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
                CodexThread(
                    id = id,
                    title = parts.getOrNull(1).nonBlankOrNull(),
                    name = parts.getOrNull(2).nonBlankOrNull(),
                    preview = parts.getOrNull(3).nonBlankOrNull(),
                    createdAt = parts.getOrNull(4).parseInstantOrNull(),
                    updatedAt = parts.getOrNull(5).parseInstantOrNull(),
                    cwd = CodexThread.normalizeProjectPath(parts.getOrNull(6)),
                    syncState =
                        parts.getOrNull(7)
                            ?.let { runCatching { CodexThreadSyncState.valueOf(it) }.getOrNull() }
                            ?: CodexThreadSyncState.live,
                    forkedFromThreadId = CodexThread.normalizeIdentifier(parts.getOrNull(8)),
                    parentThreadId = CodexThread.normalizeIdentifier(parts.getOrNull(9)),
                    agentId = CodexThread.normalizeIdentifier(parts.getOrNull(10)),
                    agentNickname = CodexThread.normalizeIdentifier(parts.getOrNull(11)),
                    agentRole = CodexThread.normalizeIdentifier(parts.getOrNull(12)),
                    model = CodexThread.normalizeIdentifier(parts.getOrNull(13)),
                    modelProvider = CodexThread.normalizeIdentifier(parts.getOrNull(14)),
                    collaborationMode =
                        parts.getOrNull(15)
                            ?.let { raw -> runCatching { CodexCollaborationModeKind.valueOf(raw) }.getOrNull() }
                            ?: CodexCollaborationModeKind.default,
                )
            }.toList()
    }
}

private fun String?.nonBlankOrNull(): String? =
    this?.trim()?.takeIf { it.isNotEmpty() }

private fun String?.parseInstantOrNull(): Instant? =
    this?.trim()?.takeIf { it.isNotEmpty() }?.let { raw ->
        runCatching { Instant.parse(raw) }.getOrNull()
    }

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())

private fun urlDecode(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
