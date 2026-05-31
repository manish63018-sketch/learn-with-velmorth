package com.velmorth.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Legacy Signup Screen. Automatically redirects users to the unified [LoginActivity].
 */
class SignupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
