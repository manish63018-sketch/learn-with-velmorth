package com.velmorth.app.di

import android.content.Context
import com.velmorth.app.data.local.LessonLoader
import com.velmorth.app.data.local.PrefsManager
import com.velmorth.app.data.repository.CategoryRepository
import com.velmorth.app.data.repository.LessonRepository
import com.velmorth.app.data.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt dependency injection module for the app.
 * Provides application-scoped singletons for core repositories and managers.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePrefsManager(@ApplicationContext context: Context): PrefsManager {
        return PrefsManager(context)
    }

    @Provides
    @Singleton
    fun provideLessonLoader(@ApplicationContext context: Context): LessonLoader {
        return LessonLoader(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(@ApplicationContext context: Context): UserRepository {
        return UserRepository(context)
    }

    @Provides
    @Singleton
    fun provideLessonRepository(@ApplicationContext context: Context): LessonRepository {
        return LessonRepository(context)
    }

    @Provides
    @Singleton
    fun provideCategoryRepository(@ApplicationContext context: Context): CategoryRepository {
        return CategoryRepository(context)
    }
}
