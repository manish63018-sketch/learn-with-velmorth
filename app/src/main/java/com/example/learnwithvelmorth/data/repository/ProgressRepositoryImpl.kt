package com.example.learnwithvelmorth.data.repository

import com.example.learnwithvelmorth.data.local.dao.ProgressDao
import com.example.learnwithvelmorth.data.local.entities.UserProgressEntity
import com.example.learnwithvelmorth.domain.model.UserProgress
import com.example.learnwithvelmorth.domain.repository.ProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class ProgressRepositoryImpl @Inject constructor(
    private val progressDao: ProgressDao,
) : ProgressRepository {

    override fun getProgressForUser(userId: String): Flow<List<UserProgress>> =
        progressDao.getProgressForUser(userId).map { list -> list.map { it.toDomain() } }

    override fun getProgressForLesson(lessonId: String): Flow<UserProgress?> =
        progressDao.getLatestProgressForLesson(lessonId).map { it?.toDomain() }

    override fun getReviewQueue(userId: String): Flow<List<String>> =
        progressDao.getProgressForUser(userId).map { list ->
            list.flatMap { it.incorrectQuestionIds }.distinct()
        }

    override suspend fun saveProgress(progress: UserProgress) =
        progressDao.saveProgress(progress.toEntity())
}

private fun UserProgressEntity.toDomain() = UserProgress(
    id = id, userId = userId, lessonId = lessonId,
    score = score, xpEarned = xpEarned,
    leavesEarned = leavesEarned, timeSpentSeconds = timeSpentSeconds,
    attemptsCount = attemptsCount, completedAt = completedAt,
    incorrectQuestionIds = incorrectQuestionIds,
)

private fun UserProgress.toEntity() = UserProgressEntity(
    id = if (id.isBlank()) UUID.randomUUID().toString() else id,
    userId = userId, lessonId = lessonId,
    score = score, xpEarned = xpEarned,
    leavesEarned = leavesEarned, timeSpentSeconds = timeSpentSeconds,
    attemptsCount = attemptsCount,
    completedAt = completedAt.ifBlank {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
    },
    incorrectQuestionIds = incorrectQuestionIds,
)
