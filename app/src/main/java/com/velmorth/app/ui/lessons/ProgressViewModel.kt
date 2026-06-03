package com.velmorth.app.ui.lessons

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Immutable snapshot of the learner's lesson progress.
 *
 * @property completedLessons   Set of lesson IDs the user has finished.
 * @property dailyXpEarned      XP earned since midnight local time (resets each day).
 * @property dailyGoal          The user's daily XP target (from PrefsManager).
 * @property reviewQueueSize    Number of lessons queued for SRS review.
 * @property totalLessonsCount  Total number of lessons available (populated lazily).
 */
data class ProgressState(
    val completedLessons: Set<String>   = emptySet(),
    val dailyXpEarned: Int              = 0,
    val dailyGoal: Int                  = 10,
    val reviewQueueSize: Int            = 0,
    val totalLessonsCount: Int          = 0
) {
    /** Completion ratio for progress bars (0.0–1.0). */
    val dailyGoalProgress: Float
        get() = if (dailyGoal > 0) (dailyXpEarned.toFloat() / dailyGoal).coerceIn(0f, 1f)
                else 0f

    /** True if today's daily goal has been met. */
    val isDailyGoalComplete: Boolean
        get() = dailyXpEarned >= dailyGoal

    /** Convenience count for completed lessons. */
    val completedCount: Int
        get() = completedLessons.size
}

/**
 * Reactive lesson progress state provider.
 *
 * Flutter equivalent: `ProgressProvider` (ChangeNotifier in provider package).
 *
 * Responsibilities:
 *  - Track which lessons are completed (from [PrefsManager] + Firestore)
 *  - Track daily XP earned (resets at midnight, stored in SharedPrefs)
 *  - Track the review queue size
 *  - Expose [markLessonComplete] to update state after a lesson session
 *
 * Consumers:
 *  - Home screen (daily XP progress bar, streak collect button)
 *  - Lessons screen (lesson cards — locked/completed/available states)
 *  - Review screen (review queue count badge)
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val prefs: PrefsManager
) : ViewModel() {

    private val _progressState = MutableStateFlow(loadProgressFromPrefs())

    /**
     * Hot stream of the current [ProgressState].
     *
     * Flutter equivalent:
     * ```dart
     * Consumer<ProgressProvider>(
     *   builder: (ctx, progress, _) =>
     *     LinearProgressIndicator(value: progress.dailyGoalProgress),
     * )
     * ```
     * Kotlin equivalent:
     * ```kotlin
     * val progress by viewModel.progressState.collectAsState()
     * LinearProgressIndicator(progress = progress.dailyGoalProgress)
     * ```
     */
    val progressState: StateFlow<ProgressState> = _progressState.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Marks a lesson as complete, awards XP locally and syncs to Firestore.
     *
     * Flutter equivalent:
     * ```dart
     * context.read<ProgressProvider>().markLessonComplete(lessonId, xpEarned: 10);
     * ```
     */
    fun markLessonComplete(lessonId: String, xpEarned: Int = 10) {
        val current = _progressState.value

        // Skip if already completed (idempotent)
        if (lessonId in current.completedLessons) {
            Log.d("ProgressViewModel", "Lesson $lessonId already completed — skipping")
            return
        }

        val newCompleted  = current.completedLessons + lessonId
        val newDailyXp    = current.dailyXpEarned + xpEarned

        // Persist locally
        prefs.completedLessons = newCompleted
        saveDailyXp(newDailyXp)

        // Optimistic UI update
        _progressState.update { it.copy(
            completedLessons = newCompleted,
            dailyXpEarned    = newDailyXp
        )}

        Log.d("ProgressViewModel", "Lesson $lessonId completed. Daily XP: $newDailyXp/${current.dailyGoal}")

        // Background Firestore sync
        viewModelScope.launch(Dispatchers.IO) {
            FirestoreProgressRepository.syncLessonComplete(
                lessonId  = lessonId,
                xpEarned  = xpEarned
            )
        }
    }

    /**
     * Adds XP to the daily total without marking a lesson complete.
     * Useful for quiz bonus XP, review session XP, etc.
     */
    fun addDailyXp(amount: Int) {
        val newDailyXp = _progressState.value.dailyXpEarned + amount
        saveDailyXp(newDailyXp)
        _progressState.update { it.copy(dailyXpEarned = newDailyXp) }
    }

    /**
     * Updates the daily goal (called when user changes it in Settings).
     */
    fun setDailyGoal(newGoal: Int) {
        prefs.dailyGoal = newGoal
        _progressState.update { it.copy(dailyGoal = newGoal) }
    }

    /**
     * Updates the total lesson count (called by LessonsViewModel once lesson list is loaded).
     */
    fun setTotalLessons(count: Int) {
        _progressState.update { it.copy(totalLessonsCount = count) }
    }

    /**
     * Updates the review queue size (called when review screen is loaded).
     */
    fun setReviewQueueSize(size: Int) {
        prefs.reviewQueue = prefs.reviewQueue // keep existing
        _progressState.update { it.copy(reviewQueueSize = size) }
    }

    /**
     * Force-refreshes progress from PrefsManager (e.g., after signing in on a new device).
     */
    fun refresh() {
        _progressState.value = loadProgressFromPrefs()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Loads initial progress from SharedPreferences. */
    private fun loadProgressFromPrefs(): ProgressState {
        // Reset daily XP if the calendar date has rolled over
        val today    = LocalDate.now().toString()
        val savedDay = prefs.dailyXpDate
        val dailyXp  = if (savedDay == today) prefs.dailyXpEarned else 0

        if (savedDay != today) {
            prefs.dailyXpEarned = 0
            prefs.dailyXpDate   = today
        }

        return ProgressState(
            completedLessons = prefs.completedLessons,
            dailyXpEarned    = dailyXp,
            dailyGoal        = prefs.dailyGoal,
            reviewQueueSize  = prefs.reviewQueue.size
        )
    }

    private fun saveDailyXp(value: Int) {
        prefs.dailyXpEarned = value
        prefs.dailyXpDate   = LocalDate.now().toString()
    }
}
