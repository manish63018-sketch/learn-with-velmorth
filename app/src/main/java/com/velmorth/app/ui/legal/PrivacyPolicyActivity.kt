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
import androidx.compose.material.icons.filled.PrivacyTip
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

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                PrivacyPolicyScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
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
                text = "Privacy Policy",
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
            // Intro Icon Card
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
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Your Privacy Matters",
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
            PolicySection(
                title = "1. Information We Collect",
                body = "Learn With Velmorth collects necessary operational data to personalize your learning journey. This includes your display name, username, email address, local progress parameters (completed units, daily XP, streak counts), and technical settings. If signed in, these are synced securely with Firebase Auth and Cloud Firestore to allow multi-device restore."
            )
            
            PolicySection(
                title = "2. How We Use Data",
                body = "We use the collected details to:\n• Sync your gamification parameters (leaves, XP, streak history) across device launches.\n• Schedule automated reminder notifications so you maintain your daily streaks.\n• Deliver personalized audio speaking analyses via our AI speech mechanisms.\n• Continuously optimize and debug technical app performance."
            )

            PolicySection(
                title = "3. Spaced Repetition & Audio Data",
                body = "Spaced repetition (SRS) card parameters are calculated locally on your device and cached in Firestore to determine when specific vocabulary items are due for review. Microphone audio data used during the AI Speaker exercises is parsed instantly for scoring and is not saved or transmitted to third-party databases."
            )

            PolicySection(
                title = "4. Data Sharing & Security",
                body = "We respect your personal space and security. We do not sell or lease your personal credentials. All information is guarded under Firebase's state-of-the-art authentication protocols. Dynamic AdMob banner ads shown to free-tier users do not utilize sensitive localized profile details."
            )

            PolicySection(
                title = "5. Your Controls & Deletion",
                body = "You have full control over your profile. You can change your name or clear your local cache at any time inside settings. To erase your account and all associated Firestore information permanently, click 'Delete Account' in the settings panel."
            )

            PolicySection(
                title = "6. Policy Changes",
                body = "We may update this policy occasionally to support new app updates. Continuing usage of the application implies acceptance of our active terms and security measures. For any questions, please contact support@velmorth.com."
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Thank you for planting the seed of learning with Velmorth! 🌿",
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
fun PolicySection(title: String, body: String) {
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
