package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import com.example.melon_monitoring_and_automation.ui.viewmodel.GreenhouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGreenhouseClick: (String) -> Unit,
    viewModel: GreenhouseViewModel = hiltViewModel()
) {
    val greenhouses by viewModel.greenhouses.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Greenhouse Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50)
                )
            )
        }
    ) { padding ->
        if (isLoading && greenhouses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (greenhouses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tidak ada greenhouse",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                items(greenhouses) { greenhouse ->
                    val readings = sensorReadings[greenhouse.id]
                    GreenhouseCard(
                        greenhouse = greenhouse,
                        sensorReadings = readings,
                        onClick = { onGreenhouseClick(greenhouse.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun GreenhouseCard(
    greenhouse: Greenhouse,
    sensorReadings: SensorReadings?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = greenhouse.name,
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = greenhouse.location,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            // Tampilkan data sensor terkini jika tersedia
            sensorReadings?.let { readings ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Data Terkini:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SensorValueItem(
                        label = "Suhu",
                        value = readings.temperature,
                        unit = "°C"
                    )
                    SensorValueItem(
                        label = "Kelembapan",
                        value = readings.humidity,
                        unit = "%"
                    )
                    SensorValueItem(
                        label = "pH",
                        value = readings.ph,
                        unit = "pH"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Klik untuk detail →",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun SensorValueItem(label: String, value: Double?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value?.let { "%.1f".format(it) } ?: "N/A",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF388E3C)
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
