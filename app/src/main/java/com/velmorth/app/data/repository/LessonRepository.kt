package com.velmorth.app.data.repository

import android.content.Context
import com.velmorth.app.data.local.LessonLoader
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.Lesson
import com.velmorth.app.data.model.Progress
import com.velmorth.app.data.model.CourseUnit

/**
 * Handles course units, lesson completions, progress calculation, and reviews.
 */
class LessonRepository(context: Context) {

    private val lessonLoader = LessonLoader(context)
    private val prefsManager = PrefsManager(context)

    /**
     * Retrieves all units and nested lessons.
     */
    fun getUnits(): List<CourseUnit> {
        val lang = prefsManager.selectedLanguage
        val fileName = "lessons/$lang.json"
        return lessonLoader.loadLessons(fileName)
    }

    /**
     * Finds a lesson by ID across all units.
     */
    fun getLessonById(lessonId: String): Lesson? {
        return getUnits().flatMap { it.lessons }.find { it.id == lessonId }
    }

    /**
     * Tracks that the user completed a specific lesson.
     */
    fun markLessonComplete(lessonId: String) {
        val completed = prefsManager.completedLessons.toMutableSet()
        completed.add(lessonId)
        prefsManager.completedLessons = completed
    }

    /**
     * Gets total completed lessons count.
     */
    fun getCompletedLessons(): Set<String> {
        return prefsManager.completedLessons
    }

    /**
     * Gets the Spaced Repetition / Review items list.
     */
    fun getReviewQueue(): List<Lesson> {
        val reviewIds = prefsManager.reviewQueue
        return getUnits().flatMap { it.lessons }.filter { it.id in reviewIds }
    }

    /**
     * Adds a lesson to the review/practice list.
     */
    fun addToReviewQueue(lessonId: String) {
        val reviews = prefsManager.reviewQueue.toMutableSet()
        reviews.add(lessonId)
        prefsManager.reviewQueue = reviews
    }

    /**
     * Removes a lesson from the review list after practice.
     */
    fun removeFromReviewQueue(lessonId: String) {
        val reviews = prefsManager.reviewQueue.toMutableSet()
        reviews.remove(lessonId)
        prefsManager.reviewQueue = reviews
    }

    /**
     * Fetches user curriculum progress.
     */
    fun getProgress(): Progress {
        val completed = prefsManager.completedLessons.toList()
        val queue = prefsManager.reviewQueue.toList()
        
        // Find current Unit and Lesson based on completed list or set default
        val allUnits = getUnits()
        var currentUnitId = "ja_u01_greetings"
        var currentLessonId = "ja_u01_l01_hello_basic"

        if (allUnits.isNotEmpty()) {
            outer@ for (unit in allUnits) {
                for (lesson in unit.lessons) {
                    if (lesson.id !in completed) {
                        currentUnitId = unit.unitId
                        currentLessonId = lesson.id
                        break@outer
                    }
                }
            }
        }

        return Progress(
            userId = "local_user",
            language = prefsManager.selectedLanguage,
            currentUnit = currentUnitId,
            currentLesson = currentLessonId,
            completedLessons = completed,
            reviewQueue = queue
        )
    }
}
