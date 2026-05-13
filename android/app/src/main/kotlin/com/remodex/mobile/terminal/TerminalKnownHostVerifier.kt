package com.remodex.mobile.terminal

import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import net.schmizz.sshj.transport.verification.HostKeyVerifier

class UnknownTerminalHostKeyException(
    val host: String,
    val port: Int,
    val fingerprint: String,
    val encodedHostKey: String,
) : RuntimeException("Trust the SSH host key before connecting to $host:$port.")

class ChangedTerminalHostKeyException(
    host: String,
    port: Int,
) : RuntimeException("The SSH host key changed for $host:$port. Verify the host before reconnecting.")

class TerminalKnownHostStore {
    private val trustedFingerprints = ConcurrentHashMap<String, String>()

    fun trust(
        host: String,
        port: Int,
        fingerprint: String,
    ) {
        trustedFingerprints[key(host, port)] = fingerprint
    }

    fun trustedFingerprint(
        host: String,
        port: Int,
    ): String? = trustedFingerprints[key(host, port)]

    private fun key(
        host: String,
        port: Int,
    ): String = "${host.trim().lowercase()}:$port"
}

class TerminalKnownHostVerifier(
    private val store: TerminalKnownHostStore,
) : HostKeyVerifier {
    override fun findExistingAlgorithms(
        hostname: String,
        port: Int,
    ): MutableList<String> = mutableListOf()

    override fun verify(
        hostname: String,
        port: Int,
        key: PublicKey,
    ): Boolean {
        val fingerprint = TerminalHostKeyFingerprint.sha256(key)
        val trusted = store.trustedFingerprint(hostname, port)
        if (trusted == null) {
            throw UnknownTerminalHostKeyException(
                host = hostname,
                port = port,
                fingerprint = fingerprint,
                encodedHostKey = Base64.getEncoder().encodeToString(key.encoded),
            )
        }
        if (trusted != fingerprint) {
            throw ChangedTerminalHostKeyException(hostname, port)
        }
        return true
    }
}

object TerminalHostKeyFingerprint {
    fun sha256(key: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.encoded)
        val encoded = Base64.getEncoder().withoutPadding().encodeToString(digest)
        return "SHA256:$encoded"
    }
}
