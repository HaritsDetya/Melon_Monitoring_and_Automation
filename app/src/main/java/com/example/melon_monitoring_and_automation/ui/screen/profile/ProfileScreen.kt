package com.example.melon_monitoring_and_automation.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.User
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import com.example.melon_monitoring_and_automation.ui.viewmodel.GreenhouseViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    greenhouseViewModel: GreenhouseViewModel = hiltViewModel()
) {
    // 🔹 FIX: Gunakan collectAsStateWithLifecycle untuk semua state
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val greenhouses by greenhouseViewModel.greenhouses.collectAsStateWithLifecycle()
    val authSuccess by viewModel.authSuccess.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var manualLogoutTriggered by remember { mutableStateOf(false) }

    // 🔹 FIX: Load user data dan greenhouse data saat screen dibuka
    LaunchedEffect(Unit) {
        println("🔹 [PROFILE] Loading profile data")

        // Load current user data
        viewModel.loadCurrentUser()

        // Load user's greenhouses
        greenhouseViewModel.loadUserGreenhouses()
    }

    // 🔹 FIX: Navigation trigger ketika logout berhasil
    LaunchedEffect(authSuccess, currentUser, manualLogoutTriggered) {
        println("🔹 [PROFILE] Navigation Check:")
        println("🔹   - authSuccess: $authSuccess")
        println("🔹   - currentUser: ${currentUser?.email ?: "null"}")
        println("🔹   - isLoading: $isLoading")

        if (manualLogoutTriggered && currentUser == null && !isLoading) {
            println("🔹 [PROFILE] 🚀 Manual logout confirmed, navigating to login")
            delay(1000)
            navController.navigate(Screen.Login.route) {
                popUpTo("main_app_graph") { inclusive = true }
            }
            manualLogoutTriggered = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF388E3C)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Memuat data...")
                    }
                }
            }

            // User Info Section
            UserInfoSection(user = currentUser)

            // User's Greenhouses
            UserGreenhousesSection(greenhouses = greenhouses)

            // Action Buttons
            ProfileActionsSection(
                onLogout = {
                    println("🔹 [PROFILE] Logout button clicked")
                    manualLogoutTriggered = true
                    viewModel.logout()
                },
                onDeleteAccount = {
                    println("🔹 [PROFILE] Delete account clicked")
                    // Handle delete account
                }
            )
        }
    }
}

@Composable
fun UserInfoSection(user: User?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Informasi Pengguna",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                user != null -> {
                    // User data available
                    InfoItem(
                        icon = Icons.Default.Person,
                        label = "Username",
                        value = user.username
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    InfoItem(
                        icon = Icons.Default.Email,
                        label = "Email",
                        value = user.email
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    InfoItem(
                        icon = Icons.Default.Phone,
                        label = "Nomor Telepon",
                        value = user.phoneNumber ?: "Tidak tersedia"
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    InfoItem(
                        icon = null,
                        label = "Bergabung sejak",
                        value = if (user.createdAt.length >= 10) {
                            user.createdAt.substring(0, 10)
                        } else {
                            user.createdAt
                        }
                    )
                }
                else -> {
                    // No user data
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Data pengguna tidak tersedia",
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Pastikan Anda sudah login",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(icon: ImageVector?, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.padding(end = 16.dp),
                tint = Color.Gray
            )
        } ?: run {
            Spacer(modifier = Modifier.padding(end = 40.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UserGreenhousesSection(greenhouses: List<Greenhouse>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Greenhouse Saya",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (greenhouses.isEmpty()) {
                Text(
                    "Belum ada greenhouse",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                greenhouses.forEach { greenhouse ->
                    GreenhouseProfileCard(greenhouse = greenhouse)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun GreenhouseProfileCard(greenhouse: Greenhouse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    greenhouse.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    greenhouse.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                greenhouse.description?.let { description ->
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileActionsSection(
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Aksi Akun",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Logout")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete Account Button
            TextButton(
                onClick = { showDeleteConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Hapus Akun",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Hapus Akun") },
            text = {
                Text("Apakah Anda yakin ingin menghapus akun? Tindakan ini tidak dapat dibatalkan.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
