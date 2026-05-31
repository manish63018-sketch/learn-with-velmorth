package com.velmorth.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.velmorth.app.utils.LeafRewardManager
import java.util.Calendar

private data class LanguageCard(
    val key: String,
    val displayName: String,
    val flag: String,
    val tagline: String,
    val gradient: List<Color>
)

private val LANGUAGE_CARDS = listOf(
    LanguageCard(
        key = "japanese",
        displayName = "Japanese",
        flag = "🇯🇵",
        tagline = "Hiragana · Katakana · Kanji",
        gradient = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
    ),
    LanguageCard(
        key = "french",
        displayName = "French",
        flag = "🇫🇷",
        tagline = "Greetings · Grammar · Culture",
        gradient = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
    ),
    LanguageCard(
        key = "sanskrit",
        displayName = "Sanskrit",
        flag = "🇮🇳",
        tagline = "Devanagari · Mantras · Roots",
        gradient = listOf(Color(0xFFE65100), Color(0xFFBF360C))
    ),
    LanguageCard(
        key = "english",
        displayName = "English",
        flag = "🇬🇧",
        tagline = "Grammar · Vocabulary · Idioms",
        gradient = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))
    )
)

/**
 * Fragment that displays the home dashboard with language cards and daily progress.
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

        // Initialise AdMob once (idempotent)
        MobileAds.initialize(requireContext()) {}

        return ComposeView(requireContext()).apply {
            setContent { HomeScreenContent() }
        }
    }

    override fun onResume() {
        super.onResume()
        // Daily login reward — grants +5 leaves once per calendar day
        val loginLeaves = LeafRewardManager.claimDailyLoginReward(prefsManager)
        if (loginLeaves > 0) {
            android.widget.Toast.makeText(
                requireContext(),
                "Daily login bonus! +$loginLeaves 🍃",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        // Force refresh UI content when returning
        view?.let {
            (it as ComposeView).setContent { HomeScreenContent() }
        }
    }

    @Composable
    private fun HomeScreenContent() {
        val user = userRepository.getUser()
        val progress = lessonRepository.getProgress()
        val dailyGoalXp = prefsManager.dailyGoal
        val hasStarted = progress.completedLessons.isNotEmpty()
        val selectedLanguage = prefsManager.selectedLanguage

        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "Good morning"
            in 12..16 -> "Good afternoon"
            else      -> "Good evening"
        }

        val xpProgressFraction = if (dailyGoalXp > 0) {
            (user.xp.toFloat() / dailyGoalXp.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F5EE))
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Green Hero Header ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    // Top row: greeting + stat chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$greeting,",
                                fontSize = 14.sp,
                                color = Color(0xFF95D5B2)
                            )
                            Text(
                                text = user.displayName.ifBlank { "Learner" } + "!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Streak chip
                            StatChip(icon = "🔥", value = "${user.streak}", label = "streak")
                            // Leaves chip
                            StatChip(icon = "🍃", value = "${user.leaves}", label = "leaves")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP + Level row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐ ${user.xp} XP  ·  Level ${user.level}",
                            fontSize = 13.sp,
                            color = Color(0xFFB7E4C7),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${user.xp}/$dailyGoalXp today",
                            fontSize = 12.sp,
                            color = Color(0xFF95D5B2)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Daily XP progress bar
                    LinearProgressIndicator(
                        progress = { xpProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color(0xFF52B788),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Mascot Companion Card ──────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F0E9)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB7E4C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🦉", fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Velmorth",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B4332)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val speechText = if (user.xp == 0) {
                            "\"Welcome back! Let's embark on today's lesson!\""
                        } else {
                            "\"Splendid progress! You're getting closer to mastery!\""
                        }
                        Text(
                            text = speechText,
                            fontSize = 12.sp,
                            color = Color(0xFF2D6A4F),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Language Cards Section ─────────────────────────────────────────
            Text(
                text = "🌍  Choose Your Language",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2×2 grid of language cards
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LANGUAGE_CARDS.chunked(2).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowCards.forEach { card ->
                            LanguageCardItem(
                                card = card,
                                isSelected = selectedLanguage == card.key,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    prefsManager.selectedLanguage = card.key
                                    // Navigate to Lessons tab in MainActivity
                                    (requireActivity() as? MainActivity)
                                        ?.let { act ->
                                            val nav = act.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(1002)
                                            nav?.selectedItemId = 2
                                        }
                                }
                            )
                        }
                        // Fill gap if odd count
                        if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Continue Learning Button ───────────────────────────────────────
            Button(
                onClick = {
                    val nextLessonId = progress.currentLesson
                    val nextLesson = lessonRepository.getLessonById(nextLessonId)

                    if (nextLesson != null) {
                        startActivity(
                            Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                putExtra("LESSON_ID", nextLesson.id)
                            }
                        )
                    } else {
                        val firstLessonId = lessonRepository.getUnits()
                            .firstOrNull()?.lessons?.firstOrNull()?.id
                        if (firstLessonId != null) {
                            startActivity(
                                Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                    putExtra("LESSON_ID", firstLessonId)
                                }
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
            ) {
                Text(
                    text = if (hasStarted) "Continue Learning" else "Start Learning",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Banner ad — shown only to non-premium users
            if (!user.isPremium) {
                Spacer(Modifier.height(16.dp))
                BannerAdView()
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    @Composable
    private fun StatChip(icon: String, value: String, label: String) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    @Composable
    private fun LanguageCardItem(
        card: LanguageCard,
        isSelected: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        val borderColor = if (isSelected) Color(0xFF52B788) else Color.Transparent
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp),
            modifier = modifier
                .clickable { onClick() }
                .then(
                    if (isSelected) Modifier.padding(1.dp) else Modifier
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(card.gradient)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(text = card.flag, fontSize = 28.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = card.tagline,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        lineHeight = 14.sp
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF52B788)
                        ) {
                            Text(
                                "✓ Active",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    /** AdMob banner ad composable */
    @Composable
    private fun BannerAdView() {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-8150181705727957/5179692569"
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
