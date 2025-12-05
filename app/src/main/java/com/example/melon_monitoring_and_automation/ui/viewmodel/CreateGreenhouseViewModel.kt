package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.CreateGreenhouseResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGreenhouseViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _createSuccess = MutableStateFlow<CreateGreenhouseResponse?>(null)
    val createSuccess: StateFlow<CreateGreenhouseResponse?> = _createSuccess.asStateFlow()

    /**
     * Create new greenhouse
     */
    fun createGreenhouse(name: String, location: String, description: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _createSuccess.value = null

            when (val result = repository.createGreenhouse(name, location, description)) {
                is NetworkResult.Success -> {
                    _createSuccess.value = result.data
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = result.message
                }
                else -> {}
            }

            _isLoading.value = false
        }
    }

    fun clearState() {
        _errorMessage.value = null
        _createSuccess.value = null
    }
}