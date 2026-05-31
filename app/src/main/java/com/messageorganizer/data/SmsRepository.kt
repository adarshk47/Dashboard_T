package com.messageorganizer.data

import android.content.Context
import android.provider.Telephony
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID

class SmsRepository(private val context: Context) {

    val blockedSenderManager = BlockedSenderManager(context)
    private val groupPreference = GroupPreference(context)

    // Simple in-memory cache
    private var cachedMessages: List<SmsMessage>? = null
    private var lastLoadTime = 0L
    private val cacheValidMs = 5 * 60 * 1000L // 5 minutes

    private val otpKeywords = listOf(
        "otp", "one time password", "one-time password", "verification code",
        "verify", "passcode", "security code", "do not share", "is your otp",
        "is the otp", "use code", "enter code", "authentication code"
    )
    private val transactionKeywords = listOf(
        "debited", "credited", "debit", "credit", "transaction", "payment",
        "spent", "paid", "inr", "rs.", "₹", "balance", "a/c", "bank",
        "transferred", "withdrawal", "deposit", "upi", "neft", "imps",
        "rtgs", "purchase", "refund", "cashback", "deducted", "sent to"
    )
    private val promoKeywords = listOf(
        "offer", "discount", "sale", "deal", "coupon", "promo",
        "% off", "flat", "exclusive", "hurry", "expires", "limited time"
    )
    private val spamKeywords = listOf(
        "click here", "call now", "earn money", "loan approved",
        "congratulations you won", "lucky winner", "work from home",
        "job offer", "make money online"
    )

    fun getAllMessages(forceRefresh: Boolean = false): List<SmsMessage> {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedMessages != null && (now - lastLoadTime) < cacheValidMs) {
            return cachedMessages!!
        }
        val messages = mutableListOf<SmsMessage>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            null, null, "${Telephony.Sms.DATE} DESC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val sender = it.getString(addrIdx) ?: "Unknown"
                if (!blockedSenderManager.isBlocked(sender)) {
                    messages.add(SmsMessage(
                        id = it.getLong(idIdx),
                        sender = sender,
                        body = it.getString(bodyIdx) ?: "",
                        timestamp = it.getLong(dateIdx)
                    ))
                }
            }
        }
        cachedMessages = messages
        lastLoadTime = now
        return messages
    }

    /** Emits groups one-by-one as they are detected — drives progressive UI */
    fun getGroupsProgressively(messages: List<SmsMessage>): Flow<List<MessageGroup>> = flow {
        val groups = mutableListOf<MessageGroup>()

        val otpMsgs = messages.filter { isOtp(it.body) }
        if (otpMsgs.isNotEmpty()) {
            groups.add(MessageGroup("otp", "OTP & Verification", GroupIcon.OTP, GroupType.OTP, messageCount = otpMsgs.size))
            emit(groups.toList())
        }

        val txMsgs = messages.filter { isTransaction(it.body) && !isOtp(it.body) }
        if (txMsgs.isNotEmpty()) {
            groups.add(MessageGroup("transaction", "Transactions & Payments", GroupIcon.TRANSACTION, GroupType.TRANSACTION, messageCount = txMsgs.size))
            emit(groups.toList())
        }

        // Debit-only group
        val debitMsgs = txMsgs.filter { it.body.lowercase().let { b -> b.contains("debited") || b.contains("deducted") || b.contains("spent") } }
        if (debitMsgs.isNotEmpty()) {
            groups.add(MessageGroup("debit", "Money Sent / Debited", GroupIcon.TRANSACTION, GroupType.TRANSACTION, messageCount = debitMsgs.size))
            emit(groups.toList())
        }

        // Credit-only group
        val creditMsgs = txMsgs.filter { it.body.lowercase().let { b -> b.contains("credited") || b.contains("received") || b.contains("refund") } }
        if (creditMsgs.isNotEmpty()) {
            groups.add(MessageGroup("credit", "Money Received / Credited", GroupIcon.TRANSACTION, GroupType.TRANSACTION, messageCount = creditMsgs.size))
            emit(groups.toList())
        }

        val promoMsgs = messages.filter { isPromo(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
        if (promoMsgs.isNotEmpty()) {
            groups.add(MessageGroup("promo", "Promotions & Offers", GroupIcon.PROMO, GroupType.PROMOTIONAL, messageCount = promoMsgs.size))
            emit(groups.toList())
        }

        val spamMsgs = messages.filter { isSpam(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
        if (spamMsgs.isNotEmpty()) {
            groups.add(MessageGroup("spam", "Spam Messages", GroupIcon.FOLDER, GroupType.CUSTOM, messageCount = spamMsgs.size))
            emit(groups.toList())
        }

        // Smart card groups — deduplicate same last-4 across senders
        val cardGroups = extractSmartCardGroups(txMsgs)
        if (cardGroups.isNotEmpty()) {
            groups.addAll(cardGroups)
            emit(groups.toList())
        }

        // Custom groups
        val customGroups = groupPreference.getCustomGroups().map { g ->
            g.copy(messageCount = messages.count { matchesCustomGroup(it, g) })
        }
        if (customGroups.isNotEmpty()) {
            groups.addAll(customGroups)
            emit(groups.toList())
        }

        emit(groups.toList())
    }

    fun getMessagesForGroup(groupId: String, messages: List<SmsMessage>): List<SmsMessage> = when (groupId) {
        "otp"         -> messages.filter { isOtp(it.body) }
        "transaction" -> messages.filter { isTransaction(it.body) && !isOtp(it.body) }
        "debit"       -> messages.filter { isTransaction(it.body) && it.body.lowercase().let { b -> b.contains("debited") || b.contains("deducted") || b.contains("spent") } }
        "credit"      -> messages.filter { isTransaction(it.body) && it.body.lowercase().let { b -> b.contains("credited") || b.contains("received") || b.contains("refund") } }
        "promo"       -> messages.filter { isPromo(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
        "spam"        -> messages.filter { isSpam(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
        else -> {
            if (groupId.startsWith("card_")) {
                val lastFour = groupId.removePrefix("card_")
                messages.filter { isTransaction(it.body) && extractCardNumber(it.body) == lastFour }
            } else {
                val cg = groupPreference.getCustomGroups().find { it.id == groupId }
                if (cg != null) messages.filter { matchesCustomGroup(it, cg) } else emptyList()
            }
        }
    }

    fun getMessagesBySender(messages: List<SmsMessage>): Map<String, List<SmsMessage>> =
        messages.groupBy { it.sender.trim().uppercase() }
            .entries.sortedByDescending { it.value.size }
            .associate { it.key to it.value }

    fun saveCustomGroup(group: MessageGroup) = groupPreference.saveCustomGroup(group)
    fun deleteCustomGroup(groupId: String) = groupPreference.deleteCustomGroup(groupId)
    fun renameCustomGroup(groupId: String, newName: String) = groupPreference.renameGroup(groupId, newName)

    fun createCustomGroup(name: String, keywords: List<String>, senderPattern: String?): MessageGroup =
        MessageGroup(id = UUID.randomUUID().toString(), name = name, icon = GroupIcon.FOLDER,
            groupType = GroupType.CUSTOM, keywords = keywords, senderPattern = senderPattern, isCustom = true)

    fun getBookmarkedIds(): Set<String> = groupPreference.getBookmarkedIds()
    fun toggleBookmark(groupId: String) = groupPreference.toggleBookmark(groupId)

    fun extractCardNumber(body: String): String? {
        val patterns = listOf(
            Regex("card\\s*(?:ending|no\\.?|ending with|ending in)?\\s*[xX*]{0,6}(\\d{4})\\b", RegexOption.IGNORE_CASE),
            Regex("ending\\s*(?:with|in)?\\s*[xX*]{0,4}(\\d{4})\\b", RegexOption.IGNORE_CASE),
            Regex("[xX*]{2,}(\\d{4})\\b"),
            Regex("(?:a/c|a\\.c|account)\\s*(?:no\\.?)?\\s*[xX*]{0,8}(\\d{4})\\b", RegexOption.IGNORE_CASE),
            Regex("(?:visa|master|rupay|debit|credit)\\s*(?:card)?\\s*[xX*]{0,4}(\\d{4})\\b", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(body); if (m != null) return m.groupValues[1]
        }
        return null
    }

    private fun extractSmartCardGroups(txMsgs: List<SmsMessage>): List<MessageGroup> {
        // Group by last-4, ignore duplicates from different senders for same card number
        return txMsgs
            .mapNotNull { msg -> extractCardNumber(msg.body)?.let { msg to it } }
            .groupBy { it.second }
            .filter { it.value.size >= 2 } // only create card group if ≥2 messages
            .map { (lastFour, entries) ->
                MessageGroup(
                    id = "card_$lastFour",
                    name = "Card ****$lastFour",
                    icon = GroupIcon.CREDIT_CARD,
                    groupType = GroupType.CARD,
                    cardLastFour = lastFour,
                    messageCount = entries.size
                )
            }
    }

    private fun isOtp(body: String) = otpKeywords.any { body.lowercase().contains(it) }
    private fun isTransaction(body: String) = transactionKeywords.any { body.lowercase().contains(it) }
    private fun isPromo(body: String) = promoKeywords.any { body.lowercase().contains(it) }
    private fun isSpam(body: String) = spamKeywords.any { body.lowercase().contains(it) }

    private fun matchesCustomGroup(msg: SmsMessage, group: MessageGroup): Boolean {
        val lower = msg.body.lowercase()
        return group.keywords.any { lower.contains(it.lowercase()) } ||
               (group.senderPattern?.let { msg.sender.contains(it, ignoreCase = true) } ?: false)
    }
}
