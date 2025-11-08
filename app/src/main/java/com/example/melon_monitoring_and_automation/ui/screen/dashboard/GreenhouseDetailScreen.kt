package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.domain.model.ChartDataPoint
import com.example.melon_monitoring_and_automation.domain.model.ChartType
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.TimeRange
import com.example.melon_monitoring_and_automation.ui.components.SensorLineChart
import com.example.melon_monitoring_and_automation.ui.viewmodel.ChartViewModel
import com.example.melon_monitoring_and_automation.ui.viewmodel.GreenhouseViewModel
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenhouseDetailScreen(
    greenhouseId: String,
    onBackClick: () -> Unit,
    viewModel: GreenhouseViewModel = hiltViewModel()
) {
    val greenhouse by viewModel.getGreenhouseById(greenhouseId).collectAsState(initial = null)
    val sensorReadings by viewModel.getLatestSensorReadings(greenhouseId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greenhouse?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Greenhouse Info Section
            GreenhouseInfoSection(greenhouse)

            // Current Sensor Readings
            SensorReadingsSection(sensorReadings)

            // Chart Section
            SensorChartSection(greenhouseId = greenhouseId)
        }
    }
}

@Composable
fun GreenhouseInfoSection(greenhouse: com.example.melon_monitoring_and_automation.domain.model.Greenhouse?) {
    greenhouse?.let {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF388E3C),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lokasi: ${it.location}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Deskripsi: ${it.description ?: "Tidak ada deskripsi"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SensorReadingsSection(sensorReadings: com.example.melon_monitoring_and_automation.domain.model.SensorReadings?) {
    sensorReadings?.let { readings ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pembacaan Sensor Terkini",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Temperature
                ReadingItem("Suhu Udara", readings.temperature?.toString() ?: "N/A", "°C")
                Spacer(modifier = Modifier.height(8.dp))

                // Humidity
                ReadingItem("Kelembapan", readings.humidity?.toString() ?: "N/A", "%")
                Spacer(modifier = Modifier.height(8.dp))

                // Water Temperature
                ReadingItem("Suhu Air", readings.waterTemp?.toString() ?: "N/A", "°C")
                Spacer(modifier = Modifier.height(8.dp))

                // pH
                ReadingItem("pH", readings.ph?.toString() ?: "N/A", "pH")
                Spacer(modifier = Modifier.height(8.dp))

                // TDS
                ReadingItem("TDS", readings.tds?.toString() ?: "N/A", "ppm")
            }
        }
    }
}

@Composable
fun ReadingItem(label: String, value: String, unit: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SensorChartSection(
    greenhouseId: String
) {
    var showChart by remember { mutableStateOf(false) }

    // Gunakan LaunchedEffect dengan delay untuk prevent immediate crash
    LaunchedEffect(greenhouseId) {
        delay(500) // Delay untuk memastikan screen sudah fully loaded
        showChart = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showChart) {
            SafeChartImplementation(greenhouseId = greenhouseId)
        } else {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SafeChartImplementation(greenhouseId: String) {
    val viewModel: ChartViewModel = hiltViewModel()
    val chartData by viewModel.chartData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // 🔹 STATE untuk menangani chart errors
    var chartError by remember { mutableStateOf<String?>(null) }

    // Load data sekali saja saat pertama kali
    LaunchedEffect(Unit) {
        try {
            viewModel.loadChartData(greenhouseId)
        } catch (e: Exception) {
            chartError = "Gagal memuat data chart: ${e.message}"
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Simple controls tanpa filter chips dulu
        Text(
            "Grafik Sensor",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        // Error Message dari ViewModel
        if (!errorMessage.isNullOrEmpty()) {
            ErrorMessageCard(message = errorMessage!!) {
                viewModel.clearErrorMessage()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Error Message dari Chart
        if (!chartError.isNullOrEmpty()) {
            ErrorMessageCard(message = chartError!!) {
                chartError = null
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Chart Display dengan state-based rendering
        if (isLoading) {
            LoadingChartPlaceholder()
        } else if (!chartError.isNullOrEmpty()) {
            ChartErrorFallback(errorMessage = chartError!!, dataSize = chartData.size)
        } else {
            // 🔹 GUARD CLAUSE untuk prevent invalid data
            SafeSensorLineChart(
                dataPoints = chartData,
                yAxisLabel = viewModel.getYAxisLabel(),
                chartTitle = viewModel.getChartTitle(),
                onError = { error -> chartError = error }
            )
        }
    }
}

// 🔹 COMPOSABLE YANG AMAN dengan validation internal
@Composable
fun SafeSensorLineChart(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    chartTitle: String,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Validasi data sebelum render
    val safeDataPoints = remember(dataPoints) {
        if (dataPoints.any { it.y.isNaN() || it.y.isInfinite() }) {
            onError("Data chart mengandung nilai tidak valid")
            emptyList()
        } else {
            dataPoints
        }
    }

    if (safeDataPoints.isEmpty()) {
        ChartErrorFallback(errorMessage = "Data tidak valid untuk ditampilkan", dataSize = dataPoints.size)
    } else {
        SensorLineChart(
            dataPoints = safeDataPoints,
            yAxisLabel = yAxisLabel,
            chartTitle = chartTitle,
            modifier = modifier
        )
    }
}

// 🔹 COMPONENT UNTUK ERROR MESSAGE
@Composable
fun ErrorMessageCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    }
}

// 🔹 COMPONENT UNTUK LOADING STATE
@Composable
fun LoadingChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Memuat data chart...")
        }
    }
}

// 🔹 COMPONENT UNTUK ERROR FALLBACK
@Composable
fun ChartErrorFallback(errorMessage: String, dataSize: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Chart Tidak Dapat Ditampilkan",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF757575)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Data tersedia: $dataSize points",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: Double, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%.1f".format(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF388E3C)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

// Helper functions
private fun getSensorTypeDisplayName(sensorType: SensorType): String {
    return when (sensorType) {
        SensorType.TEMPERATURE -> "Suhu"
        SensorType.HUMIDITY -> "Kelembapan"
        SensorType.WATER_TEMPERATURE -> "Suhu Air"
        SensorType.PH -> "pH"
        SensorType.TDS -> "TDS"
    }
}

private fun getTimeRangeDisplayName(timeRange: TimeRange): String {
    return when (timeRange) {
        TimeRange.HOURS_24 -> "24 Jam"
        TimeRange.DAYS_7 -> "7 Hari"
        TimeRange.DAYS_30 -> "30 Hari"
    }
}

private fun getSensorUnit(sensorType: SensorType): String {
    return when (sensorType) {
        SensorType.TEMPERATURE -> "°C"
        SensorType.HUMIDITY -> "%"
        SensorType.WATER_TEMPERATURE -> "°C"
        SensorType.PH -> "pH"
        SensorType.TDS -> "ppm"
    }
}
