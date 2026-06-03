package com.velmorth.app.ui.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.User
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.utils.XPManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reactive user profile state provider.
 *
 * Flutter equivalent: `UserProvider` (ChangeNotifier in provider package).
 * Exposes a [StateFlow] of [User] that composables/fragments observe instead of
 * reading [PrefsManager] directly (which is synchronous and not observable).
 *
 * Consumers:
 *  - Profile screen (name, avatar, level, xp, streak, leaves)
 *  - Home screen (streak, leaf balance, daily goal XP bar)
 *  - Shop screen (leaf balance for purchases)
 *  - Premium screen (isPremium gate)
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val prefs: PrefsManager
) : ViewModel() {

    private val _userState = MutableStateFlow(loadUserFromPrefs())

    /**
     * Hot stream of the current [User]. Always starts with the locally cached
     * SharedPreferences state and updates when Firestore syncs.
     *
     * Flutter equivalent:
     * ```dart
     * Consumer<UserProvider>(
     *   builder: (context, userProvider, _) => Text(userProvider.user.name),
     * )
     * ```
     * Kotlin equivalent:
     * ```kotlin
     * val user by viewModel.userState.collectAsState()
     * Text(text = user.name)
     * ```
     */
    val userState: StateFlow<User> = _userState.asStateFlow()

    init {
        // On first collect, immediately sync from Firestore to ensure fresh data
        refreshFromFirestore()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches the latest user doc from Firestore and merges it into the local state.
     * Non-blocking — runs on IO dispatcher.
     *
     * Call this:
     *  - On app resume (MainActivity.onResume)
     *  - After a lesson completes (to reflect new XP/level)
     *  - After a shop purchase (to reflect new leaf balance)
     */
    fun refreshFromFirestore() {
        viewModelScope.launch(Dispatchers.IO) {
            FirestoreProgressRepository.fetchUserData { data ->
                if (data != null) {
                    val name       = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: prefs.userName
                    val email      = (data["email"] as? String)?.takeIf { it.isNotBlank() } ?: prefs.userEmail
                    val username   = (data["username"] as? String)?.takeIf { it.isNotBlank() } ?: prefs.username
                    val xp         = (data["xp"] as? Long)?.toInt() ?: prefs.xp
                    val level      = (data["level"] as? Long)?.toInt() ?: prefs.level
                    val streak     = (data["streak"] as? Long)?.toInt() ?: prefs.streak
                    val leaves     = (data["leafBalance"] as? Long)?.toInt() ?: prefs.leaves
                    val isPremium  = data["isPremium"] as? Boolean ?: prefs.isPremium
                    val photoUrl   = (data["profileImage"] as? String) ?: prefs.photoUrl
                    val language   = (data["activeLanguageId"] as? String) ?: prefs.selectedLanguage
                    val lastCheckin = (data["lastCheckinDate"] as? String) ?: prefs.lastCheckinDate

                    // Persist to local cache so offline reads are up to date
                    prefs.userName        = name
                    prefs.userEmail       = email
                    prefs.username        = username
                    prefs.xp              = xp
                    prefs.level           = level
                    prefs.streak          = streak
                    prefs.leaves          = leaves
                    prefs.isPremium       = isPremium
                    prefs.photoUrl        = photoUrl
                    prefs.selectedLanguage = language
                    prefs.lastCheckinDate  = lastCheckin

                    _userState.update { it.copy(
                        name            = name,
                        email           = email,
                        username        = username,
                        xp              = xp,
                        level           = level,
                        streak          = streak,
                        leafBalance     = leaves,
                        isPremium       = isPremium,
                        photoUrl        = photoUrl,
                        selectedLanguage = language,
                        lastCheckinDate = lastCheckin
                    )}
                    Log.d("UserViewModel", "Refreshed from Firestore: xp=$xp, streak=$streak")
                }
            }
        }
    }

    /**
     * Adds delta XP to the current user, recalculates level, and syncs to Firestore.
     *
     * Flutter equivalent:
     * ```dart
     * context.read<UserProvider>().addXp(10);
     * ```
     */
    fun addXp(delta: Int) {
        val current = _userState.value
        val newXp   = current.xp + delta
        val newLevel = XPManager.getLevelForXp(newXp)

        // Optimistic local update
        prefs.xp    = newXp
        prefs.level = newLevel
        _userState.update { it.copy(xp = newXp, level = newLevel) }

        // Background Firestore sync
        viewModelScope.launch(Dispatchers.IO) {
            FirestoreProgressRepository.addXpAndUpdateLevel(
                xpToAdd    = delta,
                currentXp  = current.xp
            )
        }
    }

    /**
     * Adjusts the leaf balance by [delta] (positive = earn, negative = spend).
     *
     * Flutter equivalent:
     * ```dart
     * context.read<UserProvider>().updateLeaves(-50); // spend
     * ```
     */
    fun updateLeaves(delta: Int) {
        val newLeaves = maxOf(0, _userState.value.leafBalance + delta)
        prefs.leaves  = newLeaves
        _userState.update { it.copy(leafBalance = newLeaves) }

        // Sync full stats to Firestore (leaves + streak + xp together)
        viewModelScope.launch(Dispatchers.IO) {
            val state = _userState.value
            FirestoreProgressRepository.syncUserStats(
                xp          = state.xp,
                streak      = state.streak,
                leafBalance = newLeaves
            )
        }
    }

    /**
     * Updates the streak and syncs to Firestore.
     */
    fun updateStreak(newStreak: Int, newLeaves: Int) {
        prefs.streak = newStreak
        prefs.leaves = newLeaves
        _userState.update { it.copy(streak = newStreak, leafBalance = newLeaves) }
    }

    /**
     * Updates the display name and native language, syncing to Firestore.
     */
    fun updateProfile(displayName: String, nativeLanguage: String) {
        prefs.userName        = displayName
        prefs.nativeLanguage  = nativeLanguage
        _userState.update { it.copy(name = displayName, nativeLanguage = nativeLanguage) }

        viewModelScope.launch(Dispatchers.IO) {
            FirestoreProgressRepository.syncUserProfile(
                name           = displayName,
                nativeLanguage = nativeLanguage,
                profileImage   = null
            )
        }
    }

    /**
     * Updates the profile photo URL after a successful Firebase Storage upload.
     */
    fun updatePhotoUrl(url: String) {
        prefs.photoUrl = url
        _userState.update { it.copy(photoUrl = url) }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Builds a [User] from the current [PrefsManager] values. */
    private fun loadUserFromPrefs(): User = User(
        uid              = prefs.uid.ifEmpty { "guest" },
        name             = prefs.userName.ifEmpty { "Learner" },
        username         = prefs.username,
        email            = prefs.userEmail,
        photoUrl         = prefs.photoUrl,
        isPremium        = prefs.isPremium,
        streak           = prefs.streak,
        leafBalance      = prefs.leaves,
        darkMode         = prefs.darkMode,
        notificationsEnabled = prefs.notificationsEnabled,
        selectedLanguage = prefs.selectedLanguage,
        nativeLanguage   = prefs.nativeLanguage,
        xp               = prefs.xp,
        level            = prefs.level,
        lastCheckinDate  = prefs.lastCheckinDate
    )
}
