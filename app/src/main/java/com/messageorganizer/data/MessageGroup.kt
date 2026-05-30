package com.messageorganizer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class MessageGroup(
    val id: String,
    val name: String,
    val icon: GroupIcon,
    val groupType: GroupType,
    val keywords: List<String> = emptyList(),
    val senderPattern: String? = null,
    val cardLastFour: String? = null,
    val messageCount: Int = 0,
    val isCustom: Boolean = false
)

enum class GroupType {
    OTP,
    TRANSACTION,
    CARD,
    SENDER,
    PROMOTIONAL,
    CUSTOM
}

enum class GroupIcon {
    OTP, TRANSACTION, CREDIT_CARD, PERSON, PROMO, FOLDER
}
