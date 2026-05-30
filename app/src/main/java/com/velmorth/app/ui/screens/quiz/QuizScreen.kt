package com.velmorth.app.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.velmorth.app.domain.model.LeafTransaction
import com.velmorth.app.domain.model.LeafTransactionType
import com.velmorth.app.domain.model.UserProgress
import com.velmorth.app.domain.repository.LeafWalletRepository
import com.velmorth.app.domain.repository.LessonRepository
import com.velmorth.app.domain.repository.ProgressRepository
import com.velmorth.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
// Data models (reuse from lessonplayer or define locally)
// ─────────────────────────────────────────────

private enum class QType { MULTIPLE_CHOICE }

data class QuizQuestion(
    val id: String,
    val prompt: String,
    val targetWord: String? = null,
    val options: List<String>,
    val correctAnswer: String
)

data class QuizUiState(
    val lessonId: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isAnswered: Boolean = false,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean = false,
    val timeRemainingSeconds: Int = 60,
    val totalTimeSeconds: Int = 60,
    val isComplete: Boolean = false,
    val isTimerRunning: Boolean = false
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentQuestionIndex)
    val totalQuestions: Int get() = questions.size
    val timerProgress: Float get() = timeRemainingSeconds.toFloat() / totalTimeSeconds
    val leavesEarned: Int get() = (score.toFloat() / totalQuestions.coerceAtLeast(1) * 5).toInt()
    val xpEarned: Int get() = score * 10
    val percentScore: Float get() = score.toFloat() / totalQuestions.coerceAtLeast(1)
    val resultEmoji: String get() = when {
        percentScore > 0.80f -> "🏆"
        percentScore > 0.60f -> "⭐"
        else -> "🌱"
    }
    val resultMessage: String get() = when {
        percentScore > 0.80f -> "Outstanding! You're a forest sage!"
        percentScore > 0.60f -> "Great work! Keep growing!"
        else -> "Every seed needs time to bloom!"
    }
}

// ─────────────────────────────────────────────
// Dummy quiz questions (fallback)
// ─────────────────────────────────────────────

private val quizQuestions = listOf(
    QuizQuestion("q1", "What does \"Perro\" mean?", "Perro",
        listOf("Cat", "Dog", "Bird", "Fish"), "Dog")
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
    private val progressRepository: ProgressRepository,
    private val leafWalletRepository: LeafWalletRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var autoAdvanceJob: Job? = null

    fun initQuiz(lessonId: String) {
        viewModelScope.launch {
            val dbQuestions = lessonRepository.getQuestionsForLesson(lessonId).first()
            val mapped = if (dbQuestions.isNotEmpty()) {
                dbQuestions.map { q ->
                    QuizQuestion(
                        id = q.id,
                        prompt = q.prompt,
                        targetWord = q.targetWord,
                        options = if (q.options.isNotEmpty()) q.options else listOf("True", "False"),
                        correctAnswer = q.correctAnswer
                    )
                }
            } else {
                quizQuestions
            }

            _uiState.update {
                it.copy(
                    lessonId = lessonId,
                    questions = mapped,
                    currentQuestionIndex = 0,
                    score = 0,
                    isAnswered = false,
                    selectedAnswer = null,
                    isCorrect = false,
                    timeRemainingSeconds = 60,
                    isComplete = false,
                    isTimerRunning = true
                )
            }
            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemainingSeconds > 0 && !_uiState.value.isComplete) {
                delay(1000L)
                val current = _uiState.value
                if (!current.isComplete) {
                    val newTime = current.timeRemainingSeconds - 1
                    _uiState.update { it.copy(timeRemainingSeconds = newTime) }
                    if (newTime <= 0) {
                        _uiState.update { it.copy(isComplete = true, isTimerRunning = false) }
                    }
                }
            }
        }
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        if (state.isAnswered || state.isComplete) return
        val correct = answer.trim().equals(
            state.currentQuestion?.correctAnswer?.trim(), ignoreCase = true
        )
        _uiState.update {
            it.copy(
                isAnswered = true,
                selectedAnswer = answer,
                isCorrect = correct,
                score = if (correct) it.score + 1 else it.score
            )
        }
        // Auto-advance after short delay
        autoAdvanceJob?.cancel()
        autoAdvanceJob = viewModelScope.launch {
            delay(800L)
            nextQuestion()
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val nextIndex = state.currentQuestionIndex + 1
        if (nextIndex >= state.totalQuestions) {
            timerJob?.cancel()
            _uiState.update { it.copy(isComplete = true, isTimerRunning = false) }
        } else {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    isAnswered = false,
                    selectedAnswer = null,
                    isCorrect = false
                )
            }
        }
    }

    fun claimReward(onComplete: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            // 1. Save progress
            val progress = UserProgress(
                id = "",
                userId = "local_user",
                lessonId = state.lessonId,
                score = (state.percentScore * 100).toInt(),
                xpEarned = state.xpEarned,
                leavesEarned = state.leavesEarned,
                timeSpentSeconds = state.totalTimeSeconds - state.timeRemainingSeconds,
                attemptsCount = 1,
                completedAt = "",
                incorrectQuestionIds = emptyList()
            )
            progressRepository.saveProgress(progress)

            // 2. Add XP to user
            userRepository.addXp("local_user", state.xpEarned)

            // 3. Add leaves to user balance and transaction history
            userRepository.updateLeafBalance("local_user", state.leavesEarned)
            leafWalletRepository.addTransaction(
                LeafTransaction(
                    id = "",
                    userId = "local_user",
                    amount = state.leavesEarned,
                    type = LeafTransactionType.EARN_LESSON,
                    description = "🌿 Earned from lesson quiz!",
                    timestamp = System.currentTimeMillis()
                )
            )

            // 4. Update user streak
            userRepository.updateStreak("local_user")

            // 5. Mark lesson as completed
            lessonRepository.markLessonCompleted(state.lessonId, (state.percentScore * 100).toInt())

            // 6. Find next sequential lesson and unlock it
            val user = userRepository.getUser().first()
            if (user != null) {
                val lessons = lessonRepository.getLessonsForLanguage(user.selectedLanguageId).first()
                val sorted = lessons.sortedBy { it.orderIndex }
                val currentIndex = sorted.indexOfFirst { it.id == state.lessonId }
                if (currentIndex != -1 && currentIndex + 1 < sorted.size) {
                    val nextLesson = sorted[currentIndex + 1]
                    lessonRepository.unlockLesson(nextLesson.id)
                }
            }

            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        autoAdvanceJob?.cancel()
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun QuizScreen(
    lessonId: String,
    onComplete: () -> Unit,
    onBack: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) {
        viewModel.initQuiz(lessonId)
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFFEBF5EE))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundBrush)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Quiz Top Bar ─────────────────────────
            QuizTopBar(
                currentIndex = uiState.currentQuestionIndex,
                total = uiState.totalQuestions,
                score = uiState.score,
                timerProgress = uiState.timerProgress,
                timeRemaining = uiState.timeRemainingSeconds,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Question Card ────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                uiState.currentQuestion?.let { question ->
                    AnimatedContent(
                        targetState = uiState.currentQuestionIndex,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        },
                        label = "quiz_question_transition"
                    ) {
                        QuizQuestionCard(
                            question = question,
                            state = uiState,
                            onSelectAnswer = viewModel::selectAnswer
                        )
                    }
                }
            }
        }

        // ── Results Overlay ──────────────────────────
        AnimatedVisibility(
            visible = uiState.isComplete,
            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut()
        ) {
            QuizResultsOverlay(
                uiState = uiState,
                onClaimReward = { viewModel.claimReward(onComplete) },
                onBack = onBack
            )
        }
    }
}

// ─────────────────────────────────────────────
// Quiz top bar
// ─────────────────────────────────────────────

@Composable
private fun QuizTopBar(
    currentIndex: Int,
    total: Int,
    score: Int,
    timerProgress: Float,
    timeRemaining: Int,
    onBack: () -> Unit
) {
    val timerColor by animateColorAsState(
        targetValue = when {
            timerProgress > 0.5f -> Color(0xFF52B788)
            timerProgress > 0.25f -> Color(0xFFD4A017)
            else -> Color(0xFFE05A47)
        },
        animationSpec = tween(500),
        label = "timer_color"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        // Circular Timer
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { timerProgress },
                modifier = Modifier.size(56.dp),
                color = timerColor,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeWidth = 5.dp,
                strokeCap = StrokeCap.Round
            )
            Text(
                text = "$timeRemaining",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }

        // Score chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$score/$total ✓",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ─────────────────────────────────────────────
// Quiz question card
// ─────────────────────────────────────────────

@Composable
private fun QuizQuestionCard(
    question: QuizQuestion,
    state: QuizUiState,
    onSelectAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            repeat(state.totalQuestions) { idx ->
                Box(
                    modifier = Modifier
                        .size(if (idx == state.currentQuestionIndex) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                idx < state.currentQuestionIndex -> Color(0xFF52B788)
                                idx == state.currentQuestionIndex -> Color.White
                                else -> Color.White.copy(alpha = 0.3f)
                            }
                        )
                )
            }
        }

        // Question card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFF1B4332),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                question.targetWord?.let { word ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5EE))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = word,
                            style = MaterialTheme.typography.displaySmall.copy(
                                color = Color(0xFF40916C),
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            question.options.forEach { option ->
                QuizOptionTile(
                    option = option,
                    isSelected = option == state.selectedAnswer,
                    isCorrect = option == question.correctAnswer,
                    isAnswered = state.isAnswered,
                    onSelect = { onSelectAnswer(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// Quiz option tile
// ─────────────────────────────────────────────

@Composable
private fun QuizOptionTile(
    option: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswered: Boolean,
    onSelect: () -> Unit
) {
    val bgColor = when {
        !isAnswered -> Color.White
        isCorrect -> Color(0xFF40916C)
        isSelected -> Color(0xFFE05A47)
        else -> Color.White
    }
    val textColor = when {
        !isAnswered -> Color(0xFF1B4332)
        isCorrect || isSelected -> Color.White
        else -> Color(0xFF9DBAA3)
    }
    val scale by animateFloatAsState(
        targetValue = if (isSelected && isAnswered) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "tile_scale"
    )

    Card(
        onClick = { if (!isAnswered) onSelect() },
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = option,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    fontWeight = if (isSelected || (isAnswered && isCorrect)) FontWeight.Bold else FontWeight.Normal
                ),
                modifier = Modifier.weight(1f)
            )
            if (isAnswered) {
                when {
                    isCorrect -> Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    isSelected -> Text("✗", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Results overlay
// ─────────────────────────────────────────────

@Composable
private fun QuizResultsOverlay(
    uiState: QuizUiState,
    onClaimReward: () -> Unit,
    onBack: () -> Unit
) {
    // Leaf animation
    val leafScale by rememberInfiniteTransition(label = "leaf_pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leaf_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B4332).copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(24.dp, RoundedCornerShape(32.dp)),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1E8D0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Result emoji
                Text(
                    text = uiState.resultEmoji,
                    fontSize = 72.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quiz Complete!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color(0xFF1B4332),
                        fontWeight = FontWeight.ExtraBold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.resultMessage,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF2D6A4F),
                        textAlign = TextAlign.Center
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFCBE4D2))
                Spacer(modifier = Modifier.height(24.dp))

                // Score breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultStatColumn(
                        icon = "🎯",
                        label = "Score",
                        value = "${uiState.score}/${uiState.totalQuestions}"
                    )
                    ResultStatColumn(
                        icon = "🍃",
                        label = "Leaves",
                        value = "+${uiState.leavesEarned}",
                        animScale = leafScale
                    )
                    ResultStatColumn(
                        icon = "⚡",
                        label = "XP",
                        value = "+${uiState.xpEarned}"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Claim reward button
                Button(
                    onClick = onClaimReward,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF40916C)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Text(
                        text = "🍃  Claim Reward",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onBack) {
                    Text(
                        text = "Back to Home",
                        color = Color(0xFF2D6A4F),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Result stat column
// ─────────────────────────────────────────────

@Composable
private fun ResultStatColumn(
    icon: String,
    label: String,
    value: String,
    animScale: Float = 1f
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = icon,
            fontSize = 32.sp,
            modifier = Modifier.scale(animScale)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = Color(0xFF1B4332),
                fontWeight = FontWeight.ExtraBold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color(0xFF52B788)
            )
        )
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QuizScreenPreview() {
    MaterialTheme {
        QuizScreen(
            lessonId = "lesson_es_01",
            onComplete = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun QuizResultsPreview() {
    MaterialTheme {
        QuizResultsOverlay(
            uiState = QuizUiState(
                questions = quizQuestions,
                score = 8,
                isComplete = true,
                currentQuestionIndex = 9
            ),
            onClaimReward = {},
            onBack = {}
        )
    }
}
