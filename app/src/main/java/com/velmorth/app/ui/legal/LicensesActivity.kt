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
import androidx.compose.material.icons.filled.Terminal
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

class LicensesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                LicensesScreen(onBack = { finish() })
            }
        }
    }
}

private data class LicenseItem(
    val name: String,
    val copyright: String,
    val license: String,
    val description: String
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val items = listOf(
        LicenseItem(
            name = "Jetpack Compose & Material 3",
            copyright = "Copyright (C) 2020 The Android Open Source Project",
            license = "Apache License 2.0",
            description = "Declarative toolkit for building premium Android UI layouts programmatically."
        ),
        LicenseItem(
            name = "Firebase Android SDK (Firestore & Auth)",
            copyright = "Copyright 2019 Google LLC",
            license = "Apache License 2.0",
            description = "Provides user session security, cloud synchronization, and atomic metrics logs."
        ),
        LicenseItem(
            name = "Coil Image Loading",
            copyright = "Copyright 2023 Coil Contributors",
            license = "Apache License 2.0",
            description = "High-performance dynamic picture loader for modern Jetpack Compose layouts."
        ),
        LicenseItem(
            name = "Room Persistence Library",
            copyright = "Copyright (C) 2017 The Android Open Source Project",
            license = "Apache License 2.0",
            description = "SQLite object mapping library for offline-first caching of database entries."
        ),
        LicenseItem(
            name = "Kotlin Coroutines & Flow",
            copyright = "Copyright 2000-2020 JetBrains s.r.o.",
            license = "Apache License 2.0",
            description = "Asynchronous library for robust multitasking, UI threads mapping, and delays."
        ),
        LicenseItem(
            name = "Dagger Hilt Dependency Injection",
            copyright = "Copyright 2020 Google LLC",
            license = "Apache License 2.0",
            description = "Hilt provides a standard way to incorporate Dagger dependency injection in Android."
        )
    )

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
                text = "Open-Source Licenses",
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "We are incredibly grateful for the contributions of the open-source developer ecosystem! Below are details for third-party tools integrated within Velmorth.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                lineHeight = 17.sp
            )

            items.forEach { item ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE3F0E9), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = Color(0xFF2D6A4F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = item.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = item.description,
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            lineHeight = 16.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "${item.copyright}\nLicensed under ${item.license}",
                            fontSize = 10.sp,
                            color = Color(0xFF9CA3AF),
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
