package com.velmorth.app.data.repository

import com.velmorth.app.data.local.dao.LeafWalletDao
import com.velmorth.app.data.local.entities.LeafTransactionEntity
import com.velmorth.app.domain.repository.LeafWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/**
 * Implementation of LeafWalletRepository using Room database.
 */
class LeafWalletRepositoryImpl(private val leafWalletDao: LeafWalletDao) : LeafWalletRepository {

    override fun getBalance(): Flow<Int> {
        return leafWalletDao.getLeafBalance().map { it ?: 0 }
    }

    override fun getTransactions(): Flow<List<LeafTransactionEntity>> {
        return leafWalletDao.getAllTransactions()
    }

    override suspend fun addTransaction(amount: Int, type: String, source: String, description: String?) {
        val transaction = LeafTransactionEntity(
            amount = amount,
            type = type,
            source = source,
            description = description
        )
        withContext(Dispatchers.IO) {
            leafWalletDao.insertTransaction(transaction)
        }
    }

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            leafWalletDao.clearHistory()
        }
    }
}
