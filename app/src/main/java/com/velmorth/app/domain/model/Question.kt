package com.velmorth.app.domain.model

enum class QuestionType {
    MULTIPLE_CHOICE,    // Pick the correct translation
    FILL_IN_BLANK,      // Type the missing word
    WORD_MATCH,         // Match word to translation
    TRUE_OR_FALSE,      // Statement correct or not
    LISTENING,          // Listen and answer (premium)
    SPEAKING,           // Repeat the phrase (premium)
}

data class Question(
    val id: String,
    val lessonId: String,
    val type: QuestionType,
    val prompt: String,             // The question text / instruction
    val targetWord: String = "",    // The word/phrase being tested
    val options: List<String> = emptyList(),   // For MCQ / word match
    val correctAnswer: String,      // The correct answer string
    val explanation: String = "",   // Why this is correct (shown after)
    val audioUrl: String = "",      // For listening questions
    val imageUrl: String = "",      // For visual questions
    val orderIndex: Int = 0,
    val xpValue: Int = 5,
)
