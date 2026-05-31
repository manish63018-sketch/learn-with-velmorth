package com.velmorth.app.data.repository

import android.content.Context
import com.velmorth.app.data.model.Lesson
import com.velmorth.app.data.model.LessonCategory
import org.json.JSONObject

/**
 * Loads [LessonCategory] definitions from assets/config/categories_index.json.
 *
 * Categories are a topic-level grouping layer sitting ABOVE CourseUnits.
 * They reference lessons by ID; this repository resolves those IDs against
 * the full lesson list loaded by [LessonRepository].
 *
 * Design: intentionally a separate repository so the existing lesson loading
 * pipeline is not changed.
 */
class CategoryRepository(private val context: Context) {

    private val lessonRepository = LessonRepository(context)

    companion object {
        private const val CATEGORIES_ASSET = "config/categories_index.json"
    }

    // ── Load categories ───────────────────────────────────────────────────────

    /**
     * Returns all [LessonCategory] objects for the active language, with
     * [LessonCategory.lessonIds] populated from the JSON spec.
     */
    fun getCategories(): List<LessonCategory> {
        return try {
            val json = context.assets.open(CATEGORIES_ASSET)
                .bufferedReader().use { it.readText() }
            parseCategories(JSONObject(json))
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Returns the lessons that belong to [category], resolved from the
     * full lesson catalogue. Lessons not found are silently skipped.
     */
    fun getLessonsForCategory(category: LessonCategory): List<Lesson> {
        val allLessons = lessonRepository.getUnits().flatMap { it.lessons }
        val idSet = category.lessonIds.toSet()
        // Preserve the order declared in the category
        return category.lessonIds.mapNotNull { id -> allLessons.find { it.id == id } }
    }

    /**
     * Returns a flat, ordered list of ALL lessons across ALL categories,
     * de-duplicated by lesson ID, in category-declaration order.
     */
    fun getAllCategoryLessons(): List<Lesson> {
        val seen = mutableSetOf<String>()
        return getCategories().flatMap { cat ->
            getLessonsForCategory(cat).filter { lesson ->
                seen.add(lesson.id)
            }
        }
    }

    // ── Parsing ───────────────────────────────────────────────────────────────

    private fun parseCategories(root: JSONObject): List<LessonCategory> {
        val array = root.optJSONArray("categories") ?: return emptyList()
        val list  = mutableListOf<LessonCategory>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val lessonArray = obj.optJSONArray("lessons")
            val lessonIds   = buildList {
                if (lessonArray != null) {
                    for (j in 0 until lessonArray.length()) add(lessonArray.getString(j))
                }
            }
            list.add(
                LessonCategory(
                    categoryId    = obj.getString("category_id"),
                    categoryTitle = obj.getString("category_title"),
                    icon          = obj.optString("category_icon", "📚"),
                    descriptionEn = obj.optString("description_en", ""),
                    descriptionHi = obj.optString("description_hi", ""),
                    totalLessons  = obj.optInt("total_lessons", lessonIds.size),
                    freeLessons   = obj.optInt("free_lessons", 0),
                    premiumLessons = obj.optInt("premium_lessons", 0),
                    lessonIds     = lessonIds
                )
            )
        }
        return list
    }
}
