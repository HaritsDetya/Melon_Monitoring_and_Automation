package com.example.melon_monitoring_and_automation.domain.usecase

import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Mengambil data sensor terbaru.
// Mengubah tipe kembalian agar sesuai dengan model Supabase yang baru.
class GetRealtimeSensorDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(greenhouseId: String): Flow<SensorReading?> {
        return repository.getLatestSensorData(greenhouseId)
    }
}

// Mengambil data sensor historis.
// Mengubah fungsi menjadi 'suspend' dan tipe kembalian agar sesuai dengan repositori yang diperbarui.
class GetHistoricalDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(greenhouseId: String): List<SensorReading> {
        return repository.getHistoricalSensorData(greenhouseId)
    }
}

// Mengubah status perangkat.
// Menghapus 'greenhouseId' karena tidak lagi diperlukan di lapisan repositori.
class SetDeviceStatusUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(deviceId: String, status: Boolean) {
        repository.updateDeviceStatus(deviceId, status)
    }
}

// Mengambil daftar perangkat.
// Kode ini sudah benar.
class GetGreenhouseDevicesUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(greenhouseId: String): Flow<List<Device>> {
        return repository.getGreenhouseDevices(greenhouseId)
    }
}

// Mengatur konfigurasi otomatis perangkat.
// Menghapus 'greenhouseId' karena tidak lagi diperlukan.
class SetAutomaticSettingUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(deviceId: String, setting: String, value: Any) {
        repository.setAutomaticSetting(deviceId, setting, value)
    }
}
