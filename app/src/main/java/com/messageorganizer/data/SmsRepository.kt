package com.messageorganizer.data

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import java.util.UUID

class SmsRepository(private val context: Context) {

    private val groupPreference = GroupPreference(context)

    // OTP keywords
    private val otpKeywords = listOf(
        "otp", "one time password", "one-time password", "verification code",
        "verify", "passcode", "security code", "authentication code",
        "is your otp", "is the otp", "use code", "enter code"
    )

    // Transaction keywords
    private val transactionKeywords = listOf(
        "debited", "credited", "debit", "credit", "transaction", "payment",
        "spent", "paid", "inr", "rs.", "₹", "amount", "balance",
        "a/c", "account", "bank", "transferred", "withdrawal", "deposit",
        "upi", "neft", "imps", "rtgs", "purchase", "refund", "cashback"
    )

    // Promotional keywords
    private val promoKeywords = listOf(
        "offer", "discount", "sale", "deal", "coupon", "promo", "free",
        "win", "won", "prize", "reward", "cashback offer", "off on",
        "exclusive", "hurry", "limited time", "expires"
    )

    fun getAllMessages(): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val cursor = context.contentResolver.query(
            uri, projection, null, null,
            "${Telephony.Sms.DATE} DESC"
        )
        cursor?.use {
            val idIdx = it.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addrIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                messages.add(
                    SmsMessage(
                        id = it.getLong(idIdx),
                        sender = it.getString(addrIdx) ?: "Unknown",
                        body = it.getString(bodyIdx) ?: "",
                        timestamp = it.getLong(dateIdx)
                    )
                )
            }
        }
        return messages
    }

    fun getBuiltInGroups(messages: List<SmsMessage>): List<MessageGroup> {
        val groups = mutableListOf<MessageGroup>()

        val otpMessages = messages.filter { isOtp(it.body) }
        if (otpMessages.isNotEmpty()) {
            groups.add(MessageGroup("otp", "OTP & Verification", GroupIcon.OTP, GroupType.OTP, messageCount = otpMessages.size))
        }

        val transactionMessages = messages.filter { isTransaction(it.body) && !isOtp(it.body) }
        if (transactionMessages.isNotEmpty()) {
            groups.add(MessageGroup("transaction", "Transactions & Payments", GroupIcon.TRANSACTION, GroupType.TRANSACTION, messageCount = transactionMessages.size))
        }

        val promoMessages = messages.filter { isPromo(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
        if (promoMessages.isNotEmpty()) {
            groups.add(MessageGroup("promo", "Promotions & Offers", GroupIcon.PROMO, GroupType.PROMOTIONAL, messageCount = promoMessages.size))
        }

        // Card-wise groups: extract last 4 digits from transaction messages
        val cardGroups = extractCardGroups(transactionMessages)
        groups.addAll(cardGroups)

        return groups
    }

    fun getCustomGroups(messages: List<SmsMessage>): List<MessageGroup> {
        return groupPreference.getCustomGroups().map { group ->
            val count = messages.count { msg -> matchesCustomGroup(msg, group) }
            group.copy(messageCount = count)
        }
    }

    fun getMessagesForGroup(groupId: String, messages: List<SmsMessage>): List<SmsMessage> {
        return when (groupId) {
            "otp" -> messages.filter { isOtp(it.body) }
            "transaction" -> messages.filter { isTransaction(it.body) && !isOtp(it.body) }
            "promo" -> messages.filter { isPromo(it.body) && !isOtp(it.body) && !isTransaction(it.body) }
            else -> {
                if (groupId.startsWith("card_")) {
                    val lastFour = groupId.removePrefix("card_")
                    messages.filter { isTransaction(it.body) && extractCardNumber(it.body) == lastFour }
                } else {
                    val customGroup = groupPreference.getCustomGroups().find { it.id == groupId }
                    if (customGroup != null) {
                        messages.filter { matchesCustomGroup(it, customGroup) }
                    } else {
                        emptyList()
                    }
                }
            }
        }
    }

    fun getMessagesBySender(messages: List<SmsMessage>): Map<String, List<SmsMessage>> {
        return messages.groupBy { normalizeSender(it.sender) }
            .toSortedMap(compareByDescending { sender ->
                messages.count { normalizeSender(it.sender) == sender }
            })
    }

    fun saveCustomGroup(group: MessageGroup) {
        groupPreference.saveCustomGroup(group)
    }

    fun deleteCustomGroup(groupId: String) {
        groupPreference.deleteCustomGroup(groupId)
    }

    fun createCustomGroup(name: String, keywords: List<String>, senderPattern: String?): MessageGroup {
        return MessageGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = GroupIcon.FOLDER,
            groupType = GroupType.CUSTOM,
            keywords = keywords,
            senderPattern = senderPattern,
            isCustom = true
        )
    }

    // ---- helpers ----

    private fun isOtp(body: String): Boolean {
        val lower = body.lowercase()
        return otpKeywords.any { lower.contains(it) } ||
                Regex("\\b\\d{4,8}\\b").containsMatchIn(body) && lower.contains("otp")
    }

    private fun isTransaction(body: String): Boolean {
        val lower = body.lowercase()
        return transactionKeywords.any { lower.contains(it) }
    }

    private fun isPromo(body: String): Boolean {
        val lower = body.lowercase()
        return promoKeywords.any { lower.contains(it) }
    }

    private fun extractCardNumber(body: String): String? {
        // Match patterns like "card ending 1234", "card XX1234", "**1234", "ending with 1234"
        val patterns = listOf(
            Regex("(?:card|a/c|account)[^\\d]*(\\d{4})\\b", RegexOption.IGNORE_CASE),
            Regex("\\*+(\\d{4})\\b"),
            Regex("(?:ending|ending with|ending in)[^\\d]*(\\d{4})\\b", RegexOption.IGNORE_CASE),
            Regex("X{2,}(\\d{4})\\b", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) return match.groupValues[1]
        }
        return null
    }

    private fun extractCardGroups(transactionMessages: List<SmsMessage>): List<MessageGroup> {
        return transactionMessages
            .mapNotNull { extractCardNumber(it.body) }
            .distinct()
            .map { lastFour ->
                val count = transactionMessages.count { extractCardNumber(it.body) == lastFour }
                MessageGroup(
                    id = "card_$lastFour",
                    name = "Card ****$lastFour",
                    icon = GroupIcon.CREDIT_CARD,
                    groupType = GroupType.CARD,
                    cardLastFour = lastFour,
                    messageCount = count
                )
            }
    }

    private fun matchesCustomGroup(msg: SmsMessage, group: MessageGroup): Boolean {
        val lower = msg.body.lowercase()
        val keywordMatch = group.keywords.any { lower.contains(it.lowercase()) }
        val senderMatch = group.senderPattern?.let {
            msg.sender.contains(it, ignoreCase = true)
        } ?: false
        return keywordMatch || senderMatch
    }

    private fun normalizeSender(sender: String): String {
        return sender.trim().uppercase()
    }
}
