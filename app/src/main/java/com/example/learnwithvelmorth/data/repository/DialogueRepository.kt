package com.example.learnwithvelmorth.data.repository

import android.content.Context
import com.example.learnwithvelmorth.data.local.dao.DialogueDao
import com.example.learnwithvelmorth.data.local.entities.DialogueEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DialogueRepository @Inject constructor(
    private val dialogueDao: DialogueDao,
    @ApplicationContext private val context: Context,
) {
    /**
     * Loads dialogues_seed.json from assets into Room on first run.
     * Safe to call multiple times — uses IGNORE conflict strategy.
     */
    suspend fun seedIfEmpty() {
        if (dialogueDao.count() > 0) return
        try {
            val json = context.assets.open("db/dialogues_seed.json")
                .bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(json).jsonObject
            val array = root["dialogues"] as? JsonArray ?: return

            val entities = array.map { elem ->
                val obj = elem.jsonObject
                DialogueEntity(
                    id             = obj["id"]!!.jsonPrimitive.content,
                    trigger        = obj["trigger"]!!.jsonPrimitive.content,
                    text           = obj["text"]!!.jsonPrimitive.content,
                    emotion        = obj["emotion"]!!.jsonPrimitive.content,
                    category       = obj["category"]!!.jsonPrimitive.content,
                    language       = obj["language"]?.jsonPrimitive?.content ?: "en",
                    targetLangLine = obj["targetLangLine"]?.jsonPrimitive?.content ?: "",
                    voiceSpeed     = obj["voiceSpeed"]?.jsonPrimitive?.float ?: 1.0f,
                    weight         = obj["weight"]?.jsonPrimitive?.int ?: 1,
                )
            }
            dialogueDao.insertAll(entities)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Pick a random dialogue for a trigger, weighted by the 'weight' field. */
    suspend fun pickDialogue(trigger: String, language: String = "en"): DialogueEntity? {
        val lines = dialogueDao.getByTrigger(trigger, language)
        if (lines.isEmpty()) return null

        // Weighted random selection
        val totalWeight = lines.sumOf { it.weight }
        var random = (1..totalWeight).random()
        for (line in lines) {
            random -= line.weight
            if (random <= 0) return line
        }
        return lines.last()
    }

    /** All dialogues for a category (for browsing / debugging). */
    suspend fun getByCategory(category: String) =
        dialogueDao.getByCategory(category)
}
