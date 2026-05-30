package com.velmorth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lessons")
data class LessonEntity(
    @PrimaryKey val id: String,
    val languageId: String,
    val chapterId: String,
    val chapterTitle: String,
    val title: String,
    val description: String,
    val type: String,           // LessonType enum name
    val status: String = "LOCKED", // LessonStatus enum name
    val xpReward: Int = 10,
    val leafReward: Int = 5,
    val durationMinutes: Int = 5,
    val orderIndex: Int = 0,
    val iconEmoji: String = "📖",
    val completedAt: String? = null,
    val bestScore: Int = 0,
)
