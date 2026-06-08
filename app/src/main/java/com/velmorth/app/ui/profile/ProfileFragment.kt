package com.velmorth.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.ui.premium.PremiumActivity
import com.velmorth.app.ui.settings.SettingsActivity
import com.velmorth.app.ui.settings.LocaleViewModel
import com.velmorth.app.ui.settings.SupportedLanguage
import com.velmorth.app.ui.lessons.ProgressViewModel
import com.velmorth.app.theme.LearnWithVelmorthTheme
import com.velmorth.app.theme.velmorthColors
import dagger.hilt.android.AndroidEntryPoint

// ── Brand palette Resolved Dynamically ──────────────────────────────────────────
private val DarkGreen: Color @Composable get() = if (MaterialTheme.colorScheme.primary == Color(0xFF74C69D)) Color(0xFF0D2418) else Color(0xFF1B4332)
private val PrimaryGreen: Color @Composable get() = MaterialTheme.colorScheme.primary
private val AccentGreen: Color @Composable get() = MaterialTheme.colorScheme.secondary
private val LightGreen: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
private val BgCream: Color @Composable get() = MaterialTheme.colorScheme.background
private val CardWhite: Color @Composable get() = MaterialTheme.colorScheme.surface
private val TextDark: Color @Composable get() = MaterialTheme.colorScheme.onSurface
private val TextMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val GoldXP: Color @Composable get() = MaterialTheme.velmorthColors.leafGold
private val GoldBadge: Color @Composable get() = MaterialTheme.velmorthColors.leafGoldDark

private val LANGUAGES = listOf(
    Triple("japanese", "Japanese",  "🇯🇵"),
    Triple("french",   "French",    "🇫🇷"),
    Triple("sanskrit", "Sanskrit",  "🇮🇳"),
    Triple("english",  "English",   "🇬🇧")
)

/**
 * Full-featured Profile screen with Hilt ViewModel integration.
 */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var lessonRepository: LessonRepository
    private lateinit var prefsManager: PrefsManager

    private val userViewModel: UserViewModel by viewModels()
    private val progressViewModel: ProgressViewModel by viewModels()
    private val localeViewModel: LocaleViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userRepository   = UserRepository(requireContext())
        lessonRepository = LessonRepository(requireContext())
        prefsManager     = PrefsManager(requireContext())

        return ComposeView(requireContext()).apply {
            setContent {
                LearnWithVelmorthTheme {
                    ProfileScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.refreshFromFirestore()
        (view as? ComposeView)?.setContent {
            LearnWithVelmorthTheme {
                ProfileScreen()
            }
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────────

    @Composable
    private fun ProfileScreen() {
        val userState by userViewModel.userState.collectAsState()
        val progressState by progressViewModel.progressState.collectAsState()
        val nativeLangState by localeViewModel.nativeLanguage.collectAsState()

        val allLessons = remember(userState.selectedLanguage) {
            lessonRepository.getUnits().flatMap { it.lessons }
        }
        val totalLessons = allLessons.size
        val completedCount = progressState.completedCount
        val progressFraction = if (totalLessons > 0) completedCount.toFloat() / totalLessons else 0f
        val progressPercent = (progressFraction * 100).toInt()

        val username = userState.username.ifBlank {
            userState.name.lowercase().replace(" ", "")
        }
        val selectedLang = userState.selectedLanguage.ifEmpty { prefsManager.selectedLanguage }

        val earnedBadges = buildAchievements(userState, completedCount)
        val earnedCount = earnedBadges.count { it.unlocked }

        // Google account info
        val googleUser = FirebaseAuth.getInstance().currentUser
        val isGoogleLinked = googleUser?.providerData?.any { it.providerId == "google.com" } == true

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCream)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero Header ───────────────────────────────────────────────────
            HeroHeader(
                displayName   = userState.name,
                username      = username,
                isPremium     = userState.isPremium,
                streak        = userState.streak,
                leaves        = userState.leafBalance,
                xp            = userState.xp,
                joinedAt      = userState.joinedAt,
                photoUrl      = userState.photoUrl,
                onSettings    = {
                    startActivity(Intent(requireContext(), SettingsActivity::class.java))
                },
                onEditProfile = {
                    startActivity(Intent(requireContext(), EditProfileActivity::class.java))
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── Account Card ──────────────────────────────────────────────────
            SectionHeader("👤  Account")
            AccountCard(
                email          = userState.email.ifBlank { googleUser?.email ?: "—" },
                isGoogleLinked = isGoogleLinked,
                googleEmail    = if (isGoogleLinked) googleUser?.email else null,
                googlePhotoUrl = if (isGoogleLinked) googleUser?.photoUrl?.toString() else null,
                onEditProfile  = {
                    startActivity(Intent(requireContext(), EditProfileActivity::class.java))
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── Language Settings ─────────────────────────────────────────────
            SectionHeader("🌍  Learning Course Settings")
            LanguageCard(
                selectedLang = selectedLang,
                onLanguageSelected = { lang ->
                    localeViewModel.setSelectedLanguage(lang)
                }
            )

            Spacer(Modifier.height(16.dp))

            // ── App Language Settings ─────────────────────────────────────────
            SectionHeader("🗣️  App Translation Language")
            AppLanguageCard(
                currentLangTag = nativeLangState.localeTag,
                onLanguageSelected = { tag ->
                    val matchedLang = LocaleViewModel.SUPPORTED_NATIVE_LANGUAGES.firstOrNull { it.localeTag == tag }
                    if (matchedLang != null) {
                        localeViewModel.setNativeLanguage(matchedLang.id)
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            // ── My Progress ───────────────────────────────────────────────────
            SectionHeader("📊  My Progress")
            ProgressCard(
                language         = "${languageToFlag(selectedLang)} ${selectedLang.replaceFirstChar { it.uppercase() }}",
                progressFraction = progressFraction,
                progressPercent  = progressPercent,
                completedCount   = completedCount,
                totalLessons     = totalLessons
            )

            Spacer(Modifier.height(20.dp))

            // ── Badges ────────────────────────────────────────────────────────
            SectionHeader("🏆  Badges — $earnedCount earned")
            BadgesGrid(earnedBadges)

            Spacer(Modifier.height(20.dp))

            // ── Support ───────────────────────────────────────────────────────
            SectionHeader("💬  Support")
            SupportCard(
                onInstagram = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/velmorth"))
                    startActivity(intent)
                }
            )

            // ── Premium banner ────────────────────────────────────────────────
            if (!userState.isPremium) {
                Spacer(Modifier.height(20.dp))
                PremiumBanner {
                    startActivity(Intent(requireContext(), PremiumActivity::class.java))
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    @Composable
    private fun AppLanguageCard(
        currentLangTag: String,
        onLanguageSelected: (String) -> Unit
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "App Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LocaleViewModel.SUPPORTED_NATIVE_LANGUAGES.forEach { lang ->
                        val isSelected = currentLangTag == lang.localeTag
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageSelected(lang.localeTag) },
                            label = {
                                Text(
                                    text = "${lang.flag} ${lang.displayName}",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = BgCream,
                                labelColor = TextDark
                            )
                        )
                    }
                }
            }
        }
    }

    // ── Section header label ──────────────────────────────────────────────────

    @Composable
    private fun SectionHeader(text: String) {
        Text(
            text       = text,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextMuted,
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
    }

    // ── Account Card ─────────────────────────────────────────────────────────

    @Composable
    private fun AccountCard(
        email: String,
        isGoogleLinked: Boolean,
        googleEmail: String?,
        googlePhotoUrl: String?,
        onEditProfile: () -> Unit
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                // Email row
                AccountRow(
                    icon  = Icons.Default.Email,
                    label = "Email",
                    value = email
                )
                if (isGoogleLinked && googleEmail != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color(0xFFF0EDE8)
                    )
                    // Google account info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Google "G" icon
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F3F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Google Account", fontSize = 12.sp, color = TextMuted)
                            Text(
                                googleEmail,
                                fontSize  = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color     = TextDark,
                                maxLines  = 1,
                                overflow  = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Text(
                                "Connected",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF0EDE8))
                // Edit profile row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditProfile() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Edit Name / Username / Email", fontSize = 14.sp, color = PrimaryGreen, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    @Composable
    private fun AccountRow(icon: ImageVector, label: String, value: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, fontSize = 11.sp, color = TextMuted)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    // ── Language Switcher Card ────────────────────────────────────────────────

    @Composable
    private fun LanguageCard(
        selectedLang: String,
        onLanguageSelected: (String) -> Unit
    ) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Column(Modifier.padding(8.dp)) {
                LANGUAGES.forEachIndexed { index, (key, name, flag) ->
                    val isSelected = selectedLang.equals(key, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent)
                            .clickable { onLanguageSelected(key) }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(flag, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            name,
                            fontSize   = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) DarkGreen else TextDark,
                            modifier   = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Active",
                                tint = AccentGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < LANGUAGES.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = Color(0xFFF0EDE8)
                        )
                    }
                }
            }
        }
    }

    // ── Support Card ─────────────────────────────────────────────────────────

    @Composable
    private fun SupportCard(onInstagram: () -> Unit) {
        Card(
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInstagram() }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Instagram gradient icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE1306C), Color(0xFFF77737), Color(0xFF833AB4))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📸", fontSize = 20.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Contact Support",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextDark
                    )
                    Text(
                        "DM us on Instagram @velmorth",
                        fontSize = 12.sp,
                        color    = TextMuted
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
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
    photoUrl      : String,
    onSettings    : () -> Unit,
    onEditProfile : () -> Unit
) {
    var bitmap by remember(photoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(photoUrl) {
        if (photoUrl.isNotEmpty()) {
            try {
                if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val url = java.net.URL(photoUrl)
                            url.openStream()?.use { stream ->
                                android.graphics.BitmapFactory.decodeStream(stream)
                            }
                        }.getOrNull()
                    }?.let { bmp ->
                        bitmap = bmp
                    }
                } else {
                    val uri = android.net.Uri.parse(photoUrl)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = android.graphics.BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            bitmap = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(colors = listOf(DarkGreen, PrimaryGreen, Color(0xFF3D8B68)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: title + settings gear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Avatar with edit badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .shadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(LightGreen, AccentGreen)))
                        .border(3.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = bitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = displayName.take(1).uppercase(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkGreen
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GoldXP)
                        .border(2.2.dp, Color.White, CircleShape)
                        .clickable { onEditProfile() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))

            // Name + premium badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isPremium) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = GoldBadge) {
                        Text(
                            "👑 PRO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            Text("@$username", fontSize = 14.sp, color = LightGreen.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)

            Spacer(Modifier.height(20.dp))

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                StatPill(icon = "🔥", label = "Streak", value = "$streak days")
                StatPill(icon = "🌿", label = "Leaves",  value = "$leaves")
                StatPill(icon = "⭐", label = "XP",      value = "$xp")
            }

            Spacer(Modifier.height(18.dp))

            // Edit Profile button
            Button(
                onClick = onEditProfile,
                colors  = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape   = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            }
        }
    }
}

// ── Stat pill ─────────────────────────────────────────────────────────────────

@Composable
private fun StatPill(icon: String, label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.15f)) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 11.sp, color = LightGreen.copy(alpha = 0.8f))
        }
    }
}

// ── Progress card ─────────────────────────────────────────────────────────────

@Composable
private fun ProgressCard(
    language: String,
    progressFraction: Float,
    progressPercent: Int,
    completedCount: Int,
    totalLessons: Int
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progressFraction) {
        animatedProgress.animateTo(progressFraction, animationSpec = tween(1000, easing = FastOutSlowInEasing))
    }

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(language, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE8F5E9)) {
                    Text(
                        "$progressPercent%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0xFFE5E7EB))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(AccentGreen, PrimaryGreen)))
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("$completedCount of $totalLessons lessons completed", fontSize = 12.sp, color = TextMuted)
        }
    }
}

// ── Badges grid ───────────────────────────────────────────────────────────────

@Composable
private fun BadgesGrid(achievements: List<AchievementItem>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        achievements.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { item -> BadgeCard(item, Modifier.weight(1f)) }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(item: AchievementItem, modifier: Modifier = Modifier) {
    val bgColor     = if (item.unlocked) CardWhite         else Color(0xFFF3F4F6)
    val borderColor = if (item.unlocked) Color(0xFFB7E4C7) else Color(0xFFE5E7EB)
    val badgeBg     = if (item.unlocked) Color(0xFFE3F0E9) else Color(0xFFE5E7EB)

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(if (item.unlocked) 2.dp else 0.dp),
        modifier  = modifier.border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(if (item.unlocked) item.badge else "🔒", fontSize = 26.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.unlocked) TextDark else TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(item.desc, fontSize = 11.sp, color = TextMuted, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.unlocked) {
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFE8F5E9)) {
                    Text(
                        "✓ Earned",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFF4A261), Color(0xFFE76F51))))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Upgrade to Premium 👑", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Unlimited leaves · Dark mode · AI feedback", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
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
    user: com.velmorth.app.data.model.User,
    completedCount: Int
): List<AchievementItem> = listOf(
    AchievementItem("First Steps",     "Complete your first lesson",       completedCount >= 1,  "🌱"),
    AchievementItem("3-Day Streak",    "Maintain a 3-day active streak",   user.streak >= 3,     "🔥"),
    AchievementItem("10 Lessons",      "Complete 10 lessons",              completedCount >= 10, "📚"),
    AchievementItem("Leaf Hoarder",    "Collect 80+ leaves",               user.leaves >= 80,    "🍃"),
    AchievementItem("XP Milestone",    "Earn 500 total XP",                user.xp >= 500,       "⭐"),
    AchievementItem("Premium Scholar", "Join Velmorth Premium",            user.isPremium,       "👑")
)

private fun languageToFlag(lang: String): String = when (lang.lowercase()) {
    "japanese"   -> "🇯🇵"
    "french"     -> "🇫🇷"
    "sanskrit"   -> "🇮🇳"
    "english"    -> "🇬🇧"
    "spanish"    -> "🇪🇸"
    "german"     -> "🇩🇪"
    "korean"     -> "🇰🇷"
    "mandarin"   -> "🇨🇳"
    else         -> "🌍"
}
