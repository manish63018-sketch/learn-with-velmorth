package com.velmorth.app.data.local.dao

import androidx.room.*
import com.velmorth.app.data.local.entities.DialogueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DialogueDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(dialogues: List<DialogueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dialogue: DialogueEntity)

    /** Returns all lines matching a trigger, ordered by weight descending. */
    @Query("SELECT * FROM velmorth_dialogues WHERE trigger = :trigger AND language = :language ORDER BY weight DESC")
    suspend fun getByTrigger(trigger: String, language: String = "en"): List<DialogueEntity>

    /** Returns all lines for a category. */
    @Query("SELECT * FROM velmorth_dialogues WHERE category = :category AND language = :language")
    suspend fun getByCategory(category: String, language: String = "en"): List<DialogueEntity>

    /** Returns every line for a specific trigger — observed as a Flow. */
    @Query("SELECT * FROM velmorth_dialogues WHERE trigger = :trigger ORDER BY weight DESC")
    fun observeByTrigger(trigger: String): Flow<List<DialogueEntity>>

    @Query("SELECT COUNT(*) FROM velmorth_dialogues")
    suspend fun count(): Int

    @Query("DELETE FROM velmorth_dialogues")
    suspend fun deleteAll()
}
