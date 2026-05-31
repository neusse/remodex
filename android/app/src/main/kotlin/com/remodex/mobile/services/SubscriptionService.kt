package com.remodex.mobile.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.remodex.mobile.BuildConfig

/**
 * Local-first subscription abstraction. Disabled unless configured at build time.
 */
class SubscriptionService(
    context: Context,
) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            "remodex_subscription",
            Context.MODE_PRIVATE,
        )

    val isConfigured: Boolean = BuildConfig.SUBSCRIPTION_ENABLED

    val hasProAccess: Boolean
        get() = if (!isConfigured) true else prefs.getBoolean(KEY_HAS_PRO, false)

    val freeSendCount: Int
        get() = prefs.getInt(KEY_FREE_SEND_COUNT, 0)

    val remainingFreeSendAttempts: Int
        get() = maxOf(0, FREE_SEND_LIMIT - freeSendCount)

    val hasFreeSendAccess: Boolean
        get() = !isConfigured || hasProAccess || freeSendCount < FREE_SEND_LIMIT

    fun recordFreeSendAttempt() {
        if (!isConfigured || hasProAccess) return
        prefs.edit().putInt(KEY_FREE_SEND_COUNT, freeSendCount + 1).apply()
    }

    fun syncPurchasesAfterOfferCodeRedemption(): Boolean {
        if (!isConfigured) return true
        // A configured billing provider should verify entitlement here before updating KEY_HAS_PRO.
        return false
    }

    fun createRedeemIntent(): Intent? {
        if (!isConfigured) return null
        val managementUrl = BuildConfig.SUBSCRIPTION_MANAGEMENT_URL.trim()
        if (managementUrl.isNotEmpty()) {
            return Intent(Intent.ACTION_VIEW, Uri.parse(managementUrl))
        }
        return Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/redeem"),
        )
    }

    private companion object {
        const val KEY_HAS_PRO = "codex.subscription.hasProAccess"
        const val KEY_FREE_SEND_COUNT = "codex.subscription.freeSendCount"
        const val FREE_SEND_LIMIT = 5
    }
}
