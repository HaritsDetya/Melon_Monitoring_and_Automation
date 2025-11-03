package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow

//interface HydroponicRepository {
//    // ===================== USER =====================
//    fun getUserProfile(uid: String): Flow<User?>
//    suspend fun saveUserProfile(user: User)
//    suspend fun clearLocalCache()
//
//    // ===================== GREENHOUSE =====================
//    fun getUserGreenhouses(uid: String): Flow<List<Greenhouse>>
//    suspend fun saveGreenhouse(greenhouseId: String, greenhouse: Greenhouse)
//    suspend fun addGreenhouseToUser(uid: String, greenhouseId: String)
//
//    // ===================== SENSOR =====================
//    fun getLatestSensorData(greenhouseId: String): Flow<Pair<String, SensorReading>?>
//    fun getHistoricalSensorData(greenhouseId: String): Flow<List<Pair<Long, SensorReading>>>
//    suspend fun saveSensorReading(greenhouseId: String, sensorReading: SensorReading)
//    suspend fun addSensorReading(greenhouseId: String, sensorReading: SensorReading)
//    suspend fun editSensorReading(greenhouseId: String, readingId: String, updatedReading: SensorReading)
//    suspend fun deleteSensorReading(greenhouseId: String, readingId: String)
//
//    // ===================== DEVICE =====================
//    fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>>
//    suspend fun updateDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean)
//    suspend fun addDevice(greenhouseId: String, device: Device)
//    suspend fun editDevice(greenhouseId: String, deviceId: String, device: Device)
//    suspend fun deleteDevice(greenhouseId: String, deviceId: String)
//
//    // ===================== PLANT =====================
//    fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>>
//    suspend fun addPlant(greenhouseId: String, plant: Plant)
//    suspend fun updatePlant(greenhouseId: String, plantId: String, updatedPlant: Plant)
//    suspend fun deletePlant(greenhouseId: String, plantId: String)
//
//    suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any)
//}
