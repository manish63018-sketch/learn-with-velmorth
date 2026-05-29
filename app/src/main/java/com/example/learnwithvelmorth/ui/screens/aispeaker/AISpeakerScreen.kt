package com.example.learnwithvelmorth.ui.screens.aispeaker

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.learnwithvelmorth.theme.LearnWithVelmorthTheme
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Colour palette (local constants for easy theming)
// ─────────────────────────────────────────────────────────────────────────────

private val ForestDeep  = Color(0xFF1B4332)
private val MossGreen   = Color(0xFF40916C)
private val CreamWhite  = Color(0xFFF1E8D0)
private val LeafGold    = Color(0xFFD4A017)
private val NightForest = Color(0xFF0D2E1C)
private val StarColor   = Color(0xCCFFFFFF)

// ─────────────────────────────────────────────────────────────────────────────
// Screen entry point
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AISpeakerScreen(
    isPremium: Boolean,
    onUpgradeToPremium: () -> Unit,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ── Dark forest gradient background ───────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(NightForest, Color(0xFF143826), ForestDeep),
                    )
                )
        )

        // ── Starry particles (Canvas) ──────────────────────────────────────
        StarryBackground()

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Text(
                text = "AI Speaker 🎙️",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        // ── Content layer ─────────────────────────────────────────────────
        if (isPremium) {
            PremiumContent(
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            // Dim/blur the locked content behind the overlay
            Box(modifier = Modifier.fillMaxSize()) {
                PremiumContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(8.dp),
                )
                // Dark scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                )
                // Lock overlay card
                PremiumLockOverlay(onUpgradeToPremium = onUpgradeToPremium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stars canvas
// ─────────────────────────────────────────────────────────────────────────────

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

@Composable
private fun StarryBackground() {
    val stars = remember {
        List(80) {
            Star(
                x      = Random.nextFloat(),
                y      = Random.nextFloat(),
                radius = Random.nextFloat() * 2.5f + 0.5f,
                alpha  = Random.nextFloat() * 0.6f + 0.3f,
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
        ),
        label = "twinkle",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEachIndexed { index, star ->
            val twinkle = (sin(twinklePhase + index * 0.5f) * 0.3f + 0.7f).coerceIn(0.1f, 1f)
            drawCircle(
                color  = StarColor.copy(alpha = star.alpha * twinkle),
                radius = star.radius,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Premium content (microphone + waveform + transcript)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PremiumContent(modifier: Modifier = Modifier) {
    var isRecording by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // ── Mic permission handling ───────────────────────────────────────────
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasMicPermission = granted
        if (granted) isRecording = true
    }
    // ─────────────────────────────────────────────────────────────────────

    Column(
        modifier = modifier
            .padding(top = 100.dp, bottom = 40.dp)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Prompt text
        Text(
            text = if (isRecording) "Listening… 🎧" else "Tap & speak in Spanish",
            style = MaterialTheme.typography.headlineSmall.copy(
                color  = Color.White,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )

        // Waveform
        WaveformVisualizer(isActive = isRecording)

        // Microphone button – requests permission if not yet granted
        MicrophoneButton(
            isRecording = isRecording,
            onClick = {
                when {
                    hasMicPermission -> isRecording = !isRecording
                    else -> micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
        )

        // Permission hint when mic is denied
        if (!hasMicPermission) {
            Text(
                text  = "🎤 Microphone permission needed to record",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LeafGold.copy(alpha = 0.80f),
                ),
                textAlign = TextAlign.Center,
            )
        }

        // Transcript card
        TranscriptCard()
    }
}

@Composable
private fun WaveformVisualizer(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label        = "wavePhase",
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        drawWaveform(phase = if (isActive) phase else 0f, isActive = isActive)
    }
}

private fun DrawScope.drawWaveform(phase: Float, isActive: Boolean) {
    val barCount  = 30
    val barWidth  = size.width / (barCount * 2f)
    val midY      = size.height / 2f
    val maxHeight = size.height * 0.45f
    val color     = LeafGold

    for (i in 0 until barCount) {
        val fraction = i.toFloat() / barCount
        val height = if (isActive) {
            (sin(fraction * 4 * Math.PI + phase) * maxHeight).toFloat().let {
                maxHeight * 0.2f + (it + maxHeight) / 2f * 0.8f
            }
        } else {
            maxHeight * 0.08f
        }
        val x = fraction * size.width + barWidth / 2f
        drawLine(
            color       = color.copy(alpha = if (isActive) 0.9f else 0.35f),
            start       = Offset(x, midY - height),
            end         = Offset(x, midY + height),
            strokeWidth = barWidth,
            cap         = StrokeCap.Round,
        )
    }
}

@Composable
private fun MicrophoneButton(isRecording: Boolean, onClick: () -> Unit) {
    val pulseScale by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue  = 1f,
        targetValue   = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale",
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer glow ring
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(LeafGold.copy(alpha = 0.20f)),
            )
        }

        // Main button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isRecording)
                        Brush.radialGradient(listOf(LeafGold, Color(0xFFB8860B)))
                    else
                        Brush.radialGradient(listOf(MossGreen, ForestDeep))
                )
                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop recording" else "Start recording",
                tint   = Color.White,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun TranscriptCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.10f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text  = "Previous exchange",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.White.copy(alpha = 0.55f),
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Spacer(Modifier.height(8.dp))
            TranscriptLine(speaker = "You 🎙️",     text = "¿Cómo estás?",     color = LeafGold)
            Spacer(Modifier.height(6.dp))
            TranscriptLine(speaker = "Velmorth 🦦", text = "¡Muy bien, gracias!", color = MossGreen)
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "Score: 92% accuracy ✨",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = LeafGold,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

@Composable
private fun TranscriptLine(speaker: String, text: String, color: Color) {
    Column {
        Text(
            text  = speaker,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color.copy(alpha = 0.75f),
            ),
        )
        Text(
            text  = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Premium lock overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BoxScope.PremiumLockOverlay(onUpgradeToPremium: () -> Unit) {
    Card(
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 28.dp),
        shape  = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF142E1F).copy(alpha = 0.97f),
        ),
        elevation = CardDefaults.cardElevation(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = "🎙️", fontSize = 52.sp)

            Text(
                text  = "AI Speaker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )

            // Premium badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.horizontalGradient(listOf(LeafGold, Color(0xFFB8860B)))
                    )
                    .padding(horizontal = 16.dp, vertical = 5.dp),
            ) {
                Text(
                    text  = "✨ Premium Feature",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Feature list
            val features = listOf(
                "🎯 Pronunciation scoring",
                "💬 Real conversation practice",
                "⚡ Instant feedback",
                "🌍 Native accent training",
            )
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text  = feature,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.90f),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Unlock button
            Button(
                onClick = onUpgradeToPremium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(LeafGold, Color(0xFFB8860B))),
                            RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Unlock Premium 👑",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                }
            }

            // Free trial banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MossGreen.copy(alpha = 0.25f))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text  = "🎁 First 7 days FREE",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF86EFAC),
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0D2E1C)
@Composable
private fun AISpeakerLockedPreview() {
    LearnWithVelmorthTheme {
        AISpeakerScreen(
            isPremium           = false,
            onUpgradeToPremium  = {},
            onBack              = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D2E1C)
@Composable
private fun AISpeakerUnlockedPreview() {
    LearnWithVelmorthTheme {
        AISpeakerScreen(
            isPremium           = true,
            onUpgradeToPremium  = {},
            onBack              = {},
        )
    }
}
