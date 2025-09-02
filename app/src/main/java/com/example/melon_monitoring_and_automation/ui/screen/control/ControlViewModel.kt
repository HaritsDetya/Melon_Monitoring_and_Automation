package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val setAutomaticSettingUseCase: SetAutomaticSettingUseCase
) : ViewModel() {

    private val _waterPumpStatus = MutableStateFlow(false)
    val waterPumpStatus: StateFlow<Boolean> = _waterPumpStatus.asStateFlow()

    private val _nutrientPumpAStatus = MutableStateFlow(false)
    val nutrientPumpAStatus: StateFlow<Boolean> = _nutrientPumpAStatus.asStateFlow()

    private val _phThreshold = MutableStateFlow(6.0)
    val phThreshold: StateFlow<Double> = _phThreshold.asStateFlow()

    private val _irrigationInterval = MutableStateFlow(15)
    val irrigationInterval: StateFlow<Int> = _irrigationInterval.asStateFlow()

    fun setWaterPumpStatus(userId: String, systemId: String, status: Boolean) {
        _waterPumpStatus.value = status
        viewModelScope.launch {
            setDeviceControlUseCase("waterPump", userId, systemId, status)
        }
    }

    fun setNutrientPumpAStatus(userId: String, systemId: String, status: Boolean) {
        _nutrientPumpAStatus.value = status
        viewModelScope.launch {
            setDeviceControlUseCase("nutrientPumpA", userId, systemId, status)
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