package com.velmorth.app.domain.model

enum class LessonType {
    VOCABULARY, GRAMMAR, LISTENING, SPEAKING, READING, CULTURE
}

enum class LessonStatus {
    LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED
}

data class Lesson(
    val id: String,
    val languageId: String,
    val chapterId: String,
    val chapterTitle: String,
    val title: String,
    val description: String,
    val type: LessonType,
    val status: LessonStatus = LessonStatus.LOCKED,
    val xpReward: Int = 10,
    val leafReward: Int = 5,
    val durationMinutes: Int = 5,
    val orderIndex: Int = 0,
    val iconEmoji: String = "📖",
    val completedAt: String? = null,
    val bestScore: Int = 0,    // 0-100
)
