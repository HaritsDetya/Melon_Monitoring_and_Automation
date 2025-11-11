package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

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

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // 🔹 FIX: Enhanced navigation with delay for better UX
    LaunchedEffect(authSuccess, currentUser) {
        println("🔹 [REGISTER SCREEN] Navigation Check:")
        println("🔹   - authSuccess: $authSuccess")
        println("🔹   - currentUser: $currentUser")
        println("🔹   - isLoading: $isLoading")

        if (authSuccess && currentUser != null && !isLoading) {
            println("🔹 [REGISTER SCREEN] ✅ Registration successful, waiting a moment...")

            // Small delay to show success message
            delay(1500)

            println("🔹 [REGISTER SCREEN] 🚀 Navigating to Dashboard...")

            // Clear the back stack and navigate to main app
            navController.navigate("main_app_graph") {
                popUpTo(Screen.Register.route) {
                    inclusive = true
                }
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
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Username Icon",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
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
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Email Icon",
                        tint = Color.Gray
                    )
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
                    // Filter hanya angka dan tanda plus
                    phoneNumber = it.filter { char ->
                        char.isDigit() || char == '+'
                    }
                },
                label = { Text("Nomor Telepon", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone Icon",
                        tint = Color.Gray
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                placeholder = {
                    Text("Contoh: +628123456789", color = Color.Gray.copy(alpha = 0.6f))
                },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Password Icon",
                        tint = Color.Gray
                    )
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        viewModel.register(username, email, password, phoneNumber)
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Error Message
            if (!errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage ?: "Terjadi kesalahan yang tidak diketahui.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Register Button
            Button(
                onClick = {
                    println("🔹 Register attempt:")
                    println("🔹 Username: $username")
                    println("🔹 Email: $email")
                    println("🔹 Phone: $phoneNumber")
                    println("🔹 Password length: ${password.length}")

                    viewModel.register(username, email, password, phoneNumber)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isLoading && isFormValid(username, email, password, phoneNumber)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
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
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text(
                    "Sudah memiliki akun?",
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper function untuk validasi form
private fun isFormValid(
    username: String,
    email: String,
    password: String,
    phoneNumber: String
): Boolean {
    return username.isNotBlank() &&
            email.isNotBlank() &&
            password.length >= 6 && // Minimum 6 karakter untuk password
            phoneNumber.isNotBlank()
}

// Extension function untuk validasi email sederhana
private fun String.isValidEmail(): Boolean {
    return this.contains("@") && this.contains(".")
}

// Extension function untuk validasi nomor telepon sederhana
private fun String.isValidPhoneNumber(): Boolean {
    // Minimal 10 digit, maksimal 15 digit (termasuk kode negara)
    return this.length in 10..15 && this.all { it.isDigit() || it == '+' }
}
