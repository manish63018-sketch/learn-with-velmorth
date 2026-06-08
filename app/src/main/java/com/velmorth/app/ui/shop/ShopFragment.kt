package com.velmorth.app.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.ui.profile.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.velmorth.app.theme.LearnWithVelmorthTheme


// ── Colors ────────────────────────────────────────────────────────────────────
private val DarkGreen    = Color(0xFF1B4332)
private val PrimaryGreen = Color(0xFF2D6A4F)
private val AccentGreen  = Color(0xFF52B788)
private val LightGreen   = Color(0xFFB7E4C7)
private val BgCream      = Color(0xFFF8F5EE)
private val CardWhite    = Color(0xFFFFFFFF)
private val TextDark     = Color(0xFF1C1C1E)
private val TextMuted    = Color(0xFF6B7280)
private val GoldColor    = Color(0xFFD4AC0D)
private val DangerRed    = Color(0xFFE76F51)

// ── Shop item data ────────────────────────────────────────────────────────────

private data class LeafShopItem(
    val id          : String,
    val emoji       : String,
    val title       : String,
    val costLeaves  : Int,
    val descEn      : String,
    val descHi      : String,
    val bgColor     : Color,
    val accentColor : Color
)

private val SHOP_CATALOG = listOf(
    LeafShopItem(
        id          = "streak_freeze",
        emoji       = "🛡️",
        title       = "Streak Freeze",
        costLeaves  = 30,
        descEn      = "Protects your streak for 24 hours if you miss a day.",
        descHi      = "1 दिन miss होने पर भी streak बची रहती है।",
        bgColor     = Color(0xFFE3F2FD),
        accentColor = Color(0xFF1565C0)
    ),
    LeafShopItem(
        id          = "hint_pack",
        emoji       = "💡",
        title       = "Hint Pack",
        costLeaves  = 20,
        descEn      = "Unlimited hints for your next 5 lessons.",
        descHi      = "अगले 5 lessons में unlimited hints।",
        bgColor     = Color(0xFFFFF8E1),
        accentColor = Color(0xFFF57F17)
    ),
    LeafShopItem(
        id          = "double_xp_weekend",
        emoji       = "⚡",
        title       = "Double XP Weekend",
        costLeaves  = 50,
        descEn      = "Double XP for 48 hours.",
        descHi      = "48 घंटे के लिए दोगुना XP।",
        bgColor     = Color(0xFFF3E5F5),
        accentColor = Color(0xFF6A1B9A)
    ),
    LeafShopItem(
        id          = "theme_ocean",
        emoji       = "🌊",
        title       = "Theme: Ocean",
        costLeaves  = 100,
        descEn      = "Unlock Ocean Blue theme.",
        descHi      = "Ocean Blue थीम unlock करो।",
        bgColor     = Color(0xFFE0F7FA),
        accentColor = Color(0xFF00695C)
    )
)

// ── How-to-earn entries ───────────────────────────────────────────────────────

private data class EarnEntry(val emoji: String, val how: String, val reward: String)

private val EARN_GUIDE = listOf(
    EarnEntry("🌅", "Daily Login",       "+5 🍃 / day"),
    EarnEntry("📚", "Complete a Lesson", "+5 🍃"),
    EarnEntry("✨", "Perfect Quiz",      "+3 🍃 bonus"),
    EarnEntry("🔥", "7-Day Streak",      "+20 🍃 bonus")
)

/**
 * Leaf Shop fragment — spend earned leaves on in-app perks.
 * Leaves are earned through learning, NOT purchased with real money.
 */
@AndroidEntryPoint
class ShopFragment : Fragment() {

    private lateinit var userRepository: UserRepository
    private lateinit var prefsManager: PrefsManager
    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userRepository = UserRepository(requireContext())
        prefsManager   = PrefsManager(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                LearnWithVelmorthTheme {
                    ShopScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        userViewModel.refreshFromFirestore()
        (view as? ComposeView)?.setContent {
            LearnWithVelmorthTheme {
                ShopScreen()
            }
        }
    }

    // ── Root screen ───────────────────────────────────────────────────────────

    @Composable
    private fun ShopScreen() {
        val userState by userViewModel.userState.collectAsState()
        val leaves = userState.leafBalance
        val ownedItems   = prefsManager.ownedShopItems
        val isHindi      = prefsManager.nativeLanguage.equals("Hindi", ignoreCase = true)

        var pendingItem  by remember { mutableStateOf<LeafShopItem?>(null) }

        // Purchase confirmation dialog
        pendingItem?.let { item ->
            PurchaseDialog(
                item     = item,
                canAfford = leaves >= item.costLeaves,
                onConfirm = {
                    if (leaves >= item.costLeaves) {
                        userViewModel.updateLeaves(-item.costLeaves)
                        val owned = prefsManager.ownedShopItems.toMutableSet()
                        owned.add(item.id)
                        prefsManager.ownedShopItems = owned
                        Toast.makeText(requireContext(), "Purchased ${item.title}! 🍃", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Not enough leaves! Keep learning. 🌱", Toast.LENGTH_LONG).show()
                    }
                    pendingItem = null
                },
                onDismiss = { pendingItem = null }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Hero header ───────────────────────────────────────────────────
            ShopHeader(leaves = leaves)

            Spacer(Modifier.height(20.dp))

            // ── How to earn leaves ────────────────────────────────────────────
            SectionLabel("🌿  How to Earn Leaves")
            EarnGuideCard()

            Spacer(Modifier.height(20.dp))

            // ── Shop catalog ──────────────────────────────────────────────────
            SectionLabel("🛒  Spend Your Leaves")

            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SHOP_CATALOG.forEach { item ->
                    val owned = item.id in ownedItems
                    ShopItemCard(
                        item       = item,
                        isOwned    = owned,
                        canAfford  = leaves >= item.costLeaves,
                        isHindi    = isHindi,
                        onBuy      = {
                            if (!owned) pendingItem = item
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Hero header ───────────────────────────────────────────────────────────────

@Composable
private fun ShopHeader(leaves: Int) {
    val isDark = MaterialTheme.colorScheme.primary == Color(0xFF74C69D)
    val headerGradient = if (isDark) {
        Brush.verticalGradient(listOf(Color(0xFF0D2418), Color(0xFF1B4332)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)))
    }
    val subtitleColor = if (isDark) Color(0xFF74C69D) else Color(0xFFB7E4C7)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerGradient)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text("🍃 Leaf Shop", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                "Spend leaves earned from learning — never real money!",
                fontSize  = 13.sp,
                color     = subtitleColor,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(16.dp))

            // Balance pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🍃", fontSize = 22.sp)
                    Column {
                        Text(
                            "$leaves",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                        Text("Your balance", fontSize = 11.sp, color = subtitleColor)
                    }
                }
            }
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        fontSize   = 15.sp,
        fontWeight = FontWeight.Bold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(8.dp))
}

// ── Earn guide card ───────────────────────────────────────────────────────────

@Composable
private fun EarnGuideCard() {
    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EARN_GUIDE.forEach { entry ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Emoji badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(entry.emoji, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        entry.how,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        modifier   = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            entry.reward,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (entry != EARN_GUIDE.last()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)
                }
            }
        }
    }
}

// ── Shop item card ────────────────────────────────────────────────────────────

@Composable
private fun ShopItemCard(
    item       : LeafShopItem,
    isOwned    : Boolean,
    canAfford  : Boolean,
    isHindi    : Boolean,
    onBuy      : () -> Unit
) {
    val borderColor = when {
        isOwned   -> MaterialTheme.colorScheme.primary
        canAfford -> item.accentColor.copy(alpha = 0.3f)
        else      -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(if (isOwned) 0.dp else 2.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(item.bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(item.emoji, fontSize = 28.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        item.title,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                    // Cost chip
                    if (!isOwned) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (canAfford) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                "${item.costLeaves} 🍃",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // English description
                Text(
                    if (isHindi) item.descHi else item.descEn,
                    fontSize = 13.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show both languages if Hindi user
                if (isHindi) {
                    Spacer(Modifier.height(2.dp))
                    Text(item.descEn, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                }

                Spacer(Modifier.height(12.dp))

                if (isOwned) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Owned", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Button(
                        onClick  = onBuy,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape    = RoundedCornerShape(16.dp),
                        enabled  = canAfford,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (canAfford) "Buy 🍃" else "Need ${item.costLeaves} 🍃",
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color      = if (canAfford) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ── Purchase confirmation dialog ──────────────────────────────────────────────

@Composable
private fun PurchaseDialog(
    item      : LeafShopItem,
    canAfford : Boolean,
    onConfirm : () -> Unit,
    onDismiss : () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(item.emoji, fontSize = 36.sp) },
        title = {
            Text(
                "Buy ${item.title}?",
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item.descEn, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        "Cost: ${item.costLeaves} 🍃",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary,
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onConfirm,
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape    = RoundedCornerShape(24.dp)
            ) {
                Text("Buy Now", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape   = RoundedCornerShape(24.dp)
            ) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape          = RoundedCornerShape(20.dp)
    )
}
