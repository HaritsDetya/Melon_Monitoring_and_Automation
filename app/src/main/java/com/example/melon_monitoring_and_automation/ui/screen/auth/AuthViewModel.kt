package com.example.melon_monitoring_and_automation.ui.screen.auth

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.GreenhouseMember
import com.example.melon_monitoring_and_automation.domain.model.NewGreenhouse
import com.example.melon_monitoring_and_automation.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    private val _registrationSuccess = MutableSharedFlow<Unit>()
    val registrationSuccess = _registrationSuccess.asSharedFlow()

    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

    private val _navigateToPasswordReset = MutableSharedFlow<Unit>()
    val navigateToPasswordReset = _navigateToPasswordReset.asSharedFlow()

    init {
        viewModelScope.launch {
            supabaseClient.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val sessionUser = status.session.user
                        if (sessionUser != null) {
                            try {
                                val userProfile = repository.getUserProfile(sessionUser.id)
                                _currentUser.value = userProfile
                                _authSuccess.value = true
                            } catch (e: Exception) {
                                _errorMessage.value = "Gagal memuat profil: ${e.message}"
                            }
                        } else {
                            _errorMessage.value = "Session user tidak ditemukan."
                        }
                    }

                    else -> {
                        _currentUser.value = null
                        _authSuccess.value = false
                    }
                }
            }
        }
    }

    fun register(username: String, email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                supabaseClient.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session = supabaseClient.auth.currentSessionOrNull()
                session?.user?.let { supabaseUser ->
                    val userProfile = UserProfile(
                        user_id = supabaseUser.id,
                        username = username,
                        full_name = ""
                    )
                    val newGreenhouse = NewGreenhouse(
                        owner_id = supabaseUser.id,
                        name = "Greenhouse Utama",
                        location = "Yogyakarta"
                    )

                    repository.addGreenhouse(newGreenhouse)
                    repository.saveUserProfile(userProfile)

                    val greenhouses = repository.getUserGreenhouses(supabaseUser.id).firstOrNull()
                    val registeredGreenhouse = greenhouses?.firstOrNull()

                    registeredGreenhouse?.let { gh ->
                        val memberEntry = GreenhouseMember(
                            greenhouse_id = gh.id,
                            user_id = supabaseUser.id,
                            role = "owner"
                        )
                        repository.addGreenhouseMember(memberEntry)
                    }

                    _currentUser.value = userProfile
                    _authSuccess.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Registrasi gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                supabaseClient.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (e: Exception) {
                _errorMessage.value = "Login gagal: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun sendPasswordReset(email: String) {
        supabaseClient.auth.resetPasswordForEmail(email)
    }

    fun sendPasswordResetEmail(email: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                supabaseClient.auth.resetPasswordForEmail(email)
                _passwordResetSent.value = true
                _errorMessage.value = "Tautan reset dikirim ke $email"
            } catch (e: Exception) {
                _errorMessage.value = "Gagal mengirim tautan reset: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun handleResetPasswordDeepLink(uri: Uri) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val fragment = uri.fragment ?: return@launch
                val params = fragment.split("&").associate {
                    val (key, value) = it.split("=")
                    key to value
                }

                val accessToken = params["access_token"]
                if (accessToken.isNullOrEmpty()) {
                    _errorMessage.value = "Tautan reset tidak valid."
                    return@launch
                }

                supabaseClient.auth.exchangeCodeForSession(accessToken)
                _navigateToPasswordReset.emit(Unit)

            } catch (e: Exception) {
                _errorMessage.value = "Gagal memproses tautan reset: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                val result = supabaseClient.auth.updateUser(
                    config = {
                        password = newPassword
                    }
                )
                _errorMessage.value = "Kata sandi berhasil diperbarui."
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memperbarui kata sandi: ${e.message}"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                supabaseClient.auth.signOut()
                repository.clearLocalCache()
            } catch (e: Exception) {
                _errorMessage.value = "Gagal logout: ${e.message}"
            } finally {
                _currentUser.value = null
                _authSuccess.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
