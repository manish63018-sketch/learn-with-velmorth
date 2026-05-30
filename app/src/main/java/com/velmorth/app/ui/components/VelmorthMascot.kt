package com.velmorth.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.velmorth.app.domain.VelmorthEmotion
import com.velmorth.app.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// AnimatedMascot — full emotion-driven Velmorth character
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Full Velmorth character composable with:
 * - Emotion-driven emoji + background glow
 * - Per-emotion animations (float, bounce, shake, pulse, spin...)
 * - Lip-sync oscillation while TTS is speaking
 * - Speech bubble with optional target-language parallel line
 * - Tap to dismiss current dialogue
 *
 * Designed to be placed on HomeScreen, LessonPlayer, ReviewScreen etc.
 */
@Composable
fun AnimatedMascot(
    modifier: Modifier = Modifier,
    viewModel: VelmorthCharacterViewModel = hiltViewModel(),
    size: Dp = 100.dp,
    showBubble: Boolean = true,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AnimatedMascotContent(
        emotion        = state.emotion,
        dialogueText   = if (showBubble) state.dialogueText else "",
        targetLangLine = if (showBubble) state.targetLangLine else "",
        isSpeaking     = state.isSpeaking,
        size           = size,
        modifier       = modifier,
        onTap          = { viewModel.dismissDialogue() },
    )
}

/**
 * Stateless version — can be driven by any external emotion/text.
 */
@Composable
fun AnimatedMascotContent(
    emotion: VelmorthEmotion,
    dialogueText: String = "",
    targetLangLine: String = "",
    isSpeaking: Boolean = false,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_anim")

    // ── Float (Y offset) — all states have gentle float, speed varies ──────
    val floatDuration = when (emotion) {
        VelmorthEmotion.EXCITED -> 600
        VelmorthEmotion.HAPPY   -> 1200
        VelmorthEmotion.PROUD   -> 1800
        VelmorthEmotion.TALKING -> 800
        else                    -> 1600
    }
    val floatAmplitude = when (emotion) {
        VelmorthEmotion.EXCITED -> 10f
        VelmorthEmotion.IDLE    -> 3f
        VelmorthEmotion.SAD     -> 2f
        else                    -> 6f
    }
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -floatAmplitude,
        targetValue  = floatAmplitude,
        animationSpec = infiniteRepeatable(
            animation  = tween(floatDuration, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float_y",
    )

    // ── Rotation — wobble on happy/excited, droop on sad ───────────────────
    val targetRotation = when (emotion) {
        VelmorthEmotion.EXCITED  -> 6f
        VelmorthEmotion.HAPPY    -> 3f
        VelmorthEmotion.SAD      -> -4f
        VelmorthEmotion.THINKING -> 5f
        VelmorthEmotion.TALKING  -> 2f
        else                     -> 2f
    }
    val rotation by infiniteTransition.animateFloat(
        initialValue = -targetRotation,
        targetValue  = targetRotation,
        animationSpec = infiniteRepeatable(
            animation  = tween(floatDuration + 200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rotation",
    )

    // ── Scale — pulse on excited/proud, lip-sync on TALKING ────────────────
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = when (emotion) {
            VelmorthEmotion.EXCITED  -> 0.95f
            VelmorthEmotion.TALKING  -> 0.97f
            VelmorthEmotion.PROUD    -> 0.98f
            else                     -> 1.0f
        },
        targetValue = when (emotion) {
            VelmorthEmotion.EXCITED  -> 1.08f
            VelmorthEmotion.TALKING  -> 1.04f
            VelmorthEmotion.PROUD    -> 1.04f
            else                     -> 1.0f
        },
        animationSpec = infiniteRepeatable(
            animation  = tween(
                durationMillis = if (emotion == VelmorthEmotion.TALKING) 200 else 600,
                easing = EaseInOutSine,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )

    // ── Glow alpha — breathing glow behind mascot ──────────────────────────
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = when (emotion) {
            VelmorthEmotion.EXCITED  -> 0.75f
            VelmorthEmotion.HAPPY    -> 0.6f
            VelmorthEmotion.PROUD    -> 0.65f
            VelmorthEmotion.TALKING  -> 0.55f
            VelmorthEmotion.SAD      -> 0.2f
            else                     -> 0.35f
        },
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // ── Emoji + color per emotion ──────────────────────────────────────────
    val mascotEmoji = when (emotion) {
        VelmorthEmotion.HAPPY    -> "🦦✨"
        VelmorthEmotion.EXCITED  -> "🦦🎉"
        VelmorthEmotion.SAD      -> "🦦😢"
        VelmorthEmotion.THINKING -> "🦦🤔"
        VelmorthEmotion.PROUD    -> "🦦🏆"
        VelmorthEmotion.TALKING  -> "🦦💬"
        VelmorthEmotion.IDLE     -> "🦦"
    }

    val glowColor = when (emotion) {
        VelmorthEmotion.HAPPY    -> Color(0xFF4CAF50)
        VelmorthEmotion.EXCITED  -> Color(0xFFFFD700)
        VelmorthEmotion.SAD      -> Color(0xFF90CAF9)
        VelmorthEmotion.THINKING -> Color(0xFFCE93D8)
        VelmorthEmotion.PROUD    -> Color(0xFFFFAB40)
        VelmorthEmotion.TALKING  -> Color(0xFF80DEEA)
        VelmorthEmotion.IDLE     -> Color(0xFF81C784)
    }

    Column(
        modifier           = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        // ── Speech Bubble ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = dialogueText.isNotEmpty(),
            enter   = fadeIn(tween(300)) + expandVertically(expandFrom = Alignment.Bottom),
            exit    = fadeOut(tween(200)) + shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            SpeechBubble(
                text           = dialogueText,
                targetLangLine = targetLangLine,
                emotion        = emotion,
                modifier       = Modifier.padding(bottom = 10.dp),
            )
        }

        // ── Mascot Body ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .graphicsLayer {
                    rotationZ    = rotation
                    scaleX       = scaleAnim
                    scaleY       = scaleAnim
                }
                .size(size + 20.dp)                       // glow ring is slightly bigger
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = glowAlpha),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                )
                .clickable(enabled = onTap != null) { onTap?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            // Inner circle with slight glassmorphism
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = mascotEmoji,
                    fontSize = (size.value * 0.46f).sp,
                )
            }
        }

        // ── Speaking indicator dots ───────────────────────────────────────
        AnimatedVisibility(
            visible = isSpeaking,
            enter   = fadeIn(tween(200)),
            exit    = fadeOut(tween(200)),
        ) {
            SpeakingDots(color = glowColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Speech Bubble
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SpeechBubble(
    text: String,
    targetLangLine: String,
    emotion: VelmorthEmotion,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = when (emotion) {
        VelmorthEmotion.SAD      -> Color(0xFFE3F2FD)
        VelmorthEmotion.EXCITED  -> Color(0xFFFFFDE7)
        VelmorthEmotion.PROUD    -> Color(0xFFFFF3E0)
        VelmorthEmotion.THINKING -> Color(0xFFF3E5F5)
        else                     -> Color(0xFFF1F8E9)
    }
    val borderColor = when (emotion) {
        VelmorthEmotion.SAD      -> Color(0xFF90CAF9)
        VelmorthEmotion.EXCITED  -> Color(0xFFFFD54F)
        VelmorthEmotion.PROUD    -> Color(0xFFFFAB40)
        VelmorthEmotion.THINKING -> Color(0xFFCE93D8)
        else                     -> Color(0xFF81C784)
    }

    Box(
        modifier = modifier
            .widthIn(max = 260.dp)
            .shadow(6.dp, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .background(bubbleColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text      = text,
                color     = Color(0xFF1B5E20),
                style     = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                textAlign = TextAlign.Center,
            )
            if (targetLangLine.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = targetLangLine,
                    color      = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 13.sp,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated Speaking Dots
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SpeakingDots(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val delay = listOf(0, 150, 300)

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = Modifier.padding(top = 6.dp),
    ) {
        delay.forEach { delayMs ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(500, delayMillis = delayMs, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot_$delayMs",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha)),
            )
        }
    }
}
