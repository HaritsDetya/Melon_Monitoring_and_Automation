package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hydroponicRepository: HydroponicRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _sensorDataState =
        MutableStateFlow<UiState<Pair<Long, SensorReading>>>(UiState.Loading)
    val sensorDataState: StateFlow<UiState<Pair<Long, SensorReading>>> = _sensorDataState.asStateFlow()

    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    private val _plants = MutableStateFlow<List<Plant>>(emptyList())
    val plants: StateFlow<List<Plant>> = _plants.asStateFlow()

    init {
        loadUserGreenhouses()
    }

    fun loadUserGreenhouses() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            hydroponicRepository.getUserGreenhouses(uid)
                .collect { greenhouses ->
                    _greenhouses.value = greenhouses
                }
        }
    }

    fun loadLatestSensorData(greenhouseId: String) {
        viewModelScope.launch {
            hydroponicRepository.getLatestSensorData(greenhouseId)
                .onStart { _sensorDataState.value = UiState.Loading }
                .catch { e ->
                    _sensorDataState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
                }
                .collect { data ->
                    if (data != null) {
                        val (key, reading) = data
                        val timestamp = key.substring(1, 14).toLongOrNull() ?: System.currentTimeMillis()
                        _sensorDataState.value = UiState.Success(timestamp to reading)
                    } else {
                        _sensorDataState.value = UiState.Error("Data sensor kosong")
                    }
                }
        }
    }

    fun loadGreenhousePlants(greenhouseId: String) {
        viewModelScope.launch {
            hydroponicRepository.getGreenhousePlants(greenhouseId)
                .collect { plants ->
                    _plants.value = plants
                }
        }
    }
}
