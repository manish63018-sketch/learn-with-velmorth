package com.velmorth.app.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.velmorth.app.domain.model.LeafTransaction
import com.velmorth.app.domain.model.LeafTransactionType
import com.velmorth.app.domain.repository.LeafWalletRepository
import com.velmorth.app.domain.repository.UserRepository
import com.velmorth.app.theme.LearnWithVelmorthTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class ShopItem(
    val id: String,
    val emoji: String,
    val name: String,
    val description: String,
    val cost: Int,
)

val shopCatalog = listOf(
    ShopItem("streak_freeze",      "❄️",  "Streak Freeze",       "Keep your streak safe for one day",   cost = 30),
    ShopItem("hint_pack",          "💡",  "Hint Pack",           "10 hints for tough questions",         cost = 20),
    ShopItem("double_xp_weekend",  "⚡",  "Double XP Weekend",   "2× XP for an entire weekend",          cost = 50),
    ShopItem("theme_ocean",        "🌊",  "Theme: Ocean",        "Serene ocean blue interface",          cost = 100),
    ShopItem("theme_desert",       "🌵",  "Theme: Desert",       "Warm desert sand interface",           cost = 100),
    ShopItem("premium_trial",      "👑",  "Premium Trial",       "7-day premium access — free features", cost = 200),
)

// ─────────────────────────────────────────────────────────────────────────────
// UI State
// ─────────────────────────────────────────────────────────────────────────────

data class LeafShopUiState(
    val leafBalance: Int = 0,
    val ownedItemIds: Set<String> = emptySet(),
    val purchaseError: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class LeafShopViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val leafWalletRepository: LeafWalletRepository,
) : ViewModel() {

    // Owned item IDs derived from transaction history (SPEND_SHOP type, item.id stored in description)
    private val ownedItems: StateFlow<Set<String>> =
        leafWalletRepository.getTransactionHistory("local_user")
            .map { transactions ->
                transactions
                    .filter { it.type == LeafTransactionType.SPEND_SHOP }
                    .map { it.description } // item.id is stored in description field
                    .toSet()
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Balance derived from user entity (single source of truth)
    private val leafBalance: StateFlow<Int> =
        userRepository.getUser()
            .map { it?.leafBalance ?: 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _purchaseError = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LeafShopUiState> = combine(
        leafBalance, ownedItems, _purchaseError
    ) { balance, owned, error ->
        LeafShopUiState(
            leafBalance = balance,
            ownedItemIds = owned,
            purchaseError = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LeafShopUiState())

    fun purchase(item: ShopItem) {
        viewModelScope.launch {
            val user = userRepository.getUser().first()
            val currentBalance = user?.leafBalance ?: 0

            // Guard: already owned
            if (ownedItems.value.contains(item.id)) {
                _purchaseError.update { "You already own ${item.name}." }
                return@launch
            }
            // Guard: insufficient balance
            if (currentBalance < item.cost) {
                _purchaseError.update { "Not enough leaves to buy ${item.name}." }
                return@launch
            }

            // Deduct balance atomically
            userRepository.updateLeafBalance("local_user", -item.cost)

            // Record purchase transaction (item.id stored in description for owned tracking)
            leafWalletRepository.addTransaction(
                LeafTransaction(
                    userId      = "local_user",
                    amount      = -item.cost,
                    type        = LeafTransactionType.SPEND_SHOP,
                    description = item.id, // used to track owned items
                    timestamp   = System.currentTimeMillis(),
                )
            )
        }
    }

    fun clearError() {
        _purchaseError.update { null }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Colour palette
// ─────────────────────────────────────────────────────────────────────────────

private val ForestDeep = Color(0xFF1B4332)
private val MossGreen  = Color(0xFF40916C)
private val CreamWhite = Color(0xFFF1E8D0)
private val LeafGold   = Color(0xFFD4A017)
private val GoldLight  = Color(0xFFF5CC50)
private val GoldDark   = Color(0xFFB8860B)

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeafShopScreen(
    onBack: () -> Unit,
    viewModel: LeafShopViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingItem by remember { mutableStateOf<ShopItem?>(null) }

    // Show purchase error as a snackbar-style snack
    state.purchaseError?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = CreamWhite,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    Brush.horizontalGradient(listOf(ForestDeep, GoldDark))
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Gradient header ───────────────────────────────────────────────
            LeafShopHeader(leafBalance = state.leafBalance)

            // ── Grid ──────────────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shopCatalog, key = { it.id }) { item ->
                    ShopItemCard(
                        item        = item,
                        leafBalance = state.leafBalance,
                        isOwned     = state.ownedItemIds.contains(item.id),
                        onBuyClick  = { pendingItem = item },
                    )
                }
            }
        }
    }

    // ── Confirm dialog ────────────────────────────────────────────────────────
    pendingItem?.let { item ->
        PurchaseConfirmDialog(
            item        = item,
            leafBalance = state.leafBalance,
            onConfirm   = {
                viewModel.purchase(item)
                pendingItem = null
            },
            onDismiss   = { pendingItem = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LeafShopHeader(leafBalance: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(ForestDeep, GoldDark))
            )
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Column {
            Text(
                text  = "Leaf Shop 🍃",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color      = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = "Spend your hard-earned leaves",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.80f),
                ),
            )
            Spacer(Modifier.height(16.dp))

            // Large balance display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(listOf(GoldLight, LeafGold))
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text     = "🍃",
                            fontSize = 22.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text  = "$leafBalance",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color      = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                            ),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text  = "leaves",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.90f),
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shop item card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ShopItemCard(
    item: ShopItem,
    leafBalance: Int,
    isOwned: Boolean,
    onBuyClick: () -> Unit,
) {
    val canAfford = leafBalance >= item.cost

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(20.dp)),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Emoji icon in coloured circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isOwned    -> MossGreen.copy(alpha = 0.15f)
                            canAfford  -> LeafGold.copy(alpha = 0.15f)
                            else       -> Color(0xFFE5E7EB)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = item.emoji, fontSize = 28.sp)
            }

            Text(
                text  = item.name,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color      = ForestDeep,
                ),
                textAlign = TextAlign.Center,
            )

            Text(
                text  = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF6B7280),
                ),
                textAlign = TextAlign.Center,
                maxLines  = 2,
            )

            // Cost badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            isOwned   -> MossGreen.copy(alpha = 0.15f)
                            canAfford -> LeafGold.copy(alpha = 0.15f)
                            else      -> Color(0xFFF3F4F6)
                        }
                    )
                    .border(
                        1.dp,
                        when {
                            isOwned   -> MossGreen
                            canAfford -> LeafGold
                            else      -> Color(0xFFD1D5DB)
                        },
                        RoundedCornerShape(50),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text  = if (isOwned) "✓ Owned" else "🍃 ${item.cost}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = when {
                            isOwned   -> MossGreen
                            canAfford -> LeafGold
                            else      -> Color(0xFF9CA3AF)
                        },
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }

            // Action button / state indicator
            when {
                isOwned -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(MossGreen.copy(alpha = 0.12f))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = "✓ In your collection",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MossGreen,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                canAfford -> {
                    Button(
                        onClick = onBuyClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape  = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(MossGreen, ForestDeep)),
                                    RoundedCornerShape(50),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = "Buy",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFFF3F4F6))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text  = "Need more leaves",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.SemiBold,
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Purchase confirm dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PurchaseConfirmDialog(
    item: ShopItem,
    leafBalance: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val afterBalance = leafBalance - item.cost

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = item.emoji, fontSize = 52.sp)

                Text(
                    text  = "Confirm purchase",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color      = ForestDeep,
                    ),
                )

                Text(
                    text  = item.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF4B5563),
                    ),
                )

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "Cost",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                    )
                    Text(
                        text  = "🍃 ${item.cost}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LeafGold,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "Balance after",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF6B7280)),
                    )
                    Text(
                        text  = "🍃 $afterBalance",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MossGreen,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape  = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B7280)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D5DB)),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        shape  = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(listOf(LeafGold, GoldDark)),
                                    RoundedCornerShape(50),
                                )
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text  = "Confirm",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color      = Color.White,
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun LeafShopPreview() {
    LearnWithVelmorthTheme {
        // Preview using a static state snapshot (no ViewModel in preview)
        LeafShopScreenContent(
            state = LeafShopUiState(leafBalance = 85, ownedItemIds = setOf("hint_pack")),
            onBack = {},
            onRequestPurchase = {},
        )
    }
}

@Preview(showBackground = true, name = "Leaf Shop – Broke")
@Composable
private fun LeafShopBrokePreview() {
    LearnWithVelmorthTheme {
        LeafShopScreenContent(
            state = LeafShopUiState(leafBalance = 10),
            onBack = {},
            onRequestPurchase = {},
        )
    }
}

// Internal stateless content composable used by preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeafShopScreenContent(
    state: LeafShopUiState,
    onBack: () -> Unit,
    onRequestPurchase: (ShopItem) -> Unit,
) {
    var pendingItem by remember { mutableStateOf<ShopItem?>(null) }

    Scaffold(
        containerColor = CreamWhite,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.horizontalGradient(listOf(ForestDeep, GoldDark))),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            LeafShopHeader(leafBalance = state.leafBalance)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shopCatalog, key = { it.id }) { item ->
                    ShopItemCard(
                        item        = item,
                        leafBalance = state.leafBalance,
                        isOwned     = state.ownedItemIds.contains(item.id),
                        onBuyClick  = { pendingItem = item },
                    )
                }
            }
        }
    }

    pendingItem?.let { item ->
        PurchaseConfirmDialog(
            item        = item,
            leafBalance = state.leafBalance,
            onConfirm   = {
                onRequestPurchase(item)
                pendingItem = null
            },
            onDismiss = { pendingItem = null },
        )
    }
}
