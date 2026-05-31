package com.velmorth.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.MainActivity
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.ui.auth.LoginActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity

/**
 * Launcher Activity — hosts the [SplashScreen] composable.
 *
 * Routing priority:
 *  1. Splash animation plays
 *  2. If Firebase session is active ([FirebaseAuth.currentUser] != null) AND user is onboarded → MainActivity
 *  3. If locally onboarded (returning guest) → MainActivity
 *  4. First time / logged out → OnboardingActivity
 */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = PrefsManager(this)

        setContent {
            SplashScreen(
                onSplashFinished = {
                    val firebaseUser = try {
                        FirebaseAuth.getInstance().currentUser
                    } catch (e: Exception) {
                        null // Firebase not initialised yet
                    }

                    val nextIntent = when {
                        // Active Firebase session — go straight to app
                        firebaseUser != null -> {
                            prefs.isOnboarded = true // ensure flag is consistent
                            Intent(this@SplashActivity, MainActivity::class.java)
                        }
                        // Local guest / onboarded without Firebase
                        prefs.isOnboarded -> Intent(this@SplashActivity, MainActivity::class.java)
                        // First launch — onboarding then login
                        else -> Intent(this@SplashActivity, OnboardingActivity::class.java)
                    }

                    startActivity(nextIntent)
                    finish()
                }
            )
        }
    }
}
