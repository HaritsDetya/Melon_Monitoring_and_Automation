package com.example.melon_monitoring_and_automation.di

import android.content.Context
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.ui.wrapper.SharedViewModelWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedViewModelModule {
    @Provides
    @Singleton
    fun provideSharedViewModelWrapper(
        @ApplicationContext context: Context,
        sharedViewModel: SharedViewModel // Hilt akan menyediakan ini
    ): SharedViewModelWrapper {
        return SharedViewModelWrapper(sharedViewModel)
    }
}