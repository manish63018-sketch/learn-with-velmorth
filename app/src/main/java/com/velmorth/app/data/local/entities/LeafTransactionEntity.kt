package com.velmorth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a historical leaf transaction (earnings or spendings).
 * Part of the Room persistence layer (V2).
 */
@Entity(tableName = "leaf_transactions")
data class LeafTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Int,
    val type: String, // "EARNED" | "SPENT"
    val source: String, // "DAILY_LOGIN" | "LESSON_COMPLETE" | "SHOP_PURCHASE"
    val timestamp: Long = System.currentTimeMillis(),
    val description: String? = null
)
