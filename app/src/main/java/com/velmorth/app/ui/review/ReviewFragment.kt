package com.velmorth.app.ui.review

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.velmorth.app.data.repository.FirestoreSRSRepository
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.ui.lessons.LessonPlayerActivity
import com.velmorth.app.utils.AnalyticsManager
import com.velmorth.app.utils.NetworkUtils
import com.velmorth.app.utils.SRSManager
import com.velmorth.app.utils.SRSManager.SRSCard

/**
 * Spaced Repetition (SRS) Review Garden.
 * Shows vocab cards due today using SM-2, rates them, and syncs results to Firestore.
 */
class ReviewFragment : Fragment() {

    private lateinit var lessonRepository: LessonRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        lessonRepository = LessonRepository(requireContext())
        return ComposeView(requireContext()).apply { setContent { ReviewGardenContent() } }
    }

    override fun onResume() {
        super.onResume()
        (view as? ComposeView)?.setContent { ReviewGardenContent() }
    }

    @Composable
    private fun ReviewGardenContent() {
        // SRS cards from Firestore (due today)
        var srsCards    by remember { mutableStateOf<List<SRSCard>>(emptyList()) }
        var isLoading   by remember { mutableStateOf(true) }
        var currentIdx  by remember { mutableStateOf(0) }
        var sessionDone by remember { mutableStateOf(false) }

        // Local fallback review queue (used when offline / no Firestore cards)
        val localQueue  = remember { lessonRepository.getReviewQueue() }
        val isOnline    = NetworkUtils.isOnline(requireContext())

        LaunchedEffect(Unit) {
            if (isOnline) {
                FirestoreSRSRepository.getDueCards { cards ->
                    srsCards  = cards
                    isLoading = false
                }
            } else {
                isLoading = false
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F5EE))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text       = "Review Garden",
                        fontSize   = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFF1B4332)
                    )
                    Text(
                        text     = "Water your skills to keep them evergreen",
                        fontSize = 13.sp,
                        color    = Color(0xFF6B7280)
                    )
                }
                // Due-word count badge — show after loading
                if (!isLoading) {
                    val dueCount = if (isOnline) srsCards.size else localQueue.size
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (dueCount > 0) Color(0xFF2D6A4F) else Color(0xFFE5E7EB)
                    ) {
                        Text(
                            text = if (dueCount > 0) "$dueCount due" else "All done",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dueCount > 0) Color.White else Color(0xFF6B7280),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        color    = Color(0xFF2D6A4F),
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
                sessionDone -> SessionCompleteCard()

                // Firestore SRS cards available → SM-2 review mode
                isOnline && srsCards.isNotEmpty() && currentIdx < srsCards.size -> {
                    SRSProgressBar(current = currentIdx, total = srsCards.size)
                    Spacer(Modifier.height(20.dp))
                    SRSReviewCard(
                        card    = srsCards[currentIdx],
                        onRate  = { rating ->
                            val updatedCard = SRSManager.calculateNextReview(srsCards[currentIdx], rating)
                            FirestoreSRSRepository.updateCard(updatedCard)
                            AnalyticsManager.logSRSReview(updatedCard.vocabId, rating, updatedCard.status)
                            if (currentIdx + 1 >= srsCards.size) {
                                sessionDone = true
                            } else {
                                currentIdx++
                            }
                        }
                    )
                }

                // Offline fallback — local lesson review queue
                !isOnline && localQueue.isNotEmpty() -> {
                    OfflineBanner()
                    Spacer(Modifier.height(16.dp))
                    ActiveReviewList(localQueue)
                }

                // No cards due — garden is watered
                else -> ZeroStateGarden()
            }
        }
    }

    // ── Sub-composables ────────────────────────────────────────────────────────

    @Composable
    private fun SRSProgressBar(current: Int, total: Int) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Card ${current + 1} of $total", fontSize = 13.sp, color = Color(0xFF6B7280))
                Text("${((current.toFloat() / total) * 100).toInt()}%", fontSize = 13.sp,
                    color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress          = { current.toFloat() / total },
                modifier          = Modifier.fillMaxWidth().height(6.dp),
                color             = Color(0xFF2D6A4F),
                trackColor        = Color(0xFFD1E8DA),
                strokeCap         = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    @Composable
    private fun SRSReviewCard(card: SRSCard, onRate: (Int) -> Unit) {
        var revealed by remember(card.vocabId) { mutableStateOf(false) }

        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier                = Modifier.padding(28.dp).fillMaxWidth(),
                horizontalAlignment     = Alignment.CenterHorizontally,
                verticalArrangement     = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text       = card.vocabId.replace("_", " "),
                    fontSize   = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1B4332),
                    textAlign  = TextAlign.Center
                )
                Text(
                    text     = "Interval: ${card.intervalDays}d · ${card.status}",
                    fontSize = 11.sp,
                    color    = Color(0xFFA0A0A0)
                )
                HorizontalDivider(color = Color(0xFFF0EDE8))

                if (!revealed) {
                    Button(
                        onClick  = { revealed = true },
                        colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        shape    = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reveal Meaning 👁️", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "How well did you remember?",
                                fontSize = 14.sp,
                                color    = Color(0xFF6B7280),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Rating buttons: Again(1), Hard(3), Good(4), Easy(5)
                                listOf(1 to "Again\n🔄", 3 to "Hard\n😅", 4 to "Good\n✅", 5 to "Easy\n⚡").forEach { (rating, label) ->
                                    val bgColor = when (rating) {
                                        5    -> Color(0xFF2D6A4F)
                                        4    -> Color(0xFF52B788)
                                        3    -> Color(0xFFF59E0B)
                                        else -> Color(0xFFE53935)
                                    }
                                    Button(
                                        onClick  = { onRate(rating) },
                                        colors   = ButtonDefaults.buttonColors(containerColor = bgColor),
                                        shape    = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text(label, color = Color.White, fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SessionCompleteCard() {
        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier                = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment     = Alignment.CenterHorizontally
            ) {
                Text("🌸", fontSize = 72.sp)
                Spacer(Modifier.height(16.dp))
                Text("Review Complete!", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332), textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "All cards reviewed. Come back tomorrow to keep your garden growing!",
                    fontSize = 14.sp, color = Color(0xFF6B7280), textAlign = TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun OfflineBanner() {
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📡", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Text("Offline mode — showing local review queue",
                    fontSize = 13.sp, color = Color(0xFF92400E))
            }
        }
    }

    @Composable
    private fun ZeroStateGarden() {
        val allLessons     = lessonRepository.getUnits().flatMap { it.lessons }
        val completedCount = lessonRepository.getCompletedLessons().size
        val totalVocab     = allLessons.sumOf { it.vocabulary.size }.let { if (it == 0) allLessons.size else it }

        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier  = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Column(
                modifier            = Modifier.padding(32.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🌸", fontSize = 72.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Your garden is watered!",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color(0xFF1B4332),
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "No words due for review right now.",
                    fontSize  = 14.sp,
                    color     = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = if (completedCount > 0)
                            "$completedCount lessons reviewed · $totalVocab words tracked"
                        else
                            "Complete lessons to add words to your review garden",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D6A4F),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun ActiveReviewList(lessons: List<com.velmorth.app.data.model.Lesson>) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F0E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌱", fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "You have ${lessons.size} lessons that require watering!",
                        fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1B4332)
                    )
                }
            }
            lessons.forEach { lesson ->
                Card(
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(lesson.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                            Spacer(Modifier.height(4.dp))
                            Text("Unit: ${lesson.unitId.replace("_", " ").uppercase()}", fontSize = 12.sp, color = Color(0xFF6B7280))
                        }
                        Button(
                            onClick = {
                                startActivity(
                                    Intent(requireContext(), LessonPlayerActivity::class.java).apply {
                                        putExtra("LESSON_ID", lesson.id)
                                        putExtra("IS_REVIEW_MODE", true)
                                    }
                                )
                            },
                            colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                            shape   = RoundedCornerShape(16.dp)
                        ) {
                            Text("Water 💧", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
