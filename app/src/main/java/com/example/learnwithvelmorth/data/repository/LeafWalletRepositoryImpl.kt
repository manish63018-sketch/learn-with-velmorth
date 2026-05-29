package com.example.learnwithvelmorth.data.repository

import com.example.learnwithvelmorth.data.local.dao.LeafWalletDao
import com.example.learnwithvelmorth.data.local.entities.LeafTransactionEntity
import com.example.learnwithvelmorth.domain.model.LeafTransaction
import com.example.learnwithvelmorth.domain.model.LeafTransactionType
import com.example.learnwithvelmorth.domain.repository.LeafWalletRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class LeafWalletRepositoryImpl @Inject constructor(
    private val leafWalletDao: LeafWalletDao,
) : LeafWalletRepository {

    override fun getBalance(userId: String): Flow<Int> =
        leafWalletDao.getBalance(userId).map { it ?: 0 }

    override fun getTransactionHistory(userId: String): Flow<List<LeafTransaction>> =
        leafWalletDao.getTransactionHistory(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun addTransaction(transaction: LeafTransaction): Boolean {
        val entity = transaction.toEntity()
        leafWalletDao.insertTransaction(entity)
        return true
    }

    override suspend fun canAfford(userId: String, amount: Int): Boolean {
        val balance = leafWalletDao.getBalanceOnce(userId) ?: 0
        return balance >= amount
    }
}

private fun LeafTransactionEntity.toDomain() = LeafTransaction(
    id = id, userId = userId, amount = amount,
    type = runCatching { LeafTransactionType.valueOf(type) }.getOrDefault(LeafTransactionType.EARN_LESSON),
    description = description, timestamp = timestamp,
)

private fun LeafTransaction.toEntity() = LeafTransactionEntity(
    id = if (id.isBlank()) UUID.randomUUID().toString() else id,
    userId = userId, amount = amount,
    type = type.name, description = description, timestamp = timestamp,
)
