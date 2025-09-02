package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.components.SensorCard
import com.example.melon_monitoring_and_automation.ui.screen.auth.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()

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
            TopAppBar(title = { Text("Dashboard Hydroponic") })
        }
    ) { paddingValue ->
        Box(
            modifier = Modifier
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
            Text(
                text = "Diperbarui terakhir: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(data.timestamp)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
