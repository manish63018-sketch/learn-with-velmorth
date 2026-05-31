package com.velmorth.app.ui.legal

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpCenter
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.R
import com.velmorth.app.theme.LearnWithVelmorthTheme

class ContactSupportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                ContactSupportScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
fun ContactSupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val configurableEmail = context.getString(R.string.support_email)

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
                text = "Contact Support",
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Support Agent Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFFE3F0E9), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SupportAgent,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "How can we help you?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1C1E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Our forest support team is active 24/7 to resolve streak errors, restore premium billing, and catalog grammar feedbacks.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // FAQ Items
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HelpCenter,
                            contentDescription = null,
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Frequently Asked Questions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1C1E)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FAQItem(
                        question = "My daily streak got broken, can I restore it?",
                        answer = "Yes! You can purchase a 'Streak Freeze' boost in our Leaf Shop using earned leaf coins to prevent streaks from breaking, or contact us to verify manual restorations."
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))
                    FAQItem(
                        question = "Can I access lessons offline?",
                        answer = "Absolutely! Velmorth is completely offline-first. Your curriculum is packed inside assets, and scores are stored in Room db, syncing with Firestore when internet is available."
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // ── Instagram Support (Primary) ───────────────────────────────────
            Button(
                onClick = {
                    val uri = Uri.parse("https://www.instagram.com/mannish_2323")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        // Try native Instagram app first
                        intent.setPackage("com.instagram.android")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Browser fallback
                        intent.setPackage(null)
                        context.startActivity(intent)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)), // Instagram pink
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(text = "📸", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DM on Instagram @mannish_2323",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "💚 Preferred support channel — fastest response",
                fontSize = 12.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── Email Support (Fallback) ───────────────────────────────────────
            Button(
                onClick = {
                    val mail = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(configurableEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "Velmorth Support Request")
                    }
                    context.startActivity(Intent.createChooser(mail, "Choose Email Client"))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Email Support at $configurableEmail",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


@Composable
fun FAQItem(question: String, answer: String) {
    Column {
        Text(
            text = "Q: $question",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B4332)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = answer,
            fontSize = 12.sp,
            color = Color(0xFF4B5563),
            lineHeight = 16.sp
        )
    }
}
