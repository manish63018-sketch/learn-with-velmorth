package com.velmorth.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Authentication Service
 * Handles user registration, login, and authentication state
 */
@Singleton
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    
    /**
     * Get current authenticated user
     * @return Current FirebaseUser or null if not authenticated
     */
    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    
    /**
     * Register a new user with email and password
     * @param email User email
     * @param password User password
     * @return Result with user UID on success, error message on failure
     */
    suspend fun registerWithEmail(
        email: String,
        password: String
    ): Result<String> = try {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: throw Exception("User ID not available")
        Result.success(userId)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Login user with email and password
     * @param email User email
     * @param password User password
     * @return Result with user UID on success, error message on failure
     */
    suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<String> = try {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: throw Exception("User ID not available")
        Result.success(userId)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Send password reset email
     * @param email User email
     * @return Result indicating success or failure
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> = try {
        firebaseAuth.sendPasswordResetEmail(email).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
    
    /**
     * Check if user is authenticated
     * @return true if user is authenticated, false otherwise
     */
    fun isUserAuthenticated(): Boolean = firebaseAuth.currentUser != null
    
    /**
     * Get current user ID
     * @return User UID or null if not authenticated
     */
    fun getCurrentUserId(): String? = firebaseAuth.currentUser?.uid
}
