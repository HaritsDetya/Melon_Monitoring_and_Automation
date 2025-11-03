package com.example.melon_monitoring_and_automation.ui.screen.addDevice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddDeviceViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun addDevice(greenhouseId: String, name: String, status: Boolean, type: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val newDevice = Device(
                    id = null,
                    greenhouse_id = greenhouseId,
                    name = name,
                    status = status,
                    type = type,
                )
                repository.addDevice(newDevice)
                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal menambahkan perangkat")
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
