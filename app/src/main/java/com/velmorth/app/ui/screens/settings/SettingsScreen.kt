package com.velmorth.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.velmorth.app.domain.repository.UserRepository
import com.velmorth.app.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

// ── Data models ───────────────────────────────────────────────────────────────

data class SettingsUiState(
    val dailyGoalMinutes: Int   = 10,
    val notificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean      = false,
    val textSizeScale: Float          = 1f,
    val isPremium: Boolean            = false,
    val appVersion: String            = "1.0.0 (42)",
    val showResetConfirmDialog: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getUser().collect { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            dailyGoalMinutes = user.dailyGoalMinutes,
                            isPremium = user.isPremium
                        )
                    }
                }
            }
        }
    }

    fun setDailyGoal(minutes: Int) {
        _uiState.update { it.copy(dailyGoalMinutes = minutes) }
        viewModelScope.launch {
            userRepository.setDailyGoal("local_user", minutes)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(darkModeEnabled = enabled) }
    }

    fun setTextSizeScale(scale: Float) {
        _uiState.update { it.copy(textSizeScale = scale) }
    }

    fun showResetDialog() {
        _uiState.update { it.copy(showResetConfirmDialog = true) }
    }

    fun dismissResetDialog() {
        _uiState.update { it.copy(showResetConfirmDialog = false) }
    }

    fun confirmResetProgress(onResetDone: () -> Unit) {
        _uiState.update { it.copy(showResetConfirmDialog = false) }
        viewModelScope.launch {
            userRepository.resetUser("local_user")
            onResetDone()
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToPremium: () -> Unit,
    onBack: () -> Unit,
    onResetProgress: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val forestDeep = Color(0xFF1B4332)
    val mossGreen  = Color(0xFF40916C)
    val creamWhite = Color(0xFFF1E8D0)
    val leafGold   = Color(0xFFD4A017)

    val dailyGoalOptions = listOf(5, 10, 15, 20)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        viewModel.toggleNotifications(isGranted)
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notifications denied. Please enable them in Settings for reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Reset confirm dialog
    if (state.showResetConfirmDialog) {
        ResetProgressDialog(
            onConfirm = {
                viewModel.confirmResetProgress(onResetDone = onResetProgress)
            },
            onDismiss = { viewModel.dismissResetDialog() },
            forestDeep = forestDeep,
            mossGreen  = mossGreen,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "Settings",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = forestDeep,
                ),
            )
        },
        containerColor = creamWhite,
    ) { innerPadding ->

        LazyColumn(
            modifier       = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {

            // ─── LEARNING ───────────────────────────────────────────────────
            item { SectionHeader(title = "Learning", forestDeep = forestDeep) }

            item {
                SettingsCard {
                    Column {
                        Row(
                            modifier       = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📅", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Daily Goal",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = NunitoFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = forestDeep,
                                    ),
                                )
                                Text(
                                    "${state.dailyGoalMinutes} min / day",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = NunitoFamily,
                                        color      = Color.Gray,
                                    ),
                                )
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(dailyGoalOptions) { mins ->
                                val selected = state.dailyGoalMinutes == mins
                                FilterChip(
                                    selected = selected,
                                    onClick  = { viewModel.setDailyGoal(mins) },
                                    label    = {
                                        Text(
                                            "${mins}m",
                                            fontFamily = NunitoFamily,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    colors   = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = mossGreen,
                                        selectedLabelColor     = Color.White,
                                    ),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        emoji    = "🔔",
                        label    = "Reminder Notifications",
                        subLabel = "Daily study reminders",
                        checked  = state.notificationsEnabled && hasNotificationPermission,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.toggleNotifications(true)
                                }
                            } else {
                                viewModel.toggleNotifications(false)
                            }
                        },
                        forestDeep = forestDeep,
                        mossGreen  = mossGreen,
                    )
                }
            }

            // ─── DISPLAY ────────────────────────────────────────────────────
            item { SectionHeader(title = "Display", forestDeep = forestDeep) }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        emoji    = "🌙",
                        label    = "Dark Mode",
                        subLabel = "Switch to dark theme",
                        checked  = state.darkModeEnabled,
                        onCheckedChange = viewModel::toggleDarkMode,
                        forestDeep = forestDeep,
                        mossGreen  = mossGreen,
                    )
                }
            }

            item {
                SettingsCard {
                    Row(
                        modifier       = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔡", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Text Size",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = forestDeep,
                                ),
                            )
                            Text(
                                "${(state.textSizeScale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = NunitoFamily,
                                    color      = Color.Gray,
                                ),
                            )
                        }
                    }
                    Slider(
                        value         = state.textSizeScale,
                        onValueChange = viewModel::setTextSizeScale,
                        valueRange    = 0.8f..1.4f,
                        steps         = 5,
                        colors        = SliderDefaults.colors(
                            thumbColor       = mossGreen,
                            activeTrackColor = mossGreen,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // ─── PREMIUM ────────────────────────────────────────────────────
            item { SectionHeader(title = "Premium", forestDeep = forestDeep) }

            item {
                SettingsCard {
                    Row(
                        modifier       = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("👑", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Status",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = forestDeep,
                                ),
                            )
                            Text(
                                if (state.isPremium) "Premium Active" else "Free Plan",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = NunitoFamily,
                                    color      = if (state.isPremium) mossGreen else Color.Gray,
                                    fontWeight = if (state.isPremium) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (state.isPremium) mossGreen else leafGold,
                        ) {
                            Text(
                                text     = if (state.isPremium) "Premium ✓" else "Free",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White,
                                    fontSize   = 11.sp,
                                ),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick  = onNavigateToPremium,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (state.isPremium) mossGreen.copy(alpha = 0.15f) else leafGold,
                        ),
                    ) {
                        Text(
                            text  = if (state.isPremium) "Manage Subscription" else "Upgrade to Premium 👑",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = NunitoFamily,
                                fontWeight = FontWeight.Bold,
                                color      = if (state.isPremium) mossGreen else Color(0xFF1B2A1E),
                            ),
                        )
                    }
                }
            }

            // ─── ACCOUNT ────────────────────────────────────────────────────
            item { SectionHeader(title = "Account", forestDeep = forestDeep) }

            item {
                SettingsCard {
                    SettingsArrowRow(
                        emoji      = "✏️",
                        label      = "Edit Profile",
                        subLabel   = "Change name, avatar & language",
                        forestDeep = forestDeep,
                        onClick    = { /* navigate to edit profile */ },
                    )
                }
            }

            item {
                SettingsCard {
                    SettingsArrowRow(
                        emoji      = "🗑️",
                        label      = "Reset Progress",
                        subLabel   = "Clear all learning data",
                        labelColor = Color(0xFFEF5350),
                        forestDeep = forestDeep,
                        onClick    = { viewModel.showResetDialog() },
                    )
                }
            }

            // ─── ABOUT ──────────────────────────────────────────────────────
            item { SectionHeader(title = "About", forestDeep = forestDeep) }

            item {
                SettingsCard {
                    Column {
                        Row(
                            modifier          = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("ℹ️", fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "App Version",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = forestDeep,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                state.appVersion,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = NunitoFamily,
                                    color      = Color.Gray,
                                ),
                            )
                        }

                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = 10.dp),
                            color     = Color.Gray.copy(alpha = 0.15f),
                        )

                        SettingsArrowRow(
                            emoji      = "🔒",
                            label      = "Privacy Policy",
                            forestDeep = forestDeep,
                            onClick    = { /* open url */ },
                        )

                        HorizontalDivider(
                            modifier  = Modifier.padding(vertical = 10.dp),
                            color     = Color.Gray.copy(alpha = 0.15f),
                        )

                        SettingsArrowRow(
                            emoji      = "📜",
                            label      = "Terms of Service",
                            forestDeep = forestDeep,
                            onClick    = { /* open url */ },
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, forestDeep: Color) {
    Text(
        text     = title.uppercase(),
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp),
        style    = MaterialTheme.typography.labelSmall.copy(
            fontFamily = NunitoFamily,
            fontWeight = FontWeight.ExtraBold,
            color      = forestDeep.copy(alpha = 0.5f),
            fontSize   = 11.sp,
            letterSpacing = 1.2.sp,
        ),
    )
}

// ── Settings card wrapper ─────────────────────────────────────────────────────

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
        content = content,
    )
}

// ── Switch row ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSwitchRow(
    emoji: String,
    label: String,
    subLabel: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    forestDeep: Color,
    mossGreen: Color,
) {
    Row(
        modifier       = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    color      = forestDeep,
                ),
            )
            if (subLabel != null) {
                Text(
                    subLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = NunitoFamily,
                        color      = Color.Gray,
                    ),
                )
            }
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = mossGreen,
            ),
        )
    }
}

// ── Arrow row ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsArrowRow(
    emoji: String,
    label: String,
    subLabel: String? = null,
    labelColor: Color = Color(0xFF1B4332),
    forestDeep: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier       = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.SemiBold,
                    color      = labelColor,
                ),
            )
            if (subLabel != null) {
                Text(
                    subLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = NunitoFamily,
                        color      = Color.Gray,
                    ),
                )
            }
        }
        Icon(
            imageVector        = Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = Color.Gray.copy(alpha = 0.5f),
        )
    }
}

// ── Reset progress dialog ─────────────────────────────────────────────────────

@Composable
private fun ResetProgressDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    forestDeep: Color,
    mossGreen: Color,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
        ) {
            Column(
                modifier              = Modifier.padding(24.dp),
                horizontalAlignment   = Alignment.CenterHorizontally,
            ) {
                Text("🗑️", fontSize = 36.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text      = "Reset Progress?",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style     = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlayfairFamily,
                        fontWeight = FontWeight.Bold,
                        color      = forestDeep,
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "This will permanently delete all your learning data, XP, streaks, and badges. This action cannot be undone.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = NunitoFamily,
                        color      = Color.Gray,
                    ),
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(50),
                    ) {
                        Text(
                            "Cancel",
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.SemiBold,
                            color      = forestDeep,
                        )
                    }
                    Button(
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350),
                        ),
                    ) {
                        Text(
                            "Reset",
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                    }
                }
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SettingsScreenPreview() {
    LearnWithVelmorthTheme {
        SettingsScreen(
            onNavigateToPremium = {},
            onBack              = {},
        )
    }
}
