package com.example.melon_monitoring_and_automation.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.melon_monitoring_and_automation.data.remote.SensorReading
import kotlinx.coroutines.flow.Flow

@androidx.room.Entity(tableName = "sensor_history")
data class SensorHistoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Int = 0,
    val temperature: Double,
    val humidity: Double,
    val ph: Double,
    val ec: Double,
    val waterLevel: String,
    val timestamp: Long
)

@Dao
interface SensorHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSensorReading(reading: SensorHistoryEntity)

    @Query("SELECT * FROM sensor_history ORDER BY timestamp DESC LIMIT 100")
    fun getLatestSensorReading(): Flow<List<SensorHistoryEntity>>

    @Query("SELECT * FROM sensor_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getSensorReadingInDataRange(startTime: Long, endTime: Long): Flow<List<SensorHistoryEntity>>
}

@Database(entities = [SensorHistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sensorHistoryDao(): SensorHistoryDao
}
