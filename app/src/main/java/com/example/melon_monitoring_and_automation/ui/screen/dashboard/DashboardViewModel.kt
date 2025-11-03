package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val hydroponicRepository: HydroponicRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _sensorDataState =
        MutableStateFlow<UiState<SensorReading?>>(UiState.Loading)
    val sensorDataState: StateFlow<UiState<SensorReading?>> = _sensorDataState.asStateFlow()

    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    private val _plants = MutableStateFlow<UiState<List<Plant>>>(UiState.Loading)
    val plants: StateFlow<UiState<List<Plant>>> = _plants.asStateFlow()

    private val _activeGreenhouseId = MutableStateFlow<String?>(null)
    val activeGreenhouseId: StateFlow<String?> = _activeGreenhouseId.asStateFlow()


    init {
        loadUserGreenhouses()
    }

    fun loadUserGreenhouses() {
        val ownerId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            hydroponicRepository.getUserGreenhouses(ownerId)
                .collect { greenhouses ->
                    _greenhouses.value = greenhouses
                    if (greenhouses.isNotEmpty() && _activeGreenhouseId.value.isNullOrEmpty()) {
                        setActiveGreenhouse(greenhouses.first().id)
                    }
                }
        }
    }

    fun setActiveGreenhouse(greenhouseId: String) {
        _activeGreenhouseId.value = greenhouseId
    }

    fun loadLatestSensorData(greenhouseId: String) {
        viewModelScope.launch {
            _sensorDataState.value = UiState.Loading
            val result = withTimeoutOrNull(2000) {
                hydroponicRepository.getLatestSensorData(greenhouseId)
                    .onStart { delay(1) }
                    .catch { e ->
                        _sensorDataState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
                    }
                    .firstOrNull()
            }
            if (result != null) {
                _sensorDataState.value = UiState.Success(result)
            } else {
                _sensorDataState.value = UiState.Success(null)
            }
        }
    }

    fun loadGreenhousePlants(greenhouseId: String) {
        viewModelScope.launch {
            _plants.value = UiState.Loading
            val result = withTimeoutOrNull(2000) {
                hydroponicRepository.getGreenhousePlants(greenhouseId)
                    .onStart { delay(1) }
                    .catch { e ->
                        _plants.value = UiState.Error(e.message ?: "Terjadi kesalahan")
                    }
                    .firstOrNull()
            }
            if (result != null) {
                _plants.value = UiState.Success(result)
            } else {
                _plants.value = UiState.Success(emptyList())
            }
        }
    }
}
