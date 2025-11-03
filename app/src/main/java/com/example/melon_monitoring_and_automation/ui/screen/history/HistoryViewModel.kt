package com.example.melon_monitoring_and_automation.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.usecase.GetHistoricalDataUseCase
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoricalDataUseCase: GetHistoricalDataUseCase
) : ViewModel() {

    private val _historicalDataState =
        MutableStateFlow<UiState<List<SensorReading>>>(UiState.Loading)
    val historicalDataState: StateFlow<UiState<List<SensorReading>>> =
        _historicalDataState.asStateFlow()

    fun loadHistoricalData(
        greenhouseId: String,
        startTimestamp: Long? = null,
        endTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            _historicalDataState.value = UiState.Loading
            try {
                val allData = getHistoricalDataUseCase(greenhouseId)

                val filteredData = if (startTimestamp != null && endTimestamp != null) {
                    allData.filter { data ->
                        val timestamp = data.recorded_at?.toLongOrNull() ?: 0L
                        timestamp >= startTimestamp && timestamp <= endTimestamp
                    }
                } else {
                    allData
                }
                _historicalDataState.value = UiState.Success(filteredData)
            } catch (e: Exception) {
                _historicalDataState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun clearError() {
        _historicalDataState.value = UiState.Loading
    }
}
