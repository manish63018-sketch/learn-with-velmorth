package com.velmorth.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles all Firestore sync operations for user progress and stats.
 * Every method is safe to call offline — failures are caught and logged gracefully.
 */
object FirestoreProgressRepository {

    private const val TAG = "FirestoreProgress"
    private const val USERS = "users"
    private const val PROGRESS = "progress"
    private const val SRS = "srs"

    private fun getDb(): FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        Log.w(TAG, "Firestore not available: ${e.message}")
        null
    }

    private fun currentUid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Exception) {
        null
    }

    /**
     * Writes lesson completion to Firestore: users/{uid}/progress/{lessonId}
     * Fields: status, score, xp_earned, completed_at
     */
    fun syncLessonComplete(lessonId: String, xpEarned: Int, score: Int = 100) {
        val uid = currentUid() ?: run {
            Log.d(TAG, "No Firebase user — skipping progress sync")
            return
        }
        val db = getDb() ?: return

        val progressData = mapOf(
            "lesson_id"    to lessonId,
            "status"       to "completed",
            "score"        to score,
            "xp_earned"    to xpEarned,
            "completed_at" to FieldValue.serverTimestamp()
        )

        db.collection(USERS).document(uid)
            .collection(PROGRESS).document(lessonId)
            .set(progressData)
            .addOnSuccessListener {
                Log.d(TAG, "Progress synced for lesson $lessonId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Progress sync failed for $lessonId: ${e.message}")
            }
    }

    /**
     * Updates top-level user stats in Firestore: users/{uid}
     * Fields: xp, streak, leafBalance
     */
    fun syncUserStats(xp: Int, streak: Int, leafBalance: Int) {
        val uid = currentUid() ?: return
        val db = getDb() ?: return

        val updates = mapOf(
            "xp"          to xp,
            "streak"      to streak,
            "leafBalance" to leafBalance,
            "lastActive"  to FieldValue.serverTimestamp()
        )

        db.collection(USERS).document(uid)
            .update(updates)
            .addOnFailureListener { e ->
                Log.w(TAG, "Stats sync failed: ${e.message}")
            }
    }

    /**
     * Seeds initial SRS cards for a lesson's vocab into Firestore: users/{uid}/srs/{vocabId}
     * Only writes if the document doesn't already exist.
     */
    fun seedSRSCards(lessonId: String, vocabIds: List<String>) {
        val uid = currentUid() ?: return
        val db = getDb() ?: return
        val tomorrow = Timestamp(System.currentTimeMillis() / 1000 + 86400, 0)

        val batch = db.batch()
        vocabIds.forEach { vocabId ->
            val docRef = db.collection(USERS).document(uid)
                .collection(SRS).document(vocabId)
            val cardData = mapOf(
                "vocab_id"         to vocabId,
                "lesson_id"        to lessonId,
                "repetitions"      to 0,
                "ease_factor"      to 2.5,
                "interval_days"    to 1,
                "last_reviewed"    to FieldValue.serverTimestamp(),
                "next_review_date" to tomorrow,
                "status"           to "new"
            )
            // setOptions merge = true so existing cards aren't overwritten
            batch.set(docRef, cardData, com.google.firebase.firestore.SetOptions.merge())
        }

        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "SRS seeded ${vocabIds.size} cards for $lessonId") }
            .addOnFailureListener { e -> Log.w(TAG, "SRS seed failed: ${e.message}") }
    }
}
