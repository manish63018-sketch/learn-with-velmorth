package com.velmorth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.velmorth.app.data.local.db.StringListConverter

@Entity(tableName = "questions")
@TypeConverters(StringListConverter::class)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val type: String,           // QuestionType enum name
    val prompt: String,
    val targetWord: String = "",
    val options: List<String> = emptyList(),
    val correctAnswer: String,
    val explanation: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val orderIndex: Int = 0,
    val xpValue: Int = 5,
)
