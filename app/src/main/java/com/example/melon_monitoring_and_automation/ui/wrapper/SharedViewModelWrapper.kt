package com.example.melon_monitoring_and_automation.ui.wrapper

import com.example.melon_monitoring_and_automation.SharedViewModel
import kotlinx.coroutines.flow.StateFlow

class SharedViewModelWrapper(
    private val sharedViewModel: SharedViewModel
) {
    fun setActiveGreenhouse(greenhouseId: String) {
        sharedViewModel.setActiveGreenhouse(greenhouseId)
    }

    val activeGreenhouseId: StateFlow<String?>
        get() = sharedViewModel.activeGreenhouseId
}