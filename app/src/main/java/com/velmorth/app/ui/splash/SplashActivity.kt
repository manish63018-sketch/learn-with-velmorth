package com.velmorth.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.ui.auth.LoginActivity
import com.velmorth.app.MainActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity
import com.velmorth.app.data.local.PrefsManager

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({
            val currentUser = FirebaseAuth.getInstance().currentUser
            val prefs = PrefsManager(this)

            val intent = when {
                currentUser == null -> Intent(this, LoginActivity::class.java)
                !prefs.isOnboarded  -> Intent(this, OnboardingActivity::class.java)
                else                -> Intent(this, MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }, 2000)
    }
}
