package com.velmorth.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.velmorth.app.MainActivity
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.ui.lessons.LessonPlayerActivity
import com.velmorth.app.ui.profile.UserViewModel
import com.velmorth.app.ui.lessons.ProgressViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Home screen — focused on learning progress.
 * Layout: greeting header → stats row → Continue Learning card
 * → Daily Streak card (with Collect button) → Current Course card → Banner ad
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var lessonRepository: LessonRepository
    private lateinit var prefsManager: PrefsManager

    private val userViewModel: UserViewModel by viewModels()
    private val progressViewModel: ProgressViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userRepository = UserRepository(requireContext())
        lessonRepository = LessonRepository(requireContext())
        prefsManager = PrefsManager(requireContext())
        MobileAds.initialize(requireContext()) {}
        return ComposeView(requireContext()).apply {
            setContent { HomeScreenContent() }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let {
            (it as ComposeView).setContent { HomeScreenContent() }
        }
    }

    // ── Root composable ───────────────────────────────────────────────────────

    @Composable
    private fun HomeScreenContent() {
        val userState by userViewModel.userState.collectAsState()
        val progressState by progressViewModel.progressState.collectAsState()

        val allLessons = remember(userState.selectedLanguage) {
            lessonRepository.getUnits().flatMap { it.lessons }
        }
        val totalLessons = allLessons.size
        val completedCount = progressState.completedCount
        val progressFraction = if (totalLessons > 0) completedCount.toFloat() / totalLessons else 0f
        val progressPercent = (progressFraction * 100).toInt()

        val nextLessonId = lessonRepository.getProgress().currentLesson
        val nextLesson = remember(nextLessonId) {
            lessonRepository.getLessonById(nextLessonId)
        }
        val nextLessonTitle = nextLesson?.title ?: "No lessons left!"
        val selectedLang = userState.selectedLanguage.ifEmpty { prefsManager.selectedLanguage }

        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            else      -> "Good evening"
        }

        val langDisplay = selectedLang.replaceFirstChar { it.uppercase() }
        val langFlag = when (selectedLang.lowercase()) {
            "japanese" -> "🇯🇵"
            "french"   -> "🇫🇷"
            "sanskrit" -> "🇮🇳"
            "english"  -> "🇬🇧"
            else       -> "🌍"
        }

        // Streak checkin (runs in background and syncs reactively)
        LaunchedEffect(Unit) {
            FirestoreProgressRepository.claimStreakCheckin { result, newStreak, newLeaves ->
                if (result != FirestoreProgressRepository.StreakCheckinResult.ALREADY_CLAIMED) {
                    userViewModel.updateStreak(newStreak, newLeaves)
                }
            }
        }

        // Pull-to-refresh logic
        var isRefreshing by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        val recommendedActions = remember(progressState, userState, nextLessonTitle) {
            val list = mutableListOf<Map<String, String>>()
            if (nextLesson != null) {
                list.add(mapOf(
                    "title" to "Continue Course: ${nextLesson.title}",
                    "reason" to "Resume where you left off",
                    "actionType" to "lesson",
                    "payload" to nextLesson.id
                ))
            }
            if (progressState.reviewQueueSize > 0) {
                list.add(mapOf(
                    "title" to "Review Practice",
                    "reason" to "${progressState.reviewQueueSize} weak items waiting",
                    "actionType" to "review",
                    "payload" to ""
                ))
            } else {
                list.add(mapOf(
                    "title" to "Practice Session",
                    "reason" to "Strengthen your daily habit",
                    "actionType" to "review",
                    "payload" to ""
                ))
            }
            if (userState.leafBalance > 0) {
                list.add(mapOf(
                    "title" to "Visit Shop",
                    "reason" to "Spend your ${userState.leafBalance} leaves on power-ups",
                    "actionType" to "shop",
                    "payload" to ""
                ))
            }
            list
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                userViewModel.refreshFromFirestore()
                progressViewModel.refresh()
                scope.launch {
                    delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F4F1))
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Header ────────────────────────────────────────────────────────
                HomeHeader(
                    greeting    = greeting,
                    displayName = userState.name.ifBlank { "Learner" },
                    onAvatarClick = {
                        (requireActivity() as? MainActivity)?.let { act ->
                            act.bottomNav.selectedItemId = 5
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Stats Row ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(icon = "⭐", value = "${userState.xp}", label = "XP", modifier = Modifier.weight(1f))
                    StatCard(icon = "🌿", value = "${userState.leafBalance}", label = "Leaves", modifier = Modifier.weight(1f))
                    StatCard(icon = "🔥", value = "${userState.streak}", label = "Streak", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Continue Learning Card ─────────────────────────────────────────
                ContinueLearningCard(
                    langFlag = langFlag,
                    langName = langDisplay,
                    lessonTitle = nextLessonTitle,
                    progressPercent = progressPercent,
                    onClick = {
                        if (nextLesson != null) {
                            startActivity(
                                Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                    putExtra("LESSON_ID", nextLesson.id)
                                }
                            )
                        } else {
                            val firstId = lessonRepository.getUnits()
                                .firstOrNull()?.lessons?.firstOrNull()?.id
                            if (firstId != null) {
                                startActivity(
                                    Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                        putExtra("LESSON_ID", firstId)
                                    }
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Daily Goal Card ───────────────────────────────────────────────
                DailyGoalCard(
                    currentXp = progressState.dailyXpEarned,
                    targetXp = progressState.dailyGoal,
                    isCompleted = progressState.isDailyGoalComplete
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── Recommended Actions ───────────────────────────────────────────
                Text(
                    text = "Continue where you left off",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )

                recommendedActions.forEach { action ->
                    val title = action["title"] ?: ""
                    val reason = action["reason"] ?: ""
                    val type = action["actionType"] ?: ""
                    val payload = action["payload"] ?: ""

                    RecommendedActionTile(
                        title = title,
                        reason = reason,
                        onClick = {
                            when (type) {
                                "lesson" -> {
                                    startActivity(
                                        Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                            putExtra("LESSON_ID", payload)
                                        }
                                    )
                                }
                                "review" -> {
                                    (requireActivity() as? MainActivity)?.let { act ->
                                        act.bottomNav.selectedItemId = 3
                                    }
                                }
                                "shop" -> {
                                    (requireActivity() as? MainActivity)?.let { act ->
                                        act.bottomNav.selectedItemId = 4
                                    }
                                }
                            }
                        }
                    )
                }

                // ── Banner Ad ─────────────────────────────────────────────────────
                if (!userState.isPremium) {
                    Spacer(Modifier.height(16.dp))
                    BannerAdView()
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // ── Sub-composables ───────────────────────────────────────────────────────

    @Composable
    private fun HomeHeader(
        greeting: String,
        displayName: String,
        onAvatarClick: () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        fontSize = 13.sp,
                        color = Color(0xFF95D5B2)
                    )
                    Text(
                        text = displayName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Ready to learn today?",
                        fontSize = 13.sp,
                        color = Color(0xFFB7E4C7).copy(alpha = 0.8f)
                    )
                }

                // Avatar circle — tappable → Profile tab
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF52B788), Color(0xFF2D6A4F))
                            )
                        )
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                        .clickable { onAvatarClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }

    @Composable
    private fun StatCard(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
        Card(
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = icon, fontSize = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B4332))
                Text(text = label, fontSize = 11.sp, color = Color(0xFF6B7280))
            }
        }
    }

    @Composable
    private fun ContinueLearningCard(
        langFlag: String,
        langName: String,
        lessonTitle: String,
        progressPercent: Int,
        onClick: () -> Unit
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(3.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = langFlag, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Continue Learning",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$langName Course",
                                    fontSize = 13.sp,
                                    color = Color(0xFFB7E4C7)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF52B788).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "$progressPercent%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB7E4C7),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = lessonTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onClick,
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788)),
                        shape   = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Resume Lesson →",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun DailyGoalCard(
        currentXp: Int,
        targetXp: Int,
        isCompleted: Boolean
    ) {
        val progressFraction = if (targetXp > 0) (currentXp.toFloat() / targetXp).coerceIn(0f, 1f) else 0f
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Daily Goal",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B4332)
                            )
                            Text(
                                text = if (isCompleted) "Goal Met! 🎉" else "Keep going!",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCompleted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "$currentXp / $targetXp XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCompleted) Color(0xFF2D6A4F) else Color(0xFFF4A261),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE5E7EB))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF52B788), Color(0xFF2D6A4F))
                                )
                            )
                    )
                }
            }
        }
    }

    @Composable
    private fun RecommendedActionTile(
        title: String,
        reason: String,
        onClick: () -> Unit
    ) {
        Card(
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFF2D6A4F),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = reason,
                        fontSize = 12.sp,
                        color = Color(0xFF888888)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFBBBBBB),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    private fun BannerAdView() {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory  = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-8150181705727957/5179692569"
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
