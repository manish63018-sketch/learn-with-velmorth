package com.velmorth.app.data.auth

import com.velmorth.app.domain.model.User
import com.velmorth.app.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication Repository Implementation
 * Implements authentication operations using Firebase
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService
) : AuthRepository {
    
    override suspend fun registerUser(
        email: String,
        password: String,
        displayName: String
    ): Result<User> = try {
        // Register with Firebase
        val result = firebaseAuthService.registerWithEmail(email, password)
        
        result.map { userId ->
            User(
                id = userId,
                email = email,
                displayName = displayName,
                createdAt = System.currentTimeMillis()
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun loginUser(
        email: String,
        password: String
    ): Result<User> = try {
        val result = firebaseAuthService.loginWithEmail(email, password)
        
        result.map { userId ->
            User(
                id = userId,
                email = email,
                displayName = "", // Fetch from storage in production
                createdAt = System.currentTimeMillis()
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    override suspend fun resetPassword(email: String): Result<Unit> =
        firebaseAuthService.sendPasswordReset(email)
    
    override fun signOut() {
        firebaseAuthService.signOut()
    }
    
    override suspend fun getCurrentUser(): User? {
        val currentUser = firebaseAuthService.getCurrentUser() ?: return null
        return User(
            id = currentUser.uid,
            email = currentUser.email ?: "",
            displayName = currentUser.displayName ?: "",
            createdAt = System.currentTimeMillis()
        )
    }
    
    override fun isUserAuthenticated(): Boolean =
        firebaseAuthService.isUserAuthenticated()
    
    override fun getCurrentUserId(): String? =
        firebaseAuthService.getCurrentUserId()
}
