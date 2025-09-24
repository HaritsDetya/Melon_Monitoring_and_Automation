package com.example.melon_monitoring_and_automation.domain.usecase

import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Perlu mengubah parameter untuk menyesuaikan perubahan di repository
class GetRealtimeSensorDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(greenhouseId: String): Flow<Pair<String, SensorReading>?> {
        return repository.getLatestSensorData(greenhouseId)
    }
}

// Perlu mengubah parameter untuk menyesuaikan perubahan di repository
class GetHistoricalDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    // ✅ PERBAIKAN: Hapus parameter tanggal
    operator fun invoke(greenhouseId: String): Flow<List<Pair<Long, SensorReading>>> {
        return repository.getHistoricalSensorData(greenhouseId)
    }
}

// Perlu mengubah parameter untuk menyesuaikan perubahan di repository
class GetDeviceStatusUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(greenhouseId: String, deviceId: String): Flow<Boolean?> {
        return repository.getDeviceStatus(greenhouseId, deviceId)
    }
}

// Perlu mengubah parameter untuk menyesuaikan perubahan di repository
class SetDeviceStatusUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(greenhouseId: String, deviceId: String, status: Boolean) {
        repository.updateDeviceStatus(greenhouseId, deviceId, status)
    }
}

// Tidak perlu diubah
class GetGreenhouseDevicesUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(greenhouseId: String): Flow<List<Device>> {
        return repository.getGreenhouseDevices(greenhouseId)
    }
}

// Perlu mengubah parameter untuk menyesuaikan perubahan di repository
class SetAutomaticSettingUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(greenhouseId: String, deviceId: String, setting: String, value: Any) {
        repository.setAutomaticSetting(greenhouseId, deviceId, setting, value)
    }
}
