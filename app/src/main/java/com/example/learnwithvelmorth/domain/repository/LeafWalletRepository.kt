package com.example.learnwithvelmorth.domain.repository

import com.example.learnwithvelmorth.domain.model.LeafTransaction
import kotlinx.coroutines.flow.Flow

interface LeafWalletRepository {
    fun getBalance(userId: String): Flow<Int>
    fun getTransactionHistory(userId: String): Flow<List<LeafTransaction>>
    suspend fun addTransaction(transaction: LeafTransaction): Boolean
    suspend fun canAfford(userId: String, amount: Int): Boolean
}
