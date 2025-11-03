package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.GreenhouseMember
import com.example.melon_monitoring_and_automation.domain.model.NewGreenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.PlantHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow


interface HydroponicRepository {
    // ===================== USER =====================
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun saveUserProfile(userProfile: UserProfile)

    // ===================== GREENHOUSE =====================
    fun getUserGreenhouses(ownerId: String): Flow<List<Greenhouse>>
    suspend fun saveGreenhouse(greenhouse: Greenhouse)
    suspend fun addGreenhouse(newGreenhouse: NewGreenhouse)
    suspend fun addGreenhouseMember(member: GreenhouseMember)
    suspend fun addGreenhouseToUser(uid: String, greenhouseId: String)
    suspend fun registerUserAndGreenhouse(userProfile: UserProfile, newGreenhouse: NewGreenhouse)

    // ===================== SENSOR =====================
    fun getLatestSensorData(greenhouseId: String): Flow<SensorReading?>
    suspend fun getHistoricalSensorData(greenhouseId: String): List<SensorReading>
    suspend fun saveSensorReading(sensorReading: SensorReading)
    suspend fun addSensorReading(sensorReading: SensorReading)
    suspend fun editSensorReading(readingId: String, updatedReading: SensorReading)
    suspend fun deleteSensorReading(readingId: String)

    // ===================== DEVICE =====================
    fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>>
    suspend fun addDevice(device: Device)
    suspend fun editDevice(readingId: String, device: Device)
    suspend fun deleteDevice(deviceId: String)
    suspend fun updateDeviceStatus(deviceId: String, status: Boolean)
    suspend fun setAutomaticSetting(deviceId: String, setting: String, value: Any)

    // ===================== PLANT =====================
    fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>>
    suspend fun addPlant(plant: Plant)
    suspend fun updatePlant(readingId: String, updatedPlant: Plant)
    suspend fun deletePlant(plantId: String)
    suspend fun addPlantHistory(plantHistory: PlantHistory)

    suspend fun clearLocalCache()
}
