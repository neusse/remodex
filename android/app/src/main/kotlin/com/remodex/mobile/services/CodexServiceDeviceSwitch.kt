package com.remodex.mobile.services

import com.remodex.mobile.core.model.CodexAccessMode
import com.remodex.mobile.core.model.CodexCollaborationModeKind
import com.remodex.mobile.core.model.CodexPairingQRPayload
import com.remodex.mobile.core.model.CodexServiceTier
import com.remodex.mobile.core.model.CodexTrustedSessionResolveError
import com.remodex.mobile.core.persistence.RuntimeSelectionSnapshot
import com.remodex.mobile.core.transport.ConnectionState
import com.remodex.mobile.pairing.buildWebSocketConnectParams
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock

internal suspend fun CodexService.switchToTrustedDeviceImpl(deviceId: String) {
    val normalizedTarget = normalizedMacDeviceId(deviceId) ?: throw CodexTrustedSessionResolveError.NoTrustedMac
    if (normalizedTarget == normalizedCurrentTrustedMacDeviceId()) return
    deviceSwitchMutex.withLock {
        if (_switchingDeviceId.value != null) return
        isCancellingDeviceSwitch = false
        _switchingDeviceId.value = normalizedTarget
        _deviceSwitchNotice.value = null
        val previousCurrent = normalizedCurrentTrustedMacDeviceId()
        var effectiveTarget = normalizedTarget
        try {
            val reconnectParams =
                preferredReconnectParams(normalizedTarget)
                    ?: run {
                        val message = "Could not reconnect to the selected device."
                        _deviceSwitchNotice.value = message
                        throw CodexTrustedSessionResolveError.MacOffline(message)
                    }
            interruptRunningTurnsBeforeDeviceSwitchIfNeeded()
            saveMacScopedLocalState(previousCurrent)
            beginMacSwitchContext(effectiveTarget)
            previousCurrent?.let { setPreviousTrustedMacDeviceId(it) } ?: clearPreviousTrustedMacDeviceId()
            setCurrentTrustedMacDeviceId(effectiveTarget)
            disconnectImpl(preservePresentationState = false)
            prepareMacSwitchState(effectiveTarget, loadCachedMessages = false)
            val (url, token) = reconnectParams
            connectImpl(url, token, role = null)
            setCurrentTrustedMacDeviceId(effectiveTarget)
            _deviceSwitchNotice.value = null
            endMacSwitchContext()
        } catch (error: CancellationException) {
            finalizeCancelledDeviceSwitch(previousCurrent)
            throw error
        } catch (error: Exception) {
            if (isCancellingDeviceSwitch) {
                finalizeCancelledDeviceSwitch(previousCurrent)
                throw CancellationException()
            }
            if (_connectionState.value is ConnectionState.Connected || sessionReady) {
                setCurrentTrustedMacDeviceId(previousCurrent)
                clearPreviousTrustedMacDeviceId()
                _deviceSwitchNotice.value = error.message ?: "Could not switch devices."
                endMacSwitchContext()
                throw error
            }
            setCurrentTrustedMacDeviceId(effectiveTarget)
            macScopedContextOverrideDeviceId = effectiveTarget
            prepareMacSwitchState(effectiveTarget, loadCachedMessages = true)
            _deviceSwitchNotice.value = error.message ?: "Could not switch devices."
            endMacSwitchContext()
            throw error
        } finally {
            _switchingDeviceId.value = null
            refreshTrustedDevices()
        }
    }
}

internal fun CodexService.loadMacScopedInitialMessages(
    macDeviceId: String?,
    lastActiveThreadId: String? = macScopedSessionStore.loadLastActiveThreadId(macDeviceId),
) =
    messagePersistence.loadInitialThreadTail(
        lastActiveThreadId = lastActiveThreadId,
        tailLimit = INITIAL_TIMELINE_TAIL_LIMIT,
        macDeviceId = normalizedMacDeviceId(macDeviceId),
    ).ifEmpty {
        if (macScopedSessionStore.shouldLoadLegacyFallback(macDeviceId)) {
            messagePersistence.loadInitialThreadTail(
                lastActiveThreadId = lastActiveThreadId,
                tailLimit = INITIAL_TIMELINE_TAIL_LIMIT,
            )
        } else {
            emptyMap()
        }
    }

internal suspend fun CodexService.switchToScannedDeviceImpl(payload: CodexPairingQRPayload) {
    deviceSwitchMutex.withLock {
        if (_switchingDeviceId.value != null) return
        isCancellingDeviceSwitch = false
        _switchingDeviceId.value = payload.macDeviceId
        _deviceSwitchNotice.value = null
        val previousCurrent = normalizedCurrentTrustedMacDeviceId()
        val previousSnapshot = captureRelaySessionSnapshot()
        try {
            interruptRunningTurnsBeforeDeviceSwitchIfNeeded()
            saveMacScopedLocalState(previousCurrent)
            beginMacSwitchContext(payload.macDeviceId)
            rememberRelayPairing(payload)
            prepareMacSwitchState(payload.macDeviceId, loadCachedMessages = false)
            val (url, token) =
                buildWebSocketConnectParams(
                    sessionPersistence.loadRelaySnapshot(),
                    sessionPersistence.loadLocalRelayHostOverride().orEmpty(),
                )
            connectImpl(url, token, role = null)
            previousCurrent?.let { setPreviousTrustedMacDeviceId(it) } ?: clearPreviousTrustedMacDeviceId()
            endMacSwitchContext()
        } catch (error: CancellationException) {
            finalizeCancelledDeviceSwitch(previousCurrent)
            throw error
        } catch (error: Exception) {
            if (isCancellingDeviceSwitch) {
                finalizeCancelledDeviceSwitch(previousCurrent)
                throw CancellationException()
            }
            setCurrentTrustedMacDeviceId(previousCurrent)
            restoreRelaySessionSnapshot(previousSnapshot)
            macScopedContextOverrideDeviceId = previousCurrent
            prepareMacSwitchState(previousCurrent, loadCachedMessages = true)
            endMacSwitchContext()
            throw error
        } finally {
            _switchingDeviceId.value = null
            refreshTrustedDevices()
        }
    }
}

internal suspend fun CodexService.cancelDeviceSwitchImpl() {
    if (_switchingDeviceId.value == null) return
    isCancellingDeviceSwitch = true
    cancelTrustedSessionResolve()
    if (_connectionState.value is ConnectionState.Connecting ||
        _connectionState.value is ConnectionState.Connected ||
        sessionReady
    ) {
        disconnectImpl(preservePresentationState = false)
    }
}

private suspend fun CodexService.finalizeCancelledDeviceSwitch(previousCurrent: String?) {
    cancelTrustedSessionResolve()
    disconnectImpl(preservePresentationState = false)
    setCurrentTrustedMacDeviceId(null)
    previousCurrent?.let { setPreviousTrustedMacDeviceId(it) }
    clearSavedRelaySession()
    clearInMemoryMacScopedState()
    endMacSwitchContext()
    _deviceSwitchNotice.value = "Switch cancelled. Choose a device to reconnect."
}

private fun CodexService.beginMacSwitchContext(macDeviceId: String?) {
    suspendAutomaticMacScopedPersistence = true
    macScopedContextOverrideDeviceId = normalizedMacDeviceId(macDeviceId)
}

private suspend fun CodexService.prepareMacSwitchState(
    macDeviceId: String?,
    loadCachedMessages: Boolean,
) {
    clearInMemoryMacScopedState()
    if (loadCachedMessages) {
        loadMacScopedLocalState(macDeviceId)
        messageTimelineStore.replaceAll(loadMacScopedInitialMessages(macDeviceId))
    }
    loadMacScopedDefaultsState(macDeviceId)
}

private fun CodexService.endMacSwitchContext() {
    macScopedContextOverrideDeviceId = null
    suspendAutomaticMacScopedPersistence = false
}

internal fun CodexService.saveMacScopedLocalState(macDeviceId: String?) {
    if (suspendAutomaticMacScopedPersistence) return
    val store = macScopedSessionStore
    val device = resolvedMacScopedPersistenceDeviceId() ?: macDeviceId ?: return
    store.saveCachedThreads(device, _threads.value)
    store.saveLastActiveThreadId(device, _activeThreadId.value)
    store.saveThreadRenames(device, persistedThreadRenameById.toMap())
    store.saveAssociatedManagedWorktreePaths(device, associatedManagedWorktreePathByThreadId.toMap())
    store.saveLocallyDeletedThreadIds(device, loadScopedLocallyDeletedThreadIds())
    store.saveLocallyArchivedThreadIds(device, loadScopedLocallyArchivedThreadIds())
    store.saveRuntimeSelection(
        device,
        RuntimeSelectionSnapshot(
            selectedModelId = _selectedModelId.value,
            selectedReasoningEffort = _selectedReasoningEffort.value,
            selectedAccessMode = _selectedAccessMode.value.name,
            selectedServiceTier = _selectedServiceTier.value?.name,
        ),
    )
    messagePersistence.save(messageTimelineStore.messagesByThread.value, device)
}

internal fun CodexService.loadMacScopedLocalState(macDeviceId: String?) {
    val store = macScopedSessionStore
    val device = normalizedMacDeviceId(macDeviceId) ?: return
    isApplyingMacScopedState = true
    try {
        _threads.value = store.loadCachedThreads(device)
        _activeThreadId.value = store.loadLastActiveThreadId(device)
        persistedThreadRenameById.clear()
        persistedThreadRenameById.putAll(store.loadThreadRenames(device))
        associatedManagedWorktreePathByThreadId.clear()
        associatedManagedWorktreePathByThreadId.putAll(store.loadAssociatedManagedWorktreePaths(device))
    } finally {
        isApplyingMacScopedState = false
    }
}

internal fun CodexService.loadMacScopedDefaultsState(macDeviceId: String?) {
    val store = macScopedSessionStore
    val device = normalizedMacDeviceId(macDeviceId) ?: return
    isApplyingMacScopedState = true
    try {
        val runtime = store.loadRuntimeSelection(device)
        _selectedModelId.value = runtime.selectedModelId
        _selectedReasoningEffort.value = runtime.selectedReasoningEffort
        runtime.selectedAccessMode?.let { raw ->
            runCatching { CodexAccessMode.valueOf(raw) }.getOrNull()?.let { _selectedAccessMode.value = it }
        }
        runtime.selectedServiceTier?.let { raw ->
            runCatching { CodexServiceTier.valueOf(raw) }.getOrNull()?.let { _selectedServiceTier.value = it }
        }
        _threads.value = store.loadCachedThreads(device)
        _activeThreadId.value = store.loadLastActiveThreadId(device)
    } finally {
        isApplyingMacScopedState = false
    }
}

internal suspend fun CodexService.clearInMemoryMacScopedState() {
    isApplyingMacScopedState = true
    try {
        _threads.value = emptyList()
        _activeThreadId.value = null
        persistedThreadRenameById.clear()
        associatedManagedWorktreePathByThreadId.clear()
        synchronized(runningTurnStateLock) {
            _runningTurnIdByThread.value = emptyMap()
            _protectedRunningFallbackThreadIds.value = emptySet()
        }
        turnDraftQueueStore.clear()
        messageTimelineStore.replaceAll(emptyMap())
    } finally {
        isApplyingMacScopedState = false
    }
}

private suspend fun CodexService.interruptRunningTurnsBeforeDeviceSwitchIfNeeded() {
    if (_connectionState.value !is ConnectionState.Connected && !sessionReady) return
    val running = _runningTurnIdByThread.value
    val protected = _protectedRunningFallbackThreadIds.value
    if (running.isEmpty() && protected.isEmpty()) return
    running.forEach { (threadId, turnId) ->
        runCatching { interruptTurnForRepository(threadId, turnId) }
    }
}
