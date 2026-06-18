package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * DEVICE MANAGEMENT VIEWMODEL CLASS
 *
 * Tujuan:
 * - Mengelola state dan business logic untuk device management screen
 * - Menangani operasi pairing, unpairing, dan monitoring device IoT
 * - Mengkoordinasikan data loading dan real-time updates
 *
 * Responsibilities:
 * - Load devices data berdasarkan greenhouse filter
 * - Handle device pairing dengan kode manual/QR
 * - Manage device unpairing process
 * - Provide device status updates
 * - Handle error states dan success feedback
 *
 * @author Your Name
 * @since Version 1.0
 * @property repository Repository untuk data hydroponic dan device management
 */

@HiltViewModel
class DeviceManagementViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    // ============ STATE FLOW DEFINITIONS ============

    private val _devices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val devices: StateFlow<List<IoTDevice>> = _devices.asStateFlow()

    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    private val _selectedGreenhouse = MutableStateFlow<Greenhouse?>(null)
    val selectedGreenhouse: StateFlow<Greenhouse?> = _selectedGreenhouse.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ============ DATA LOADING METHODS ============

    /**
     * Load user greenhouses untuk filter dropdown
     */
    fun loadUserGreenhouses() {
        viewModelScope.launch {
            val currentUser = repository.getCurrentUser()
            currentUser?.let { user ->
                when (val result = repository.getUserGreenhouses(user.id)) {
                    is NetworkResult.Success -> {
                        _greenhouses.value = result.data
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal memuat greenhouse: ${result.message}"
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Load devices berdasarkan greenhouse filter
     */
    fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val greenhouseId = _selectedGreenhouse.value?.id
                val result = if (greenhouseId != null) {
                    repository.getIoTDevicesByGreenhouse(greenhouseId)
                } else {
                    // Load all devices for all user greenhouses
                    loadAllUserDevices()
                }

                when (result) {
                    is NetworkResult.Success -> {
                        _devices.value = result.data
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal memuat perangkat: ${result.message}"
                    }
                    else -> {}
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load semua devices untuk semua greenhouse user
     */
    private suspend fun loadAllUserDevices(): NetworkResult<List<IoTDevice>> {
        val allDevices = mutableListOf<IoTDevice>()

        _greenhouses.value.forEach { greenhouse ->
            when (val result = repository.getIoTDevicesByGreenhouse(greenhouse.id)) {
                is NetworkResult.Success -> {
                    allDevices.addAll(result.data)
                }
                is NetworkResult.Error -> {
                    // Continue with other greenhouses even if one fails
                    println("Failed to load devices for greenhouse ${greenhouse.id}: ${result.message}")
                }
                else -> {}
            }
        }

        return NetworkResult.Success(allDevices)
    }

    // ============ DEVICE OPERATIONS ============

    /**
     * Pair device dengan greenhouse tertentu
     */
    fun pairDevice(deviceId: String, greenhouseId: String, pairingCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = repository.pairIoTDevice(deviceId, greenhouseId, pairingCode)) {
                is NetworkResult.Success -> {
                    // Reload devices setelah pairing berhasil
                    loadDevices()
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal pairing device: ${result.message}"
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    /**
     * Unpair device dari greenhouse
     */
    fun unpairDevice(deviceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            when (val result = repository.unpairIoTDevice(deviceId)) {
                is NetworkResult.Success -> {
                    // Reload devices setelah unpairing berhasil
                    loadDevices()
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal unpair device: ${result.message}"
                }
                else -> {}
            }
            _isLoading.value = false
        }
    }

    /**
     * Validate pairing code sebelum pairing
     */
    fun validatePairingCode(deviceId: String, pairingCode: String) {
        viewModelScope.launch {
            when (val result = repository.validatePairingCode(deviceId, pairingCode)) {
                is NetworkResult.Success -> {
                    // Validation successful
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                else -> {}
            }
        }
    }

    // ============ FILTER METHODS ============

    /**
     * Select greenhouse untuk filtering
     */
    fun selectGreenhouse(greenhouse: Greenhouse?) {
        _selectedGreenhouse.value = greenhouse
        loadDevices() // Reload devices dengan filter baru
    }

    // ============ STATE MANAGEMENT ============

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}