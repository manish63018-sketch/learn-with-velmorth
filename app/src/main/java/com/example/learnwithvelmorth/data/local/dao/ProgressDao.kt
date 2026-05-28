package com.example.learnwithvelmorth.data.local.dao

import androidx.room.*
import com.example.learnwithvelmorth.data.local.entities.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE userId = :userId ORDER BY completedAt DESC")
    fun getProgressForUser(userId: String): Flow<List<UserProgressEntity>>

    @Query("SELECT * FROM user_progress WHERE lessonId = :lessonId ORDER BY completedAt DESC LIMIT 1")
    fun getLatestProgressForLesson(lessonId: String): Flow<UserProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: UserProgressEntity)

    // Returns all incorrectQuestionIds from the last 7 days for spaced repetition
    @Query("""
        SELECT incorrectQuestionIds FROM user_progress 
        WHERE userId = :userId 
        AND completedAt >= :sevenDaysAgo
        ORDER BY completedAt DESC
    """)
    suspend fun getRecentIncorrectAnswerSets(userId: String, sevenDaysAgo: String): List<String>

    @Query("SELECT COUNT(*) FROM user_progress WHERE userId = :userId AND completedAt >= :today")
    suspend fun getLessonsCompletedToday(userId: String, today: String): Int
}
