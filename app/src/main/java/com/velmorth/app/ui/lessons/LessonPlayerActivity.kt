package com.velmorth.app.ui.lessons

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.*
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.utils.LeafRewardManager
import com.velmorth.app.utils.NetworkUtils
import com.velmorth.app.utils.TTSManager
import com.velmorth.app.utils.XPManager
import java.util.Locale

/**
 * Premium Interactive Lesson Player Game with Study Flow and localized English/Hindi instruction.
 */
class LessonPlayerActivity : ComponentActivity() {

    private lateinit var lessonRepository: LessonRepository
    private lateinit var userRepository: UserRepository
    private lateinit var prefsManager: PrefsManager
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lessonRepository = LessonRepository(this)
        userRepository = UserRepository(this)
        prefsManager = PrefsManager(this)
        ttsManager = TTSManager(this)

        val lessonId = intent.getStringExtra("LESSON_ID") ?: "ja_u01_l01_hello_basic"
        val isReviewMode = intent.getBooleanExtra("IS_REVIEW_MODE", false)
        val lesson = lessonRepository.getLessonById(lessonId)

        if (lesson == null) {
            Toast.makeText(this, "Lesson not found!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            LessonPlayerScreen(
                lesson = lesson,
                isReview = isReviewMode,
                    onComplete = { xpEarned, leavesEarned ->
                    val user = userRepository.getUser()
                    val totalXp = user.xp + xpEarned

                    // Streak increments if daily goal reached
                    val dailyGoal = prefsManager.dailyGoal
                    val activeStreak = if (user.xp < dailyGoal && totalXp >= dailyGoal) {
                        user.streak + 1
                    } else {
                        user.streak
                    }

                    // Leveling checks
                    val updatedLevel = XPManager.getLevelForXp(totalXp)

                    // Leaves: leavesEarned already includes perfect-quiz bonus (set by QuizPlayScreen)
                    val updatedLeaves = user.leaves + leavesEarned
                    userRepository.updateStats(totalXp, activeStreak, updatedLevel, updatedLeaves)

                    // 7-day streak milestone bonus
                    val streakBonus = LeafRewardManager.checkStreakBonus(prefsManager, activeStreak)
                    if (streakBonus > 0) {
                        Toast.makeText(this, "🔥 7-Day Streak! +$streakBonus bonus leaves! 🍃", Toast.LENGTH_LONG).show()
                    }

                    if (isReviewMode) {
                        lessonRepository.removeFromReviewQueue(lesson.id)
                        Toast.makeText(this, "Watering complete! Lesson refreshed. 🌱", Toast.LENGTH_SHORT).show()
                    } else {
                        val wasAlreadyCompleted = lessonRepository.getCompletedLessons().contains(lesson.id)
                        
                        lessonRepository.markLessonComplete(lesson.id)
                        lessonRepository.addToReviewQueue(lesson.id)
                        val bonusMsg = if (leavesEarned > LeafRewardManager.LESSON_COMPLETE_REWARD) " ✨ Perfect Quiz! +${LeafRewardManager.PERFECT_QUIZ_BONUS} bonus!" else ""
                        Toast.makeText(this, "Lesson completed! +$xpEarned XP, +$leavesEarned Leaves! 🍃$bonusMsg", Toast.LENGTH_LONG).show()
                        // ── Firestore sync (online only) ──────────────────────────
                        if (NetworkUtils.isOnline(this)) {
                            FirestoreProgressRepository.syncLessonComplete(
                                lessonId = lesson.id,
                                xpEarned = xpEarned
                            )
                            FirestoreProgressRepository.syncUserStats(
                                xp = totalXp,
                                streak = activeStreak,
                                leafBalance = updatedLeaves
                            )
                            // Seed SRS cards for vocab in this lesson (only if first time)
                            if (!wasAlreadyCompleted) {
                                val vocabIds = lesson.vocabulary.map { it.vocabId }
                                if (vocabIds.isNotEmpty()) {
                                    FirestoreProgressRepository.seedSRSCards(
                                        lessonId = lesson.id,
                                        vocabIds = vocabIds
                                    )
                                }
                            }
                        }
                    }

                    finish()
                },
                onExit = {
                    finish()
                }
            )
        }
    }

    @Composable
    fun LessonPlayerScreen(
        lesson: Lesson,
        isReview: Boolean,
        onComplete: (Int, Int) -> Unit,
        onExit: () -> Unit
    ) {
        var isStudying by rememberSaveable { mutableStateOf(true) }
        val isHindi = prefsManager.nativeLanguage.equals("Hindi", ignoreCase = true)

        if (isStudying) {
            StudyIntroScreen(
                lesson = lesson,
                isHindi = isHindi,
                onStartQuiz = { isStudying = false },
                onExit = onExit
            )
        } else {
            QuizPlayScreen(
                lesson = lesson,
                isHindi = isHindi,
                onComplete = onComplete,
                onExit = onExit
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun StudyIntroScreen(
        lesson: Lesson,
        isHindi: Boolean,
        onStartQuiz: () -> Unit,
        onExit: () -> Unit
    ) {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = if (isHindi) {
            listOf("अवलोकन", "शब्दावली", "व्याकरण", "उच्चारण", "उदाहरण")
        } else {
            listOf("Overview", "Vocabulary", "Grammar", "Pronunciation", "Examples")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F5EE))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "✕ बाहर निकलें" else "✕ Exit",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.clickable { onExit() }
                )
                Text(
                    text = "Velmorth 🌿",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lesson Title
            Text(
                text = lesson.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Row
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF1B4332),
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    0 -> OverviewTab(lesson, isHindi)
                    1 -> VocabularyTab(lesson.vocabulary, isHindi)
                    2 -> GrammarTab(lesson.grammarPoint, isHindi)
                    3 -> PronunciationTab(lesson.pronunciation, isHindi)
                    4 -> ExamplesTab(lesson.examples, isHindi)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sticky Bottom Button
            Button(
                onClick = onStartQuiz,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = if (isHindi) "क्विज अभ्यास शुरू करें ➔" else "Start Practice Quiz ➔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }

    @Composable
    fun OverviewTab(lesson: Lesson, isHindi: Boolean) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "अध्याय विवरण" else "Lesson Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = (if (isHindi) "कठिनाई: " else "Difficulty: ") + lesson.difficulty,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4A5C4E),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "🎁 +${lesson.xpReward} XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD4A017)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1E8D0))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isHindi) "सीखने का लक्ष्य" else "Learning Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = lesson.lessonGoal,
                    fontSize = 15.sp,
                    color = Color(0xFF4A5C4E)
                )

                if (lesson.isPremium) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEFC050).copy(alpha = 0.2f))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHindi) "💎 प्रीमियम पाठ - असीमित अभ्यास उपलब्ध है!" else "💎 Premium Lesson - Unlimited practice available!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B6914)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun VocabularyTab(vocabList: List<VocabularyItem>, isHindi: Boolean) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            vocabList.forEach { vocab ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = vocab.kanji,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C2B1E)
                                )
                                if (vocab.kanji != vocab.kana) {
                                    Text(
                                        text = vocab.kana,
                                        fontSize = 14.sp,
                                        color = Color(0xFF4A5C4E)
                                    )
                                }
                                Text(
                                    text = "[${vocab.romaji}]",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF8FA896)
                                )
                            }
                            
                            // Audio Button
                            IconButton(
                                onClick = { ttsManager.speak(vocab.kana, Locale.JAPANESE) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFD8F3DC))
                            ) {
                                Text(text = "🔊", fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFFAFAF5))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Meanings
                        val meaningText = if (isHindi && !vocab.meaningHi.isNullOrEmpty()) {
                            vocab.meaningHi
                        } else {
                            vocab.meaningEn
                        }
                        Text(
                            text = (if (isHindi) "अर्थ: " else "Meaning: ") + meaningText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B4332)
                        )

                        if (!vocab.notes.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = (if (isHindi) "टिप्पणी: " else "Note: ") + vocab.notes,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun GrammarTab(grammar: GrammarPoint, isHindi: Boolean) {
        if (grammar.grammarId.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(text = if (isHindi) "इस पाठ में कोई व्याकरण बिंदु नहीं है।" else "No grammar focus in this lesson.", modifier = Modifier.padding(16.dp))
            }
            return
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = grammar.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFD8F3DC))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = (if (isHindi) "संरचना: " else "Structure: ") + grammar.structure,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B4332)
                        )
                        Text(
                            text = "[${grammar.romajiStructure}]",
                            fontSize = 13.sp,
                            color = Color(0xFF4A5C4E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val explanationText = if (isHindi && !grammar.shortExplanationHi.isNullOrEmpty()) {
                    grammar.shortExplanationHi
                } else {
                    grammar.shortExplanationEn
                }
                Text(
                    text = explanationText,
                    fontSize = 14.sp,
                    color = Color(0xFF4A5C4E)
                )

                if (grammar.focusExamples.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isHindi) "उदाहरण वाक्य" else "Focus Examples",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B4332)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    grammar.focusExamples.forEachIndexed { index, ex ->
                        val romaji = grammar.focusExamplesRomaji.getOrNull(index) ?: ""
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { ttsManager.speak(ex, Locale.JAPANESE) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🔊 ", fontSize = 14.sp)
                            Column {
                                Text(text = ex, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C2B1E))
                                if (romaji.isNotEmpty()) {
                                    Text(text = romaji, fontSize = 12.sp, color = Color(0xFF8FA896))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun PronunciationTab(pron: PronunciationHelp, isHindi: Boolean) {
        val tips = if (isHindi && !pron.tipsHi.isNullOrEmpty()) {
            pron.tipsHi
        } else {
            pron.tipsEn
        }

        if (tips.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isHindi) "इस पाठ में कोई उच्चारण युक्तियाँ नहीं हैं।" else "No pronunciation tips in this lesson.",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF4A5C4E)
                )
            }
            return
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isHindi) "उच्चारण युक्तियाँ" else "Pronunciation Tips",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )
                Spacer(modifier = Modifier.height(12.dp))

                tips.forEach { tip ->
                    Row(modifier = Modifier.padding(vertical = 6.dp)) {
                        Text(text = "🌱  ", color = Color(0xFF52B788))
                        Text(text = tip, fontSize = 14.sp, color = Color(0xFF4A5C4E))
                    }
                }
            }
        }
    }

    @Composable
    fun ExamplesTab(examples: List<ExampleSentence>, isHindi: Boolean) {
        if (examples.isEmpty()) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isHindi) "इस पाठ में कोई अतिरिक्त उदाहरण नहीं हैं।" else "No examples in this lesson.", modifier = Modifier.padding(16.dp))
            }
            return
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            examples.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.japanese,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1C2B1E)
                                )
                                Text(
                                    text = "[${item.romaji}]",
                                    fontSize = 13.sp,
                                    color = Color(0xFF8FA896)
                                )
                            }

                            IconButton(
                                onClick = { ttsManager.speak(item.japanese, Locale.JAPANESE) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFFD8F3DC))
                            ) {
                                Text(text = "🔊", fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFFAFAF5))
                        Spacer(modifier = Modifier.height(10.dp))

                        val translation = if (isHindi && !item.translationHi.isNullOrEmpty()) {
                            item.translationHi
                        } else {
                            item.translationEn
                        }
                        Text(
                            text = translation,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1B4332)
                        )

                        if (!item.contextNote.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.contextNote,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun QuizPlayScreen(
        lesson: Lesson,
        isHindi: Boolean,
        onComplete: (Int, Int) -> Unit,
        onExit: () -> Unit
    ) {
        val exercises = lesson.exercises
        var currentIndex by rememberSaveable { mutableIntStateOf(0) }
        val currentExercise = exercises[currentIndex]

        var selectedOption by rememberSaveable { mutableStateOf("") }
        var fillInput by rememberSaveable { mutableStateOf("") }
        var translateInput by rememberSaveable { mutableStateOf("") }

        // State for MATCH matching type exercise
        // Note: For complex objects like lists, we'll keep simple state for now 
        // as custom savers are required for rememberSaveable with lists.
        val matchedPairs = remember { mutableStateListOf<Pair<String, String>>() }
        var selectedJp by rememberSaveable { mutableStateOf<String?>(null) }
        var selectedEn by rememberSaveable { mutableStateOf<String?>(null) }
        var incorrectPair by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }

        // Shuffle pairs once per exercise
        val shuffledJp = remember(currentExercise) { currentExercise.pairsJapanese.shuffled() }
        val shuffledEn = remember(currentExercise) { currentExercise.pairsEnglish.shuffled() }

        var hasChecked by rememberSaveable { mutableStateOf(false) }
        var isCorrect by rememberSaveable { mutableStateOf(false) }
        var wrongAnswers by rememberSaveable { mutableIntStateOf(0) }  // tracks mistakes for perfect-quiz bonus

        var userLeaves by rememberSaveable { mutableIntStateOf(prefsManager.leaves) }
        val isPremium = prefsManager.isPremium

        // Speak when exercise changes
        DisposableEffect(currentIndex) {
            val jpText = when (currentExercise.type) {
                "multiple_choice" -> currentExercise.options.firstOrNull() ?: ""
                "translate" -> currentExercise.answer
                "match" -> ""
                else -> currentExercise.answer
            }
            if (jpText.isNotEmpty() && jpText.any { it.code in 0x3040..0x309F || it.code in 0x30A0..0x30FF }) {
                ttsManager.speak(jpText, Locale.JAPANESE)
            }
            onDispose {}
        }

        // Auto grade if correct in MATCH
        val isMatchCompleted = currentExercise.type == "match" && matchedPairs.size == currentExercise.pairsJapanese.size && currentExercise.pairsJapanese.isNotEmpty()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F5EE))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header stats
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "✕ बाहर निकलें" else "✕ Quit",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.clickable { onExit() }
                    )

                    // Leaves/Hearts
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE3F0E9))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🍃", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isPremium) "∞" else "$userLeaves",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2D6A4F)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Indicator
                val progressFraction = ((currentIndex).toFloat() / exercises.size.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFF2D6A4F),
                    trackColor = Color(0xFFE5E7EB)
                )
            }

            // Question Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = (if (isHindi) "अभ्यास " else "Exercise ") + "${currentIndex + 1} / ${exercises.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF52B788),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val questionText = if (isHindi && !currentExercise.questionHi.isNullOrEmpty()) {
                    currentExercise.questionHi!!
                } else {
                    currentExercise.questionEn
                }
                Text(
                    text = questionText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sound trigger button
                if (currentExercise.type != "match") {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFB7E4C7))
                            .clickable {
                                val txtToSpeak = if (currentExercise.type == "translate") currentExercise.answer else currentExercise.questionEn
                                ttsManager.speak(txtToSpeak, Locale.JAPANESE)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🔊 " + (if (isHindi) "ज़ोर से पढ़ें" else "Read Aloud"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Exercise answer selection body
                when (currentExercise.type) {
                    "multiple_choice" -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            currentExercise.options.forEachIndexed { idx, option ->
                                val isSelected = option == selectedOption
                                val romaji = currentExercise.optionsRomaji.getOrNull(idx) ?: ""
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFB7E4C7) else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2D6A4F) else Color(0xFFE5E7EB),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = !hasChecked) { selectedOption = option }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = option,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1C1C1E),
                                            textAlign = TextAlign.Center
                                        )
                                        if (romaji.isNotEmpty()) {
                                            Text(
                                                text = romaji,
                                                fontSize = 12.sp,
                                                color = Color(0xFF6B7280),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "match" -> {
                        MatchExercise(
                            exercise = currentExercise,
                            matchedPairs = matchedPairs,
                            selectedJp = selectedJp,
                            onSelectedJpChange = { selectedJp = it },
                            selectedEn = selectedEn,
                            onSelectedEnChange = { selectedEn = it },
                            incorrectPair = incorrectPair,
                            onIncorrectPairChange = { incorrectPair = it },
                            shuffledJp = shuffledJp,
                            shuffledEn = shuffledEn,
                            isEnabled = !hasChecked
                        )
                    }

                    "fill_in_blank", "fill_blank" -> {
                        OutlinedTextField(
                            value = fillInput,
                            onValueChange = { fillInput = it },
                            enabled = !hasChecked,
                            label = { Text(if (isHindi) "रिक्त स्थान भरें" else "Fill the missing word") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2D6A4F),
                                focusedLabelColor = Color(0xFF2D6A4F)
                            )
                        )
                    }

                    "translate" -> {
                        OutlinedTextField(
                            value = translateInput,
                            onValueChange = { translateInput = it },
                            enabled = !hasChecked,
                            label = { Text(if (isHindi) "अनुवाद दर्ज करें" else "Enter Translation") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2D6A4F),
                                focusedLabelColor = Color(0xFF2D6A4F)
                            )
                        )
                    }
                }
            }

            // Answer checking sheet & continue buttons
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasChecked) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCorrect) Color(0xFFE3F0E9) else Color(0xFFFCEADE)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isCorrect) {
                                    if (isHindi) "उत्कृष्ट! सही है! 🎉" else "Excellent! Correct! 🎉"
                                } else {
                                    if (isHindi) "गलत उत्तर ✕" else "Incorrect answer ✕"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCorrect) Color(0xFF2D6A4F) else Color(0xFFE76F51)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            if (currentExercise.type != "match") {
                                Text(
                                    text = (if (isHindi) "सही उत्तर: " else "Correct answer: ") + currentExercise.answer,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1C1C1E)
                                )
                            } else {
                                Text(
                                    text = if (isHindi) "सभी जोड़े सफलतापूर्वक मिल गए हैं!" else "All pairs matched successfully!",
                                    fontSize = 13.sp,
                                    color = Color(0xFF1C1C1E)
                                )
                            }

                            val explanationText = if (isHindi && !currentExercise.explanationHi.isNullOrEmpty()) {
                                currentExercise.explanationHi
                            } else {
                                currentExercise.explanationEn
                            }
                            if (!explanationText.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = explanationText,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4A5C4E)
                                )
                            }

                            val hintText = if (isHindi && !currentExercise.hintHi.isNullOrEmpty()) {
                                currentExercise.hintHi
                            } else {
                                currentExercise.hintEn
                            }
                            if (!hintText.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = (if (isHindi) "संकेत: " else "Hint: ") + hintText,
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        if (!hasChecked) {
                            // Grade answer
                            val userAnswer = when (currentExercise.type) {
                                "multiple_choice" -> selectedOption
                                "translate" -> translateInput.trim()
                                "match" -> "COMPLETED" // Handled by isMatchCompleted
                                else -> fillInput.trim()
                            }

                            isCorrect = if (currentExercise.type == "match") {
                                isMatchCompleted
                            } else {
                                userAnswer.equals(currentExercise.answer, ignoreCase = true)
                            }
                            hasChecked = true

                            if (!isCorrect && !isPremium) {
                                // Deduct a leaf on wrong answer
                                userLeaves = (userLeaves - 5).coerceAtLeast(0)
                                prefsManager.leaves = userLeaves
                                wrongAnswers++
                            }
                        } else {
                            // Advance
                            if (currentIndex + 1 < exercises.size) {
                                currentIndex++
                                selectedOption = ""
                                fillInput = ""
                                translateInput = ""
                                matchedPairs.clear()
                                selectedJp = null
                                selectedEn = null
                                incorrectPair = null
                                hasChecked = false
                                isCorrect = false
                            } else {
                                val isPerfect = wrongAnswers == 0
                                val leavesToAward = LeafRewardManager.LESSON_COMPLETE_REWARD +
                                    if (isPerfect) LeafRewardManager.PERFECT_QUIZ_BONUS else 0
                                onComplete(lesson.xpReward, leavesToAward)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    shape = RoundedCornerShape(24.dp),
                    enabled = when (currentExercise.type) {
                        "multiple_choice" -> selectedOption.isNotEmpty() || hasChecked
                        "translate" -> translateInput.isNotEmpty() || hasChecked
                        "match" -> isMatchCompleted || hasChecked
                        else -> fillInput.isNotEmpty() || hasChecked
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (!hasChecked) {
                            if (isHindi) "जांचें" else "Check"
                        } else {
                            if (isHindi) "जारी रखें" else "Continue"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }

    @Composable
    fun MatchExercise(
        exercise: Exercise,
        matchedPairs: MutableList<Pair<String, String>>,
        selectedJp: String?,
        onSelectedJpChange: (String?) -> Unit,
        selectedEn: String?,
        onSelectedEnChange: (String?) -> Unit,
        incorrectPair: Pair<String, String>?,
        onIncorrectPairChange: (Pair<String, String>?) -> Unit,
        shuffledJp: List<String>,
        shuffledEn: List<String>,
        isEnabled: Boolean
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Japanese
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shuffledJp.forEach { jpWord ->
                    val isMatched = matchedPairs.any { it.first == jpWord }
                    val isSelected = selectedJp == jpWord
                    val isIncorrect = incorrectPair?.first == jpWord

                    val bgColor = when {
                        isMatched -> Color(0xFFD8F3DC)
                        isIncorrect -> Color(0xFFFCEADE)
                        isSelected -> Color(0xFFB7E4C7)
                        else -> Color.White
                    }
                    val borderColor = when {
                        isMatched -> Color(0xFF2D6A4F)
                        isIncorrect -> Color(0xFFE76F51)
                        isSelected -> Color(0xFF1B4332)
                        else -> Color(0xFFE5E7EB)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = isEnabled && !isMatched) {
                                onIncorrectPairChange(null)
                                if (isSelected) {
                                    onSelectedJpChange(null)
                                } else {
                                    onSelectedJpChange(jpWord)
                                    if (selectedEn != null) {
                                        val jpIndex = exercise.pairsJapanese.indexOf(jpWord)
                                        val enIndex = exercise.pairsEnglish.indexOf(selectedEn)
                                        if (jpIndex == enIndex && jpIndex != -1) {
                                            matchedPairs.add(Pair(jpWord, selectedEn))
                                            onSelectedJpChange(null)
                                            onSelectedEnChange(null)
                                        } else {
                                            onIncorrectPairChange(Pair(jpWord, selectedEn))
                                            onSelectedJpChange(null)
                                            onSelectedEnChange(null)
                                        }
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = jpWord,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1C2B1E),
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Right Column: English
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                shuffledEn.forEach { enWord ->
                    val isMatched = matchedPairs.any { it.second == enWord }
                    val isSelected = selectedEn == enWord
                    val isIncorrect = incorrectPair?.second == enWord

                    val bgColor = when {
                        isMatched -> Color(0xFFD8F3DC)
                        isIncorrect -> Color(0xFFFCEADE)
                        isSelected -> Color(0xFFB7E4C7)
                        else -> Color.White
                    }
                    val borderColor = when {
                        isMatched -> Color(0xFF2D6A4F)
                        isIncorrect -> Color(0xFFE76F51)
                        isSelected -> Color(0xFF1B4332)
                        else -> Color(0xFFE5E7EB)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clickable(enabled = isEnabled && !isMatched) {
                                onIncorrectPairChange(null)
                                if (isSelected) {
                                    onSelectedEnChange(null)
                                } else {
                                    onSelectedEnChange(enWord)
                                    if (selectedJp != null) {
                                        val jpIndex = exercise.pairsJapanese.indexOf(selectedJp)
                                        val enIndex = exercise.pairsEnglish.indexOf(enWord)
                                        if (jpIndex == enIndex && jpIndex != -1) {
                                            matchedPairs.add(Pair(selectedJp, enWord))
                                            onSelectedJpChange(null)
                                            onSelectedEnChange(null)
                                        } else {
                                            onIncorrectPairChange(Pair(selectedJp, enWord))
                                            onSelectedJpChange(null)
                                            onSelectedEnChange(null)
                                        }
                                    }
                                }
                            }
                    ) {
                        Text(
                            text = enWord,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1C2B1E),
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}
