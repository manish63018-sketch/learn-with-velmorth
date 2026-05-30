package com.example.learnwithvelmorth.domain.repository

import com.example.learnwithvelmorth.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun updateStreak(userId: String)
    suspend fun updateLeafBalance(userId: String, delta: Int)
    suspend fun setPremiumStatus(userId: String, isPremium: Boolean)
    suspend fun setSelectedLanguage(userId: String, languageId: String)
    suspend fun setDailyGoal(userId: String, minutes: Int)
    suspend fun updateGrowthPoints(userId: String, points: Int)
    suspend fun updateVelmorthMood(userId: String, mood: String)
    suspend fun addXp(userId: String, xp: Int)
    suspend fun resetUser(userId: String)
}
