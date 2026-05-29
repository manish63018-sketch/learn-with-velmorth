package com.example.learnwithvelmorth.ui.screens.premium

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnwithvelmorth.theme.*
import kotlin.math.sin
import kotlin.random.Random

// ── Star data ────────────────────────────────────────────────────────────────

private data class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

private val stars: List<Star> = List(120) {
    Star(
        x = Random.nextFloat(),
        y = Random.nextFloat(),
        radius = Random.nextFloat() * 2.5f + 0.5f,
        alpha = Random.nextFloat() * 0.7f + 0.3f,
    )
}

private fun DrawScope.drawStars(animatedAlpha: Float) {
    stars.forEach { star ->
        drawCircle(
            color = Color.White.copy(alpha = star.alpha * animatedAlpha),
            radius = star.radius,
            center = Offset(star.x * size.width, star.y * size.height),
        )
    }
}

// ── Feature row data ─────────────────────────────────────────────────────────

private data class Feature(val label: String, val free: Boolean?, val premium: Boolean)

private val features = listOf(
    Feature("Lessons per day",    null,  true),   // special text row
    Feature("Vocabulary content", null,  true),
    Feature("AI Speaker",         false, true),
    Feature("Offline mode",       false, true),
    Feature("Ad-free",            false, true),
)

private val freeTexts    = listOf("3 / day",     "Basic only", null, null, null)
private val premiumTexts = listOf("Unlimited",   "All content", null, null, null)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun PremiumPaywallScreen(
    onSubscribe: (planType: String) -> Unit,
    onBack: () -> Unit,
) {
    val ForestNight = Color(0xFF0D2218)
    val forestDeep  = Color(0xFF1B4332)
    val leafGold    = Color(0xFFD4A017)
    val creamWhite  = Color(0xFFF1E8D0)

    var selectedPlan by remember { mutableStateOf("annual") }

    // Crown pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "crown_pulse")
    val crownScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "crown_scale",
    )

    // Stars twinkle animation
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "star_alpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(ForestNight, forestDeep),
                    )
                )
        )

        // ── Stars canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawStars(starAlpha)
        }

        // ── Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 12.dp)
                .size(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = creamWhite.copy(alpha = 0.8f),
            )
        }

        // ── Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 100.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // Crown
            Text(
                text = "👑",
                fontSize = 64.sp,
                modifier = Modifier.scale(crownScale),
            )

            Spacer(Modifier.height(16.dp))

            // Heading
            Text(
                text = "Velmorth Premium",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = PlayfairFamily,
                    fontWeight  = FontWeight.Bold,
                    color       = leafGold,
                    fontSize    = 30.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle
            Text(
                text = "Unlock the full forest",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = NunitoFamily,
                    color      = creamWhite.copy(alpha = 0.75f),
                    fontSize   = 16.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            // ── Feature comparison table
            FeatureComparisonTable(
                leafGold   = leafGold,
                creamWhite = creamWhite,
            )

            Spacer(Modifier.height(28.dp))

            // ── Pricing cards
            PricingCard(
                title       = "Monthly",
                price       = "$4.99",
                period      = "/ month",
                subLabel    = null,
                isBestValue = false,
                isSelected  = selectedPlan == "monthly",
                leafGold    = leafGold,
                creamWhite  = creamWhite,
                forestDeep  = forestDeep,
                onClick     = { selectedPlan = "monthly" },
            )

            Spacer(Modifier.height(12.dp))

            PricingCard(
                title       = "Annual",
                price       = "$2.99",
                period      = "/ month",
                subLabel    = "$35.99 billed annually",
                isBestValue = true,
                isSelected  = selectedPlan == "annual",
                leafGold    = leafGold,
                creamWhite  = creamWhite,
                forestDeep  = forestDeep,
                onClick     = { selectedPlan = "annual" },
            )

            Spacer(Modifier.height(32.dp))

            // ── CTA button
            Button(
                onClick = { onSubscribe(selectedPlan) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(50)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(leafGold, Color(0xFFF5C842)),
                            ),
                            shape = RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Start 7-Day Free Trial",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color(0xFF1B2A1E),
                            fontSize   = 17.sp,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Fine print
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = "✓ Cancel anytime",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = creamWhite.copy(alpha = 0.55f),
                        fontFamily = NunitoFamily,
                    ),
                )
                Text(
                    text  = "✓ No credit card required",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = creamWhite.copy(alpha = 0.55f),
                        fontFamily = NunitoFamily,
                    ),
                )
            }
        }
    }
}

// ── Feature comparison table ──────────────────────────────────────────────────

@Composable
private fun FeatureComparisonTable(leafGold: Color, creamWhite: Color) {
    val mossGreen = Color(0xFF40916C)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(16.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text     = "Feature",
                modifier = Modifier.weight(1.6f),
                style    = MaterialTheme.typography.labelMedium.copy(
                    color      = creamWhite.copy(alpha = 0.5f),
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text      = "Free",
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelMedium.copy(
                    color      = creamWhite.copy(alpha = 0.5f),
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text      = "Premium",
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelMedium.copy(
                    color      = leafGold,
                    fontFamily = NunitoFamily,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        HorizontalDivider(
            color     = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp,
            modifier  = Modifier.padding(vertical = 10.dp),
        )

        val featureRows = listOf(
            Triple("Lessons per day",    "3 / day",    "Unlimited"),
            Triple("Vocabulary content", "Basic only", "All content"),
            Triple("AI Speaker",         null,         null),
            Triple("Offline mode",       null,         null),
            Triple("Ad-free",            null,         null),
        )

        val freeFlags:    List<Boolean?> = listOf(null, null, false, false, false)
        val premiumFlags: List<Boolean>  = listOf(true, true, true,  true,  true)

        featureRows.forEachIndexed { index, (label, freeText, premiumText) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = label,
                    modifier = Modifier.weight(1.6f),
                    style    = MaterialTheme.typography.bodyMedium.copy(
                        color      = creamWhite.copy(alpha = 0.85f),
                        fontFamily = NunitoFamily,
                    ),
                )
                // Free column
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        freeText != null -> Text(
                            text  = freeText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color      = creamWhite.copy(alpha = 0.6f),
                                fontFamily = NunitoFamily,
                            ),
                        )
                        freeFlags[index] == false -> Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Not available",
                            tint               = Color(0xFFEF5350),
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }
                // Premium column
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        premiumText != null -> Text(
                            text  = premiumText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color      = mossGreen,
                                fontFamily = NunitoFamily,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        premiumFlags[index] -> Icon(
                            imageVector        = Icons.Default.Check,
                            contentDescription = "Available",
                            tint               = mossGreen,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ── Pricing card ──────────────────────────────────────────────────────────────

@Composable
private fun PricingCard(
    title: String,
    price: String,
    period: String,
    subLabel: String?,
    isBestValue: Boolean,
    isSelected: Boolean,
    leafGold: Color,
    creamWhite: Color,
    forestDeep: Color,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) leafGold else Color.White.copy(alpha = 0.15f)
    val bgColor     = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.04f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier       = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = NunitoFamily,
                            fontWeight = FontWeight.Bold,
                            color      = creamWhite,
                        ),
                    )
                    if (isBestValue) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = leafGold,
                        ) {
                            Text(
                                text     = "Best Value",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style    = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = NunitoFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color(0xFF1B2A1E),
                                    fontSize   = 10.sp,
                                ),
                            )
                        }
                    }
                }
                if (subLabel != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = subLabel,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color      = creamWhite.copy(alpha = 0.5f),
                            fontFamily = NunitoFamily,
                        ),
                    )
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text  = price,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = PlayfairFamily,
                        fontWeight = FontWeight.Bold,
                        color      = if (isSelected) leafGold else creamWhite,
                    ),
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text     = period,
                    modifier = Modifier.padding(bottom = 4.dp),
                    style    = MaterialTheme.typography.bodySmall.copy(
                        color      = creamWhite.copy(alpha = 0.55f),
                        fontFamily = NunitoFamily,
                    ),
                )
            }
        }
    }
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PremiumPaywallScreenPreview() {
    LearnWithVelmorthTheme {
        PremiumPaywallScreen(
            onSubscribe = {},
            onBack      = {},
        )
    }
}
