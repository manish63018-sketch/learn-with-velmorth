package com.example.learnwithvelmorth.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnwithvelmorth.data.local.db.StringListConverter

@Entity(tableName = "user_progress")
@TypeConverters(StringListConverter::class)
data class UserProgressEntity(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val lessonId: String,
    val score: Int = 0,
    val xpEarned: Int = 0,
    val leavesEarned: Int = 0,
    val timeSpentSeconds: Int = 0,
    val attemptsCount: Int = 0,
    val completedAt: String = "",
    val incorrectQuestionIds: List<String> = emptyList(),
)
