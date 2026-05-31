package com.velmorth.app.data.model

/**
 * Tracks the user's progress through a specific language path.
 */
data class Progress(
    val userId: String,
    val language: String,
    val currentUnit: String,
    val currentLesson: String,
    val completedLessons: List<String>,
    val reviewQueue: List<String>
)
