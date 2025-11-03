package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.usecase.GetGreenhouseDevicesUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetDeviceStatusUseCase
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val setDeviceStatusUseCase: SetDeviceStatusUseCase,
    private val getGreenhouseDevicesUseCase: GetGreenhouseDevicesUseCase
) : ViewModel() {

    private val _devices = MutableStateFlow<UiState<List<Device>>>(UiState.Loading)
    val devices: StateFlow<UiState<List<Device>>> = _devices.asStateFlow()

    fun loadGreenhouseDevices(greenhouseId: String) {
        viewModelScope.launch {
            getGreenhouseDevicesUseCase(greenhouseId)
                .onStart { _devices.value = UiState.Loading }
                .catch { e ->
                    _devices.value = UiState.Error(e.message ?: "Gagal memuat perangkat")
                }
                .collect { devices ->
                    _devices.value = UiState.Success(devices)
                }
        }
    }

    fun setDeviceStatus(deviceId: String, status: Boolean) {
        viewModelScope.launch {
            try {
                // 1. Jalankan UseCase untuk update di Supabase
                setDeviceStatusUseCase(deviceId, status)

                // 2. PERBAIKAN KRITIS: Update state lokal secara OPTIMIS
                val currentDevices = (_devices.value as? UiState.Success)?.data ?: return@launch

                val updatedList = currentDevices.map {
                    // Cari perangkat yang diupdate, dan ubah statusnya
                    if (it.id == deviceId) {
                        it.copy(status = status)
                    } else {
                        it
                    }
                }

                // Setel state baru. Ini memicu Compose untuk mengganti Switch ke status 'OFF'
                _devices.value = UiState.Success(updatedList)

            } catch (e: Exception) {
                // Jika Supabase gagal, tampilkan error dan mungkin revert state
                _devices.value = UiState.Error("Gagal mengubah status: ${e.message}")
            }
        }
    }
}
