package com.velmorth.app.domain.repository

import com.velmorth.app.domain.model.User

/**
 * Authentication Repository Interface
 * Defines authentication operations contract
 */
interface AuthRepository {
    
    /**
     * Register a new user
     * @param email User email
     * @param password User password
     * @param displayName User display name
     * @return Result with registered user on success
     */
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String
    ): Result<User>
    
    /**
     * Login existing user
     * @param email User email
     * @param password User password
     * @return Result with authenticated user on success
     */
    suspend fun loginUser(
        email: String,
        password: String
    ): Result<User>
    
    /**
     * Send password reset email
     * @param email User email
     * @return Result indicating success or failure
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Sign out current user
     */
    fun signOut()
    
    /**
     * Get current authenticated user
     * @return Current user or null if not authenticated
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Check if user is authenticated
     * @return true if user is authenticated
     */
    fun isUserAuthenticated(): Boolean
    
    /**
     * Get current user ID
     * @return User UID or null
     */
    fun getCurrentUserId(): String?
}
