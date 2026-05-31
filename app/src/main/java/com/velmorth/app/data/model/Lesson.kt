package com.velmorth.app.data.model

/**
 * Represents a unit of curriculum (e.g., Greetings, Numbers).
 * Named CourseUnit to avoid shadowing kotlin.Unit.
 */
data class CourseUnit(
    val unitId: String,
    val title: String,
    val lessons: List<Lesson>
)

/**
 * A topic-based grouping of lessons (e.g. "Food", "Greetings").
 * Sits above CourseUnit in the hierarchy — a category can span multiple units.
 */
data class LessonCategory(
    val categoryId   : String,
    val categoryTitle: String,
    val icon         : String,
    val descriptionEn: String,
    val descriptionHi: String,
    val totalLessons : Int,
    val freeLessons  : Int,
    val premiumLessons: Int,
    val lessonIds    : List<String>   // lesson IDs that belong to this category
)

/**
 * Represents a specific language lesson with rich educational structures.
 */
data class Lesson(
    val id: String,
    val unitId: String,
    val title: String,
    val difficulty: String,
    val isPremium: Boolean,
    val xpReward: Int,
    val lessonGoal: String,
    val vocabulary: List<VocabularyItem>,
    val grammarPoint: GrammarPoint,
    val pronunciation: PronunciationHelp,
    val examples: List<ExampleSentence>,
    val exercises: List<Exercise>,
    val reviewWords: List<String>,
    val unlockRequirement: UnlockRequirement
)

data class VocabularyItem(
    val vocabId: String,
    val kanji: String,
    val kana: String,
    val romaji: String,
    val meaningEn: String,
    val meaningHi: String?,
    val partOfSpeech: String,
    val topicTags: List<String>,
    val frequency: Int,
    val notes: String?
)

data class GrammarPoint(
    val grammarId: String,
    val title: String,
    val structure: String,
    val romajiStructure: String,
    val shortExplanationEn: String,
    val shortExplanationHi: String?,
    val focusExamples: List<String>,
    val focusExamplesRomaji: List<String>,
    val notes: String?
)

data class PronunciationHelp(
    val tipsEn: List<String>,
    val tipsHi: List<String>?,
    val audioIds: List<String>
)

data class ExampleSentence(
    val exampleId: String,
    val japanese: String,
    val romaji: String,
    val translationEn: String,
    val translationHi: String?,
    val highlightVocabIds: List<String>,
    val contextNote: String?
)

data class UnlockRequirement(
    val requiresLessons: List<String>,
    val minXpTotal: Int,
    val requiresPremium: Boolean
)
