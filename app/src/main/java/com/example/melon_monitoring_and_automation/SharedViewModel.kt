package com.example.melon_monitoring_and_automation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SharedViewModel @Inject constructor() : ViewModel() {

    private val _activeGreenhouseId = MutableStateFlow<String?>(null)
    val activeGreenhouseId: StateFlow<String?> = _activeGreenhouseId.asStateFlow()

    fun setActiveGreenhouse(greenhouseId: String) {
        _activeGreenhouseId.value = greenhouseId
    }
}
