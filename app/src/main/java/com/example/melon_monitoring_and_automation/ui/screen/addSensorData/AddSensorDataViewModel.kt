package com.example.melon_monitoring_and_automation.ui.screen.addSensorData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddSensorDataViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun addSensorReading(
        greenhouseId: String,
        temperature: Float?,
        humidity: Float?,
        ph: Float?,
        tds: Float?
    ) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val newSensorReading = SensorReading(
                    id = null,
                    greenhouse_id = greenhouseId,
                    temperature = temperature,
                    humidity = humidity,
                    ph = ph,
                    tds = tds
                )

                repository.saveSensorReading(newSensorReading)

                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal menambahkan data sensor")
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
