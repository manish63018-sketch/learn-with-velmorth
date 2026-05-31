package com.velmorth.app.ui.premium

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.data.local.PrefsManager

// ── Colors ─────────────────────────────────────────────────────────────────────
private val BgDeep       = Color(0xFF0F2D1E)
private val BgDark       = Color(0xFF1B4332)
private val BgMid        = Color(0xFF2D6A4F)
private val GoldPrimary  = Color(0xFFD4AC0D)
private val GoldLight    = Color(0xFFF4D03F)
private val GoldSurface  = Color(0xFF3D2E00)
private val MintLight    = Color(0xFFB7E4C7)
private val CardSurface  = Color(0xFF1F3D2E)
private val CardBorder   = Color(0xFF2D5A3F)
private val TextWhite    = Color(0xFFF8F5EE)
private val TextDim      = Color(0xFF8FA896)

// ── Subscription Plan model ────────────────────────────────────────────────────

private data class SubscriptionPlan(
    val planId          : String,
    val title           : String,
    val priceInr        : Int,
    val billingPeriod   : String,   // "monthly" | "yearly"
    val trialDays       : Int,
    val autoRenew       : Boolean,
    val savingLabel     : String?,  // e.g. "Save 44%"
    val playStoreId     : String,
    // Derived display fields
    val priceLabel      : String,
    val subLabel        : String
)

private val PLANS = listOf(
    SubscriptionPlan(
        planId        = "velmorth_premium_monthly",
        title         = "Premium Monthly",
        priceInr      = 149,
        billingPeriod = "monthly",
        trialDays     = 1,
        autoRenew     = true,
        savingLabel   = null,
        playStoreId   = "velmorth.premium.monthly",
        priceLabel    = "₹149 / month",
        subLabel      = "1-day free trial · Auto-renews monthly"
    ),
    SubscriptionPlan(
        planId        = "velmorth_premium_yearly",
        title         = "Premium Yearly",
        priceInr      = 999,
        billingPeriod = "yearly",
        trialDays     = 1,
        autoRenew     = true,
        savingLabel   = "Save 44%",
        playStoreId   = "velmorth.premium.yearly",
        priceLabel    = "₹999 / year",
        subLabel      = "1-day free trial · Just ₹83/month · Auto-renews yearly"
    )
)

// ── Premium features list ──────────────────────────────────────────────────────

private data class PremiumFeature(val icon: ImageVector, val text: String)

private val FEATURES = listOf(
    PremiumFeature(Icons.Default.AllInclusive,   "Unlimited 🍃 Leaves — never run out mid-lesson"),
    PremiumFeature(Icons.Default.Block,           "Zero ads — pure, distraction-free learning"),
    PremiumFeature(Icons.Default.RecordVoiceOver, "Native speaker audio & pronunciation feedback"),
    PremiumFeature(Icons.Default.DarkMode,        "Premium dark mode & exclusive themes"),
    PremiumFeature(Icons.Default.OfflinePin,      "Offline mode — learn anywhere, anytime"),
    PremiumFeature(Icons.Default.Psychology,      "AI-powered personalised review sessions")
)

/**
 * Premium paywall — exact plan spec from product JSON:
 *   • velmorth_premium_monthly  ₹149/mo  1-day trial
 *   • velmorth_premium_yearly   ₹999/yr  1-day trial  "Save 44%"
 *
 * Play Store product IDs are wired at the bottom of the file.
 * Billing SDK integration left as a stub (TODO) for when Play Store is connected.
 */
class PremiumActivity : ComponentActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefsManager = PrefsManager(this)
        setContent { PremiumScreen() }
    }

    @Composable
    private fun PremiumScreen() {
        // Default selection: yearly (best value)
        var selectedIdx by remember { mutableStateOf(1) }

        // Crown pulse animation
        val infiniteAnim = rememberInfiniteTransition(label = "crown")
        val crownScale by infiniteAnim.animateFloat(
            initialValue   = 1f,
            targetValue    = 1.08f,
            animationSpec  = infiniteRepeatable(
                animation  = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "crownScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(BgDeep, BgDark, BgMid))
                )
        ) {
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Top bar ───────────────────────────────────────────────────
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back", tint = TextWhite)
                    }
                    Text("Velmorth Premium", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = GoldPrimary)
                    // Restore
                    TextButton(onClick = {
                        Toast.makeText(this@PremiumActivity,
                            "Restore: connect Play Billing SDK 🚧", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Restore", fontSize = 13.sp, color = MintLight)
                    }
                }

                // ── Crown hero ────────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text("👑", fontSize = 72.sp,
                    modifier = Modifier.scale(crownScale))
                Spacer(Modifier.height(8.dp))
                Text(
                    "Unlock Premium",
                    fontSize   = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = GoldLight,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "The ultimate language learning experience",
                    fontSize  = 14.sp,
                    color     = MintLight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(28.dp))

                // ── Features card ─────────────────────────────────────────────
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        FEATURES.forEach { feature ->
                            FeatureRow(feature)
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Plan selector header ──────────────────────────────────────
                Text(
                    "Choose Your Plan",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextWhite,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(10.dp))

                // ── Plan cards ────────────────────────────────────────────────
                Column(
                    modifier            = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PLANS.forEachIndexed { idx, plan ->
                        PlanCard(
                            plan       = plan,
                            isSelected = idx == selectedIdx,
                            onClick    = { selectedIdx = idx }
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // ── Trial badge ───────────────────────────────────────────────
                Surface(
                    shape  = RoundedCornerShape(20.dp),
                    color  = Color(0xFF2A4A35)
                ) {
                    Row(
                        modifier          = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.CardGiftcard, null,
                            tint = GoldPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            "Start with a FREE 1-day trial — cancel anytime",
                            fontSize = 13.sp,
                            color    = MintLight,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── CTA button ────────────────────────────────────────────────
                val selectedPlan = PLANS[selectedIdx]
                Button(
                    onClick = {
                        launchPurchase(selectedPlan)
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                    shape   = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, null,
                        tint = BgDeep, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Start Free Trial → ${selectedPlan.priceLabel}",
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = BgDeep
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Legal footnote
                Text(
                    "Subscription auto-renews. Cancel before trial ends to avoid charges.\n" +
                    "Managed via Google Play. Privacy Policy · Terms of Service",
                    fontSize  = 11.sp,
                    color     = TextDim,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(horizontal = 28.dp)
                )
            }
        }
    }

    /**
     * Launches the Play Store billing flow for [plan].
     * TODO: Replace stub with actual BillingClient.launchBillingFlow() call when
     *       the Play Store app is published and billing SDK is integrated.
     *
     * Product IDs to register in Play Console:
     *   • velmorth.premium.monthly  (subscription)
     *   • velmorth.premium.yearly   (subscription)
     */
    private fun launchPurchase(plan: SubscriptionPlan) {
        // Stub — swap this block with BillingClient when Play Store is live
        Toast.makeText(
            this,
            "Play Billing coming soon!\n" +
            "Product: ${plan.playStoreId}\n" +
            "Trial: ${plan.trialDays} day(s) free 🚧",
            Toast.LENGTH_LONG
        ).show()

        // DEV MODE: grant premium locally for testing UI
        if (isDevMode()) {
            prefsManager.isPremium = true
            Toast.makeText(this, "Dev mode: Premium activated locally ✓", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        }
    }

    /** Returns true only on debug builds — never on release. */
    private fun isDevMode(): Boolean =
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
}

// ── Composables ───────────────────────────────────────────────────────────────

@Composable
private fun FeatureRow(feature: PremiumFeature) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A5A3A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, contentDescription = null,
                tint = GoldPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            feature.text,
            fontSize   = 14.sp,
            color      = TextWhite,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PlanCard(
    plan       : SubscriptionPlan,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    val borderBrush = if (isSelected) {
        Brush.linearGradient(listOf(GoldPrimary, GoldLight))
    } else {
        Brush.linearGradient(listOf(CardBorder, CardBorder))
    }
    val cardBg = if (isSelected) Color(0xFF253D2C) else CardSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .then(
                if (isSelected) Modifier.border(
                    width  = 2.dp,
                    brush  = borderBrush,
                    shape  = RoundedCornerShape(18.dp)
                ) else Modifier.border(
                    width  = 1.dp,
                    color  = CardBorder,
                    shape  = RoundedCornerShape(18.dp)
                )
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Radio dot
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) GoldPrimary else Color.Transparent)
                    .border(
                        width  = 2.dp,
                        color  = if (isSelected) GoldPrimary else TextDim,
                        shape  = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BgDeep)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Plan info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        plan.title,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextWhite
                    )
                    // "Save 44%" badge
                    plan.savingLabel?.let { label ->
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = GoldPrimary
                        ) {
                            Text(
                                label,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color      = BgDeep,
                                modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(plan.subLabel, fontSize = 12.sp, color = MintLight.copy(alpha = 0.8f))

                // Trial callout
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardGiftcard, null,
                        tint = GoldPrimary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${plan.trialDays}-day free trial included",
                        fontSize  = 11.sp,
                        color     = GoldPrimary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Price
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    plan.priceLabel.substringBefore(" /"),
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = if (isSelected) GoldLight else TextWhite
                )
                Text(
                    "/ ${plan.billingPeriod}",
                    fontSize = 11.sp,
                    color    = TextDim
                )
            }
        }
    }
}
