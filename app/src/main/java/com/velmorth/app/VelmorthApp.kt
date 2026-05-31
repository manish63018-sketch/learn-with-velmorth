package com.velmorth.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.utils.AnalyticsManager
import com.velmorth.app.utils.NotificationScheduler

/**
 * Main Application class for Learn With Velmorth.
 * Initialises Firebase Crashlytics, Analytics, and schedules daily reminder if enabled.
 */
class VelmorthApp : Application() {

    /** App-wide single PrefsManager instance — avoids creating multiple per screen. */
    lateinit var prefs: PrefsManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsManager(this)

        // ── Firebase initialisation ────────────────────────────────────────────
        try {
            FirebaseApp.initializeApp(this)

            // Crashlytics — disabled in debug for speed
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
}
