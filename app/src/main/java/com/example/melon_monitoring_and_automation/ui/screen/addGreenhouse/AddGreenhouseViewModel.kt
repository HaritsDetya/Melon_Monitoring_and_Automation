package com.example.melon_monitoring_and_automation.ui.screen.addGreenhouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.NewGreenhouse
import com.example.melon_monitoring_and_automation.domain.model.UserProfile
import com.example.melon_monitoring_and_automation.ui.wrapper.SharedViewModelWrapper
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddGreenhouseViewModel @Inject constructor(
    private val hydroponicRepository: HydroponicRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val uiState: StateFlow<UiState<Unit>> = _uiState.asStateFlow()

    fun addGreenhouse(
        name: String,
        location: String,
        sharedViewModel: SharedViewModel
    ) {
        val ownerId = supabaseClient.auth.currentUserOrNull()?.id ?: run {
            _uiState.value = UiState.Error("Pengguna tidak terautentikasi.")
            return
        }
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val newGreenhouse = NewGreenhouse(
                    owner_id = ownerId,
                    name = name,
                    location = location
                )
                hydroponicRepository.addGreenhouse(newGreenhouse)

                val allGreenhouses = hydroponicRepository.getUserGreenhouses(ownerId).first()
                val newlyAddedGreenhouse = allGreenhouses.find { it.name == name && it.location == location }
                newlyAddedGreenhouse?.let {
                    sharedViewModel.setActiveGreenhouse(it.id)
                }

                _uiState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                val userFriendlyMessage = if (e.message?.contains("duplicate key value") == true) {
                    "Greenhouse dengan nama dan lokasi ini sudah ada. Silakan gunakan nama atau lokasi lain."
                } else {
                    "Terjadi kesalahan: ${e.message}"
                }
                _uiState.value = UiState.Error(userFriendlyMessage)
            }
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }
}
