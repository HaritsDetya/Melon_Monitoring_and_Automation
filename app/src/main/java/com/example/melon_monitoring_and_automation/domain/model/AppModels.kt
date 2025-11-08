package com.example.melon_monitoring_and_automation.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.time.LocalDate

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    @SerialName("phone_number") val phoneNumber: String?,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class Greenhouse(
    val id: String,
    val name: String,
    val location: String,
    @SerialName("owner_id") val ownerId: String,
    val description: String?,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ControlDevices(
    val id: String,
    @SerialName("greenhouse_id") val greenhouseId: String,
    val fan: Boolean,
    val pump: Boolean,
    @SerialName("auto_mode") val autoMode: Boolean,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class SensorReadings(
    val id: String,
    @SerialName("greenhouse_id") val greenhouseId: String,
    val temperature: Double?,
    val humidity: Double?,
    @SerialName("water_temp") val waterTemp: Double?,
    val ph: Double?,
    val tds: Double?,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class SensorHistory(
    val id: String,
    @SerialName("greenhouse_id") val greenhouseId: String,
    @SerialName("sensor_type") val sensorType: SensorType,
    val value: Double,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
enum class SensorType {
    @SerialName("TEMPERATURE") TEMPERATURE,
    @SerialName("HUMIDITY") HUMIDITY,
    @SerialName("WATER_TEMPERATURE") WATER_TEMPERATURE,
    @SerialName("PH") PH,
    @SerialName("TDS") TDS
}

// Data model untuk settings automation
@Serializable
data class AutomationSettings(
    val id: String,

    @SerialName("greenhouse_id")
    val greenhouseId: String,

    @SerialName("max_temperature")
    val maxTemperature: Double,

    @SerialName("min_temperature")
    val minTemperature: Double,

    @SerialName("nutrient_droplets")
    val nutrientDroplets: Int,

    @SerialName("updated_at")
    val updatedAt: String
)

// Data model untuk chart data
@Serializable
data class ChartData(
    val timestamp: String,
    val value: Double,
    val sensorType: SensorType
)

// Data model untuk chart configuration
data class ChartConfig(
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedSensorType: SensorType = SensorType.TEMPERATURE,
    val timeRange: TimeRange = TimeRange.HOURS_24,
    val chartType: ChartType = ChartType.LINE
)

enum class TimeRange {
    HOURS_24, DAYS_7, DAYS_30
}

enum class ChartType {
    LINE, BAR
}

// Data point untuk chart
data class ChartDataPoint(
    val x: Float, // Time in hours or days
    val y: Float, // Sensor value
    val label: String, // Time label
    val rawValue: Double // Original value
)