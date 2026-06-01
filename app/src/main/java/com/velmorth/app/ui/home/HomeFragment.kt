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
import java.util.Calendar

/**
 * Home screen — focused on learning progress.
 * Layout: greeting header → stats row → Continue Learning card
 * → Daily Streak card (with Collect button) → Current Course card → Banner ad
 */
class HomeFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var lessonRepository: LessonRepository
    private lateinit var prefsManager: PrefsManager

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
        val user            = userRepository.getUser()
        val progress        = lessonRepository.getProgress()
        val allLessons      = lessonRepository.getUnits().flatMap { it.lessons }
        val completedCount  = lessonRepository.getCompletedLessons().size
        val totalLessons    = allLessons.size
        val progressFraction = if (totalLessons > 0) completedCount.toFloat() / totalLessons else 0f
        val hasStarted      = progress.completedLessons.isNotEmpty()
        val selectedLang    = prefsManager.selectedLanguage

        // Streak card state (checked against Firestore on composition)
        var streakCollected by remember { mutableStateOf(false) }
        var currentStreak   by remember { mutableIntStateOf(user.streak) }
        var currentLeaves   by remember { mutableIntStateOf(user.leaves) }
        var streakLoading   by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            FirestoreProgressRepository.claimStreakCheckin { result, newStreak, newLeaves ->
                if (result == FirestoreProgressRepository.StreakCheckinResult.ALREADY_CLAIMED) {
                    streakCollected = true
                } else {
                    // CLAIMED or RESET — update local prefs too
                    prefsManager.streak = newStreak
                    prefsManager.leaves = newLeaves
                    currentStreak = newStreak
                    currentLeaves = newLeaves
                }
                if (newStreak > 0) currentStreak = newStreak
                if (newLeaves > 0) currentLeaves = newLeaves
                streakLoading = false
            }
        }

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
                displayName = user.displayName.ifBlank { "Learner" },
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
                StatCard(icon = "⭐", value = "${user.xp}", label = "XP", modifier = Modifier.weight(1f))
                StatCard(icon = "🍃", value = "$currentLeaves", label = "Leaves", modifier = Modifier.weight(1f))
                StatCard(icon = "🔥", value = "$currentStreak", label = "Streak", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Continue Learning Card ─────────────────────────────────────────
            ContinueLearningCard(
                hasStarted = hasStarted,
                langFlag   = langFlag,
                langName   = langDisplay,
                onClick    = {
                    val nextLessonId = progress.currentLesson
                    val nextLesson   = lessonRepository.getLessonById(nextLessonId)

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

            // ── Daily Streak Card ─────────────────────────────────────────────
            DailyStreakCard(
                streak          = currentStreak,
                isCollected     = streakCollected,
                isLoading       = streakLoading,
                onCollect       = {
                    streakLoading = true
                    FirestoreProgressRepository.claimStreakCheckin { result, newStreak, newLeaves ->
                        when (result) {
                            FirestoreProgressRepository.StreakCheckinResult.ALREADY_CLAIMED -> {
                                streakCollected = true
                            }
                            else -> {
                                prefsManager.streak = newStreak
                                prefsManager.leaves = newLeaves
                                currentStreak = newStreak
                                currentLeaves = newLeaves
                                streakCollected = true
                            }
                        }
                        streakLoading = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Current Course Card ───────────────────────────────────────────
            CurrentCourseCard(
                langFlag         = langFlag,
                langName         = langDisplay,
                progressFraction = progressFraction,
                completedCount   = completedCount,
                totalLessons     = totalLessons,
                onBrowseCourses  = {
                    (requireActivity() as? MainActivity)?.let { act ->
                        act.bottomNav.selectedItemId = 2
                    }
                }
            )

            // ── Banner Ad ─────────────────────────────────────────────────────
            if (!user.isPremium) {
                Spacer(Modifier.height(16.dp))
                BannerAdView()
            }

            Spacer(modifier = Modifier.height(20.dp))
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
        hasStarted: Boolean,
        langFlag: String,
        langName: String,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = langFlag, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (hasStarted) "Continue Learning" else "Start Learning",
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
                            text = if (hasStarted) "Resume Lesson →" else "Begin Now →",
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
    private fun DailyStreakCard(
        streak: Int,
        isCollected: Boolean,
        isLoading: Boolean,
        onCollect: () -> Unit
    ) {
        val stateLabel = when {
            isLoading   -> "Checking..."
            isCollected -> "Collected Today ✓"
            else        -> "Collect Daily Reward"
        }
        val stateBg = if (isCollected) Color(0xFF52B788) else Color(0xFFF4A261)

        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flame circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔥", fontSize = 30.sp)
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Daily Streak",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4332)
                    )
                    Text(
                        text = "$streak day${if (streak == 1) "" else "s"} in a row",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Reward: +5 🍃 leaves",
                        fontSize = 11.sp,
                        color = Color(0xFFF4A261),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(10.dp))

                Button(
                    onClick  = { if (!isCollected && !isLoading) onCollect() },
                    enabled  = !isCollected && !isLoading,
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = stateBg,
                        disabledContainerColor = if (isCollected) Color(0xFF52B788) else Color(0xFFD1D5DB)
                    ),
                    shape    = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = if (isCollected) "✓ Done" else "Collect",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CurrentCourseCard(
        langFlag: String,
        langName: String,
        progressFraction: Float,
        completedCount: Int,
        totalLessons: Int,
        onBrowseCourses: () -> Unit
    ) {
        val progressPercent = (progressFraction * 100).toInt()

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
                        Text(text = langFlag, fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Current Course",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = langName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B4332)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text = "$progressPercent%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2D6A4F),
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

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$completedCount of $totalLessons lessons",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    TextButton(
                        onClick = onBrowseCourses,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "Browse All →",
                            fontSize = 12.sp,
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
