package com.remodex.mobile.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TerminalPersistenceTest {
    @Test
    fun profilesPersistAndUseFallbackLabel() {
        val store = InMemoryTerminalStore()
        val repository = TerminalProfileRepository(store, newId = { "profile-1" })

        val profile =
            repository.saveProfile(
                TerminalProfileDraft(
                    host = "devbox.local",
                    port = "2222",
                    username = "andre",
                    privateKey = encryptedPrivateKey(),
                ),
            ).getOrThrow()

        assertEquals("profile-1", profile.id)
        assertEquals("andre@devbox.local:2222", profile.displayLabel)
        assertEquals(listOf(profile), TerminalProfileRepository(store).loadProfiles())
    }

    @Test
    fun createEditDeleteProfiles() {
        val store = InMemoryTerminalStore()
        val repository = TerminalProfileRepository(store, newId = { "profile-1" })
        val created =
            repository.saveProfile(
                TerminalProfileDraft(
                    label = "Main",
                    host = "devbox.local",
                    username = "andre",
                    privateKey = encryptedPrivateKey(),
                ),
            ).getOrThrow()

        val edited =
            repository.saveProfile(
                TerminalProfileDraft(
                    id = created.id,
                    label = "Renamed",
                    host = "devbox.local",
                    username = "andre",
                    privateKey = encryptedPrivateKey(),
                ),
            ).getOrThrow()

        assertEquals("Renamed", edited.displayLabel)
        assertEquals(1, repository.loadProfiles().size)

        repository.deleteProfile(created.id)
        assertTrue(repository.loadProfiles().isEmpty())
    }

    @Test
    fun unencryptedKeysAreRejected() {
        val repository = TerminalProfileRepository(InMemoryTerminalStore())

        assertFailsWith<UnencryptedTerminalPrivateKeyException> {
            repository.saveProfile(
                TerminalProfileDraft(
                    host = "devbox.local",
                    username = "andre",
                    privateKey = unencryptedPrivateKey(),
                ),
            ).getOrThrow()
        }
    }

    @Test
    fun unencryptedKeysCanBeSavedWithExplicitOptIn() {
        val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = { "profile-1" })

        val profile =
            repository.saveProfile(
                TerminalProfileDraft(
                    host = "devbox.local",
                    username = "andre",
                    privateKey = unencryptedPrivateKey(),
                    allowUnencryptedKey = true,
                ),
            ).getOrThrow()

        assertTrue(profile.allowUnencryptedKey)
    }

    @Test
    fun trustedHostsPersistAndRetrustReplacesFingerprint() {
        val store = InMemoryTerminalStore()
        val repository = TerminalTrustedHostRepository(store)
        repository.trust("DevBox.local", 22, "SHA256:first")
        assertEquals("SHA256:first", TerminalTrustedHostRepository(store).trustedFingerprint("devbox.local", 22))

        repository.trust("devbox.local", 22, "SHA256:second")
        assertEquals("SHA256:second", repository.trustedFingerprint("DEVBOX.local", 22))
    }

    @Test
    fun connectionConfigRequiresRuntimePassphrase() {
        val profile =
            TerminalConnectionConfig(
                profileId = "profile-1",
                host = "devbox.local",
                port = 22,
                username = "andre",
                privateKey = encryptedPrivateKey(),
                passphrase = "",
                allowUnencryptedKey = false,
            )

        assertTrue(!profile.isComplete)
        assertNull(profile.passphrase.takeIf(String::isNotBlank))
    }

    @Test
    fun connectionConfigAllowsEmptyPassphraseForExplicitlyUnencryptedProfiles() {
        val profile =
            TerminalConnectionConfig(
                profileId = "profile-1",
                host = "devbox.local",
                port = 22,
                username = "andre",
                privateKey = unencryptedPrivateKey(),
                passphrase = "",
                allowUnencryptedKey = true,
            )

        assertTrue(profile.isComplete)
    }
}

internal fun encryptedPrivateKey(): String =
    """
    -----BEGIN ENCRYPTED PRIVATE KEY-----
    encrypted
    -----END ENCRYPTED PRIVATE KEY-----
    """.trimIndent()

internal fun unencryptedPrivateKey(): String =
    """
    -----BEGIN PRIVATE KEY-----
    plain
    -----END PRIVATE KEY-----
    """.trimIndent()
