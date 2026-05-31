package com.velmorth.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.ui.premium.PremiumActivity
import com.velmorth.app.ui.settings.SettingsActivity

// ── Brand palette ─────────────────────────────────────────────────────────────
private val DarkGreen    = Color(0xFF1B4332)
private val PrimaryGreen = Color(0xFF2D6A4F)
private val AccentGreen  = Color(0xFF52B788)
private val LightGreen   = Color(0xFFB7E4C7)
private val BgCream      = Color(0xFFF8F5EE)
private val CardWhite    = Color(0xFFFFFFFF)
private val TextDark     = Color(0xFF1C1C1E)
private val TextMuted    = Color(0xFF6B7280)
private val GoldXP       = Color(0xFFF4A261)
private val GoldBadge    = Color(0xFFD4AC0D)

/**
 * Full-featured Profile screen:
 *  • Hero header with avatar, name, @username, premium badge
 *  • Quick-stat pills: 🔥 Streak · 🌿 Leaves · ⭐ XP
 *  • Edit Profile & Settings action buttons
 *  • My Progress card with animated progress bar
 *  • Badges / Achievements grid
 *  • Joined date footer
 *  • Premium upsell banner (free users only)
 */
class ProfileFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var lessonRepository: LessonRepository
    private lateinit var prefsManager: PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userRepository   = UserRepository(requireContext())
        lessonRepository = LessonRepository(requireContext())
        prefsManager     = PrefsManager(requireContext())

        return ComposeView(requireContext()).apply {
            setContent { ProfileScreen() }
        }
    }

    override fun onResume() {
        super.onResume()
        (view as? ComposeView)?.setContent { ProfileScreen() }
    }

    // ── Root screen ───────────────────────────────────────────────────────────

    @Composable
    private fun ProfileScreen() {
        val user              = userRepository.getUser()
        val allLessons        = lessonRepository.getUnits().flatMap { it.lessons }
        val completedCount    = lessonRepository.getCompletedLessons().size
        val totalLessons      = allLessons.size
        val progressFraction  = if (totalLessons > 0) completedCount.toFloat() / totalLessons else 0f
        val progressPercent   = (progressFraction * 100).toInt()

        val username          = prefsManager.username.ifBlank {
            user.displayName.lowercase().replace(" ", "")
        }
        val languageLabel     = prefsManager.selectedLanguage
            .replaceFirstChar { it.uppercase() }
        val languageFlag      = languageToFlag(prefsManager.selectedLanguage)

        val earnedBadges      = buildAchievements(user, completedCount)
        val earnedCount       = earnedBadges.count { it.unlocked }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCream)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero Header ───────────────────────────────────────────────────
            HeroHeader(
                displayName = user.displayName,
                username    = username,
                isPremium   = user.isPremium,
                streak      = user.streak,
                leaves      = user.leaves,
                xp          = user.xp,
                joinedAt    = user.joinedAt,
                onSettings  = {
                    startActivity(Intent(requireContext(), SettingsActivity::class.java))
                },
                onEditProfile = {
                    startActivity(Intent(requireContext(), EditProfileActivity::class.java))
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── My Progress ───────────────────────────────────────────────────
            SectionLabel("📊  My Progress")
            ProgressCard(
                language        = "$languageFlag $languageLabel",
                progressFraction = progressFraction,
                progressPercent  = progressPercent,
                completedCount   = completedCount,
                totalLessons     = totalLessons
            )

            Spacer(Modifier.height(20.dp))

            // ── Badges ────────────────────────────────────────────────────────
            SectionLabel("🏆  Badges earned: $earnedCount")
            BadgesGrid(earnedBadges)

            Spacer(Modifier.height(20.dp))

            // ── Joined date ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null,
                    tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Joined: ${user.joinedAt}", fontSize = 13.sp, color = TextMuted)
            }

            // ── Premium upsell banner ─────────────────────────────────────────
            if (!user.isPremium) {
                Spacer(Modifier.height(20.dp))
                PremiumBanner {
                    startActivity(Intent(requireContext(), PremiumActivity::class.java))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Hero Header ───────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(
    displayName   : String,
    username      : String,
    isPremium     : Boolean,
    streak        : Int,
    leaves        : Int,
    xp            : Int,
    joinedAt      : String,
    onSettings    : () -> Unit,
    onEditProfile : () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkGreen, PrimaryGreen, Color(0xFF3D8B68))
                )
            )
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar row: title + settings gear
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Profile",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
                IconButton(onClick = onSettings) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint               = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Avatar circle with edit icon overlay
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(LightGreen, AccentGreen))
                        )
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = displayName.take(1).uppercase(),
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = DarkGreen
                    )
                }
                // Edit pencil badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GoldXP)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit",
                        tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Name + premium badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = displayName,
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = GoldBadge
                    ) {
                        Text(
                            text     = "👑 PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Username
            Text(
                text      = "@$username",
                fontSize  = 14.sp,
                color     = LightGreen.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(20.dp))

            // ── Stat pills row ────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                StatPill(icon = "🔥", label = "Streak", value = "$streak days")
                StatPill(icon = "🌿", label = "Leaves",  value = "$leaves")
                StatPill(icon = "⭐", label = "XP",      value = "$xp")
            }

            Spacer(Modifier.height(18.dp))

            // ── Edit Profile button ───────────────────────────────────────────
            Button(
                onClick = onEditProfile,
                colors  = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape   = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null,
                    tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            }
        }
    }
}

// ── Stat pill ─────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(icon: String, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = label, fontSize = 11.sp, color = LightGreen.copy(alpha = 0.8f))
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 16.sp,
        fontWeight = FontWeight.Bold,
        color      = DarkGreen,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(8.dp))
}

// ── My Progress card ──────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(
    language         : String,
    progressFraction : Float,
    progressPercent  : Int,
    completedCount   : Int,
    totalLessons     : Int
) {
    // Animate bar on first composition
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progressFraction) {
        animatedProgress.animateTo(
            targetValue    = progressFraction,
            animationSpec  = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(language, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        "$progressPercent%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color    = PrimaryGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Gradient progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentGreen, Color(0xFF2D6A4F))
                            )
                        )
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "$completedCount of $totalLessons lessons completed",
                fontSize = 12.sp,
                color    = TextMuted
            )
        }
    }
}

// ── Badges grid ───────────────────────────────────────────────────────────────

@Composable
private fun BadgesGrid(achievements: List<AchievementItem>) {
    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        // Render in rows of 2
        achievements.chunked(2).forEach { rowItems ->
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    BadgeCard(item, Modifier.weight(1f))
                }
                // Fill empty slot if odd count
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(item: AchievementItem, modifier: Modifier = Modifier) {
    val bgColor     = if (item.unlocked) CardWhite            else Color(0xFFF3F4F6)
    val borderColor = if (item.unlocked) Color(0xFFB7E4C7)    else Color(0xFFE5E7EB)
    val badgeBg     = if (item.unlocked) Color(0xFFE3F0E9)    else Color(0xFFE5E7EB)

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (item.unlocked) 2.dp else 0.dp),
        modifier  = modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (item.unlocked) item.badge else "🔒",
                    fontSize = 26.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text       = item.title,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                color      = if (item.unlocked) TextDark else TextMuted,
                textAlign  = TextAlign.Center,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text      = item.desc,
                fontSize  = 11.sp,
                color     = TextMuted,
                textAlign = TextAlign.Center,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis
            )
            if (item.unlocked) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        "✓ Earned",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color      = PrimaryGreen,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// ── Premium upsell banner ─────────────────────────────────────────────────────

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    Card(
        shape  = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFF4A261), Color(0xFFE76F51)))
                )
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Upgrade to Premium 👑",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Unlimited leaves · Dark mode · AI feedback",
                        fontSize = 12.sp,
                        color    = Color.White.copy(alpha = 0.9f)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ── Data & helpers ────────────────────────────────────────────────────────────

private data class AchievementItem(
    val title    : String,
    val desc     : String,
    val unlocked : Boolean,
    val badge    : String
)

private fun buildAchievements(
    user           : com.velmorth.app.data.model.User,
    completedCount : Int
): List<AchievementItem> = listOf(
    AchievementItem("First Steps",      "Complete your first lesson",       completedCount >= 1,  "🌱"),
    AchievementItem("3-Day Streak",     "Maintain a 3-day active streak",   user.streak >= 3,     "🔥"),
    AchievementItem("10 Lessons",       "Complete 10 lessons",              completedCount >= 10, "📚"),
    AchievementItem("Leaf Hoarder",     "Collect 80+ leaves",               user.leaves >= 80,    "🍃"),
    AchievementItem("XP Milestone",     "Earn 500 total XP",                user.xp >= 500,       "⭐"),
    AchievementItem("Premium Scholar",  "Join Velmorth Premium",            user.isPremium,       "👑")
)

private fun languageToFlag(lang: String): String = when (lang.lowercase()) {
    "japanese"   -> "🇯🇵"
    "spanish"    -> "🇪🇸"
    "french"     -> "🇫🇷"
    "german"     -> "🇩🇪"
    "korean"     -> "🇰🇷"
    "mandarin"   -> "🇨🇳"
    "italian"    -> "🇮🇹"
    "portuguese" -> "🇧🇷"
    else         -> "🌍"
}
