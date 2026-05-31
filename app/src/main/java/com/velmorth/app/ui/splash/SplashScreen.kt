package com.velmorth.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.R
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ── Brand colors ─────────────────────────────────────────────────────────────
private val ForestGreen = Color(0xFF0F4D37)
private val LeafGreen   = Color(0xFF8BEA57)
private val DarkLeaf    = Color(0xFF24583F)
private val GoldGlow    = Color(0xFFD4AC0D)
private val MintText    = Color(0xFFA7F542)

// ── Sparkle particle data ─────────────────────────────────────────────────────
private data class Particle(
    val x: Float,      // 0f..1f relative to screen width
    val y: Float,      // 0f..1f relative to screen height
    val size: Dp,
    val durationMs: Int,
    val delayMs: Int,
    val color: Color
)

private val particles: List<Particle> = buildList {
    val colors = listOf(LeafGreen, GoldGlow, Color(0xFF52B788), Color(0xFFB7E4C7))
    val rng = Random(42)
    repeat(18) {
        add(
            Particle(
                x          = rng.nextFloat(),
                y          = rng.nextFloat(),
                size       = (4 + rng.nextInt(5)).dp,
                durationMs = 1800 + rng.nextInt(1400),
                delayMs    = rng.nextInt(2000),
                color      = colors[rng.nextInt(colors.size)]
            )
        )
    }
}

/**
 * Premium splash composable.
 *
 * - Logo gently floats up/down and pulses
 * - Animated gradient progress bar with rounded caps
 * - Floating sparkle particles around the screen
 * - "Growing Knowledge…" shimmers in sync with bar
 * - Calls [onSplashFinished] once bar reaches 100 %
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {

    // ── Progress: 0f → 1f driven by coroutine ────────────────────────────────
    var progress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "progress"
    )

    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(40)
            progress = (progress + 0.02f).coerceAtMost(1f)
        }
        delay(400)
        onSplashFinished()
    }

    // ── Logo float animation (gentle bob) ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "logo_float")
    val logoOffsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue  = 8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoFloat"
    )
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulse"
    )

    // ── Shimmer alpha for text ────────────────────────────────────────────────
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // ── Full-screen canvas ────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A3826), ForestGreen, Color(0xFF0F4D37))
                )
            )
    ) {

        // ── Floating sparkle particles ────────────────────────────────────────
        particles.forEach { p ->
            SparkleParticle(particle = p)
        }

        // ── Main content column ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo — floats + pulses
            Image(
                painter          = painterResource(id = R.drawable.velmorth_logo),
                contentDescription = "Velmorth Logo",
                modifier         = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        translationY = logoOffsetY
                        scaleX       = logoPulse
                        scaleY       = logoPulse
                    }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Title
            Text(
                text       = "Learn with Velmorth",
                color      = Color.White,
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle — shimmers
            Text(
                text      = "Plant the Seed of Learning 🌱",
                color     = MintText.copy(alpha = shimmerAlpha),
                fontSize  = 15.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(52.dp))

            // ── Gradient progress bar ─────────────────────────────────────────
            GradientProgressBar(
                progress = animatedProgress,
                modifier = Modifier
                    .width(280.dp)
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // "Growing Knowledge..." label — fades with shimmer
            Text(
                text      = "Growing Knowledge...",
                color     = Color.White.copy(alpha = shimmerAlpha),
                fontSize  = 13.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

// ── Gradient progress bar ─────────────────────────────────────────────────────

@Composable
private fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(DarkLeaf)
    ) {
        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(50.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(LeafGreen, GoldGlow)
                    )
                )
        )

        // Gleam overlay — a semi-transparent white strip at top half for "glass" effect
        Box(
            modifier = Modifier
                .fillMaxHeight(0.45f)
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp))
                .background(Color.White.copy(alpha = 0.18f))
        )
    }
}

// ── Sparkle particle ──────────────────────────────────────────────────────────

@Composable
private fun SparkleParticle(particle: Particle) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_${particle.delayMs}")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(particle.durationMs, delayMillis = particle.delayMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(particle.durationMs, delayMillis = particle.delayMs, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.TopStart)
    ) {
        Box(
            modifier = Modifier
                .padding(
                    start = (particle.x * 360).dp,
                    top   = (particle.y * 800).dp
                )
                .size(particle.size)
                .scale(scale)
                .alpha(alpha)
                .clip(CircleShape)
                .background(particle.color)
        )
    }
}
