package com.remodex.mobile.ui.mydevices

import android.content.Context
import com.remodex.mobile.AppContainer

object MyDeviceMenuVisibilityStore {
    private const val KEY_PREFIX = "codex.myDevices.visibleInMenu."

    private val memoryValues = mutableMapOf<String, Boolean>()
    private val memoryStrings = mutableMapOf<String, String>()

    private fun prefs(): android.content.SharedPreferences? =
        runCatching {
            AppContainer.appContext.getSharedPreferences("remodex_my_devices", Context.MODE_PRIVATE)
        }.getOrNull()

    fun isVisible(deviceId: String?): Boolean {
        val key = storageKey(deviceId) ?: return true
        prefs()?.let { store ->
            if (!store.contains(key)) return true
            return store.getBoolean(key, true)
        }
        return memoryValues[key] ?: true
    }

    fun setVisible(
        isVisible: Boolean,
        deviceId: String?,
    ) {
        val key = storageKey(deviceId) ?: return
        prefs()?.edit()?.putBoolean(key, isVisible)?.apply()
            ?: run { memoryValues[key] = isVisible }
    }

    fun removePreference(deviceId: String?) {
        val key = storageKey(deviceId) ?: return
        prefs()?.edit()?.remove(key)?.apply()
        memoryValues.remove(key)
    }

    internal fun readString(key: String): String? =
        prefs()?.getString(key, null) ?: memoryStrings[key]

    internal fun writeString(
        key: String,
        value: String,
    ) {
        prefs()?.edit()?.putString(key, value)?.apply()
            ?: run { memoryStrings[key] = value }
    }

    internal fun removeString(key: String) {
        prefs()?.edit()?.remove(key)?.apply()
        memoryStrings.remove(key)
    }

    private fun storageKey(deviceId: String?): String? {
        val normalized = deviceId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return KEY_PREFIX + normalized
    }
}
