package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val repository: HydroponicRepository
) : ViewModel() {

    private var authStateListener: AuthStateListener? = null

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    init {
        authStateListener = AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            viewModelScope.launch {
                if (firebaseUser != null) {
                    try {
                        val userProfile = repository.getUserProfile(firebaseUser.uid).firstOrNull()
                        _currentUser.value = userProfile
                        _authSuccess.value = userProfile != null
                    } catch (e: Exception) {
                        _errorMessage.value = "Gagal memuat profil: ${e.message}"
                        _authSuccess.value = false
                    }
                } else {
                    _currentUser.value = null
                    _authSuccess.value = false
                }
            }
        }
        // ✅ Perbaikan: Tambahkan listener saat init
        auth.addAuthStateListener(authStateListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        // ✅ Perbaikan: Hapus listener saat ViewModel dibersihkan
        authStateListener?.let {
            auth.removeAuthStateListener(it)
        }
    }

//    private fun fetchUserProfile(uid: String) {
//        viewModelScope.launch {
//            _isLoading.value = true
//            try {
//                val userProfile = repository.getUserProfile(uid).firstOrNull()
//                _currentUser.value = userProfile
//                _authSuccess.value = userProfile != null
//            } catch (e: Exception) {
//                _errorMessage.value = "Gagal memuat profil pengguna: ${e.message}"
//                _authSuccess.value = false
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }

    fun register(username: String, email: String, password: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false
        viewModelScope.launch {
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user
                firebaseUser?.let {
                    val newGreenhouseId = "gh_${it.uid}"

                    // ✅ Buat data Device, Plant, dan Sensor default
                    val defaultDevices = mapOf(
                        "dev_01" to Device(
                            id = "dev_01",
                            name = "Pompa Nutrisi",
                            type = "pump",
                            config = null,
                            status = false,
                            greenhouseId = newGreenhouseId
                        ),
                        "dev_02" to Device(
                            id = "dev_02",
                            name = "Lampu LED",
                            type = "light",
                            config = mapOf("schedule" to "08:00-20:00"),
                            status = false,
                            greenhouseId = newGreenhouseId
                        )
                    )

                    val defaultPlants = mapOf(
                        "pl_01" to Plant(
                            id = "pl_01",
                            name = "Melon 01",
                            type = "Melon Golden",
                            planted_at = "2025-08-20"
                        )
                    )

                    val defaultSensorReading = SensorReading(
                        temperature = 25.0,
                        humidity = 60.0,
                        ph = 6.0,
                        ec = 1.5,
                        waterLevel = "Normal",
                        light = 300.0,
                        recorded_at = System.currentTimeMillis()
                    )

                    // ✅ Buat objek Greenhouse dengan data default
                    val newGreenhouse = Greenhouse(
                        id = newGreenhouseId,
                        name = "Greenhouse Utama",
                        location = "Yogyakarta",
                        owner_id = it.uid,
                        devices = defaultDevices, // ✅ Masukkan data devices default
                        plants = defaultPlants // ✅ Masukkan data plants default
                    )

                    // ✅ Buat objek User
                    val newUser = User(
                        uid = it.uid,
                        email = email,
                        username = username,
                        greenhouses = mapOf(newGreenhouseId to true)
                    )

                    // ✅ Simpan data-data tersebut ke Firebase
                    repository.saveGreenhouse(newGreenhouseId, newGreenhouse)
                    repository.saveUserProfile(newUser)
                    repository.saveSensorReading(newGreenhouseId, defaultSensorReading)

                    _currentUser.value = newUser
                    _authSuccess.value = true
                } ?: run {
                    _errorMessage.value = "Pengguna tidak ditemukan setelah registrasi."
                }
            } catch (e: Exception) {
                _errorMessage.value = mapFirebaseError(e)
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
                // 1. Lakukan otentikasi
                auth.signInWithEmailAndPassword(email, password).await()

                // 2. Jika otentikasi berhasil, ambil UID dan profil pengguna
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val userProfile = repository.getUserProfile(uid).firstOrNull()

                    // 3. Perbarui StateFlow berdasarkan profil yang berhasil diambil
                    _currentUser.value = userProfile
                    _authSuccess.value = userProfile != null
                } else {
                    _errorMessage.value = "Pengguna tidak ditemukan setelah login."
                    _authSuccess.value = false
                }
            } catch (e: Exception) {
                // Tangani error otentikasi
                _errorMessage.value = mapFirebaseError(e)
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        // ✅ Perbaikan: Hapus listener secara manual sebelum sign-out
        authStateListener?.let {
            auth.removeAuthStateListener(it)
        }
        auth.signOut()

        // Batalkan operasi lain yang masih berjalan
        viewModelScope.coroutineContext.cancelChildren()

        _currentUser.value = null
        _authSuccess.value = false
        _isLoading.value = false

        viewModelScope.launch {
            repository.clearLocalCache()
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    private fun mapFirebaseError(e: Exception): String {
        return when {
            e.message?.contains("email address is already in use") == true -> "Email sudah digunakan"
            e.message?.contains("password is invalid") == true -> "Password salah"
            e.message?.contains("no user record") == true -> "Akun tidak ditemukan"
            else -> "Terjadi kesalahan: ${e.message}"
        }
    }
}
