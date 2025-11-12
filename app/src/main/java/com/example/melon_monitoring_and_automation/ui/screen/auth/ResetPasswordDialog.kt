package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordDialog(
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var emailInput by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val passwordResetSent by viewModel.passwordResetSent.collectAsState()

    // 🔹 TAMBAHKAN: Dapatkan context
    val context = LocalContext.current

    // Auto-close dialog ketika reset password berhasil dikirim
    LaunchedEffect(passwordResetSent) {
        if (passwordResetSent) {
            onDismiss()
            viewModel.clearErrorMessage()
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            viewModel.clearErrorMessage()
        },
        title = {
            Text(
                "Reset Kata Sandi",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    "Masukkan alamat email yang terdaftar. Kami akan mengirimkan tautan untuk mereset kata sandi Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    placeholder = { Text("contoh@email.com") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = Color.Gray
                        )
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !errorMessage.isNullOrEmpty()
                )

                if (!errorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (passwordResetSent) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✅ Tautan reset telah dikirim ke email Anda",
                        color = Color(0xFF388E3C),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.clearErrorMessage()
                    // 🔹 PERBAIKAN: Gunakan fungsi dengan context
                    viewModel.sendPasswordResetEmail(emailInput, context)
                },
                enabled = emailInput.isNotBlank() && !isLoading && !passwordResetSent,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(16.dp)
                    )
                } else {
                    Text("Kirim Tautan")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    viewModel.clearErrorMessage()
                }
            ) {
                Text("Batal")
            }
        }
    )
}
