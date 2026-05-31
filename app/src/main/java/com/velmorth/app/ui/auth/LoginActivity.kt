package com.velmorth.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.velmorth.app.R
import com.velmorth.app.MainActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity
import com.velmorth.app.theme.LearnWithVelmorthTheme
import com.velmorth.app.data.local.PrefsManager

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LearnWithVelmorthTheme {
                AuthScreen(
                    onAuthSuccess = { isNew ->
                        val prefs = PrefsManager(this)
                        if (isNew || !prefs.isOnboarded) {
                            startActivity(Intent(this, OnboardingActivity::class.java))
                        } else {
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AuthScreen(onAuthSuccess: (Boolean) -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    var isLogin by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val green = Color(0xFF2D6A4F)
    val cream = Color(0xFFF8F5EE)
    val darkGreen = Color(0xFF1B4332)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo + Title
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = darkGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isLogin) stringResource(R.string.login_subtitle)
                       else stringResource(R.string.signup_subtitle),
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Tab switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8E0D0), RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf(true to "Log In", false to "Sign Up")
                    .forEach { (isLoginTab, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isLogin == isLoginTab) green else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clickable { isLogin = isLoginTab; errorMessage = "" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isLogin == isLoginTab) Color.White else Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Signup-only fields
            AnimatedVisibility(
                visible = !isLogin,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    AuthTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Display Name",
                        keyboardType = KeyboardType.Text
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AuthTextField(
                        value = username,
                        onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } },
                        label = "Username (@username)",
                        keyboardType = KeyboardType.Text
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Email
            AuthTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = "Email Address",
                keyboardType = KeyboardType.Email
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Password
            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                visible = passwordVisible,
                onToggle = { passwordVisible = !passwordVisible }
            )

            // Confirm password (signup only)
            AnimatedVisibility(
                visible = !isLogin,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = "Confirm Password",
                        visible = confirmVisible,
                        onToggle = { confirmVisible = !confirmVisible }
                    )
                }
            }

            // Forgot password
            AnimatedVisibility(visible = isLogin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        if (email.isBlank()) { errorMessage = "Please enter your email address to reset your password."; return@TextButton }
                        auth.sendPasswordResetEmail(email.trim())
                        errorMessage = "A password reset email has been sent successfully."
                    }) {
                        Text(
                            text = "Forgot Password?",
                            color = green,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Error message
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFB00020),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary CTA button
            Button(
                onClick = {
                    errorMessage = ""
                    val validationError = validateInputs(
                        isLogin = isLogin,
                        name = name,
                        username = username,
                        email = email,
                        password = password,
                        confirmPassword = confirmPassword
                    )
                    if (validationError != null) { errorMessage = validationError; return@Button }

                    isLoading = true
                    if (isLogin) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { isLoading = false; onAuthSuccess(false) }
                            .addOnFailureListener { isLoading = false; errorMessage = it.localizedMessage ?: "An error occurred. Please try again." }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val user = result.user!!
                                // Send email verification
                                user.sendEmailVerification()
                                // Save to Firestore
                                val userDoc = hashMapOf(
                                    "uid" to user.uid,
                                    "name" to name.trim(),
                                    "username" to username.trim().lowercase(),
                                    "email" to email.trim(),
                                    "photoUrl" to "",
                                    "isPremium" to false,
                                    "streak" to 0,
                                    "leafBalance" to 5,
                                    "darkMode" to false,
                                    "notificationsEnabled" to true,
                                    "emailVerified" to false,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )
                                db.collection("users").document(user.uid).set(userDoc)
                                    .addOnSuccessListener {
                                        // Save to local prefs
                                        val prefs = PrefsManager(context)
                                        prefs.userName = name.trim()
                                        prefs.username = username.trim().lowercase()
                                        prefs.userEmail = email.trim()
                                        prefs.isPremium = false
                                        prefs.streak = 0
                                        prefs.leaves = 5
                                        isLoading = false
                                        onAuthSuccess(true)
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        errorMessage = it.localizedMessage ?: "An error occurred. Please try again."
                                    }
                            }
                            .addOnFailureListener { isLoading = false; errorMessage = it.localizedMessage ?: "An error occurred. Please try again." }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = green),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (isLogin) "Log In" else "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "By continuing, you agree to our Privacy Policy.",
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2D6A4F),
            focusedLabelColor = Color(0xFF2D6A4F)
        )
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = Color(0xFF2D6A4F)
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2D6A4F),
            focusedLabelColor = Color(0xFF2D6A4F)
        )
    )
}

fun validateInputs(
    isLogin: Boolean, name: String, username: String,
    email: String, password: String, confirmPassword: String
): String? {
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        return "Please enter a valid email address."
    if (password.length < 8)
        return "Password must be at least 8 characters long."
    if (!isLogin) {
        if (name.isBlank()) return "Display Name is required."
        if (username.length < 3) return "Username must be at least 3 characters long."
        if (password != confirmPassword) return "Passwords do not match."
    }
    return null
}
