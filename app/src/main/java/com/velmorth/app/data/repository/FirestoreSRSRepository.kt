package com.velmorth.app.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.velmorth.app.utils.SRSManager.SRSCard

/**
 * Reads and writes SRS card data from Firestore: users/{uid}/srs/{vocabId}
 * All methods are fire-and-forget with silent failure logging.
 */
object FirestoreSRSRepository {

    private const val TAG = "FirestoreSRS"

    private fun getDb(): FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }

    private fun currentUid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid
    } catch (e: Exception) {
        null
    }

    /**
     * Fetches all SRS cards that are due today for the current user.
     * Calls [onResult] with the list (empty if offline or no cards found).
     */
    fun getDueCards(onResult: (List<SRSCard>) -> Unit) {
        val uid = currentUid() ?: run { onResult(emptyList()); return }
        val db  = getDb()     ?: run { onResult(emptyList()); return }
        val now = System.currentTimeMillis()

        db.collection("users").document(uid)
            .collection("srs")
            .whereLessThanOrEqualTo("next_review_date", com.google.firebase.Timestamp(now / 1000, 0))
            .get()
            .addOnSuccessListener { snapshot ->
                val cards = snapshot.documents.mapNotNull { doc ->
                    try {
                        SRSCard(
                            vocabId        = doc.getString("vocab_id")       ?: doc.id,
                            lessonId       = doc.getString("lesson_id")      ?: "",
                            repetitions    = (doc.getLong("repetitions")     ?: 0L).toInt(),
                            easeFactor     = doc.getDouble("ease_factor")    ?: 2.5,
                            intervalDays   = (doc.getLong("interval_days")   ?: 1L).toInt(),
                            nextReviewDate = (doc.getTimestamp("next_review_date")?.seconds ?: (now / 1000)) * 1000L,
                            status         = doc.getString("status")         ?: "new"
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Error parsing SRS card ${doc.id}: ${e.message}")
                        null
                    }
                }
                Log.d(TAG, "Fetched ${cards.size} due SRS cards")
                onResult(cards)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to fetch SRS cards: ${e.message}")
                onResult(emptyList())
            }
    }

    /**
     * Updates a single SRS card after the user rates it.
     * Writes: repetitions, ease_factor, interval_days, next_review_date, status
     */
    fun updateCard(card: SRSCard) {
        val uid = currentUid() ?: return
        val db  = getDb()      ?: return

        val data = mapOf(
            "repetitions"      to card.repetitions,
            "ease_factor"      to card.easeFactor,
            "interval_days"    to card.intervalDays,
            "next_review_date" to com.google.firebase.Timestamp(card.nextReviewDate / 1000, 0),
            "status"           to card.status
        )

        db.collection("users").document(uid)
            .collection("srs").document(card.vocabId)
            .update(data)
            .addOnFailureListener { e ->
                Log.w(TAG, "SRS update failed for ${card.vocabId}: ${e.message}")
            }
    }
}
