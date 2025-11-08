package com.example.melon_monitoring_and_automation.ui.screen.auth

import android.content.Context
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.core.net.toUri
import androidx.core.content.edit

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

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val deepLinkProcessed by viewModel.deepLinkProcessed.collectAsState()

    // 🔹 FIX: Process deep link dengan context
    LaunchedEffect(Unit) {
        println("🔹 [RESET PASSWORD] Screen initialized")

        // Cek apakah ada token yang sudah disimpan
        val storedToken = viewModel.getStoredAccessToken(context)
        if (storedToken != null) {
            println("🔹 [RESET PASSWORD] Using stored access token")
            accessToken = storedToken
        } else {
            // Process deep link untuk mendapatkan token
            val pendingDeepLink = activity?.getPendingDeepLinkInstance()
            if (pendingDeepLink != null) {
                println("🔹 [RESET PASSWORD] 📨 Processing deep link for token...")
                viewModel.processPasswordResetDeepLink(
                    context = context,
                    uriString = pendingDeepLink.toString()
                ) { token ->
                    accessToken = token
                    if (token != null) {
                        println("🔹 [RESET PASSWORD] ✅ Token received: ${token.take(10)}...")
                    } else {
                        println("🔹 [RESET PASSWORD] ❌ Failed to get token")
                        viewModel.setErrorMessage("Gagal mendapatkan token dari link")
                    }
                }
                activity.markDeepLinkProcessed()
            } else {
                viewModel.setErrorMessage("Tidak ada link reset password yang aktif")
            }
        }
    }

    // Handle success
    LaunchedEffect(authSuccess) {
        if (authSuccess) {
            println("🔹 [RESET PASSWORD] Password reset successful, navigating to login...")
            delay(2000)
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // Handle successful deep link processing
    LaunchedEffect(deepLinkProcessed) {
        if (deepLinkProcessed) {
            println("🔹 [RESET PASSWORD] Deep link processed successfully, session ready")
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") }
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

                    // Status info
                    if (accessToken != null) {
                        Text(
                            text = "✅ Token valid. Silakan masukkan password baru.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF388E3C),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    } else {
                        Text(
                            text = "⏳ Menunggu token reset password...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF57C00),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

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
                        enabled = accessToken != null && !isLoading
                    )

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
                        enabled = accessToken != null && !isLoading
                    )

                    // Error Message
                    if (!errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Success Message
                    if (authSuccess) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "✅ Password berhasil direset! Mengarahkan ke login...",
                            color = Color(0xFF388E3C),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reset Button
                    Button(
                        onClick = {
                            when {
                                newPassword.isBlank() || confirmPassword.isBlank() -> {
                                    viewModel.setErrorMessage("Harap isi semua field")
                                }
                                newPassword != confirmPassword -> {
                                    viewModel.setErrorMessage("Password tidak cocok")
                                }
                                newPassword.length < 6 -> {
                                    viewModel.setErrorMessage("Password minimal 6 karakter")
                                }
                                accessToken == null -> {
                                    viewModel.setErrorMessage("Token tidak tersedia")
                                }
                                else -> {
                                    viewModel.clearErrorMessage()
                                    println("🔹 [RESET PASSWORD] Using direct API call with token")
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        enabled = accessToken != null && !isLoading && newPassword.isNotBlank() && confirmPassword.isNotBlank()
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

                    // Back to Login
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Text(
                            "Kembali ke Login",
                            color = Color(0xFF4CAF50)
                        )
                    }

                    // Request new link
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Login.route)
                        }
                    ) {
                        Text(
                            "Request link reset baru",
                            color = Color(0xFF757575)
                        )
                    }
                }
            }
        }
    }
}
