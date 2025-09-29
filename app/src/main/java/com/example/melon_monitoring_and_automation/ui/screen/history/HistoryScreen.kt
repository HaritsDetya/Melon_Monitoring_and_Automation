package com.example.melon_monitoring_and_automation.ui.screen.history

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val historicalDataState by viewModel.historicalDataState.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()

    LaunchedEffect(activeGreenhouseId) {
        if (activeGreenhouseId != null) {
            viewModel.loadHistoricalData(activeGreenhouseId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Riwayat Data Sensor") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (activeGreenhouseId != null) {
                when (historicalDataState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> {
                        val message = (historicalDataState as UiState.Error).message
                        ErrorDialog(message = message) {
                            viewModel.clearError()
                        }
                    }
                    is UiState.Success -> {
                        val historicalData = (historicalDataState as UiState.Success).data
                        if (historicalData.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = historicalData,
                                    key = { it.first }
                                ) { (timestamp, data) ->
                                    HistoryItemCard(timestamp, data)
                                }
                            }
                        } else {
                            Text("Tidak ada riwayat data yang tersedia.")
                        }
                    }
                }
            } else {
                Text("Pilih Greenhouse untuk melihat riwayat data.")
            }
        }
    }
}

@Composable
private fun HistoryItemCard(timestamp: Long, data: SensorReading) {
    val formattedDate = remember(timestamp) {
        SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Waktu: $formattedDate")
            Text("Suhu: ${data.temperature}°C, Kelembapan: ${data.humidity}%")
            Text("pH: ${"%.2f".format(data.ph)}, EC: ${"%.2f".format(data.ec)} mS/cm")
        }
    }
}
