package com.velmorth.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.velmorth.app.theme.*
import com.velmorth.app.ui.components.PillButton
import com.velmorth.app.ui.components.AnimatedMascotContent
import com.velmorth.app.domain.VelmorthEmotion
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

private data class Language(
    val id: String,
    val name: String,
    val flag: String,
)

private data class DailyGoal(
    val minutes: Int,
    val label: String,
    val icon: String,
)

private val LANGUAGES = listOf(
    Language("ja", "Japanese",   "🇯🇵"),
    Language("en", "English",    "🇬🇧"),
    Language("fr", "French",     "🇫🇷"),
    Language("sa", "Sanskrit",   "🕉️"),
    Language("es", "Spanish",    "🇪🇸"),
    Language("de", "German",     "🇩🇪"),
    Language("it", "Italian",    "🇮🇹"),
    Language("ko", "Korean",     "🇰🇷"),
)

// Native languages user can select as their mother tongue
private val NATIVE_LANGUAGES = listOf(
    Language("hi", "Hindi",      "🇮🇳"),
    Language("en", "English",    "🇬🇧"),
    Language("es", "Spanish",    "🇪🇸"),
    Language("fr", "French",     "🇫🇷"),
    Language("de", "German",     "🇩🇪"),
    Language("pt", "Portuguese", "🇧🇷"),
    Language("zh", "Mandarin",   "🇨🇳"),
    Language("ar", "Arabic",     "🇸🇦"),
)

private val DAILY_GOALS = listOf(
    DailyGoal(5,  "5 min",  "🌱"),
    DailyGoal(10, "10 min", "🌿"),
    DailyGoal(15, "15 min", "🌲"),
    DailyGoal(20, "20 min", "🏔️"),
)

// ─────────────────────────────────────────────────────────────────────────────
// OnboardingScreen
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 4-page onboarding flow with HorizontalPager.
 * Pages: Welcome → Target Language → Native Language → Daily Goal
 *
 * @param onComplete Called when user finishes onboarding. Delivers chosen
 *                   language ID, native language ID, and daily goal in minutes.
 */
@Composable
fun OnboardingScreen(
    onComplete: (languageId: String, nativeLanguageId: String, dailyGoalMinutes: Int) -> Unit,
) {
    // ── State ──────────────────────────────────────────────────────────────
    var selectedLanguageId by remember { mutableStateOf("ja") }
    var selectedNativeLanguageId by remember { mutableStateOf("hi") }
    var selectedGoalMinutes by remember { mutableIntStateOf(10) }

    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    val isLastPage = pagerState.currentPage == 3
    val continueEnabled = when (pagerState.currentPage) {
        1    -> selectedLanguageId.isNotEmpty()
        2    -> selectedNativeLanguageId.isNotEmpty()
        else -> true
    }

    // ── Background gradient (shifts slightly per page) ─────────────────────
    val bgGradientStart by animateColorAsState(
        targetValue = when (pagerState.currentPage) {
            0    -> ForestDeep
            1    -> ForestMid
            2    -> Color(0xFF1A3A2A)
            else -> MossGreen
        },
        animationSpec = tween(600),
        label = "bg_start",
    )
    val bgGradientEnd by animateColorAsState(
        targetValue = when (pagerState.currentPage) {
            0    -> ForestMid
            1    -> MossGreen
            2    -> MossGreen
            else -> LeafGreen
        },
        animationSpec = tween(600),
        label = "bg_end",
    )

    // ── Root layout ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(bgGradientStart, bgGradientEnd))
            )
    ) {
        LeafParticlesEffect()
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Pager ─────────────────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> LanguageSelectionPage(
                        selectedLanguageId = selectedLanguageId,
                        onLanguageSelected = { selectedLanguageId = it },
                    )
                    2 -> NativeLanguagePage(
                        selectedNativeId = selectedNativeLanguageId,
                        onNativeSelected = { selectedNativeLanguageId = it },
                    )
                    3 -> DailyGoalPage(
                        selectedGoalMinutes = selectedGoalMinutes,
                        onGoalSelected      = { selectedGoalMinutes = it },
                    )
                }
            }

            // ── Bottom bar: dots + button ──────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // Page indicator dots
                PageIndicatorDots(
                    pageCount   = 4,
                    currentPage = pagerState.currentPage,
                )

                // Continue / Start button
                PillButton(
                    text    = if (isLastPage) "Start Learning! 🌿" else "Continue",
                    enabled = continueEnabled,
                    onClick = {
                        if (isLastPage) {
                            onComplete(selectedLanguageId, selectedNativeLanguageId, selectedGoalMinutes)
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    backgroundColor = if (isLastPage) LeafGold else MossGreen,
                    contentColor    = if (isLastPage) ForestDeep else CreamWhite,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 1 — Welcome
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomePage() {
    val mascotScale = remember { Animatable(0f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        mascotScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness    = Spring.StiffnessMedium,
            )
        )
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Velmorth mascot companion in glow ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            AnimatedMascotContent(
                emotion = VelmorthEmotion.HAPPY,
                dialogueText = "Hello! I am Velmorth, your otter spirit companion. Let's learn and grow together!",
                modifier = Modifier.scale(mascotScale.value)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text      = "Welcome to Velmorth!",
            style     = MaterialTheme.typography.headlineLarge.copy(
                fontFamily    = PlayfairFamily,
                fontWeight    = FontWeight.Bold,
                fontSize      = 32.sp,
            ),
            color     = CreamWhite,
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(contentAlpha.value),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text      = "Learn languages with a friendly\nforest companion",
            style     = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = NunitoFamily,
                lineHeight = 28.sp,
            ),
            color     = CreamWhite.copy(alpha = 0.82f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(contentAlpha.value),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Feature pills
        val features = listOf("🎯 Bite-sized lessons", "🔥 Daily streaks", "🍃 Earn leaf coins")
        features.forEach { feature ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value)
                    .clip(PillShape)
                    .background(CreamWhite.copy(alpha = 0.10f))
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(
                    text  = feature,
                    style = MaterialTheme.typography.titleSmall,
                    color = CreamWhite,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 2 — Language Selection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LanguageSelectionPage(
    selectedLanguageId: String,
    onLanguageSelected: (String) -> Unit,
) {
    val titleAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(450))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text      = "Choose Your Language",
            style     = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 28.sp,
            ),
            color     = CreamWhite,
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text      = "What do you want to learn?",
            style     = MaterialTheme.typography.bodyMedium,
            color     = CreamWhite.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(20.dp))

        LazyVerticalGrid(
            columns                  = GridCells.Fixed(2),
            horizontalArrangement    = Arrangement.spacedBy(12.dp),
            verticalArrangement      = Arrangement.spacedBy(12.dp),
            contentPadding           = PaddingValues(bottom = 8.dp),
            modifier                 = Modifier.fillMaxWidth(),
            userScrollEnabled        = false,
        ) {
            items(LANGUAGES) { language ->
                LanguageCard(
                    language           = language,
                    isSelected         = language.id == selectedLanguageId,
                    onSelect           = { onLanguageSelected(language.id) },
                )
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: Language,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMedium,
        ),
        label = "card_scale_${language.id}",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MossGreen else CreamWhite.copy(alpha = 0.18f),
        animationSpec = tween(250),
        label = "border_${language.id}",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MossGreen.copy(alpha = 0.30f) else CreamWhite.copy(alpha = 0.10f),
        animationSpec = tween(250),
        label = "bg_${language.id}",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            .clip(ForestCardShape)
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = ForestCardShape,
            )
            .clickable { onSelect() }
            .padding(vertical = 18.dp, horizontal = 12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text     = language.flag,
                fontSize = 36.sp,
            )
            Text(
                text  = language.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                color = if (isSelected) CreamWhite else CreamWhite.copy(alpha = 0.82f),
            )
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .clip(PillShape)
                        .background(MossGreen)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 3 — Native Language Selection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NativeLanguagePage(
    selectedNativeId: String,
    onNativeSelected: (String) -> Unit,
) {
    val titleAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(450))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text      = "Your Native Language",
            style     = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 28.sp,
            ),
            color     = CreamWhite,
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text      = "What language do you think in?",
            style     = MaterialTheme.typography.bodyMedium,
            color     = CreamWhite.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ForestCardShape)
                .background(CreamWhite.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text  = "💡 This helps Velmorth give better hints and explanations in your language.",
                style = MaterialTheme.typography.bodySmall,
                color = CreamWhite.copy(alpha = 0.80f),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns                  = GridCells.Fixed(2),
            horizontalArrangement    = Arrangement.spacedBy(12.dp),
            verticalArrangement      = Arrangement.spacedBy(12.dp),
            contentPadding           = PaddingValues(bottom = 8.dp),
            modifier                 = Modifier.fillMaxWidth(),
            userScrollEnabled        = false,
        ) {
            items(NATIVE_LANGUAGES) { language ->
                LanguageCard(
                    language   = language,
                    isSelected = language.id == selectedNativeId,
                    onSelect   = { onNativeSelected(language.id) },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page 4 — Daily Goal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyGoalPage(
    selectedGoalMinutes: Int,
    onGoalSelected: (Int) -> Unit,
) {
    val titleAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(450))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text      = "Set Your Daily Goal",
            style     = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
                fontSize   = 28.sp,
            ),
            color     = CreamWhite,
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text      = "How long do you want to practice each day?",
            style     = MaterialTheme.typography.bodyMedium,
            color     = CreamWhite.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.alpha(titleAlpha.value),
        )

        Spacer(modifier = Modifier.height(36.dp))

        DAILY_GOALS.forEachIndexed { index, goal ->
            GoalPillButton(
                goal            = goal,
                isSelected      = goal.minutes == selectedGoalMinutes,
                onSelect        = { onGoalSelected(goal.minutes) },
                animationDelay  = index * 80,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Motivational note
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ForestCardShape)
                .background(CreamWhite.copy(alpha = 0.10f))
                .padding(16.dp),
        ) {
            Text(
                text  = "🌱  Even 5 minutes a day builds lifelong fluency. Consistency is the secret!",
                style = MaterialTheme.typography.bodySmall,
                color = CreamWhite.copy(alpha = 0.80f),
            )
        }
    }
}

@Composable
private fun GoalPillButton(
    goal: DailyGoal,
    isSelected: Boolean,
    onSelect: () -> Unit,
    animationDelay: Int = 0,
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        alpha.animateTo(1f, tween(350))
    }

    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "goal_scale_${goal.minutes}",
    )

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(listOf(MossGreen, LeafGreen))
    } else {
        Brush.horizontalGradient(listOf(CreamWhite.copy(0.12f), CreamWhite.copy(0.08f)))
    }

    Row(
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha.value)
            .clip(PillShape)
            .background(bgBrush)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = CreamWhite.copy(alpha = 0.22f),
                shape = PillShape,
            )
            .clickable { onSelect() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(text = goal.icon, fontSize = 28.sp)
            Text(
                text  = goal.label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    fontSize   = 18.sp,
                ),
                color = CreamWhite,
            )
        }

        if (isSelected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(CreamWhite.copy(alpha = 0.25f)),
            ) {
                Text(text = "✓", fontSize = 16.sp, color = CreamWhite, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page Indicator Dots
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PageIndicatorDots(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        modifier              = modifier,
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage

            val dotWidth by animateDpAsState(
                targetValue   = if (isActive) 28.dp else 8.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessMedium,
                ),
                label = "dot_width_$index",
            )

            val dotColor by animateColorAsState(
                targetValue   = if (isActive) LeafGold else CreamWhite.copy(alpha = 0.38f),
                animationSpec = tween(300),
                label         = "dot_color_$index",
            )

            Box(
                modifier = Modifier
                    .width(dotWidth)
                    .height(8.dp)
                    .clip(PillShape)
                    .background(dotColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    LearnWithVelmorthTheme {
        OnboardingScreen(onComplete = { _, _, _ -> })
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OnboardingPageIndicatorPreview() {
    LearnWithVelmorthTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ForestDeep),
            contentAlignment = Alignment.Center,
        ) {
            PageIndicatorDots(pageCount = 3, currentPage = 1)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Leaf Particles Effect for Nature aesthetic
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LeafParticlesEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition("leaves")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Setup 10 falling leaves
    val leaves = remember {
        List(10) {
            val xPercent = kotlin.random.Random.nextFloat()
            val yOffsetStart = -100f - kotlin.random.Random.nextFloat() * 400f
            val speedMultiplier = 0.8f + kotlin.random.Random.nextFloat() * 1.2f
            val rotationSpeed = 120f + kotlin.random.Random.nextFloat() * 240f
            val sizeSp = 16f + kotlin.random.Random.nextFloat() * 14f  // plain Float, convert to sp at use-site
            val leafEmoji = listOf("🍃", "🌿", "🌱").random()

            object {
                val x = xPercent
                val yStart = yOffsetStart
                val speed = speedMultiplier
                val rotSpeed = rotationSpeed
                val emoji = leafEmoji
                val fontSize = sizeSp  // Float — will be converted to .sp at call site
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight

        leaves.forEach { leaf ->
            val currentY = leaf.yStart + (screenHeight.value + 300) * progress * leaf.speed
            val swing = kotlin.math.sin(progress * 4 * leaf.speed) * 30f
            val currentX = (screenWidth.value * leaf.x).dp + swing.dp

            if (currentY > 0 && currentY < screenHeight.value + 50) {
                Text(
                    text = leaf.emoji,
                    fontSize = leaf.fontSize.sp,
                    color = Color.Unspecified,
                    modifier = Modifier
                        .offset(x = currentX, y = currentY.dp)
                        .graphicsLayer {
                            rotationZ = progress * leaf.rotSpeed
                        }
                        .alpha(0.5f),
                )
            }
        }
    }
}

