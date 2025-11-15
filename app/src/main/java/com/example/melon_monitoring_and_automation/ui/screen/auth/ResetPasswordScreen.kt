package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.MainActivity
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.delay

// 🔹 FIX: Pindahkan sealed class ke luar composable
sealed class TokenStatus {
    object CHECKING : TokenStatus()
    object VALID : TokenStatus()
    object INVALID : TokenStatus()
    object EXPIRED : TokenStatus()
    object NOT_FOUND : TokenStatus()
}

// Di ResetPasswordScreen.kt - PERBAIKAN ERROR
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? MainActivity

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var accessToken by remember { mutableStateOf<String?>(null) }
    var tokenStatus by remember { mutableStateOf<TokenStatus>(TokenStatus.CHECKING) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resetPasswordSuccess by viewModel.resetPasswordSuccess.collectAsState()

    // Di ResetPasswordScreen - tambahkan state untuk success UI
    var showSuccessMessage by remember { mutableStateOf(false) }

    // 🔹 PERBAIKAN: Handle auto-navigate back untuk kasus no token
    var shouldNavigateBack by remember { mutableStateOf(false) }

    // 🔹 PERBAIKAN: Main LaunchedEffect untuk token processing
    LaunchedEffect(Unit) {
        println("🔹 [RESET PASSWORD] 🚀 Screen LaunchedEffect started")
        viewModel.clearErrorMessage()

        // 🔹 DEBUG: Cek state activity
        println("🔹 [RESET PASSWORD] Activity: $activity")

    // Consume deep link dari activity
        val pendingDeepLink = activity?.consumePendingDeepLink()
        println("🔹 [RESET PASSWORD] 🔍 Consumed deep link: $pendingDeepLink")

        if (pendingDeepLink != null) {
            println("🔹 [RESET PASSWORD] ✅ PROCESSING DEEP LINK")
            tokenStatus = TokenStatus.CHECKING

            viewModel.processPasswordResetDeepLink(
                context = context,
                uriString = pendingDeepLink.toString()
            ) { token ->
                println("🔹 [RESET PASSWORD] 🎯 DEEP LINK PROCESSING RESULT: $token")
                if (token != null) {
                    accessToken = token
                    tokenStatus = TokenStatus.VALID
                    println("🔹 [RESET PASSWORD] ✅ TOKEN VALID")
                } else {
                    tokenStatus = TokenStatus.INVALID
                    println("🔹 [RESET PASSWORD] ❌ TOKEN INVALID")
                }
            }
        } else {
            // Cek token yang sudah disimpan
            val storedToken = viewModel.getStoredAccessToken(context)
            println("🔹 [RESET PASSWORD] Stored token: ${storedToken?.take(20)}...")

            if (storedToken != null) {
                println("🔹 [RESET PASSWORD] Using stored access token")
                tokenStatus = TokenStatus.CHECKING

                try {
                    val isValid = withTimeout(10000) {
                        viewModel.verifyTokenValidity(storedToken)
                    }

                    if (isValid) {
                        accessToken = storedToken
                        tokenStatus = TokenStatus.VALID
                        println("🔹 [RESET PASSWORD] ✅ Token valid on screen load")
                    } else {
                        tokenStatus = TokenStatus.EXPIRED
                        viewModel.setErrorMessage("Token reset password sudah kadaluarsa. Silakan request link baru.")
                        println("🔹 [RESET PASSWORD] ❌ Token expired on screen load")
                        viewModel.clearRecoveryTokens(context)
                    }
                } catch (e: Exception) {
                    tokenStatus = TokenStatus.INVALID
                    viewModel.setErrorMessage("Error: ${e.message}")
                    println("🔹 [RESET PASSWORD] ❌ Token verification error: ${e.message}")
                }
            } else {
                tokenStatus = TokenStatus.NOT_FOUND
                viewModel.setErrorMessage("Tidak ada link reset password yang aktif.")
                println("🔹 [RESET PASSWORD] ❌ No token found")

                // 🔹 PERBAIKAN: Set state untuk auto navigate back
                shouldNavigateBack = true
            }
        }
    }

    // 🔹 PERBAIKAN: Separate LaunchedEffect untuk handle navigation
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            println("🔹 [RESET PASSWORD] 🚀 Auto-navigating back due to invalid/no token")
            delay(3000) // Tunggu 3 detik agar user bisa baca pesan error
            navController.popBackStack()
        }
    }

    LaunchedEffect(resetPasswordSuccess) {
        if (resetPasswordSuccess) {
            showSuccessMessage = true
            println("🔹 [RESET PASSWORD] ✅ Password reset successful!")
            viewModel.clearRecoveryTokens(context)

            // Tunggu lebih lama agar user baca pesan success
            delay(3000)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.ResetPassword.route) { inclusive = true }
            }
        }
    }

    // Di UI - tampilkan success message yang lebih menonjol
    if (showSuccessMessage) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Password Berhasil Direset!",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mengarahkan ke halaman login...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // 🔹 PERBAIKAN: Juga handle navigate back untuk token invalid dari deep link
    LaunchedEffect(tokenStatus) {
        if (tokenStatus == TokenStatus.INVALID) {
            println("🔹 [RESET PASSWORD] 🚀 Auto-navigating back due to invalid token")
            delay(2000)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.ResetPassword.route) { inclusive = true }
            }
        }
    }

    // ... sisa kode UI yang existing ...
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Manual back navigation
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Show token status
                    when (tokenStatus) {
                        TokenStatus.CHECKING -> {
                            Text(
                                text = "🔍 Memeriksa token...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF57C00),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFF57C00)
                            )
                        }
                        TokenStatus.VALID -> {
                            Text(
                                text = "✅ Token valid. Silakan masukkan password baru.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF388E3C),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            // 🔹 TAMBAHKAN: Password fields hanya show ketika token valid
                            Text(
                                text = "Masukkan password baru Anda",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("Password Baru") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Password")
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                isError = newPassword.isNotBlank() && !isPasswordStrong(newPassword)
                            )

                            // 🔹 TAMBAHKAN: Password Strength Indicator
                            if (newPassword.isNotBlank()) {
                                val strength = getPasswordStrength(newPassword)
                                val strengthColor = when (strength) {
                                    "Kuat" -> Color(0xFF388E3C)
                                    "Sedang" -> Color(0xFFF57C00)
                                    else -> MaterialTheme.colorScheme.error
                                }

                                Text(
                                    text = "Kekuatan password: $strength",
                                    color = strengthColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .align(Alignment.Start)
                                        .padding(top = 4.dp)
                                )
                            }

                            if (newPassword.isNotBlank() && newPassword.length < 6) {
                                Text(
                                    text = "Password minimal 6 karakter",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Confirm Password Field
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Konfirmasi Password") },
                                singleLine = true,
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = "Confirm Password")
                                },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                isError = confirmPassword.isNotBlank() && newPassword != confirmPassword
                            )

                            if (confirmPassword.isNotBlank() && newPassword != confirmPassword) {
                                Text(
                                    text = "Password tidak cocok",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    when {
                                        newPassword.isBlank() || confirmPassword.isBlank() -> {
                                            viewModel.setErrorMessage("Harap isi semua field")
                                        }
                                        newPassword.length < 6 -> {
                                            viewModel.setErrorMessage("Password minimal 6 karakter")
                                        }
                                        newPassword != confirmPassword -> {
                                            viewModel.setErrorMessage("Password tidak cocok")
                                        }
                                        !isPasswordStrong(newPassword) -> {
                                            viewModel.setErrorMessage("Password harus mengandung huruf besar, kecil, dan angka")
                                        }
                                        accessToken == null -> {
                                            viewModel.setErrorMessage("Token tidak tersedia. Silakan request link reset baru.")
                                        }
                                        else -> {
                                            viewModel.clearErrorMessage()
                                            println("🔹 [RESET PASSWORD] Starting password reset with token...")

                                            // 🔹 FIX: Pass context ke viewModel
                                            viewModel.updatePasswordWithToken(
                                                newPassword = newPassword,
                                                accessToken = accessToken!!,
                                                context = context, // 🔹 TAMBAHKAN context di sini
                                                onSuccess = {
                                                    // Success handled by LaunchedEffect
                                                    println("🔹 [RESET PASSWORD] Password update success callback")
                                                },
                                                onError = { error ->
                                                    viewModel.setErrorMessage(error)
                                                    println("🔹 [RESET PASSWORD] Password update error: $error")
                                                }
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    disabledContainerColor = Color(0xFFC8E6C9)
                                ),
                                enabled = tokenStatus == TokenStatus.VALID &&
                                        !isLoading &&
                                        newPassword.isNotBlank() &&
                                        confirmPassword.isNotBlank() &&
                                        newPassword.length >= 6 &&
                                        newPassword == confirmPassword &&
                                        isPasswordStrong(newPassword) // 🔹 TAMBAHKAN validasi strength di enabled
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White
                                    )
                                } else {
                                    Text("Reset Password", color = Color.White, fontSize = 16.sp)
                                }
                            }
                        }
                        TokenStatus.INVALID -> {
                            Text(
                                text = "❌ Token tidak valid atau sudah kadaluarsa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Mengarahkan ke halaman login...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        TokenStatus.NOT_FOUND -> {
                            Text(
                                text = "📧 Tidak ada token yang ditemukan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF757575),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "Mengarahkan kembali...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        TokenStatus.EXPIRED -> {
                            Text(
                                text = "⏰ Token sudah kadaluarsa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                    }

                    // Error Message
                    if (!errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    // Success Message
                    if (resetPasswordSuccess) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Text(
                                text = "✅ Password berhasil direset! Mengarahkan ke login...",
                                color = Color(0xFF388E3C),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Manual back button untuk semua kondisi
                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("Kembali ke Login", color = Color(0xFF4CAF50), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

// 🔹 TAMBAHKAN: Fungsi helper untuk password strength indicator
private fun getPasswordStrength(password: String): String {
    if (password.length < 6) return "Lemah"

    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigits = password.any { it.isDigit() }
    val hasSpecial = password.any { !it.isLetterOrDigit() }

    val strength = listOf(hasUpperCase, hasLowerCase, hasDigits, hasSpecial).count { it }

    return when {
        strength >= 3 && password.length >= 8 -> "Kuat"
        strength >= 2 -> "Sedang"
        else -> "Lemah"
    }
}

// 🔹 Fungsi validasi password strength (sudah ada, pastikan ada)
private fun isPasswordStrong(password: String): Boolean {
    if (password.length < 6) return false
    val hasUpperCase = password.any { it.isUpperCase() }
    val hasLowerCase = password.any { it.isLowerCase() }
    val hasDigits = password.any { it.isDigit() }
    return hasUpperCase && hasLowerCase && hasDigits
}
