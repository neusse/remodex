package com.remodex.mobile.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {
    @Test
    fun startsWithProfileListAndCreatesProfile() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = { "profile-1" })
                val viewModel = TerminalViewModel(repository, TerminalTrustedHostRepository(InMemoryTerminalStore()), RecordingTerminalClient())

                assertTrue(viewModel.state.value.profiles.isEmpty())
                viewModel.startCreateProfile()
                viewModel.updateEditorHost("devbox.local")
                viewModel.updateEditorUsername("andre")
                viewModel.updateEditorPrivateKey(encryptedPrivateKey())
                viewModel.saveEditor()

                assertEquals("profile-1", viewModel.state.value.profiles.single().id)
                assertEquals(TerminalSurface.Profiles, viewModel.state.value.surface)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun connectRequiresPassphraseBeforeClientRuns() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient()
                val viewModel = viewModelWithProfile(client)
                viewModel.openProfile("profile-1")
                viewModel.connect()
                advanceUntilIdle()

                assertEquals(0, client.connectCalls)
                assertEquals("Enter the passphrase before connecting.", viewModel.state.value.selectedSession?.errorMessage)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun explicitlyUnencryptedProfileCanConnectWithoutPassphrase() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient()
                val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = { "profile-1" })
                repository.saveProfile(
                    TerminalProfileDraft(
                        host = "devbox.local",
                        username = "andre",
                        privateKey = unencryptedPrivateKey(),
                        allowUnencryptedKey = true,
                    ),
                ).getOrThrow()
                val viewModel = TerminalViewModel(repository, TerminalTrustedHostRepository(InMemoryTerminalStore()), client)

                viewModel.openProfile("profile-1")
                viewModel.connect()
                advanceUntilIdle()

                assertEquals(1, client.connectCalls)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun openProfileCanEnableUnencryptedModeBeforeConnecting() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient()
                val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = { "profile-1" })
                repository.saveProfile(
                    TerminalProfileDraft(
                        host = "devbox.local",
                        username = "andre",
                        privateKey = encryptedPrivateKey(),
                    ),
                ).getOrThrow()
                val viewModel = TerminalViewModel(repository, TerminalTrustedHostRepository(InMemoryTerminalStore()), client)

                viewModel.openProfile("profile-1")
                viewModel.updateSelectedProfileAllowUnencryptedKey(true)

                assertTrue(viewModel.state.value.selectedProfile?.allowUnencryptedKey == true)
                assertTrue(repository.loadProfiles().single().allowUnencryptedKey)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun wrongPassphraseShowsSpecificError() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient(connectError = IllegalArgumentException("decrypt failed"))
                val viewModel = viewModelWithProfile(client)
                viewModel.openProfile("profile-1")
                viewModel.updatePassphrase("wrong")
                viewModel.connect()
                advanceUntilIdle()

                assertEquals("The passphrase is incorrect.", viewModel.state.value.selectedSession?.errorMessage)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun rejectedPublicKeyAuthExplainsUsernameOrAuthorizedKeys() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client =
                    RecordingTerminalClient(
                        connectError =
                            TerminalPublicKeyAuthenticationException(
                                username = "andre",
                                cause = IllegalStateException("auth failed"),
                            ),
                    )
                val viewModel = viewModelWithProfile(client)
                viewModel.openProfile("profile-1")
                viewModel.updatePassphrase("secret")
                viewModel.connect()
                advanceUntilIdle()

                assertEquals(
                    "Public-key authentication was rejected for andre. Check the username and authorized_keys on the server.",
                    viewModel.state.value.selectedSession?.errorMessage,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun unknownHostRequestsTrustThenReconnects() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient(
                    connectError =
                        UnknownTerminalHostKeyException(
                            host = "devbox.local",
                            port = 22,
                            fingerprint = "SHA256:first",
                            encodedHostKey = "encoded",
                        ),
                )
                val trustedHosts = TerminalTrustedHostRepository(InMemoryTerminalStore())
                val viewModel = viewModelWithProfile(client, trustedHosts)
                viewModel.openProfile("profile-1")
                viewModel.updatePassphrase("secret")
                viewModel.connect()
                advanceUntilIdle()

                assertNotNull(viewModel.state.value.selectedSession?.pendingHostTrust)
                client.connectError = null
                viewModel.trustPendingHostAndConnect()
                advanceUntilIdle()

                assertEquals("SHA256:first", trustedHosts.trustedFingerprint("devbox.local", 22))
                assertEquals(2, client.connectCalls)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun changedHostRequiresExplicitRetrust() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val trustedHosts = TerminalTrustedHostRepository(InMemoryTerminalStore())
                trustedHosts.trust("devbox.local", 22, "SHA256:old")
                val client =
                    RecordingTerminalClient(
                        connectError =
                            ChangedTerminalHostKeyException(
                                host = "devbox.local",
                                port = 22,
                                fingerprint = "SHA256:new",
                                encodedHostKey = "encoded",
                            ),
                    )
                val viewModel = viewModelWithProfile(client, trustedHosts)
                viewModel.openProfile("profile-1")
                viewModel.updatePassphrase("secret")
                viewModel.connect()
                advanceUntilIdle()

                assertTrue(viewModel.state.value.selectedSession?.pendingHostTrust?.replacesExistingTrust == true)
                assertEquals("SHA256:old", trustedHosts.trustedFingerprint("devbox.local", 22))
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun openingTwoProfilesCreatesSwitchableSessions() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = sequenceOf("profile-1", "profile-2").iterator()::next)
                repository.saveProfile(
                    TerminalProfileDraft(
                        host = "one.local",
                        username = "andre",
                        privateKey = encryptedPrivateKey(),
                    ),
                ).getOrThrow()
                repository.saveProfile(
                    TerminalProfileDraft(
                        host = "two.local",
                        username = "andre",
                        privateKey = encryptedPrivateKey(),
                    ),
                ).getOrThrow()
                var nextSession = 0
                val viewModel =
                    TerminalViewModel(
                        profileRepository = repository,
                        trustedHostRepository = TerminalTrustedHostRepository(InMemoryTerminalStore()),
                        clientFactory = { RecordingTerminalClient() },
                        newSessionId = { "session-${++nextSession}" },
                    )

                viewModel.openProfile("profile-1")
                viewModel.openProfile("profile-2")

                assertEquals(listOf("session-1", "session-2"), viewModel.state.value.sessions.map { it.id })
                assertEquals("session-2", viewModel.state.value.selectedSessionId)

                viewModel.selectSession("session-1")
                assertEquals("profile-1", viewModel.state.value.selectedSession?.profileId)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun resizeAndDisconnectStillDelegateToLiveClient() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val client = RecordingTerminalClient()
                val viewModel = viewModelWithProfile(client)
                viewModel.openProfile("profile-1")
                viewModel.updatePassphrase("secret")
                viewModel.connect()
                advanceUntilIdle()

                viewModel.resize(TerminalSize(cols = 120, rows = 40))
                viewModel.disconnect()
                advanceUntilIdle()

                assertEquals(TerminalSize(cols = 120, rows = 40), client.lastResize)
                assertEquals(1, client.disconnectCalls)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun viewModelWithProfile(
        client: RecordingTerminalClient,
        trustedHosts: TerminalTrustedHostRepository = TerminalTrustedHostRepository(InMemoryTerminalStore()),
    ): TerminalViewModel {
        val repository = TerminalProfileRepository(InMemoryTerminalStore(), newId = { "profile-1" })
        repository.saveProfile(
            TerminalProfileDraft(
                host = "devbox.local",
                username = "andre",
                privateKey = encryptedPrivateKey(),
            ),
        ).getOrThrow()
        return TerminalViewModel(repository, trustedHosts, client)
    }
}

private class RecordingTerminalClient(
    var connectError: Throwable? = null,
) : TerminalClient {
    private val _events = MutableSharedFlow<TerminalEvent>()
    override val events: SharedFlow<TerminalEvent> = _events
    var connectCalls: Int = 0
    var disconnectCalls: Int = 0
    var lastResize: TerminalSize? = null

    override suspend fun connect(
        config: TerminalConnectionConfig,
        initialSize: TerminalSize,
    ) {
        connectCalls += 1
        connectError?.let { throw it }
        _events.emit(TerminalEvent.StatusChanged(TerminalStatus.Connected))
    }

    override suspend fun write(bytes: ByteArray) = Unit

    override suspend fun resize(size: TerminalSize) {
        lastResize = size
    }

    override suspend fun disconnect() {
        disconnectCalls += 1
    }
}
