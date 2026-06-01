package com.velmorth.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents a learner profile in Learn With Velmorth, compatible with Firebase Firestore.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val isPremium: Boolean = false,
    val streak: Int = 0,
    val leafBalance: Int = 0,
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val createdAt: Timestamp? = null,

    // Core learning fields required by local offline database and UI
    val selectedLanguage: String = "japanese",
    val activeLanguageId: String = "japanese",
    val nativeLanguage: String = "english",
    val xp: Int = 0,
    val level: Int = 1,

    // Streak check-in date (ISO "yyyy-MM-dd") persisted in Firestore
    val lastCheckinDate: String = ""
) {
    // Compatibility properties to prevent breaking existing UI code
    @get:Exclude
    val id: String get() = uid

    @get:Exclude
    val displayName: String get() = name

    @get:Exclude
    val leaves: Int get() = leafBalance

    @get:Exclude
    val joinedAt: String
        get() {
            val date = createdAt?.toDate() ?: Date()
            val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            return sdf.format(date)
        }
}
