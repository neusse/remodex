package com.remodex.mobile.services

import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.CodexThreadSyncState
import com.remodex.mobile.data.ThreadListSync

/**
 * Mirrors [CodexService+Sync.swift](../../../../../../../../CodexMobile/CodexMobile/Services/CodexService+Sync.swift).
 */
internal suspend fun CodexService.refreshThreadsInternal() {
    if (!sessionReady) return
    val locallyDeleted = sessionPersistence.loadLocallyDeletedThreadIds()
    val locallyArchived = sessionPersistence.loadLocallyArchivedThreadIds()
    val fetched =
        runCatching { ThreadListSync.fetchMerged(this) }.getOrElse { return }
            .filter { it.id !in locallyDeleted }
            .map { thread ->
                if (thread.id in locallyArchived && thread.syncState == CodexThreadSyncState.live) {
                    thread.copy(syncState = CodexThreadSyncState.archivedLocal)
                } else {
                    thread
                }
            }
    val serverThreads = fetched.map { incoming -> applyAuthoritativeProjectPathToServerThread(incoming) }
    val preserveLocalThreadIds =
        (
            setOfNotNull(_activeThreadId.value?.trim()?.takeIf { it.isNotEmpty() }) +
                _runningTurnIdByThread.value.keys +
                _protectedRunningFallbackThreadIds.value
        ).filter { it !in locallyDeleted && it !in locallyArchived }.toSet()
    val merged =
        mergeFetchedThreadsPreservingLocalRows(
            fetched = serverThreads,
            previous = _threads.value,
            preserveLocalThreadIds = preserveLocalThreadIds,
            persistedRename = { tid -> persistedThreadRename(tid) },
        )
    publishThreads(merged)
}

internal fun CodexService.publishThreads(threads: List<CodexThread>) {
    _threads.value = threads
    sessionPersistence.saveCachedThreads(threads)
}

internal fun mergeFetchedThreadsPreservingLocalRows(
    fetched: List<CodexThread>,
    previous: List<CodexThread>,
    preserveLocalThreadIds: Set<String>,
    persistedRename: (String) -> String?,
): List<CodexThread> {
    val merged =
        mergeThreadListWithPersistedRenames(
            fetched = fetched,
            previous = previous,
            persistedRename = persistedRename,
        )
    val mergedIds = merged.mapTo(mutableSetOf()) { it.id }
    val previousById = previous.associateBy { it.id }
    val preserved =
        preserveLocalThreadIds.mapNotNull { id ->
            previousById[id]
                ?.takeIf { it.syncState == CodexThreadSyncState.live && it.id !in mergedIds }
                ?.let { thread -> thread.withPersistedThreadRename(persistedRename(thread.id)) }
        }
    if (preserved.isEmpty()) return merged
    return sortThreadsForBridge(merged + preserved)
}
