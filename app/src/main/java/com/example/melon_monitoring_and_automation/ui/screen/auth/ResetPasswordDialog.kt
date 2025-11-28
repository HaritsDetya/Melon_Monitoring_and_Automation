/**
 * RESET PASSWORD DIALOG COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan dialog untuk meminta email reset password
 * - Mengirim email reset password ke alamat yang dimasukkan user
 * - Memberikan feedback status pengiriman email (success/error)
 * - Auto-close dialog ketika email berhasil dikirim
 *
 * Fitur:
 * - Email input validation
 * - Loading state selama proses pengiriman
 * - Error handling dengan user-friendly messages
 * - Auto-dismiss pada success
 *
 * @author Your Name
 * @since Version 1.0
 * @param onDismiss Callback ketika dialog ditutup
 * @param onSend Callback ketika email berhasil dikirim
 * @param viewModel ViewModel untuk handle business logic
 */

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
    // LOCAL STATE - Email input dari user
    var emailInput by remember { mutableStateOf("") }

    // VIEWMODEL STATE - Collect state dari AuthViewModel
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val passwordResetSent by viewModel.passwordResetSent.collectAsState()

    val context = LocalContext.current

    // AUTO-CLOSE HANDLER - Tutup dialog otomatis ketika email terkirim
    LaunchedEffect(passwordResetSent) {
        if (passwordResetSent) {
            onDismiss()
            viewModel.clearErrorMessage()
        }
    }

    // DIALOG UI - Material3 AlertDialog
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
                // INSTRUCTION TEXT - Petunjuk untuk user
                Text(
                    "Masukkan alamat email yang terdaftar. Kami akan mengirimkan tautan untuk mereset kata sandi Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // EMAIL INPUT FIELD
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
                    isError = !errorMessage.isNullOrEmpty() // Tampilkan error state
                )

                // ERROR MESSAGE DISPLAY
                if (!errorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // SUCCESS MESSAGE DISPLAY
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
            // SEND BUTTON - Kirim email reset
            Button(
                onClick = {
                    viewModel.clearErrorMessage()
                    viewModel.sendPasswordResetEmail(emailInput, context)
                },
                enabled = emailInput.isNotBlank() && !isLoading && !passwordResetSent,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isLoading) {
                    // LOADING INDICATOR - Tampilkan selama proses
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                    )
                } else {
                    Text("Kirim Tautan")
                }
            }
        },
        dismissButton = {
            // CANCEL BUTTON - Tutup dialog
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
