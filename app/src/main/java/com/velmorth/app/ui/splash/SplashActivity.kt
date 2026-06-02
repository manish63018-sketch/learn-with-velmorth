package com.velmorth.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.MainActivity
import com.velmorth.app.R
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.ui.auth.LoginActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity
import com.velmorth.app.ui.permissions.PermissionsActivity
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SplashContent()
        }
    }
    @Composable
    private fun SplashContent() {
        var startAnimation by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }

        // Breathtaking fade-in for the splash screen mascot and background
        val backgroundAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            label = "backgroundAlpha"
        )

        LaunchedEffect(Unit) {
            startAnimation = true

            // Animate progress bar over 2.5 seconds
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = 2500, easing = FastOutSlowInEasing)
            ) { value, _ ->
                progress = value
            }

            // Brief pause at 100% for premium feel
            delay(200)

            // Run Firestore initialization in background (non-blocking)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                FirestoreProgressRepository.ensureUserDocExists()
                FirestoreProgressRepository.ensureSettingsDocExists()
                FirestoreProgressRepository.ensureLanguageLessonsExist()
            }

            val prefs = PrefsManager(this@SplashActivity)

            val intent = when {
                prefs.isFirstLaunch -> {
                    prefs.isFirstLaunch = false
                    Intent(this@SplashActivity, PermissionsActivity::class.java)
                }
                currentUser == null  -> Intent(this@SplashActivity, LoginActivity::class.java)
                !prefs.isOnboarded   -> Intent(this@SplashActivity, OnboardingActivity::class.java)
                else                 -> Intent(this@SplashActivity, MainActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // ── UI ────────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F2218)) // Dark forest green background color matching the image
        ) {
            // Full Screen Background Image with subtle fade-in transition
            Image(
                painter = painterResource(id = R.drawable.splash_bg),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(backgroundAlpha),
                contentScale = ContentScale.Crop
            )

            // Dynamic progress bar and thumb
            // Positioned exactly at 76.7% from top (bias = 0.534f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(BiasAlignment(0f, 0.534f))
                    .alpha(backgroundAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.58f)
                        .height(36.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Track capsule
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                color = Color(0xFF071911), // Extra dark green track background
                                shape = RoundedCornerShape(50)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFF52B788).copy(alpha = 0.35f), // Glowing light green border
                                shape = RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Progress fill capsule
                        if (progress > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF1B4332), // Forest green
                                                Color(0xFF2D6A4F), // Mid green
                                                Color(0xFF52B788)  // Glowing light green
                                            )
                                        ),
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }

                    // Leaf thumb at the end of progress
                    if (progress > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.splash_leaf),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(x = 12.dp) // Center the leaf thumb at the progress edge
                            )
                        }
                    }
                }
            }
        }
    }
}
