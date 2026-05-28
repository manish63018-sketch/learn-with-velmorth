package com.example.learnwithvelmorth.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = "local_user",
    val name: String = "Learner",
    val avatarEmoji: String = "🌿",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val leafBalance: Int = 0,
    val isPremium: Boolean = false,
    val selectedLanguageId: String = "es",
    val dailyGoalMinutes: Int = 10,
    val lastActiveDate: String = "",
    val joinedDate: String = "",
    val growthPoints: Int = 0,
    val velmorthMood: String = "HAPPY",
)
