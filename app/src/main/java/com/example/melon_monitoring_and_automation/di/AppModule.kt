package com.example.melon_monitoring_and_automation.di

//import android.content.Context
//import androidx.room.Room
//import com.example.melon_monitoring_and_automation.data.local.AppDatabase
//import com.example.melon_monitoring_and_automation.data.local.DeviceDao
//import com.example.melon_monitoring_and_automation.data.local.GreenhouseDao
//import com.example.melon_monitoring_and_automation.data.local.PlantDao
//import com.example.melon_monitoring_and_automation.data.local.SensorHistoryDao
//import com.example.melon_monitoring_and_automation.data.local.UserDao
//import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
//import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
//import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepositoryImpl
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import dagger.Module
//import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton
//
//@Module
//@InstallIn(SingletonComponent::class)
//object AppModule {
//
//    @Provides
//    @Singleton
//    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
//
//    @Provides
//    @Singleton
//    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()
//
//    @Provides
//    @Singleton
//    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
//        return Room.databaseBuilder(
//            context,
//            AppDatabase::class.java,
//            "hydroponic_database"
//        )
//            .fallbackToDestructiveMigration()
//            .build()
//    }
//
//    @Provides
//    @Singleton
//    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
//
//    @Provides
//    @Singleton
//    fun provideGreenhouseDao(db: AppDatabase): GreenhouseDao = db.greenhouseDao()
//
//    @Provides
//    @Singleton
//    fun provideDeviceDao(db: AppDatabase): DeviceDao = db.deviceDao()
//
//    @Provides
//    @Singleton
//    fun providePlantDao(db: AppDatabase): PlantDao = db.plantDao()
//
//    @Provides
//    @Singleton
//    fun provideSensorHistoryDao(db: AppDatabase): SensorHistoryDao = db.sensorHistoryDao()
//
//    @Provides
//    @Singleton
//    fun provideDatabaseReference(): DatabaseReference {
//        return FirebaseDatabase.getInstance().reference
//    }
//
//    @Provides
//    @Singleton
//    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
//
//    @Provides
//    @Singleton
//    fun provideHydroponicRepository(
//        firebaseDataSource: FirebaseDataSource,
//        userDao: UserDao,
//        greenhouseDao: GreenhouseDao,
//        deviceDao: DeviceDao,
//        plantDao: PlantDao,
//        sensorHistoryDao: SensorHistoryDao,
//        dispatcherProvider: DispatcherProvider
//    ): HydroponicRepository {
//        return HydroponicRepositoryImpl(
//            firebaseDataSource,
//            userDao,
//            greenhouseDao,
//            sensorHistoryDao,
//            deviceDao,
//            plantDao,
//            dispatcherProvider
//        )
//    }
//}
