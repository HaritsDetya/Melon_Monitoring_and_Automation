package com.example.melon_monitoring_and_automation.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historicalData by viewModel.historicalData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Riwayat Data Sensor") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                isLoading -> LoadingIndicator()
                errorMessage != null -> ErrorDialog(message = errorMessage!!) {

                }
                historicalData.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(historicalData) { data ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Waktu: ${SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(data.timestamp)}")
                                    Text("Suhu: ${data.temperature}°C, Kelembapan: ${data.humidity}%")
                                    Text("pH: ${String.format("%.2f", data.ph)}, EC: ${String.format("%.2f", data.ec)} mS/cm")
                                    Text("Level Air: ${data.waterLevel}")
                                }
                            }
                        }
                    }
                }
                else -> Text("Tidak ada riwayat data yang tersedia.")
            }
        }
    }
}