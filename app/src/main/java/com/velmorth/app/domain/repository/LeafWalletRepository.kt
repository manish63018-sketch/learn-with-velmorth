package com.velmorth.app.domain.repository

import com.velmorth.app.data.local.entities.LeafTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Domain-level interface for leaf wallet operations.
 */
interface LeafWalletRepository {
    fun getBalance(): Flow<Int>
    fun getTransactions(): Flow<List<LeafTransactionEntity>>
    suspend fun addTransaction(amount: Int, type: String, source: String, description: String? = null)
    suspend fun clearHistory()
}
