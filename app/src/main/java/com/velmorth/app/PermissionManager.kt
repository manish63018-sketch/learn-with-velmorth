package com.velmorth.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─────────────────────────────────────────────────────────────────────────────
// Permission groups the app needs
// ─────────────────────────────────────────────────────────────────────────────

private data class PermissionInfo(
    val permission: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private fun buildRequiredPermissions(): List<PermissionInfo> = buildList {
    add(
        PermissionInfo(
            permission  = Manifest.permission.RECORD_AUDIO,
            icon        = Icons.Default.MicNone,
            title       = "Microphone",
            description = "Required for AI Speaker – speak and get pronunciation feedback.",
        )
    )
    add(
        PermissionInfo(
            permission  = Manifest.permission.CAMERA,
            icon        = Icons.Default.Camera,
            title       = "Camera",
            description = "Used for profile photos and future AR features.",
        )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            PermissionInfo(
                permission  = Manifest.permission.READ_MEDIA_IMAGES,
                icon        = Icons.Default.Storage,
                title       = "Photos & Media",
                description = "Lets you pick a profile picture from your gallery.",
            )
        )
        add(
            PermissionInfo(
                permission  = Manifest.permission.POST_NOTIFICATIONS,
                icon        = Icons.Default.NotificationsNone,
                title       = "Notifications",
                description = "Sends daily streak reminders and lesson alerts.",
            )
        )
    } else {
        add(
            PermissionInfo(
                permission  = Manifest.permission.READ_EXTERNAL_STORAGE,
                icon        = Icons.Default.Storage,
                title       = "Storage",
                description = "Lets you pick a profile picture from your gallery.",
            )
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Public composable – call once near the root of the app
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Displays a rationale overlay the first time the app runs that explains
 * what each permission is used for, then fires the system permission
 * request for all of them at once.
 *
 * @param onAllHandled called after the user either grants or dismisses all permissions.
 */
@Composable
fun AppPermissionHandler(onAllHandled: () -> Unit = {}) {
    val allPermissions = remember { buildRequiredPermissions() }

    var showRationale by remember { mutableStateOf(true) }
    android.util.Log.d("VelmorthDebug", "AppPermissionHandler composition: showRationale = $showRationale, permissions count = ${allPermissions.size}")

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        android.util.Log.d("VelmorthDebug", "AppPermissionHandler: launcher callback: $results")
        // Regardless of what was granted/denied we continue – the app degrades
        // gracefully per feature.
        onAllHandled()
    }

    AnimatedVisibility(
        visible = showRationale,
        enter   = fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f),
        exit    = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center,
        ) {
            PermissionRationaleCard(
                permissions = allPermissions,
                onAllow = {
                    android.util.Log.d("VelmorthDebug", "AppPermissionHandler: onAllow triggered")
                    showRationale = false
                    launcher.launch(allPermissions.map { it.permission }.toTypedArray())
                },
                onSkip = {
                    android.util.Log.d("VelmorthDebug", "AppPermissionHandler: onSkip triggered")
                    showRationale = false
                    onAllHandled()
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rationale card UI (no Dialog wrapper — rendered directly in Box)
// ─────────────────────────────────────────────────────────────────────────────

private val PermForestDeep   = Color(0xFF1B4332)
private val PermMossGreen    = Color(0xFF40916C)
private val PermLeafGold     = Color(0xFFD4A017)
private val PermCreamWhite   = Color(0xFFF1E8D0)

@Composable
private fun PermissionRationaleCard(
    permissions: List<PermissionInfo>,
    onAllow: () -> Unit,
    onSkip: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight(),
        shape  = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF122A1D)),
        elevation = CardDefaults.cardElevation(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Text(text = "🦦", fontSize = 48.sp)
            Text(
                text  = "Before we begin…",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color      = PermCreamWhite,
                    fontWeight = FontWeight.ExtraBold,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Velmorth needs a few permissions to give you the best learning experience.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.70f),
                ),
                textAlign = TextAlign.Center,
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Permission rows
            permissions.forEach { perm ->
                PermissionRow(perm)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            // Allow button
            Button(
                onClick = onAllow,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape  = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(PermMossGreen, PermLeafGold)),
                            RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Allow Permissions ✅",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color      = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 16.sp,
                        ),
                    )
                }
            }

            // Skip link
            TextButton(onClick = onSkip) {
                Text(
                    text  = "Maybe later",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White.copy(alpha = 0.45f),
                    ),
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(info: PermissionInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PermMossGreen.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = info.icon,
                contentDescription = null,
                tint               = PermLeafGold,
                modifier           = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = info.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color      = PermCreamWhite,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text  = info.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.60f),
                ),
            )
        }
    }
}
