package com.velmorth.app.ui.profile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.UserRepository

/**
 * Screen to edit profile name and native language.
 */
class EditProfileActivity : ComponentActivity() {

    private lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        userRepository = UserRepository(this)
        val user = userRepository.getUser()

        setContent {
            EditProfileScreen(
                initialName = user.displayName,
                initialNativeLang = user.nativeLanguage,
                onSave = { updatedName, updatedNativeLang ->
                    if (updatedName.trim().isNotEmpty()) {
                        userRepository.updateProfile(updatedName, updatedNativeLang)
                        val prefs = PrefsManager(this@EditProfileActivity)
                        com.velmorth.app.data.repository.FirestoreProgressRepository.syncUserProfile(
                            name = updatedName.trim(),
                            nativeLanguage = updatedNativeLang,
                            profileImage = prefs.photoUrl
                        )
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Display Name cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                },
                onCancel = {
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialName: String,
    initialNativeLang: String,
    onSave: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var displayName by remember { mutableStateOf(initialName) }
    var nativeLanguage by remember { mutableStateOf(initialNativeLang) }
    var isExpanded by remember { mutableStateOf(false) }

    val languages = listOf("English", "Hindi", "Spanish", "French", "Japanese", "German", "Chinese")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5EE))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Edit Profile",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B4332),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Update your display details immediately",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display Name Field
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2D6A4F),
                unfocusedBorderColor = Color(0xFFE5E7EB),
                focusedLabelColor = Color(0xFF2D6A4F)
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Native Language Selector
        Text(
            text = "Native Language",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1C1C1E),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )

        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = !isExpanded }
        ) {
            OutlinedTextField(
                value = nativeLanguage,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2D6A4F),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                )
            )
            ExposedDropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                languages.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            nativeLanguage = selection
                            isExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = { onSave(displayName, nativeLanguage) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = ButtonDefaults.outlinedButtonBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(text = "Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
        }
    }
}
