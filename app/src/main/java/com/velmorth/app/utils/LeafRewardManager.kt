package com.velmorth.app.utils

import android.content.Context
import com.velmorth.app.data.local.PrefsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized leaf reward system for Learn with Velmorth.
 *
 * Reward rules:
 *  • Daily login       → +5 🍃 (once per calendar day)
 *  • Lesson complete   → +5 🍃 (base)
 *  • Perfect quiz      → +3 🍃 bonus (0 wrong answers during the lesson)
 *  • 7-day streak      → +20 🍃 bonus (once per 7-day block)
 */
object LeafRewardManager {

    private const val DAILY_LOGIN_REWARD    = 5
    const val LESSON_COMPLETE_REWARD        = 5
    const val PERFECT_QUIZ_BONUS            = 3
    private const val WEEKLY_STREAK_BONUS   = 20
    private const val STREAK_BONUS_INTERVAL = 7

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // ── Daily Login ───────────────────────────────────────────────────────────

    /**
     * Call when the app becomes active (MainActivity.onResume / HomeFragment.onResume).
     * Returns the leaves awarded (0 if already claimed today).
     */
    fun claimDailyLoginReward(prefs: PrefsManager): Int {
        val today = dateFormat.format(Date())
        if (prefs.lastLoginDate == today) return 0

        prefs.lastLoginDate = today
        prefs.leaves = prefs.leaves + DAILY_LOGIN_REWARD
        return DAILY_LOGIN_REWARD
    }

    // ── Lesson Complete ───────────────────────────────────────────────────────

    /**
     * Call from LessonPlayerActivity.onComplete.
     *
     * @param isPerfect  true when the user had zero wrong answers in the quiz
     * @return total leaves to add (base + any bonuses)
     */
    fun lessonCompleteReward(prefs: PrefsManager, isPerfect: Boolean): Int {
        var total = LESSON_COMPLETE_REWARD
        if (isPerfect) total += PERFECT_QUIZ_BONUS
        prefs.leaves = prefs.leaves + total
        return total
    }

    // ── 7-Day Streak Bonus ────────────────────────────────────────────────────

    /**
     * Call after updating the streak in LessonPlayerActivity.
     * Awards +20 leaves every 7-day milestone (7, 14, 21 …).
     * Tracks via [PrefsManager.weeklyStreakBonusGiven] to avoid double-awarding.
     *
     * @return 20 if the bonus was awarded, 0 otherwise.
     */
    fun checkStreakBonus(prefs: PrefsManager, newStreak: Int): Int {
        if (newStreak > 0 && newStreak % STREAK_BONUS_INTERVAL == 0) {
            // Only award once per milestone (reset flag when streak changes block)
            if (!prefs.weeklyStreakBonusGiven) {
                prefs.weeklyStreakBonusGiven = true
                prefs.leaves = prefs.leaves + WEEKLY_STREAK_BONUS
                return WEEKLY_STREAK_BONUS
            }
        } else {
            // Not on a 7-day boundary → reset flag so next milestone can fire
            prefs.weeklyStreakBonusGiven = false
        }
        return 0
    }
}
