package com.remodex.mobile.services

import com.remodex.mobile.core.model.CodexTrustedMacRecord
import com.remodex.mobile.core.model.CodexTrustedMacRegistry
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TrustedDeviceHelpersTest {
    private val macA =
        CodexTrustedMacRecord(
            macDeviceId = "mac-a",
            macIdentityPublicKey = "key-a",
            lastPairedAt = Instant.parse("2026-05-01T10:00:00Z"),
            relayURL = "ws://192.168.1.10:8787/relay",
            lastResolvedSessionId = "session-a",
        )

    @Test
    fun removingTrustedDevice_dropsOnlyRequestedRecord() {
        val registry =
            CodexTrustedMacRegistry(
                records =
                    mapOf(
                        "mac-a" to macA,
                        "mac-b" to macA.copy(macDeviceId = "mac-b", macIdentityPublicKey = "key-b"),
                    ),
            )

        val next = registry.removingTrustedDevice(" mac-a ")

        assertFalse(next.records.containsKey("mac-a"))
        assertEquals("mac-b", next.records.keys.single())
    }

    @Test
    fun trustedMacFallbackReconnectParams_usesTrustedMacSessionNotCurrentSnapshot() {
        val (url, token) =
            trustedMacFallbackReconnectParams(
                trustedMac = macA,
                sessionId = "session-a",
            ) ?: error("expected reconnect params")

        assertEquals("ws://192.168.1.10:8787/relay/session-a", url)
        assertEquals("session-a", token)
    }

    @Test
    fun trustedMacFallbackRelaySnapshot_targetsTrustedMacForHandshake() {
        val snapshot = trustedMacFallbackRelaySnapshot(macA, "session-a")
            ?: error("expected fallback snapshot")

        assertEquals("session-a", snapshot.relaySessionId)
        assertEquals("mac-a", snapshot.relayMacDeviceId)
        assertEquals("key-a", snapshot.relayMacIdentityPublicKey)
        assertEquals("mac-a", snapshot.lastTrustedMacDeviceId)
        assertEquals("1", snapshot.relayProtocolVersion)
        assertEquals("0", snapshot.relayLastAppliedBridgeOutboundSeq)
    }
}
