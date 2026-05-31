package com.velmorth.app.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.velmorth.app.MainActivity
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.ui.lessons.LessonPlayerActivity
import com.velmorth.app.utils.LeafRewardManager
import java.util.Calendar

/**
 * Fragment that displays the home dashboard.
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
            (it as ComposeView).setContent {
                HomeScreenContent()
            }
        }
    }

    @Composable
    private fun HomeScreenContent() {
        val user = userRepository.getUser()
        val progress = lessonRepository.getProgress()
        val dailyGoalXp = prefsManager.dailyGoal
        val hasStarted = progress.completedLessons.isNotEmpty()

        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Premium status bar (Streak + Leaves)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Streak Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFCEADE)) // Light orange background
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔥", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.streak} days",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE76F51) // Streak Fire color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Leaves/Gold Chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE3F0E9)) // Light green background
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🍃", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.leaves}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D6A4F) // Primary Green
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Greeting
            Text(
                text = "$greeting, ${user.displayName}!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Daily Goal card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's XP Target",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "${user.xp} / $dailyGoalXp XP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D6A4F)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { xpProgressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = Color(0xFF52B788), // Accent green
                        trackColor = Color(0xFFE5E7EB)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val statusMsg = if (user.xp >= dailyGoalXp) {
                        "Daily goal completed! Keep going to build your streak!"
                    } else {
                        "Earn ${dailyGoalXp - user.xp} XP more to complete today's goal!"
                    }

                    Text(
                        text = statusMsg,
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mascot Companion Widget (Simple dynamic presentation)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F0E9)), // surface green
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB7E4C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🦉", fontSize = 36.sp) // Velmorth the Owl Mascot
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Velmorth",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B4332)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val speechText = if (user.xp == 0) {
                            "\"Welcome back! Let's embark on today's language lesson together!\""
                        } else {
                            "\"Splendid progress! You're getting closer to absolute mastery!\""
                        }
                        Text(
                            text = speechText,
                            fontSize = 13.sp,
                            color = Color(0xFF2D6A4F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = {
                    // Start next uncompleted lesson, or launch LessonPlayerActivity
                    val nextLessonId = progress.currentLesson
                    val nextLesson = lessonRepository.getLessonById(nextLessonId)
                    
                    if (nextLesson != null) {
                        val intent = Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                            putExtra("LESSON_ID", nextLesson.id)
                        }
                        startActivity(intent)
                    } else {
                        // All lessons completed or fallback to first
                        val firstLessonId = lessonRepository.getUnits().firstOrNull()?.lessons?.firstOrNull()?.id
                        if (firstLessonId != null) {
                            val intent = Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                putExtra("LESSON_ID", firstLessonId)
                            }
                            startActivity(intent)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (hasStarted) "Continue Learning" else "Start Learning",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Banner ad — shown only to non-premium users
            if (!user.isPremium) {
                Spacer(Modifier.height(16.dp))
                BannerAdView()
            }
        }
    }

    /** AdMob banner ad composable — uses test ad unit ID during development. */
    @Composable
    private fun BannerAdView() {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory  = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-8150181705727957/5179692569" // Production banner ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
