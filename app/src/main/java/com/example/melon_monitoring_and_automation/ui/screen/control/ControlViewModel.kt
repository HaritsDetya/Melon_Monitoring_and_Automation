package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.usecase.GetDeviceStatusUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.GetGreenhouseDevicesUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetDeviceStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val setDeviceStatusUseCase: SetDeviceStatusUseCase,
    private val getGreenhouseDevicesUseCase: GetGreenhouseDevicesUseCase
) : ViewModel() {

    private val _devicesWithStatus = MutableStateFlow<List<Device>>(emptyList())
    val devicesWithStatus: StateFlow<List<Device>> = _devicesWithStatus.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

//    init {
//        viewModelScope.launch {
//            combine(_devices, _deviceStatus) { devices, statusMap ->
//                devices.map { device ->
//                    device to (statusMap[device.id] == true)
//                }
//            }.collect {
//                _devicesWithStatus.value = it
//            }
//        }
//    }

    fun loadGreenhouseDevices(greenhouseId: String) {
        viewModelScope.launch {
            try {
                getGreenhouseDevicesUseCase(greenhouseId)
                    // ✅ Tambahkan onStart untuk mengatur loading menjadi true
                    .onStart { _isLoading.value = true }
                    // ✅ Tambahkan catch untuk menangani error
                    .catch { e ->
                        _errorMessage.value = "Gagal memuat perangkat: ${e.message}"
                        _devicesWithStatus.value = emptyList()
                        _isLoading.value = false // Berhenti loading saat error
                    }
                    .collect { devices ->
                        _devicesWithStatus.value = devices
                        _isLoading.value = false // Berhenti loading saat data berhasil diterima
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat perangkat: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun setDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean) {
        viewModelScope.launch {
            try {
                setDeviceStatusUseCase(greenhouseId, deviceId, status)
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengubah status: ${e.message}"
            }
        }
    }
}
