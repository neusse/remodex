package com.remodex.mobile.terminal

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Base64

class MissingTerminalPassphraseException : IllegalArgumentException("Passphrase is required for every terminal connection.")

class UnencryptedTerminalPrivateKeyException : IllegalArgumentException("Only encrypted SSH private keys are supported.")

object TerminalPrivateKeyInspector {
    fun isEncrypted(privateKey: String): Boolean {
        val normalized = privateKey.replace("\r\n", "\n").trim()
        if (normalized.contains("BEGIN ENCRYPTED PRIVATE KEY")) return true
        if (normalized.contains("Proc-Type: 4,ENCRYPTED")) return true
        if (!normalized.contains("BEGIN OPENSSH PRIVATE KEY")) return false
        return opensshCipherName(normalized)?.let { it != "none" } == true
    }

    private fun opensshCipherName(privateKey: String): String? {
        val payload =
            privateKey.lineSequence()
                .filterNot { it.startsWith("-----") }
                .joinToString(separator = "")
        val bytes = runCatching { Base64.getDecoder().decode(payload) }.getOrNull() ?: return null
        val prefix = "openssh-key-v1\u0000".toByteArray(StandardCharsets.US_ASCII)
        if (!bytes.copyOfRange(0, minOf(bytes.size, prefix.size)).contentEquals(prefix)) return null
        val buffer = ByteBuffer.wrap(bytes, prefix.size, bytes.size - prefix.size)
        val length = buffer.int.takeIf { it in 1..buffer.remaining() } ?: return null
        val cipherBytes = ByteArray(length)
        buffer.get(cipherBytes)
        return String(cipherBytes, StandardCharsets.US_ASCII)
    }
}
