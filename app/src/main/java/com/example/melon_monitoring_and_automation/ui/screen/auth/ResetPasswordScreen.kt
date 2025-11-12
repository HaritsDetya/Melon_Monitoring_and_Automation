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
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

// 🔹 FIX: Pindahkan sealed class ke luar composable
sealed class TokenStatus {
    object CHECKING : TokenStatus()
    object VALID : TokenStatus()
    object INVALID : TokenStatus()
    object EXPIRED : TokenStatus()
    object NOT_FOUND : TokenStatus()
}

// Ganti fungsi ResetPasswordScreen dengan yang diperbaiki:
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

    // Handle token validation pada screen load
    LaunchedEffect(Unit) {
        println("🔹 [RESET PASSWORD] Screen initialized")
        viewModel.clearErrorMessage()

        // Cek jika ada error deep link yang pending
        val pendingDeepLink = activity?.getPendingDeepLinkInstance()
        if (pendingDeepLink != null) {
            // 🔹 PERBAIKAN: Gunakan fungsi dari ViewModel
            val errorMessage = viewModel.getDeepLinkErrorMessage(pendingDeepLink)
            if (errorMessage != null) {
                println("🔹 [RESET PASSWORD] Error deep link detected on screen load")
                viewModel.setErrorMessage(errorMessage)
                tokenStatus = TokenStatus.INVALID
                activity.markDeepLinkProcessed()
                return@LaunchedEffect
            }
        }

        // Lanjut dengan proses normal...
        // Cek token yang sudah disimpan
        val storedToken = viewModel.getStoredAccessToken(context)
        if (storedToken != null) {
            println("🔹 [RESET PASSWORD] Using stored access token: ${storedToken.take(20)}...")
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
            } catch (e: TimeoutCancellationException) {
                tokenStatus = TokenStatus.INVALID
                viewModel.setErrorMessage("Timeout saat memverifikasi token. Silakan coba lagi.")
                println("🔹 [RESET PASSWORD] ❌ Token verification timeout")
            } catch (e: Exception) {
                tokenStatus = TokenStatus.INVALID
                viewModel.setErrorMessage("Error memverifikasi token: ${e.message}")
                println("🔹 [RESET PASSWORD] ❌ Token verification error: ${e.message}")
            }
        } else {
            // Process deep link (non-error)
            val pendingDeepLink = activity?.getPendingDeepLinkInstance()
            if (pendingDeepLink != null) {
                println("🔹 [RESET PASSWORD] Processing deep link...")
                tokenStatus = TokenStatus.CHECKING

                viewModel.processPasswordResetDeepLink(
                    context = context,
                    uriString = pendingDeepLink.toString()
                ) { token ->
                    if (token != null) {
                        accessToken = token
                        tokenStatus = TokenStatus.VALID
                        println("🔹 [RESET PASSWORD] ✅ Token valid from deep link")
                    } else {
                        tokenStatus = TokenStatus.INVALID
                        println("🔹 [RESET PASSWORD] ❌ Token invalid from deep link")
                    }
                }
                activity?.markDeepLinkProcessed()
            } else {
                tokenStatus = TokenStatus.NOT_FOUND
                viewModel.setErrorMessage("Tidak ada link reset password yang aktif.")
            }
        }
    }

    // Handle success
    LaunchedEffect(resetPasswordSuccess) {
        if (resetPasswordSuccess) {
            println("🔹 [RESET PASSWORD] Password reset successful!")
            viewModel.clearRecoveryTokens(context) // 🔹 Pastikan pakai context

            // Show success message sebelum navigate
            delay(2000)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                        }
                        TokenStatus.INVALID -> {
                            Text(
                                text = "❌ Token tidak valid atau sudah kadaluarsa.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }
                        TokenStatus.NOT_FOUND -> {
                            Text(
                                text = "📧 Tidak ada token yang ditemukan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF757575),
                                modifier = Modifier.padding(bottom = 16.dp)
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

                    // Password fields hanya enabled jika token valid
                    val fieldsEnabled = tokenStatus == TokenStatus.VALID && !isLoading

                    Text(
                        text = "Masukkan password baru Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // New Password Field
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
                        enabled = fieldsEnabled,
                        isError = newPassword.isNotBlank() && newPassword.length < 6
                    )

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
                        enabled = fieldsEnabled,
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reset Button
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
                                accessToken == null -> {
                                    viewModel.setErrorMessage("Token tidak tersedia")
                                }
                                else -> {
                                    viewModel.clearErrorMessage()
                                    println("🔹 [RESET PASSWORD] Starting password reset with token...")
                                    viewModel.updatePasswordWithToken(
                                        newPassword = newPassword,
                                        accessToken = accessToken!!,
                                        onSuccess = {
                                            // Success handled by LaunchedEffect
                                        },
                                        onError = { error ->
                                            viewModel.setErrorMessage(error)
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
                                newPassword == confirmPassword
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

                    // Navigation buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text("Kembali ke Login", color = Color(0xFF4CAF50))
                    }

                    if (tokenStatus != TokenStatus.VALID) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                navController.navigate(Screen.Login.route)
                            }
                        ) {
                            Text("Request link reset baru", color = Color(0xFF757575))
                        }
                    }
                }
            }
        }
    }
}
