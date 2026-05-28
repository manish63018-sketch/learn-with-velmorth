package com.example.learnwithvelmorth.data.local.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.learnwithvelmorth.data.local.dao.*
import com.example.learnwithvelmorth.data.local.entities.*
import com.example.learnwithvelmorth.data.repository.DialogueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Database(
    entities = [
        UserEntity::class,
        LessonEntity::class,
        QuestionEntity::class,
        UserProgressEntity::class,
        LeafTransactionEntity::class,
        DialogueEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(StringListConverter::class)
abstract class VelmorthDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun lessonDao(): LessonDao
    abstract fun progressDao(): ProgressDao
    abstract fun leafWalletDao(): LeafWalletDao
    abstract fun dialogueDao(): DialogueDao

    companion object {
        @Volatile
        private var INSTANCE: VelmorthDatabase? = null

        fun getInstance(context: Context): VelmorthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VelmorthDatabase::class.java,
                    "velmorth.db"
                )
                    .addCallback(SeedCallback(context))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Seed default user and lesson data on first creation
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                seedDefaultUser(database)
                seedLessons(context, database)
                // DialogueRepository handles its own seeding via seedIfEmpty()
                // It is triggered lazily from VelmorthCharacterViewModel on first use
            }
        }

        private suspend fun seedDefaultUser(db: VelmorthDatabase) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            db.userDao().insertUser(
                UserEntity(
                    id = "local_user",
                    name = "Learner",
                    avatarEmoji = "🌿",
                    leafBalance = 0,
                    joinedDate = today,
                    lastActiveDate = today,
                )
            )
            // Welcome leaf bonus
            db.leafWalletDao().insertTransaction(
                LeafTransactionEntity(
                    id = "welcome_bonus",
                    userId = "local_user",
                    amount = 50,
                    type = "ADMIN_GRANT",
                    description = "🌿 Welcome to Velmorth! Here are 50 leaves to start your journey.",
                    timestamp = System.currentTimeMillis(),
                )
            )
        }

        private suspend fun seedLessons(context: Context, db: VelmorthDatabase) {
            try {
                val json = context.assets.open("db/lessons_seed.json")
                    .bufferedReader().use { it.readText() }
                val root = Json.parseToJsonElement(json).jsonObject
                val lessonsArray = root["lessons"] as? JsonArray ?: return
                val questionsArray = root["questions"] as? JsonArray ?: return

                val lessons = lessonsArray.map { elem ->
                    val obj = elem.jsonObject
                    LessonEntity(
                        id = obj["id"]!!.jsonPrimitive.content,
                        languageId = obj["languageId"]!!.jsonPrimitive.content,
                        chapterId = obj["chapterId"]!!.jsonPrimitive.content,
                        chapterTitle = obj["chapterTitle"]!!.jsonPrimitive.content,
                        title = obj["title"]!!.jsonPrimitive.content,
                        description = obj["description"]!!.jsonPrimitive.content,
                        type = obj["type"]!!.jsonPrimitive.content,
                        status = obj["status"]!!.jsonPrimitive.content,
                        xpReward = obj["xpReward"]!!.jsonPrimitive.content.toInt(),
                        leafReward = obj["leafReward"]!!.jsonPrimitive.content.toInt(),
                        durationMinutes = obj["durationMinutes"]!!.jsonPrimitive.content.toInt(),
                        orderIndex = obj["orderIndex"]!!.jsonPrimitive.content.toInt(),
                        iconEmoji = obj["iconEmoji"]!!.jsonPrimitive.content,
                    )
                }

                val questions = questionsArray.map { elem ->
                    val obj = elem.jsonObject
                    val opts = (obj["options"] as? JsonArray)?.map { it.jsonPrimitive.content } ?: emptyList()
                    QuestionEntity(
                        id = obj["id"]!!.jsonPrimitive.content,
                        lessonId = obj["lessonId"]!!.jsonPrimitive.content,
                        type = obj["type"]!!.jsonPrimitive.content,
                        prompt = obj["prompt"]!!.jsonPrimitive.content,
                        targetWord = obj["targetWord"]?.jsonPrimitive?.content ?: "",
                        options = opts,
                        correctAnswer = obj["correctAnswer"]!!.jsonPrimitive.content,
                        explanation = obj["explanation"]?.jsonPrimitive?.content ?: "",
                        orderIndex = obj["orderIndex"]!!.jsonPrimitive.content.toInt(),
                        xpValue = obj["xpValue"]?.jsonPrimitive?.content?.toInt() ?: 5,
                    )
                }

                db.lessonDao().insertLessons(lessons)
                db.lessonDao().insertQuestions(questions)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
