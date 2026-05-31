package com.remodex.mobile.ui.navigation

import android.net.Uri
import com.remodex.mobile.ui.draft.NewChatDraftSource

/** Top-level destinations for [AppNavHost] (parity with ContentView navigation stack). */
object AppRoutes {
    const val Home = "home"
    const val Settings = "settings"
    const val MyDevices = "my_devices"
    const val Archived = "archived"
    const val About = "about"
    const val WhatsNew = "whats_new"
    const val TesterHq = "tester_hq"
    const val Terminal = "terminal"
    const val TerminalCdHintQuery = "cdHint"

    const val NewChatDraftRouteIdArg = "routeId"
    const val NewChatDraftSourceArg = "source"
    const val NewChatDraftProjectPathArg = "preferredProjectPath"

    const val NewChatDraft = "new_chat_draft/{routeId}/{source}?${NewChatDraftProjectPathArg}={${NewChatDraftProjectPathArg}}"

    fun newChatDraftRoute(
        routeId: String,
        source: NewChatDraftSource,
        preferredProjectPath: String? = null,
    ): String {
        val path = preferredProjectPath?.trim()?.takeIf { it.isNotEmpty() }
        val base = "new_chat_draft/$routeId/${source.name}"
        return if (path == null) {
            base
        } else {
            "$base?$NewChatDraftProjectPathArg=${Uri.encode(path)}"
        }
    }

    fun terminalRoute(cdHint: String? = null): String {
        val path = cdHint?.trim()?.takeIf { it.isNotEmpty() } ?: return Terminal
        return "$Terminal?$TerminalCdHintQuery=${Uri.encode(path)}"
    }
}
