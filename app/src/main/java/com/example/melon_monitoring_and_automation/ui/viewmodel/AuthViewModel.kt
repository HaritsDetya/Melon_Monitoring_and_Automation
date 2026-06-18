/**
 * AUTH VIEWMODEL
 *
 * Tujuan:
 * - Menangani semua business logic terkait autentikasi user
 * - Mengelola state autentikasi (login, register, logout, reset password)
 * - Berkomunikasi dengan Supabase Auth dan local repository
 * - Memproses deep links untuk reset password flow
 * - Menyediakan state management untuk UI composables
 *
 * Architecture:
 * - Menggunakan Hilt untuk dependency injection
 * - State management dengan MutableStateFlow dan StateFlow
 * - Coroutines untuk async operations
 * - Repository pattern untuk data abstraction
 *
 * Key Features:
 * - Email/password authentication
 * - Password reset dengan deep link processing
 * - Token validation dan management
 * - Comprehensive error handling
 * - Secure password update flow
 * - Account deletion functionality
 *
 * @author Your Name
 * @since Version 1.0
 * @param supabaseClient Supabase client untuk auth operations
 * @param repository Local repository untuk data management
 */

package com.example.melon_monitoring_and_automation.ui.viewmodel

import android.content.Context
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
import androidx.core.content.edit
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.ui.components.PasswordValidator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val repository: HydroponicRepository
) : ViewModel() {

    // STATE MANAGEMENT - MutableStateFlow untuk UI state
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

    // Change password state
    private val _changePasswordSuccess = MutableStateFlow(false)
    val changePasswordSuccess: StateFlow<Boolean> = _changePasswordSuccess.asStateFlow()

    // Reset password success state
    private val _resetPasswordSuccess = MutableStateFlow(false)
    val resetPasswordSuccess: StateFlow<Boolean> = _resetPasswordSuccess.asStateFlow()

    /**
     * SUPABASE CONFIGURATION CONSTANTS
     * Hardcoded values untuk Supabase URL dan Anon Key
     * Note: Untuk production, consider menggunakan BuildConfig atau secure storage
     */
    companion object {
        private const val SUPABASE_URL = "https://aniututpufnxevcdxiio.supabase.co"
        private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV0dXRwdWZueGV2Y2R4aWlvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk3MTYzOTMsImV4cCI6MjA3NTI5MjM5M30.zszt_8P1WNugBVr3FyhqTYmmF3BAOQ2RwFxiCt4bm50"
    }

    init {
        println("🔹 [AUTH] AuthViewModel initialized")
    }

    /**
     * GET CURRENT USER
     * Mendapatkan current user dari Supabase auth session
     * @return Current user atau null jika tidak ada session
     */
    fun getCurrentUser() = supabaseClient.auth.currentUserOrNull()

    /**
     * CHECK AUTH STATUS
     * Memeriksa status autentikasi user saat ini
     * Hanya dijalankan jika auto-check enabled
     * Digunakan untuk auto-redirect di SplashScreen
     */
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
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [AUTH] Auth check error: ${e.message}")
                _authSuccess.value = false
                _currentUser.value = null
            }
        }
    }

    /**
     * ENABLE AUTO CHECK
     * Mengaktifkan auto-check auth status
     * Digunakan untuk screen yang memerlukan auth status checking
     */
    fun enableAutoCheck() {
        _autoCheckEnabled.value = true
        println("🔹 [AUTH] Auto-check enabled")
    }

    /**
     * REGISTER USER
     * Mendaftarkan user baru dengan email dan password
     * Includes password strength validation
     *
     * @param username Username untuk user baru
     * @param email Email address untuk registrasi
     * @param password Password untuk akun baru
     * @param phoneNumber Nomor telepon user (opsional)
     */
    fun register(username: String, email: String, password: String, phoneNumber: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting registration for: $email")

                // Password strength validation sebelum register
                if (password.length < 6) {
                    throw Exception("Password minimal 6 karakter")
                }

                if (!PasswordValidator.isPasswordStrong(password)) {
                    throw Exception("Password harus mengandung huruf besar, kecil, dan angka")
                }

                val result = repository.signUpWithEmail(username, email, password, phoneNumber)

                if (result.isSuccess) {
                    val user = result.getOrNull()
                    println("🔹 [AUTH] ✅ Registration successful: ${user?.email}")

                    _currentUser.value = user
                    _authSuccess.value = true
                    _errorMessage.value = null

                } else {
                    val error = result.exceptionOrNull()?.message ?: "Registrasi gagal"
                    println("🔹 [AUTH] ❌ Registration failed: $error")

                    _errorMessage.value = error
                    _authSuccess.value = false
                    _currentUser.value = null
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * LOGIN USER
     * Authentikasi user dengan email dan password
     * Includes comprehensive error handling dan user-friendly messages
     *
     * @param email Email address user
     * @param password Password user
     */
    fun login(email: String, password: String) {
        if (_isLoading.value) return

        _isLoading.value = true
        _errorMessage.value = null
        _authSuccess.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting login for: $email")

                // Email format validation
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

                    // User-friendly error messages
                    val userFriendlyError = when {
                        error.contains("Invalid login credentials") -> "Email atau password salah"
                        error.contains("Email not confirmed") -> "Email belum dikonfirmasi"
                        error.contains("Unable to resolve host") -> "Tidak dapat terhubung ke server"
                        else -> "Login gagal: $error"
                    }

                    _errorMessage.value = userFriendlyError
                    _authSuccess.value = false
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * VALIDATE EMAIL FORMAT
     * Helper function untuk validasi format email
     *
     * @param email Email address yang akan divalidasi
     * @return Boolean true jika format email valid
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * LOGOUT USER
     * Sign out user dari Supabase auth dan clear semua local states
     * Comprehensive cleanup process dengan error handling
     */
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

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * UPDATE PASSWORD WITH TOKEN
     * Mengupdate password user menggunakan access token dari reset link
     * Includes comprehensive validation dan error handling
     *
     * @param newPassword Password baru
     * @param accessToken Token dari reset link
     * @param context Context untuk shared preferences
     * @param onSuccess Callback ketika berhasil
     * @param onError Callback ketika gagal
     */
    fun updatePasswordWithToken(
        newPassword: String,
        accessToken: String,
        context: Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _resetPasswordSuccess.value = false

                println("🔹 [AUTH] Starting password update process...")
                println("🔹 [AUTH] Token: ${accessToken.take(20)}...")
                println("🔹 [AUTH] New Password: ${newPassword.take(3)}...")

                // Password strength validation
                if (newPassword.length < 6) {
                    throw Exception("Password minimal 6 karakter")
                }

                if (!isPasswordStrong(newPassword)) {
                    throw Exception("Password harus mengandung huruf besar, kecil, dan angka")
                }

                // Update password via Supabase Auth API
                val result = updatePasswordViaSupabase(newPassword, accessToken)

                if (result) {
                    _isLoading.value = false
                    _resetPasswordSuccess.value = true
                    _authSuccess.value = true
                    println("🔹 [AUTH] ✅ Password updated successfully")

                    // Clear token setelah berhasil digunakan
                    clearRecoveryTokens(context)
                    onSuccess()
                } else {
                    throw Exception("Gagal mengupdate password")
                }

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Password update process failed: ${e.message}")

                val errorMessage = handlePasswordUpdateError(e.message ?: "")
                _errorMessage.value = errorMessage
                onError(errorMessage)
            }
        }
    }

    /**
     * VALIDATE PASSWORD STRENGTH
     * Internal helper untuk validasi strength password
     *
     * @param password Password yang akan divalidasi
     * @return Boolean true jika password memenuhi strength requirements
     */
    private fun isPasswordStrong(password: String): Boolean {
        if (password.length < 6) return false

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigits = password.any { it.isDigit() }

        // Minimal requirement: huruf besar, kecil, dan angka
        return hasUpperCase && hasLowerCase && hasDigits
    }

    /**
     * UPDATE PASSWORD VIA SUPABASE API
     * Internal method untuk mengupdate password menggunakan Supabase Auth API
     * Menggunakan OkHttp client untuk direct API calls
     *
     * @param newPassword Password baru
     * @param accessToken Access token untuk authentication
     * @return Boolean true jika update berhasil
     */
    private suspend fun updatePasswordViaSupabase(newPassword: String, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [AUTH] === USING SUPABASE AUTH API FOR PASSWORD UPDATE ===")

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
                    .addHeader("apikey", ANON_KEY)
                    .addHeader("Prefer", "return=minimal")
                    .build()

                println("🔹 [AUTH] Sending password update request...")

                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    val responseBody = response.body?.string() ?: "No response body"

                    println("🔹 [AUTH] Password Update Response Code: $responseCode")
                    println("🔹 [AUTH] Password Update Response Body: $responseBody")

                    when (responseCode) {
                        200 -> {
                            println("🔹 [AUTH] ✅ SUCCESS: Password updated via Auth API")
                            true
                        }
                        400 -> {
                            println("🔹 [AUTH] ❌ ERROR 400: Bad Request - $responseBody")
                            throw Exception("Bad request: $responseBody")
                        }
                        401 -> {
                            println("🔹 [AUTH] ❌ ERROR 401: Unauthorized - Invalid token")
                            throw Exception("Token tidak valid")
                        }
                        422 -> {
                            println("🔹 [AUTH] ❌ ERROR 422: Unprocessable Entity - $responseBody")
                            throw Exception(parsePasswordError(responseBody))
                        }
                        else -> {
                            println("🔹 [AUTH] ❌ ERROR $responseCode: $responseBody")
                            throw Exception("Server error: $responseCode")
                        }
                    }
                }

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [AUTH] ❌ Supabase Auth API Exception: ${e.message}")
                throw e // Re-throw exception untuk ditangani di caller
            }
        }
    }

    /**
     * PARSE PASSWORD ERROR
     * Internal helper untuk parse error messages dari Supabase response
     *
     * @param responseBody Response body dari Supabase API
     * @return String error message yang sudah di-parse
     */
    private fun parsePasswordError(responseBody: String): String {
        return when {
            responseBody.contains("same_password", ignoreCase = true) ->
                "same_password"
            responseBody.contains("password_length", ignoreCase = true) ->
                "password_length"
            responseBody.contains("weak_password", ignoreCase = true) ->
                "weak_password"
            else -> responseBody
        }
    }

    /**
     * PROCESS PASSWORD RESET DEEP LINK
     * Memproses deep link dari email reset password
     * Mengekstrak token dari URL fragment
     *
     * @param context Context untuk shared preferences
     * @param uriString URI string dari deep link
     * @param onTokenExtracted Callback ketika token berhasil diekstrak
     */
    fun processPasswordResetDeepLink(context: Context, uriString: String, onTokenExtracted: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                println("🔹 [AUTH] 🔄 START: Processing deep link")
                println("🔹 [AUTH] URI: $uriString")

                val uri = uriString.toUri()
                val fragment = uri.fragment
                println("🔹 [AUTH] Fragment: ${fragment?.take(50)}...")

                if (fragment == null) {
                    println("🔹 [AUTH] ❌ No fragment in deep link")
                    _errorMessage.value = "Link reset password tidak valid."
                    onTokenExtracted(null)
                    return@launch
                }

                if (fragment.contains("error")) {
                    println("🔹 [AUTH] ❌ Error deep link detected")
                    val errorMessage = parseDeepLinkError(fragment)
                    _errorMessage.value = errorMessage
                    onTokenExtracted(null)
                    return@launch
                }

                // Extract token dengan improved parsing
                val accessToken = extractTokenFromFragment(fragment)
                println("🔹 [AUTH] Extracted token: ${accessToken?.take(20)}...")

                if (accessToken != null) {
                    println("🔹 [AUTH] ✅ Access token extracted successfully")

                    // Simpan token untuk digunakan nanti
                    saveAccessToken(context, accessToken)
                    _deepLinkProcessed.value = true
                    _errorMessage.value = null
                    onTokenExtracted(accessToken)

                    println("🔹 [AUTH] ✅ Token saved for password update")
                } else {
                    println("🔹 [AUTH] ❌ No access token found")
                    _errorMessage.value = "Tidak dapat mengekstrak token dari link."
                    onTokenExtracted(null)
                }

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [AUTH] ❌ Error processing deep link: ${e.message}")
                _errorMessage.value = "Error memproses link: ${e.message}"
                onTokenExtracted(null)
            } finally {
                println("🔹 [AUTH] ✅ FINISH: Deep link processing completed")
            }
        }
    }

    /**
     * EXTRACT TOKEN FROM FRAGMENT
     * Internal helper untuk mengekstrak token dari URL fragment
     * Menggunakan multiple regex patterns untuk compatibility
     *
     * @param fragment URL fragment dari deep link
     * @return Extracted token atau null jika tidak ditemukan
     */
    private fun extractTokenFromFragment(fragment: String?): String? {
        if (fragment == null) {
            println("🔹 [AUTH] ❌ Fragment is null")
            return null
        }

        println("🔹 [AUTH] Extracting token from fragment: ${fragment.take(100)}...")

        // Coba beberapa pattern extraction
        val patterns = listOf(
            "access_token=([^&]+)",
            "token=([^&]+)",
            "#access_token=([^&]+)"
        )

        for (pattern in patterns) {
            val regex = pattern.toRegex()
            val match = regex.find(fragment)
            if (match != null) {
                val token = match.groupValues[1]
                println("🔹 [AUTH] ✅ Token found using pattern: $pattern")
                return token
            }
        }

        println("🔹 [AUTH] ❌ No token pattern matched")
        return null
    }

    /**
     * SEND PASSWORD RESET EMAIL
     * Mengirim email reset password ke alamat yang dimasukkan
     * Includes redirect URL untuk deep link handling
     *
     * @param email Email address untuk reset password
     * @param context Context untuk shared preferences
     */
    fun sendPasswordResetEmail(email: String, context: Context) {
        _isLoading.value = true
        _errorMessage.value = null
        _passwordResetSent.value = false

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] === SENDING PASSWORD RESET EMAIL ===")
                println("🔹 [AUTH] Email: $email")

                // Redirect URL untuk deep link processing
                val redirectTo = "app://supabase.com/auth/reset-password"

                println("🔹 [AUTH] Using redirect URL: $redirectTo")

                // Clear stored tokens sebelum mengirim email baru
                clearRecoveryTokens(context)

                supabaseClient.auth.resetPasswordForEmail(
                    email = email,
                    redirectUrl = redirectTo
                )

                println("🔹 [AUTH] ✅ Reset email sent successfully")
                _passwordResetSent.value = true
                _errorMessage.value = "✅ Tautan reset password telah dikirim ke $email. " +
                        "Link hanya valid selama 1 jam."

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * SEND PASSWORD RESET EMAIL (OVERLOAD)
     * Overload method tanpa context untuk backward compatibility
     *
     * @param email Email address untuk reset password
     */
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

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * CLEAR RECOVERY TOKENS
     * Menghapus stored tokens dari shared preferences
     *
     * @param context Context untuk mengakses shared preferences
     */
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

    /**
     * SAVE ACCESS TOKEN
     * Internal helper untuk menyimpan access token ke shared preferences
     *
     * @param context Context untuk shared preferences
     * @param token Access token yang akan disimpan
     */
    private fun saveAccessToken(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("recovery_access_token", token) }
        println("🔹 [AUTH] Access token saved")
    }

    /**
     * GET STORED ACCESS TOKEN
     * Mengambil stored access token dari shared preferences
     *
     * @param context Context untuk shared preferences
     * @return Stored access token atau null jika tidak ada
     */
    fun getStoredAccessToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("recovery_access_token", null).also {
            if (it != null) {
                println("🔹 [AUTH] Retrieved stored token: ${it.take(10)}...")
            }
        }
    }

    /**
     * LOAD CURRENT USER
     * Memuat data current user dari repository
     * Digunakan untuk refresh user data di UI
     */
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
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * CLEAR ERROR MESSAGE
     * Membersihkan error message state
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * CLEAR ALL STATES
     * Membersihkan semua state kecuali auth success dan user
     * Digunakan untuk reset state antara screen transitions
     */
    fun clearAllStates() {
        _isLoading.value = false
        _errorMessage.value = null
        _passwordResetSent.value = false
        println("🔹 [AUTH] States cleared (except auth success and user)")
    }

    /**
     * SET ERROR MESSAGE
     * Manual set error message untuk display di UI
     *
     * @param message Error message yang akan ditampilkan
     */
    fun setErrorMessage(message: String) {
        _errorMessage.value = message
    }

    /**
     * VERIFY TOKEN VALIDITY
     * Internal method untuk memverifikasi validitas access token
     * Menggunakan Supabase Auth API untuk validation
     *
     * @param accessToken Token yang akan diverifikasi
     * @return Boolean true jika token valid
     */
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

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [AUTH] ❌ Token verification error: ${e.message}")
                false
            }
        }
    }

    /**
     * DELETE ACCOUNT
     * Menghapus akun user secara permanen
     * Menggunakan Edge Function untuk secure deletion
     *
     * @param onSuccess Callback ketika berhasil
     * @param onError Callback ketika gagal
     */
    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                println("🔹 [AUTH] Starting secure account deletion via Edge Function...")

                // Panggil repository untuk account deletion
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

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                _isLoading.value = false
                println("🔹 [AUTH] ❌ Deletion exception: ${e.message}")
                val errorMsg = "Error: ${e.message ?: "Unknown error"}"
                _errorMessage.value = errorMsg
                onError(errorMsg)
            }
        }
    }

    /**
     * CHANGE PASSWORD
     * Mengubah password untuk user yang sudah login
     * Memerlukan re-authentication dengan password saat ini
     *
     * @param currentPassword Password saat ini
     * @param newPassword Password baru
     * @param onSuccess Callback ketika berhasil
     * @param onError Callback ketika gagal
     */
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
                } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
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

    /**
     * RESET CHANGE PASSWORD STATE
     * Membersihkan state change password success
     */
    fun resetChangePasswordState() {
        _changePasswordSuccess.value = false
    }

    /**
     * PARSE DEEP LINK ERROR
     * Parse error messages dari deep link fragment
     *
     * @param errorFragment URL fragment yang mengandung error
     * @return User-friendly error message
     */
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

    /**
     * HANDLE PASSWORD UPDATE ERROR
     * Internal helper untuk handle dan translate password update errors
     *
     * @param error Raw error message
     * @return User-friendly error message
     */
    private fun handlePasswordUpdateError(error: String): String {
        return when {
            error.contains("same_password", ignoreCase = true) ->
                "Password baru harus berbeda dari password lama. Silakan gunakan password yang berbeda."
            error.contains("password_length", ignoreCase = true) ->
                "Password terlalu pendek. Minimal 6 karakter."
            error.contains("weak_password", ignoreCase = true) ->
                "Password terlalu lemah. Gunakan kombinasi huruf besar, kecil, dan angka."
            error.contains("expired", ignoreCase = true) ->
                "Token sudah kadaluarsa. Silakan request link baru."
            error.contains("401") == true ->
                "Token tidak valid. Silakan request link baru."
            error.contains("422") == true ->
                "Password tidak memenuhi requirement."
            error.contains("Unable to resolve host") == true ->
                "Tidak dapat terhubung ke server. Periksa koneksi internet."
            else -> "Gagal mereset password: $error"
        }
    }
}
