package com.velmorth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores all pre-programmed Velmorth dialogue lines.
 * Each row corresponds to one sentence Velmorth can speak.
 *
 * @param trigger  — the event that activates this line, e.g. "lesson_complete"
 * @param emotion  — one of: happy | sad | excited | thinking | proud | idle
 * @param category — broad grouping: greeting | lesson | quiz | shop | motivation
 * @param language — "en" (default) or any future supported locale
 * @param targetLangLine — optional foreign-language parallel line shown in bubble
 * @param voiceSpeed — TTS speech rate multiplier (default 1.0)
 * @param weight   — higher weight = picked more often when multiple lines match the same trigger
 */
@Entity(tableName = "velmorth_dialogues")
data class DialogueEntity(
    @PrimaryKey val id: String,
    val trigger: String,
    val text: String,
    val emotion: String,
    val category: String,
    val language: String = "en",
    val targetLangLine: String = "",
    val voiceSpeed: Float = 1.0f,
    val weight: Int = 1,
)
