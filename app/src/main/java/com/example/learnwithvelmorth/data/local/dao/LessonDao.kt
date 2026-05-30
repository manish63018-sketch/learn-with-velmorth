package com.example.learnwithvelmorth.data.local.dao

import androidx.room.*
import com.example.learnwithvelmorth.data.local.entities.LessonEntity
import com.example.learnwithvelmorth.data.local.entities.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {
    @Query("SELECT * FROM lessons WHERE languageId = :languageId ORDER BY orderIndex ASC")
    fun getLessonsForLanguage(languageId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    fun getLessonById(lessonId: String): Flow<LessonEntity?>

    @Query("SELECT * FROM questions WHERE lessonId = :lessonId ORDER BY orderIndex ASC")
    fun getQuestionsForLesson(lessonId: String): Flow<List<QuestionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLessons(lessons: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("UPDATE lessons SET status = 'AVAILABLE' WHERE id = :lessonId")
    suspend fun unlockLesson(lessonId: String)

    @Query("UPDATE lessons SET status = 'COMPLETED', completedAt = :completedAt, bestScore = MAX(bestScore, :score) WHERE id = :lessonId")
    suspend fun markLessonCompleted(lessonId: String, score: Int, completedAt: String)

    @Query("SELECT COUNT(*) FROM lessons WHERE languageId = :languageId AND status = 'COMPLETED'")
    suspend fun getCompletedLessonCount(languageId: String): Int

    @Query("UPDATE lessons SET status = 'LOCKED', completedAt = NULL, bestScore = 0")
    suspend fun resetAllLessons()

    @Query("UPDATE lessons SET status = 'AVAILABLE' WHERE orderIndex = 1")
    suspend fun unlockFirstLessons()
}
