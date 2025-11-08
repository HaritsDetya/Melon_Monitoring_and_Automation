package com.example.melon_monitoring_and_automation.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _authSuccess = MutableStateFlow(false)
    val authSuccess: StateFlow<Boolean> = _authSuccess.asStateFlow()

    private val _passwordResetSent = MutableStateFlow(false)
    val passwordResetSent: StateFlow<Boolean> = _passwordResetSent.asStateFlow()

    private val _deepLinkProcessed = MutableStateFlow(false)
    val deepLinkProcessed: StateFlow<Boolean> = _deepLinkProcessed.asStateFlow()

    private var _autoCheckEnabled = MutableStateFlow(true)

    fun getCurrentSession() = supabaseClient.auth.currentSessionOrNull()
    fun getCurrentUser() = supabaseClient.auth.currentUserOrNull()

    init {
        println("🔹 [AUTH] AuthViewModel initialized")
    }

    // 🔹 FIX: Simplified check auth status
    fun checkAuthStatus() {
        if (!_autoCheckEnabled.value) {
            println("🔹 [AUTH] Auto-check disabled, skipping auth check")
            return
        }

        viewModelScope.launch {
            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser != null) {
                    _currentUser.value = currentUser
                    _authSuccess.value = true
                    println("🔹 [AUTH] Auth check: User authenticated - ${currentUser.email}")
                } else {
                    _currentUser.value = null
                    _authSuccess.value = false
                    println("🔹 [AUTH] Auth check: No authenticated user")
                }
            } catch (e: Exception) {
                println("🔹 [AUTH] Auth check error: ${e.message}")
                _authSuccess.value = false
                _currentUser.value = null
            }
        }
    }

    // ✅ Enable auto-check hanya untuk screen tertentu
    fun enableAutoCheck() {
        _autoCheckEnabled.value = true
        println("🔹 [AUTH] Auto-check enabled")
    }

    fun disableAutoCheck() {
        _autoCheckEnabled.value = false
        println("🔹 [AUTH] Auto-check disabled")
    }

    // ✅ Register user baru - FIXED
    fun register(username: String, email: String, password: String, phoneNumber: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false // Reset success state

        viewModelScope.launch {
            try {
                val result = repository.signUpWithEmail(username, email, password, phoneNumber)
                if (result.isSuccess) {
                    val user = result.getOrNull()
                    _currentUser.value = user
                    _authSuccess.value = true
                    _errorMessage.value = null

                    // Clear form data after successful registration
                    // This will trigger navigation in MainApp
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Registrasi gagal"
                    _authSuccess.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Registrasi gagal: ${e.message}"
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting login for: $email")

                // Validasi email format sederhana
                if (!isValidEmail(email)) {
                    _errorMessage.value = "Format email tidak valid"
                    return@launch
                }

                val result = repository.signInWithEmail(email, password)

                if (result.isSuccess) {
                    val user = result.getOrNull()
                    println("🔹 [AUTH] Login successful, user: ${user?.email}")

                    _currentUser.value = user
                    _authSuccess.value = true
                    _errorMessage.value = null

                    println("🔹 [AUTH] Auth state updated: authSuccess=true")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Login gagal"
                    println("🔹 [AUTH] Login failed: $error")

                    // Handle specific errors
                    val userFriendlyError = when {
                        error.contains("Invalid login credentials") -> "Email atau password salah"
                        error.contains("Email not confirmed") -> "Email belum dikonfirmasi"
                        error.contains("Unable to resolve host") -> "Tidak dapat terhubung ke server"
                        else -> "Login gagal: $error"
                    }

                    _errorMessage.value = userFriendlyError
                    _authSuccess.value = false
                }
            } catch (e: Exception) {
                println("🔹 [AUTH] Login exception: ${e.message}")
                val userFriendlyError = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet."
                    else -> "Terjadi kesalahan: ${e.message}"
                }
                _errorMessage.value = userFriendlyError
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
                println("🔹 [AUTH] Login process completed, isLoading=false")
            }
        }
    }

    // Helper function untuk validasi email
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // 🔹 FIX: Improved logout - clear states properly
    fun logout() {
        println("🔹 [AUTH] Starting logout process")

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                println("🔹 [AUTH] Step 1: Signing out from Supabase auth")

                // Step 1: Sign out from Supabase
                supabaseClient.auth.signOut()

                println("🔹 [AUTH] Step 2: Clearing session")

                // Step 2: Clear session
                supabaseClient.auth.clearSession()

                println("🔹 [AUTH] Step 3: Clearing repository cache")

                // Step 3: Clear repository cache
                repository.clearLocalCache()

                println("🔹 [AUTH] Step 4: Clearing local states")

                // Step 4: Clear all local states
                _currentUser.value = null
                _authSuccess.value = false
                _errorMessage.value = null
                _passwordResetSent.value = false

                println("🔹 [AUTH] Logout completed successfully")

            } catch (e: Exception) {
                println("🔹 [AUTH] Logout error: ${e.message}")
                _errorMessage.value = "Logout gagal: ${e.message}"

                // Even if there's an error, clear local states
                _currentUser.value = null
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePasswordWithToken(newPassword: String, accessToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                println("🔹 [AUTH] Starting password update process...")

                // Coba OkHttp dulu (lebih robust)
                println("🔹 [AUTH] 🔄 Trying method 1: OkHttp")
                var result = updatePasswordViaOkHttp(newPassword, accessToken)

                if (!result) {
                    // Fallback ke HttpURLConnection
                    println("🔹 [AUTH] 🔄 Trying method 2: HttpURLConnection")
                    result = updatePasswordViaHttpURLConnection(newPassword, accessToken)
                }

                if (result) {
                    _isLoading.value = false
                    _authSuccess.value = true
                    println("🔹 [AUTH] ✅ Password updated successfully")
                    onSuccess()
                } else {
                    _isLoading.value = false
                    onError("""
                    Gagal mereset password melalui API.
                    
                    Kemungkinan penyebab:
                    • Masalah koneksi internet
                    • Token sudah kadaluarsa
                    • Server sedang maintenance
                    
                    Silakan coba lagi atau request link baru.
                """.trimIndent())
                }

            } catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Password update process failed: ${e.message}")

                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true ->
                        "Tidak dapat terhubung ke server. Periksa koneksi internet dan coba lagi."
                    e.message?.contains("timeout") == true ->
                        "Timeout koneksi. Server mungkin sedang lambat, coba lagi."
                    e.message?.contains("SSL") == true ->
                        "Masalah keamanan koneksi. Pastikan device trust certificate."
                    else -> "Gagal mereset password: ${e.message ?: "Unknown error"}"
                }

                onError(errorMessage)
            }
        }
    }

    // Di AuthViewModel.kt - tambahkan function untuk cek koneksi internet
    private suspend fun hasInternetConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Method 1: Ping test
                Runtime.getRuntime().exec("ping -c 1 8.8.8.8").waitFor() == 0
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun updatePasswordViaHttpURLConnection(newPassword: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [AUTH] Using HttpURLConnection for password update")

                val supabaseUrl = "https://aniuutpufnxevcdxiio.supabase.co"
                val url = URL("$supabaseUrl/auth/v1/user")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV1dHB1Zm54ZXZjZHhpaW8iLCJyb2xlIjoiYW5vbiIsImlhdCI6MTcyMDY2MDYxNiwiZXhwIjoxODM2NDM2NjE2fQ.2WlRXLx6S8Kx4R2y5x2V2n2y6n2y6n2y6n2y6n2y6n")
                    setRequestProperty("Prefer", "return=minimal")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val requestBody = """
                {"password": "$newPassword"}
            """.trimIndent()

                connection.outputStream.use { outputStream ->
                    outputStream.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                println("🔹 [AUTH] Response Code: $responseCode")

                responseCode == 200

            } catch (e: Exception) {
                println("🔹 [AUTH] HttpURLConnection error: ${e.message}")
                false
            }
        }
    }

    private suspend fun updatePasswordViaOkHttp(newPassword: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [AUTH] === USING OKHTTP FOR PASSWORD UPDATE ===")

                val supabaseUrl = "https://aniuutpufnxevcdxiio.supabase.co"
                val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV1dHB1Zm54ZXZjZHhpaW8iLCJyb2xlIjoiYW5vbiIsImlhdCI6MTcyMDY2MDYxNiwiZXhwIjoxODM2NDM2NjE2fQ.2WlRXLx6S8Kx4R2y5x2V2n2y6n2y6n2y6n2y6n2y6n"

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val requestBody = """
                {
                    "password": "$newPassword"
                }
            """.trimIndent().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$supabaseUrl/auth/v1/user")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", anonKey)
                    .addHeader("Prefer", "return=minimal")
                    .build()

                println("🔹 [AUTH] Sending OkHttp request...")

                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "No response body"

                    println("🔹 [AUTH] OkHttp Response Code: $responseCode")
                    println("🔹 [AUTH] OkHttp Response Body: $responseBody")

                    when (responseCode) {
                        200 -> {
                            println("🔹 [AUTH] ✅ SUCCESS: Password updated via OkHttp")
                            true
                        }
                        400 -> {
                            println("🔹 [AUTH] ❌ ERROR 400: $responseBody")
                            false
                        }
                        401 -> {
                            println("🔹 [AUTH] ❌ ERROR 401: Invalid token")
                            false
                        }
                        else -> {
                            println("🔹 [AUTH] ❌ ERROR $responseCode: $responseBody")
                            false
                        }
                    }
                }

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ OkHttp Exception: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun testNetworkConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Test multiple endpoints
                val testUrls = listOf(
                    "https://www.google.com",
                    "https://8.8.8.8", // Google DNS
                    "https://api.supabase.co"
                )

                val results = testUrls.map { url ->
                    try {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000
                        connection.requestMethod = "HEAD"
                        val result = connection.responseCode == 200
                        println("🔹 [NETWORK TEST] $url: ${if (result) "✅" else "❌"}")
                        result
                    } catch (e: Exception) {
                        println("🔹 [NETWORK TEST] $url: ❌ ${e.message}")
                        false
                    }
                }

                val hasConnection = results.any { it }
                println("🔹 [NETWORK TEST] Overall: ${if (hasConnection) "✅ CONNECTED" else "❌ NO INTERNET"}")
                hasConnection

            } catch (e: Exception) {
                println("🔹 [NETWORK TEST] Exception: ${e.message}")
                false
            }
        }
    }

    // Di AuthViewModel.kt - perbaiki function processPasswordResetDeepLink
    fun processPasswordResetDeepLink(context: Context, uriString: String, onTokenExtracted: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Processing deep link: ${uriString.take(100)}...")

                val uri = uriString.toUri()
                val fragment = uri.fragment

                if (fragment != null && fragment.contains("access_token")) {
                    println("🔹 [AUTH] Found access token in deep link")

                    // Extract tokens
                    val params = fragment.split("&").associate {
                        val parts = it.split("=")
                        if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
                    }

                    val accessToken = params["access_token"]
                    val type = params["type"]

                    println("🔹 [AUTH] Token type: $type")

                    if (accessToken != null) {
                        println("🔹 [AUTH] ✅ Access token extracted: ${accessToken.take(20)}...")

                        // Simpan token untuk digunakan nanti
                        saveAccessToken(context, accessToken)

                        _deepLinkProcessed.value = true
                        _errorMessage.value = null
                        println("🔹 [AUTH] ✅ Deep link processed successfully")

                        // Panggil callback dengan token
                        onTokenExtracted(accessToken)

                    } else {
                        println("🔹 [AUTH] ❌ No access token found")
                        _errorMessage.value = "Token tidak ditemukan dalam link"
                        onTokenExtracted(null)
                    }
                } else {
                    println("🔹 [AUTH] ❌ Invalid deep link format")
                    _errorMessage.value = "Format link reset password tidak valid"
                    onTokenExtracted(null)
                }

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Error processing deep link: ${e.message}")
                _errorMessage.value = "Error memproses link: ${e.message}"
                onTokenExtracted(null)
            }
        }
    }

    // 🔹 FIX: Simpan access token untuk digunakan nanti
    private fun saveAccessToken(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("recovery_access_token", token) }
        println("🔹 [AUTH] Access token saved")
    }

    // 🔹 FIX: Ambil access token yang disimpan
    fun getStoredAccessToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("recovery_access_token", null).also {
            if (it != null) {
                println("🔹 [AUTH] Retrieved stored token: ${it.take(10)}...")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                println("🔹 [AUTH] === SENDING PASSWORD RESET EMAIL ===")
                println("🔹 [AUTH] Email: $email")

                // 🔹 FIX: Gunakan URL yang SESUAI dengan yang ada di Supabase Dashboard
                val redirectUrl = "app://supabase.com/auth/reset-password"

                println("🔹 [AUTH] Using redirect URL: $redirectUrl")
                println("🔹 [AUTH] Calling Supabase resetPasswordForEmail...")

                supabaseClient.auth.resetPasswordForEmail(
                    email = email,
                    redirectUrl = redirectUrl
                )

                println("🔹 [AUTH] ✅ Reset email sent successfully")
                _passwordResetSent.value = true
                _errorMessage.value = "Tautan reset password telah dikirim ke $email. " +
                        "Buka link tersebut dari email untuk melanjutkan."

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Failed to send reset email: ${e.message}")
                println("🔹 [AUTH] Error type: ${e.javaClass.simpleName}")
                e.printStackTrace()

                _errorMessage.value = "Gagal mengirim tautan reset: ${e.message}"
                _passwordResetSent.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("🔹 [AUTH-VIEWMODEL] Loading current user...")

                val user = repository.getCurrentUser()
                if (user != null) {
                    _currentUser.value = user
                    _authSuccess.value = true
                    println("🔹 [AUTH-VIEWMODEL] Current user loaded: ${user.email}")
                } else {
                    _currentUser.value = null
                    _authSuccess.value = false
                    println("🔹 [AUTH-VIEWMODEL] No current user found")
                }
            } catch (e: Exception) {
                println("🔹 [AUTH-VIEWMODEL] Error loading current user: ${e.message}")
                _errorMessage.value = "Gagal memuat data pengguna"
                _currentUser.value = null
                _authSuccess.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearAllStates() {
        _isLoading.value = false
        _errorMessage.value = null
        // Jangan reset authSuccess dan currentUser di sini
        // _authSuccess.value = false
        // _currentUser.value = null
        _passwordResetSent.value = false
        println("🔹 [AUTH] States cleared (except auth success and user)")
    }

    // Method khusus untuk reset saat logout
    private fun resetAllStatesForLogout() {
        _isLoading.value = false
        _errorMessage.value = null
        _authSuccess.value = false
        _currentUser.value = null
        _passwordResetSent.value = false
        println("🔹 [AUTH] All states reset for logout")
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }
}
