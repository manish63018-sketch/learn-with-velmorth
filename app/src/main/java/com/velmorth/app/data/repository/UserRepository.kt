package com.velmorth.app.data.repository

import android.content.Context
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository in charge of loading, saving, and updating the local [User] state.
 */
class UserRepository(context: Context) {

    private val prefsManager = PrefsManager(context)

    /**
     * Obtains the current active user state.
     */
    fun getUser(): User {
        val email = prefsManager.userEmail
        val name = prefsManager.userName
        
        // If name is empty, it means we have a guest or uninitialized profile
        val displayName = if (name.isEmpty()) "Learner" else name
        val userEmail = if (email.isEmpty()) "guest@velmorth.com" else email
        val userUsername = prefsManager.username.ifEmpty { displayName.lowercase().replace(" ", "") }

        return User(
            uid = "local_user",
            name = displayName,
            username = userUsername,
            email = userEmail,
            photoUrl = "",
            isPremium = prefsManager.isPremium,
            streak = prefsManager.streak,
            leafBalance = prefsManager.leaves,
            darkMode = prefsManager.darkMode,
            notificationsEnabled = prefsManager.notificationsEnabled,
            createdAt = null,
            selectedLanguage = prefsManager.selectedLanguage,
            nativeLanguage = prefsManager.nativeLanguage,
            xp = prefsManager.xp,
            level = prefsManager.level
        )
    }

    /**
     * Persists the given [User] updates back to SharedPreferences.
     */
    fun saveUser(user: User) {
        prefsManager.userName = user.name
        prefsManager.username = user.username
        prefsManager.userEmail = user.email
        prefsManager.selectedLanguage = user.selectedLanguage
        prefsManager.nativeLanguage = user.nativeLanguage
        prefsManager.xp = user.xp
        prefsManager.streak = user.streak
        prefsManager.level = user.level
        prefsManager.leaves = user.leafBalance
        prefsManager.isPremium = user.isPremium
        prefsManager.darkMode = user.darkMode
        prefsManager.notificationsEnabled = user.notificationsEnabled
    }

    /**
     * Directly updates user stats.
     */
    fun updateStats(xp: Int, streak: Int, level: Int, leaves: Int) {
        prefsManager.xp = xp
        prefsManager.streak = streak
        prefsManager.level = level
        prefsManager.leaves = leaves
    }

    /**
     * Directly updates profile details.
     */
    fun updateProfile(displayName: String, nativeLanguage: String) {
        prefsManager.userName = displayName
        prefsManager.nativeLanguage = nativeLanguage
    }

    /**
     * Set onboarding state
     */
    fun setOnboarded(onboarded: Boolean) {
        prefsManager.isOnboarded = onboarded
    }

    /**
     * Check onboarding state
     */
    fun isOnboarded(): Boolean {
        return prefsManager.isOnboarded
    }

    private fun getOrCreateJoinedDate(): String {
        // If we want a dynamic joined date, we can persist a join date or return current format
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }
}
