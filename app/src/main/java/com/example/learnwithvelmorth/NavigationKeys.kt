package com.example.learnwithvelmorth

import androidx.navigation3.runtime.NavKey

// ======================================================
// Navigation Keys — one per screen
// ======================================================

data object Splash : NavKey
data object Onboarding : NavKey
data object Home : NavKey
data object Lessons : NavKey
data class LessonPlayer(val lessonId: String) : NavKey
data object ReviewGarden : NavKey
data class Quiz(val lessonId: String) : NavKey
data object AISpeaker : NavKey
data object LeafShop : NavKey
data object Premium : NavKey
data object Profile : NavKey
data object Settings : NavKey

// Legacy — remove after migrating
data object Main : NavKey
