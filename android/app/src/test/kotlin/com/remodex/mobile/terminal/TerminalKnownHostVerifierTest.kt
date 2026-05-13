package com.remodex.mobile.terminal

import java.security.KeyPairGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TerminalKnownHostVerifierTest {
    @Test
    fun firstSeenHostRequestsTrust() {
        val key = testPublicKey()
        val verifier = TerminalKnownHostVerifier(TerminalKnownHostStore())

        val error =
            assertFailsWith<UnknownTerminalHostKeyException> {
                verifier.verify("devbox.local", 22, key)
            }

        assertEquals("devbox.local", error.host)
        assertEquals(22, error.port)
        assertEquals(TerminalHostKeyFingerprint.sha256(key), error.fingerprint)
    }

    @Test
    fun trustedHostKeyIsAccepted() {
        val key = testPublicKey()
        val store = TerminalKnownHostStore()
        store.trust("devbox.local", 22, TerminalHostKeyFingerprint.sha256(key))

        assertTrue(TerminalKnownHostVerifier(store).verify("devbox.local", 22, key))
    }

    @Test
    fun changedHostKeyIsBlocked() {
        val firstKey = testPublicKey()
        val changedKey = testPublicKey()
        val store = TerminalKnownHostStore()
        store.trust("devbox.local", 22, TerminalHostKeyFingerprint.sha256(firstKey))

        assertFailsWith<ChangedTerminalHostKeyException> {
            TerminalKnownHostVerifier(store).verify("devbox.local", 22, changedKey)
        }
    }

    private fun testPublicKey() =
        KeyPairGenerator.getInstance("RSA")
            .apply { initialize(1024) }
            .generateKeyPair()
            .public
}
