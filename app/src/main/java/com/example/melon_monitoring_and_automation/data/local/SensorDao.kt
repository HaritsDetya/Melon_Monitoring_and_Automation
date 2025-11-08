package com.example.melon_monitoring_and_automation.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}

@Dao
interface GreenhouseDao {
    @Query("SELECT * FROM greenhouses WHERE owner_id = :ownerId")
    suspend fun getGreenhousesByOwnerId(ownerId: String): List<GreenhouseEntity>

    @Query("SELECT * FROM greenhouses WHERE id = :greenhouseId")
    suspend fun getGreenhouseById(greenhouseId: String): GreenhouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(greenhouses: List<GreenhouseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(greenhouse: GreenhouseEntity)

    @Query("DELETE FROM greenhouses")
    suspend fun clearAll()
}

@Dao
interface ControlDevicesDao {
    @Query("SELECT * FROM control_devices WHERE greenhouse_id = :greenhouseId")
    suspend fun getControlDeviceByGreenhouse(greenhouseId: String): ControlDevicesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<ControlDevicesEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: ControlDevicesEntity)

    @Update
    suspend fun update(device: ControlDevicesEntity)

    @Query("UPDATE control_devices SET fan = :fan, updated_at = :updatedAt WHERE id = :deviceId")
    suspend fun updateFanStatus(deviceId: String, fan: Boolean, updatedAt: String)

    @Query("UPDATE control_devices SET pump = :pump, updated_at = :updatedAt WHERE id = :deviceId")
    suspend fun updatePumpStatus(deviceId: String, pump: Boolean, updatedAt: String)

    @Query("UPDATE control_devices SET auto_mode = :autoMode, updated_at = :updatedAt WHERE id = :deviceId")
    suspend fun updateAutoMode(deviceId: String, autoMode: Boolean, updatedAt: String)

    @Query("DELETE FROM control_devices")
    suspend fun clearAll()
}

@Dao
interface SensorReadingsDao {
    @Query("SELECT * FROM sensor_readings WHERE greenhouse_id = :greenhouseId ORDER BY recorded_at DESC LIMIT 1")
    suspend fun getLatestReading(greenhouseId: String): SensorReadingsEntity?

    @Query("SELECT * FROM sensor_readings WHERE greenhouse_id = :greenhouseId ORDER BY recorded_at DESC")
    suspend fun getAllReadings(greenhouseId: String): List<SensorReadingsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<SensorReadingsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: SensorReadingsEntity)

    @Query("DELETE FROM sensor_readings")
    suspend fun clearAll()
}

@Dao
interface SensorHistoryDao {
    @Query("SELECT * FROM sensor_history WHERE greenhouse_id = :greenhouseId AND sensor_type = :sensorType ORDER BY recorded_at DESC LIMIT :limit")
    suspend fun getSensorHistory(greenhouseId: String, sensorType: String, limit: Int = 100): List<SensorHistoryEntity>

    @Query("SELECT * FROM sensor_history WHERE greenhouse_id = :greenhouseId AND recorded_at >= :startTime ORDER BY recorded_at ASC")
    suspend fun getSensorHistoryByTimeRange(greenhouseId: String, startTime: String): List<SensorHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(history: List<SensorHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: SensorHistoryEntity)

    @Query("DELETE FROM sensor_history")
    suspend fun clearAll()
}

@Dao
interface AutomationSettingsDao {
    @Query("SELECT * FROM automation_settings WHERE greenhouse_id = :greenhouseId")
    suspend fun getAutomationSettings(greenhouseId: String): AutomationSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<AutomationSettingsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: AutomationSettingsEntity)

    @Update
    suspend fun update(setting: AutomationSettingsEntity)

    @Query("UPDATE automation_settings SET max_temperature = :maxTemp, min_temperature = :minTemp, updated_at = :updatedAt WHERE greenhouse_id = :greenhouseId")
    suspend fun updateTemperatureThresholds(greenhouseId: String, maxTemp: Double, minTemp: Double, updatedAt: String)

    @Query("UPDATE automation_settings SET nutrient_droplets = :droplets, updated_at = :updatedAt WHERE greenhouse_id = :greenhouseId")
    suspend fun updateNutrientDroplets(greenhouseId: String, droplets: Int, updatedAt: String)

    @Query("DELETE FROM automation_settings")
    suspend fun clearAll()
}