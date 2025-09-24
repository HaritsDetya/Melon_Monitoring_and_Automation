package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.User
import kotlinx.coroutines.flow.Flow

interface HydroponicRepository {
    // ===================== USER =====================
    fun getUserProfile(uid: String): Flow<User?>
    suspend fun saveUserProfile(user: User)
    suspend fun clearLocalCache()

    // ===================== GREENHOUSE =====================
    fun getUserGreenhouses(uid: String): Flow<List<Greenhouse>>
    suspend fun saveGreenhouse(greenhouseId: String, greenhouse: Greenhouse)
    suspend fun addGreenhouseToUser(uid: String, greenhouseId: String)

    // ===================== SENSOR =====================
    // ✅ Perbaikan: Mengubah tipe data timestamp ke String (Push ID)
    fun getLatestSensorData(greenhouseId: String): Flow<Pair<String, SensorReading>?>
    // ✅ Perbaikan: Menggunakan Long untuk timestamp yang lebih efisien di Room
    fun getHistoricalSensorData(greenhouseId: String): Flow<List<Pair<Long, SensorReading>>>
    suspend fun saveSensorReading(greenhouseId: String, sensorReading: SensorReading)

    // ===================== DEVICE =====================
    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    fun getDeviceStatus(greenhouseId: String, deviceId: String): Flow<Boolean?>
    fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>>
    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    suspend fun updateDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean)
    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any)
}
