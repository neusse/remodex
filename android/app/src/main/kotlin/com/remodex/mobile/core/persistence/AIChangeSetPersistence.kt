package com.remodex.mobile.core.persistence

import android.content.Context
import com.remodex.mobile.core.model.AIChangeSet
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Plain JSON ledger for assistant change sets (same filename/layout as iOS).
 * Mirrors [AIChangeSetPersistence.swift](CodexMobile/CodexMobile/Services/AIChangeSetPersistence.swift).
 */
class AIChangeSetPersistence(
    private val context: Context,
) {
    private val fileName = "codex-ai-change-sets-v1.json"
    private val macScopedDirName = "codex-ai-change-sets-mac-v1"

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    private val listSerializer = ListSerializer(AIChangeSet.serializer())

    fun load(macDeviceId: String? = null): List<AIChangeSet> {
        val file = storeFile(macDeviceId)
        if (!file.exists() || file.length() == 0L) return emptyList()
        val text = runCatching { file.readText() }.getOrNull() ?: return emptyList()
        return runCatching { json.decodeFromString(listSerializer, text) }.getOrDefault(emptyList())
    }

    fun save(
        value: List<AIChangeSet>,
        macDeviceId: String? = null,
    ) {
        val data =
            runCatching { json.encodeToString(listSerializer, value) }
                .getOrNull()
                ?: return
        val file = storeFile(macDeviceId)
        file.parentFile?.mkdirs()
        file.writeText(data)
    }

    private fun storeFile(macDeviceId: String? = null): File {
        val dir = File(context.filesDir, context.packageName).apply { mkdirs() }
        val device = macDeviceId?.trim()?.takeIf { it.isNotEmpty() } ?: return File(dir, fileName)
        val scopedDir = dir.resolve(macScopedDirName).resolve(hash(device)).apply { mkdirs() }
        return File(scopedDir, fileName)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
