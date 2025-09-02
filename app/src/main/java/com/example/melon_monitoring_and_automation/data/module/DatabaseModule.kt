package com.example.melon_monitoring_and_automation.data.module

import android.content.Context;
import androidx.room.Room;
import com.example.melon_monitoring_and_automation.data.local.AppDatabase;
import com.google.firebase.database.FirebaseDatabase
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideAppDatabase(
//        @ApplicationContext context: Context
//    ): AppDatabase {
//        return Room.databaseBuilder(
//            context.applicationContext,
//            AppDatabase::class.java,
//            "app_database"
//        ).build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideFirebaseDatabase(): FirebaseDatabase {
//        return FirebaseDatabase.getInstance()
//    }
//}
