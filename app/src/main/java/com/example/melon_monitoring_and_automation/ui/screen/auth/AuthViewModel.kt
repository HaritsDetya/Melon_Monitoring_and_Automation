package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserModel?>(null)
    val currentUser: StateFlow<UserModel?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            viewModelScope.launch {
                _isLoading.value = true
                if (firebaseUser != null) {
                    try {
                        val userProfile = repository.getUserProfile(firebaseUser.uid).firstOrNull()
                        _currentUser.value = userProfile
                        _authSuccess.value = userProfile != null
                    } catch (e: Exception) {
                        _errorMessage.value = "Gagal memuat profil pengguna: ${e.message}"
                        _authSuccess.value = false
                    }
                } else {
                    _currentUser.value = null
                    _authSuccess.value = false
                }
                _isLoading.value = false
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                firebaseUser?.let {
                    val newUser = UserModel(uid = it.uid, email = it.email, username = username)
                    repository.saveUserProfile(newUser)
                    repository.saveSystemData(it.uid, "mainSystem")

                    _currentUser.value = newUser
                    _authSuccess.value = true
                } ?: run {
                    _errorMessage.value = "Pengguna tidak ditemukan setelah registrasi."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        clearErrorMessage()
        auth.signOut()
        _currentUser.value = null
        _authSuccess.value = false
        println("Pengguna berhasil logout")
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
