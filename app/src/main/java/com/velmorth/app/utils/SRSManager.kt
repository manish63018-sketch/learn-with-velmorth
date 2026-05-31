package com.velmorth.app.utils

/**
 * SM-2 Spaced Repetition Algorithm implementation.
 *
 * Rating scale (same as SuperMemo):
 *  5 = Perfect response (Easy)
 *  4 = Correct with hesitation (Good)
 *  3 = Correct with difficulty (Hard)
 *  0–2 = Incorrect (Again — resets interval)
 */
object SRSManager {

    private const val MIN_EASE_FACTOR = 1.3

    /**
     * Immutable SRS card state. Stored per-vocab in Firestore.
     */
    data class SRSCard(
        val vocabId       : String  = "",
        val lessonId      : String  = "",
        val repetitions   : Int     = 0,
        val easeFactor    : Double  = 2.5,
        val intervalDays  : Int     = 1,
        val nextReviewDate: Long    = System.currentTimeMillis() + 86_400_000L, // tomorrow
        val status        : String  = "new"  // "new" | "learning" | "review" | "mastered"
    )

    /**
     * Calculates the new SRS state after a user rates a card.
     *
     * @param card   Current card state
     * @param rating User's self-rating: 0–5
     * @return Updated card with new interval, ease factor, and next review timestamp
     */
    fun calculateNextReview(card: SRSCard, rating: Int): SRSCard {
        val clampedRating = rating.coerceIn(0, 5)

        return if (clampedRating < 3) {
            // Incorrect — restart from beginning
            card.copy(
                repetitions    = 0,
                intervalDays   = 1,
                easeFactor     = (card.easeFactor - 0.2).coerceAtLeast(MIN_EASE_FACTOR),
                nextReviewDate = System.currentTimeMillis() + 86_400_000L,
                status         = "learning"
            )
        } else {
            // Correct — advance interval
            val newRepetitions = card.repetitions + 1
            val newInterval = when (newRepetitions) {
                1    -> 1
                2    -> 6
                else -> (card.intervalDays * card.easeFactor).toInt().coerceAtLeast(1)
            }
            // Update ease factor: EF := EF + (0.1 – (5–rating) * (0.08 + (5–rating) * 0.02))
            val delta = 0.1 - (5 - clampedRating) * (0.08 + (5 - clampedRating) * 0.02)
            val newEaseFactor = (card.easeFactor + delta).coerceAtLeast(MIN_EASE_FACTOR)
            val newStatus = if (newInterval >= 21) "mastered" else "review"
            val nextReview = System.currentTimeMillis() + newInterval * 86_400_000L

            card.copy(
                repetitions    = newRepetitions,
                intervalDays   = newInterval,
                easeFactor     = newEaseFactor,
                nextReviewDate = nextReview,
                status         = newStatus
            )
        }
    }

    /** Returns true if this card is due for review today or earlier. */
    fun isDueToday(card: SRSCard): Boolean =
        card.nextReviewDate <= System.currentTimeMillis()

    /**
     * Converts a 0–5 SM-2 rating to a human label shown on review buttons.
     */
    fun ratingLabel(rating: Int): String = when (rating) {
        5    -> "Easy ⚡"
        4    -> "Good ✅"
        3    -> "Hard 😅"
        else -> "Again 🔄"
    }
}
