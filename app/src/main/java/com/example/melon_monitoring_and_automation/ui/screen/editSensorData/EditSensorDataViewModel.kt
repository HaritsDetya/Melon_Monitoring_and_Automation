package com.example.melon_monitoring_and_automation.ui.screen.editSensorData

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditSensorDataViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<SensorReading?>>(UiState.Loading)
    val uiState: StateFlow<UiState<SensorReading?>> = _uiState.asStateFlow()

    fun loadSensorReading(greenhouseId: String, readingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val allReadings = repository.getHistoricalSensorData(greenhouseId)
                val sensorReading = allReadings.find { it.id == readingId }
                if (sensorReading != null) {
                    _uiState.value = UiState.Success(sensorReading)
                } else {
                    _uiState.value = UiState.Error("Data sensor tidak ditemukan.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memuat data sensor.")
            }
        }
    }

    fun updateSensorReading(readingId: String, updatedReading: SensorReading) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.editSensorReading(readingId, updatedReading)
                _uiState.value = UiState.Success(updatedReading)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memperbarui data sensor.")
            }
        }
    }

    fun deleteSensorReading(readingId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.deleteSensorReading(readingId)
                _uiState.value = UiState.Success(null)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal menghapus data sensor.")
            }
        }
    }

    fun clearError() {
        val lastKnownData = (uiState.value as? UiState.Success)?.data
        _uiState.value = UiState.Success(lastKnownData)
    }
}
