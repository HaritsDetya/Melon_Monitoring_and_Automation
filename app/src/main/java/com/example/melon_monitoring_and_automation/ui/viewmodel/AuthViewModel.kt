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
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit
import com.example.melon_monitoring_and_automation.MainActivity
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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

    // State untuk change password
    private val _changePasswordSuccess = MutableStateFlow(false)
    val changePasswordSuccess: StateFlow<Boolean> = _changePasswordSuccess.asStateFlow()

    fun getCurrentSession() = supabaseClient.auth.currentSessionOrNull()
    fun getCurrentUser() = supabaseClient.auth.currentUserOrNull()

    // 🔹 FIX: Tambahkan state untuk reset password success
    private val _resetPasswordSuccess = MutableStateFlow(false)
    val resetPasswordSuccess: StateFlow<Boolean> = _resetPasswordSuccess.asStateFlow()

    // 🔹 FIX: Constants untuk Supabase configuration
    companion object {
        private const val SUPABASE_URL = "https://aniututpufnxevcdxiio.supabase.co"
        private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV0dXRwdWZueGV2Y2R4aWlvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk3MTYzOTMsImV4cCI6MjA3NTI5MjM5M30.zszt_8P1WNugBVr3FyhqTYmmF3BAOQ2RwFxiCt4bm50"
    }

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

    // ✅ Register user baru - ENHANCED VERSION
    fun register(username: String, email: String, password: String, phoneNumber: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false // Reset success state

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting registration for: $email")

                val result = repository.signUpWithEmail(username, email, password, phoneNumber)

                if (result.isSuccess) {
                    val user = result.getOrNull()
                    println("🔹 [AUTH] ✅ Registration successful: ${user?.email}")

                    _currentUser.value = user
                    _authSuccess.value = true
                    _errorMessage.value = null

                    // Clear form data after successful registration
                    // This will trigger navigation in RegisterScreen

                } else {
                    val error = result.exceptionOrNull()?.message ?: "Registrasi gagal"
                    println("🔹 [AUTH] ❌ Registration failed: $error")

                    _errorMessage.value = error
                    _authSuccess.value = false
                    _currentUser.value = null
                }
            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Registration exception: ${e.message}")
                _errorMessage.value = "Registrasi gagal: ${e.message}"
                _authSuccess.value = false
                _currentUser.value = null
            } finally {
                _isLoading.value = false
                println("🔹 [AUTH] Registration process completed")
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

    // Di AuthViewModel.kt - perbaiki updatePasswordWithToken
    fun updatePasswordWithToken(newPassword: String, accessToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _resetPasswordSuccess.value = false

                println("🔹 [AUTH] Starting password update process...")
                println("🔹 [AUTH] Token: ${accessToken.take(20)}...")

                // 🔹 FIX: Coba verifikasi token dulu sebelum update password
                val isTokenValid = verifyTokenValidity(accessToken)
                if (!isTokenValid) {
                    _isLoading.value = false
                    onError("Token reset password sudah kadaluarsa. Silakan request link baru.")
                    return@launch
                }

                // Coba OkHttp
                println("🔹 [AUTH] 🔄 Trying method 1: OkHttp")
                var result = updatePasswordViaOkHttp(newPassword, accessToken)

                if (!result) {
                    // Fallback ke HttpURLConnection
                    println("🔹 [AUTH] 🔄 Trying method 2: HttpURLConnection")
                    result = updatePasswordViaHttpURLConnection(newPassword, accessToken)
                }

                if (result) {
                    _isLoading.value = false
                    _resetPasswordSuccess.value = true
                    _authSuccess.value = true
                    println("🔹 [AUTH] ✅ Password updated successfully")
                    onSuccess()
                } else {
                    _isLoading.value = false
                    onError("""
                    Gagal mereset password.
                    
                    Kemungkinan penyebab:
                    • Token sudah kadaluarsa
                    • Link reset sudah digunakan
                    • Masalah koneksi server
                    
                    Silakan request link reset baru.
                """.trimIndent())
                }

            } catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Password update process failed: ${e.message}")

                val errorMessage = when {
                    e.message?.contains("expired") == true -> "Token sudah kadaluarsa. Request link baru."
                    e.message?.contains("403") == true -> "Token tidak valid. Request link baru."
                    e.message?.contains("Unable to resolve host") == true -> "Tidak dapat terhubung ke server."
                    else -> "Gagal mereset password: ${e.message ?: "Unknown error"}"
                }

                onError(errorMessage)
            }
        }
    }

    // 🔹 FIX: Perbaiki semua method yang menggunakan URL
    private suspend fun updatePasswordViaHttpURLConnection(newPassword: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [AUTH] Using HttpURLConnection for password update")

                // 🔹 FIX: Gunakan constant URL
                val url = URL("$SUPABASE_URL/auth/v1/user")
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "PUT"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("apikey", ANON_KEY) // 🔹 FIX: Gunakan constant
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

                // 🔹 FIX: Gunakan constant URL
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
                    .url("$SUPABASE_URL/auth/v1/user")
                    .put(requestBody)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("apikey", ANON_KEY) // 🔹 FIX: Gunakan constant
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

    fun processPasswordResetDeepLink(context: Context, uriString: String, onTokenExtracted: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Processing deep link: $uriString")

                val uri = uriString.toUri()
                val fragment = uri.fragment

                // 🔹 PERBAIKAN: Improved error detection
                if (fragment == null) {
                    println("🔹 [AUTH] ❌ No fragment in deep link")
                    _errorMessage.value = "Link reset password tidak valid."
                    onTokenExtracted(null)
                    return@launch
                }

                if (fragment.contains("error")) {
                    println("🔹 [AUTH] ❌ Error deep link detected: $fragment")
                    val errorMessage = parseDeepLinkError(fragment)
                    _errorMessage.value = errorMessage
                    onTokenExtracted(null)
                    return@launch
                }

                // Extract token
                val accessToken = extractTokenFromFragment(fragment)

                if (accessToken != null) {
                    println("🔹 [AUTH] ✅ Access token extracted: ${accessToken.take(20)}...")

                    // Verifikasi token
                    println("🔹 [AUTH] Verifying token validity...")
                    val isValid = withTimeout(10000) {
                        verifyTokenValidity(accessToken)
                    }

                    if (isValid) {
                        saveAccessToken(context, accessToken)
                        _deepLinkProcessed.value = true
                        _errorMessage.value = null
                        println("🔹 [AUTH] ✅ Token valid, deep link processed successfully")
                        onTokenExtracted(accessToken)
                    } else {
                        println("🔹 [AUTH] ❌ Token verification failed - token expired")
                        _errorMessage.value = "Link reset password sudah kadaluarsa. Silakan request link baru."
                        onTokenExtracted(null)
                    }
                } else {
                    println("🔹 [AUTH] ❌ No access token found in deep link")
                    _errorMessage.value = "Tidak dapat mengekstrak token dari link. Pastikan link valid."
                    onTokenExtracted(null)
                }

            } catch (e: TimeoutCancellationException) {
                println("🔹 [AUTH] ❌ Token verification timeout: ${e.message}")
                _errorMessage.value = "Timeout saat memverifikasi token. Silakan coba lagi."
                onTokenExtracted(null)
            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Error processing deep link: ${e.message}")
                _errorMessage.value = "Error memproses link: ${e.message}"
                onTokenExtracted(null)
            }
        }
    }

    // 🔹 IMPROVED: Better token extraction
    private fun extractTokenFromFragment(fragment: String?): String? {
        if (fragment == null) return null

        return fragment.split("&").find { it.startsWith("access_token=") }?.substringAfter("access_token=")
    }

    // Di AuthViewModel.kt - perbaiki fungsi sendPasswordResetEmail
    fun sendPasswordResetEmail(email: String, context: Context) {
        _isLoading.value = true
        _errorMessage.value = null
        _passwordResetSent.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] === SENDING PASSWORD RESET EMAIL ===")
                println("🔹 [AUTH] Email: $email")

                // 🔹 PERBAIKAN: Gunakan redirect URL yang lebih spesifik
                val redirectTo = "app://supabase.com/auth/reset-password"

                println("🔹 [AUTH] Using redirect URL: $redirectTo")

                // 🔹 PERBAIKAN: Clear stored tokens sebelum mengirim email baru
                clearRecoveryTokens(context)

                supabaseClient.auth.resetPasswordForEmail(
                    email = email,
                    redirectUrl = redirectTo
                )

                println("🔹 [AUTH] ✅ Reset email sent successfully")
                _passwordResetSent.value = true
                _errorMessage.value = "✅ Tautan reset password telah dikirim ke $email. " +
                        "Link hanya valid selama 1 jam."

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Failed to send reset email: ${e.message}")
                e.printStackTrace()

                val userFriendlyError = when {
                    e.message?.contains("user not found", ignoreCase = true) == true ->
                        "Email tidak terdaftar dalam sistem"
                    e.message?.contains("rate limit", ignoreCase = true) == true ->
                        "Terlalu banyak percobaan. Coba lagi dalam 1 jam."
                    e.message?.contains("email", ignoreCase = true) == true ->
                        "Format email tidak valid"
                    else -> "Gagal mengirim tautan reset: ${e.message ?: "Coba lagi nanti"}"
                }

                _errorMessage.value = userFriendlyError
                _passwordResetSent.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🔹 PERBAIKAN: Tambahkan overload function tanpa context untuk backward compatibility
    fun sendPasswordResetEmail(email: String) {
        // Default implementation tanpa context - tidak clear tokens
        _isLoading.value = true
        _errorMessage.value = null
        _passwordResetSent.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] === SENDING PASSWORD RESET EMAIL (NO CONTEXT) ===")
                println("🔹 [AUTH] Email: $email")

                val redirectTo = "app://supabase.com/auth/reset-password"
                println("🔹 [AUTH] Using redirect URL: $redirectTo")

                supabaseClient.auth.resetPasswordForEmail(
                    email = email,
                    redirectUrl = redirectTo
                )

                println("🔹 [AUTH] ✅ Reset email sent successfully")
                _passwordResetSent.value = true
                _errorMessage.value = "✅ Tautan reset password telah dikirim ke $email. " +
                        "Link hanya valid selama 1 jam."

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Failed to send reset email: ${e.message}")
                e.printStackTrace()

                val userFriendlyError = when {
                    e.message?.contains("user not found", ignoreCase = true) == true ->
                        "Email tidak terdaftar dalam sistem"
                    e.message?.contains("rate limit", ignoreCase = true) == true ->
                        "Terlalu banyak percobaan. Coba lagi dalam 1 jam."
                    e.message?.contains("email", ignoreCase = true) == true ->
                        "Format email tidak valid"
                    else -> "Gagal mengirim tautan reset: ${e.message ?: "Coba lagi nanti"}"
                }

                _errorMessage.value = userFriendlyError
                _passwordResetSent.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearRecoveryTokens(context: Context) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            remove("recovery_access_token")
            remove("recovery_refresh_token")
            remove("token_saved_time")
        }.apply()

        _resetPasswordSuccess.value = false
        _deepLinkProcessed.value = false
        println("🔹 [AUTH] Recovery tokens cleared")
    }

    // 🔹 TAMBAHKAN: Overload function tanpa parameter untuk kasus tertentu
    fun clearRecoveryTokens() {
        _resetPasswordSuccess.value = false
        _deepLinkProcessed.value = false
        println("🔹 [AUTH] Recovery tokens state cleared (no context)")
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
        _passwordResetSent.value = false
        println("🔹 [AUTH] States cleared (except auth success and user)")
    }

    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    // Di AuthViewModel.kt - perbaiki verifyTokenValidity
    internal suspend fun verifyTokenValidity(accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [AUTH] Verifying token validity...")

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .get()
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                println("🔹 [AUTH] Token verification - Code: ${response.code}, Body: $responseBody")

                when (response.code) {
                    200 -> {
                        println("🔹 [AUTH] ✅ Token is valid")
                        true
                    }
                    401, 403 -> {
                        println("🔹 [AUTH] ❌ Token invalid/expired: $responseBody")
                        false
                    }
                    else -> {
                        println("🔹 [AUTH] ⚠️ Token verification unknown response: ${response.code}")
                        false
                    }
                }

            } catch (e: Exception) {
                println("🔹 [AUTH] ❌ Token verification error: ${e.message}")
                false
            }
        }
    }

    // Di AuthViewModel.kt
    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting secure account deletion via Edge Function...")

                // 🎯 PANGGIL REPOSITORY YANG SUDAH DIUPDATE
                val result = repository.deleteUserAccount()

                if (result is NetworkResult.Success && result.data == true) {
                    // Clear semua local states
                    _currentUser.value = null
                    _authSuccess.value = false
                    _errorMessage.value = null
                    _passwordResetSent.value = false

                    _isLoading.value = false
                    println("🔹 [AUTH] ✅ Account deletion completed successfully")
                    onSuccess()
                } else {
                    _isLoading.value = false
                    val errorMsg = (result as? NetworkResult.Error)?.message ?: "Failed to delete account"
                    println("🔹 [AUTH] ❌ Deletion failed: $errorMsg")
                    _errorMessage.value = errorMsg
                    onError(errorMsg)
                }

            } catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Deletion exception: ${e.message}")
                val errorMsg = "Error: ${e.message ?: "Unknown error"}"
                _errorMessage.value = errorMsg
                onError(errorMsg)
            }
        }
    }

    // Fungsi untuk change password (untuk user yang sudah login)
    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _changePasswordSuccess.value = false

                println("🔹 [AUTH] Starting password change process...")

                // Step 1: Re-authenticate user dengan password saat ini
                val currentUser = supabaseClient.auth.currentUserOrNull()
                if (currentUser == null) {
                    _isLoading.value = false
                    onError("User tidak terautentikasi. Silakan login ulang.")
                    return@launch
                }

                // Step 2: Re-authenticate dengan email dan password saat ini
                try {
                    supabaseClient.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                        this.email = currentUser.email ?: ""
                        this.password = currentPassword
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                    onError("Password saat ini salah")
                    return@launch
                }

                // Step 3: Update password dengan user yang sudah terautentikasi ulang
                supabaseClient.auth.updateUser {
                    this.password = newPassword
                }

                _isLoading.value = false
                _changePasswordSuccess.value = true
                println("🔹 [AUTH] ✅ Password changed successfully")
                onSuccess()

            } catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Password change failed: ${e.message}")

                val errorMessage = when {
                    e.message?.contains("Invalid login credentials") == true -> "Password saat ini salah"
                    e.message?.contains("Password should be at least") == true -> "Password baru terlalu pendek (minimal 6 karakter)"
                    else -> "Gagal mengubah password: ${e.message ?: "Unknown error"}"
                }

                onError(errorMessage)
            }
        }
    }

    // Reset change password state
    fun resetChangePasswordState() {
        _changePasswordSuccess.value = false
    }

    // Di AuthViewModel.kt - ubah menjadi public
    fun parseDeepLinkError(errorFragment: String): String {
        return when {
            errorFragment.contains("otp_expired") -> "Link reset password sudah kadaluarsa. Silakan request link baru."
            errorFragment.contains("access_denied") -> "Akses ditolak. Link reset password tidak valid."
            errorFragment.contains("invalid") -> "Link reset password tidak valid."
            errorFragment.contains("Email+link+is+invalid+or+has+expired") -> "Link reset password sudah kadaluarsa atau tidak valid. Silakan request link baru."
            errorFragment.contains("error_description") -> {
                // Extract custom error description
                val descPattern = "error_description=([^&]+)".toRegex()
                val match = descPattern.find(errorFragment)
                match?.groupValues?.get(1)?.replace("+", " ") ?: "Terjadi error dengan link reset password."
            }
            else -> "Terjadi error dengan link reset password. Silakan request link baru."
        }
    }

    // 🔹 TAMBAHKAN: Fungsi helper untuk Composable
    fun getDeepLinkErrorMessage(deepLinkUri: Uri?): String? {
        val fragment = deepLinkUri?.fragment ?: return null
        if (fragment.contains("error")) {
            return parseDeepLinkError(fragment)
        }
        return null
    }
}
