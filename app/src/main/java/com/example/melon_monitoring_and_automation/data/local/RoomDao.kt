package com.example.melon_monitoring_and_automation.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.UserProfile

// ===================== Entity =====================

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val user_id: String,
    val username: String,
    val full_name: String? = null
)

@Entity(tableName = "greenhouses")
data class GreenhouseEntity(
    @PrimaryKey val id: String,
    val owner_id: String,
    val name: String,
    val location: String
)

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val greenhouse_id: String,
    val name: String,
    val type: String,
    val status: Boolean = false,
    val schedule: String? = null
)

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val greenhouse_id: String,
    val name: String,
    val plant_date: String,
    val variety: String
)

@Entity(tableName = "sensor_readings")
data class SensorReadingEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val greenhouse_id: String,
    val humidity: Float? = null,
    val ph: Float? = null,
    val temperature: Float? = null,
    val tds: Float? = null,
    val recorded_at: String? = null
)


// ===================== Dao =====================

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserProfileEntity)

    @Query("DELETE FROM user_profiles")
    suspend fun clearAll()
}

@Dao
interface GreenhouseDao {
    @Query("SELECT * FROM greenhouses WHERE owner_id = :ownerId")
    suspend fun getGreenhousesByOwnerId(ownerId: String): List<GreenhouseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(greenhouses: List<GreenhouseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(greenhouse: GreenhouseEntity)

    @Query("DELETE FROM greenhouses")
    suspend fun clearAll()
}

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants WHERE greenhouse_id = :greenhouseId")
    suspend fun getPlantsByGreenhouse(greenhouseId: String): List<PlantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PlantEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantEntity)

    @Query("DELETE FROM plants")
    suspend fun clearAll()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE greenhouse_id = :greenhouseId")
    suspend fun getDevicesByGreenhouse(greenhouseId: String): List<DeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(devices: List<DeviceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("UPDATE devices SET status = :status WHERE id = :deviceId")
    suspend fun updateStatus(deviceId: String, status: Boolean)

    @Query("DELETE FROM devices")
    suspend fun clearAll()
}

@Dao
interface SensorReadingDao {
    @Query("SELECT * FROM sensor_readings WHERE greenhouse_id = :greenhouseId ORDER BY recorded_at DESC")
    suspend fun getAllReadings(greenhouseId: String): List<SensorReadingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<SensorReadingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: SensorReadingEntity)

    @Query("DELETE FROM sensor_readings")
    suspend fun clearAll()
}


// ===================== Database =====================

@Database(
    entities = [UserProfileEntity::class, GreenhouseEntity::class, DeviceEntity::class, SensorReadingEntity::class, PlantEntity::class],
    version = 8, // Increment the version to trigger a database migration
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun greenhouseDao(): GreenhouseDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sensorReadingDao(): SensorReadingDao
    abstract fun plantDao(): PlantDao
}

// ===================== Mapper Functions =====================

// User
fun UserProfile.toEntity(): UserProfileEntity =
    UserProfileEntity(user_id, username, full_name)
fun UserProfileEntity.toModel(): UserProfile =
    UserProfile(user_id, username, full_name)

// Greenhouse
fun Greenhouse.toEntity(): GreenhouseEntity =
    GreenhouseEntity(id, owner_id, name, location)
fun GreenhouseEntity.toModel(): Greenhouse =
    Greenhouse(id, owner_id, name, location)

// Device
fun Device.toEntity(): DeviceEntity {
    val entityId = this.id ?: java.util.UUID.randomUUID().toString()
    return DeviceEntity(entityId, greenhouse_id, name, type, status == true)
}
fun DeviceEntity.toModel(): Device =
    Device(id, greenhouse_id, name, type, status)

// Plant
fun Plant.toEntity(): PlantEntity {
    val entityId = this.id ?: java.util.UUID.randomUUID().toString()
    return PlantEntity(entityId, greenhouse_id, name, plant_date, variety)
}
fun PlantEntity.toModel(): Plant =
    Plant(id, greenhouse_id, name, plant_date, variety)

// SensorReading
fun SensorReading.toEntity(): SensorReadingEntity {
    val entityId = this.id ?: java.util.UUID.randomUUID().toString()
    return SensorReadingEntity(entityId, greenhouse_id, humidity, ph, temperature, tds, recorded_at)
}
fun SensorReadingEntity.toModel(): SensorReading =
    SensorReading(id, greenhouse_id, humidity, ph, temperature, tds, recorded_at)

