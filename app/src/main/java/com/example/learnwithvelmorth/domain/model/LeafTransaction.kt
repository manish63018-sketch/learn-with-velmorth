package com.example.learnwithvelmorth.domain.model

enum class LeafTransactionType {
    EARN_LESSON,        // Earned by completing a lesson
    EARN_STREAK,        // Bonus for daily streak
    EARN_QUIZ,          // Earned from quiz mode
    EARN_REVIEW,        // Earned from review session
    SPEND_SHOP,         // Spent in the leaf shop
    SPEND_HINT,         // Used a hint
    ADMIN_GRANT,        // Welcome bonus / admin
}

data class LeafTransaction(
    val id: String = "",
    val userId: String = "local_user",
    val amount: Int,                   // Positive = earn, negative = spend
    val type: LeafTransactionType,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
