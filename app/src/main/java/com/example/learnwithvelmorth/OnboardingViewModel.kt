package com.example.learnwithvelmorth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnwithvelmorth.domain.model.User
import com.example.learnwithvelmorth.domain.repository.UserRepository
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
) : ViewModel() {

    fun saveOnboardingData(languageId: String, dailyGoalMinutes: Int) {
        viewModelScope.launch {
            val existingUser = userRepository.getUser().first()
            if (existingUser == null) {
                // No user yet — create a fresh one with onboarding choices
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                userRepository.saveUser(
                    User(
                        id               = "local_user",
                        name             = "Learner",
                        avatarEmoji      = "🌿",
                        leafBalance      = 50,          // welcome bonus
                        selectedLanguageId = languageId,
                        dailyGoalMinutes = dailyGoalMinutes,
                        joinedDate       = today,
                        lastActiveDate   = today,
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
