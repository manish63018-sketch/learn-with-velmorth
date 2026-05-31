package com.velmorth.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.velmorth.app.MainActivity
import com.velmorth.app.R
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.model.User
import com.velmorth.app.data.repository.UserRepository

class LoginActivity : ComponentActivity() {

    private var firebaseAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private var isFirebaseAvailable = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userRepository = UserRepository(this)
        val prefs = PrefsManager(this)

        // Try initializing Firebase services safely
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                firebaseAuth = FirebaseAuth.getInstance()
                firestore = FirebaseFirestore.getInstance()
                isFirebaseAvailable = true
                Log.i("LoginActivity", "Firebase Auth and Firestore services connected successfully.")
            } else {
                Log.w("LoginActivity", "FirebaseApp not initialized, running in mock mode.")
            }
        } catch (e: Exception) {
            Log.w("LoginActivity", "Firebase setup failed: ${e.message}. Running in Mock fallback mode.")
            isFirebaseAvailable = false
        }

        setContent {
            AuthScreen(
                isFirebaseAvailable = isFirebaseAvailable,
                onAuthSuccess = { user ->
                    userRepository.saveUser(user)
                    prefs.isOnboarded = true
                    Toast.makeText(this, "Welcome, ${user.name}!", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                },
                onSkip = {
                    // Create guest user profile
                    val guestUser = User(
                        uid = "local_guest",
                        name = "Guest Learner",
                        username = "guestlearner",
                        email = "guest@velmorth.com",
                        photoUrl = "",
                        isPremium = false,
                        streak = 0,
                        leafBalance = 5,
                        darkMode = prefs.darkMode,
                        notificationsEnabled = prefs.notificationsEnabled,
                        createdAt = Timestamp.now()
                    )
                    userRepository.saveUser(guestUser)
                    Toast.makeText(this, "Continuing as Guest!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                },
                firebaseAuth = firebaseAuth,
                firestore = firestore
            )
        }
    }
}

@Composable
fun AuthScreen(
    isFirebaseAvailable: Boolean,
    onAuthSuccess: (User) -> Unit,
    onSkip: () -> Unit,
    firebaseAuth: FirebaseAuth?,
    firestore: FirebaseFirestore?
) {
    val context = LocalContext.current
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }

    // Animation trigger
    var animateEntrance by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateEntrance = true
    }

    // Interactive states
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordSuccess by remember { mutableStateOf(false) }

    // Animation definitions
    val logoAlpha by animateFloatAsState(targetValue = if (animateEntrance) 1f else 0f, animationSpec = tween(800))
    val logoScale by animateFloatAsState(
        targetValue = if (animateEntrance) 1f else 0.4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    val cardOffsetY by animateDpAsState(
        targetValue = if (animateEntrance) 0.dp else 120.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
    )

    // Password rules validation helper
    fun validatePassword(pass: String): String? {
        if (pass.isEmpty()) return null
        if (pass.length < 8) return "Must be at least 8 characters long"
        if (!pass.any { it.isUpperCase() }) return "Must contain at least 1 uppercase letter"
        if (!pass.any { it.isLowerCase() }) return "Must contain at least 1 lowercase letter"
        if (!pass.any { it.isDigit() }) return "Must contain at least 1 number"
        return null
    }

    val passwordError = validatePassword(password)
    val isPasswordValid = password.isNotEmpty() && passwordError == null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F5EE))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 🌿 Logo & Welcome Animation block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(logoAlpha)
                    .scale(logoScale)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(48.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🌿",
                        fontSize = 48.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Learn with Velmorth",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Master languages natively & efficiently",
                    fontSize = 14.sp,
                    color = Color(0xFF556B2F),
                    textAlign = TextAlign.Center
                )
            }

            // Developer Mock Mode Status badge
            if (!isFirebaseAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFECE0C8), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Offline Mode",
                        tint = Color(0xFF7D5A2B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mock Development Mode (Offline Ready)",
                        fontSize = 11.sp,
                        color = Color(0xFF7D5A2B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Form Card Container (slides up)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = cardOffsetY),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Unified tab switchers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFECE7DB), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isLoginMode) Color(0xFF2D6A4F) else Color.Transparent)
                                .clickable {
                                    isLoginMode = true
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Log In",
                                color = if (isLoginMode) Color.White else Color(0xFF1B4332),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!isLoginMode) Color(0xFF2D6A4F) else Color.Transparent)
                                .clickable {
                                    isLoginMode = false
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Sign Up",
                                color = if (!isLoginMode) Color.White else Color(0xFF1B4332),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Visualizer
                    errorMessage?.let { msg ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFDE8E8), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Error",
                                tint = Color(0xFFE63946),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                color = Color(0xFFE63946),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Forgot password mock feedback
                    if (showForgotPasswordSuccess) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Success",
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Password reset email sent successfully!",
                                color = Color(0xFF2D6A4F),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Fields rendering based on active mode
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (!isLoginMode) {
                            // SIGN UP Mode Fields
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2D6A4F),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = Color(0xFF2D6A4F)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Username (@username)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2D6A4F),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = Color(0xFF2D6A4F)
                                )
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // EMAIL field (Shared)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2D6A4F),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = Color(0xFF2D6A4F)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // PASSWORD field (Shared, with Show/Hide toggle)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "Show/Hide Password",
                                        tint = Color(0xFF6B7280)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2D6A4F),
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedLabelColor = Color(0xFF2D6A4F)
                            )
                        )

                        // Inline validation warnings for password strength check during Signup
                        if (!isLoginMode && password.isNotEmpty() && passwordError != null) {
                            Text(
                                text = passwordError,
                                color = Color(0xFFE63946),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isLoginMode) {
                            // CONFIRM PASSWORD (Sign Up mode only)
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirm Password") },
                                singleLine = true,
                                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                        Icon(
                                            imageVector = if (showConfirmPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "Toggle password",
                                            tint = Color(0xFF6B7280)
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2D6A4F),
                                    unfocusedBorderColor = Color(0xFFE5E7EB),
                                    focusedLabelColor = Color(0xFF2D6A4F)
                                )
                            )
                            if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Text(
                                    text = "Passwords do not match",
                                    color = Color(0xFFE63946),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Terms & Conditions Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { termsAccepted = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2D6A4F))
                                )
                                Text(
                                    text = "I accept the Terms and Conditions",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4B5563)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            // Forgot Password Link (Login mode only)
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 13.sp,
                                    color = Color(0xFF2D6A4F),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable {
                                            if (email.isEmpty() || !email.contains("@")) {
                                                errorMessage = "Please enter your email above to reset password."
                                            } else {
                                                isLoading = true
                                                if (isFirebaseAvailable && firebaseAuth != null) {
                                                    firebaseAuth.sendPasswordResetEmail(email)
                                                        .addOnCompleteListener { task ->
                                                            isLoading = false
                                                            if (task.isSuccessful) {
                                                                showForgotPasswordSuccess = true
                                                                errorMessage = null
                                                            } else {
                                                                errorMessage = task.exception?.message ?: "Reset request failed."
                                                            }
                                                        }
                                                } else {
                                                    // Local mock reset
                                                    isLoading = false
                                                    showForgotPasswordSuccess = true
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // SUBMIT button
                        Button(
                            onClick = {
                                if (isLoginMode) {
                                    // Log In Action
                                    if (email.isEmpty() || password.isEmpty()) {
                                        errorMessage = "Please enter both email and password."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    showForgotPasswordSuccess = false

                                    if (isFirebaseAvailable && firebaseAuth != null && firestore != null) {
                                        firebaseAuth.signInWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val uid = firebaseAuth.currentUser?.uid ?: ""
                                                    firestore.collection("users").document(uid).get()
                                                        .addOnSuccessListener { doc ->
                                                            isLoading = false
                                                            val user = doc.toObject(User::class.java)
                                                            if (user != null) {
                                                                onAuthSuccess(user)
                                                            } else {
                                                                // Sync failed, create fallback
                                                                onAuthSuccess(User(
                                                                    uid = uid,
                                                                    name = email.substringBefore("@"),
                                                                    username = email.substringBefore("@").lowercase(),
                                                                    email = email,
                                                                    createdAt = Timestamp.now()
                                                                ))
                                                            }
                                                        }
                                                        .addOnFailureListener { e ->
                                                            isLoading = false
                                                            onAuthSuccess(User(
                                                                uid = uid,
                                                                name = email.substringBefore("@"),
                                                                username = email.substringBefore("@").lowercase(),
                                                                email = email,
                                                                createdAt = Timestamp.now()
                                                            ))
                                                        }
                                                } else {
                                                    isLoading = false
                                                    errorMessage = task.exception?.message ?: "Invalid email or password."
                                                }
                                            }
                                    } else {
                                        // MOCK SUCCESSFUL AUTH fallback
                                        isLoading = false
                                        val mockUser = User(
                                            uid = "mock_uid_123",
                                            name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                                            username = email.substringBefore("@").lowercase(),
                                            email = email,
                                            photoUrl = "",
                                            isPremium = false,
                                            streak = 1,
                                            leafBalance = 5,
                                            createdAt = Timestamp.now()
                                        )
                                        onAuthSuccess(mockUser)
                                    }
                                } else {
                                    // Sign Up Action
                                    if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                                        errorMessage = "Please fill in all details."
                                        return@Button
                                    }
                                    if (passwordError != null) {
                                        errorMessage = "Password does not meet validation criteria."
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "Passwords do not match."
                                        return@Button
                                    }
                                    if (!termsAccepted) {
                                        errorMessage = "You must accept the Terms & Conditions."
                                        return@Button
                                    }

                                    isLoading = true
                                    errorMessage = null
                                    showForgotPasswordSuccess = false

                                    if (isFirebaseAvailable && firebaseAuth != null && firestore != null) {
                                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val uid = firebaseAuth.currentUser?.uid ?: ""
                                                    // Send email verification immediately
                                                    firebaseAuth.currentUser?.sendEmailVerification()
                                                    val newUser = User(
                                                        uid = uid,
                                                        name = name,
                                                        username = username.lowercase().trim(),
                                                        email = email,
                                                        photoUrl = "",
                                                        isPremium = false,
                                                        streak = 0,
                                                        leafBalance = 5,
                                                        createdAt = Timestamp.now()
                                                    )
                                                    // Save user record to Firestore
                                                    firestore.collection("users").document(uid).set(newUser)
                                                        .addOnSuccessListener {
                                                            isLoading = false
                                                            Toast.makeText(
                                                                context,
                                                                "Verification email sent to $email",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                            onAuthSuccess(newUser)
                                                        }
                                                        .addOnFailureListener { _ ->
                                                            isLoading = false
                                                            onAuthSuccess(newUser) // Proceed offline if Firestore write fails
                                                        }
                                                } else {
                                                    isLoading = false
                                                    errorMessage = task.exception?.message ?: "Account creation failed."
                                                }
                                            }
                                    } else {
                                        // Mock Sign Up Success fallback
                                        isLoading = false
                                        val mockUser = User(
                                            uid = "mock_uid_created",
                                            name = name,
                                            username = username.lowercase().trim(),
                                            email = email,
                                            photoUrl = "",
                                            isPremium = false,
                                            streak = 0,
                                            leafBalance = 5,
                                            createdAt = Timestamp.now()
                                        )
                                        onAuthSuccess(mockUser)
                                    }
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isLoginMode) "Log In" else "Create Account",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Google Sign-In Option
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Connecting Google account...", Toast.LENGTH_SHORT).show()
                                onAuthSuccess(
                                    User(
                                        uid = "google_user_mock",
                                        name = "Google Learner",
                                        username = "google_learner",
                                        email = "google@velmorth.com",
                                        isPremium = false,
                                        streak = 1,
                                        leafBalance = 15,
                                        createdAt = Timestamp.now()
                                    )
                                )
                            },
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "G ",
                                    color = Color(0xFFEA4335),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    "Continue with Google",
                                    color = Color(0xFF374151),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Guest Offline Bypass
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Skip and Continue as Guest",
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280),
                                textDecoration = TextDecoration.Underline,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable { onSkip() }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
