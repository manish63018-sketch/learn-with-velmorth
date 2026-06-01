package com.velmorth.app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.utils.AnalyticsManager
import com.velmorth.app.utils.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp

/**
 * Main Application class for Learn With Velmorth.
 * Initialises Firebase, applies theme mode, and schedules daily reminders.
 */
@HiltAndroidApp
class VelmorthApp : Application() {

    /** App-wide single PrefsManager instance — avoids creating multiple per screen. */
    lateinit var prefs: PrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)

        // ── Apply saved theme mode at startup ─────────────────────────────────
        applyThemeMode(prefs.themeMode)

        // ── Firebase initialisation ────────────────────────────────────────────
        try {
            FirebaseApp.initializeApp(this)

            // Crashlytics — disabled in debug builds for speed
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                log("VelmorthApp started – Crashlytics active")
            }

            // Analytics
            val fa = FirebaseAnalytics.getInstance(this)
            AnalyticsManager.init(fa)

            Log.i("VelmorthApp", "Firebase initialised successfully.")
        } catch (e: Exception) {
            Log.w("VelmorthApp", "Firebase initialisation skipped: ${e.message}")
        }

        // ── Daily reminder scheduling ─────────────────────────────────────────
        if (prefs.notificationsEnabled) {
            try {
                NotificationScheduler.scheduleDailyReminder(
                    context = this,
                    hour    = prefs.reminderHour,
                    minute  = prefs.reminderMinute
                )
            } catch (e: Exception) {
                Log.w("VelmorthApp", "Failed to schedule reminder: ${e.message}")
            }
        }
    }

    /**
     * Applies the global night mode based on the user's saved preference.
     * "dark"   → AppCompatDelegate.MODE_NIGHT_YES
     * "light"  → AppCompatDelegate.MODE_NIGHT_NO
     * "system" → AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
     */
    fun applyThemeMode(mode: String) {
        val nightMode = when (mode) {
            "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
