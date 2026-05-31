package com.velmorth.app.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.velmorth.app.MainActivity
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.theme.LearnWithVelmorthTheme
import com.velmorth.app.ui.auth.LoginActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity

class PermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                PermissionsScreen(
                    onComplete = {
                        val prefs = PrefsManager(this)
                        prefs.isFirstLaunch = false
                        
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val intent = when {
                            currentUser == null -> Intent(this, LoginActivity::class.java)
                            !prefs.isOnboarded  -> Intent(this, OnboardingActivity::class.java)
                            else                -> Intent(this, MainActivity::class.java)
                        }
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

private data class PermissionItemState(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val permissionString: String?,
    val isRequired: Boolean = false
)

@Composable
fun PermissionsScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    
    // List of permissions we want to display & manage
    val items = remember {
        listOf(
            PermissionItemState(
                title = "Internet Connectivity",
                description = "Required to sync progress with cloud Firestore and load dynamic content.",
                icon = Icons.Default.Wifi,
                permissionString = null // Automatically granted in Manifest
            ),
            PermissionItemState(
                title = "Microphone / Speaking",
                description = "Required for AI Speaker speech exercises and real-time pronunciation reviews.",
                icon = Icons.Default.Mic,
                permissionString = Manifest.permission.RECORD_AUDIO
            ),
            PermissionItemState(
                title = "Camera Capture",
                description = "Used to capture custom profile pictures directly within the app settings.",
                icon = Icons.Default.CameraAlt,
                permissionString = Manifest.permission.CAMERA
            ),
            PermissionItemState(
                title = "Push Notifications",
                description = "Sends daily learning reminders, streak alerts, and critical account messages.",
                icon = Icons.Default.Notifications,
                permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            ),
            PermissionItemState(
                title = "Photos & Media Storage",
                description = "Allows selecting custom profile pictures and offline content storage.",
                icon = Icons.Default.Storage,
                permissionString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
            )
        )
    }

    // Map to keep track of current granted state of each permission in-UI
    val grantedStates = remember { mutableStateMapOf<String?, Boolean>() }
    
    // Initialize states
    LaunchedEffect(Unit) {
        items.forEach { item ->
            val p = item.permissionString
            if (p == null) {
                grantedStates[p] = true // Manifest-only permissions are automatically granted
            } else {
                grantedStates[p] = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    var selectedPermissionToRequest by remember { mutableStateOf<String?>(null) }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        selectedPermissionToRequest?.let { perm ->
            grantedStates[perm] = isGranted
            if (isGranted) {
                Toast.makeText(context, "Permission granted! ✓", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permission denied. Certain features may be limited.", Toast.LENGTH_SHORT).show()
            }
        }
        selectedPermissionToRequest = null
    }

    val requestMultipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (perm, isGranted) ->
            grantedStates[perm] = isGranted
        }
        Toast.makeText(context, "Permissions processed successfully!", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5EE)) // Warm cream background
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title Header
        Text(
            text = "🦦",
            fontSize = 50.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "App Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B4332), // Forest deep green
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Velmorth needs these accesses to deliver a high-fidelity learning experience.",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // List container
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEach { item ->
                val isGranted = grantedStates[item.permissionString] ?: false
                
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon circle
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE3F0E9)), // Warm soft green background
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Details Column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1C1C1E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Status Badge or Button
                        if (isGranted) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "Granted",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2D6A4F),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    item.permissionString?.let { perm ->
                                        selectedPermissionToRequest = perm
                                        requestPermissionLauncher.launch(perm)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Grant",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Request remaining or complete CTA
        val ungrantedList = items.filter { item ->
            item.permissionString != null && !(grantedStates[item.permissionString] ?: false)
        }

        if (ungrantedList.isNotEmpty()) {
            Button(
                onClick = {
                    val listToRequest = ungrantedList.mapNotNull { it.permissionString }.toTypedArray()
                    requestMultipleLauncher.launch(listToRequest)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF52B788)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Request All Missing 🔑",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Continue to App",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
