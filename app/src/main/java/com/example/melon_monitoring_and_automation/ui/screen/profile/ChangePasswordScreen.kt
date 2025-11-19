package com.example.melon_monitoring_and_automation.ui.screen.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.ui.components.PasswordValidator
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPasswordRequirements by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val changePasswordSuccess by viewModel.changePasswordSuccess.collectAsState()

    val darkGreen = Color(0xFF2E7D32)

    // Set status bar
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // Reset state ketika screen pertama kali dibuka
    LaunchedEffect(Unit) {
        viewModel.resetChangePasswordState()
        viewModel.clearErrorMessage()
    }

    // Handle success
    LaunchedEffect(changePasswordSuccess) {
        if (changePasswordSuccess) {
            println("🔹 [CHANGE PASSWORD] Password changed successfully!")

            // Show success message sebelum navigate back
            delay(1500)
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubah Password") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White)
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
                        text = "Ubah Password",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Masukkan password saat ini dan password baru Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Current Password Field
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Password Saat Ini") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = "Current Password")
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 🔹 PERBAIKAN: New Password Field dengan Validasi Strength
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                if (it.isNotEmpty()) showPasswordRequirements = true
                            },
                            label = { Text("Password Baru") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "New Password")
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading,
                            isError = newPassword.isNotBlank() && (
                                    newPassword.length < 6 ||
                                            !PasswordValidator.isPasswordStrong(newPassword)
                                    )
                        )

                        // 🔹 TAMBAHKAN: Password Strength Indicator
                        if (newPassword.isNotBlank()) {
                            val strength = PasswordValidator.getPasswordStrength(newPassword)
                            val strengthColor = PasswordValidator.getStrengthColor(strength)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Kekuatan password: $strength",
                                    color = strengthColor,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )

                                // Progress bar sederhana
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                ) {
                                    val progress = when (strength) {
                                        "Kuat" -> 1f
                                        "Sedang" -> 0.66f
                                        else -> 0.33f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(progress)
                                            .height(4.dp)
                                            .background(strengthColor, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }

                        // Validasi panjang password
                        if (newPassword.isNotBlank() && newPassword.length < 6) {
                            Text(
                                text = "Password minimal 6 karakter",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }

                        // Validasi strength password
                        if (newPassword.isNotBlank() && newPassword.length >= 6 &&
                            !PasswordValidator.isPasswordStrong(newPassword)) {
                            Text(
                                text = "Password harus mengandung huruf besar, kecil, dan angka",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }
                    }

                    // 🔹 TAMBAHKAN: Password Requirements List
                    if (showPasswordRequirements && newPassword.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Requirements:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                PasswordValidator.getPasswordRequirements(newPassword).forEach { (requirement, met) ->
                                    Text(
                                        text = "• $requirement",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (met) Color(0xFF388E3C) else Color(0xFF757575),
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Confirm Password Field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Konfirmasi Password Baru") },
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
                    if (changePasswordSuccess) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Text(
                                text = "✅ Password berhasil diubah!",
                                color = Color(0xFF388E3C),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Change Password Button
                    Button(
                        onClick = {
                            when {
                                currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                                    viewModel.setErrorMessage("Harap isi semua field")
                                }
                                newPassword.length < 6 -> {
                                    viewModel.setErrorMessage("Password baru minimal 6 karakter")
                                }
                                !PasswordValidator.isPasswordStrong(newPassword) -> {
                                    viewModel.setErrorMessage("Password harus mengandung huruf besar, kecil, dan angka")
                                }
                                newPassword != confirmPassword -> {
                                    viewModel.setErrorMessage("Password baru tidak cocok")
                                }
                                else -> {
                                    viewModel.clearErrorMessage()
                                    println("🔹 [CHANGE PASSWORD] Starting password change...")
                                    viewModel.changePassword(
                                        currentPassword = currentPassword,
                                        newPassword = newPassword,
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
                        enabled = !isLoading &&
                                currentPassword.isNotBlank() &&
                                newPassword.isNotBlank() &&
                                confirmPassword.isNotBlank() &&
                                newPassword.length >= 6 &&
                                PasswordValidator.isPasswordStrong(newPassword) && // 🔹 TAMBAHKAN validasi strength
                                newPassword == confirmPassword
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Ubah Password", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel Button
                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF4CAF50)
                        ),
                        enabled = !isLoading
                    ) {
                        Text("Batal", color = Color(0xFF4CAF50), fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
