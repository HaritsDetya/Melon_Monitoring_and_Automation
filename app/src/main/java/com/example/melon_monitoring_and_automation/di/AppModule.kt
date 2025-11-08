package com.example.melon_monitoring_and_automation.di

import android.content.Context
import androidx.room.Room
import com.example.melon_monitoring_and_automation.data.local.AppDatabase
import com.example.melon_monitoring_and_automation.data.local.LocalRepository
import com.example.melon_monitoring_and_automation.data.network.SupabaseManager
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.usecase.DataUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "hydroponic_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLocalRepository(database: AppDatabase): LocalRepository {
        return LocalRepository(database)
    }

    @Provides
    @Singleton
    fun provideHydroponicRepository(): HydroponicRepository {
        return HydroponicRepository()
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseManager.client
    }

    @Provides
    @Singleton
    fun provideDataUseCase(repository: HydroponicRepository): DataUseCase {
        return DataUseCase(repository)
    }
}