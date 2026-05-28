package com.example.learnwithvelmorth.ui.screens.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learnwithvelmorth.theme.LearnWithVelmorthTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class ReviewCard(
    val id: String,
    val frontText: String,
    val backText: String,
    /** 0 = seedling 🌱, 1 = sprout 🌿, 2 = tree 🌲 */
    val plantLevel: Int = 0,
    val isRevealed: Boolean = false,
)

data class ReviewGardenUiState(
    val cards: List<ReviewCard> = emptyList(),
    val streak: Int = 0,
    val isLoading: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class ReviewViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewGardenUiState())
    val uiState: StateFlow<ReviewGardenUiState> = _uiState.asStateFlow()

    /** Convenience accessor used by screen. */
    val reviewCards: StateFlow<List<ReviewCard>> get() = MutableStateFlow(_uiState.value.cards)

    init {
        loadDummyCards()
    }

    private fun loadDummyCards() {
        _uiState.update {
            it.copy(
                streak = 5,
                cards = listOf(
                    ReviewCard("1", "Hola",        "Hello",        plantLevel = 2),
                    ReviewCard("2", "Gracias",     "Thank you",    plantLevel = 1),
                    ReviewCard("3", "Agua",        "Water",        plantLevel = 0),
                    ReviewCard("4", "Casa",        "House",        plantLevel = 1),
                    ReviewCard("5", "Perro",       "Dog",          plantLevel = 0),
                    ReviewCard("6", "Gato",        "Cat",          plantLevel = 2),
                    ReviewCard("7", "Libro",       "Book",         plantLevel = 0),
                    ReviewCard("8", "Árbol",       "Tree",         plantLevel = 1),
                ),
            )
        }
    }

    fun revealCard(cardId: String) {
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == cardId) card.copy(isRevealed = true) else card
                }
            )
        }
    }

    fun markEasy(cardId: String) {
        _uiState.update { state ->
            state.copy(
                streak = state.streak + 1,
                cards = state.cards.filter { it.id != cardId },
            )
        }
    }

    fun markHard(cardId: String) {
        _uiState.update { state ->
            state.copy(
                cards = state.cards.map { card ->
                    if (card.id == cardId) card.copy(isRevealed = false) else card
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewGardenScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReviewGardenContent(
        uiState = uiState,
        onBack = onBack,
        onReveal = viewModel::revealCard,
        onMarkEasy = viewModel::markEasy,
        onMarkHard = viewModel::markHard,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewGardenContent(
    uiState: ReviewGardenUiState,
    onBack: () -> Unit,
    onReveal: (String) -> Unit,
    onMarkEasy: (String) -> Unit,
    onMarkHard: (String) -> Unit,
) {
    val forestDeep   = Color(0xFF1B4332)
    val mossGreen    = Color(0xFF40916C)
    val creamWhite   = Color(0xFFF1E8D0)
    val leafGold     = Color(0xFFD4A017)
    val streakOrange = Color(0xFFFF6B35)

    Scaffold(
        containerColor = creamWhite,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                modifier = Modifier.background(
                    Brush.verticalGradient(listOf(forestDeep, mossGreen))
                ),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Gradient header ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(forestDeep, mossGreen)))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Column {
                    Text(
                        text = "Review Garden 🌻",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Water your plants with practice",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                        ),
                    )

                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatChip(
                            label = "${uiState.cards.size} cards due",
                            backgroundColor = Color.White.copy(alpha = 0.20f),
                            textColor = Color.White,
                        )
                        StatChip(
                            label = "Streak bonus 🔥 ×${uiState.streak}",
                            backgroundColor = streakOrange.copy(alpha = 0.85f),
                            textColor = Color.White,
                        )
                    }
                }
            }

            // ── Body ──────────────────────────────────────────────────────────
            if (uiState.cards.isEmpty()) {
                EmptyGardenState(creamWhite = creamWhite, mossGreen = mossGreen)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(uiState.cards, key = { _, card -> card.id }) { index, card ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { it / 4 * (index + 1) },
                            ),
                        ) {
                            PlantCard(
                                card = card,
                                leafGold = leafGold,
                                mossGreen = mossGreen,
                                forestDeep = forestDeep,
                                onReveal = { onReveal(card.id) },
                                onMarkEasy = { onMarkEasy(card.id) },
                                onMarkHard = { onMarkHard(card.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatChip(
    label: String,
    backgroundColor: Color,
    textColor: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

@Composable
private fun PlantCard(
    card: ReviewCard,
    leafGold: Color,
    mossGreen: Color,
    forestDeep: Color,
    onReveal: () -> Unit,
    onMarkEasy: () -> Unit,
    onMarkHard: () -> Unit,
) {
    val plantEmoji = when (card.plantLevel) {
        2    -> "🌲"
        1    -> "🌿"
        else -> "🌱"
    }

    val revealAlpha by animateFloatAsState(
        targetValue = if (card.isRevealed) 1f else 0f,
        animationSpec = tween(400),
        label = "revealAlpha",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Plant level badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(mossGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "$plantEmoji Level ${card.plantLevel + 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = mossGreen,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Text(
                    text = plantEmoji,
                    fontSize = 32.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Front word
            Text(
                text = card.frontText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = forestDeep,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            // Reveal / translation
            if (!card.isRevealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(mossGreen.copy(alpha = 0.10f))
                        .clickable { onReveal() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "👆 Tap to reveal",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = mossGreen,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer(alpha = revealAlpha),
                ) {
                    Text(
                        text = card.backText,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = leafGold,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(16.dp))

                    // 👍 / 👎 buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Hard button
                        OutlinedButton(
                            onClick = onMarkHard,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFE63946),
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, Color(0xFFE63946)
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "Hard",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Hard")
                        }

                        // Easy button
                        Button(
                            onClick = onMarkEasy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = mossGreen,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "Easy",
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Easy")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGardenState(creamWhite: Color, mossGreen: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(creamWhite),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Text(text = "🌸", fontSize = 80.sp)
            Text(
                text = "Garden is fresh!",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = mossGreen,
                ),
            )
            Text(
                text = "Come back tomorrow to water your plants 💧",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF6B7280),
                ),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            // Mascot placeholder
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(mossGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "🦦", fontSize = 52.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun ReviewGardenPreview() {
    LearnWithVelmorthTheme {
        ReviewGardenContent(
            uiState = ReviewGardenUiState(
                streak = 5,
                cards = listOf(
                    ReviewCard("1", "Hola",    "Hello",    plantLevel = 2),
                    ReviewCard("2", "Gracias", "Thank you",plantLevel = 1, isRevealed = true),
                    ReviewCard("3", "Agua",    "Water",    plantLevel = 0),
                ),
            ),
            onBack = {},
            onReveal = {},
            onMarkEasy = {},
            onMarkHard = {},
        )
    }
}

@Preview(showBackground = true, name = "Empty Garden")
@Composable
private fun ReviewGardenEmptyPreview() {
    LearnWithVelmorthTheme {
        ReviewGardenContent(
            uiState = ReviewGardenUiState(streak = 0, cards = emptyList()),
            onBack = {},
            onReveal = {},
            onMarkEasy = {},
            onMarkHard = {},
        )
    }
}
