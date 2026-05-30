package com.velmorth.app.data.local.dao

import androidx.room.*
import com.velmorth.app.data.local.entities.LeafTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeafWalletDao {
    @Query("SELECT SUM(amount) FROM leaf_transactions WHERE userId = :userId")
    fun getBalance(userId: String): Flow<Int?>

    @Query("SELECT * FROM leaf_transactions WHERE userId = :userId ORDER BY timestamp DESC LIMIT 50")
    fun getTransactionHistory(userId: String): Flow<List<LeafTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LeafTransactionEntity)

    @Query("SELECT SUM(amount) FROM leaf_transactions WHERE userId = :userId")
    suspend fun getBalanceOnce(userId: String): Int?

    @Query("DELETE FROM leaf_transactions WHERE userId = :userId")
    suspend fun clearTransactionsForUser(userId: String)
}
