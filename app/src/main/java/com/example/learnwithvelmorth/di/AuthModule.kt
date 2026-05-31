package com.example.learnwithvelmorth.di

import com.example.learnwithvelmorth.data.auth.AuthRepositoryImpl
import com.example.learnwithvelmorth.data.auth.FirebaseAuthService
import com.example.learnwithvelmorth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module for Authentication Dependencies
 * Provides Firebase Auth and Repository instances
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    
    /**
     * Provide FirebaseAuth instance
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Provide FirebaseAuthService
     */
    @Provides
    @Singleton
    fun provideFirebaseAuthService(
        firebaseAuth: FirebaseAuth
    ): FirebaseAuthService = FirebaseAuthService(firebaseAuth)
    
    /**
     * Provide AuthRepository implementation
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuthService: FirebaseAuthService
    ): AuthRepository = AuthRepositoryImpl(firebaseAuthService)
}
