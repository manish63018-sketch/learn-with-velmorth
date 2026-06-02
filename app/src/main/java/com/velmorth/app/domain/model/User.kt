package com.velmorth.app.domain.model

/**
 * User Domain Model
 * Represents a user in the application
 *
 * @property id Unique user identifier (Firebase UID)
 * @property email User's email address
 * @property displayName User's display name
 * @property createdAt Timestamp when user was created
 * @property lastLogin Timestamp of last login
 * @property xpPoints Total XP earned
 * @property level User's current level
 * @property isPremium Whether user has premium subscription
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: Long,
    val lastLogin: Long = System.currentTimeMillis(),
    val xpPoints: Int = 0,
    val level: Int = 1,
    val isPremium: Boolean = false
)
