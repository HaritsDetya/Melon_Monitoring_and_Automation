package com.example.melon_monitoring_and_automation.di

import android.content.Context
import androidx.room.Room
import com.example.melon_monitoring_and_automation.data.local.AppDatabase
import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.usecase.GetHistoricalHydroponicDataUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.GetRealtimeHydroponicDataUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetAutomaticSettingUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetDeviceControlUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance()
    }

    @Provides
    @Singleton
    fun provideAppData(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "hydroponic_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideFirebaseDataSource(database: FirebaseDatabase): FirebaseDataSource {
        return FirebaseDataSource(database)
    }

    @Provides
    @Singleton
    fun provideHydroponicRepository(
        firebaseDataSource: FirebaseDataSource,
        localDatabase: AppDatabase
    ): HydroponicRepository {
        return HydroponicRepository(firebaseDataSource, localDatabase)
    }

    @Provides
    fun provideGetRealtimeHydroponicDataUseCase(repository: HydroponicRepository): GetRealtimeHydroponicDataUseCase {
        return GetRealtimeHydroponicDataUseCase(repository)
    }

    @Provides
    fun provideGetHistoricalHydroponicDataUseCase(repository: HydroponicRepository): GetHistoricalHydroponicDataUseCase {
        return GetHistoricalHydroponicDataUseCase(repository)
    }

    @Provides
    fun provideSetDeviceControlUseCase(repository: HydroponicRepository): SetDeviceControlUseCase {
        return SetDeviceControlUseCase(repository)
    }

    @Provides
    fun provideSetAutomaticSettingUseCase(repository: HydroponicRepository): SetAutomaticSettingUseCase {
        return SetAutomaticSettingUseCase(repository)
    }
}