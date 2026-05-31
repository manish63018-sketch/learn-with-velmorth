package com.velmorth.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.MainActivity
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

        // Logo scale animation
        val scale by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0.75f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            label = "logoScale"
        )

        // Logo alpha animation
        val alpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            label = "logoAlpha"
        )

        // Text slide-in alpha
        val textAlpha by animateFloatAsState(
            targetValue = if (startAnimation) 1f else 0f,
            animationSpec = tween(durationMillis = 1200, delayMillis = 400, easing = FastOutSlowInEasing),
            label = "textAlpha"
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D2B1C), // Deep forest night
                            Color(0xFF1B4332), // Forest green
                            Color(0xFF2D6A4F)  // Mid green
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {

                // ── Logo Circle ───────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(scale)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF52B788).copy(alpha = 0.3f),
                                    Color(0xFF1B4332).copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF52B788), Color(0xFF2D6A4F))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 56.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── App Title ─────────────────────────────────────────────────
                Text(
                    text = "Learn With Velmorth",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Tagline ───────────────────────────────────────────────────
                Text(
                    text = "Plant the Seed of Learning",
                    color = Color(0xFF95D5B2),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(textAlpha)
                )

                Spacer(modifier = Modifier.height(60.dp))

                // ── Animated Loading Bar ──────────────────────────────────────
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(textAlpha)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color(0xFF52B788),
                        trackColor = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (progress < 0.95f) "Loading..." else "Ready!",
                        color = Color(0xFF95D5B2).copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Bottom footer ─────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .alpha(textAlpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "by Velmorth",
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}
