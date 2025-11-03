package com.example.melon_monitoring_and_automation.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import kotlinx.coroutines.withContext

@Composable
fun ResetPasswordScreen(
    navController: NavController,
    supabaseClient: SupabaseClient
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Reset Your Password", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") }
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") }
        )

        Spacer(Modifier.height(16.dp))

        Button(onClick = {
            if (newPassword == confirmPassword && newPassword.isNotBlank()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        supabaseClient.auth.updateUser {
                            password = newPassword
                        }
                        withContext(Dispatchers.Main) {
                            message = "Password updated successfully!"
                            navController.navigate("login")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            message = e.message ?: "Error updating password"
                        }
                    }
                }
            } else {
                message = "Passwords do not match!"
            }
        }) {
            Text("Reset Password")
        }

        Spacer(Modifier.height(8.dp))
        Text(message)
    }
}
