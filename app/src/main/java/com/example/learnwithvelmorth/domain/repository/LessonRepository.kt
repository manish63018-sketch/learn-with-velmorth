package com.example.learnwithvelmorth.domain.repository

import com.example.learnwithvelmorth.domain.model.Lesson
import com.example.learnwithvelmorth.domain.model.Question
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    fun getLessonsForLanguage(languageId: String): Flow<List<Lesson>>
    fun getLessonById(lessonId: String): Flow<Lesson?>
    fun getQuestionsForLesson(lessonId: String): Flow<List<Question>>
    suspend fun unlockLesson(lessonId: String)
    suspend fun markLessonCompleted(lessonId: String, score: Int)
}
