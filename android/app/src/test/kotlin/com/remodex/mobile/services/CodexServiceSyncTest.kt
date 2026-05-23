package com.remodex.mobile.services

import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.CodexThreadSyncState
import kotlin.test.Test
import kotlin.test.assertEquals

class CodexServiceSyncTest {
    @Test
    fun mergeFetchedThreadsPreservingLocalRows_keepsActiveLocalThreadMissingFromStaleFetch() {
        val localNewThread =
            CodexThread(
                id = "thread-new",
                title = "New Thread",
                cwd = "/repo",
            )
        val fetchedThread = CodexThread(id = "thread-old", title = "Existing")

        val merged =
            mergeFetchedThreadsPreservingLocalRows(
                fetched = listOf(fetchedThread),
                previous = listOf(localNewThread),
                preserveLocalThreadIds = setOf("thread-new"),
                persistedRename = { null },
            )

        assertEquals(setOf("thread-old", "thread-new"), merged.map { it.id }.toSet())
        assertEquals("/repo", merged.single { it.id == "thread-new" }.cwd)
    }

    @Test
    fun mergeFetchedThreadsPreservingLocalRows_doesNotKeepUnprotectedLocalThread() {
        val localNewThread = CodexThread(id = "thread-new", title = "New Thread", cwd = "/repo")
        val fetchedThread = CodexThread(id = "thread-old", title = "Existing")

        val merged =
            mergeFetchedThreadsPreservingLocalRows(
                fetched = listOf(fetchedThread),
                previous = listOf(localNewThread),
                preserveLocalThreadIds = emptySet(),
                persistedRename = { null },
            )

        assertEquals(listOf("thread-old"), merged.map { it.id })
    }

    @Test
    fun mergeFetchedThreadsPreservingLocalRows_doesNotKeepLocallyArchivedThread() {
        val archivedThread =
            CodexThread(
                id = "thread-new",
                title = "New Thread",
                syncState = CodexThreadSyncState.archivedLocal,
            )
        val fetchedThread = CodexThread(id = "thread-old", title = "Existing")

        val merged =
            mergeFetchedThreadsPreservingLocalRows(
                fetched = listOf(fetchedThread),
                previous = listOf(archivedThread),
                preserveLocalThreadIds = setOf("thread-new"),
                persistedRename = { null },
            )

        assertEquals(listOf("thread-old"), merged.map { it.id })
    }
}
