package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.IoTDeviceRepository
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicePairingViewModel @Inject constructor(
    private val deviceRepository: IoTDeviceRepository
) : ViewModel() {

    sealed class PairingState {
        object Idle : PairingState()
        object Loading : PairingState()
        data class Success(val device: IoTDevice) : PairingState()
        data class Error(val message: String) : PairingState()
    }

    // State flows
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _pairingCode = MutableStateFlow("")
    val pairingCode: StateFlow<String> = _pairingCode.asStateFlow()

    private val _selectedGreenhouseId = MutableStateFlow<String?>(null)
    val selectedGreenhouseId: StateFlow<String?> = _selectedGreenhouseId.asStateFlow()

    // Actions
    fun onDeviceIdChange(newValue: String) {
        _deviceId.value = newValue
    }

    fun onPairingCodeChange(newValue: String) {
        _pairingCode.value = newValue
    }

    fun onGreenhouseSelected(greenhouseId: String) {
        _selectedGreenhouseId.value = greenhouseId
    }

    fun pairDevice() {
        val currentDeviceId = _deviceId.value
        val currentPairingCode = _pairingCode.value
        val currentGreenhouseId = _selectedGreenhouseId.value

        if (currentDeviceId.isBlank() || currentPairingCode.isBlank()) {
            _pairingState.value = PairingState.Error("Device ID dan kode pairing harus diisi")
            return
        }

        if (currentGreenhouseId == null) {
            _pairingState.value = PairingState.Error("Pilih greenhouse terlebih dahulu")
            return
        }

        viewModelScope.launch {
            _pairingState.value = PairingState.Loading

            val result = deviceRepository.pairDevice(
                currentDeviceId,
                currentPairingCode,
                currentGreenhouseId
            )

            _pairingState.value = when (result) {
                is NetworkResult.Success -> PairingState.Success(result.data)
                is NetworkResult.Error -> {
                    val errorMsg = when {
                        result.message.contains("not found", ignoreCase = true) ->
                            "Device ID tidak ditemukan"
                        result.message.contains("pairing code", ignoreCase = true) ->
                            "Kode pairing salah"
                        result.message.contains("already paired", ignoreCase = true) ->
                            "Device sudah terhubung dengan greenhouse lain"
                        else -> "Gagal menghubungkan device: ${result.message}"
                    }
                    PairingState.Error(errorMsg)
                }
            }
        }
    }

    fun resetState() {
        _pairingState.value = PairingState.Idle
        _deviceId.value = ""
        _pairingCode.value = ""
    }
}
