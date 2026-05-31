package com.remodex.mobile.core.model

import com.remodex.mobile.core.transport.ConnectionState
import com.remodex.mobile.data.CodexRepository

object PetCompanionStatusLogic {
    fun snapshot(repository: CodexRepository): PetCompanionStatusSnapshot {
        val pendingApproval = repository.pendingApprovalRequest.value
        if (pendingApproval != null) {
            return PetCompanionStatusSnapshot(
                phase = PetCompanionPhase.Waiting,
                title = "Approval needed",
                detail = pendingApproval.threadId?.let { "Waiting for you" } ?: "Waiting for you",
            )
        }
        val runningThreadIds = petRunningThreadIds(repository)
        if (runningThreadIds.isNotEmpty()) {
            val threadId = preferredThreadId(runningThreadIds, repository.activeThreadId.value, repository.threads.value)
            return PetCompanionStatusSnapshot(
                phase = PetCompanionPhase.Running,
                title = if (runningThreadIds.size > 1) "Working ${runningThreadIds.size} chats" else "Working",
                detail = threadTitle(threadId, repository),
            )
        }
        return PetCompanionStatusSnapshot.idle
    }

    private fun petRunningThreadIds(repository: CodexRepository): Set<String> {
        val running = repository.runningTurnIdByThread.value.keys
        val protected = repository.protectedRunningFallbackThreadIds.value
        return running + protected
    }

    private fun preferredThreadId(
        candidates: Set<String>,
        activeThreadId: String?,
        threads: List<CodexThread>,
    ): String? {
        if (candidates.isEmpty()) return null
        if (activeThreadId != null && candidates.contains(activeThreadId)) return activeThreadId
        return threads.firstOrNull { candidates.contains(it.id) }?.id ?: candidates.sorted().firstOrNull()
    }

    private fun threadTitle(
        threadId: String?,
        repository: CodexRepository,
    ): String {
        val thread = threadId?.let { id -> repository.threads.value.firstOrNull { it.id == id } }
        val title = thread?.displayTitle?.trim().orEmpty()
        return title.ifEmpty { "Chat" }
    }
}

private val CodexThread.displayTitle: String
    get() = title?.trim()?.takeIf { it.isNotEmpty() } ?: name?.trim()?.takeIf { it.isNotEmpty() }.orEmpty()

fun ConnectionState.isConnectedForPet(): Boolean = this is ConnectionState.Connected
