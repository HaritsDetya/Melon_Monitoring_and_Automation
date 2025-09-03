package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import com.example.melon_monitoring_and_automation.domain.usecase.GetRealtimeHydroponicDataUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getRealtimeHydroponicDataUseCase: GetRealtimeHydroponicDataUseCase,
    private val hydroponicRepository: HydroponicRepository
) : ViewModel() {

    private val _hydroponicData = MutableStateFlow<HydroponicData?>(null)
    val hydroponicData: StateFlow<HydroponicData?> = _hydroponicData.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    val userProfile = hydroponicRepository.getUserProfile(firebaseUser.uid).firstOrNull()
                    _currentUser.value = userProfile
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat profil pengguna: ${e.message}"
            }
        }
    }

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
