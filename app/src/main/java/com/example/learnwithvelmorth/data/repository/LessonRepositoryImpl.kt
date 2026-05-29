package com.example.learnwithvelmorth.data.repository

import com.example.learnwithvelmorth.data.local.dao.LessonDao
import com.example.learnwithvelmorth.data.local.entities.LessonEntity
import com.example.learnwithvelmorth.data.local.entities.QuestionEntity
import com.example.learnwithvelmorth.domain.model.*
import com.example.learnwithvelmorth.domain.repository.LessonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class LessonRepositoryImpl @Inject constructor(
    private val lessonDao: LessonDao,
) : LessonRepository {

    override fun getLessonsForLanguage(languageId: String): Flow<List<Lesson>> =
        lessonDao.getLessonsForLanguage(languageId).map { list -> list.map { it.toDomain() } }

    override fun getLessonById(lessonId: String): Flow<Lesson?> =
        lessonDao.getLessonById(lessonId).map { it?.toDomain() }

    override fun getQuestionsForLesson(lessonId: String): Flow<List<Question>> =
        lessonDao.getQuestionsForLesson(lessonId).map { list -> list.map { it.toDomain() } }

    override suspend fun unlockLesson(lessonId: String) =
        lessonDao.unlockLesson(lessonId)

    override suspend fun markLessonCompleted(lessonId: String, score: Int) {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        lessonDao.markLessonCompleted(lessonId, score, now)
    }
}

private fun LessonEntity.toDomain() = Lesson(
    id = id, languageId = languageId, chapterId = chapterId,
    chapterTitle = chapterTitle, title = title, description = description,
    type = runCatching { LessonType.valueOf(type) }.getOrDefault(LessonType.VOCABULARY),
    status = runCatching { LessonStatus.valueOf(status) }.getOrDefault(LessonStatus.LOCKED),
    xpReward = xpReward, leafReward = leafReward,
    durationMinutes = durationMinutes, orderIndex = orderIndex,
    iconEmoji = iconEmoji, completedAt = completedAt, bestScore = bestScore,
)

private fun QuestionEntity.toDomain() = Question(
    id = id, lessonId = lessonId,
    type = runCatching { QuestionType.valueOf(type) }.getOrDefault(QuestionType.MULTIPLE_CHOICE),
    prompt = prompt, targetWord = targetWord,
    options = options, correctAnswer = correctAnswer,
    explanation = explanation, audioUrl = audioUrl,
    imageUrl = imageUrl, orderIndex = orderIndex, xpValue = xpValue,
)
