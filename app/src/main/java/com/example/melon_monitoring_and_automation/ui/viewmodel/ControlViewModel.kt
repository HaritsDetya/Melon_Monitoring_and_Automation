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

/**
 * CONTROL VIEWMODEL CLASS
 *
 * Tujuan:
 * - Mengelola state dan business logic untuk control screen
 * - Menangani komunikasi dengan repository untuk operasi perangkat
 * - Mengkoordinasikan real-time updates dan automasi
 *
 * Responsibilities:
 * - Load greenhouse data dan device settings
 * - Handle device control operations (blower, pump, nutrient system)
 * - Manage automation settings dan temperature monitoring
 * - Provide real-time device status updates
 * - Handle error states dan success feedback
 *
 * @author Your Name
 * @since Version 1.0
 * @property repository Repository untuk data hydroponic dan device control
 */

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    // ============ STATE FLOW DEFINITIONS ============

    /**
     * State flow untuk daftar greenhouse user.
     *
     * Di-load pada initialization dan di-update secara periodic.
     */
    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    /**
     * State flow untuk kontrol perangkat per greenhouse.
     *
     * Structure: Map<GreenhouseId, ControlDevices>
     * Menyimpan status real-time semua perangkat.
     */
    private val _controlDevices = MutableStateFlow<Map<String, ControlDevices>>(emptyMap())
    val controlDevices: StateFlow<Map<String, ControlDevices>> = _controlDevices.asStateFlow()

    /**
     * State flow untuk pengaturan automasi per greenhouse.
     *
     * Structure: Map<GreenhouseId, AutomationSettings>
     * Menyimpan threshold settings untuk automasi.
     */
    private val _automationSettings = MutableStateFlow<Map<String, AutomationSettings>>(emptyMap())
    val automationSettings: StateFlow<Map<String, AutomationSettings>> = _automationSettings.asStateFlow()

    /**
     * State flow untuk loading state.
     *
     * True selama operasi network atau device control berlangsung.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * State flow untuk error messages.
     *
     * Menyimpan pesan error untuk ditampilkan ke user.
     * Dapat di-clear dengan clearErrorMessage().
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * State flow untuk update success status.
     *
     * True ketika operasi update berhasil, digunakan untuk show feedback.
     */
    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess.asStateFlow()

    // ============ REALTIME UPDATES MANAGEMENT ============

    /**
     * Map untuk menyimpan realtime update jobs per greenhouse.
     *
     * Digunakan untuk manage realtime subscriptions dan cleanup.
     */
    private val realtimeChannels = mutableMapOf<String, Job>()

    // ============ INITIALIZATION ============

    init {
        loadUserGreenhouses()
    }

    // ============ DATA LOADING METHODS ============

    /**
     * Load user greenhouses dan associated device data.
     *
     * Flow:
     * 1. Set loading state ke true
     * 2. Get current user dari auth repository
     * 3. Load user greenhouses
     * 4. Untuk setiap greenhouse, load control devices dan automation settings
     * 5. Setup realtime updates
     * 6. Handle errors appropriately
     */
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
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                _errorMessage.value = "Gagal memuat greenhouse: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load control device untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-load device-nya
     *
     * Jika device tidak ditemukan, akan dibuat device baru.
     */
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

    /**
     * Load automation settings untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-load settings-nya
     *
     * Jika settings tidak ditemukan, akan dibuat settings default.
     */
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

    // ============ DEVICE CREATION METHODS ============

    /**
     * Create automation settings default untuk greenhouse.
     *
     * @param greenhouseId ID greenhouse untuk create settings
     *
     * Default settings:
     * - Max temperature: 38°C
     * - Min temperature: 25°C
     * - Nutrient droplets: 10
     */
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

    /**
     * Create control device default untuk greenhouse.
     *
     * @param greenhouseId ID greenhouse untuk create device
     *
     * Default device state:
     * - Fan: false
     * - Pump: false
     * - Auto mode: false
     */
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

    // ============ DEVICE CONTROL METHODS ============

    /**
     * Toggle blower (fan) state untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-update
     * @param enabled Status baru untuk blower (true = on, false = off)
     *
     * Flow:
     * 1. Set loading state
     * 2. Get current device state
     * 3. Update device dengan new state
     * 4. Send update ke repository
     * 5. Update local state jika successful
     * 6. Handle errors
     */
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

    /**
     * Set auto mode untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-update
     * @param autoMode Status auto mode baru
     *
     * Jika switching ke auto mode, manual control akan dimatikan.
     * Jika auto mode di-enable, temperature monitoring akan di-start.
     */
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

    /**
     * Set nutrient droplets count untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-update
     * @param droplets Jumlah tetesan nutrisi per siklus (1-50)
     */
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

                // Update in repository
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

    /**
     * Update temperature thresholds untuk automasi blower.
     *
     * @param greenhouseId ID greenhouse yang akan di-update
     * @param minTemperature Suhu minimum untuk mematikan blower
     * @param maxTemperature Suhu maksimum untuk menyalakan blower
     *
     * Validasi: minTemperature harus < maxTemperature
     */
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

    /**
     * Toggle main pump state untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-update
     * @param enabled Status baru untuk pompa (true = on, false = off)
     */
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

    // ============ AUTOMATION METHODS ============

    /**
     * Start temperature monitoring untuk automasi blower.
     *
     * @param greenhouseId ID greenhouse yang akan di-monitor
     *
     * Flow:
     * 1. Get latest temperature readings
     * 2. Compare dengan threshold settings
     * 3. Turn on/off blower berdasarkan rules:
     *    - Turn on jika temperature > max threshold
     *    - Turn off jika temperature < min threshold
     */
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

    // ============ REALTIME UPDATES MANAGEMENT ============

    /**
     * Setup realtime updates untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse untuk setup realtime updates
     *
     * Menggunakan repository's realtime stream untuk mendapatkan
     * device updates secara real-time.
     */
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

    // ============ STATE MANAGEMENT METHODS ============

    /**
     * Clear error message state.
     *
     * Dipanggil dari UI ketika user dismiss error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clear update success state.
     *
     * Dipanggil setelah success message ditampilkan.
     */
    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }

    // ============ LIFECYCLE MANAGEMENT ============

    /**
     * Cleanup resources ketika ViewModel di-destroy.
     *
     * Membersihkan semua realtime channels untuk prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        // Clean up realtime channels
        realtimeChannels.values.forEach { it.cancel() }
    }
}
