package com.example.melon_monitoring_and_automation.ui.screen.editPlant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditPlantViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Plant?>>(UiState.Loading)
    val uiState: StateFlow<UiState<Plant?>> = _uiState.asStateFlow()

    fun loadPlant(greenhouseId: String, plantId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val plants = repository.getGreenhousePlants(greenhouseId).first()
                val plant = plants.find { it.id == plantId }
                if (plant != null) {
                    _uiState.value = UiState.Success(plant)
                } else {
                    _uiState.value = UiState.Error("Tanaman tidak ditemukan.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memuat data tanaman.")
            }
        }
    }

    fun updatePlant(updatedPlant: Plant) {
        viewModelScope.launch {
            val originalPlant = (uiState.value as? UiState.Success)?.data
            if (originalPlant == null) {
                _uiState.value = UiState.Error("Tidak ada data tanaman untuk diperbarui.")
                return@launch
            }
            _uiState.value = UiState.Loading
            try {
//                repository.updatePlant(updatedPlant)
                _uiState.value = UiState.Success(updatedPlant)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal memperbarui data tanaman.")
            }
        }
    }

    fun deletePlant(plantId: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                repository.deletePlant(plantId)
                _uiState.value = UiState.Success(null)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Gagal menghapus data tanaman.")
            }
        }
    }

    fun clearError() {
        _uiState.value = UiState.Success(null)
    }
}
