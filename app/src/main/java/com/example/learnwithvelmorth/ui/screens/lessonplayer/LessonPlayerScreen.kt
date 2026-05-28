package com.example.learnwithvelmorth.ui.screens.lessonplayer

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learnwithvelmorth.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import com.example.learnwithvelmorth.ui.components.VelmorthCharacterViewModel
import com.example.learnwithvelmorth.domain.VelmorthTrigger
import javax.inject.Inject

// ─────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────

enum class QuestionType { MULTIPLE_CHOICE, FILL_IN_BLANK }

data class LessonQuestion(
    val id: String,
    val type: QuestionType,
    val prompt: String,
    val targetWord: String? = null,
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String
)

data class LessonPlayerUiState(
    val lessonId: String = "",
    val questions: List<LessonQuestion> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val isAnswered: Boolean = false,
    val selectedAnswer: String? = null,
    val isCorrect: Boolean = false,
    val fillInText: String = "",
    val isComplete: Boolean = false
) {
    val currentQuestion: LessonQuestion? get() = questions.getOrNull(currentQuestionIndex)
    val totalQuestions: Int get() = questions.size
    val progress: Float get() = if (totalQuestions == 0) 0f
        else (currentQuestionIndex + (if (isAnswered) 1 else 0)).toFloat() / totalQuestions
}

// ─────────────────────────────────────────────
// Dummy data
// ─────────────────────────────────────────────

private val spanishQuestions = listOf(
    LessonQuestion(
        id = "q1",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "What does this word mean?",
        targetWord = "Árbol",
        options = listOf("Tree", "River", "Mountain", "Cloud"),
        correctAnswer = "Tree",
        explanation = "\"Árbol\" means \"Tree\" in Spanish. Trees are called árboles in the plural!"
    ),
    LessonQuestion(
        id = "q2",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "Which sentence is correct in Spanish?",
        targetWord = null,
        options = listOf("Yo hablas español", "Yo hablo español", "Yo hablamos español", "Yo hablan español"),
        correctAnswer = "Yo hablo español",
        explanation = "\"Yo hablo\" uses the first-person singular conjugation of hablar."
    ),
    LessonQuestion(
        id = "q3",
        type = QuestionType.FILL_IN_BLANK,
        prompt = "Fill in the blank: \"El gato ___ en la silla.\" (The cat sits on the chair.)",
        targetWord = "está sentado",
        options = emptyList(),
        correctAnswer = "está sentado",
        explanation = "\"Está sentado\" is the correct phrase meaning \"is seated\"."
    ),
    LessonQuestion(
        id = "q4",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "How do you say \"Good morning\" in Spanish?",
        targetWord = null,
        options = listOf("Buenas noches", "Buenos días", "Buenas tardes", "Hola amigo"),
        correctAnswer = "Buenos días",
        explanation = "\"Buenos días\" literally means \"Good days\" and is used as \"Good morning\"."
    ),
    LessonQuestion(
        id = "q5",
        type = QuestionType.MULTIPLE_CHOICE,
        prompt = "What is the Spanish word for \"Water\"?",
        targetWord = "Agua",
        options = listOf("Fuego", "Tierra", "Agua", "Aire"),
        correctAnswer = "Agua",
        explanation = "\"Agua\" means water. It's one of the four classical elements in Spanish!"
    )
)

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

@HiltViewModel
class LessonPlayerViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LessonPlayerUiState())
    val uiState: StateFlow<LessonPlayerUiState> = _uiState.asStateFlow()

    private var onCompleteCallback: ((Int) -> Unit)? = null

    fun initLesson(lessonId: String, onComplete: (Int) -> Unit) {
        onCompleteCallback = onComplete
        _uiState.update { it.copy(lessonId = lessonId, questions = spanishQuestions) }
    }

    fun updateFillInText(text: String) {
        _uiState.update { it.copy(fillInText = text) }
    }

    fun selectAnswer(answer: String) {
        val state = _uiState.value
        if (state.isAnswered) return
        val correct = answer.trim().equals(
            state.currentQuestion?.correctAnswer?.trim(),
            ignoreCase = true
        )
        _uiState.update {
            it.copy(
                isAnswered = true,
                selectedAnswer = answer,
                isCorrect = correct,
                score = if (correct) it.score + 1 else it.score
            )
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        if (!state.isAnswered) return
        val nextIndex = state.currentQuestionIndex + 1
        if (nextIndex >= state.totalQuestions) {
            _uiState.update { it.copy(isComplete = true) }
            onCompleteCallback?.invoke(state.score)
        } else {
            _uiState.update {
                it.copy(
                    currentQuestionIndex = nextIndex,
                    isAnswered = false,
                    selectedAnswer = null,
                    isCorrect = false,
                    fillInText = ""
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@Composable
fun LessonPlayerScreen(
    lessonId: String,
    onComplete: (score: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: LessonPlayerViewModel = hiltViewModel(),
    characterViewModel: VelmorthCharacterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) {
        viewModel.initLesson(lessonId, onComplete)
        characterViewModel.fireEvent(VelmorthTrigger.LESSON_START)
    }

    // React to answer events
    val isAnswered = uiState.isAnswered
    val isCorrect  = uiState.isCorrect
    LaunchedEffect(isAnswered) {
        if (isAnswered) {
            val trigger = if (isCorrect) VelmorthTrigger.CORRECT_ANSWER else VelmorthTrigger.WRONG_ANSWER
            characterViewModel.fireEvent(trigger)
        }
    }

    // React to lesson completion
    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            val total = uiState.totalQuestions
            val score = uiState.score
            if (total > 0 && score == total) {
                characterViewModel.fireEvent(VelmorthTrigger.PERFECT_SCORE)
            } else {
                characterViewModel.fireEvent(VelmorthTrigger.LESSON_COMPLETE)
            }
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF8F4EC),
            Color(0xFFEBF5EE)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Bar ──────────────────────────────
            LessonTopBar(
                currentIndex = uiState.currentQuestionIndex,
                total = uiState.totalQuestions,
                progress = uiState.progress,
                onBack = onBack
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        label = "question_transition"
                    ) {
                        QuestionContent(
                            question = question,
                            state = uiState,
                            onSelectAnswer = viewModel::selectAnswer,
                            onFillInTextChange = viewModel::updateFillInText,
                            onSubmitFillIn = { viewModel.selectAnswer(uiState.fillInText) }
                        )
                    }
                }
            }

            // ── Mascot Reaction ──────────────────────
            MascotReactionBar(
                isAnswered = uiState.isAnswered,
                isCorrect = uiState.isCorrect
            )

            // ── Next Button ──────────────────────────
            AnimatedVisibility(
                visible = uiState.isAnswered,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                NextButton(onClick = viewModel::nextQuestion)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Top bar with progress
// ─────────────────────────────────────────────

@Composable
private fun LessonTopBar(
    currentIndex: Int,
    total: Int,
    progress: Float,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1E8D0))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1B4332)
                )
            }
            Text(
                text = "Question ${currentIndex + 1} of $total",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF1B4332),
                    fontWeight = FontWeight.SemiBold
                )
            )
            // Leaf icon placeholder
            Text(text = "🌿", fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50)),
            color = Color(0xFF40916C),
            trackColor = Color(0xFFCBE4D2),
        )
    }
}

// ─────────────────────────────────────────────
// Question content switcher
// ─────────────────────────────────────────────

@Composable
private fun QuestionContent(
    question: LessonQuestion,
    state: LessonPlayerUiState,
    onSelectAnswer: (String) -> Unit,
    onFillInTextChange: (String) -> Unit,
    onSubmitFillIn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Question Card ────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(24.dp)),
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5EE))
                            .padding(horizontal = 24.dp, vertical = 10.dp)
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

        Spacer(modifier = Modifier.height(20.dp))

        // ── Answer Input ─────────────────────────────
        when (question.type) {
            QuestionType.MULTIPLE_CHOICE -> {
                MultipleChoiceOptions(
                    options = question.options,
                    correctAnswer = question.correctAnswer,
                    selectedAnswer = state.selectedAnswer,
                    isAnswered = state.isAnswered,
                    onSelect = onSelectAnswer
                )
            }
            QuestionType.FILL_IN_BLANK -> {
                FillInBlankInput(
                    value = state.fillInText,
                    onValueChange = onFillInTextChange,
                    isAnswered = state.isAnswered,
                    isCorrect = state.isCorrect,
                    onSubmit = onSubmitFillIn
                )
            }
        }

        // ── Explanation ──────────────────────────────
        AnimatedVisibility(
            visible = state.isAnswered,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isCorrect) Color(0xFFE8F5EE) else Color(0xFFFFF0EE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = if (state.isCorrect) "💡" else "📖",
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = question.explanation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2D6A4F),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────
// Multiple choice options
// ─────────────────────────────────────────────

@Composable
private fun MultipleChoiceOptions(
    options: List<String>,
    correctAnswer: String,
    selectedAnswer: String?,
    isAnswered: Boolean,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedAnswer
            val isCorrectOption = option == correctAnswer
            val bgColor = when {
                !isAnswered -> Color.White
                isCorrectOption -> Color(0xFF40916C)
                isSelected && !isCorrectOption -> Color(0xFFE05A47)
                else -> Color.White
            }
            val textColor = when {
                !isAnswered -> Color(0xFF1B4332)
                isCorrectOption || (isSelected && !isCorrectOption) -> Color.White
                else -> Color(0xFF6B8F71)
            }
            val borderColor = when {
                !isAnswered -> Color(0xFFCBE4D2)
                isCorrectOption -> Color(0xFF40916C)
                isSelected -> Color(0xFFE05A47)
                else -> Color(0xFFE0E0E0)
            }

            val scale by animateFloatAsState(
                targetValue = if (isSelected && isAnswered) 1.02f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "option_scale"
            )

            Card(
                onClick = { if (!isAnswered) onSelect(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .border(
                        width = if (isSelected || (isAnswered && isCorrectOption)) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = textColor,
                            fontWeight = if (isSelected || (isAnswered && isCorrectOption))
                                FontWeight.Bold else FontWeight.Normal
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (isAnswered) {
                        when {
                            isCorrectOption -> Text("✓", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            isSelected -> Text("✗", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Fill in blank
// ─────────────────────────────────────────────

@Composable
private fun FillInBlankInput(
    value: String,
    onValueChange: (String) -> Unit,
    isAnswered: Boolean,
    isCorrect: Boolean,
    onSubmit: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val borderColor = when {
        !isAnswered -> Color(0xFF40916C)
        isCorrect -> Color(0xFF40916C)
        else -> Color(0xFFE05A47)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (!isAnswered) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = {
                Text(
                    "Type your answer…",
                    color = Color(0xFFADBFAD)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = Color(0xFFCBE4D2),
                focusedTextColor = Color(0xFF1B4332),
                unfocusedTextColor = Color(0xFF1B4332),
                cursorColor = Color(0xFF40916C),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                if (value.isNotBlank() && !isAnswered) onSubmit()
            }),
            enabled = !isAnswered,
            singleLine = true
        )

        if (!isAnswered && value.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    onSubmit()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C))
            ) {
                Text("Check Answer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─────────────────────────────────────────────
// Mascot reaction bar
// ─────────────────────────────────────────────

@Composable
private fun MascotReactionBar(
    isAnswered: Boolean,
    isCorrect: Boolean
) {
    val reactionText = when {
        !isAnswered -> "🌿 Choose your answer!"
        isCorrect -> "🎉 Correct! Well done!"
        else -> "💪 Keep trying! You've got this!"
    }
    val bgColor by animateColorAsState(
        targetValue = when {
            !isAnswered -> Color(0xFFF1E8D0)
            isCorrect -> Color(0xFFD8F3DC)
            else -> Color(0xFFFFE8E4)
        },
        animationSpec = tween(400),
        label = "mascot_bg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = reactionText,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "mascot_text"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color(0xFF1B4332),
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────
// Next button
// ─────────────────────────────────────────────

@Composable
private fun NextButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1B4332)
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "Next",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFFD4A017)
            )
        }
    }
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LessonPlayerScreenPreview() {
    MaterialTheme {
        LessonPlayerScreen(
            lessonId = "lesson_es_01",
            onComplete = {},
            onBack = {}
        )
    }
}
