package com.example.melon_monitoring_and_automation.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * GREENHOUSE VIEWMODEL CLASS
 *
 * Tujuan:
 * - Mengelola data greenhouse dan sensor readings untuk UI layer
 * - Menyediakan state streams yang reactive untuk Compose UI
 * - Menangani business logic terkait greenhouse operations
 * - Mengkoordinasikan data fetching dan state management
 *
 * Responsibilities:
 * - Memuat daftar greenhouse milik user
 * - Memuat data sensor terkini untuk setiap greenhouse
 * - Menyediakan access methods untuk greenhouse specific data
 * - Menangani error states dan loading states
 * - Mendukung data refresh operations
 *
 * @author Your Name
 * @since Version 1.0
 * @property repository Repository untuk data hydroponic
 * @property authRepository Repository untuk authentication
 */

@HiltViewModel
class GreenhouseViewModel @Inject constructor(
    private val repository: HydroponicRepository,
    private val authRepository: HydroponicRepository
) : ViewModel() {

    // ============ STATE FLOW DEFINITIONS ============

    /**
     * State flow untuk daftar greenhouse user.
     *
     * Diupdate ketika:
     * - Screen pertama kali dimuat
     * - Manual refresh dipanggil
     * - User login/logout state berubah
     */
    private val _greenhouses = MutableStateFlow<List<Greenhouse>>(emptyList())
    val greenhouses: StateFlow<List<Greenhouse>> = _greenhouses.asStateFlow()

    /**
     * State flow untuk pembacaan sensor terkini per greenhouse.
     *
     * Structure: Map<GreenhouseId, SensorReadings>
     * Diupdate secara asynchronous untuk setiap greenhouse.
     */
    private val _sensorReadings = MutableStateFlow<Map<String, SensorReadings>>(emptyMap())
    val sensorReadings: StateFlow<Map<String, SensorReadings>> = _sensorReadings.asStateFlow()

    /**
     * State flow untuk loading state.
     *
     * True ketika:
     * - Memuat daftar greenhouse pertama kali
     * - Manual refresh operations
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * State flow untuk error messages.
     *
     * Berisi pesan error yang dapat ditampilkan ke user.
     * Dapat di-clear dengan clearErrorMessage().
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ============ INITIALIZATION ============

    init {
        loadUserGreenhouses()
    }

    // ============ PRIVATE METHODS ============

    /**
     * Memuat sensor readings untuk semua greenhouse yang dimiliki user.
     *
     * Dipanggil setelah daftar greenhouse berhasil dimuat.
     * Menjalankan concurrent requests untuk setiap greenhouse.
     */
    private fun loadAllSensorReadings() {
        viewModelScope.launch {
            _greenhouses.value.forEach { greenhouse ->
                loadLatestSensorReadings(greenhouse.id)
            }
        }
    }

    /**
     * Memuat pembacaan sensor terkini untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan dimuat sensor readings-nya
     *
     * Error handling:
     * - Menampilkan error message jika gagal
     * - Tidak menghentikan loading greenhouse lainnya
     */
    private fun loadLatestSensorReadings(greenhouseId: String) {
        viewModelScope.launch {
            when (val result = repository.getLatestSensorData(greenhouseId)) {
                is NetworkResult.Success -> {
                    result.data?.let { readings ->
                        // Update map dengan readings baru
                        _sensorReadings.value = _sensorReadings.value + (greenhouseId to readings)
                    }
                }
                is NetworkResult.Error -> {
                    _errorMessage.value = "Gagal memuat data sensor: ${result.message}"
                }
                else -> {
                    // NetworkResult.Loading tidak perlu ditangani khusus
                }
            }
        }
    }

    // ============ PUBLIC METHODS ============

    /**
     * Memuat daftar greenhouse untuk user yang sedang login.
     *
     * Flow:
     * 1. Set loading state ke true
     * 2. Dapatkan current user dari auth repository
     * 3. Jika user ada, load greenhouses dari repository
     * 4. Jika sukses, trigger load sensor readings untuk setiap greenhouse
     * 5. Handle errors dan update state accordingly
     *
     * Dipanggil pada:
     * - Initialization
     * - Manual refresh
     * - Setelah user login
     */
    fun loadUserGreenhouses() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("🔹 [GREENHOUSE-VIEWMODEL] Loading user greenhouses...")

                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    println("🔹 [GREENHOUSE-VIEWMODEL] Loading greenhouses for user: ${currentUser.id}")
                    val result = repository.getUserGreenhouses(currentUser.id)

                    when (result) {
                        is NetworkResult.Success -> {
                            _greenhouses.value = result.data ?: emptyList()
                            println("🔹 [GREENHOUSE-VIEWMODEL] Greenhouses loaded: ${_greenhouses.value.size}")

                            // Trigger load sensor readings setelah greenhouses berhasil dimuat
                            loadAllSensorReadings()
                        }
                        is NetworkResult.Error -> {
                            _errorMessage.value = result.message
                            println("🔹 [GREENHOUSE-VIEWMODEL] Error loading greenhouses: ${result.message}")
                        }
                    }
                } else {
                    println("🔹 [GREENHOUSE-VIEWMODEL] No current user, cannot load greenhouses")
                    _greenhouses.value = emptyList()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Gagal memuat greenhouse: ${e.message}"
                println("🔹 [GREENHOUSE-VIEWMODEL] Exception loading greenhouses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Mendapatkan greenhouse tertentu berdasarkan ID.
     *
     * @param greenhouseId ID greenhouse yang dicari
     * @return StateFlow<Greenhouse?> yang akan emit greenhouse jika ditemukan, atau null
     *
     * Note: Return StateFlow untuk reactive updates jika greenhouse data berubah.
     */
    fun getGreenhouseById(greenhouseId: String): StateFlow<Greenhouse?> {
        return _greenhouses.map { greenhouses ->
            greenhouses.find { it.id == greenhouseId }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000), // Stop setelah 5 detik tidak ada subscriber
            null // Initial value
        )
    }

    /**
     * Mendapatkan pembacaan sensor terkini untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang dicari
     * @return StateFlow<SensorReadings?> yang akan emit readings jika ada, atau null
     *
     * Note: Return StateFlow untuk reactive updates jika sensor data berubah.
     */
    fun getLatestSensorReadings(greenhouseId: String): StateFlow<SensorReadings?> {
        return _sensorReadings.map { readingsMap ->
            readingsMap[greenhouseId]
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000), // Stop setelah 5 detik tidak ada subscriber
            null // Initial value
        )
    }

    /**
     * Refresh data untuk greenhouse tertentu.
     *
     * @param greenhouseId ID greenhouse yang akan di-refresh
     *
     * Gunakan untuk:
     * - Manual refresh dari UI
     * - Periodic updates
     * - Setelah actions yang mengubah state greenhouse
     */
    fun refreshGreenhouseData(greenhouseId: String) {
        loadLatestSensorReadings(greenhouseId)
    }

    /**
     * Refresh semua data (greenhouses dan sensor readings).
     *
     * Gunakan untuk:
     * - Pull-to-refresh di dashboard
     * - Setelah login/logout
     * - Periodic background updates
     */
    fun refreshAllData() {
        loadUserGreenhouses()
    }

    /**
     * Clear error message state.
     *
     * Dipanggil dari UI ketika user dismiss error message.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
