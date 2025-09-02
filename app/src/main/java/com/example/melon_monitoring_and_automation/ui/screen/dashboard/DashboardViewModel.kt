package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import com.example.melon_monitoring_and_automation.domain.usecase.GetRealtimeHydroponicDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getRealtimeHydroponicDataUseCase: GetRealtimeHydroponicDataUseCase
) : ViewModel() {

    private val _hydroponicData = MutableStateFlow<HydroponicData?>(null)
    val hydroponicData: StateFlow<HydroponicData?> = _hydroponicData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchRealtimeData(userId: String, systemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                getRealtimeHydroponicDataUseCase(userId, systemId).collect { data ->
                    _hydroponicData.value = data
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data: ${e.message}"
                _isLoading.value = false
            }
        }
    }
}