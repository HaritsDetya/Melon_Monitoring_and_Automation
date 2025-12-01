package com.example.melon_monitoring_and_automation.domain.model

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Locale

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

// Tambahkan di file domain model Anda
data class ChartConfig(
    val selectedDate: Long = System.currentTimeMillis(),
    val selectedSensorType: SensorType = SensorType.TEMPERATURE,
    val timeRange: TimeRange = TimeRange.HOURS_24,
    val chartType: ChartType = ChartType.LINE,
    // ✅ PERBAIKAN: Tambah custom date range
    val customDateRange: DateRange? = null
)

// ✅ Model untuk custom date range
data class DateRange(
    val startDate: Long, // Timestamp in milliseconds
    val endDate: Long,   // Timestamp in milliseconds
    val label: String    // Display label e.g., "1-7 Dec 2024"
) {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun createWeeklyRange(startDate: Long, endDate: Long): DateRange {
            val dateFormat = SimpleDateFormat("d MMM", Locale.getDefault())
            val startStr = dateFormat.format(startDate)
            val endStr = dateFormat.format(endDate)

            // Tambahkan tahun jika berbeda
            val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
            val startYear = yearFormat.format(startDate)
            val endYear = yearFormat.format(endDate)

            val yearSuffix = if (startYear != endYear) " $startYear" else ""

            return DateRange(
                startDate = startDate,
                endDate = endDate,
                label = "$startStr - $endStr$yearSuffix"
            )
        }

        // ✅ PERBAIKAN: Method untuk membuat range dari minggu tertentu dalam bulan
        @RequiresApi(Build.VERSION_CODES.O)
        fun createFromWeekInMonth(monthYear: MonthYear, weekIndex: Int): DateRange {
            val calendar = Calendar.getInstance()
            calendar.set(monthYear.year, monthYear.monthValue - 1, 1)

            // Cari hari pertama minggu tersebut
            val firstDayOfWeek = (weekIndex * 7) + 1
            calendar.set(Calendar.DAY_OF_MONTH, firstDayOfWeek.coerceAtLeast(1))

            val startDate = calendar.timeInMillis

            // Akhir minggu (6 hari setelah start)
            calendar.add(Calendar.DAY_OF_MONTH, 6)
            val lastDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, minOf(calendar.get(Calendar.DAY_OF_MONTH), lastDayOfMonth))

            val endDate = calendar.timeInMillis

            return createWeeklyRange(startDate, endDate)
        }
    }

    // ✅ PERBAIKAN: Helper method untuk mendapatkan durasi dalam hari
    fun getDurationInDays(): Long {
        return (endDate - startDate) / (24 * 60 * 60 * 1000) + 1 // +1 untuk inclusive
    }
}

// ✅ Extended TimeRange untuk include custom
enum class TimeRange {
    HOURS_24, DAYS_7, DAYS_30, CUSTOM
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

// ✅ Model untuk month-year selection
data class MonthYear(
    val monthValue: Int, // 1-12
    val year: Int
) {
    val displayName: String
        get() {
            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            return "${monthNames[monthValue - 1]} $year"
        }
}

// ============ IoT DEVICE MODELS (NEW - FOR SUPABASE INTEGRATION) ============

@Serializable
data class IoTDevice(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    @SerialName("serial_number") val serialNumber: String,
    @SerialName("device_name") val deviceName: String?,
    @SerialName("device_type") val deviceType: DeviceType = DeviceType.HYDROPONIC_SENSOR,
    @SerialName("greenhouse_id") val greenhouseId: String? = null,
    @SerialName("pairing_code") val pairingCode: String,
    @SerialName("is_paired") val isPaired: Boolean = false,
    @SerialName("paired_at") val pairedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_seen") val lastSeen: String? = null,
    @SerialName("firmware_version") val firmwareVersion: String? = null,
    @SerialName("encryption_key") val encryptionKey: String? = null
)

@Serializable
enum class DeviceType {
    @SerialName("HYDROPONIC_SENSOR") HYDROPONIC_SENSOR,
    @SerialName("TEMPERATURE_SENSOR") TEMPERATURE_SENSOR,
    @SerialName("HUMIDITY_SENSOR") HUMIDITY_SENSOR,
    @SerialName("PH_SENSOR") PH_SENSOR,
    @SerialName("TDS_SENSOR") TDS_SENSOR,
    @SerialName("WATER_TEMP_SENSOR") WATER_TEMP_SENSOR,
    @SerialName("CONTROLLER_DEVICE") CONTROLLER_DEVICE
}

@Serializable
data class DevicePairingRequest(
    @SerialName("device_id") val deviceId: String,
    @SerialName("pairing_code") val pairingCode: String,
    @SerialName("greenhouse_id") val greenhouseId: String
)

@Serializable
data class DeviceStatusUpdate(
    @SerialName("last_seen") val lastSeen: String,
    @SerialName("firmware_version") val firmwareVersion: String? = null
)

@Serializable
data class DeviceTelemetryData(
    @SerialName("device_id") val deviceId: String,
    @SerialName("greenhouse_id") val greenhouseId: String,
    val temperature: Double? = null,
    val humidity: Double? = null,
    @SerialName("water_temp") val waterTemp: Double? = null,
    val ph: Double? = null,
    val tds: Double? = null,
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("signal_strength") val signalStrength: Int? = null,
    @SerialName("recorded_at") val recordedAt: String
)

@Serializable
data class DeviceCommand(
    val id: String,
    @SerialName("device_id") val deviceId: String,
    val command: String,
    val payload: String? = null,
    @SerialName("is_executed") val isExecuted: Boolean = false,
    @SerialName("executed_at") val executedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SendDeviceCommand(
    @SerialName("device_id") val deviceId: String,
    val command: String,
    val payload: String? = null
)

// Response untuk pairing device
@Serializable
data class DevicePairingResponse(
    val success: Boolean,
    val message: String,
    val device: IoTDevice? = null
)

// Response untuk device telemetry
@Serializable
data class DeviceTelemetryResponse(
    val device: IoTDevice,
    val telemetry: DeviceTelemetryData? = null,
    @SerialName("last_reading") val lastReading: SensorReadings? = null
)

// Untuk list devices dengan pagination
@Serializable
data class DevicesListResponse(
    val devices: List<IoTDevice>,
    val count: Int,
    @SerialName("total_count") val totalCount: Int
)
