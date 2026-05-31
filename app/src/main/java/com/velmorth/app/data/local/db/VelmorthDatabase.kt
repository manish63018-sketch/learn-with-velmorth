package com.velmorth.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.velmorth.app.data.local.dao.LeafWalletDao
import com.velmorth.app.data.local.entities.LeafTransactionEntity

/**
 * Main Room database for the Velmorth application.
 * Currently tracks leaf transactions, with hooks for more entities in V2.
 */
@Database(
    entities = [LeafTransactionEntity::class],
    version = 3, // Synchronized with schemas/3.json if exists
    exportSchema = true
)
abstract class VelmorthDatabase : RoomDatabase() {

    abstract fun leafWalletDao(): LeafWalletDao

    companion object {
        @Volatile
        private var INSTANCE: VelmorthDatabase? = null

        fun getDatabase(context: Context): VelmorthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VelmorthDatabase::class.java,
                    "velmorth_database"
                )
                .fallbackToDestructiveMigration() // V2 experimental
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
