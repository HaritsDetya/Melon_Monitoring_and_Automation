package com.example.melon_monitoring_and_automation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserEntity::class,
        GreenhouseEntity::class,
        ControlDevicesEntity::class,
        SensorReadingsEntity::class,
        SensorHistoryEntity::class,
        AutomationSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun greenhouseDao(): GreenhouseDao
    abstract fun controlDevicesDao(): ControlDevicesDao
    abstract fun sensorReadingsDao(): SensorReadingsDao
    abstract fun sensorHistoryDao(): SensorHistoryDao
    abstract fun automationSettingsDao(): AutomationSettingsDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "hydroponic_db"
            ).apply {
                // Untuk development, hapus di production
                fallbackToDestructiveMigration()
                // Tambahkan callback jika perlu
//                addCallback(object : RoomDatabase.Callback() {
//                    override fun onCreate(db: SupportSQLiteDatabase) {
//                        super.onCreate(db)
//                        // Insert initial data jika perlu
//                    }
//                })
            }.build()
        }
    }
}