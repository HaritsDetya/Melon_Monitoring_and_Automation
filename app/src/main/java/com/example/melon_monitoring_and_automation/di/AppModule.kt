package com.example.melon_monitoring_and_automation.di

import android.content.Context
import com.example.melon_monitoring_and_automation.data.network.SupabaseManager
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.data.repository.IoTDeviceRepository
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
    fun provideHydroponicRepository(
        @ApplicationContext context: Context
    ): HydroponicRepository {
        return HydroponicRepository(context)
    }

    @Provides
    @Singleton
    fun provideIoTDeviceRepository(
        @ApplicationContext context: Context
    ): IoTDeviceRepository {
        return IoTDeviceRepository(context)
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseManager.client
    }
}