package com.velmorth.app.data.local

import android.content.Context
import com.velmorth.app.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/**
 * Loads and parses JSON lesson curriculum content from the app assets.
 * Implements a memory cache to ensure asset reading is only performed once.
 */
class LessonLoader(private val context: Context) {
    
    private var cachedUnits: List<CourseUnit>? = null

    /**
     * Loads the language curriculum units.
     * @param fileName Path to the JSON asset file (default is "lessons/japanese.json").
     * @return List of units with lessons and exercises inside. Returns an empty list if file missing or corrupt.
     */
    fun loadLessons(fileName: String = "lessons/japanese.json"): List<CourseUnit> {
        cachedUnits?.let { return it }

        val unitsList = mutableListOf<CourseUnit>()
        try {
            // Extract language name from fileName (e.g. "lessons/japanese.json" -> "japanese")
            val langKey = fileName.substringAfter("lessons/").substringBefore(".json")
            
            // Try loading from config/units_index.json
            var loadedFromIndex = false
            try {
                val indexJsonString = context.assets.open("config/units_index.json").bufferedReader().use { it.readText() }
                val indexRoot = JSONObject(indexJsonString)
                
                var unitsArray: JSONArray? = null
                if (indexRoot.has("units")) {
                    val indexLang = indexRoot.optString("language", "")
                    val indexLangName = indexRoot.optString("language_name", "")
                    if (indexLang.equals(langKey, ignoreCase = true) || 
                        indexLangName.equals(langKey, ignoreCase = true) ||
                        (langKey.equals("japanese", ignoreCase = true) && indexLang.equals("ja", ignoreCase = true))
                    ) {
                        unitsArray = indexRoot.getJSONArray("units")
                    }
                } else {
                    val languagesObj = indexRoot.optJSONObject("languages")
                    if (languagesObj != null && languagesObj.has(langKey)) {
                        unitsArray = languagesObj.getJSONArray(langKey)
                    }
                }

                if (unitsArray != null) {
                    for (u in 0 until unitsArray.length()) {
                        val unitIndexObj = unitsArray.getJSONObject(u)
                        val unitFile = unitIndexObj.getString("file")
                        
                        // Load and parse the individual unit JSON file
                        val unitJsonString = context.assets.open(unitFile).bufferedReader().use { it.readText() }
                        val unitObj = JSONObject(unitJsonString)
                        unitsList.add(parseUnit(unitObj))
                    }
                    loadedFromIndex = true
                }
            } catch (e: Exception) {
                // If index loading fails, we'll fall back to monolithic loading
                e.printStackTrace()
            }

            if (!loadedFromIndex) {
                val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val unitsArray = root.getJSONArray("units")
                for (i in 0 until unitsArray.length()) {
                    val unitObj = unitsArray.getJSONObject(i)
                    unitsList.add(parseUnit(unitObj))
                }
            }
            cachedUnits = unitsList
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return unitsList
    }

    private fun parseUnit(unitObj: JSONObject): CourseUnit {
        val unitId = unitObj.getString("unit_id")
        val unitTitle = unitObj.getString("unit_title")
        val lessonsArray = unitObj.getJSONArray("lessons")
        val lessonsList = mutableListOf<Lesson>()

        for (j in 0 until lessonsArray.length()) {
            val lessonObj = lessonsArray.getJSONObject(j)
            lessonsList.add(parseLesson(unitId, lessonObj))
        }
        return CourseUnit(unitId, unitTitle, lessonsList)
    }

    private fun parseLesson(unitId: String, lessonObj: JSONObject): Lesson {
        val lessonId = lessonObj.getString("lesson_id")
        val lessonTitle = lessonObj.getString("lesson_title")
        val difficulty = lessonObj.optString("difficulty", "beginner-1")
        val isPremium = lessonObj.optBoolean("is_premium", false)
        val xpReward = lessonObj.optInt("xp_reward", 10)
        val lessonGoal = lessonObj.optString("lesson_goal", "")

        // 1. Vocabulary
        val vocabArray = lessonObj.optJSONArray("vocabulary")
        val vocabList = mutableListOf<VocabularyItem>()
        if (vocabArray != null) {
            for (v in 0 until vocabArray.length()) {
                val vObj = vocabArray.getJSONObject(v)
                vocabList.add(
                    VocabularyItem(
                        vocabId = vObj.getString("vocab_id"),
                        kanji = vObj.getString("kanji"),
                        kana = vObj.getString("kana"),
                        romaji = vObj.getString("romaji"),
                        meaningEn = vObj.getString("meaning_en"),
                        meaningHi = vObj.optNullableString("meaning_hi"),
                        partOfSpeech = vObj.optString("part_of_speech", ""),
                        topicTags = jsonArrayToStringList(vObj.optJSONArray("topic_tags")),
                        frequency = vObj.optInt("frequency", 5),
                        notes = vObj.optNullableString("notes")
                    )
                )
            }
        }

        // 2. Grammar Point
        val gramObj = lessonObj.optJSONObject("grammar_point")
        val grammarPoint = if (gramObj != null) {
            GrammarPoint(
                grammarId = gramObj.getString("grammar_id"),
                title = gramObj.getString("title"),
                structure = gramObj.getString("structure"),
                romajiStructure = gramObj.getString("romaji_structure"),
                shortExplanationEn = gramObj.getString("short_explanation_en"),
                shortExplanationHi = gramObj.optNullableString("short_explanation_hi"),
                focusExamples = jsonArrayToStringList(gramObj.optJSONArray("focus_examples")),
                focusExamplesRomaji = jsonArrayToStringList(gramObj.optJSONArray("focus_examples_romaji")),
                notes = gramObj.optNullableString("notes")
            )
        } else {
            GrammarPoint("", "", "", "", "", null, emptyList(), emptyList(), null)
        }

        // 3. Pronunciation Help
        val pronObj = lessonObj.optJSONObject("pronunciation")
        val pronunciation = if (pronObj != null) {
            PronunciationHelp(
                tipsEn = jsonArrayToStringList(pronObj.optJSONArray("tips_en")),
                tipsHi = jsonArrayToStringList(pronObj.optJSONArray("tips_hi")),
                audioIds = jsonArrayToStringList(pronObj.optJSONArray("audio_ids"))
            )
        } else {
            PronunciationHelp(emptyList(), emptyList(), emptyList())
        }

        // 4. Examples
        val examplesArray = lessonObj.optJSONArray("examples")
        val examplesList = mutableListOf<ExampleSentence>()
        if (examplesArray != null) {
            for (e in 0 until examplesArray.length()) {
                val eObj = examplesArray.getJSONObject(e)
                examplesList.add(
                    ExampleSentence(
                        exampleId = eObj.getString("example_id"),
                        japanese = eObj.getString("japanese"),
                        romaji = eObj.getString("romaji"),
                        translationEn = eObj.getString("translation_en"),
                        translationHi = eObj.optNullableString("translation_hi"),
                        highlightVocabIds = jsonArrayToStringList(eObj.optJSONArray("highlight_vocab_ids")),
                        contextNote = eObj.optNullableString("context_note")
                    )
                )
            }
        }

        // 5. Exercises (1, 2, 3)
        val exercisesList = mutableListOf<Exercise>()
        for (exKey in listOf("exercise_1", "exercise_2", "exercise_3")) {
            if (lessonObj.has(exKey)) {
                val exObj = lessonObj.getJSONObject(exKey)
                val type = exObj.getString("type")
                val id = exObj.optString("exercise_id", "${lessonId}_$exKey")
                val questionEn = exObj.optString("question_en", exObj.optString("prompt_en", ""))
                val questionHi = exObj.optNullableString("question_hi") ?: exObj.optNullableString("prompt_hi")
                val correctAns = exObj.optString("correct_answer", "")
                val correctAnsRomaji = exObj.optNullableString("correct_answer_romaji")
                val correctIdx = exObj.optInt("correct_option_index", -1)
                val xp = exObj.optInt("xp_reward", 3)
                
                val options = jsonArrayToStringList(exObj.optJSONArray("options"))
                val optionsRomaji = jsonArrayToStringList(exObj.optJSONArray("options_romaji"))
                val pairsJp = jsonArrayToStringList(exObj.optJSONArray("pairs_japanese"))
                val pairsEn = jsonArrayToStringList(exObj.optJSONArray("pairs_english"))
                
                val explanationEn = exObj.optNullableString("explanation_en")
                val explanationHi = exObj.optNullableString("explanation_hi")
                val hintEn = exObj.optNullableString("hint_en")
                val hintHi = exObj.optNullableString("hint_hi")

                // Resolve answer for multiple choice if not explicitly stated
                val finalAnswer = if (type == "multiple_choice" && correctIdx in options.indices) {
                    options[correctIdx]
                } else {
                    correctAns
                }

                exercisesList.add(
                    Exercise(
                        id = id,
                        type = type,
                        questionEn = questionEn,
                        questionHi = questionHi,
                        answer = finalAnswer,
                        answerRomaji = correctAnsRomaji,
                        options = options,
                        optionsRomaji = optionsRomaji,
                        correctOptionIndex = correctIdx,
                        pairsJapanese = pairsJp,
                        pairsEnglish = pairsEn,
                        explanationEn = explanationEn,
                        explanationHi = explanationHi,
                        hintEn = hintEn,
                        hintHi = hintHi,
                        xpReward = xp
                    )
                )
            }
        }

        // 6. Review Words
        val reviewWords = jsonArrayToStringList(lessonObj.optJSONArray("review_words"))

        // 7. Unlock Requirement
        val unlockObj = lessonObj.optJSONObject("unlock_requirement")
        val unlockRequirement = if (unlockObj != null) {
            UnlockRequirement(
                requiresLessons = jsonArrayToStringList(unlockObj.optJSONArray("requires_lessons")),
                minXpTotal = unlockObj.optInt("min_xp_total", 0),
                requiresPremium = unlockObj.optBoolean("requires_premium", false)
            )
        } else {
            UnlockRequirement(emptyList(), 0, false)
        }

        return Lesson(
            id = lessonId,
            unitId = unitId,
            title = lessonTitle,
            difficulty = difficulty,
            isPremium = isPremium,
            xpReward = xpReward,
            lessonGoal = lessonGoal,
            vocabulary = vocabList,
            grammarPoint = grammarPoint,
            pronunciation = pronunciation,
            examples = examplesList,
            exercises = exercisesList,
            reviewWords = reviewWords,
            unlockRequirement = unlockRequirement
        )
    }

    private fun jsonArrayToStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    private fun JSONObject.optNullableString(key: String): String? {
        return if (has(key) && !isNull(key)) getString(key) else null
    }
}
