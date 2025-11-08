package com.example.melon_monitoring_and_automation.data.local

import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import com.example.melon_monitoring_and_automation.domain.model.SensorHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalRepository(
    private val db: AppDatabase
) {

    // User operations
    suspend fun saveUser(user: User) {
        db.userDao().insert(user.toEntity())
    }

    suspend fun getUser(userId: String): User? {
        return db.userDao().getUserById(userId)?.toModel()
    }

    suspend fun getUserByEmail(email: String): User? {
        return db.userDao().getUserByEmail(email)?.toModel()
    }

    // Greenhouse operations
    suspend fun saveGreenhouses(greenhouses: List<Greenhouse>) {
        db.greenhouseDao().insertAll(greenhouses.map { it.toEntity() })
    }

    suspend fun getGreenhousesByOwner(ownerId: String): List<Greenhouse> {
        return db.greenhouseDao().getGreenhousesByOwnerId(ownerId).map { it.toModel() }
    }

    suspend fun getGreenhouseById(greenhouseId: String): Greenhouse? {
        return db.greenhouseDao().getGreenhouseById(greenhouseId)?.toModel()
    }

    // Control Devices operations
    suspend fun saveControlDevice(device: ControlDevices) {
        db.controlDevicesDao().insert(device.toEntity())
    }

    suspend fun getControlDevice(greenhouseId: String): ControlDevices? {
        return db.controlDevicesDao().getControlDeviceByGreenhouse(greenhouseId)?.toModel()
    }

    suspend fun updateFanStatus(deviceId: String, fan: Boolean, updatedAt: String) {
        db.controlDevicesDao().updateFanStatus(deviceId, fan, updatedAt)
    }

    suspend fun updatePumpStatus(deviceId: String, pump: Boolean, updatedAt: String) {
        db.controlDevicesDao().updatePumpStatus(deviceId, pump, updatedAt)
    }

    suspend fun updateAutoMode(deviceId: String, autoMode: Boolean, updatedAt: String) {
        db.controlDevicesDao().updateAutoMode(deviceId, autoMode, updatedAt)
    }

    // Sensor Readings operations
    suspend fun saveSensorReading(reading: SensorReadings) {
        db.sensorReadingsDao().insert(reading.toEntity())
    }

    suspend fun getLatestSensorReading(greenhouseId: String): SensorReadings? {
        return db.sensorReadingsDao().getLatestReading(greenhouseId)?.toModel()
    }

    // Sensor History operations
    suspend fun saveSensorHistory(history: List<SensorHistory>) {
        db.sensorHistoryDao().insertAll(history.map { it.toEntity() })
    }

    suspend fun getSensorHistory(greenhouseId: String, sensorType: SensorType, limit: Int = 100): List<SensorHistory> {
        return db.sensorHistoryDao().getSensorHistory(greenhouseId, sensorType.name, limit).map { it.toModel() }
    }

    // Automation Settings operations
    suspend fun saveAutomationSettings(settings: AutomationSettings) {
        db.automationSettingsDao().insert(settings.toEntity())
    }

    suspend fun getAutomationSettings(greenhouseId: String): AutomationSettings? {
        return db.automationSettingsDao().getAutomationSettings(greenhouseId)?.toModel()
    }

    suspend fun updateTemperatureThresholds(greenhouseId: String, maxTemp: Double, minTemp: Double, updatedAt: String) {
        db.automationSettingsDao().updateTemperatureThresholds(greenhouseId, maxTemp, minTemp, updatedAt)
    }

    suspend fun updateNutrientDroplets(greenhouseId: String, droplets: Int, updatedAt: String) {
        db.automationSettingsDao().updateNutrientDroplets(greenhouseId, droplets, updatedAt)
    }

    // Clear all data (for logout)
    suspend fun clearAllData() {
        db.userDao().clearAll()
        db.greenhouseDao().clearAll()
        db.controlDevicesDao().clearAll()
        db.sensorReadingsDao().clearAll()
        db.sensorHistoryDao().clearAll()
        db.automationSettingsDao().clearAll()
    }
}