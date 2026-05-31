package com.remodex.mobile.services

import android.util.Log
import com.remodex.mobile.core.error.CodexServiceError
import com.remodex.mobile.core.model.CodexPairingQRPayload
import com.remodex.mobile.core.model.CodexTrustedMacRecord
import com.remodex.mobile.core.model.CodexTrustedMacRegistry
import com.remodex.mobile.core.model.CodexTrustedSessionResolveResponse
import com.remodex.mobile.core.model.CODEX_SECURE_PROTOCOL_VERSION
import com.remodex.mobile.core.model.JSONValue
import com.remodex.mobile.core.model.PetCompanion
import com.remodex.mobile.core.model.petCompanionFromJson
import com.remodex.mobile.core.model.petListFromRpcResult
import com.remodex.mobile.core.persistence.RelaySessionSnapshot
import com.remodex.mobile.core.persistence.RuntimeSelectionSnapshot
import com.remodex.mobile.core.security.CodexSecureKeys
import com.remodex.mobile.pairing.buildWebSocketConnectParams
import java.time.Instant

private const val PET_COMPANION_LOG_TAG = "RemodexPet"

internal fun CodexService.trustedSessionResolveClient(): CodexTrustedSessionResolveClient {
    val existing = trustedSessionResolveClientLazy
    if (existing != null) return existing
    return CodexTrustedSessionResolveClient(httpCallClient, secureStore, json).also {
        trustedSessionResolveClientLazy = it
    }
}

fun CodexService.initializeTrustedDeviceState() {
    refreshTrustedDevices()
    val snapshot = sessionPersistence.loadRelaySnapshot()
    _currentTrustedMacDeviceId.value =
        snapshot.lastTrustedMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }
            ?: snapshot.relayMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }
    _relayMacDeviceId.value = snapshot.relayMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }
}

fun CodexService.refreshTrustedDevices() {
    _trustedDevices.value = presentationTrustedMacRecords()
}

fun CodexService.presentationTrustedMacRecords(registry: CodexTrustedMacRegistry = loadTrustedRegistry()): List<CodexTrustedMacRecord> {
    return registry.records.values.toList()
}

internal fun CodexService.loadTrustedRegistry(): CodexTrustedMacRegistry =
    secureStore.readCodable(CodexSecureKeys.trustedMacRegistry) ?: CodexTrustedMacRegistry.empty

internal fun CodexTrustedMacRegistry.removingTrustedDevice(deviceId: String?): CodexTrustedMacRegistry {
    val normalized = deviceId?.trim()?.takeIf { it.isNotEmpty() } ?: return this
    return CodexTrustedMacRegistry(records - normalized)
}

internal fun CodexService.forgetTrustedDeviceImpl(deviceId: String) {
    val normalized = normalizedMacDeviceId(deviceId) ?: return
    val nextRegistry = loadTrustedRegistry().removingTrustedDevice(normalized)
    secureStore.writeCodable(CodexSecureKeys.trustedMacRegistry, nextRegistry)
    com.remodex.mobile.ui.mydevices.MyDeviceMenuVisibilityStore.removePreference(normalized)
    com.remodex.mobile.ui.mydevices.SidebarComputerNicknameStore.setNickname("", normalized)
    macScopedSessionStore.clearDevice(normalized)
    if (_previousTrustedMacDeviceId.value == normalized) clearPreviousTrustedMacDeviceId()
    refreshTrustedDevices()
}

internal fun CodexService.trustedMacRecord(deviceId: String?): CodexTrustedMacRecord? {
    val normalized = normalizedMacDeviceId(deviceId) ?: return null
    return loadTrustedRegistry().records[normalized]
}

internal fun CodexService.normalizedCurrentTrustedMacDeviceId(): String? =
    normalizedMacDeviceId(macScopedContextOverrideDeviceId)
        ?: _currentTrustedMacDeviceId.value?.trim()?.takeIf { it.isNotEmpty() }
        ?: sessionPersistence.loadRelaySnapshot().lastTrustedMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }

internal fun CodexService.normalizedPreviousTrustedMacDeviceId(): String? =
    _previousTrustedMacDeviceId.value?.trim()?.takeIf { it.isNotEmpty() }

internal fun CodexService.normalizedRelayMacDeviceId(): String? =
    _relayMacDeviceId.value?.trim()?.takeIf { it.isNotEmpty() }
        ?: sessionPersistence.loadRelaySnapshot().relayMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }

internal fun CodexService.setCurrentTrustedMacDeviceId(deviceId: String?) {
    val normalized = normalizedMacDeviceId(deviceId)
    _currentTrustedMacDeviceId.value = normalized
    val snapshot = sessionPersistence.loadRelaySnapshot()
    sessionPersistence.saveRelaySnapshot(snapshot.copy(lastTrustedMacDeviceId = normalized))
}

internal fun CodexService.setPreviousTrustedMacDeviceId(deviceId: String?) {
    _previousTrustedMacDeviceId.value = normalizedMacDeviceId(deviceId)
}

internal fun CodexService.clearPreviousTrustedMacDeviceId() {
    _previousTrustedMacDeviceId.value = null
}

internal fun CodexService.normalizedMacDeviceId(deviceId: String?): String? =
    deviceId?.trim()?.takeIf { it.isNotEmpty() }

internal fun CodexService.resolvedMacScopedPersistenceDeviceId(): String? =
    normalizedMacDeviceId(macScopedContextOverrideDeviceId) ?: normalizedCurrentTrustedMacDeviceId()

internal fun CodexService.applyResolvedTrustedSession(
    resolved: CodexTrustedSessionResolveResponse,
    relayURL: String,
) {
    val registry = loadTrustedRegistry()
    val previous = registry.records[resolved.macDeviceId]
    val updated =
        CodexTrustedMacRecord(
            macDeviceId = resolved.macDeviceId,
            macIdentityPublicKey = resolved.macIdentityPublicKey,
            lastPairedAt = previous?.lastPairedAt ?: Instant.now(),
            relayURL = relayURL,
            displayName = resolved.displayName ?: previous?.displayName,
            lastResolvedSessionId = resolved.sessionId,
            lastResolvedAt = Instant.now(),
            lastUsedAt = Instant.now(),
        )
    secureStore.writeCodable(
        CodexSecureKeys.trustedMacRegistry,
        CodexTrustedMacRegistry(registry.records + (resolved.macDeviceId to updated)),
    )
    sessionPersistence.saveRelaySnapshot(
        sessionPersistence.loadRelaySnapshot().copy(
            relaySessionId = resolved.sessionId,
            relayUrl = relayURL,
            relayMacDeviceId = resolved.macDeviceId,
            relayMacIdentityPublicKey = resolved.macIdentityPublicKey,
            lastTrustedMacDeviceId = resolved.macDeviceId,
        ),
    )
    _relayMacDeviceId.value = resolved.macDeviceId
    refreshTrustedDevices()
}

internal fun CodexService.clearSavedRelaySession() {
    sessionPersistence.clearRelaySession()
    _relayMacDeviceId.value = null
}

internal suspend fun CodexService.resolveTrustedMacSession(deviceId: String? = null): CodexTrustedSessionResolveResponse {
    val targetId = normalizedMacDeviceId(deviceId) ?: normalizedCurrentTrustedMacDeviceId()
    val trustedMac =
        trustedMacRecord(targetId) ?: trustedMacRecord(normalizedCurrentTrustedMacDeviceId())
            ?: throw com.remodex.mobile.core.model.CodexTrustedSessionResolveError.NoTrustedMac
    val resolved = trustedSessionResolveClient().resolveTrustedMacSession(trustedMac)
    applyResolvedTrustedSession(resolved, trustedMac.relayURL.orEmpty())
    return resolved
}

internal fun CodexService.cancelTrustedSessionResolve() {
    trustedSessionResolveClient().cancel()
}

internal suspend fun CodexService.preferredReconnectParams(targetMacDeviceId: String?): Pair<String, String>? {
    val normalizedTarget = normalizedMacDeviceId(targetMacDeviceId) ?: return null
    val trustedMac = trustedMacRecord(normalizedTarget) ?: return null
    return runCatching {
        resolveTrustedMacSession(normalizedTarget)
        buildWebSocketConnectParams(
            sessionPersistence.loadRelaySnapshot(),
            sessionPersistence.loadLocalRelayHostOverride().orEmpty(),
        )
    }.getOrNull()
        ?: trustedMac.lastResolvedSessionId?.let { sessionId ->
            val fallbackSnapshot = trustedMacFallbackRelaySnapshot(trustedMac, sessionId) ?: return@let null
            sessionPersistence.saveRelaySnapshot(fallbackSnapshot)
            _relayMacDeviceId.value = trustedMac.macDeviceId
            trustedMacFallbackReconnectParams(
                trustedMac = trustedMac,
                sessionId = sessionId,
                relayHostOverride = sessionPersistence.loadLocalRelayHostOverride().orEmpty(),
            )
        }
        ?: run {
            val snap = sessionPersistence.loadRelaySnapshot()
            if (snap.relayMacDeviceId == normalizedTarget && !snap.relayUrl.isNullOrBlank() && !snap.relaySessionId.isNullOrBlank()) {
                runCatching {
                    buildWebSocketConnectParams(
                        snap,
                        sessionPersistence.loadLocalRelayHostOverride().orEmpty(),
                    )
                }.getOrNull()
            } else {
                null
            }
        }
}

internal suspend fun CodexService.preferredReconnectUrl(targetMacDeviceId: String?): String? =
    preferredReconnectParams(targetMacDeviceId)?.first

internal fun trustedMacFallbackReconnectParams(
    trustedMac: CodexTrustedMacRecord,
    sessionId: String,
    relayHostOverride: String = "",
): Pair<String, String>? {
    return buildWebSocketConnectParams(
        trustedMacFallbackRelaySnapshot(trustedMac, sessionId) ?: return null,
        relayHostOverride,
    )
}

internal fun trustedMacFallbackRelaySnapshot(
    trustedMac: CodexTrustedMacRecord,
    sessionId: String,
): RelaySessionSnapshot? {
    val relay = trustedMac.relayURL?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val sid = sessionId.trim().takeIf { it.isNotEmpty() } ?: return null
    return RelaySessionSnapshot(
        relaySessionId = sid,
        relayUrl = relay,
        relayMacDeviceId = trustedMac.macDeviceId,
        relayMacIdentityPublicKey = trustedMac.macIdentityPublicKey,
        relayProtocolVersion = CODEX_SECURE_PROTOCOL_VERSION.toString(),
        relayLastAppliedBridgeOutboundSeq = "0",
        lastTrustedMacDeviceId = trustedMac.macDeviceId,
    )
}

internal fun CodexService.persistActiveThreadId(threadId: String?) {
    val id = threadId?.trim()?.takeIf { it.isNotEmpty() }
    val device = resolvedMacScopedPersistenceDeviceId()
    if (device != null) {
        macScopedSessionStore.saveLastActiveThreadId(device, id)
    } else {
        sessionPersistence.saveLastActiveThreadId(id)
    }
}

internal fun CodexService.persistRuntimeSelectionSnapshot(snapshot: RuntimeSelectionSnapshot) {
    val device = resolvedMacScopedPersistenceDeviceId()
    if (device != null) {
        macScopedSessionStore.saveRuntimeSelection(device, snapshot)
    } else {
        sessionPersistence.saveRuntimeSelection(snapshot)
    }
}

internal fun CodexService.loadScopedLocallyDeletedThreadIds(): Set<String> {
    val device = resolvedMacScopedPersistenceDeviceId()
    return if (device != null) {
        macScopedSessionStore.loadLocallyDeletedThreadIds(device)
    } else {
        sessionPersistence.loadLocallyDeletedThreadIds()
    }
}

internal fun CodexService.loadScopedLocallyArchivedThreadIds(): Set<String> {
    val device = resolvedMacScopedPersistenceDeviceId()
    return if (device != null) {
        macScopedSessionStore.loadLocallyArchivedThreadIds(device)
    } else {
        sessionPersistence.loadLocallyArchivedThreadIds()
    }
}

internal fun CodexService.addScopedLocallyDeletedThreadId(threadId: String) {
    val device = resolvedMacScopedPersistenceDeviceId()
    if (device != null) {
        val next = macScopedSessionStore.loadLocallyDeletedThreadIds(device) + threadId
        macScopedSessionStore.saveLocallyDeletedThreadIds(device, next)
    } else {
        sessionPersistence.addLocallyDeletedThreadId(threadId)
    }
}

internal fun CodexService.setScopedLocallyArchivedThreadId(
    threadId: String,
    isArchived: Boolean,
) {
    val device = resolvedMacScopedPersistenceDeviceId()
    if (device != null) {
        val current = macScopedSessionStore.loadLocallyArchivedThreadIds(device)
        val next = if (isArchived) current + threadId else current - threadId
        macScopedSessionStore.saveLocallyArchivedThreadIds(device, next)
    } else if (isArchived) {
        sessionPersistence.addLocallyArchivedThreadId(threadId)
    } else {
        sessionPersistence.removeLocallyArchivedThreadId(threadId)
    }
}

internal fun CodexService.captureRelaySessionSnapshot(): RelaySessionSnapshot =
    sessionPersistence.loadRelaySnapshot()

internal fun CodexService.restoreRelaySessionSnapshot(snapshot: RelaySessionSnapshot) {
    sessionPersistence.saveRelaySnapshot(snapshot)
    _relayMacDeviceId.value = snapshot.relayMacDeviceId?.trim()?.takeIf { it.isNotEmpty() }
}

internal suspend fun CodexService.listPets(includeData: Boolean = false): List<PetCompanion> {
    Log.i(PET_COMPANION_LOG_TAG, "pet/list request includeData=$includeData sessionReady=$sessionReady")
    val response =
        sendRequestImpl(
            method = "pet/list",
            params =
                JSONValue.Obj(
                    mapOf(
                        "includeData" to JSONValue.Bool(includeData),
                        "metadataOnly" to JSONValue.Bool(!includeData),
                    ),
                ),
        )
    response.error?.let { error ->
        Log.w(PET_COMPANION_LOG_TAG, "pet/list RPC error code=${error.code} message=${error.message}")
        throw CodexServiceError.RpcFailure(error)
    }
    val pets = petListFromRpcResult(response.result)
    Log.i(PET_COMPANION_LOG_TAG, "pet/list loaded count=${pets.size} includeData=$includeData")
    return pets
}

internal suspend fun CodexService.readPet(id: String): PetCompanion {
    Log.i(PET_COMPANION_LOG_TAG, "pet/read request id=$id sessionReady=$sessionReady")
    return try {
        readPetDirect(id)
    } catch (error: Exception) {
        if (!isUnsupportedPetReadError(error)) {
            Log.w(PET_COMPANION_LOG_TAG, "pet/read failed id=$id: ${error.message}")
            throw error
        }
        Log.i(PET_COMPANION_LOG_TAG, "pet/read unsupported for id=$id; falling back to pet/list includeData=true")
        listPets(includeData = true).firstOrNull { it.id == id } ?: run {
            Log.w(PET_COMPANION_LOG_TAG, "pet/read fallback did not return id=$id")
            throw error
        }
    }
}

private suspend fun CodexService.readPetDirect(id: String): PetCompanion {
    val response =
        sendRequestImpl(
            method = "pet/read",
            params = JSONValue.Obj(mapOf("id" to JSONValue.Str(id))),
        )
    response.error?.let { error ->
        Log.w(PET_COMPANION_LOG_TAG, "pet/read RPC error id=$id code=${error.code} message=${error.message}")
        throw CodexServiceError.RpcFailure(error)
    }
    return response.result?.let { petCompanionFromJson(it, requiresSpritesheetData = true) }
        ?: run {
            Log.w(PET_COMPANION_LOG_TAG, "pet/read returned invalid pet id=$id")
            throw CodexServiceError.InvalidResponse("The selected pet did not include a valid spritesheet.")
        }
}

private fun isUnsupportedPetReadError(error: Exception): Boolean {
    val message = error.message?.lowercase().orEmpty()
    return message.contains("unknown variant") ||
        message.contains("unknown method") ||
        message.contains("pet/read")
}

internal fun CodexService.rememberRelayPairing(payload: CodexPairingQRPayload) {
    sessionPersistence.applyPairingPayload(payload, secureStore)
    payload.displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { displayName ->
        val registry = loadTrustedRegistry()
        val previous = registry.records[payload.macDeviceId]
        val record =
            CodexTrustedMacRecord(
                macDeviceId = payload.macDeviceId,
                macIdentityPublicKey = payload.macIdentityPublicKey,
                lastPairedAt = previous?.lastPairedAt ?: Instant.now(),
                relayURL = payload.relay,
                displayName = displayName,
                lastResolvedSessionId = previous?.lastResolvedSessionId,
                lastResolvedAt = previous?.lastResolvedAt,
                lastUsedAt = previous?.lastUsedAt,
            )
        secureStore.writeCodable(
            CodexSecureKeys.trustedMacRegistry,
            CodexTrustedMacRegistry(registry.records + (payload.macDeviceId to record)),
        )
    }
    _relayMacDeviceId.value = payload.macDeviceId
    setCurrentTrustedMacDeviceId(payload.macDeviceId)
    refreshTrustedDevices()
}
