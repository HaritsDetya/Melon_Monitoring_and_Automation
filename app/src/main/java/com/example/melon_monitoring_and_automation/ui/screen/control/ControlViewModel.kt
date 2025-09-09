package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.domain.usecase.GetRealtimeControlDataUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.GetRealtimeHydroponicDataUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetAutomaticSettingUseCase
import com.example.melon_monitoring_and_automation.domain.usecase.SetDeviceControlUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ControlViewModel @Inject constructor(
    private val setDeviceControlUseCase: SetDeviceControlUseCase,
    private val setAutomaticSettingUseCase: SetAutomaticSettingUseCase,
    private val getRealtimeControlDataUseCase: GetRealtimeControlDataUseCase
) : ViewModel() {

    private val _waterPumpStatus = MutableStateFlow(false)
    val waterPumpStatus: StateFlow<Boolean> = _waterPumpStatus.asStateFlow()

    private val _nutrientPumpAStatus = MutableStateFlow(false)
    val nutrientPumpAStatus: StateFlow<Boolean> = _nutrientPumpAStatus.asStateFlow()

    private val _phThreshold = MutableStateFlow(6.0)
    val phThreshold: StateFlow<Double> = _phThreshold.asStateFlow()

    private val _irrigationInterval = MutableStateFlow(15)
    val irrigationInterval: StateFlow<Int> = _irrigationInterval.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchControlData(userId: String, systemId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                getRealtimeControlDataUseCase(userId, systemId).collect { controlData ->
                    _waterPumpStatus.value = controlData.waterPump
                    _nutrientPumpAStatus.value = controlData.nutrientPumpA
                    if (true) {
                        _phThreshold.value = controlData.phThreshold
                    }
                    if (true) {
                        _irrigationInterval.value = controlData.irrigationInterval
                    }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat data kontrol: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun setWaterPumpStatus(userId: String, systemId: String, status: Boolean) {
        viewModelScope.launch {
            setDeviceControlUseCase(userId, systemId, "waterPump", status)
        }
    }

    fun setNutrientPumpAStatus(userId: String, systemId: String, status: Boolean) {
        viewModelScope.launch {
            setDeviceControlUseCase(userId, systemId, "nutrientPumpA", status)
        }
    }

    fun setPhThreshold(ph: Double) {
        _phThreshold.value = ph
    }

    fun setIrrigationInterval(interval: Int) {
        _irrigationInterval.value = interval
    }

    fun saveAutomaticSettings(userId: String, systemId: String) {
        viewModelScope.launch {
            setAutomaticSettingUseCase(userId, systemId, "phThreshold", _phThreshold.value)
            setAutomaticSettingUseCase(userId, systemId, "irrigationInterval", _irrigationInterval.value)
            println("Pengaturan otomatis disimpan.")
        }
    }

}
