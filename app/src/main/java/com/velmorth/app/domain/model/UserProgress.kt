package com.velmorth.app.domain.model

data class UserProgress(
    val id: String = "",
    val userId: String = "local_user",
    val lessonId: String,
    val score: Int = 0,            // 0-100
    val xpEarned: Int = 0,
    val leavesEarned: Int = 0,
    val timeSpentSeconds: Int = 0,
    val attemptsCount: Int = 0,
    val completedAt: String = "",  // ISO datetime
    val incorrectQuestionIds: List<String> = emptyList(), // For spaced repetition
)
