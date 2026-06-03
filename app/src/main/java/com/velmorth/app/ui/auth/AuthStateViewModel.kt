package com.velmorth.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Sealed class representing all possible auth states.
 * Equivalent to Flutter's StreamBuilder ConnectionState + snapshot logic.
 */
sealed class AuthState {
    /** Auth check in progress — show splash/loading screen. */
    object Loading : AuthState()

    /** User is signed in. Holds the current FirebaseUser. */
    data class Authenticated(val user: FirebaseUser) : AuthState()

    /** No signed-in user — route to LoginActivity. */
    object Unauthenticated : AuthState()
}

/**
 * Provides a reactive [StateFlow] of [AuthState] driven by [FirebaseAuth.addAuthStateListener].
 *
 * Flutter equivalent:
 * ```dart
 * StreamBuilder(
 *   stream: AuthService().authStateChanges,
 *   builder: (context, snapshot) {
 *     if (snapshot.connectionState == ConnectionState.waiting) return SplashScreen();
 *     if (snapshot.hasData) return MainScreen();
 *     return LoginScreen();
 *   },
 * )
 * ```
 *
 * Usage in an Activity/Fragment:
 * ```kotlin
 * viewModel.authState.collectLatest { state ->
 *     when (state) {
 *         is AuthState.Loading        -> showSplash()
 *         is AuthState.Authenticated  -> navigateToMain()
 *         is AuthState.Unauthenticated -> navigateToLogin()
 *     }
 * }
 * ```
 */
@HiltViewModel
class AuthStateViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    /**
     * Kotlin Flow that mirrors Firebase auth state changes.
     * Uses [callbackFlow] to bridge the Firebase listener callback into a cold Flow,
     * then converts it to a hot [StateFlow] so all collectors share the same stream.
     */
    val authState: StateFlow<AuthState> = authStateChanges(firebaseAuth)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AuthState.Loading
        )

    /**
     * Signs the current user out. AuthState will automatically emit [AuthState.Unauthenticated].
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
}

/**
 * Converts [FirebaseAuth.addAuthStateListener] into a Kotlin [Flow].
 * The flow emits whenever the auth state changes and cleans up the listener
 * automatically when the collector is cancelled.
 */
private fun authStateChanges(auth: FirebaseAuth): Flow<AuthState> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        trySend(
            if (user != null) AuthState.Authenticated(user)
            else AuthState.Unauthenticated
        )
    }
    auth.addAuthStateListener(listener)
    // Remove listener when the collector scope is cancelled (Activity destroyed, etc.)
    awaitClose { auth.removeAuthStateListener(listener) }
}
