package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GreenhouseViewModel @Inject constructor(
    private val repository: HydroponicRepository,
    private val authRepository: HydroponicRepository
) : ViewModel() {

    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    private val _sensorReadings = MutableStateFlow<Map<String, SensorReadings>>(emptyMap())
    val sensorReadings: StateFlow<Map<String, SensorReadings>> = _sensorReadings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadUserGreenhouses()
    }

    // 🔹 ADD: Method untuk load sensor readings untuk semua greenhouse
    private fun loadAllSensorReadings() {
        viewModelScope.launch {
            _greenhouses.value.forEach { greenhouse ->
                loadLatestSensorReadings(greenhouse.id)
            }
        }
    }

    // 🔹 FIX: Panggil load sensor readings setelah greenhouses loaded
    fun loadUserGreenhouses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("🔹 [GREENHOUSE-VIEWMODEL] Loading user greenhouses...")

                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    println("🔹 [GREENHOUSE-VIEWMODEL] Loading greenhouses for user: ${currentUser.id}")
                    val result = repository.getUserGreenhouses(currentUser.id)

                    when (result) {
                        is NetworkResult.Success -> {
                            _greenhouses.value = result.data ?: emptyList()
                            println("🔹 [GREENHOUSE-VIEWMODEL] Greenhouses loaded: ${_greenhouses.value.size}")

                            // 🔹 FIX: Load sensor readings setelah greenhouses berhasil dimuat
                            loadAllSensorReadings()
                        }
                        is NetworkResult.Error -> {
                            _errorMessage.value = result.message
                            println("🔹 [GREENHOUSE-VIEWMODEL] Error loading greenhouses: ${result.message}")
                        }
                    }
                } else {
                    println("🔹 [GREENHOUSE-VIEWMODEL] No current user, cannot load greenhouses")
                    _greenhouses.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat greenhouse: ${e.message}"
                println("🔹 [GREENHOUSE-VIEWMODEL] Exception loading greenhouses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadLatestSensorReadings(greenhouseId: String) {
        viewModelScope.launch {
            when (val result = repository.getLatestSensorData(greenhouseId)) {
                is NetworkResult.Success -> {
                    result.data?.let { readings ->
                        _sensorReadings.value = _sensorReadings.value + (greenhouseId to readings)
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal memuat data sensor: ${result.message}"
                }
                else -> {}
            }
        }
    }

    // Method untuk mendapatkan greenhouse by ID
    fun getGreenhouseById(greenhouseId: String): StateFlow<Greenhouse?> {
        return _greenhouses.map { greenhouses ->
            greenhouses.find { it.id == greenhouseId }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
    }

    // Method untuk mendapatkan latest sensor readings by greenhouse ID
    fun getLatestSensorReadings(greenhouseId: String): StateFlow<SensorReadings?> {
        return _sensorReadings.map { readingsMap ->
            readingsMap[greenhouseId]
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
    }

    // Refresh data untuk greenhouse tertentu
    fun refreshGreenhouseData(greenhouseId: String) {
        loadLatestSensorReadings(greenhouseId)
    }

    // Refresh semua data
    fun refreshAllData() {
        loadUserGreenhouses()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
