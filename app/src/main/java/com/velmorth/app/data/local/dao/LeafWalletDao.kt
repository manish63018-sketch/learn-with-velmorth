package com.velmorth.app.data.local.dao

import androidx.room.*
import com.velmorth.app.data.local.entities.LeafTransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for leaf transactions.
 * Part of the Room persistence layer (V2).
 */
@Dao
interface LeafWalletDao {

    @Query("SELECT * FROM leaf_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<LeafTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransaction(transaction: LeafTransactionEntity): Long

    @Query("SELECT SUM(amount) FROM leaf_transactions")
    fun getLeafBalance(): Flow<Int?>

    @Query("DELETE FROM leaf_transactions")
    fun clearHistory(): Int
}
