package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.melon_monitoring_and_automation.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.ui.components.PasswordValidator
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

// Di RegisterScreen.kt - PERBAIKI dengan approach yang benar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var showPasswordRequirements by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Auto-show requirements ketika password focused dan tidak empty
    if (isPasswordFocused && password.isNotEmpty()) {
        showPasswordRequirements = true
    }

    // 🔹 FIX: Enhanced navigation with delay for better UX
    LaunchedEffect(authSuccess, currentUser) {
        if (authSuccess && currentUser != null && !isLoading) {
            delay(1500)
            navController.navigate("main_app_graph") {
                popUpTo(Screen.Register.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                )
            )
    ) {
        Image(
            painter = painterResource(id = R.drawable.plant),
            contentDescription = "Plant pots background",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 50.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Daftar Akun",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Buat akun baru untuk memulai.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = "Username Icon", tint = Color.Gray)
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color.Gray)
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    phoneNumber = it.filter { char -> char.isDigit() || char == '+' }
                },
                label = { Text("Nomor Telepon", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = "Phone Icon", tint = Color.Gray)
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                placeholder = { Text("Contoh: +628123456789", color = Color.Gray.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 IMPROVED: Password Field dengan Strength Indicator
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (it.isNotEmpty()) showPasswordRequirements = true
                    },
                    label = { Text("Password", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = Color.Gray)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isFormValid(username, email, password, phoneNumber)) {
                                viewModel.register(username, email, password, phoneNumber)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    isError = password.isNotBlank() && !PasswordValidator.isPasswordStrong(password),
                    // 🔹 FIX: Gunakan interactionSource untuk track focus
                    interactionSource = remember { MutableInteractionSource() }.also { interactionSource ->
                        LaunchedEffect(interactionSource) {
                            interactionSource.interactions.collect { interaction ->
                                when (interaction) {
                                    is FocusInteraction.Focus -> {
                                        isPasswordFocused = true
                                        if (password.isNotEmpty()) showPasswordRequirements = true
                                    }
                                    is FocusInteraction.Unfocus -> {
                                        isPasswordFocused = false
                                        if (password.isEmpty()) showPasswordRequirements = false
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                )

                // 🔹 TAMBAHKAN: Password Strength Indicator
                if (password.isNotBlank()) {
                    val strength = PasswordValidator.getPasswordStrength(password)
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
            }

            // 🔹 TAMBAHKAN: Password Requirements List
            if (showPasswordRequirements) {
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
                        PasswordValidator.getPasswordRequirements(password).forEach { (requirement, met) ->
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

            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (!errorMessage.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan yang tidak diketahui.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Register Button
            Button(
                onClick = {
                    viewModel.register(username, email, password, phoneNumber)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isLoading && isFormValidWithStrength(username, email, password, phoneNumber)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Daftar", color = Color.White, fontSize = 16.sp)
                }
            }

            // Show success message
            if (authSuccess && currentUser != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✅ Registrasi berhasil! Mengarahkan ke dashboard...",
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    "Sudah memiliki akun?",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 🔹 PERBAIKI: Validasi form dengan strength requirement
private fun isFormValidWithStrength(
    username: String,
    email: String,
    password: String,
    phoneNumber: String
): Boolean {
    return username.isNotBlank() &&
            email.isNotBlank() &&
            email.contains("@") && email.contains(".") &&
            password.length >= 6 &&
            PasswordValidator.isPasswordStrong(password) && // 🔹 TAMBAHKAN strength validation
            phoneNumber.isNotBlank() &&
            phoneNumber.length in 10..15
}

// 🔹 BUAT: Validasi form dasar (untuk backward compatibility)
private fun isFormValid(
    username: String,
    email: String,
    password: String,
    phoneNumber: String
): Boolean {
    return username.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 &&
            phoneNumber.isNotBlank()
}
