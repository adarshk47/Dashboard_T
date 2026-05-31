package com.messageorganizer.util

import com.messageorganizer.data.SmsMessage
import java.util.Date

data class TransactionEntry(
    val message: SmsMessage,
    val amount: Double,
    val type: TransactionType,
    val cardLastFour: String?,
    val date: Date
)

enum class TransactionType { DEBIT, CREDIT, UNKNOWN }

object TransactionParser {

    private val amountPatterns = listOf(
        Regex("(?:Rs\\.?|INR|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Regex("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:Rs\\.?|INR|₹)"),
        Regex("(?:amount|amt)[:\\s]+(?:Rs\\.?|INR|₹)?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    )

    fun parseAmount(body: String): Double? {
        for (pattern in amountPatterns) {
            val match = pattern.find(body) ?: continue
            val amountStr = match.groupValues[1].replace(",", "")
            val amount = amountStr.toDoubleOrNull()
            if (amount != null && amount > 0) return amount
        }
        return null
    }

    fun parseType(body: String): TransactionType {
        val lower = body.lowercase()
        return when {
            lower.contains("debited") || lower.contains("withdrawn") ||
            lower.contains("spent") || lower.contains("payment of") ||
            lower.contains("paid") || lower.contains("deducted") -> TransactionType.DEBIT
            lower.contains("credited") || lower.contains("received") ||
            lower.contains("refund") || lower.contains("cashback") ||
            lower.contains("deposited") -> TransactionType.CREDIT
            else -> TransactionType.UNKNOWN
        }
    }

    fun toEntries(messages: List<SmsMessage>, getCard: (String) -> String?): List<TransactionEntry> {
        return messages.mapNotNull { msg ->
            val amount = parseAmount(msg.body) ?: return@mapNotNull null
            TransactionEntry(
                message = msg,
                amount = amount,
                type = parseType(msg.body),
                cardLastFour = getCard(msg.body),
                date = Date(msg.timestamp)
            )
        }
    }
}
