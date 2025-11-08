package com.example.melon_monitoring_and_automation.di

import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.usecase.DataUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object UseCaseModule {
//
//    @Provides
//    @Singleton
//    fun provideDataUseCase(repository: HydroponicRepository): DataUseCase {
//        return DataUseCase(repository)
//    }
//}
