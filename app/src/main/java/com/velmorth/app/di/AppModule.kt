package com.velmorth.app.di

import android.content.Context
import com.velmorth.app.data.DataRepository
import com.velmorth.app.data.DefaultDataRepository
import com.velmorth.app.data.local.dao.*
import com.velmorth.app.data.local.db.VelmorthDatabase
import com.velmorth.app.data.repository.*
import com.velmorth.app.domain.repository.*
import com.velmorth.app.domain.VelmorthSpeaker
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
    @Singleton
    fun provideUserDao(db: VelmorthDatabase): UserDao = db.userDao()

    @Provides
    @Singleton
    fun provideLessonDao(db: VelmorthDatabase): LessonDao = db.lessonDao()

    @Provides
    @Singleton
    fun provideProgressDao(db: VelmorthDatabase): ProgressDao = db.progressDao()

    @Provides
    @Singleton
    fun provideLeafWalletDao(db: VelmorthDatabase): LeafWalletDao = db.leafWalletDao()

    @Provides
    @Singleton
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

    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindDataRepository(impl: DefaultDataRepository): DataRepository
}
