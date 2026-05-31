package com.example.learnwithvelmorth.data.auth

import com.example.learnwithvelmorth.domain.model.User
import com.example.learnwithvelmorth.domain.repository.AuthRepository
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
        
        result.onSuccess { userId ->
            // TODO: Save user profile to Firestore or local database
            // val user = User(
            //     id = userId,
            //     email = email,
            //     displayName = displayName,
            //     createdAt = System.currentTimeMillis()
            // )
            // Save user...
        }
        
        result.map {
            User(
                id = it,
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
        
        result.map {
            // TODO: Fetch user profile from Firestore or local database
            User(
                id = it,
                email = email,
                displayName = "", // Fetch from storage
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
        // TODO: Fetch full user profile from storage
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
