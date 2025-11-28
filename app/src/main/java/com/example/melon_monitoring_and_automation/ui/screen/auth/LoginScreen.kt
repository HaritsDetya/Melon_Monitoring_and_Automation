/**
 * LOGIN SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Memungkinkan user login dengan email dan password
 * - Menangani forgot password flow
 * - Navigasi ke register screen untuk user baru
 * - Memproses deep links untuk error handling dari email reset password
 *
 * Fitur:
 * - Form validation untuk email dan password
 * - Loading states selama proses login
 * - Error handling dan user feedback
 * - Password reset functionality
 * - Deep link processing untuk auth errors
 *
 * @author Your Name
 * @since Version 1.0
 * @param navController Navigator untuk berpindah screen
 * @param viewModel ViewModel yang menangani business logic autentikasi
 */

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.BuildConfig
import com.example.melon_monitoring_and_automation.MainActivity
import com.example.melon_monitoring_and_automation.R
import com.example.melon_monitoring_and_automation.ui.components.PasswordValidator
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // LOCAL STATE MANAGEMENT - Form inputs dan UI state
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var showLoginPasswordInfo by remember { mutableStateOf(false) }
    var isPasswordFocused by remember { mutableStateOf(false) }

    // VIEWMODEL STATE - Collect state dari AuthViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // ACTIVITY CONTEXT - Untuk deep link handling
    val context = LocalContext.current
    val activity = context as? MainActivity

    // PASSWORD INFO VISIBILITY - Tampilkan info ketika password focused
    if (isPasswordFocused && password.isNotEmpty()) {
        showLoginPasswordInfo = true
    }

    // NAVIGATION HANDLER - Redirect setelah login berhasil
    LaunchedEffect(authSuccess, currentUser) {
        if (authSuccess && currentUser != null && !isLoading) {
            println("🔹 [LOGIN SCREEN] ✅ Login successful, navigating to main app")
            delay(1000) // Beri waktu untuk user melihat feedback sukses
            navController.navigate("main_app_graph") {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    // DEEP LINK HANDLER - Process error dari password reset email
    LaunchedEffect(Unit) {
        val pendingDeepLink = activity?.getPendingDeepLinkInstance()
        if (pendingDeepLink?.fragment?.contains("error") == true) {
            println("🔹 [LOGIN SCREEN] Error deep link detected: ${pendingDeepLink.fragment}")
            val errorMessage = viewModel.parseDeepLinkError(pendingDeepLink.fragment!!)
            viewModel.setErrorMessage(errorMessage)
            activity.markDeepLinkProcessed()
        }
    }

    // CLEANUP HANDLER - Reset state ketika masuk login screen
    LaunchedEffect(Unit) {
        println("🔹 [LOGIN SCREEN] Cleaning up any pending deep links")
        viewModel.clearErrorMessage()
    }

    // MAIN UI LAYOUT
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                )
            )
    ) {
        // BACKGROUND DECORATION - Plant image untuk aesthetic
        Image(
            painter = painterResource(id = R.drawable.plant),
            contentDescription = "Leaf background decoration",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp),
            contentScale = ContentScale.Fit
        )

        // LOADING OVERLAY - Tampilkan selama proses login
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Sedang login...", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        // MAIN CONTENT COLUMN
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // HEADER SECTION
            Text(
                text = "Selamat Datang Kembali",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Masuk ke akun Anda untuk melanjutkan.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // EMAIL INPUT FIELD
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = Color.Gray) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = Color.Gray)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.height(16.dp))

            // PASSWORD INPUT FIELD
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = Color.Gray) },
                singleLine = true,
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
                        // Auto-submit ketika tekan done di keyboard
                        if (!isLoading && email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email, password)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ERROR MESSAGE DISPLAY
            if (!errorMessage.isNullOrEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = errorMessage ?: "Terjadi kesalahan",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SUCCESS MESSAGE DISPLAY
            if (authSuccess && currentUser != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Text(
                        text = "✅ Login berhasil! Mengarahkan ke dashboard...",
                        color = Color(0xFF388E3C),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // FORGOT PASSWORD LINK
            TextButton(
                modifier = Modifier.align(Alignment.Start),
                onClick = { showResetDialog = true },
                enabled = !isLoading
            ) {
                Text("Lupa Kata Sandi?", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // LOGIN BUTTON
            Button(
                onClick = {
                    viewModel.clearAllStates()
                    viewModel.login(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Login", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // REGISTER NAVIGATION
            TextButton(
                onClick = {
                    viewModel.clearAllStates()
                    navController.navigate(Screen.Register.route)
                },
                enabled = !isLoading
            ) {
                Text("Belum memiliki akun?", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
        }

        // RESET PASSWORD DIALOG
        if (showResetDialog) {
            ResetPasswordDialog(
                onDismiss = { showResetDialog = false },
                onSend = { resetEmail -> viewModel.sendPasswordResetEmail(resetEmail) },
                viewModel = viewModel
            )
        }
    }
}
