package com.example.learnwithvelmorth.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learnwithvelmorth.theme.*
import com.example.learnwithvelmorth.ui.components.LeafBalanceChip
import com.example.learnwithvelmorth.ui.components.StreakCounter
import com.example.learnwithvelmorth.ui.components.XPProgressBar
import com.example.learnwithvelmorth.ui.components.AnimatedMascot
import com.example.learnwithvelmorth.ui.components.AnimatedMascotContent
import com.example.learnwithvelmorth.ui.components.VelmorthCharacterViewModel
import com.example.learnwithvelmorth.domain.VelmorthTrigger
import com.example.learnwithvelmorth.domain.VelmorthEmotion
import com.example.learnwithvelmorth.domain.repository.UserRepository
import com.example.learnwithvelmorth.domain.repository.LessonRepository
import com.example.learnwithvelmorth.domain.model.LessonStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

// ============================================================
// UI State
// ============================================================

data class HomeUiState(
    val userName: String = "Learner",
    val greeting: String = "Good morning 🌅",
    val streak: Int = 0,
    val leafBalance: Int = 0,
    val totalXp: Int = 0,
    val maxXp: Int = 100,
    val dailyProgress: Float = 0f,         // 0f–1f fraction
    val dailyLessonsDone: Int = 0,
    val dailyLessonsGoal: Int = 5,
    val currentLessonTitle: String = "Greetings",
    val currentLessonId: String = "es_ch1_l1",
    val velmorthTip: String = "Consistency beats intensity. Even 5 minutes a day builds lasting fluency! 🌱",
    val growthPoints: Int = 0,
    val velmorthMood: String = "HAPPY",
    val isReturningUser: Boolean = false,
)

// ============================================================
// ViewModel
// ============================================================

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
) : ViewModel() {

    val homeState: StateFlow<HomeUiState> = userRepository.getUser()
        .flatMapLatest { user ->
            if (user != null) {
                lessonRepository.getLessonsForLanguage(user.selectedLanguageId).map { lessons ->
                    val activeLesson = lessons.sortedBy { it.orderIndex }.firstOrNull { it.status == LessonStatus.AVAILABLE }
                        ?: lessons.sortedBy { it.orderIndex }.firstOrNull { it.status == LessonStatus.COMPLETED }
                        ?: lessons.firstOrNull()

                    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                        in 5..11  -> "Good morning 🌅"
                        in 12..17 -> "Good afternoon ☀️"
                        else      -> "Good evening 🌙"
                    }

                    val tip = when (user.velmorthMood.uppercase()) {
                        "SLEEPY", "TIRED" -> "Velmorth looks a bit sleepy... Maybe pet or feed him to cheer him up? 🦦💤"
                        "HUNGRY" -> "I am hungry! 🦦 Can you feed me some leaves? 🍃"
                        "EXCITED" -> "Velmorth is super excited to learn with you today! 🦦✨ Let's do a lesson!"
                        "SAD" -> "Velmorth feels a bit lonely. 🦦😢 A quick pet will make him happy!"
                        else -> "Consistency is key! Every seed you plant today will become a mighty tree tomorrow. 🌱"
                    }

                    val completedCount = lessons.count { it.status == LessonStatus.COMPLETED }
                    val isReturning = completedCount > 0

                    HomeUiState(
                        userName = user.name,
                        greeting = greeting,
                        streak = user.currentStreak,
                        leafBalance = user.leafBalance,
                        totalXp = user.totalXp,
                        maxXp = 100, // standard level xp ceiling
                        dailyProgress = (user.totalXp % 100) / 100f,
                        dailyLessonsDone = completedCount,
                        dailyLessonsGoal = user.dailyGoalMinutes / 2, // assume 2 min per lesson
                        currentLessonTitle = activeLesson?.title ?: "No lessons available",
                        currentLessonId = activeLesson?.id ?: "",
                        velmorthTip = tip,
                        growthPoints = user.growthPoints,
                        velmorthMood = user.velmorthMood,
                        isReturningUser = isReturning,
                    )
                }
            } else {
                flowOf(HomeUiState())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState()
        )

    fun feedVelmorth() {
        viewModelScope.launch {
            val user = userRepository.getUser().first() ?: return@launch
            if (user.leafBalance >= 5) {
                userRepository.updateLeafBalance("local_user", -5)
                userRepository.updateGrowthPoints("local_user", 10)
                userRepository.updateVelmorthMood("local_user", "EXCITED")
            }
        }
    }

    fun petVelmorth() {
        viewModelScope.launch {
            userRepository.updateGrowthPoints("local_user", 2)
            userRepository.updateVelmorthMood("local_user", "HAPPY")
        }
    }
}

// ============================================================
// Screen Entry Point
// ============================================================

@Composable
fun HomeScreen(
    onStartLesson: () -> Unit,
    onNavigateToProfile: () -> Unit,
    leafBalance: Int,
    streak: Int,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    characterViewModel: VelmorthCharacterViewModel = hiltViewModel(),
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()

    // Fire time-of-day greeting on first screen entry
    LaunchedEffect(Unit) {
        characterViewModel.greetUser(speakAloud = true)
    }

    HomeScreenContent(
        state               = state,
        onStartLesson       = onStartLesson,
        onNavigateToProfile = onNavigateToProfile,
        onFeedVelmorth      = {
            viewModel.feedVelmorth()
            characterViewModel.fireEvent(VelmorthTrigger.LEAF_EARNED)
        },
        onPetVelmorth       = {
            viewModel.petVelmorth()
            characterViewModel.fireEvent(VelmorthTrigger.MOTIVATION_RANDOM)
        },
        characterViewModel  = characterViewModel,
        modifier            = modifier,
    )
}

// ============================================================
// Screen Content (stateless, testable)
// ============================================================

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onStartLesson: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onFeedVelmorth: () -> Unit,
    onPetVelmorth: () -> Unit,
    characterViewModel: VelmorthCharacterViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        // ── Top Header ─────────────────────────────────────
        item {
            HomeHeader(
                greeting        = state.greeting,
                userName        = state.userName,
                streak          = state.streak,
                leafBalance     = state.leafBalance,
                mood            = state.velmorthMood,
                onNavigateToProfile = onNavigateToProfile,
            )
        }

        // ── Today's Goal Card ──────────────────────────────
        item {
            TodayGoalCard(
                modifier            = Modifier.padding(horizontal = 20.dp),
                totalXp             = state.totalXp,
                maxXp               = state.maxXp,
                dailyLessonsDone    = state.dailyLessonsDone,
                dailyLessonsGoal    = state.dailyLessonsGoal,
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Companion Box Card ─────────────────────────────
        item {
            CompanionCard(
                mood = state.velmorthMood,
                growthPoints = state.growthPoints,
                leafBalance = state.leafBalance,
                tip = state.velmorthTip,
                onFeed = onFeedVelmorth,
                onPet = onPetVelmorth,
                characterViewModel = characterViewModel,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Continue Learning Card ─────────────────────────
        item {
            ContinueLearningCard(
                modifier           = Modifier.padding(horizontal = 20.dp),
                lessonTitle        = state.currentLessonTitle,
                isReturning        = state.isReturningUser,
                onStartLesson      = onStartLesson,
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Streak Section ─────────────────────────────────
        item {
            StreakSection(
                modifier = Modifier.padding(horizontal = 20.dp),
                streak   = state.streak,
            )
        }
    }
}

// ============================================================
// Sub-components
// ============================================================

@Composable
private fun HomeHeader(
    greeting: String,
    userName: String,
    streak: Int,
    leafBalance: Int,
    mood: String,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bouncing mascot animation
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_bounce")
    val mascotOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = -8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "mascot_y",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(ForestDeep, ForestMid),
                )
            )
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
    ) {
        Column {
            // Chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                StreakCounter(streak = streak, compact = true)
                Spacer(Modifier.size(8.dp))
                LeafBalanceChip(balance = leafBalance, compact = true)
            }

            Spacer(Modifier.height(12.dp))

            // Greeting + mascot
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = greeting,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontFamily = NunitoFamily,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = userName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontFamily = PlayfairFamily,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Mascot header
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .offset(y = mascotOffsetY.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedMascotContent(
                        emotion = VelmorthEmotion.fromString(mood),
                        size = 38.dp,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayGoalCard(
    totalXp: Int,
    maxXp: Int,
    dailyLessonsDone: Int,
    dailyLessonsGoal: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-16).dp)
            .shadow(elevation = 6.dp, shape = ForestCardShape)
            .clip(ForestCardShape)
            .background(WarmWhite)
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    text  = "Today's Goal 🎯",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text  = "$dailyLessonsDone/$dailyLessonsGoal lessons done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            XPProgressBar(currentXp = totalXp, maxXp = maxXp)
        }
    }
}

@Composable
private fun ContinueLearningCard(
    lessonTitle: String,
    isReturning: Boolean,
    onStartLesson: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = ForestCardShape)
            .clip(ForestCardShape)
            .background(
                Brush.linearGradient(
                    listOf(MossGreen, ForestMid)
                )
            )
            .padding(20.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text  = if (isReturning) "Continue Learning 📚" else "Start Learning 📚",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontFamily = NunitoFamily,
            )
            Text(
                text  = lessonTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onStartLesson,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = WarmWhite,
                    contentColor   = ForestDeep,
                ),
                shape = MaterialTheme.shapes.extraLarge,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(
                    text  = if (isReturning) "Resume →" else "Start →",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = ForestDeep,
                )
            }
        }
    }
}

@Composable
private fun StreakSection(
    streak: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ForestCardShape)
            .background(SunriseOrange.copy(alpha = 0.10f))
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(
                    text  = "Day Streak 🔥",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "Keep it going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(SunriseOrange.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = "$streak",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color    = SunriseOrange,
                    fontFamily = PlayfairFamily,
                )
            }
        }
    }
}

@Composable
private fun CompanionCard(
    mood: String,
    growthPoints: Int,
    leafBalance: Int,
    tip: String,
    onFeed: () -> Unit,
    onPet: () -> Unit,
    characterViewModel: VelmorthCharacterViewModel,
    modifier: Modifier = Modifier,
) {
    val level = (growthPoints / 100) + 1
    val progress = (growthPoints % 100) / 100f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = ForestCardShape)
            .clip(ForestCardShape)
            .background(WarmWhite)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, ForestCardShape)
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Companion Garden 🦦",
                    style = MaterialTheme.typography.titleMedium,
                    color = ForestDeep,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = PlayfairFamily,
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MossGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Lv. $level",
                        style = MaterialTheme.typography.labelMedium,
                        color = ForestMid,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // ── Full animated mascot with speech bubble ──────
            AnimatedMascot(
                viewModel = characterViewModel,
                size      = 110.dp,
                modifier  = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Vine Growth progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMid
                    )
                    Text(
                        text = "${growthPoints % 100}/100 GP",
                        style = MaterialTheme.typography.labelSmall,
                        color = ForestMid,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(FogGreen)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(listOf(MossGreen, LeafGreen))
                            )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPet,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FogGreen,
                        contentColor = ForestDeep
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "👋 Pet",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onFeed,
                    enabled = leafBalance >= 5,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeafGold,
                        contentColor = ForestDeep,
                        disabledContainerColor = Color.LightGray
                    ),
                    shape = CircleShape
                ) {
                    Text(
                        text = "🍃 Feed (5)",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// ============================================================
// Previews
// ============================================================

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun HomeScreenPreview() {
    LearnWithVelmorthTheme {
        HomeScreenContent(
            state = HomeUiState(
                userName         = "Alex",
                greeting         = "Good afternoon ☀️",
                streak           = 7,
                leafBalance      = 240,
                totalXp          = 320,
                maxXp            = 500,
                dailyProgress    = 0.6f,
                dailyLessonsDone = 3,
                dailyLessonsGoal = 5,
                currentLessonTitle = "Chapter 2: Colors & Shapes",
                velmorthTip = "Try repeating words out loud — speaking activates deeper memory pathways! 🗣️",
            ),
            onStartLesson       = {},
            onNavigateToProfile = {},
            onFeedVelmorth      = {},
            onPetVelmorth       = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Dark")
@Composable
fun HomeScreenDarkPreview() {
    LearnWithVelmorthTheme(darkTheme = true) {
        HomeScreenContent(
            state = HomeUiState(
                userName         = "Alex",
                greeting         = "Good evening 🌙",
                streak           = 12,
                leafBalance      = 580,
                totalXp          = 450,
                maxXp            = 500,
                dailyProgress    = 0.9f,
                dailyLessonsDone = 4,
                dailyLessonsGoal = 5,
                currentLessonTitle = "Chapter 3: Family & Home",
                velmorthTip = "Consistency beats intensity. Even 5 minutes a day builds lasting fluency! 🌱",
            ),
            onStartLesson       = {},
            onNavigateToProfile = {},
            onFeedVelmorth      = {},
            onPetVelmorth       = {},
        )
    }
}
