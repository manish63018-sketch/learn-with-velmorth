package com.example.learnwithvelmorth

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// ======================================================
// Navigation Keys — one per screen
// ======================================================

@Serializable
data object Splash : NavKey

@Serializable
data object Onboarding : NavKey

@Serializable
data object Home : NavKey

@Serializable
data object Lessons : NavKey

@Serializable
data class LessonPlayer(val lessonId: String) : NavKey

@Serializable
data object ReviewGarden : NavKey

@Serializable
data class Quiz(val lessonId: String) : NavKey

@Serializable
data object AISpeaker : NavKey

@Serializable
data object LeafShop : NavKey

@Serializable
data object Premium : NavKey

@Serializable
data object Profile : NavKey

@Serializable
data object Settings : NavKey

// Legacy — remove after migrating
@Serializable
data object Main : NavKey

