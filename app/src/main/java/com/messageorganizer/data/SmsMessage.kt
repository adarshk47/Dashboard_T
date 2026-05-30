package com.messageorganizer.data

data class SmsMessage(
    val id: Long,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val type: MessageType = MessageType.INBOX
)

enum class MessageType {
    INBOX, SENT
}
