package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    private val _controlDevices = MutableStateFlow<Map<String, ControlDevices>>(emptyMap())
    val controlDevices: StateFlow<Map<String, ControlDevices>> = _controlDevices.asStateFlow()

    private val _automationSettings = MutableStateFlow<Map<String, AutomationSettings>>(emptyMap())
    val automationSettings: StateFlow<Map<String, AutomationSettings>> = _automationSettings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    // Realtime flows
    private val realtimeChannels = mutableMapOf<String, Job>()

    init {
        loadUserGreenhouses()
    }

    fun loadUserGreenhouses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current user ID from auth menggunakan repository yang sama
                val currentUser = repository.getCurrentUser()
                if (currentUser != null) {
                    when (val result = repository.getUserGreenhouses(currentUser.id)) {
                        is NetworkResult.Success -> {
                            _greenhouses.value = result.data
                            // Load control devices for each greenhouse
                            result.data.forEach { greenhouse ->
                                loadControlDevice(greenhouse.id)
                                loadAutomationSettings(greenhouse.id)
                                setupRealtimeUpdates(greenhouse.id)
                            }
                        }
                        is NetworkResult.Error -> {
                            _errorMessage.value = result.message
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat greenhouse: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadControlDevice(greenhouseId: String) {
        viewModelScope.launch {
            when (val result = repository.getControlDevice(greenhouseId)) {
                is NetworkResult.Success -> {
                    result.data?.let { device ->
                        _controlDevices.value = _controlDevices.value + (greenhouseId to device)
                    } ?: run {
                        // Create control device if not exists
                        createControlDevice(greenhouseId)
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal memuat kontrol device: ${result.message}"
                }
                else -> {}
            }
        }
    }

    private fun loadAutomationSettings(greenhouseId: String) {
        viewModelScope.launch {
            println("🔹 [CONTROL-VM] Loading automation settings for: $greenhouseId")

            when (val result = repository.getAutomationSettings(greenhouseId)) {
                is NetworkResult.Success -> {
                    result.data?.let { settings ->
                        println("🔹 [CONTROL-VM] Settings loaded: $settings")
                        _automationSettings.value = _automationSettings.value + (greenhouseId to settings)
                    } ?: run {
                        println("🔹 [CONTROL-VM] No settings found, creating default...")
                        // Create default automation settings if not exists
                        createAutomationSettings(greenhouseId)
                    }
                }
                is NetworkResult.Error -> {
                    println("🔹 [CONTROL-VM] Error loading settings: ${result.message}")
                    _errorMessage.value = "Gagal memuat automation settings: ${result.message}"
                    // Create default settings as fallback
                    createAutomationSettings(greenhouseId)
                }
                else -> {
                    println("🔹 [CONTROL-VM] Unknown result when loading settings")
                }
            }
        }
    }

    private fun createAutomationSettings(greenhouseId: String) {
        viewModelScope.launch {
            println("🔹 [CONTROL-VM] Creating automation settings for: $greenhouseId")

            when (val result = repository.createAutomationSettings(greenhouseId)) {
                is NetworkResult.Success -> {
                    println("🔹 [CONTROL-VM] Settings created successfully")
                    _automationSettings.value = _automationSettings.value + (greenhouseId to result.data)
                }
                is NetworkResult.Error -> {
                    println("🔹 [CONTROL-VM] Error creating settings: ${result.message}")
                    _errorMessage.value = "Gagal membuat automation settings: ${result.message}"
                    // Create default settings locally as fallback
                    val defaultSettings = AutomationSettings(
                        id = UUID.randomUUID().toString(),
                        greenhouseId = greenhouseId,
                        maxTemperature = 38.0,
                        minTemperature = 25.0,
                        nutrientDroplets = 10,
                        updatedAt = System.now().toString()
                    )
                    _automationSettings.value = _automationSettings.value + (greenhouseId to defaultSettings)
                }
                else -> {
                    println("🔹 [CONTROL-VM] Unknown result when creating settings")
                }
            }
        }
    }

    private fun createControlDevice(greenhouseId: String) {
        viewModelScope.launch {
            when (val result = repository.createControlDeviceIfNotExists(greenhouseId)) {
                is NetworkResult.Success -> {
                    _controlDevices.value = _controlDevices.value + (greenhouseId to result.data)
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal membuat kontrol device: ${result.message}"
                }
                else -> {}
            }
        }
    }

    // 🔹 Toggle Blower (Fan)
    fun toggleBlower(greenhouseId: String, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentDevice = _controlDevices.value[greenhouseId]
            if (currentDevice != null) {
                val updatedDevice = currentDevice.copy(
                    fan = enabled,
                    updatedAt = System.now().toString()
                )

                when (val result = repository.updateDeviceControl(updatedDevice)) {
                    is NetworkResult.Success -> {
                        _controlDevices.value = _controlDevices.value + (greenhouseId to updatedDevice)
                        _updateSuccess.value = true
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal mengupdate blower: ${result.message}"
                    }
                    else -> {}
                }
            }
            _isLoading.value = false
        }
    }

    // 🔹 Set Auto Mode
    fun setAutoMode(greenhouseId: String, autoMode: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentDevice = _controlDevices.value[greenhouseId]
            if (currentDevice != null) {
                val updatedDevice = currentDevice.copy(
                    autoMode = autoMode,
                    // If switching to auto mode, turn off manual control
                    fan = if (autoMode) false else currentDevice.fan,
                    updatedAt = System.now().toString()
                )

                when (val result = repository.updateDeviceControl(updatedDevice)) {
                    is NetworkResult.Success -> {
                        _controlDevices.value = _controlDevices.value + (greenhouseId to updatedDevice)
                        _updateSuccess.value = true

                        // If auto mode is enabled, start checking temperature
                        if (autoMode) {
                            startTemperatureMonitoring(greenhouseId)
                        }
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal mengupdate mode auto: ${result.message}"
                    }
                    else -> {}
                }
            }
            _isLoading.value = false
        }
    }

    // 🔹 Set Nutrient Droplets
    fun setNutrientDroplets(greenhouseId: String, droplets: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentSettings = _automationSettings.value[greenhouseId]
            if (currentSettings != null) {
                val updatedSettings = currentSettings.copy(
                    nutrientDroplets = droplets,
                    updatedAt = System.now().toString()
                )

                // Update in repository (you'll need to add this method)
                when (val result = repository.updateAutomationSettings(updatedSettings)) {
                    is NetworkResult.Success -> {
                        _automationSettings.value = _automationSettings.value + (greenhouseId to updatedSettings)
                        _updateSuccess.value = true
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal mengupdate droplet: ${result.message}"
                    }
                    else -> {}
                }
            }
            _isLoading.value = false
        }
    }

    // 🔹 Temperature Monitoring for Auto Mode
    private fun startTemperatureMonitoring(greenhouseId: String) {
        viewModelScope.launch {
            // Get latest temperature readings
            when (val result = repository.getLatestSensorData(greenhouseId)) {
                is NetworkResult.Success -> {
                    result.data?.temperature?.let { temperature ->
                        val settings = _automationSettings.value[greenhouseId]
                        val device = _controlDevices.value[greenhouseId]

                        if (settings != null && device != null && device.autoMode) {
                            val shouldTurnOnFan = temperature > settings.maxTemperature
                            val shouldTurnOffFan = temperature < settings.minTemperature

                            if (shouldTurnOnFan && !device.fan) {
                                // Temperature too high, turn on fan
                                toggleBlower(greenhouseId, true)
                            } else if (shouldTurnOffFan && device.fan) {
                                // Temperature normal, turn off fan
                                toggleBlower(greenhouseId, false)
                            }
                        }
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal memantau suhu: ${result.message}"
                }
                else -> {}
            }
        }
    }

    // 🔹 Update Temperature Thresholds
    fun updateTemperatureThresholds(
        greenhouseId: String,
        minTemperature: Double,
        maxTemperature: Double
    ) {
        viewModelScope.launch {
            println("🔹 [CONTROL-VM] === START UPDATE TEMPERATURE THRESHOLDS ===")
            println("🔹 [CONTROL-VM] Greenhouse: $greenhouseId")
            println("🔹 [CONTROL-VM] New thresholds - min: $minTemperature, max: $maxTemperature")

            _isLoading.value = true
            _errorMessage.value = null

            val currentSettings = _automationSettings.value[greenhouseId]
            println("🔹 [CONTROL-VM] Current settings: $currentSettings")

            if (currentSettings != null) {
                val updatedSettings = currentSettings.copy(
                    minTemperature = minTemperature,
                    maxTemperature = maxTemperature,
                    updatedAt = System.now().toString()
                )

                println("🔹 [CONTROL-VM] Updated settings: $updatedSettings")
                println("🔹 [CONTROL-VM] Calling repository update...")

                when (val result = repository.updateAutomationSettings(updatedSettings)) {
                    is NetworkResult.Success -> {
                        println("🔹 [CONTROL-VM] ✅ Update successful: ${result.data}")
                        _automationSettings.value = _automationSettings.value + (greenhouseId to updatedSettings)
                        _updateSuccess.value = true
                    }
                    is NetworkResult.Error -> {
                        println("🔹 [CONTROL-VM] ❌ Update failed: ${result.message}")
                        _errorMessage.value = "Gagal mengupdate threshold suhu: ${result.message}"
                    }
                    else -> {
                        println("🔹 [CONTROL-VM] ⚠️ Unknown result type")
                    }
                }
            } else {
                println("🔹 [CONTROL-VM] ❌ No current settings found for greenhouse")
                _errorMessage.value = "Settings tidak ditemukan untuk greenhouse ini"
            }

            _isLoading.value = false
            println("🔹 [CONTROL-VM] === END UPDATE TEMPERATURE THRESHOLDS ===")
        }
    }

    // 🔹 Toggle Pump
    fun togglePump(greenhouseId: String, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val currentDevice = _controlDevices.value[greenhouseId]
            if (currentDevice != null) {
                val updatedDevice = currentDevice.copy(
                    pump = enabled,
                    updatedAt = System.now().toString()
                )

                when (val result = repository.updateDeviceControl(updatedDevice)) {
                    is NetworkResult.Success -> {
                        _controlDevices.value = _controlDevices.value + (greenhouseId to updatedDevice)
                        _updateSuccess.value = true
                    }
                    is NetworkResult.Error -> {
                        _errorMessage.value = "Gagal mengupdate pompa: ${result.message}"
                    }
                    else -> {}
                }
            }
            _isLoading.value = false
        }
    }

    // 🔹 Realtime Updates Setup
    private fun setupRealtimeUpdates(greenhouseId: String) {
        // Stop existing channel if any
        realtimeChannels[greenhouseId]?.cancel()

        val job = viewModelScope.launch {
            repository.getRealtimeDeviceUpdates(greenhouseId).collect { device ->
                device?.let { updatedDevice ->
                    _controlDevices.value = _controlDevices.value + (greenhouseId to updatedDevice)

                    // If auto mode is enabled, check temperature when new data arrives
                    if (updatedDevice.autoMode) {
                        startTemperatureMonitoring(greenhouseId)
                    }
                }
            }
        }

        realtimeChannels[greenhouseId] = job
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up realtime channels
        realtimeChannels.values.forEach { it.cancel() }
    }
}
