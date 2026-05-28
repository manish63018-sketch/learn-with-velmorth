package com.example.learnwithvelmorth.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaf_transactions")
data class LeafTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val amount: Int,
    val type: String,               // LeafTransactionType enum name
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)
