package com.remodex.mobile.terminal

class InMemoryTerminalStore : TerminalKeyValueStore {
    private val values = mutableMapOf<String, String>()

    override fun readString(key: String): String? = values[key]

    override fun writeString(
        key: String,
        value: String,
    ) {
        values[key] = value
    }

    override fun deleteValue(key: String) {
        values.remove(key)
    }
}
