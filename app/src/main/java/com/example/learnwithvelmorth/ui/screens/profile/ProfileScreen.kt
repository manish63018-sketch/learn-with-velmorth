package com.example.learnwithvelmorth.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnwithvelmorth.domain.repository.UserRepository
import com.example.learnwithvelmorth.domain.repository.LessonRepository
import com.example.learnwithvelmorth.domain.model.LessonStatus
import com.example.learnwithvelmorth.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ── Data models ───────────────────────────────────────────────────────────────

data class Badge(val emoji: String, val label: String, val unlocked: Boolean)

data class ProfileUiState(
    val isLoading: Boolean       = false,
    val avatarEmoji: String      = "🌿",
    val userName: String         = "Forest Learner",
    val joinedDaysAgo: Int       = 0,
    val streak: Int              = 0,
    val totalXp: Int             = 0,
    val lessonsCompleted: Int    = 0,
    val currentLevelXp: Int      = 0,
    val levelMaxXp: Int          = 100,
    val currentLevel: Int        = 1,
    val learningLanguage: String = "Spanish 🇪🇸",
    val languageProgress: Float  = 0f,
    val badges: List<Badge>      = emptyList(),
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = userRepository.getUser()
        .flatMapLatest { user ->
            if (user != null) {
                lessonRepository.getLessonsForLanguage(user.selectedLanguageId).map { lessons ->
                    val completedCount = lessons.count { it.status == LessonStatus.COMPLETED }
                    val progressFraction = if (lessons.isNotEmpty()) {
                        completedCount.toFloat() / lessons.size
                    } else {
                        0f
                    }

                    val level = (user.totalXp / 100) + 1
                    val currentXp = user.totalXp % 100

                    val daysAgo = getJoinedDaysAgo(user.joinedDate)

                    val languageName = when (user.selectedLanguageId) {
                        "es" -> "Spanish 🇪🇸"
                        "fr" -> "French 🇫🇷"
                        else -> "Spanish 🇪🇸"
                    }

                    val badgesList = listOf(
                        Badge("🌱", "First Lesson", completedCount > 0),
                        Badge("🔥", "3-Day Streak", user.currentStreak >= 3),
                        Badge("⭐", "10 Lessons", completedCount >= 10),
                        Badge("🍃", "Leaf Collector", user.leafBalance >= 100),
                    )

                    ProfileUiState(
                        isLoading = false,
                        avatarEmoji = user.avatarEmoji,
                        userName = user.name,
                        joinedDaysAgo = daysAgo,
                        streak = user.currentStreak,
                        totalXp = user.totalXp,
                        lessonsCompleted = completedCount,
                        currentLevelXp = currentXp,
                        levelMaxXp = 100,
                        currentLevel = level,
                        learningLanguage = languageName,
                        languageProgress = progressFraction,
                        badges = badgesList,
                    )
                }
            } else {
                flowOf(ProfileUiState())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState(isLoading = true)
        )

    private fun getJoinedDaysAgo(joinedDateStr: String?): Int {
        if (joinedDateStr.isNullOrBlank()) return 0
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val joinedDate = sdf.parse(joinedDateStr) ?: return 0
            val diffInMillis = Date().time - joinedDate.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            diffInDays.toInt().coerceAtLeast(0)
        } catch (e: java.lang.Exception) {
            0
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val forestDeep = Color(0xFF1B4332)
    val mossGreen  = Color(0xFF40916C)
    val creamWhite = Color(0xFFF1E8D0)
    val leafGold   = Color(0xFFD4A017)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "Profile",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairFamily,
                            fontWeight = FontWeight.Bold,
                            color      = creamWhite,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint               = creamWhite,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector        = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint               = creamWhite,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = forestDeep,
                ),
            )
        },
        containerColor = creamWhite,
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {

            // ── Header gradient section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(forestDeep, mossGreen),
                        )
                    ),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(leafGold.copy(alpha = 0.3f), Color.Transparent)
                                )
                            )
                            .border(3.dp, leafGold, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = state.avatarEmoji, fontSize = 40.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = state.userName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairFamily,
                            fontWeight = FontWeight.Bold,
                            color      = creamWhite,
                        ),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = "Joined ${state.joinedDaysAgo} days ago",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = NunitoFamily,
                            color      = creamWhite.copy(alpha = 0.65f),
                        ),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji    = "🔥",
                    value    = "${state.streak}",
                    label    = "Streak",
                    accent   = Color(0xFFFF6B35),
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji    = "⭐",
                    value    = "${state.totalXp}",
                    label    = "Total XP",
                    accent   = leafGold,
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    emoji    = "✅",
                    value    = "${state.lessonsCompleted}",
                    label    = "Lessons",
                    accent   = mossGreen,
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── XP Progress card
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Row(
                    modifier       = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "Level ${state.currentLevel}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlayfairFamily,
                            fontWeight = FontWeight.Bold,
                            color      = forestDeep,
                        ),
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text  = "${state.currentLevelXp} / ${state.levelMaxXp} XP",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = NunitoFamily,
                            color      = Color.Gray,
                        ),
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress            = state.currentLevelXp.toFloat() / state.levelMaxXp,
                    modifier            = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50)),
                    color               = mossGreen,
                    trackColor          = mossGreen.copy(alpha = 0.15f),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${state.levelMaxXp - state.currentLevelXp} XP to Level ${state.currentLevel + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = NunitoFamily,
                        color      = Color.Gray,
                    ),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Language card
            SectionCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text  = "Currently Learning",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = NunitoFamily,
                        color      = Color.Gray,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier       = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text  = "Learning ${state.learningLanguage}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            color      = forestDeep,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text  = "${(state.languageProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            color      = mossGreen,
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress   = state.languageProgress,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color      = leafGold,
                    trackColor = leafGold.copy(alpha = 0.15f),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Achievement badges
            Text(
                text     = "Achievements",
                modifier = Modifier.padding(horizontal = 16.dp),
                style    = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlayfairFamily,
                    fontWeight = FontWeight.Bold,
                    color      = forestDeep,
                ),
            )

            Spacer(Modifier.height(10.dp))

            LazyRow(
                contentPadding       = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.badges) { badge ->
                    BadgeChip(badge = badge, leafGold = leafGold, mossGreen = mossGreen)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Edit Profile button
            OutlinedButton(
                onClick  = { /* navigate to edit profile */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(50.dp),
                shape    = RoundedCornerShape(50),
                border   = BorderStroke(2.dp, forestDeep),
            ) {
                Text(
                    text  = "Edit Profile",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = NunitoFamily,
                        fontWeight = FontWeight.Bold,
                        color      = forestDeep,
                    ),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    modifier: Modifier,
    emoji: String,
    value: String,
    label: String,
    accent: Color,
) {
    val forestDeep = Color(0xFF1B4332)

    Column(
        modifier           = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.5.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text      = value,
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.titleLarge.copy(
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
                color      = accent,
                fontSize   = 22.sp,
            ),
        )
        Text(
            text      = label,
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.labelSmall.copy(
                fontFamily = NunitoFamily,
                color      = Color.Gray,
            ),
        )
    }
}

// ── Section card ──────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        content = content,
    )
}

// ── Badge chip ────────────────────────────────────────────────────────────────

@Composable
private fun BadgeChip(badge: Badge, leafGold: Color, mossGreen: Color) {
    val bg     = if (badge.unlocked) mossGreen.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.08f)
    val border = if (badge.unlocked) mossGreen.copy(alpha = 0.4f)  else Color.Gray.copy(alpha = 0.2f)
    val textColor = if (badge.unlocked) Color(0xFF1B4332) else Color.Gray

    Column(
        modifier           = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text     = badge.emoji,
            fontSize = 28.sp,
            color    = if (badge.unlocked) Color.Unspecified else Color.Gray.copy(alpha = 0.4f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text      = badge.label,
            textAlign = TextAlign.Center,
            style     = MaterialTheme.typography.labelSmall.copy(
                fontFamily = NunitoFamily,
                fontWeight = FontWeight.SemiBold,
                color      = textColor,
                fontSize   = 11.sp,
            ),
        )
        if (!badge.unlocked) {
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Locked",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = NunitoFamily,
                    color      = Color.Gray.copy(alpha = 0.5f),
                    fontSize   = 9.sp,
                ),
            )
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    LearnWithVelmorthTheme {
        // Inline preview without ViewModel
        val state = ProfileUiState()
        val forestDeep = Color(0xFF1B4332)
        val mossGreen  = Color(0xFF40916C)
        val creamWhite = Color(0xFFF1E8D0)
        val leafGold   = Color(0xFFD4A017)

        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Text(
                            "Profile",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = PlayfairFamily,
                                fontWeight = FontWeight.Bold,
                                color      = creamWhite,
                            ),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.ArrowBack, null, tint = creamWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = forestDeep),
                )
            },
            containerColor = creamWhite,
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Brush.verticalGradient(listOf(forestDeep, mossGreen))),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(3.dp, leafGold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text(state.avatarEmoji, fontSize = 40.sp) }
                        Spacer(Modifier.height(8.dp))
                        Text(state.userName, color = creamWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Joined ${state.joinedDaysAgo} days ago", color = creamWhite.copy(alpha = 0.65f), fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(Modifier.weight(1f), "🔥", "${state.streak}", "Streak", Color(0xFFFF6B35))
                    StatCard(Modifier.weight(1f), "⭐", "${state.totalXp}", "Total XP", leafGold)
                    StatCard(Modifier.weight(1f), "✅", "${state.lessonsCompleted}", "Lessons", mossGreen)
                }
                Spacer(Modifier.height(20.dp))

                LazyRow(
                    contentPadding        = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.badges) { badge ->
                        BadgeChip(badge, leafGold, mossGreen)
                    }
                }
                Spacer(Modifier.height(28.dp))
                OutlinedButton(
                    onClick  = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    shape    = RoundedCornerShape(50),
                    border   = BorderStroke(2.dp, forestDeep),
                ) {
                    Text("Edit Profile", color = forestDeep, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
