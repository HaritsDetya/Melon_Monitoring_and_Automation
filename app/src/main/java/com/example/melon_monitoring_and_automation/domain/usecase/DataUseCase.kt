package com.example.melon_monitoring_and_automation.domain.usecase

import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import javax.inject.Inject

class DataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {

    /**
     * Ambil data sensor terbaru dari greenhouse tertentu.
     * Mengembalikan hasil dalam bentuk NetworkResult.
     */
    suspend fun getLatestSensorData(greenhouseId: String): NetworkResult<SensorReadings?> {
        return repository.getLatestSensorData(greenhouseId)
    }

    /**
     * Perbarui status kontrol perangkat (fan, pump, auto_mode).
     */
    suspend fun updateDeviceControl(device: ControlDevices): NetworkResult<Boolean> {
        return repository.updateDeviceControl(device)
    }

    /**
     * Get control device status
     */
    suspend fun getControlDevice(greenhouseId: String): NetworkResult<ControlDevices?> {
        return repository.getControlDevice(greenhouseId)
    }

    /**
     * Create control device if not exists
     */
    suspend fun createControlDeviceIfNotExists(greenhouseId: String): NetworkResult<ControlDevices> {
        return repository.createControlDeviceIfNotExists(greenhouseId)
    }
}