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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
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
import com.example.melon_monitoring_and_automation.domain.model.DateRange
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.TimeRange
import com.example.melon_monitoring_and_automation.ui.components.CustomDateRangeChip
import com.example.melon_monitoring_and_automation.ui.components.DateRangePicker
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
    val chartConfig by viewModel.chartConfig.collectAsState()

    // ✅ PERBAIKAN: Tambah state untuk date range picker
    val showDateRangePicker by viewModel.showDateRangePicker.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val weeklyRanges by viewModel.weeklyRanges.collectAsState()

    var chartError by remember { mutableStateOf<String?>(null) }

    // Load data dan available months
    LaunchedEffect(chartConfig.selectedSensorType, chartConfig.timeRange, chartConfig.customDateRange) {
        try {
            viewModel.loadChartData(greenhouseId)
        } catch (e: Exception) {
            chartError = "Gagal memuat data chart: ${e.message}"
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadAvailableMonths(greenhouseId)
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ✅ PERBAIKAN: Update ChartControls dengan custom range
            EnhancedChartControls(
                selectedSensorType = chartConfig.selectedSensorType,
                selectedTimeRange = chartConfig.timeRange,
                customDateRange = chartConfig.customDateRange,
                onSensorTypeChanged = { viewModel.updateSensorType(it) },
                onTimeRangeChanged = { viewModel.updateTimeRange(it) },
                onCustomRangeClicked = { viewModel.showDateRangePicker() }
            )

            // Date Range Picker Overlay
            DateRangePicker(
                showDateRangePicker = showDateRangePicker,
                availableMonths = availableMonths,
                selectedMonth = selectedMonth,
                weeklyRanges = weeklyRanges,
                selectedTimeRange = chartConfig.timeRange,
                customDateRange = chartConfig.customDateRange,
                onMonthSelected = { viewModel.selectMonth(it) },
                onDateRangeSelected = { viewModel.updateCustomDateRange(it) },
                onClose = { viewModel.hideDateRangePicker() },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Error messages dan chart content (sama seperti sebelumnya)
            if (!errorMessage.isNullOrEmpty()) {
                ErrorMessageCard(message = errorMessage!!) {
                    viewModel.clearErrorMessage()
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (!chartError.isNullOrEmpty()) {
                ErrorMessageCard(message = chartError!!) {
                    chartError = null
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                LoadingChartPlaceholder()
            } else if (!chartError.isNullOrEmpty()) {
                ChartErrorFallback(errorMessage = chartError!!, dataSize = chartData.size)
            } else if (chartData.isEmpty()) {
                EmptyChartPlaceholder()
            } else {
                SafeSensorLineChart(
                    dataPoints = chartData,
                    yAxisLabel = viewModel.getYAxisLabel(),
                    chartTitle = viewModel.getChartTitle(),
                    onError = { error -> chartError = error }
                )
            }
        }
    }
}

// ✅ PERBAIKAN: Enhanced Chart Controls dengan custom range
@Composable
fun EnhancedChartControls(
    selectedSensorType: SensorType,
    selectedTimeRange: TimeRange,
    customDateRange: DateRange?,
    onSensorTypeChanged: (SensorType) -> Unit,
    onTimeRangeChanged: (TimeRange) -> Unit,
    onCustomRangeClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Grafik Sensor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Sensor Type Filter
        Text(
            "Jenis Sensor:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            SensorType.entries.forEach { sensorType ->
                FilterChip(
                    selected = selectedSensorType == sensorType,
                    onClick = { onSensorTypeChanged(sensorType) },
                    label = { Text(getSensorTypeDisplayName(sensorType)) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Time Range Filter dengan Custom Option
        Text(
            "Rentang Waktu:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            // Predefined ranges
            TimeRange.entries.forEach { timeRange ->
                if (timeRange != TimeRange.CUSTOM) {
                    FilterChip(
                        selected = selectedTimeRange == timeRange,
                        onClick = { onTimeRangeChanged(timeRange) },
                        label = {
                            Text(
                                getTimeRangeDisplayName(timeRange)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            // Custom range chip
            CustomDateRangeChip(
                isSelected = selectedTimeRange == TimeRange.CUSTOM,
                customDateRange = customDateRange,
                onClick = onCustomRangeClicked
            )
        }

        // ✅ PERBAIKAN: Tampilkan info custom range yang aktif
        if (selectedTimeRange == TimeRange.CUSTOM && customDateRange != null) {
            Text(
                text = "Rentang kustom: ${customDateRange.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF388E3C),
                modifier = Modifier.padding(top = 8.dp)
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

private fun getTimeRangeDisplayName(timeRange: TimeRange, customDateRange: DateRange? = null): String {
    return when (timeRange) {
        TimeRange.HOURS_24 -> "24 Jam"
        TimeRange.DAYS_7 -> "7 Hari"
        TimeRange.DAYS_30 -> "30 Hari"
        TimeRange.CUSTOM -> customDateRange?.let {
            "Kustom: ${it.label}"
        } ?: "Rentang Kustom"
    }
}

@Composable
fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Build,
                contentDescription = "Empty Chart",
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tidak Ada Data",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tidak ada data sensor yang tersedia\nuntuk rentang waktu yang dipilih",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center
            )
        }
    }
}
