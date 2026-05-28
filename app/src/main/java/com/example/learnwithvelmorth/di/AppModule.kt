package com.example.learnwithvelmorth.di

import android.content.Context
import com.example.learnwithvelmorth.data.local.dao.*
import com.example.learnwithvelmorth.data.local.db.VelmorthDatabase
import com.example.learnwithvelmorth.data.repository.*
import com.example.learnwithvelmorth.domain.repository.*
import com.example.learnwithvelmorth.domain.VelmorthSpeaker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VelmorthDatabase =
        VelmorthDatabase.getInstance(context)

    @Provides
    fun provideUserDao(db: VelmorthDatabase): UserDao = db.userDao()

    @Provides
    fun provideLessonDao(db: VelmorthDatabase): LessonDao = db.lessonDao()

    @Provides
    fun provideProgressDao(db: VelmorthDatabase): ProgressDao = db.progressDao()

    @Provides
    fun provideLeafWalletDao(db: VelmorthDatabase): LeafWalletDao = db.leafWalletDao()

    @Provides
    fun provideDialogueDao(db: VelmorthDatabase): DialogueDao = db.dialogueDao()

    @Provides
    @Singleton
    fun provideVelmorthSpeaker(@ApplicationContext context: Context): VelmorthSpeaker =
        VelmorthSpeaker(context)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLessonRepository(impl: LessonRepositoryImpl): LessonRepository

    @Binds
    @Singleton
    abstract fun bindLeafWalletRepository(impl: LeafWalletRepositoryImpl): LeafWalletRepository
}
