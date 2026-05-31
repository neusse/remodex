package com.remodex.mobile.core.persistence

import com.remodex.mobile.core.model.CodexCollaborationModeKind
import com.remodex.mobile.core.model.CodexThread
import com.remodex.mobile.core.model.CodexThreadSyncState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class MacScopedSessionStoreTest {
    @Test
    fun formatScopedKey_prefixesMacDeviceId() {
        assertEquals(
            "mac.mac-a.codex.ui.lastActiveThreadId",
            MacScopedSessionStore.formatScopedKey("codex.ui.lastActiveThreadId", "mac-a"),
        )
        assertEquals(
            "mac.mac-a.codex.composer.draftsByThread",
            MacScopedSessionStore.formatScopedKey(MacScopedSessionStore.KEY_COMPOSER_DRAFTS, "mac-a"),
        )
        assertEquals(
            "codex.ui.lastActiveThreadId",
            MacScopedSessionStore.formatScopedKey("codex.ui.lastActiveThreadId", null),
        )
        assertEquals(
            "codex.ui.lastActiveThreadId",
            MacScopedSessionStore.formatScopedKey("codex.ui.lastActiveThreadId", "  "),
        )
    }

    @Test
    fun cachedThreadSnapshot_roundTripsMetadataAndEscapedText() {
        val original =
            CodexThread(
                id = "thread-1",
                title = "Title\twith tab",
                name = "Name\nwith newline",
                preview = "Preview with symbols %20",
                createdAt = Instant.parse("2026-05-01T10:00:00Z"),
                updatedAt = Instant.parse("2026-05-02T10:00:00Z"),
                cwd = "C:\\Users\\andre\\Project",
                syncState = CodexThreadSyncState.archivedLocal,
                forkedFromThreadId = "thread-0",
                parentThreadId = "parent-1",
                agentId = "agent-1",
                agentNickname = "Agent",
                agentRole = "reviewer",
                model = "gpt-5",
                modelProvider = "openai",
                collaborationMode = CodexCollaborationModeKind.plan,
            )

        val encoded = MacScopedSessionStore.encodeCachedThreadSnapshot(listOf(original))
        val decoded = MacScopedSessionStore.decodeCachedThreadSnapshot(encoded)

        assertEquals(listOf(original), decoded)
    }
}
