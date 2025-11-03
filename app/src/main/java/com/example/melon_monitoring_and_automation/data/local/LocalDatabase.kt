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

//@Entity(tableName = "users")
//data class UserEntity(
//    @PrimaryKey val uid: String,
//    val name: String,
//    val email: String
//)
//
//@Entity(tableName = "greenhouses")
//data class GreenhouseEntity(
//    @PrimaryKey val id: String,
//    val name: String,
//    val location: String
//)
//
//@Entity(tableName = "devices")
//data class DeviceEntity(
//    @PrimaryKey val id: String,
//    val greenhouseId: String,
//    val name: String,
//    val type: String,
//    val status: Boolean = false
//)
//
//@Entity(tableName = "plants")
//data class PlantEntity(
//    @PrimaryKey val id: String,
//    val greenhouseId: String,
//    val name: String,
//    val type: String,
//    val planted_at: String
//)
//
//@Entity(tableName = "sensor_history")
//data class SensorHistoryEntity(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val greenhouseId: String,
//    val timestamp: Long,
//    val temperature: Double,
//    val humidity: Double,
//    val ph: Double,
//    val ec: Double,
//    val light: Double,
//    val recorded_at: Long?
//)
//
//@Dao
//interface UserDao {
//    @Query("SELECT * FROM users LIMIT 1")
//    suspend fun getUser(): UserEntity?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(user: UserEntity)
//
//    @Query("DELETE FROM users")
//    suspend fun clearAll()
//}
//
//@Dao
//interface GreenhouseDao {
//    @Query("SELECT * FROM greenhouses")
//    suspend fun getAllGreenhouses(): List<GreenhouseEntity>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(greenhouses: List<GreenhouseEntity>)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(greenhouse: GreenhouseEntity)
//
//    @Query("DELETE FROM greenhouses")
//    suspend fun clearAll()
//}
//
//@Dao
//interface PlantDao {
//    @Query("SELECT * FROM plants WHERE greenhouseId = :greenhouseId")
//    suspend fun getPlantsByGreenhouse(greenhouseId: String): List<PlantEntity>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(plants: List<PlantEntity>)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(plant: PlantEntity)
//
//    @Query("DELETE FROM plants")
//    suspend fun clearAll()
//}
//
//@Dao
//interface DeviceDao {
//    @Query("SELECT * FROM devices WHERE greenhouseId = :greenhouseId")
//    suspend fun getDevicesByGreenhouse(greenhouseId: String): List<DeviceEntity>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(devices: List<DeviceEntity>)
//
//    @Query("UPDATE devices SET status = :status WHERE id = :deviceId")
//    suspend fun updateStatus(deviceId: String, status: Boolean)
//
//    @Query("SELECT status FROM devices WHERE id = :deviceId LIMIT 1")
//    suspend fun getDeviceStatus(deviceId: String): Boolean?
//
//    @Query("DELETE FROM devices")
//    suspend fun clearAll()
//}
//
//@Dao
//interface SensorHistoryDao {
//    @Query("SELECT * FROM sensor_history WHERE greenhouseId = :greenhouseId AND timestamp BETWEEN :startDate AND :endDate")
//    suspend fun getSensorReadingsBetween(greenhouseId: String, startDate: Long, endDate: Long): List<SensorHistoryEntity>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertSensorReading(reading: SensorHistoryEntity)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(readings: List<SensorHistoryEntity>)
//
//    @Query("DELETE FROM sensor_history")
//    suspend fun clearAll()
//}
//
//// Database
//
//@Database(
//    entities = [UserEntity::class, GreenhouseEntity::class, DeviceEntity::class, SensorHistoryEntity::class, PlantEntity::class],
//    version = 7,
//    exportSchema = false
//)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun userDao(): UserDao
//    abstract fun greenhouseDao(): GreenhouseDao
//    abstract fun deviceDao(): DeviceDao
//    abstract fun sensorHistoryDao(): SensorHistoryDao
//    abstract fun plantDao(): PlantDao
//}
//
//// User
//fun User.toEntity(): UserEntity = UserEntity(uid, username, email ?: "")
//fun UserEntity.toModel(): User = User(uid, name, email, emptyMap())
//
//// Greenhouse
//fun Greenhouse.toEntity(): GreenhouseEntity = GreenhouseEntity(id, name, location)
//fun GreenhouseEntity.toModel(): Greenhouse = Greenhouse(id, name, location, "")
//
//// Device
//fun Device.toEntity(greenhouseId: String, status: Boolean): DeviceEntity = DeviceEntity(
//    id = id,
//    greenhouseId = greenhouseId,
//    name = name,
//    type = type,
//    status = status
//)
//
//fun DeviceEntity.toModel(): Device = Device(
//    id = id,
//    greenhouseId = greenhouseId,
//    name = name,
//    type = type,
//    status = status,
//    config = null
//)
//
//fun Plant.toEntity(greenhouseId: String): PlantEntity = PlantEntity(
//    id = id,
//    greenhouseId = greenhouseId,
//    name = name,
//    type = type,
//    planted_at = planted_at
//)
//
//fun PlantEntity.toModel(): Plant = Plant(
//    id = id,
//    greenhouseId = greenhouseId,
//    name = name,
//    type = type,
//    planted_at = planted_at
//)
//
//// SensorReading
//fun SensorReading.toEntity(greenhouseId: String, timestamp: Long): SensorHistoryEntity =
//    SensorHistoryEntity(
//        greenhouseId = greenhouseId,
//        timestamp = timestamp,
//        temperature = temperature,
//        humidity = humidity,
//        ph = ph,
//        ec = ec,
//        light = light,
//        recorded_at = recorded_at
//    )
//
//fun SensorHistoryEntity.toDomain(): SensorReading =
//    SensorReading(
//        temperature = temperature,
//        humidity = humidity,
//        ph = ph,
//        ec = ec,
//        light = light
//    )
