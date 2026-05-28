package com.example.learnwithvelmorth.ui.screens.lessons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.learnwithvelmorth.domain.model.Lesson
import com.example.learnwithvelmorth.domain.model.LessonStatus
import com.example.learnwithvelmorth.domain.model.LessonType
import com.example.learnwithvelmorth.theme.CreamWhite
import com.example.learnwithvelmorth.theme.ForestCardShape
import com.example.learnwithvelmorth.theme.ForestDeep
import com.example.learnwithvelmorth.theme.ForestMid
import com.example.learnwithvelmorth.theme.LearnWithVelmorthTheme
import com.example.learnwithvelmorth.theme.LeafGold
import com.example.learnwithvelmorth.theme.MossGreen
import com.example.learnwithvelmorth.theme.NunitoFamily
import com.example.learnwithvelmorth.theme.PlayfairFamily
import com.example.learnwithvelmorth.theme.PureWhite
import com.example.learnwithvelmorth.theme.WarmWhite
import com.example.learnwithvelmorth.theme.velmorthColors
import com.example.learnwithvelmorth.ui.components.LeafBalanceChip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// ============================================================
// ViewModel
// ============================================================

@HiltViewModel
class LessonPathViewModel @Inject constructor() : ViewModel() {

    private val _lessons = MutableStateFlow(dummyLessons())
    val lessons: StateFlow<List<Lesson>> = _lessons.asStateFlow()

    private fun dummyLessons(): List<Lesson> = listOf(
        // Chapter 1 — Foundations
        Lesson(
            id = "lesson_001", languageId = "jp", chapterId = "ch_01",
            chapterTitle = "Chapter 1: Foundations", title = "Greetings & Introductions",
            description = "Learn basic greetings", type = LessonType.VOCABULARY,
            status = LessonStatus.COMPLETED, xpReward = 15, leafReward = 5,
            durationMinutes = 5, orderIndex = 0, iconEmoji = "👋", bestScore = 95,
        ),
        Lesson(
            id = "lesson_002", languageId = "jp", chapterId = "ch_01",
            chapterTitle = "Chapter 1: Foundations", title = "Numbers 1–20",
            description = "Count from 1 to 20", type = LessonType.VOCABULARY,
            status = LessonStatus.COMPLETED, xpReward = 10, leafReward = 5,
            durationMinutes = 5, orderIndex = 1, iconEmoji = "🔢", bestScore = 88,
        ),
        Lesson(
            id = "lesson_003", languageId = "jp", chapterId = "ch_01",
            chapterTitle = "Chapter 1: Foundations", title = "Basic Phrases",
            description = "Essential everyday phrases", type = LessonType.GRAMMAR,
            status = LessonStatus.COMPLETED, xpReward = 10, leafReward = 5,
            durationMinutes = 6, orderIndex = 2, iconEmoji = "💬", bestScore = 100,
        ),

        // Chapter 2 — Daily Life
        Lesson(
            id = "lesson_004", languageId = "jp", chapterId = "ch_02",
            chapterTitle = "Chapter 2: Daily Life", title = "Colors & Shapes",
            description = "Describe the world around you", type = LessonType.VOCABULARY,
            status = LessonStatus.AVAILABLE, xpReward = 10, leafReward = 5,
            durationMinutes = 7, orderIndex = 3, iconEmoji = "🎨",
        ),
        Lesson(
            id = "lesson_005", languageId = "jp", chapterId = "ch_02",
            chapterTitle = "Chapter 2: Daily Life", title = "Food & Drink",
            description = "Order food like a local", type = LessonType.VOCABULARY,
            status = LessonStatus.LOCKED, xpReward = 10, leafReward = 5,
            durationMinutes = 8, orderIndex = 4, iconEmoji = "🍜",
        ),
        Lesson(
            id = "lesson_006", languageId = "jp", chapterId = "ch_02",
            chapterTitle = "Chapter 2: Daily Life", title = "Telling the Time",
            description = "Hours, minutes & schedules", type = LessonType.GRAMMAR,
            status = LessonStatus.LOCKED, xpReward = 15, leafReward = 8,
            durationMinutes = 10, orderIndex = 5, iconEmoji = "⏰",
        ),

        // Chapter 3 — People & Places
        Lesson(
            id = "lesson_007", languageId = "jp", chapterId = "ch_03",
            chapterTitle = "Chapter 3: People & Places", title = "Family Members",
            description = "Talk about your family", type = LessonType.VOCABULARY,
            status = LessonStatus.LOCKED, xpReward = 10, leafReward = 5,
            durationMinutes = 7, orderIndex = 6, iconEmoji = "👨‍👩‍👧",
        ),
        Lesson(
            id = "lesson_008", languageId = "jp", chapterId = "ch_03",
            chapterTitle = "Chapter 3: People & Places", title = "Directions & Places",
            description = "Navigate the city", type = LessonType.SPEAKING,
            status = LessonStatus.LOCKED, xpReward = 20, leafReward = 10,
            durationMinutes = 10, orderIndex = 7, iconEmoji = "🗺️",
        ),
        Lesson(
            id = "lesson_009", languageId = "jp", chapterId = "ch_03",
            chapterTitle = "Chapter 3: People & Places", title = "Culture Corner",
            description = "Discover local traditions", type = LessonType.CULTURE,
            status = LessonStatus.LOCKED, xpReward = 25, leafReward = 15,
            durationMinutes = 12, orderIndex = 8, iconEmoji = "🏯",
        ),

        // Chapter 4 — Advanced Expression
        Lesson(
            id = "lesson_010", languageId = "jp", chapterId = "ch_04",
            chapterTitle = "Chapter 4: Advanced Expression", title = "Emotions & Feelings",
            description = "Express how you feel", type = LessonType.VOCABULARY,
            status = LessonStatus.LOCKED, xpReward = 15, leafReward = 8,
            durationMinutes = 9, orderIndex = 9, iconEmoji = "😊",
        ),
        Lesson(
            id = "lesson_011", languageId = "jp", chapterId = "ch_04",
            chapterTitle = "Chapter 4: Advanced Expression", title = "Story Listening",
            description = "Listen and comprehend a short story", type = LessonType.LISTENING,
            status = LessonStatus.LOCKED, xpReward = 30, leafReward = 15,
            durationMinutes = 15, orderIndex = 10, iconEmoji = "🎧",
        ),
    )
}

// ============================================================
// Screen Entry Point
// ============================================================

@Composable
fun LessonPathScreen(
    onStartLesson: (lessonId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LessonPathViewModel = hiltViewModel(),
) {
    val lessons by viewModel.lessons.collectAsStateWithLifecycle()
    LessonPathContent(
        lessons       = lessons,
        onStartLesson = onStartLesson,
        onBack        = onBack,
        modifier      = modifier,
    )
}

// ============================================================
// Screen Content (stateless, testable)
// ============================================================

@Composable
fun LessonPathContent(
    lessons: List<Lesson>,
    onStartLesson: (lessonId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leafBalance = lessons.filter { it.status == LessonStatus.COMPLETED }
        .sumOf { it.leafReward }

    // Group lessons by chapter
    val chapters = lessons.groupBy { it.chapterId to it.chapterTitle }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Top Bar ─────────────────────────────────────────
        LessonPathTopBar(
            leafBalance = leafBalance,
            onBack      = onBack,
        )

        // ── Lesson List ──────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            chapters.forEach { (chapterKey, chapterLessons) ->
                val (_, chapterTitle) = chapterKey

                // Chapter header banner
                item(key = chapterKey.first) {
                    ChapterHeaderBanner(chapterTitle = chapterTitle)
                }

                // Lesson nodes
                items(
                    items = chapterLessons.sortedBy { it.orderIndex },
                    key   = { it.id },
                ) { lesson ->
                    LessonNode(
                        lesson        = lesson,
                        onStartLesson = onStartLesson,
                    )
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // Bottom padding
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ============================================================
// Sub-components
// ============================================================

@Composable
private fun LessonPathTopBar(
    leafBalance: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(ForestDeep, ForestMid)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        // Back button
        IconButton(
            onClick  = onBack,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Go back",
                tint               = PureWhite,
            )
        }

        // Title
        Text(
            text       = "My Path",
            style      = MaterialTheme.typography.titleLarge,
            color      = PureWhite,
            fontFamily = PlayfairFamily,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.align(Alignment.Center),
        )

        // Leaf balance
        LeafBalanceChip(
            balance  = leafBalance,
            compact  = true,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
        )
    }
}

@Composable
private fun ChapterHeaderBanner(
    chapterTitle: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(ForestCardShape)
            .background(
                Brush.horizontalGradient(
                    listOf(ForestDeep, ForestMid, MossGreen)
                )
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Decorative accent dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(LeafGold),
            )
            Text(
                text       = chapterTitle,
                style      = MaterialTheme.typography.titleMedium,
                color      = PureWhite,
                fontFamily = PlayfairFamily,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LessonNode(
    lesson: Lesson,
    onStartLesson: (lessonId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAvailable = lesson.status == LessonStatus.AVAILABLE
    val isCompleted = lesson.status == LessonStatus.COMPLETED
    val isLocked    = lesson.status == LessonStatus.LOCKED

    // Visual state values
    val nodeBackground: Color = when {
        isCompleted -> LeafGold.copy(alpha = 0.14f)
        isAvailable -> MossGreen.copy(alpha = 0.12f)
        else        -> MaterialTheme.velmorthColors.lockedContent.copy(alpha = 0.08f)
    }
    val nodeIconBg: Color = when {
        isCompleted -> LeafGold
        isAvailable -> MossGreen
        else        -> MaterialTheme.velmorthColors.lockedContent
    }
    val borderColor: Color = when {
        isCompleted -> LeafGold.copy(alpha = 0.5f)
        isAvailable -> MossGreen.copy(alpha = 0.5f)
        else        -> MaterialTheme.velmorthColors.lockedContent.copy(alpha = 0.2f)
    }
    val titleColor = when {
        isLocked -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else     -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = if (isAvailable) 4.dp else 1.dp, shape = ForestCardShape)
            .clip(ForestCardShape)
            .background(nodeBackground)
            .then(
                if (isAvailable) {
                    Modifier.clickable { onStartLesson(lesson.id) }
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Icon circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(nodeIconBg),
            contentAlignment = Alignment.Center,
        ) {
            if (isCompleted) {
                Text(text = "✓", fontSize = 22.sp, color = PureWhite, fontWeight = FontWeight.ExtraBold)
            } else if (isLocked) {
                Text(text = "🔒", fontSize = 22.sp)
            } else {
                Text(text = lesson.iconEmoji, fontSize = 26.sp)
            }
        }

        // Lesson info
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text       = lesson.title,
                style      = MaterialTheme.typography.titleSmall,
                color      = titleColor,
                fontWeight = FontWeight.Bold,
                fontFamily = NunitoFamily,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                LessonTypeChip(type = lesson.type, isLocked = isLocked)
                Text(
                    text  = "${lesson.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (isLocked) 0.4f else 1f
                    ),
                )
            }
            if (isCompleted && lesson.bestScore > 0) {
                Text(
                    text  = "Best: ${lesson.bestScore}%  ⭐ ${lesson.xpReward} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.velmorthColors.leafGoldDark,
                    fontWeight = FontWeight.SemiBold,
                )
            } else if (isAvailable) {
                Text(
                    text  = "⭐ ${lesson.xpReward} XP  🍃 ${lesson.leafReward}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MossGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Status indicator / arrow
        if (isAvailable) {
            Text(
                text  = "›",
                fontSize = 28.sp,
                color = MossGreen,
                fontWeight = FontWeight.ExtraBold,
            )
        } else if (isCompleted) {
            Text(
                text  = "✓",
                fontSize = 20.sp,
                color = LeafGold,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

@Composable
private fun LessonTypeChip(
    type: LessonType,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val (label, bg) = when (type) {
        LessonType.VOCABULARY -> "Vocab"    to MossGreen
        LessonType.GRAMMAR    -> "Grammar"  to ForestMid
        LessonType.LISTENING  -> "Listen"   to Color(0xFF457B9D)
        LessonType.SPEAKING   -> "Speak"    to Color(0xFFE76F51)
        LessonType.READING    -> "Read"     to Color(0xFF9B89C4)
        LessonType.CULTURE    -> "Culture"  to LeafGold
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(bg.copy(alpha = if (isLocked) 0.2f else 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else bg,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ============================================================
// Previews
// ============================================================

private val previewLessons = listOf(
    Lesson(
        id = "l1", languageId = "jp", chapterId = "ch_01",
        chapterTitle = "Chapter 1: Foundations", title = "Greetings",
        description = "", type = LessonType.VOCABULARY,
        status = LessonStatus.COMPLETED, iconEmoji = "👋", bestScore = 95,
        xpReward = 15, leafReward = 5, durationMinutes = 5, orderIndex = 0,
    ),
    Lesson(
        id = "l2", languageId = "jp", chapterId = "ch_01",
        chapterTitle = "Chapter 1: Foundations", title = "Numbers 1–20",
        description = "", type = LessonType.VOCABULARY,
        status = LessonStatus.AVAILABLE, iconEmoji = "🔢",
        xpReward = 10, leafReward = 5, durationMinutes = 5, orderIndex = 1,
    ),
    Lesson(
        id = "l3", languageId = "jp", chapterId = "ch_01",
        chapterTitle = "Chapter 1: Foundations", title = "Basic Phrases",
        description = "", type = LessonType.GRAMMAR,
        status = LessonStatus.LOCKED, iconEmoji = "💬",
        xpReward = 10, leafReward = 5, durationMinutes = 6, orderIndex = 2,
    ),
    Lesson(
        id = "l4", languageId = "jp", chapterId = "ch_02",
        chapterTitle = "Chapter 2: Daily Life", title = "Colors & Shapes",
        description = "", type = LessonType.VOCABULARY,
        status = LessonStatus.LOCKED, iconEmoji = "🎨",
        xpReward = 10, leafReward = 5, durationMinutes = 7, orderIndex = 3,
    ),
    Lesson(
        id = "l5", languageId = "jp", chapterId = "ch_02",
        chapterTitle = "Chapter 2: Daily Life", title = "Story Listening",
        description = "", type = LessonType.LISTENING,
        status = LessonStatus.LOCKED, iconEmoji = "🎧",
        xpReward = 20, leafReward = 10, durationMinutes = 12, orderIndex = 4,
    ),
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun LessonPathScreenPreview() {
    LearnWithVelmorthTheme {
        LessonPathContent(
            lessons       = previewLessons,
            onStartLesson = {},
            onBack        = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, name = "Dark")
@Composable
fun LessonPathScreenDarkPreview() {
    LearnWithVelmorthTheme(darkTheme = true) {
        LessonPathContent(
            lessons       = previewLessons,
            onStartLesson = {},
            onBack        = {},
        )
    }
}
