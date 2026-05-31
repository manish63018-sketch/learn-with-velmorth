package com.velmorth.app.ui.legal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.theme.LearnWithVelmorthTheme

class TermsConditionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                TermsConditionsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun TermsConditionsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5EE)) // Warm cream background
    ) {
        // Premium Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B4332)) // Forest green header
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = "Terms & Conditions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFE3F0E9), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Terms of Service",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                        Text(
                            text = "Last updated: May 31, 2026",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Body Sections
            TermsSection(
                title = "1. Acceptance of Terms",
                body = "By creating an account, loading lessons, or engaging with our interactive mascot, you agree to comply with and be bound by these dynamic Terms & Conditions. If you do not agree to these rules, please discontinue use of the application."
            )

            TermsSection(
                title = "2. User Accounts & Security",
                body = "You are responsible for keeping your local credentials secure. When signing up, you agree to provide authentic and unique usernames. We reserve the right to revoke or suspend accounts demonstrating abusive behavior, spamming, or custom curriculum injections."
            )

            TermsSection(
                title = "3. Leaf Economy & Virtual Assets",
                body = "Leaves are gamified, in-app points earned through completing language exercises, reviewing card queues, and claiming daily login rewards. Leaves have NO real-world financial valuation and cannot be traded, exchanged, or liquidated for legal currency."
            )

            TermsSection(
                title = "4. Premium Subscriptions",
                body = "Velmorth Premium unlocks unlimited leaves, premium dark mode settings, and interactive voice tools. Subscriptions are billed on a recurring monthly or annual basis. You can cancel at any time through standard Play Store payment SDK controls. Restorations are subject to verification."
            )

            TermsSection(
                title = "5. Copyright & Curriculum License",
                body = "All syllabus designs, graphics, mascot iterations, and translation structures are the property of the Velmorth Team and protected under international copyright law. We grant you a limited, non-transferable license to consume this content for personal, non-commercial education."
            )

            TermsSection(
                title = "6. Limitation of Liability",
                body = "Learn With Velmorth is provided 'as is'. While we aim to eliminate all crashes and validate all translations, we do not warrant complete accuracy or uninterrupted database sync. We are not liable for any progress data losses resulting from cache clearance or system resets."
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Thank you for studying under the protection of the forest! 🌿",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2D6A4F),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TermsSection(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B4332)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            fontSize = 13.sp,
            color = Color(0xFF4B5563),
            lineHeight = 18.sp
        )
    }
}
