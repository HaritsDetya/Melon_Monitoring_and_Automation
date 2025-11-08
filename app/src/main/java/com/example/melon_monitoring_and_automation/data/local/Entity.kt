package com.example.melon_monitoring_and_automation.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import com.example.melon_monitoring_and_automation.domain.model.SensorHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.User
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val phone_number: String? = null,
    val created_at: String
)

@Entity(tableName = "greenhouses")
data class GreenhouseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val location: String,
    val owner_id: String,
    val description: String? = null,
    val created_at: String
)

@Entity(tableName = "control_devices")
data class ControlDevicesEntity(
    @PrimaryKey val id: String,
    val greenhouse_id: String,
    val fan: Boolean = false,
    val pump: Boolean = false,
    val auto_mode: Boolean = false,
    val updated_at: String
)

@Entity(tableName = "sensor_readings")
data class SensorReadingsEntity(
    @PrimaryKey val id: String,
    val greenhouse_id: String,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val water_temp: Double? = null,
    val ph: Double? = null,
    val tds: Double? = null,
    val recorded_at: String
)

@Entity(tableName = "sensor_history")
data class SensorHistoryEntity(
    @PrimaryKey val id: String,
    val greenhouse_id: String,
    val sensor_type: String, // Store as String for SensorType enum
    val value: Double,
    val recorded_at: String
)

@Entity(tableName = "automation_settings")
data class AutomationSettingsEntity(
    @PrimaryKey val id: String,
    val greenhouse_id: String,
    val max_temperature: Double = 38.0,
    val min_temperature: Double = 25.0,
    val nutrient_droplets: Int = 10,
    val updated_at: String
)

// Extension functions untuk konversi antara Entity dan Domain Model

// User
fun User.toEntity(): UserEntity =
    UserEntity(id, username, email, phoneNumber, createdAt)

fun UserEntity.toModel(): User =
    User(id, username, email, phone_number, created_at)

// Greenhouse
fun Greenhouse.toEntity(): GreenhouseEntity =
    GreenhouseEntity(id, name, location, ownerId, description, createdAt)

fun GreenhouseEntity.toModel(): Greenhouse =
    Greenhouse(id, name, location, owner_id, description, created_at)

// ControlDevices
fun ControlDevices.toEntity(): ControlDevicesEntity =
    ControlDevicesEntity(id, greenhouseId, fan, pump, autoMode, updatedAt)

fun ControlDevicesEntity.toModel(): ControlDevices =
    ControlDevices(id, greenhouse_id, fan, pump, auto_mode, updated_at)

// SensorReadings
fun SensorReadings.toEntity(): SensorReadingsEntity =
    SensorReadingsEntity(id, greenhouseId, temperature, humidity, waterTemp, ph, tds, recordedAt)

fun SensorReadingsEntity.toModel(): SensorReadings =
    SensorReadings(id, greenhouse_id, temperature, humidity, water_temp, ph, tds, recorded_at)

// SensorHistory
fun SensorHistory.toEntity(): SensorHistoryEntity =
    SensorHistoryEntity(id, greenhouseId, sensorType.name, value, recordedAt)

fun SensorHistoryEntity.toModel(): SensorHistory {
    val sensorType = try {
        SensorType.valueOf(sensor_type)
    } catch (e: Exception) {
        SensorType.TEMPERATURE // default fallback
    }
    return SensorHistory(id, greenhouse_id, sensorType, value, recorded_at)
}

// AutomationSettings
fun AutomationSettings.toEntity(): AutomationSettingsEntity =
    AutomationSettingsEntity(id, greenhouseId, maxTemperature, minTemperature, nutrientDroplets, updatedAt)

fun AutomationSettingsEntity.toModel(): AutomationSettings =
    AutomationSettings(id, greenhouse_id, max_temperature, min_temperature, nutrient_droplets, updated_at)