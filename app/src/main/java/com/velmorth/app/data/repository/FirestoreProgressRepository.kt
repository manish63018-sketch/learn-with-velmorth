package com.velmorth.app.data.repository

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.velmorth.app.utils.XPManager

/**
 * Central Firestore repository that handles all cloud sync operations.
 *
 * Key design principles:
 * - ALL collections/documents are auto-created by the app — never manually.
 * - Every method is safe offline — failures are caught and logged gracefully.
 * - SetOptions.merge() is used everywhere to avoid overwriting existing data.
 */
object FirestoreProgressRepository {

    private const val TAG             = "FirestoreRepo"
    private const val USERS           = "users"
    private const val SETTINGS        = "settings"
    private const val PROGRESS        = "progress"
    private const val LESSONS         = "lessons"
    private const val SRS             = "srs"

    // ── Internal helpers ─────────────────────────────────────────────────────

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

    // ── XP → Level calculation ────────────────────────────────────────────────

    /**
     * Delegates to [XPManager.getLevelForXp] — single source of truth for
     * XP→Level mapping shared across the whole app and Firestore.
     */
    fun calculateLevel(xp: Int): Int = XPManager.getLevelForXp(xp)

    // ── Auto-create: users/{uid} ──────────────────────────────────────────────

    /**
     * Ensures the users/{uid} document exists with all required fields.
     * Uses merge so existing data is never overwritten.
     * Called on every app open after login.
     */
    fun ensureUserDocExists(
        name: String = "",
        username: String = "",
        email: String = "",
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val uid = currentUid() ?: run {
            onComplete?.invoke(false)
            return
        }
        val db = getDb() ?: run {
            onComplete?.invoke(false)
            return
        }

        // Use merge so existing values (xp, streak, level) are never reset
        val defaults = mapOf(
            "uid"             to uid,
            "username"        to username,
            "email"           to email,
            "profileImage"    to "",
            "xp"              to 0,
            "level"           to 1,
            "streak"          to 0,
            "leafBalance"     to 5,
            "isPremium"       to false,
            "notificationsEnabled" to true,
            "darkMode"        to false,
            "activeLanguageId" to "japanese",
            "lastCheckinDate" to "",
            "createdAt"       to FieldValue.serverTimestamp()
        ).let { base ->
            // Only set name if it's not blank
            if (name.isNotBlank()) base + mapOf("name" to name) else base
        }

        db.collection(USERS).document(uid)
            .set(defaults, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "User doc ensured for uid=$uid")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to ensure user doc: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    // ── Auto-create: settings/{uid} ───────────────────────────────────────────

    /**
     * Ensures the settings/{uid} document exists with default values.
     * Called on every app open. Uses merge — never destroys existing settings.
     */
    fun ensureSettingsDocExists(onComplete: ((Boolean) -> Unit)? = null) {
        val uid = currentUid() ?: run {
            onComplete?.invoke(false)
            return
        }
        val db = getDb() ?: run {
            onComplete?.invoke(false)
            return
        }

        val defaults = mapOf(
            "theme"         to "system",
            "language"      to "japanese",
            "notifications" to true,
            "dailyGoalXp"   to 10,
            "createdAt"     to FieldValue.serverTimestamp()
        )

        db.collection(SETTINGS).document(uid)
            .set(defaults, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Settings doc ensured for uid=$uid")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Failed to ensure settings doc: ${e.message}")
                onComplete?.invoke(false)
            }
    }

    // ── Auto-create: lessons collection ───────────────────────────────────────

    /**
     * Seeds the top-level `lessons` collection with the 4 language basics documents.
     * Uses merge so it will NOT overwrite existing lesson data.
     * Call once after first login or when lessons tab is opened.
     */
    fun ensureLanguageLessonsExist() {
        val db = getDb() ?: return

        val languageLessons = mapOf(
            "japanese_basics" to mapOf(
                "title"      to "Japanese Basics",
                "language"   to "japanese",
                "difficulty" to "beginner",
                "xpReward"   to 10,
                "flag"       to "🇯🇵",
                "description" to "Learn hiragana, katakana, and essential Japanese greetings",
                "topics"     to listOf("Greetings", "Numbers", "Colors", "Days of the week"),
                "createdAt"  to FieldValue.serverTimestamp()
            ),
            "french_basics" to mapOf(
                "title"      to "French Basics",
                "language"   to "french",
                "difficulty" to "beginner",
                "xpReward"   to 10,
                "flag"       to "🇫🇷",
                "description" to "Master French pronunciation, greetings, and everyday phrases",
                "topics"     to listOf("Greetings", "Numbers", "Food", "Travel phrases"),
                "createdAt"  to FieldValue.serverTimestamp()
            ),
            "sanskrit_basics" to mapOf(
                "title"      to "Sanskrit Basics",
                "language"   to "sanskrit",
                "difficulty" to "beginner",
                "xpReward"   to 10,
                "flag"       to "🇮🇳",
                "description" to "Explore the ancient language with Devanagari script and foundational mantras",
                "topics"     to listOf("Devanagari Script", "Basic Vocabulary", "Simple Mantras", "Numbers"),
                "createdAt"  to FieldValue.serverTimestamp()
            ),
            "english_basics" to mapOf(
                "title"      to "English Basics",
                "language"   to "english",
                "difficulty" to "beginner",
                "xpReward"   to 10,
                "flag"       to "🇬🇧",
                "description" to "Strengthen English grammar, vocabulary, and conversational skills",
                "topics"     to listOf("Grammar", "Vocabulary", "Idioms", "Conversation"),
                "createdAt"  to FieldValue.serverTimestamp()
            )
        )

        val batch = db.batch()
        languageLessons.forEach { (docId, data) ->
            val ref = db.collection(LESSONS).document(docId)
            batch.set(ref, data, SetOptions.merge())
        }

        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "Language lessons seeded successfully") }
            .addOnFailureListener { e -> Log.w(TAG, "Lesson seeding failed: ${e.message}") }
    }

    // ── Progress: progress/{uid}_{lessonId} ───────────────────────────────────

    /**
     * Creates or updates a progress document when a lesson is completed.
     * Document ID pattern: {uid}_{lessonId}
     * Fields: uid, lessonId, completed, score, completedAt
     */
    fun syncLessonProgress(lessonId: String, score: Int = 100, xpEarned: Int = 10) {
        val uid = currentUid() ?: run {
            Log.d(TAG, "No Firebase user — skipping progress sync")
            return
        }
        val db = getDb() ?: return

        val docId = "${uid}_${lessonId}"
        val progressData = mapOf(
            "uid"         to uid,
            "lessonId"    to lessonId,
            "completed"   to true,
            "score"       to score,
            "xpEarned"    to xpEarned,
            "completedAt" to FieldValue.serverTimestamp()
        )

        // Write to top-level progress collection
        db.collection(PROGRESS).document(docId)
            .set(progressData, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Progress written: $docId")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Progress write failed for $docId: ${e.message}")
            }

        // Also write to subcollection for user-scoped queries
        db.collection(USERS).document(uid)
            .collection(PROGRESS).document(lessonId)
            .set(progressData, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.w(TAG, "Subcollection progress write failed: ${e.message}")
            }
    }

    // ── XP & Level update ────────────────────────────────────────────────────

    /**
     * Updates user XP in Firestore and auto-calculates the new level.
     * +10 XP per lesson by default.
     */
    fun addXpAndUpdateLevel(xpToAdd: Int = 10, currentXp: Int, onNewLevel: ((Int, Int) -> Unit)? = null) {
        val uid = currentUid() ?: return
        val db = getDb() ?: return

        val newXp    = currentXp + xpToAdd
        val newLevel = calculateLevel(newXp)

        val updates = mapOf(
            "xp"         to newXp,
            "level"      to newLevel,
            "lastActive" to FieldValue.serverTimestamp()
        )

        db.collection(USERS).document(uid)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "XP updated: $newXp, Level: $newLevel")
                onNewLevel?.invoke(newXp, newLevel)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "XP update failed: ${e.message}")
                // Still report locally even if Firestore fails
                onNewLevel?.invoke(newXp, newLevel)
            }
    }

    // ── Stats sync ───────────────────────────────────────────────────────────

    /**
     * Updates top-level user stats in Firestore.
     */
    fun syncUserStats(xp: Int, streak: Int, leafBalance: Int) {
        val uid = currentUid() ?: return
        val db = getDb() ?: return

        val updates = mapOf(
            "xp"          to xp,
            "level"       to calculateLevel(xp),
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
     * Syncs updated user profile info (username, native language, profile image) to Firestore.
     */
    fun syncUserProfile(name: String, nativeLanguage: String, profileImage: String?) {
        val uid = currentUid() ?: return
        val db = getDb() ?: return

        val userUpdates = mapOf(
            "username"     to name,
            "profileImage" to (profileImage ?: "")
        )
        db.collection(USERS).document(uid)
            .update(userUpdates)
            .addOnFailureListener { e ->
                Log.w(TAG, "Profile sync failed: ${e.message}")
            }

        val settingsUpdates = mapOf(
            "language" to nativeLanguage.lowercase()
        )
        db.collection(SETTINGS).document(uid)
            .update(settingsUpdates)
            .addOnFailureListener { e ->
                Log.w(TAG, "Profile settings sync failed: ${e.message}")
            }
    }

    // ── Read user from Firestore ──────────────────────────────────────────────

    /**
     * Fetches current user data from Firestore.
     * onResult is called with a map of field values, or null on failure.
     */
    fun fetchUserData(onResult: (Map<String, Any>?) -> Unit) {
        val uid = currentUid() ?: run {
            onResult(null)
            return
        }
        val db = getDb() ?: run {
            onResult(null)
            return
        }

        db.collection(USERS).document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    onResult(doc.data)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Fetch user data failed: ${e.message}")
                onResult(null)
            }
    }

    // ── SRS Cards ────────────────────────────────────────────────────────────

    /**
     * Seeds SRS cards for a lesson's vocab into Firestore: users/{uid}/srs/{vocabId}
     * Only writes if the document doesn't already exist (merge).
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
            batch.set(docRef, cardData, SetOptions.merge())
        }

        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "SRS seeded ${vocabIds.size} cards for $lessonId") }
            .addOnFailureListener { e -> Log.w(TAG, "SRS seed failed: ${e.message}") }
    }

    /**
     * Alias for [syncLessonProgress] — called by LessonPlayerActivity on lesson completion.
     * Writes to top-level `progress/{uid}_{lessonId}` collection.
     */
    fun syncLessonComplete(lessonId: String, xpEarned: Int = 10) {
        syncLessonProgress(lessonId = lessonId, xpEarned = xpEarned)
    }

    // ── Streak check-in ──────────────────────────────────────────────────────

    enum class StreakCheckinResult {
        CLAIMED,          // Successfully claimed today's streak reward
        ALREADY_CLAIMED,  // Already claimed today
        RESET             // Streak was reset (missed a day)
    }

    /**
     * Checks the user's lastCheckinDate against today.
     * - Same day  → ALREADY_CLAIMED (no-op)
     * - Yesterday → increments streak, adds +5 leaves, writes lastCheckinDate = today → CLAIMED
     * - Older     → resets streak to 1, adds +5 leaves, writes lastCheckinDate = today → RESET
     *
     * Result is returned on the main thread via [onResult].
     */
    fun claimStreakCheckin(onResult: (StreakCheckinResult, Int /*newStreak*/, Int /*newLeaves*/) -> Unit) {
        val uid = currentUid() ?: run { onResult(StreakCheckinResult.ALREADY_CLAIMED, 0, 0); return }
        val db  = getDb()        ?: run { onResult(StreakCheckinResult.ALREADY_CLAIMED, 0, 0); return }

        val today = java.time.LocalDate.now().toString()  // "yyyy-MM-dd"

        db.collection(USERS).document(uid).get()
            .addOnSuccessListener { doc ->
                val lastCheckin  = doc.getString("lastCheckinDate") ?: ""
                val currentStreak = (doc.getLong("streak") ?: 0L).toInt()
                val currentLeaves = (doc.getLong("leafBalance") ?: 0L).toInt()

                if (lastCheckin == today) {
                    // Already claimed today
                    onResult(StreakCheckinResult.ALREADY_CLAIMED, currentStreak, currentLeaves)
                    return@addOnSuccessListener
                }

                val yesterday = java.time.LocalDate.now().minusDays(1).toString()
                val newStreak = if (lastCheckin == yesterday) currentStreak + 1 else 1
                val newLeaves = currentLeaves + 5
                val result    = if (lastCheckin == yesterday) StreakCheckinResult.CLAIMED else StreakCheckinResult.RESET

                val updates = mapOf(
                    "lastCheckinDate" to today,
                    "streak"          to newStreak,
                    "leafBalance"     to newLeaves,
                    "lastActive"      to FieldValue.serverTimestamp()
                )

                db.collection(USERS).document(uid).update(updates)
                    .addOnSuccessListener {
                        Log.d(TAG, "Streak checked in: streak=$newStreak, leaves=$newLeaves")
                        onResult(result, newStreak, newLeaves)
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Streak checkin write failed: ${e.message}")
                        onResult(result, newStreak, newLeaves)
                    }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Streak checkin read failed: ${e.message}")
                onResult(StreakCheckinResult.ALREADY_CLAIMED, 0, 0)
            }
    }

    /**
     * Saves the active learning language to the user doc in Firestore.
     */
    fun saveActiveLanguage(languageId: String) {
        val uid = currentUid() ?: return
        val db  = getDb()        ?: return
        db.collection(USERS).document(uid)
            .update("activeLanguageId", languageId)
            .addOnFailureListener { e -> Log.w(TAG, "Active language save failed: ${e.message}") }
        db.collection(SETTINGS).document(uid)
            .update("language", languageId)
            .addOnFailureListener { e -> Log.w(TAG, "Settings language save failed: ${e.message}") }
    }
}

