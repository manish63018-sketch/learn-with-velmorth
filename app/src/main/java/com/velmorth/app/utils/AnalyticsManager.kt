package com.velmorth.app.utils

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Typed wrapper around FirebaseAnalytics for all Velmorth app events.
 * Call [init] from VelmorthApp before any logging.
 */
object AnalyticsManager {

    private const val TAG = "Analytics"
    private var analytics: FirebaseAnalytics? = null

    /** Call once from Application.onCreate() */
    fun init(fa: FirebaseAnalytics) {
        analytics = fa
        Log.d(TAG, "FirebaseAnalytics initialised")
    }

    /** Fired when a user completes a lesson. */
    fun logLessonComplete(lessonId: String, xpEarned: Int, leavesEarned: Int) {
        log("lesson_complete") {
            putString("lesson_id",     lessonId)
            putInt("xp_earned",        xpEarned)
            putInt("leaves_earned",    leavesEarned)
        }
    }

    /** Fired on successful login. method = "email" | "google" | "mock" */
    fun logLogin(method: String) {
        log(FirebaseAnalytics.Event.LOGIN) {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /** Fired on successful account creation. */
    fun logSignup(method: String) {
        log(FirebaseAnalytics.Event.SIGN_UP) {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /** Fired when user purchases a Leaf shop item. */
    fun logShopPurchase(itemId: String, leafCost: Int) {
        log("shop_purchase") {
            putString("item_id",   itemId)
            putInt("leaf_cost",    leafCost)
        }
    }

    /** Fired when the user's streak changes. */
    fun logStreakUpdated(streakDays: Int) {
        log("streak_updated") {
            putInt("streak_days", streakDays)
        }
    }

    /** Fired when an SRS card is reviewed. */
    fun logSRSReview(vocabId: String, rating: Int, status: String) {
        log("srs_review") {
            putString("vocab_id", vocabId)
            putInt("rating",      rating)
            putString("status",   status)
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun log(event: String, block: Bundle.() -> Unit) {
        val fa = analytics ?: run {
            Log.d(TAG, "Analytics not initialised — skipping event: $event")
            return
        }
        val bundle = Bundle().apply(block)
        fa.logEvent(event, bundle)
    }
}
