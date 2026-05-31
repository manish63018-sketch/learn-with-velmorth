package com.velmorth.app.data.model

/**
 * Represents a single exercise item within a lesson, supporting localization and multiple interactive types.
 */
data class Exercise(
    val id: String,
    val type: String, // "multiple_choice", "match", "fill_in_blank", etc.
    val questionEn: String,
    val questionHi: String?,
    val answer: String,
    val answerRomaji: String?,
    val options: List<String>,
    val optionsRomaji: List<String>,
    val correctOptionIndex: Int,
    val pairsJapanese: List<String>,
    val pairsEnglish: List<String>,
    val explanationEn: String?,
    val explanationHi: String?,
    val hintEn: String?,
    val hintHi: String?,
    val xpReward: Int
)
