package com.velmorth.app.ui.lessons

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.theme.LearnWithVelmorthTheme
import com.velmorth.app.theme.velmorthColors
import androidx.fragment.app.Fragment
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.Lesson
import com.velmorth.app.data.model.LessonCategory
import com.velmorth.app.data.repository.CategoryRepository
import com.velmorth.app.data.repository.LessonRepository

// Brand palette is now resolved dynamically from MaterialTheme.colorScheme and MaterialTheme.velmorthColors.

/**
 * Lessons screen — two-level hierarchy:
 *   Category grid  →  (tap to expand)  →  Lesson list
 *
 * Data sources:
 *   [CategoryRepository] for category definitions
 *   [LessonRepository]   for lesson content & completion state
 */
class LessonsFragment : Fragment() {

    private lateinit var lessonRepository   : LessonRepository
    private lateinit var categoryRepository : CategoryRepository
    private lateinit var prefsManager       : PrefsManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lessonRepository   = LessonRepository(requireContext())
        categoryRepository = CategoryRepository(requireContext())
        prefsManager       = PrefsManager(requireContext())
        return ComposeView(requireContext()).apply {
            setContent {
                LearnWithVelmorthTheme {
                    LessonsScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (view as? ComposeView)?.setContent {
            LearnWithVelmorthTheme {
                LessonsScreen()
            }
        }
    }

    // ── Root screen ───────────────────────────────────────────────────────────

    @Composable
    private fun LessonsScreen() {
        val categories   = categoryRepository.getCategories()
        val progress     = lessonRepository.getProgress()
        val completedSet = progress.completedLessons.toSet()
        val isHindi      = prefsManager.nativeLanguage.equals("Hindi", ignoreCase = true)
        val isPremium    = prefsManager.isPremium

        // Determine dark theme dynamically
        val isDark = MaterialTheme.colorScheme.primary == Color(0xFF74C69D)

        val headerGradient = if (isDark) {
            Brush.verticalGradient(listOf(Color(0xFF0D2418), Color(0xFF1B4332)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)))
        }
        val headerSubtitleColor = if (isDark) Color(0xFF74C69D) else Color(0xFFB7E4C7)

        // Track which category is expanded (null = all collapsed)
        var expandedCategoryId by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGradient)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Text(
                        "Course Path",
                        fontSize   = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "🇯🇵 ${prefsManager.selectedLanguage.replaceFirstChar { it.uppercase() }} · " +
                        "${completedSet.size} lessons completed",
                        fontSize = 13.sp,
                        color    = headerSubtitleColor.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (categories.isEmpty()) {
                // Fallback to unit view if categories not loaded
                FallbackUnitView(progress.currentLesson, completedSet)
            } else {
                // ── Category cards ────────────────────────────────────────────
                categories.forEach { category ->
                    val categoryLessons = categoryRepository.getLessonsForCategory(category)
                    val isExpanded      = expandedCategoryId == category.categoryId
                    val completedInCat  = categoryLessons.count { it.id in completedSet }

                    CategorySection(
                        category        = category,
                        lessons         = categoryLessons,
                        isExpanded      = isExpanded,
                        completedCount  = completedInCat,
                        completedSet    = completedSet,
                        currentLessonId = progress.currentLesson,
                        isPremium       = isPremium,
                        isHindi         = isHindi,
                        onToggle        = {
                            expandedCategoryId =
                                if (isExpanded) null else category.categoryId
                        },
                        onLessonClick   = { lesson ->
                            when {
                                lesson.isPremium && !isPremium ->
                                    Toast.makeText(requireContext(),
                                        "🔒 Premium lesson — upgrade to unlock!", Toast.LENGTH_SHORT).show()
                                else -> startActivity(
                                    Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                        putExtra("LESSON_ID", lesson.id)
                                    }
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Category section (header + expandable lesson list) ────────────────────

    @Composable
    private fun CategorySection(
        category        : LessonCategory,
        lessons         : List<Lesson>,
        isExpanded      : Boolean,
        completedCount  : Int,
        completedSet    : Set<String>,
        currentLessonId : String,
        isPremium       : Boolean,
        isHindi         : Boolean,
        onToggle        : () -> Unit,
        onLessonClick   : (Lesson) -> Unit
    ) {
        val totalForBar    = category.totalLessons.coerceAtLeast(1)
        val progressFrac   = (completedCount.toFloat() / totalForBar).coerceIn(0f, 1f)
        val allCompleted   = completedCount >= category.totalLessons && category.totalLessons > 0
        val description    = if (isHindi) category.descriptionHi else category.descriptionEn

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // ── Category header card ──────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = if (allCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier  = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon circle
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    if (allCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (allCompleted) "✅" else category.icon,
                                fontSize = 26.sp
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    category.categoryTitle,
                                    fontSize   = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = if (allCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                    maxLines   = 1,
                                    overflow   = TextOverflow.Ellipsis
                                )
                                // Premium badge
                                if (category.premiumLessons > 0 && category.freeLessons == 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.velmorthColors.leafGold.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            "👑 Premium",
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color      = MaterialTheme.velmorthColors.leafGoldDark,
                                            modifier   = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                description,
                                fontSize = 12.sp,
                                color = if (allCompleted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(8.dp))

                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFrac)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                                            )
                                        )
                                )
                            }
                            Spacer(Modifier.height(4.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "$completedCount / ${category.totalLessons} lessons",
                                    fontSize = 11.sp,
                                    color = if (allCompleted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (category.freeLessons > 0) {
                                        StatChip(
                                            "${category.freeLessons} Free",
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (category.premiumLessons > 0) {
                                        StatChip(
                                            "${category.premiumLessons} 👑",
                                            MaterialTheme.velmorthColors.leafGold.copy(alpha = 0.2f),
                                            MaterialTheme.velmorthColors.leafGoldDark
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Chevron
                        Icon(
                            imageVector        = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint               = if (allCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // ── Expandable lesson list ────────────────────────────────────────
            AnimatedVisibility(
                visible       = isExpanded,
                enter         = expandVertically(animationSpec = tween(250)),
                exit          = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (lessons.isEmpty()) {
                        Card(
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier  = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Lessons coming soon for this category!",
                                modifier  = Modifier.padding(16.dp),
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize  = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        var foundCurrent = false
                        lessons.forEachIndexed { idx, lesson ->
                            val isCompleted = lesson.id in completedSet
                            val isCurrent   = lesson.id == currentLessonId ||
                                (!isCompleted && !foundCurrent)
                            if (isCurrent) foundCurrent = true
                            val isLocked    = lesson.isPremium && !isPremium

                            LessonRow(
                                lesson      = lesson,
                                index       = idx + 1,
                                isCompleted = isCompleted,
                                isCurrent   = isCurrent && !isCompleted,
                                isLocked    = isLocked,
                                onClick     = { onLessonClick(lesson) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Lesson row ────────────────────────────────────────────────────────────

    @Composable
    private fun LessonRow(
        lesson      : Lesson,
        index       : Int,
        isCompleted : Boolean,
        isCurrent   : Boolean,
        isLocked    : Boolean,
        onClick     : () -> Unit
    ) {
        val bg = when {
            isCompleted -> MaterialTheme.colorScheme.primaryContainer
            isCurrent   -> MaterialTheme.colorScheme.primary
            isLocked    -> MaterialTheme.colorScheme.surfaceVariant
            else        -> MaterialTheme.colorScheme.surface
        }
        val titleColor = when {
            isCurrent -> MaterialTheme.colorScheme.onPrimary
            isLocked  -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            else      -> MaterialTheme.colorScheme.onSurface
        }
        val subColor   = if (isCurrent) MaterialTheme.colorScheme.onPrimary.copy(0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
        val borderColor = when {
            isCurrent   -> MaterialTheme.colorScheme.outline
            isCompleted -> MaterialTheme.colorScheme.primary
            else        -> Color.Transparent
        }

        Card(
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(if (isCurrent) 3.dp else 1.dp),
            modifier  = Modifier
                .fillMaxWidth()
                .border(
                    width = if (isCurrent || isCompleted) 1.5.dp else 0.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .clickable { onClick() }
        ) {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Index/status circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent   -> MaterialTheme.colorScheme.primaryContainer
                                isLocked    -> MaterialTheme.colorScheme.outline
                                else        -> MaterialTheme.colorScheme.primaryContainer
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text     = when {
                            isCompleted -> "✓"
                            isLocked    -> "🔒"
                            isCurrent   -> "▶"
                            else        -> "$index"
                        },
                        fontSize   = if (isCompleted || isLocked) 16.sp else 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isCompleted) MaterialTheme.colorScheme.onPrimary else if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        lesson.title,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = titleColor,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("+${lesson.xpReward} XP", fontSize = 11.sp, color = subColor)
                        if (lesson.isPremium) Text("👑 Premium", fontSize = 11.sp, color = MaterialTheme.velmorthColors.leafGold)
                    }
                }

                // Difficulty chip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimary.copy(0.2f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        lesson.difficulty.replace("-", " "),
                        fontSize = 10.sp,
                        color    = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    // ── Fallback unit view ────────────────────────────────────────────────────
    // Used when categories_index.json is missing or empty

    @Composable
    private fun FallbackUnitView(
        currentLessonId: String,
        completedSet   : Set<String>
    ) {
        val units = lessonRepository.getUnits()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            units.forEach { unit ->
                var foundCurrent = false
                Column(Modifier.fillMaxWidth()) {
                    // Unit header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(14.dp)
                    ) {
                        Text(unit.title, fontSize = 16.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    unit.lessons.forEachIndexed { idx, lesson ->
                        val isCompleted = lesson.id in completedSet
                        val isCurrent   = lesson.id == currentLessonId ||
                            (!isCompleted && !foundCurrent)
                        if (isCurrent) foundCurrent = true
                        LessonRow(
                            lesson      = lesson,
                            index       = idx + 1,
                            isCompleted = isCompleted,
                            isCurrent   = isCurrent && !isCompleted,
                            isLocked    = false,
                            onClick     = {
                                startActivity(
                                    Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                        putExtra("LESSON_ID", lesson.id)
                                    }
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun StatChip(label: String, bg: Color, textColor: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = bg) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color    = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
