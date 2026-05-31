package com.messageorganizer.data

import android.content.Context

class BlockedSenderManager(context: Context) {

    private val prefs = context.getSharedPreferences("blocked_senders", Context.MODE_PRIVATE)

    fun blockSender(sender: String) {
        val blocked = getBlockedSenders().toMutableSet()
        blocked.add(sender.trim().uppercase())
        prefs.edit().putStringSet("senders", blocked).apply()
    }

    fun unblockSender(sender: String) {
        val blocked = getBlockedSenders().toMutableSet()
        blocked.remove(sender.trim().uppercase())
        prefs.edit().putStringSet("senders", blocked).apply()
    }

    fun isBlocked(sender: String): Boolean =
        getBlockedSenders().contains(sender.trim().uppercase())

    fun getBlockedSenders(): Set<String> =
        prefs.getStringSet("senders", emptySet()) ?: emptySet()
}
