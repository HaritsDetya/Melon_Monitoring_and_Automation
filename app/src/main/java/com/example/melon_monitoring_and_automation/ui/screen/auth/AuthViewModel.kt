package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
            if (firebaseUser != null) {
                viewModelScope.launch {
                    repository.getUserProfile(firebaseUser.uid).collect { userProfile ->
                        _currentUser.value = userProfile
                        _authSuccess.value = userProfile != null
                    }
                }
            } else {
                _currentUser.value = null
                _authSuccess.value = false
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            firebaseUser?.let {
                                val newUser = UserModel(uid = it.uid, email = it.email, username = username)
                                viewModelScope.launch {
                                    repository.saveUserProfile(newUser)
                                    _currentUser.value = newUser
                                    _authSuccess.value = true
                                    _isLoading.value = false
                                    println("Registrasi & penyimpanan profil berhasil untuk: ${newUser.email}")
                                }
                            } ?: run {
                                _errorMessage.value = "Pengguna tidak ditemukan setelah registrasi."
                                _isLoading.value = false
                            }
                        } else {
                            _errorMessage.value = task.exception?.message
                            _isLoading.value = false
                            println("Registrasi gagal: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message
                println("Exception saat registrasi: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val firebaseUser = task.result?.user
                            firebaseUser?.let {
                                viewModelScope.launch {
                                    repository.getUserProfile(it.uid).collect { userProfile ->
                                        _currentUser.value = userProfile
                                        _authSuccess.value = userProfile != null
                                        _isLoading.value = false
                                        println("Login berhasil untuk: ${userProfile?.email}")
                                    }
                                }
                            } ?: run {
                                _errorMessage.value = "Pengguna tidak ditemukan setelah login."
                            }
                        } else {
                            _errorMessage.value =task.exception?.message
                            _isLoading.value = false
                            println("Login gagal: ${task.exception?.message}")
                        }
                    }
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessage.value = e.message
                println("Exception saat login: ${e.message}")
            }
        }
    }

    fun logout() {
        auth.signOut()
        _currentUser.value = null
        _authSuccess.value = false
        println("Pengguna berhasil logout")
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

}