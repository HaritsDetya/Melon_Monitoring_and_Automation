package com.example.melon_monitoring_and_automation.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import com.example.melon_monitoring_and_automation.domain.usecase.GetHistoricalHydroponicDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val hydroponicRepository: HydroponicRepository
) : ViewModel() {

    private val _historicalData = MutableStateFlow<List<HydroponicData>>(emptyList())
    val historicalData: StateFlow<List<HydroponicData>> = _historicalData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchHistoricalData(userId: String, systemId: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val firebaseData = hydroponicRepository.getHistoricalHydroponicDataFromFirebase(userId, systemId, startTime, endTime)
                firebaseData.collect { data ->
                    _historicalData.value = data
                    _isLoading.value = false
                }

            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat riwayat data: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}
