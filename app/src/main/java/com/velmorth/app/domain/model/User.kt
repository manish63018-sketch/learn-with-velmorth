package com.velmorth.app.domain.model

data class User(
    val id: String = "local_user",
    val name: String = "Learner",
    val avatarEmoji: String = "🌿",
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXp: Int = 0,
    val leafBalance: Int = 0,
    val isPremium: Boolean = false,
    val selectedLanguageId: String = "ja",
    val nativeLanguageId: String = "en",    // Added: Decision #4
    val dailyGoalMinutes: Int = 10,
    val lastActiveDate: String = "",        // ISO date string yyyy-MM-dd
    val joinedDate: String = "",
    val growthPoints: Int = 0,
    val velmorthMood: String = "HAPPY",
)
