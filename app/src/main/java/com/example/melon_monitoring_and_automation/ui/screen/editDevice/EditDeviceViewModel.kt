package com.example.melon_monitoring_and_automation.ui.screen.editDevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditDeviceViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Device?>>(UiState.Loading)
    val uiState: StateFlow<UiState<Device?>> = _uiState.asStateFlow()

    fun loadDevice(greenhouseId: String, deviceId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val devices = repository.getGreenhouseDevices(greenhouseId).first()
                val device = devices.find { it.id == deviceId }
                if (device != null) {
                    _uiState.value = UiState.Success(device)
                } else {
                    _uiState.value = UiState.Error("Perangkat tidak ditemukan.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memuat data perangkat.")
            }
        }
    }

    fun updateDevice(updatedDevice: Device) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
//                repository.editDevice(updatedDevice)
                _uiState.value = UiState.Success(updatedDevice)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memperbarui data perangkat.")
            }
        }
    }

    fun setDeviceStatus(deviceId: String, status: Boolean) {
        viewModelScope.launch {
            val originalDevice = (uiState.value as? UiState.Success)?.data
            if (originalDevice == null) {
                _uiState.value = UiState.Error("Tidak ada data perangkat untuk diperbarui.")
                return@launch
            }
            try {
                repository.updateDeviceStatus(deviceId, status)
                _uiState.value = UiState.Success(originalDevice.copy(status = status))
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal mengubah status perangkat.")
            }
        }
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.deleteDevice(deviceId)
                _uiState.value = UiState.Success(null)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal menghapus data perangkat.")
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Success(null)
    }
}
