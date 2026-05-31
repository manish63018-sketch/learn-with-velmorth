package com.velmorth.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.velmorth.app.MainActivity
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.ui.auth.LoginActivity

/**
 * Handles the step-by-step onboarding flow for new users.
 * Stage 1: Welcome & mascot animation.
 * Stage 2: Choose target language (embedded via [LanguageSelectFragment]).
 * Stage 3: Choose native language & daily practice commitment goal.
 */
class OnboardingActivity : FragmentActivity() {

    private lateinit var prefsManager: PrefsManager
    private val containerId = View.generateViewId()
    private var currentStage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefsManager = PrefsManager(this)

        // Programmatic container layout (eliminates the need for layout XML files)
        val frameLayout = FrameLayout(this).apply {
            id = containerId
        }
        setContentView(frameLayout)

        if (savedInstanceState == null) {
            showStage(currentStage)
        }
    }

    /**
     * Advances to the next onboarding stage.
     */
    fun nextStage() {
        currentStage++
        if (currentStage > 3) {
            // Complete onboarding
            prefsManager.isOnboarded = true
            
            // Navigate to LoginActivity so they can register locally, or go to MainActivity
            // The prompt says "On onboarding finish: save to PrefsManager, set isOnboarded=true, go to MainActivity"
            // Let's go to LoginActivity or MainActivity. Let's go directly to MainActivity but allow guest
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            showStage(currentStage)
        }
    }

    private fun showStage(stage: Int) {
        val fragment: Fragment = when (stage) {
            1 -> WelcomeOnboardingFragment()
            2 -> LanguageSelectFragment()
            else -> GoalOnboardingFragment()
        }

        supportFragmentManager.commit {
            setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            replace(containerId, fragment)
        }
    }
}
