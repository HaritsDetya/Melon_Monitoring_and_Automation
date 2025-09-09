package com.example.melon_monitoring_and_automation.ui.screen.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
    userId: String,
    systemId: String
) {
    LaunchedEffect(userId, systemId) {
        if (userId.isNotEmpty() && systemId.isNotEmpty()) {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - (7 * 24 * 60 * 60 * 1000)
            viewModel.fetchHistoricalData(userId, systemId, startTime, endTime)
        }
    }

    val historicalData by viewModel.historicalData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Riwayat Data Sensor") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp, 8.dp, 16.dp, 16.dp),
            contentAlignment = Alignment.Center
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
                                    Text("Waktu: ${SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(data.timestamp))}")
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
