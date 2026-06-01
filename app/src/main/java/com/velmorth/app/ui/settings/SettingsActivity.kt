package com.velmorth.app.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.velmorth.app.BuildConfig
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.ui.legal.*
import com.velmorth.app.ui.premium.PremiumActivity
import com.velmorth.app.ui.splash.SplashActivity
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.utils.NotificationScheduler

// ── Colors ────────────────────────────────────────────────────────────────────
private val BgCream        = Color(0xFFF8F5EE)
private val DarkGreen      = Color(0xFF1B4332)
private val PrimaryGreen   = Color(0xFF2D6A4F)
private val AccentGreen    = Color(0xFF52B788)
private val TextDark       = Color(0xFF1C1C1E)
private val TextMuted      = Color(0xFF6B7280)
private val CardWhite      = Color(0xFFFFFFFF)
private val DividerColor   = Color(0xFFF0EDE8)
private val DangerRed      = Color(0xFFE53935)

/**
 * Full-featured Settings screen with Firestore sync.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = PrefsManager(this)
        setContent { SettingsScreen(prefs) }
    }

    @Composable
    private fun SettingsScreen(prefs: PrefsManager) {
        val context = LocalContext.current

        // ── State ─────────────────────────────────────────────────────────────
        var displayName        by remember { mutableStateOf(prefs.userName) }
        var username           by remember { mutableStateOf(prefs.username) }
        var email              by remember { mutableStateOf(prefs.userEmail) }
        var dailyReminder      by remember { mutableStateOf(prefs.notificationsEnabled) }
        var reminderHour       by remember { mutableStateOf(prefs.reminderHour) }
        var reminderMinute     by remember { mutableStateOf(prefs.reminderMinute) }
        var streakAlert        by remember { mutableStateOf(prefs.streakAlertEnabled) }
        var promoAlerts        by remember { mutableStateOf(prefs.promoAlertsEnabled) }
        var themeMode          by remember { mutableStateOf(prefs.themeMode) }

        // Dialogs
        var showNameDialog     by remember { mutableStateOf(false) }
        var showUsernameDialog by remember { mutableStateOf(false) }
        var showEmailDialog    by remember { mutableStateOf(false) }
        var showDeleteDialog   by remember { mutableStateOf(false) }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showNameDialog) {
            EditFieldDialog(
                title       = "Change Name",
                label       = "Display Name",
                initialValue = displayName,
                onDismiss   = { showNameDialog = false },
                onSave      = { v ->
                    displayName = v; prefs.userName = v
                    showNameDialog = false
                    Toast.makeText(context, "Name updated ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }
        if (showUsernameDialog) {
            EditFieldDialog(
                title        = "Change Username",
                label        = "Username",
                initialValue = username,
                onDismiss    = { showUsernameDialog = false },
                onSave       = { v ->
                    username = v; prefs.username = v
                    showUsernameDialog = false
                    Toast.makeText(context, "Username updated ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }
        if (showEmailDialog) {
            EditFieldDialog(
                title        = "Change Email",
                label        = "Email address",
                initialValue = email,
                onDismiss    = { showEmailDialog = false },
                onSave       = { v ->
                    email = v; prefs.userEmail = v
                    showEmailDialog = false
                    Toast.makeText(context, "Email updated ✓", Toast.LENGTH_SHORT).show()
                }
            )
        }
        if (showDeleteDialog) {
            DeleteAccountDialog(
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    val fbUser = FirebaseAuth.getInstance().currentUser
                    fbUser?.delete()?.addOnCompleteListener {
                        FirebaseAuth.getInstance().signOut()
                        prefs.clearAll()
                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                        startActivity(
                            Intent(this@SettingsActivity, SplashActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    } ?: run {
                        prefs.clearAll()
                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                        startActivity(
                            Intent(this@SettingsActivity, SplashActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                    }
                }
            )
        }

        // ── Layout ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgCream)
                .verticalScroll(rememberScrollState())
        ) {

            // Top bar
            SettingsTopBar(onBack = { finish() })

            Spacer(Modifier.height(8.dp))

            // ── 1. Account ────────────────────────────────────────────────────
            SectionHeader("Account")
            SectionCard {
                SettingsRow(
                    icon       = Icons.Default.Person,
                    title      = "Change Name",
                    subtitle   = displayName.ifBlank { "Not set" },
                    onClick    = { showNameDialog = true }
                )
                RowDivider()
                SettingsRow(
                    icon       = Icons.Default.AlternateEmail,
                    title      = "Change Username",
                    subtitle   = username.ifBlank { "Not set" },
                    onClick    = { showUsernameDialog = true }
                )
                RowDivider()
                SettingsRow(
                    icon       = Icons.Default.Email,
                    title      = "Change Email",
                    subtitle   = email.ifBlank { "Not set" },
                    onClick    = { showEmailDialog = true }
                )
                RowDivider()
                SettingsRow(
                    icon       = Icons.Default.DeleteForever,
                    title      = "Delete Account",
                    subtitle   = "Permanently remove all data",
                    titleColor = DangerRed,
                    iconTint   = DangerRed,
                    onClick    = { showDeleteDialog = true }
                )
            }

            // ── 2. Notifications ──────────────────────────────────────────────
            SectionHeader("Notifications")
            SectionCard {
                SwitchRow(
                    icon     = Icons.Default.NotificationsActive,
                    title    = "Daily Reminder",
                    subtitle = "Get reminded to study every day",
                    checked  = dailyReminder,
                    onToggle = { enabled ->
                        dailyReminder = enabled
                        prefs.notificationsEnabled = enabled
                        // Sync to Firestore settings
                        FirestoreProgressRepository.ensureSettingsDocExists()
                        if (enabled) {
                            NotificationScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
                        } else {
                            NotificationScheduler.cancelReminder(context)
                        }
                    }
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Default.Schedule,
                    title    = "Reminder Time",
                    subtitle = "%02d:%02d".format(reminderHour, reminderMinute),
                    onClick  = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                reminderHour   = h; prefs.reminderHour   = h
                                reminderMinute = m; prefs.reminderMinute = m
                                if (dailyReminder) {
                                    NotificationScheduler.scheduleDailyReminder(context, h, m)
                                }
                            },
                            reminderHour,
                            reminderMinute,
                            true
                        ).show()
                    }
                )
                RowDivider()
                SwitchRow(
                    icon     = Icons.Default.Whatshot,
                    title    = "Streak Alert",
                    subtitle = "Warn when your streak is at risk",
                    checked  = streakAlert,
                    onToggle = { streakAlert = it; prefs.streakAlertEnabled = it }
                )
                RowDivider()
                SwitchRow(
                    icon     = Icons.Default.Campaign,
                    title    = "Promotional Alerts",
                    subtitle = "New features, offers & updates",
                    checked  = promoAlerts,
                    onToggle = { promoAlerts = it; prefs.promoAlertsEnabled = it }
                )
            }

            // ── 3. App Permissions ────────────────────────────────────────────
            SectionHeader("App Permissions")
            SectionCard {
                SettingsRow(
                    icon     = Icons.Default.Notifications,
                    title    = "Notification Permission",
                    subtitle = "Manage in System Settings",
                    onClick  = {
                        val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(AndroidSettings.EXTRA_APP_PACKAGE, packageName)
                        }
                        startActivity(intent)
                    }
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Default.RecordVoiceOver,
                    title    = "Audio / TTS Permission",
                    subtitle = "Required for Read-Aloud lessons",
                    onClick  = {
                        Toast.makeText(context, "TTS is granted at runtime during lessons", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // ── 4. Appearance ─────────────────────────────────────────────────
            SectionHeader("Appearance")
            SectionCard {
                ThemeOptionRow(
                    icon     = Icons.Default.LightMode,
                    label    = "Light Mode",
                    selected = themeMode == "light",
                    onClick  = {
                        themeMode = "light"
                        prefs.themeMode = "light"
                        prefs.darkMode = false
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                )
                RowDivider()
                ThemeOptionRow(
                    icon     = Icons.Default.DarkMode,
                    label    = "Dark Mode",
                    selected = themeMode == "dark",
                    onClick  = {
                        themeMode = "dark"
                        prefs.themeMode = "dark"
                        prefs.darkMode = true
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    }
                )
                RowDivider()
                ThemeOptionRow(
                    icon     = Icons.Default.SettingsBrightness,
                    label    = "System Default",
                    selected = themeMode == "system",
                    onClick  = {
                        themeMode = "system"
                        prefs.themeMode = "system"
                        prefs.darkMode = false
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    }
                )
            }

            // ── 5. Subscription ───────────────────────────────────────────────
            SectionHeader("Subscription")
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint               = if (prefs.isPremium) Color(0xFFD4AC0D) else TextMuted,
                        modifier           = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Current Plan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        Text(
                            if (prefs.isPremium) "Premium 👑" else "Free",
                            fontSize = 12.sp, color = if (prefs.isPremium) Color(0xFFD4AC0D) else TextMuted
                        )
                    }
                    Surface(
                        shape  = RoundedCornerShape(20.dp),
                        color  = if (prefs.isPremium) Color(0xFFFFF3CD) else Color(0xFFE8F5E9)
                    ) {
                        Text(
                            text     = if (prefs.isPremium) "PREMIUM" else "FREE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color    = if (prefs.isPremium) Color(0xFF856404) else PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                RowDivider()
                SettingsRow(
                    icon     = Icons.Default.Star,
                    title    = "Upgrade to Premium",
                    subtitle = "Unlimited leaves, dark mode & more",
                    iconTint = Color(0xFFD4AC0D),
                    onClick  = { startActivity(Intent(this@SettingsActivity, PremiumActivity::class.java)) }
                )
            }

            // ── 6. Legal ──────────────────────────────────────────────────────
            SectionHeader("Legal")
            SectionCard {
                SettingsRow(
                    icon     = Icons.Default.PrivacyTip,
                    title    = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick  = { startActivity(Intent(this@SettingsActivity, PrivacyPolicyActivity::class.java)) }
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Default.Gavel,
                    title    = "Terms & Conditions",
                    subtitle = "Usage rules & license",
                    onClick  = { startActivity(Intent(this@SettingsActivity, TermsConditionsActivity::class.java)) }
                )
                RowDivider()
                SettingsRow(
                    icon     = Icons.Default.Info,
                    title    = "Open-Source Licenses",
                    subtitle = "Third-party library attributions",
                    onClick  = { startActivity(Intent(this@SettingsActivity, LicensesActivity::class.java)) }
                )
            }

            // ── 7. About & Support ────────────────────────────────────────────
            SectionHeader("About & Support")
            SectionCard {
                SettingsRow(
                    icon      = Icons.Default.Info,
                    title     = "About Velmorth",
                    subtitle  = "App story & version info",
                    onClick   = { startActivity(Intent(this@SettingsActivity, AboutAppActivity::class.java)) }
                )
                RowDivider()
                SettingsRow(
                    icon      = Icons.Default.Info,
                    title     = "App Version",
                    subtitle  = "v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    showArrow = false
                )
                // Instagram support link — ONLY support channel
                SettingsRow(
                    icon       = Icons.Default.Share,
                    title      = "Instagram Support",
                    subtitle   = "@velmorth — DM for bugs, feedback & help",
                    iconTint   = Color(0xFFE1306C),
                    onClick    = {
                        val intent = Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://www.instagram.com/velmorth"))
                        try {
                            intent.setPackage("com.instagram.android")
                            startActivity(intent)
                        } catch (e: Exception) {
                            intent.setPackage(null)
                            startActivity(intent)
                        }
                    }
                )
            }

            // Log out button
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                    performLogout(context)
                },
                colors   = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape    = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(50.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Log Out", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    fun performLogout(context: android.content.Context) {
        FirebaseAuth.getInstance().signOut()
        PrefsManager(context).clearAll()
        val intent = android.content.Intent(context, com.velmorth.app.ui.auth.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                       android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B4332))
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint               = Color.White
            )
        }
        Text(
            text       = "Settings",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color    = Color(0xFF2D6A4F),
        letterSpacing = 1.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 52.dp),
        thickness = 0.8.dp,
        color     = DividerColor
    )
}

@Composable
private fun SettingsRow(
    icon       : ImageVector,
    title      : String,
    subtitle   : String  = "",
    titleColor : Color   = TextDark,
    iconTint   : Color   = Color(0xFF2D6A4F),
    showArrow  : Boolean = true,
    onClick    : (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            if (subtitle.isNotBlank())
                Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        if (showArrow && onClick != null) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint               = Color(0xFFCBD5E1),
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SwitchRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    checked  : Boolean,
    onToggle : (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = Color(0xFF2D6A4F)
            )
        )
    }
}

@Composable
private fun ThemeOptionRow(
    icon     : ImageVector,
    label    : String,
    selected : Boolean,
    onClick  : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDark, modifier = Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick  = onClick,
            colors   = RadioButtonDefaults.colors(selectedColor = Color(0xFF2D6A4F))
        )
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun EditFieldDialog(
    title        : String,
    label        : String,
    initialValue : String,
    onDismiss    : () -> Unit,
    onSave       : (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            tonalElevation = 4.dp
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value         = text,
                    onValueChange = { text = it },
                    label         = { Text(label) },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF2D6A4F),
                        focusedLabelColor    = Color(0xFF2D6A4F),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(24.dp)
                    ) { Text("Cancel") }
                    Button(
                        onClick  = { if (text.isNotBlank()) onSave(text.trim()) },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        shape    = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save", color = Color.White) }
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onDismiss : () -> Unit,
    onConfirm : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon             = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(36.dp))
        },
        title = {
            Text("Delete Account?", fontWeight = FontWeight.Bold, color = TextDark, textAlign = TextAlign.Center)
        },
        text  = {
            Text(
                "This will permanently delete all your progress, XP, streaks, and settings. This action cannot be undone.",
                color   = TextMuted,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape   = RoundedCornerShape(24.dp)
            ) { Text("Delete Forever", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape   = RoundedCornerShape(24.dp)
            ) { Text("Cancel") }
        },
        containerColor = CardWhite,
        shape          = RoundedCornerShape(20.dp)
    )
}
