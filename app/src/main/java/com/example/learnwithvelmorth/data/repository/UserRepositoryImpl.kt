package com.example.learnwithvelmorth.data.repository

import com.example.learnwithvelmorth.data.local.dao.UserDao
import com.example.learnwithvelmorth.data.local.entities.UserEntity
import com.example.learnwithvelmorth.domain.model.User
import com.example.learnwithvelmorth.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
) : UserRepository {

    override fun getUser(): Flow<User?> =
        userDao.getUser("local_user").map { it?.toDomain() }

    override suspend fun saveUser(user: User) =
        userDao.insertUser(user.toEntity())

    override suspend fun updateStreak(userId: String) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(Date())
        val user = userDao.getUser(userId).first()
        val newStreak = if (wasYesterday(user?.lastActiveDate, fmt)) {
            (user?.currentStreak ?: 0) + 1
        } else {
            1 // streak broken or first time — reset to 1
        }
        userDao.updateStreak(userId, newStreak, today)
    }

    private fun wasYesterday(dateStr: String?, fmt: SimpleDateFormat): Boolean {
        if (dateStr.isNullOrBlank()) return false
        return try {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DATE, -1)
            val yesterday = fmt.format(cal.time)
            dateStr == yesterday
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateLeafBalance(userId: String, delta: Int) =
        userDao.updateLeafBalance(userId, delta)

    override suspend fun setPremiumStatus(userId: String, isPremium: Boolean) =
        userDao.setPremiumStatus(userId, isPremium)

    override suspend fun setSelectedLanguage(userId: String, languageId: String) =
        userDao.setSelectedLanguage(userId, languageId)

    override suspend fun setDailyGoal(userId: String, minutes: Int) =
        userDao.setDailyGoal(userId, minutes)

    override suspend fun updateGrowthPoints(userId: String, points: Int) =
        userDao.updateGrowthPoints(userId, points)

    override suspend fun updateVelmorthMood(userId: String, mood: String) =
        userDao.updateVelmorthMood(userId, mood)
}

private fun UserEntity.toDomain() = User(
    id = id, name = name, avatarEmoji = avatarEmoji,
    currentStreak = currentStreak, longestStreak = longestStreak,
    totalXp = totalXp, leafBalance = leafBalance,
    isPremium = isPremium, selectedLanguageId = selectedLanguageId,
    dailyGoalMinutes = dailyGoalMinutes,
    lastActiveDate = lastActiveDate, joinedDate = joinedDate,
    growthPoints = growthPoints, velmorthMood = velmorthMood,
)

private fun User.toEntity() = UserEntity(
    id = id, name = name, avatarEmoji = avatarEmoji,
    currentStreak = currentStreak, longestStreak = longestStreak,
    totalXp = totalXp, leafBalance = leafBalance,
    isPremium = isPremium, selectedLanguageId = selectedLanguageId,
    dailyGoalMinutes = dailyGoalMinutes,
    lastActiveDate = lastActiveDate, joinedDate = joinedDate,
    growthPoints = growthPoints, velmorthMood = velmorthMood,
)
