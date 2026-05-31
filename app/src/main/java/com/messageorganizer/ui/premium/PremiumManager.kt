package com.messageorganizer.ui.premium

import android.content.Context

object PremiumManager {

    private const val KEY_PREMIUM = "is_premium"

    fun isPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences("premium", Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PREMIUM, false)
    }

    // Called after successful payment verification
    fun activatePremium(context: Context) {
        context.getSharedPreferences("premium", Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREMIUM, true).apply()
    }

    // Premium features list
    val premiumFeatures = listOf(
        "Export messages to Excel/CSV",
        "Transaction summary with charts",
        "Auto backup to Google Drive",
        "Block spam senders",
        "Date range filter",
        "Card-wise spending report",
        "Home screen widget",
        "Ad-free experience",
        "PIN lock for app",
        "Custom group icons & colors"
    )
}
