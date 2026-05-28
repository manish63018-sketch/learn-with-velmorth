package com.example.learnwithvelmorth.data.local.dao

import androidx.room.*
import com.example.learnwithvelmorth.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUser(userId: String = "local_user"): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET currentStreak = :streak, longestStreak = MAX(longestStreak, :streak), lastActiveDate = :today WHERE id = :userId")
    suspend fun updateStreak(userId: String, streak: Int, today: String)

    @Query("UPDATE users SET leafBalance = leafBalance + :delta WHERE id = :userId")
    suspend fun updateLeafBalance(userId: String, delta: Int)

    @Query("UPDATE users SET totalXp = totalXp + :xp WHERE id = :userId")
    suspend fun addXp(userId: String, xp: Int)

    @Query("UPDATE users SET isPremium = :isPremium WHERE id = :userId")
    suspend fun setPremiumStatus(userId: String, isPremium: Boolean)

    @Query("UPDATE users SET selectedLanguageId = :languageId WHERE id = :userId")
    suspend fun setSelectedLanguage(userId: String, languageId: String)

    @Query("UPDATE users SET dailyGoalMinutes = :minutes WHERE id = :userId")
    suspend fun setDailyGoal(userId: String, minutes: Int)

    @Query("UPDATE users SET growthPoints = growthPoints + :points WHERE id = :userId")
    suspend fun updateGrowthPoints(userId: String, points: Int)

    @Query("UPDATE users SET velmorthMood = :mood WHERE id = :userId")
    suspend fun updateVelmorthMood(userId: String, mood: String)
}
