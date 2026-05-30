package com.velmorth.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velmorth.app.domain.model.User
import com.velmorth.app.domain.model.LeafTransaction
import com.velmorth.app.domain.model.LeafTransactionType
import com.velmorth.app.domain.repository.UserRepository
import com.velmorth.app.domain.repository.LeafWalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel used by the Onboarding screen.
 * Persists the user's selected language and daily goal to the local DB
 * so that future app launches skip onboarding.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val leafWalletRepository: LeafWalletRepository,
) : ViewModel() {

    fun saveOnboardingData(languageId: String, nativeLanguageId: String, dailyGoalMinutes: Int) {
        viewModelScope.launch {
            val existingUser = userRepository.getUser().first()
            if (existingUser == null) {
                // No user yet — create a fresh one with onboarding choices
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                userRepository.saveUser(
                    User(
                        id                 = "local_user",
                        name               = "Learner",
                        avatarEmoji        = "🌿",
                        leafBalance        = 50,          // welcome bonus
                        selectedLanguageId = languageId,
                        nativeLanguageId   = nativeLanguageId,
                        dailyGoalMinutes   = dailyGoalMinutes,
                        joinedDate         = today,
                        lastActiveDate     = today,
                    )
                )
                // Welcome leaf bonus
                leafWalletRepository.addTransaction(
                    LeafTransaction(
                        id               = "welcome_bonus",
                        userId           = "local_user",
                        amount           = 50,
                        type             = LeafTransactionType.ADMIN_GRANT,
                        description      = "🌿 Welcome to Velmorth! Here are 50 leaves to start your journey.",
                        timestamp        = System.currentTimeMillis(),
                    )
                )
            } else {
                // User exists (shouldn't happen in normal flow) — just update prefs
                userRepository.setSelectedLanguage("local_user", languageId)
                userRepository.setDailyGoal("local_user", dailyGoalMinutes)
            }
        }
    }
}
