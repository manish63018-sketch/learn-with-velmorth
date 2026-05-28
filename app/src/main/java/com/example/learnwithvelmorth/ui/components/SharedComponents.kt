package com.example.learnwithvelmorth.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch
import com.example.learnwithvelmorth.theme.*

/**
 * Animated leaf balance chip showing current leaf count.
 * Pulses gently when balance changes.
 */
@Composable
fun LeafBalanceChip(
    balance: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val leafGold = MaterialTheme.velmorthColors.leafGold
    val scale = remember { Animatable(1f) }

    LaunchedEffect(balance) {
        scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    Row(
        modifier = modifier
            .scale(scale.value)
            .clip(PillShape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        leafGold.copy(alpha = 0.15f),
                        MaterialTheme.velmorthColors.leafGoldLight.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "🍃",
            fontSize = if (compact) 14.sp else 18.sp,
        )
        Text(
            text = balance.toString(),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.velmorthColors.leafGoldDark,
        )
    }
}

/**
 * Animated streak counter with fire icon.
 */
@Composable
fun StreakCounter(
    streak: Int,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val streakColor = MaterialTheme.velmorthColors.streakFire
    val infiniteTransition = rememberInfiniteTransition(label = "streak_anim")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fire_scale"
    )

    Row(
        modifier = modifier
            .clip(PillShape)
            .background(streakColor.copy(alpha = 0.12f))
            .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 6.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "🔥",
            fontSize = if (compact) 14.sp else 18.sp,
            modifier = Modifier.scale(if (streak > 0) fireScale else 1f),
        )
        Text(
            text = "$streak",
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = streakColor,
        )
    }
}

/**
 * Styled forest card container.
 */
@Composable
fun ForestCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .clip(ForestCardShape)
            .background(backgroundColor)
            .padding(20.dp),
        content = content,
    )
}

/**
 * Premium badge overlay.
 */
@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    Text(
        text = "✨ PREMIUM",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.velmorthColors.leafGold,
        modifier = modifier
            .clip(PillShape)
            .background(MaterialTheme.velmorthColors.premiumAccent.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * XP Progress bar with animated fill.
 */
@Composable
fun XPProgressBar(
    currentXp: Int,
    maxXp: Int,
    modifier: Modifier = Modifier,
) {
    val progress = (currentXp.toFloat() / maxXp.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "⭐ $currentXp XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Next: $maxXp XP",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MossGreen, LeafGreen, MintGreen)
                        )
                    )
            )
        }
    }
}

/**
 * Pill-shaped primary button.
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    leadingIcon: String? = null,
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(PillShape)
            .background(if (enabled) backgroundColor else MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (enabled) Modifier.clickableWithRipple {
                    scope.launch {
                        scale.animateTo(0.94f, spring(stiffness = Spring.StiffnessHigh))
                        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                    onClick()
                } else Modifier
            )
            .padding(horizontal = 32.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Text(leadingIcon, fontSize = 18.sp)
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// Extension helper for clickable with ripple
private fun Modifier.clickableWithRipple(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)
