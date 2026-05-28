package com.example.learnwithvelmorth.domain.repository

import com.example.learnwithvelmorth.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow

interface ProgressRepository {
    fun getProgressForUser(userId: String): Flow<List<UserProgress>>
    fun getProgressForLesson(lessonId: String): Flow<UserProgress?>
    fun getReviewQueue(userId: String): Flow<List<String>>  // Returns question IDs for review
    suspend fun saveProgress(progress: UserProgress)
}
