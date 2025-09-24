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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getHistoricalDataUseCase: GetHistoricalDataUseCase
) : ViewModel() {

    private val _historicalDataState =
        MutableStateFlow<UiState<List<Pair<Long, SensorReading>>>>(UiState.Loading)
    val historicalDataState: StateFlow<UiState<List<Pair<Long, SensorReading>>>> =
        _historicalDataState.asStateFlow()

    fun loadHistoricalData(
        greenhouseId: String,
        startTimestamp: Long? = null,
        endTimestamp: Long? = null
    ) {
        viewModelScope.launch {
            getHistoricalDataUseCase(greenhouseId)
                .onStart { _historicalDataState.value = UiState.Loading }
                .catch { e ->
                    _historicalDataState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
                }
                .collect { allData ->
                    val filteredData = if (startTimestamp != null && endTimestamp != null) {
                        allData.filter { (timestamp, _) ->
                            timestamp >= startTimestamp && timestamp <= endTimestamp
                        }
                    } else {
                        allData
                    }
                    _historicalDataState.value = UiState.Success(filteredData)
                }
        }
    }

    fun clearError() {
        _historicalDataState.value = UiState.Loading
    }
}

