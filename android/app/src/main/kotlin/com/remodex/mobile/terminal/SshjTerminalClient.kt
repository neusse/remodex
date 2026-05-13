package com.remodex.mobile.terminal

import java.io.InputStream
import java.io.OutputStream
import com.hierynomus.sshj.transport.kex.DHGroups
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.connection.channel.direct.PTYMode
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.kex.KeyExchange
import net.schmizz.sshj.userauth.password.PasswordFinder
import net.schmizz.sshj.userauth.password.Resource

class SshjTerminalClient(
    private val knownHostStore: TerminalKnownHostStore,
) : TerminalClient {
    private val readerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events =
        MutableSharedFlow<TerminalEvent>(
            extraBufferCapacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    override val events: SharedFlow<TerminalEvent> = _events

    private var sshClient: SSHClient? = null
    private var session: Session? = null
    private var shell: Session.Shell? = null
    private var shellOutput: OutputStream? = null
    private var readerJob: Job? = null

    override suspend fun connect(
        config: TerminalConnectionConfig,
        initialSize: TerminalSize,
    ) {
        disconnect()
        val size = initialSize.normalized
        emit(TerminalEvent.StatusChanged(TerminalStatus.Connecting))
        try {
            withContext(Dispatchers.IO) {
                val client = SSHClient(androidCompatibleSshConfig())
                client.addHostKeyVerifier(TerminalKnownHostVerifier(knownHostStore))
                client.connect(config.host.trim(), config.port)
                val keyProvider =
                    client.loadKeys(
                        config.privateKey.normalizedPem(),
                        null,
                        config.passphrase.toPasswordFinderOrNull(),
                    )
                client.authPublickey(config.username.trim(), keyProvider)

                val sshSession = client.startSession()
                sshSession.allocatePTY("xterm-256color", size.cols, size.rows, 0, 0, emptyMap<PTYMode, Int>())
                val openedShell = sshSession.startShell()
                sshClient = client
                session = sshSession
                shell = openedShell
                shellOutput = openedShell.outputStream
                emit(TerminalEvent.StatusChanged(TerminalStatus.Connected))
                startReaders(openedShell.inputStream, openedShell.errorStream)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val unknownHostKey = e.findCause<UnknownTerminalHostKeyException>()
            if (unknownHostKey != null) {
                disconnect()
                throw unknownHostKey
            }
            disconnect()
            emit(TerminalEvent.StatusChanged(TerminalStatus.Error))
            emit(TerminalEvent.Error(e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName, e))
            throw e
        }
    }

    override suspend fun write(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        withContext(Dispatchers.IO) {
            shellOutput?.write(bytes)
            shellOutput?.flush()
        }
    }

    override suspend fun resize(size: TerminalSize) {
        val normalized = size.normalized
        withContext(Dispatchers.IO) {
            shell?.changeWindowDimensions(normalized.cols, normalized.rows, 0, 0)
        }
    }

    override suspend fun disconnect() {
        val currentReader = readerJob
        val currentShell = shell
        val currentSession = session
        val currentClient = sshClient
        readerJob = null
        shellOutput = null
        shell = null
        session = null
        sshClient = null
        currentReader?.cancel()
        withContext(Dispatchers.IO) {
            runCatching { currentShell?.close() }
            runCatching { currentSession?.close() }
            runCatching { currentClient?.disconnect() }
            runCatching { currentClient?.close() }
        }
        emit(TerminalEvent.StatusChanged(TerminalStatus.Disconnected))
    }

    private fun startReaders(
        stdout: InputStream,
        stderr: InputStream,
    ) {
        val parent = Job()
        readerScope.launch(parent) { readLoop(stdout) }
        readerScope.launch(parent) { readLoop(stderr) }
        readerJob = parent
    }

    private suspend fun readLoop(input: InputStream) {
        val buffer = ByteArray(4096)
        try {
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) emit(TerminalEvent.Output(buffer.copyOf(read)))
            }
        } catch (_: Exception) {
            // Closing the SSH channel interrupts reads; the owning disconnect path emits the status.
        }
    }

    private fun emit(event: TerminalEvent) {
        _events.tryEmit(event)
    }

    private fun androidCompatibleSshConfig(): DefaultConfig {
        // Android's built-in "BC" provider can be incomplete; let JCA choose platform providers.
        SecurityUtils.setSecurityProvider(null)
        SecurityUtils.setRegisterBouncyCastle(false)
        return DefaultConfig().apply {
            // Android's bundled BC provider can miss X25519/EC, so only advertise DH SHA-2 KEX.
            keyExchangeFactories =
                listOf<Factory.Named<KeyExchange>>(
                    DHGroups.Group14SHA256(),
                    DHGroups.Group16SHA512(),
                    DHGroups.Group18SHA512(),
                )
        }
    }

    private fun String.normalizedPem(): String =
        replace("\r\n", "\n").trim()

    private fun String.toPasswordFinderOrNull(): PasswordFinder? {
        if (isEmpty()) return null
        val password = toCharArray()
        return object : PasswordFinder {
            override fun reqPassword(resource: Resource<*>?): CharArray = password
            override fun shouldRetry(resource: Resource<*>?): Boolean = false
        }
    }

    private inline fun <reified T : Throwable> Throwable.findCause(): T? {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}
