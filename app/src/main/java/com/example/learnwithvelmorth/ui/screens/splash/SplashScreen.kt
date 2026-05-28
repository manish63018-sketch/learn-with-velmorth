package com.example.learnwithvelmorth.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnwithvelmorth.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// SplashScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full-screen splash shown on app cold-start.
 * Auto-navigates to Onboarding after 2.5 seconds.
 * No ViewModel required.
 */
@Composable
fun SplashScreen(
    onNavigateToOnboarding: () -> Unit,
) {
    // ── Auto-navigate after 2.5 s ──────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(2_500)
        onNavigateToOnboarding()
    }

    // ── Animation states ───────────────────────────────────────────────────
    val mascotScale = remember { Animatable(0f) }
    val titleAlpha  = remember { Animatable(0f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val shimmerOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Mascot springs in first
        mascotScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio  = Spring.DampingRatioLowBouncy,
                stiffness     = Spring.StiffnessMediumLow,
            )
        )
        // Title fades in
        titleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        // Subtitle fades in with a slight delay feel
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    // Gentle continuous shimmer on the gradient overlay
    LaunchedEffect(Unit) {
        shimmerOffset.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3_000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            )
        )
    }

    // ── UI ─────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to ForestDeep,
                        0.55f to ForestMid,
                        1.0f to MossGreen,
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {

        // ── Decorative radial glow behind mascot ─────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MossGreen.copy(alpha = 0.35f),
                            ForestDeep.copy(alpha = 0f),
                        )
                    )
                )
        )

        // ── Main content column ──────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        ) {

            Spacer(modifier = Modifier.weight(1f))

            // Mascot emoji with spring scale-in
            Text(
                text = "🌿",
                fontSize = 96.sp,
                modifier = Modifier
                    .scale(mascotScale.value)
                    .padding(bottom = 8.dp),
            )

            // Shimmer ring accent under mascot
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                LeafGold.copy(alpha = 0.0f),
                                LeafGold.copy(alpha = 0.7f * shimmerOffset.value),
                                LeafGold.copy(alpha = 0.0f),
                            )
                        ),
                        shape = PillShape
                    )
                    .alpha(mascotScale.value)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // App name — Playfair Display, cream white
            Text(
                text = "Learn With Velmorth",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = PlayfairFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 34.sp,
                    letterSpacing = 0.3.sp,
                ),
                color     = CreamWhite,
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(titleAlpha.value),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle — Nunito, softer cream
            Text(
                text = "Your forest of words awaits",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.6.sp,
                ),
                color     = CreamWhite.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(subtitleAlpha.value),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Bottom leaf dot loader
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 48.dp)
                    .alpha(subtitleAlpha.value),
            ) {
                repeat(3) { idx ->
                    BouncingLeafDot(delayMs = idx * 160)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bouncing leaf loading indicator dot
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BouncingLeafDot(delayMs: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_bounce_$delayMs")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -10f,
        animationSpec = infiniteRepeatable(
            animation   = tween(
                durationMillis = 500,
                delayMillis    = delayMs,
                easing         = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_y_$delayMs",
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(y = offsetY.dp)
            .background(
                color = LeafGold.copy(alpha = 0.9f),
                shape = androidx.compose.foundation.shape.CircleShape,
            )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SplashScreenPreview() {
    LearnWithVelmorthTheme {
        SplashScreen(onNavigateToOnboarding = {})
    }
}
