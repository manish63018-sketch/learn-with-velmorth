package com.velmorth.app.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages the local persistence of all user configurations, statistics, and preferences.
 */
class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "velmorth_prefs"

        // Keys
        private const val KEY_USER_NAME            = "USER_NAME"
        private const val KEY_USERNAME             = "USERNAME"
        private const val KEY_USER_EMAIL           = "USER_EMAIL"
        private const val KEY_SELECTED_LANGUAGE    = "SELECTED_LANGUAGE"
        private const val KEY_NATIVE_LANGUAGE      = "NATIVE_LANGUAGE"
        private const val KEY_XP                   = "XP"
        private const val KEY_STREAK               = "STREAK"
        private const val KEY_LEVEL                = "LEVEL"
        private const val KEY_LEAVES               = "LEAVES"
        private const val KEY_IS_PREMIUM           = "IS_PREMIUM"
        private const val KEY_IS_ONBOARDED         = "IS_ONBOARDED"
        private const val KEY_DAILY_GOAL           = "DAILY_GOAL"
        private const val KEY_DARK_MODE            = "DARK_MODE"
        private const val KEY_THEME_MODE           = "THEME_MODE"
        private const val KEY_NOTIFICATIONS_ENABLED= "NOTIFICATIONS_ENABLED"
        private const val KEY_REMINDER_HOUR        = "REMINDER_HOUR"
        private const val KEY_REMINDER_MINUTE      = "REMINDER_MINUTE"
        private const val KEY_STREAK_ALERT         = "STREAK_ALERT"
        private const val KEY_PROMO_ALERTS         = "PROMO_ALERTS"
        private const val KEY_TEXT_SIZE            = "TEXT_SIZE"

        // Extended items for lesson/review state persistence
        private const val KEY_COMPLETED_LESSONS    = "COMPLETED_LESSONS"
        private const val KEY_REVIEW_QUEUE         = "REVIEW_QUEUE"
        private const val KEY_OWNED_SHOP_ITEMS     = "OWNED_SHOP_ITEMS"
        private const val KEY_LAST_LOGIN_DATE      = "LAST_LOGIN_DATE"
        private const val KEY_WEEKLY_STREAK_BONUS  = "WEEKLY_STREAK_BONUS"
        private const val KEY_FIRST_LAUNCH         = "FIRST_LAUNCH"
        private const val KEY_PHOTO_URL            = "PHOTO_URL"
        private const val KEY_LAST_CHECKIN_DATE    = "LAST_CHECKIN_DATE"
        private const val KEY_UID                  = "FIREBASE_UID"

        // Daily XP tracking for ProgressViewModel (Flutter: ProgressProvider)
        private const val KEY_DAILY_XP_EARNED      = "DAILY_XP_EARNED"
        private const val KEY_DAILY_XP_DATE        = "DAILY_XP_DATE"
    }

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var selectedLanguage: String
        get() = prefs.getString(KEY_SELECTED_LANGUAGE, "japanese") ?: "japanese"
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value).apply()

    var nativeLanguage: String
        get() = prefs.getString(KEY_NATIVE_LANGUAGE, "english") ?: "english"
        set(value) = prefs.edit().putString(KEY_NATIVE_LANGUAGE, value).apply()

    var xp: Int
        get() = prefs.getInt(KEY_XP, 0)
        set(value) = prefs.edit().putInt(KEY_XP, value).apply()

    var streak: Int
        get() = prefs.getInt(KEY_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_STREAK, value).apply()

    var level: Int
        get() = prefs.getInt(KEY_LEVEL, 1)
        set(value) = prefs.edit().putInt(KEY_LEVEL, value).apply()

    var leaves: Int
        get() = prefs.getInt(KEY_LEAVES, 50)
        set(value) = prefs.edit().putInt(KEY_LEAVES, value).apply()

    var isPremium: Boolean
        get() = prefs.getBoolean(KEY_IS_PREMIUM, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PREMIUM, value).apply()

    var isOnboarded: Boolean
        get() = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ONBOARDED, value).apply()

    var dailyGoal: Int // In XP or minutes. Let's make it standard daily XP target (e.g. 10 XP default)
        get() = prefs.getInt(KEY_DAILY_GOAL, 10)
        set(value) = prefs.edit().putInt(KEY_DAILY_GOAL, value).apply()

    var darkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    /** "light" | "dark" | "system" */
    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 9)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    var streakAlertEnabled: Boolean
        get() = prefs.getBoolean(KEY_STREAK_ALERT, true)
        set(value) = prefs.edit().putBoolean(KEY_STREAK_ALERT, value).apply()

    var promoAlertsEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROMO_ALERTS, false)
        set(value) = prefs.edit().putBoolean(KEY_PROMO_ALERTS, value).apply()

    var textSize: String
        get() = prefs.getString(KEY_TEXT_SIZE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_TEXT_SIZE, value).apply()

    // Completed Lessons set
    var completedLessons: Set<String>
        get() = prefs.getStringSet(KEY_COMPLETED_LESSONS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_COMPLETED_LESSONS, value).apply()

    // Review Queue list (lessons marked for review)
    var reviewQueue: Set<String>
        get() = prefs.getStringSet(KEY_REVIEW_QUEUE, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_REVIEW_QUEUE, value).apply()

    // Owned Shop Item IDs set
    var ownedShopItems: Set<String>
        get() = prefs.getStringSet(KEY_OWNED_SHOP_ITEMS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_OWNED_SHOP_ITEMS, value).apply()

    /** ISO date string "yyyy-MM-dd" of the last time the daily login bonus was granted */
    var lastLoginDate: String
        get() = prefs.getString(KEY_LAST_LOGIN_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_LOGIN_DATE, value).apply()

    /** True if the 7-day streak bonus has already been given for the current streak week */
    var weeklyStreakBonusGiven: Boolean
        get() = prefs.getBoolean(KEY_WEEKLY_STREAK_BONUS, false)
        set(value) = prefs.edit().putBoolean(KEY_WEEKLY_STREAK_BONUS, value).apply()

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var photoUrl: String
        get() = prefs.getString(KEY_PHOTO_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHOTO_URL, value).apply()

    /** ISO date string "yyyy-MM-dd" of the last streak check-in (used by the Home Collect button) */
    var lastCheckinDate: String
        get() = prefs.getString(KEY_LAST_CHECKIN_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_CHECKIN_DATE, value).apply()

    /** Firebase UID for the currently signed-in user. Empty string if not signed in. */
    var uid: String
        get() = prefs.getString(KEY_UID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_UID, value).apply()

    /** XP earned today (resets when [dailyXpDate] changes). */
    var dailyXpEarned: Int
        get() = prefs.getInt(KEY_DAILY_XP_EARNED, 0)
        set(value) = prefs.edit().putInt(KEY_DAILY_XP_EARNED, value).apply()

    /** ISO date string "yyyy-MM-dd" when [dailyXpEarned] was last written. */
    var dailyXpDate: String
        get() = prefs.getString(KEY_DAILY_XP_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DAILY_XP_DATE, value).apply()

    /**
     * Clears all user-related state (useful for Log Out).
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
