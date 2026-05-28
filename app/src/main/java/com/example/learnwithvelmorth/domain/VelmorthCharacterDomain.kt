package com.example.learnwithvelmorth.domain

/**
 * All possible emotional states Velmorth can display.
 * Each state drives a distinct animation + emoji set in the UI composable.
 */
enum class VelmorthEmotion {
    IDLE,      // gentle breathing / floating
    HAPPY,     // smiling, gentle bounce
    SAD,       // drooping eyes, slow sway
    EXCITED,   // fast bounce, arms up (simulated)
    THINKING,  // slight tilt, finger-to-chin vibe
    PROUD,     // upright, slow confident pulse
    TALKING;   // mouth-scale oscillation synced to TTS

    companion object {
        fun fromString(value: String): VelmorthEmotion =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: IDLE
    }
}

/**
 * All recognized trigger strings used across the app.
 * Pass these to VelmorthCharacterViewModel.fireEvent().
 */
object VelmorthTrigger {
    const val APP_OPEN              = "app_open"
    const val APP_OPEN_MORNING      = "app_open_morning"
    const val APP_OPEN_AFTERNOON    = "app_open_afternoon"
    const val APP_OPEN_EVENING      = "app_open_evening"
    const val APP_OPEN_NIGHT        = "app_open_night"
    const val APP_CLOSE             = "app_close"

    const val LESSON_START          = "lesson_start"
    const val LESSON_COMPLETE       = "lesson_complete"
    const val FIRST_LESSON          = "first_lesson"
    const val CHAPTER_COMPLETE      = "chapter_complete"

    const val CORRECT_ANSWER        = "correct_answer"
    const val WRONG_ANSWER          = "wrong_answer"
    const val MULTIPLE_WRONG        = "multiple_wrong_answers"
    const val PERFECT_SCORE         = "perfect_score"

    const val QUIZ_START            = "quiz_start"
    const val QUIZ_COMPLETE         = "quiz_complete"

    const val REVIEW_START          = "review_start"

    const val STREAK_MAINTAINED     = "streak_maintained"
    const val STREAK_BROKEN         = "streak_broken"

    const val LEAF_EARNED           = "leaf_earned"
    const val XP_EARNED             = "xp_earned"
    const val LEVEL_UP              = "level_up"
    const val DAILY_GOAL_DONE       = "daily_goal_done"

    const val HEARTS_LOW            = "hearts_low"
    const val HEARTS_REFILLED       = "hearts_refilled"
    const val HINT_USED             = "hint_used"

    const val SHOP_VISIT            = "shop_visit"
    const val PURCHASE_MADE         = "purchase_made"
    const val PREMIUM_PROMPT        = "premium_prompt"

    const val PROFILE_VISIT         = "profile_visit"
    const val SETTINGS_VISIT        = "settings_visit"
    const val AI_SPEAKER_START      = "ai_speaker_start"

    const val IDLE                  = "idle"
    const val MOTIVATION_RANDOM     = "motivation_random"

    /** Derive a time-of-day greeting trigger based on the current hour. */
    fun greetingForHour(hour: Int): String = when (hour) {
        in 5..11  -> APP_OPEN_MORNING
        in 12..17 -> APP_OPEN_AFTERNOON
        in 18..21 -> APP_OPEN_EVENING
        else       -> APP_OPEN_NIGHT
    }
}
