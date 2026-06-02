package com.velmorth.app.ui.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository
import com.velmorth.app.data.repository.UserRepository
import com.velmorth.app.utils.NetworkUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── Colors ───────────────────────────────────────────────────────────────────
private val EPDarkGreen    = Color(0xFF1B4332)
private val EPPrimaryGreen = Color(0xFF2D6A4F)
private val EPAccentGreen  = Color(0xFF52B788)
private val EPLightGreen   = Color(0xFFB7E4C7)
private val EPBgCream      = Color(0xFFF0F4F1)
private val EPCardWhite    = Color(0xFFFFFFFF)
private val EPTextDark     = Color(0xFF1C1C1E)
private val EPTextMuted    = Color(0xFF6B7280)
private val EPGoldXP       = Color(0xFFF4A261)
private val EPDangerRed    = Color(0xFFE76F51)

/**
 * Premium Edit Profile screen with:
 *  - Gallery photo picker (ActivityResultContracts.GetContent)
 *  - Camera capture (ACTION_IMAGE_CAPTURE with FileProvider)
 *  - Preview of selected photo
 *  - Firebase Storage upload with progress indicator
 *  - Display name & native language editing
 *  - Firestore sync on save
 */
class EditProfileActivity : ComponentActivity() {

    private lateinit var userRepository: UserRepository
    private lateinit var prefsManager: PrefsManager

    // Temp file for camera capture
    private var cameraImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userRepository = UserRepository(this)
        prefsManager   = PrefsManager(this)

        setContent {
            EditProfileScreen()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditProfileScreen() {
        val context       = LocalContext.current
        val user          = userRepository.getUser()

        var displayName   by remember { mutableStateOf(user.displayName.ifBlank { prefsManager.userName }) }
        var nativeLanguage by remember { mutableStateOf(user.nativeLanguage.ifBlank { prefsManager.nativeLanguage }) }
        var isExpanded    by remember { mutableStateOf(false) }

        // Photo state
        var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
        var currentPhotoUrl  by remember { mutableStateOf(prefsManager.photoUrl) }
        var isUploading      by remember { mutableStateOf(false) }
        var showPhotoDialog  by remember { mutableStateOf(false) }

        val languages = listOf("English", "Hindi", "Spanish", "French", "Japanese", "German", "Chinese", "Arabic")

        // ── Gallery picker launcher ──────────────────────────────────────────
        val galleryLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) selectedPhotoUri = uri
        }

        // ── Camera capture launcher ──────────────────────────────────────────
        val cameraLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && cameraImageUri != null) {
                selectedPhotoUri = cameraImageUri
            }
        }

        // ── Permission launcher for camera ───────────────────────────────────
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                cameraImageUri = createTempImageUri(context)
                cameraImageUri?.let { cameraLauncher.launch(it) }
            } else {
                Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Photo option dialog ──────────────────────────────────────────────
        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                icon = {
                    Text("📸", fontSize = 32.sp)
                },
                title = {
                    Text("Change Profile Photo", fontWeight = FontWeight.Bold, color = EPTextDark)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Gallery option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F5E9))
                                .clickable {
                                    showPhotoDialog = false
                                    galleryLauncher.launch("image/*")
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, null, tint = EPPrimaryGreen, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Choose from Gallery", fontWeight = FontWeight.SemiBold, color = EPTextDark, fontSize = 15.sp)
                                Text("Select from your photos", fontSize = 12.sp, color = EPTextMuted)
                            }
                        }
                        // Camera option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE3F2FD))
                                .clickable {
                                    showPhotoDialog = false
                                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.CAMERA
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPermission) {
                                        cameraImageUri = createTempImageUri(context)
                                        cameraImageUri?.let { cameraLauncher.launch(it) }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color(0xFF1565C0), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Take a Photo", fontWeight = FontWeight.SemiBold, color = EPTextDark, fontSize = 15.sp)
                                Text("Use camera to capture", fontSize = 12.sp, color = EPTextMuted)
                            }
                        }
                        // Remove photo option (only show if they have one)
                        if (selectedPhotoUri != null || currentPhotoUrl.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFCEADE))
                                    .clickable {
                                        showPhotoDialog = false
                                        selectedPhotoUri = null
                                        currentPhotoUrl  = ""
                                        prefsManager.photoUrl = ""
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = EPDangerRed, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Remove Photo", fontWeight = FontWeight.SemiBold, color = EPDangerRed, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showPhotoDialog = false }) {
                        Text("Cancel", color = EPTextMuted)
                    }
                },
                containerColor = EPCardWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }

        // ── Main Screen Layout ───────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EPBgCream)
                .verticalScroll(rememberScrollState())
        ) {
            // Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(EPDarkGreen, EPPrimaryGreen))
                    )
                    .padding(top = 16.dp, bottom = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Text(
                            "Edit Profile",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(48.dp))
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── Avatar picker ────────────────────────────────────────
                    Box(contentAlignment = Alignment.BottomEnd) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(EPLightGreen, EPAccentGreen)))
                                .border(3.dp, Color.White, CircleShape)
                                .clickable { showPhotoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            val selectedUri = selectedPhotoUri
                            var bitmap by remember(selectedUri, currentPhotoUrl) { mutableStateOf<android.graphics.Bitmap?>(null) }
                            LaunchedEffect(selectedUri, currentPhotoUrl) {
                                val uriToLoad = selectedUri ?: currentPhotoUrl.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
                                if (uriToLoad != null) {
                                    val uriStr = uriToLoad.toString()
                                    if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                            runCatching {
                                                java.net.URL(uriStr).openStream()?.use { stream ->
                                                    android.graphics.BitmapFactory.decodeStream(stream)
                                                }
                                            }.getOrNull()
                                        }?.let { bmp ->
                                            bitmap = bmp
                                        }
                                    } else {
                                        runCatching {
                                            context.contentResolver.openInputStream(uriToLoad)?.use { stream ->
                                                android.graphics.BitmapFactory.decodeStream(stream)
                                            }
                                        }.getOrNull()?.let { bmp ->
                                            bitmap = bmp
                                        }
                                    }
                                } else {
                                    bitmap = null
                                }
                            }

                            val bmp = bitmap
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = displayName.take(1).uppercase().ifEmpty { "?" },
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EPDarkGreen
                                )
                            }
                        }

                        // Edit badge
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(EPGoldXP)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { showPhotoDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, "Edit Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Tap avatar to change photo",
                        fontSize = 12.sp,
                        color = EPLightGreen.copy(alpha = 0.8f)
                    )

                    // Show "photo selected" indicator
                    AnimatedVisibility(
                        visible = selectedPhotoUri != null,
                        enter = fadeIn(animationSpec = tween(300))
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "✓ New photo selected — save to upload",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Fields card ──────────────────────────────────────────────────
            Card(
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = EPCardWhite),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-20).dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    // Display Name
                    Text(
                        "Display Name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EPTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value          = displayName,
                        onValueChange  = { displayName = it },
                        singleLine     = true,
                        modifier       = Modifier.fillMaxWidth(),
                        shape          = RoundedCornerShape(14.dp),
                        leadingIcon    = {
                            Icon(Icons.Default.Person, null, tint = EPAccentGreen, modifier = Modifier.size(20.dp))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = EPPrimaryGreen,
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedLabelColor    = EPPrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    // Native Language dropdown
                    Text(
                        "Native Language",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EPTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = !isExpanded }
                    ) {
                        OutlinedTextField(
                            value         = nativeLanguage,
                            onValueChange = {},
                            readOnly      = true,
                            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                            leadingIcon   = {
                                Icon(Icons.Default.Language, null, tint = EPAccentGreen, modifier = Modifier.size(20.dp))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape  = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = EPPrimaryGreen,
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded          = isExpanded,
                            onDismissRequest  = { isExpanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        nativeLanguage = lang
                                        isExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Upload progress indicator
                    if (isUploading) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color    = EPPrimaryGreen,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Uploading photo…",
                                fontSize = 13.sp,
                                color    = EPTextMuted
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Save button
                    Button(
                        onClick = {
                            if (displayName.trim().isEmpty()) {
                                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val finalSave = { photoUrlToSave: String ->
                                prefsManager.photoUrl = photoUrlToSave
                                userRepository.updateProfile(displayName.trim(), nativeLanguage)
                                FirestoreProgressRepository.syncUserProfile(
                                    name          = displayName.trim(),
                                    nativeLanguage = nativeLanguage,
                                    profileImage  = photoUrlToSave
                                )
                                Toast.makeText(context, "Profile updated! ✅", Toast.LENGTH_SHORT).show()
                                finish()
                            }

                            val photoUri = selectedPhotoUri
                            if (photoUri != null && NetworkUtils.isOnline(context)) {
                                // Upload to Firebase Storage first
                                isUploading = true
                                FirestoreProgressRepository.uploadProfilePhoto(
                                    imageUri  = photoUri,
                                    onSuccess = { downloadUrl ->
                                        isUploading = false
                                        // Also persist local URI for offline viewing
                                        finalSave(downloadUrl)
                                    },
                                    onFailure = { errMsg ->
                                        isUploading = false
                                        // Fallback: save local URI so profile still shows the photo
                                        Toast.makeText(context, "Photo saved locally (upload failed: $errMsg)", Toast.LENGTH_LONG).show()
                                        finalSave(photoUri.toString())
                                    }
                                )
                            } else if (photoUri != null) {
                                // Offline: save local URI
                                finalSave(photoUri.toString())
                            } else {
                                // No new photo selected
                                finalSave(currentPhotoUrl)
                            }
                        },
                        enabled  = !isUploading,
                        colors   = ButtonDefaults.buttonColors(containerColor = EPPrimaryGreen),
                        shape    = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Default.Save, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Save Changes",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Cancel button
                    OutlinedButton(
                        onClick  = { finish() },
                        enabled  = !isUploading,
                        shape    = RoundedCornerShape(16.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = EPTextMuted),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            "Cancel",
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color      = EPTextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Helper: create a temp file URI for camera output ─────────────────────

    private fun createTempImageUri(context: Context): Uri? {
        return try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val photoDir  = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Velmorth"
            ).also { it.mkdirs() }
            val photoFile = File(photoDir, "IMG_$timestamp.jpg")
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
        } catch (e: Exception) {
            // Fallback to cache dir
            try {
                val cacheDir = File(context.cacheDir, "images").also { it.mkdirs() }
                val file     = File(cacheDir, "temp_photo.jpg")
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } catch (ex: Exception) {
                null
            }
        }
    }
}
