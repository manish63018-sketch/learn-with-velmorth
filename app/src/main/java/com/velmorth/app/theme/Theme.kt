package com.velmorth.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============================================================
// Light Color Scheme — Forest Day
// ============================================================
private val LightColorScheme = lightColorScheme(
    primary = ForestMid,
    onPrimary = PureWhite,
    primaryContainer = FogGreen,
    onPrimaryContainer = ForestDeep,

    secondary = MossGreen,
    onSecondary = PureWhite,
    secondaryContainer = PaleGreen,
    onSecondaryContainer = ForestDeep,

    tertiary = LeafGold,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFFFF3CD),
    onTertiaryContainer = Color(0xFF4A3500),

    error = CoralRed,
    onError = PureWhite,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = CreamWhite,
    onBackground = TextDark,

    surface = WarmWhite,
    onSurface = TextDark,
    surfaceVariant = SoftBeige,
    onSurfaceVariant = TextMid,

    outline = Color(0xFFB5C9B8),
    outlineVariant = Color(0xFFD9E8DC),

    inverseSurface = ForestDeep,
    inverseOnSurface = CreamWhite,
    inversePrimary = MintGreen,
)

// ============================================================
// Dark Color Scheme — Forest Night
// ============================================================
private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = ForestDeep,
    primaryContainer = ForestMid,
    onPrimaryContainer = FogGreen,

    secondary = LeafGreen,
    onSecondary = ForestDeep,
    secondaryContainer = ForestMid,
    onSecondaryContainer = PaleGreen,

    tertiary = LeafGoldLight,
    onTertiary = Color(0xFF3A2900),
    tertiaryContainer = LeafGoldDark,
    onTertiaryContainer = Color(0xFFFFF3CD),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = ForestNight,
    onBackground = PaleGreen,

    surface = MossNight,
    onSurface = PaleGreen,
    surfaceVariant = Color(0xFF1E3526),
    onSurfaceVariant = Color(0xFF8FAE96),

    outline = Color(0xFF4A6652),
    outlineVariant = Color(0xFF2D4A36),

    inverseSurface = FogGreen,
    inverseOnSurface = ForestDeep,
    inversePrimary = ForestMid,
)

// ============================================================
// Velmorth Extended Colors (extra design tokens)
// ============================================================
data class VelmorthColors(
    val leafGold: Color,
    val leafGoldLight: Color,
    val leafGoldDark: Color,
    val streakFire: Color,
    val aiSpeakerAccent: Color,
    val premiumAccent: Color,
    val correctAnswer: Color,
    val wrongAnswer: Color,
    val lockedContent: Color,
    val mascotBubble: Color,
)

val LocalVelmorthColors = staticCompositionLocalOf {
    VelmorthColors(
        leafGold = LeafGold,
        leafGoldLight = LeafGoldLight,
        leafGoldDark = LeafGoldDark,
        streakFire = SunriseOrange,
        aiSpeakerAccent = SkyBlue,
        premiumAccent = LavenderMist,
        correctAnswer = MossGreen,
        wrongAnswer = CoralRed,
        lockedContent = Color(0xFFB0B0B0),
        mascotBubble = WarmWhite,
    )
}

val MaterialTheme.velmorthColors: VelmorthColors
    @Composable get() = LocalVelmorthColors.current

// ============================================================
// Theme Composable
// ============================================================
@Composable
fun LearnWithVelmorthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val velmorthColors = if (darkTheme) {
        VelmorthColors(
            leafGold = LeafGoldLight,
            leafGoldLight = Color(0xFFFFD980),
            leafGoldDark = LeafGold,
            streakFire = Color(0xFFFF8C42),
            aiSpeakerAccent = Color(0xFF7EC8E3),
            premiumAccent = Color(0xFFBBA8E8),
            correctAnswer = MintGreen,
            wrongAnswer = Color(0xFFFF6B6B),
            lockedContent = Color(0xFF505050),
            mascotBubble = MossNight,
        )
    } else {
        VelmorthColors(
            leafGold = LeafGold,
            leafGoldLight = LeafGoldLight,
            leafGoldDark = LeafGoldDark,
            streakFire = SunriseOrange,
            aiSpeakerAccent = SkyBlue,
            premiumAccent = LavenderMist,
            correctAnswer = MossGreen,
            wrongAnswer = CoralRed,
            lockedContent = Color(0xFFB0B0B0),
            mascotBubble = WarmWhite,
        )
    }

    CompositionLocalProvider(LocalVelmorthColors provides velmorthColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
