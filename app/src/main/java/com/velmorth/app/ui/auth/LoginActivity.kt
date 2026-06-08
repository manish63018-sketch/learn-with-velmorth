package com.velmorth.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.velmorth.app.R
import com.velmorth.app.MainActivity
import com.velmorth.app.ui.onboarding.OnboardingActivity
import com.velmorth.app.theme.LearnWithVelmorthTheme
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.FirestoreProgressRepository

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
    val auth    = FirebaseAuth.getInstance()

    var isLogin           by remember { mutableStateOf(true) }
    var name              by remember { mutableStateOf("") }
    var username          by remember { mutableStateOf("") }
    var email             by remember { mutableStateOf("") }
    var password          by remember { mutableStateOf("") }
    var confirmPassword   by remember { mutableStateOf("") }
    var passwordVisible   by remember { mutableStateOf(false) }
    var confirmVisible    by remember { mutableStateOf(false) }
    var isLoading         by remember { mutableStateOf(false) }
    var googleLoading     by remember { mutableStateOf(false) }
    var errorMessage      by remember { mutableStateOf("") }


    // Google Sign-In client
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Launcher for the Google account picker
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleLoading = true
        try {
            val task    = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken

            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val user    = authResult.user!!
                        val isNew   = authResult.additionalUserInfo?.isNewUser == true
                        val prefs   = PrefsManager(context)

                        // Always persist the real Firebase UID locally
                        prefs.uid       = user.uid
                        prefs.userName  = user.displayName ?: prefs.userName
                        prefs.userEmail = user.email ?: prefs.userEmail
                        prefs.photoUrl  = user.photoUrl?.toString() ?: prefs.photoUrl

                        if (isNew) {
                            // New Google user — initialise with defaults
                            prefs.isPremium = false
                            prefs.streak    = 0
                            prefs.leaves    = 5
                            prefs.xp        = 0
                            prefs.level     = 1
                            // Use central repository to create user doc + settings
                            FirestoreProgressRepository.ensureUserDocExists(
                                name     = prefs.userName,
                                username = user.email?.substringBefore("@")
                                    ?.lowercase()
                                    ?.filter { c -> c.isLetterOrDigit() || c == '_' } ?: "",
                                email    = prefs.userEmail
                            )
                            FirestoreProgressRepository.ensureSettingsDocExists()
                            FirestoreProgressRepository.ensureLanguageLessonsExist()
                            googleLoading = false
                            onAuthSuccess(true)
                        } else {
                            // Existing Google user — sync prefs from Firestore then proceed
                            FirestoreProgressRepository.fetchUserData { data ->
                                if (data != null) {
                                    prefs.userName  = (data["name"] as? String) ?: prefs.userName
                                    prefs.userEmail = (data["email"] as? String) ?: prefs.userEmail
                                    prefs.xp        = (data["xp"] as? Long)?.toInt() ?: 0
                                    prefs.level     = (data["level"] as? Long)?.toInt() ?: 1
                                    prefs.streak    = (data["streak"] as? Long)?.toInt() ?: 0
                                    prefs.leaves    = (data["leafBalance"] as? Long)?.toInt() ?: 5
                                    prefs.isPremium = data["isPremium"] as? Boolean ?: false
                                }
                                FirestoreProgressRepository.ensureUserDocExists(
                                    name     = prefs.userName,
                                    username = prefs.username,
                                    email    = prefs.userEmail
                                )
                                googleLoading = false
                                onAuthSuccess(false)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        googleLoading = false
                        errorMessage = friendlyAuthError(e.message ?: "")
                    }
            } else {
                googleLoading = false
                errorMessage = "Google Sign-In failed. Please try again."
            }
        } catch (e: ApiException) {
            googleLoading = false
            errorMessage = "Google Sign-In cancelled or failed (code ${e.statusCode})."
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                text       = stringResource(R.string.app_name),
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text     = if (isLogin) stringResource(R.string.login_subtitle)
                           else stringResource(R.string.signup_subtitle),
                fontSize = 14.sp,
                color    = Color.Gray
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── Google Sign-In Button ─────────────────────────────────────────
            GoogleSignInButton(
                isLoading = googleLoading,
                onClick   = {
                    googleLoading = true
                    errorMessage  = ""
                    googleSignInClient.signOut().addOnCompleteListener {
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // OR divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "  or continue with email  ",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tab switcher: Log In / Sign Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(4.dp)
            ) {
                listOf(true to "Log In", false to "Sign Up")
                    .forEach { (isLoginTab, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isLogin == isLoginTab) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clickable { isLogin = isLoginTab; errorMessage = "" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = label,
                            color      = if (isLogin == isLoginTab) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Signup-only fields
            AnimatedVisibility(
                visible = !isLogin,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                Column {
                    AuthTextField(value = name, onValueChange = { name = it }, label = "Display Name", keyboardType = KeyboardType.Text)
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
            AuthTextField(value = email, onValueChange = { email = it.trim() }, label = "Email Address", keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.height(12.dp))

            // Password
            PasswordField(value = password, onValueChange = { password = it }, label = "Password", visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible })

            // Confirm password
            AnimatedVisibility(visible = !isLogin, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    PasswordField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm Password", visible = confirmVisible, onToggle = { confirmVisible = !confirmVisible })
                }
            }

            // Forgot password
            AnimatedVisibility(visible = isLogin) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        if (email.isBlank()) { errorMessage = "Enter your email address to reset password."; return@TextButton }
                        auth.sendPasswordResetEmail(email.trim())
                            .addOnSuccessListener { errorMessage = "✓ Password reset email sent to $email" }
                            .addOnFailureListener { errorMessage = it.localizedMessage ?: "Reset failed." }
                    }) {
                        Text("Forgot Password?", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                }
            }

            // Error / info banner
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = if (errorMessage.startsWith("✓")) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text     = errorMessage,
                        color    = if (errorMessage.startsWith("✓")) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary CTA button (email auth)
            Button(
                onClick = {
                    errorMessage = ""
                    val validationError = validateInputs(isLogin, name, username, email, password, confirmPassword)
                    if (validationError != null) { errorMessage = validationError; return@Button }

                    isLoading = true

                    if (isLogin) {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val user  = result.user ?: return@addOnSuccessListener
                                val prefs = PrefsManager(context)
                                // Persist real Firebase UID on every email login
                                prefs.uid = user.uid
                                FirestoreProgressRepository.ensureUserDocExists(
                                    name     = prefs.userName,
                                    username = prefs.username,
                                    email    = email
                                )
                                FirestoreProgressRepository.ensureSettingsDocExists()
                                FirestoreProgressRepository.ensureLanguageLessonsExist()
                                isLoading = false
                                onAuthSuccess(false)
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = friendlyAuthError(e.message ?: "")
                            }
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { result ->
                                val user = result.user!!
                                user.sendEmailVerification()
                                val prefs = PrefsManager(context)
                                // Persist real Firebase UID immediately
                                prefs.uid       = user.uid
                                prefs.userName  = name.trim()
                                prefs.username  = username.trim().lowercase()
                                prefs.userEmail = email.trim()
                                prefs.isPremium = false
                                prefs.streak    = 0
                                prefs.leaves    = 5
                                prefs.xp        = 0
                                prefs.level     = 1
                                // Use central repository for Firestore doc creation
                                FirestoreProgressRepository.ensureUserDocExists(
                                    name     = name.trim(),
                                    username = username.trim().lowercase(),
                                    email    = email.trim()
                                )
                                FirestoreProgressRepository.ensureSettingsDocExists()
                                FirestoreProgressRepository.ensureLanguageLessonsExist()
                                isLoading = false
                                onAuthSuccess(true)
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = friendlyAuthError(e.message ?: "")
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape    = RoundedCornerShape(14.dp),
                enabled  = !isLoading && !googleLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text       = if (isLogin) "Log In" else "Create Account",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text     = "By continuing, you agree to our Privacy Policy.",
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ── Google Sign-In Button ─────────────────────────────────────────────────────

@Composable
private fun GoogleSignInButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick  = { if (!isLoading) onClick() },
        enabled  = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF4285F4), strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google "G" icon in brand colors
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "G",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color(0xFF4285F4),
                        textAlign  = TextAlign.Center
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "Continue with Google",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/** Maps Firebase Auth error codes to user-friendly messages. */
private fun friendlyAuthError(raw: String): String = when {
    "network" in raw.lowercase() || "internet" in raw.lowercase() ->
        "No internet connection. Please check your network and try again."
    "password is invalid" in raw.lowercase() || "wrong-password" in raw.lowercase() ->
        "Incorrect password. Please try again or use Forgot Password."
    "no user record" in raw.lowercase() || "user-not-found" in raw.lowercase() ->
        "No account found with this email. Please sign up first."
    "email address is already in use" in raw.lowercase() ->
        "An account with this email already exists. Please log in."
    "email address is badly formatted" in raw.lowercase() ->
        "Please enter a valid email address."
    "password should be at least" in raw.lowercase() ->
        "Password must be at least 6 characters."
    raw.isBlank() -> "An unexpected error occurred. Please try again."
    else -> raw
}

@Composable
fun AuthTextField(value: String, onValueChange: (String) -> Unit, label: String, keyboardType: KeyboardType) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label) },
        modifier      = Modifier.fillMaxWidth(),
        singleLine    = true,
        shape         = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLabelColor  = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun PasswordField(value: String, onValueChange: (String) -> Unit, label: String, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label) },
        modifier            = Modifier.fillMaxWidth(),
        singleLine          = true,
        shape               = RoundedCornerShape(12.dp),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions     = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon        = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF2D6A4F),
            focusedLabelColor  = Color(0xFF2D6A4F)
        )
    )
}

fun validateInputs(
    isLogin: Boolean, name: String, username: String,
    email: String, password: String, confirmPassword: String
): String? {
    if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
        return "Please enter a valid email address."
    if (password.length < 6)
        return "Password must be at least 6 characters long."
    if (!isLogin) {
        if (name.isBlank()) return "Display Name is required."
        if (username.length < 3) return "Username must be at least 3 characters long."
        if (password != confirmPassword) return "Passwords do not match."
    }
    return null
}
