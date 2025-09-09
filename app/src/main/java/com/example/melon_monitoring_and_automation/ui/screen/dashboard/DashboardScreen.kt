package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.components.SensorCard
import com.example.melon_monitoring_and_automation.ui.screen.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))

    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            val userId = user.uid
            val systemId = "mainSystem"
            viewModel.fetchRealtimeData(userId, systemId)
        }
    }

    val hydroponicData by viewModel.hydroponicData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Selamat Datang, ${currentUser?.username ?: "Pengguna"}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Text(currentDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValue ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValue)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> LoadingIndicator()
                errorMessage != null -> ErrorDialog(message = errorMessage!!) {

                }
                hydroponicData != null -> {
                    HydroponicDashboardContent(hydroponicData!!)
                }
                else -> Text("Tidak ada data hidroponik yang tersedia")
            }
        }
    }
}

@Composable
fun HydroponicDashboardContent(data: HydroponicData) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            SensorCard(
                title = "Suhu & Kelembapan",
                value = "${data.temperature}°C / ${data.humidity}%",
                desc = "Kondisi Lingkungan"
            )
        }
        item {
            SensorCard(
                title = "pH Air",
                value = String.format("%.2f", data.ph),
                desc = "Kadar keasaman larutan"
            )
        }
        item {
            SensorCard(
                title = "EC (Nutrisi)",
                value = "${String.format("%.2f", data.ec)} mS/cm",
                desc = "Konsentrarsi nutrisi"
            )
        }
        item {
            SensorCard(
                title = "Level Air",
                value = data.waterLevel,
                desc = "Status ketinggian air"
            )
        }
        item {
            SensorCard(
                title = "Diperbarui terakhir",
                value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp)),
                desc = "Waktu pembaruan"
            )
        }
    }
}
