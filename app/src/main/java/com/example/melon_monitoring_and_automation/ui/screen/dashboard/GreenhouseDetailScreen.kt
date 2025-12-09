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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.domain.model.ChartDataPoint
import com.example.melon_monitoring_and_automation.domain.model.ChartType
import com.example.melon_monitoring_and_automation.domain.model.DateRange
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.TimeRange
import com.example.melon_monitoring_and_automation.ui.components.CustomDateRangeChip
import com.example.melon_monitoring_and_automation.ui.components.DateRangePicker
import com.example.melon_monitoring_and_automation.ui.components.DateUtils
import com.example.melon_monitoring_and_automation.ui.components.SensorLineChart
import com.example.melon_monitoring_and_automation.ui.viewmodel.ChartViewModel
import com.example.melon_monitoring_and_automation.ui.viewmodel.GreenhouseViewModel
import kotlinx.coroutines.delay

/**
 * GREENHOUSE DETAIL SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan informasi detail lengkap tentang greenhouse tertentu
 * - Menyediakan data sensor terkini dalam format tabel
 * - Menampilkan grafik data historis dengan berbagai filter options
 * - Memungkinkan analisis trend data sensor over time
 *
 * Fitur:
 * - Greenhouse information section (nama, lokasi, deskripsi)
 * - Current sensor readings dalam format tabel
 * - Interactive charts dengan multiple sensor types
 * - Time range filters (24 jam, 7 hari, 30 hari, custom)
 * - Date range picker untuk custom time selection
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouseId ID greenhouse yang akan ditampilkan detailnya
 * @param onBackClick Callback untuk navigasi kembali
 * @param viewModel ViewModel untuk data greenhouse
 * @requires Android O (API 26) untuk date/time operations
 */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenhouseDetailScreen(
    greenhouseId: String,
    onBackClick: () -> Unit,
    viewModel: GreenhouseViewModel = hiltViewModel()
) {
    // STATE COLLECTION - Collect state dari ViewModel
    val greenhouse by viewModel.getGreenhouseById(greenhouseId).collectAsState(initial = null)
    val sensorReadings by viewModel.getLatestSensorReadings(greenhouseId).collectAsState(initial = null)

    val darkGreen = Color(0xFF2E7D32)

    // STATUS BAR CONFIGURATION
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // MAIN SCAFFOLD LAYOUT
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(greenhouse?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // GREENHOUSE INFO SECTION
            GreenhouseInfoSection(greenhouse)

            // CURRENT SENSOR READINGS SECTION
            SensorReadingsSection(sensorReadings)

            // CHART SECTION - Dengan delayed loading untuk prevent crash
            SensorChartSection(greenhouseId = greenhouseId)
        }
    }
}

/**
 * GREENHOUSE INFO SECTION COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan informasi dasar greenhouse
 * - Memberikan context tentang greenhouse yang sedang dilihat
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouse Objek greenhouse, atau null jika sedang loading
 */

@Composable
fun GreenhouseInfoSection(greenhouse: com.example.melon_monitoring_and_automation.domain.model.Greenhouse?) {
    greenhouse?.let { gh ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)) // Light green background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // GREENHOUSE NAME
                Text(
                    text = gh.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF388E3C), // Dark green
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // LOCATION
                Text(
                    text = "Lokasi: ${gh.location}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // DESCRIPTION (OPTIONAL)
                Text(
                    text = "Deskripsi: ${gh.description ?: "Tidak ada deskripsi"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * SENSOR READINGS SECTION COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan pembacaan sensor terkini dalam format tabel
 * - Menyediakan quick overview semua parameter sensor
 *
 * @author Your Name
 * @since Version 1.0
 * @param sensorReadings Data sensor terkini, atau null jika tidak tersedia
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SensorReadingsSection(sensorReadings: com.example.melon_monitoring_and_automation.domain.model.SensorReadings?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // SECTION TITLE
            Text(
                text = "Pembacaan Sensor Terkini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // LOGIC CHECK: Apakah data ada?
            if (sensorReadings != null) {
                // DATA ADA -> Tampilkan List
                ReadingItem("Suhu Udara", formatValue(sensorReadings.temperature), "°C")
                Spacer(modifier = Modifier.height(8.dp))

                ReadingItem("Kelembapan", formatValue(sensorReadings.humidity), "%")
                Spacer(modifier = Modifier.height(8.dp))

                ReadingItem("Suhu Air", formatValue(sensorReadings.waterTemp), "°C")
                Spacer(modifier = Modifier.height(8.dp))

                ReadingItem("pH Air", formatValue(sensorReadings.ph), "pH")
                Spacer(modifier = Modifier.height(8.dp))

                ReadingItem("TDS (Nutrisi)", formatValue(sensorReadings.tds), "ppm")

                Spacer(modifier = Modifier.height(16.dp))

                val timeStr = DateUtils.formatToLocalTime(
                    sensorReadings.recordedAt,
                    "EEEE, dd MMMM HH:mm:ss" // Contoh: Senin, 08 Desember 10:45:00
                )

                Text(
                    text = "Terakhir diperbarui: $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

            } else {
                // DATA KOSONG -> Tampilkan Pesan "Menunggu Data"
                EmptySensorDataPlaceholder()
            }
        }
    }
}

/**
 * Helper untuk format value agar tidak crash/jelek saat null
 */
private fun formatValue(value: Double?): String {
    return value?.let { "%.1f".format(it) } ?: "-"
}

@Composable
fun EmptySensorDataPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "No Data",
            tint = Color.LightGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Belum ada data sensor",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = "Pastikan perangkat IoT terhubung dan mengirim data.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * READING ITEM COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan satu baris data sensor dalam format label-value
 * - Menyediakan format yang konsisten untuk semua sensor readings
 *
 * @author Your Name
 * @since Version 1.0
 * @param label Nama parameter sensor
 * @param value Nilai sensor (format string)
 * @param unit Unit pengukuran
 */

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

/**
 * SENSOR CHART SECTION COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan grafik data sensor historis
 * - Menyediakan interactive chart dengan berbagai filter options
 * - Menggunakan delayed loading untuk prevent crash
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouseId ID greenhouse untuk memuat data chart
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SensorChartSection(
    greenhouseId: String
) {
    // STATE MANAGEMENT - Kontrol kapan chart ditampilkan
    var showChart by remember { mutableStateOf(false) }

    // DELAYED LOADING EFFECT - Prevent immediate crash
    LaunchedEffect(greenhouseId) {
        delay(500) // Delay untuk memastikan screen sudah fully loaded
        showChart = true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showChart) {
            SafeChartImplementation(greenhouseId = greenhouseId)
        } else {
            // LOADING STATE - Selama delay
            ChartLoadingState()
        }
    }
}

/**
 * CHART LOADING STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI loading selama chart data dimuat
 * - Memberikan feedback visual kepada user
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun ChartLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * SAFE CHART IMPLEMENTATION COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan implementasi chart yang aman dengan error handling
 * - Mengkoordinasikan semua komponen chart-related
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouseId ID greenhouse untuk memuat data
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SafeChartImplementation(greenhouseId: String) {
    val viewModel: ChartViewModel = hiltViewModel()

    // STATE COLLECTION - Dari ChartViewModel
    val chartData by viewModel.chartData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val chartConfig by viewModel.chartConfig.collectAsState()

    // DATE RANGE PICKER STATE
    val showDateRangePicker by viewModel.showDateRangePicker.collectAsState()
    val availableMonths by viewModel.availableMonths.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val weeklyRanges by viewModel.weeklyRanges.collectAsState()

    // LOCAL ERROR STATE
    var chartError by remember { mutableStateOf<String?>(null) }

    // LOAD CHART DATA EFFECT - Ketika configuration berubah
    LaunchedEffect(chartConfig.selectedSensorType, chartConfig.timeRange, chartConfig.customDateRange) {
        try {
            viewModel.loadChartData(greenhouseId)
        } catch (e: Exception) {
            chartError = "Gagal memuat data chart: ${e.message}"
        }
    }

    // LOAD AVAILABLE MONTHS EFFECT - Pada initialization
    LaunchedEffect(Unit) {
        viewModel.loadAvailableMonths(greenhouseId)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // CHART CONTROLS - Dengan filter options
        EnhancedChartControls(
            selectedSensorType = chartConfig.selectedSensorType,
            selectedTimeRange = chartConfig.timeRange,
            customDateRange = chartConfig.customDateRange,
            onSensorTypeChanged = { viewModel.updateSensorType(it) },
            onTimeRangeChanged = { viewModel.updateTimeRange(it) },
            onCustomRangeClicked = { viewModel.showDateRangePicker() }
        )

        // DATE RANGE PICKER OVERLAY
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

        // ERROR MESSAGES HANDLING
        ChartErrorHandling(
            viewModelError = errorMessage,
            localError = chartError,
            onDismissViewModelError = { viewModel.clearErrorMessage() },
            onDismissLocalError = { chartError = null }
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .padding(horizontal = 8.dp)) {
            ChartContent(
                isLoading = isLoading,
                chartError = chartError,
                chartData = chartData,
                viewModel = viewModel,
                onError = { error -> chartError = error }
            )
        }
    }
}

/**
 * CHART ERROR HANDLING COMPOSABLE
 *
 * Tujuan:
 * - Menangani penampilan error messages dari berbagai sumber
 * - Menyediakan unified error handling untuk chart system
 *
 * @author Your Name
 * @since Version 1.0
 * @param viewModelError Error dari ViewModel
 * @param localError Error lokal dari chart rendering
 * @param onDismissViewModelError Callback untuk dismiss ViewModel error
 * @param onDismissLocalError Callback untuk dismiss local error
 */

@Composable
private fun ChartErrorHandling(
    viewModelError: String?,
    localError: String?,
    onDismissViewModelError: () -> Unit,
    onDismissLocalError: () -> Unit
) {
    if (!viewModelError.isNullOrEmpty()) {
        ErrorMessageCard(message = viewModelError!!) {
            onDismissViewModelError()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (!localError.isNullOrEmpty()) {
        ErrorMessageCard(message = localError!!) {
            onDismissLocalError()
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * CHART CONTENT COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan konten chart berdasarkan state current
 * - Menangani semua possible states (loading, error, empty, success)
 *
 * @author Your Name
 * @since Version 1.0
 * @param isLoading Boolean status loading data
 * @param chartError Error message lokal
 * @param chartData List data points untuk chart
 * @param viewModel ChartViewModel untuk mendapatkan label dan title
 * @param onError Callback untuk menangani error selama rendering
 */

@Composable
private fun ChartContent(
    isLoading: Boolean,
    chartError: String?,
    chartData: List<ChartDataPoint>,
    viewModel: ChartViewModel,
    onError: (String) -> Unit
) {
    when {
        isLoading -> {
            LoadingChartPlaceholder()
        }
        !chartError.isNullOrEmpty() -> {
            ChartErrorFallback(errorMessage = chartError!!, dataSize = chartData.size)
        }
        chartData.isEmpty() -> {
            EmptyChartPlaceholder()
        }
        else -> {
            SafeSensorLineChart(
                dataPoints = chartData,
                yAxisLabel = viewModel.getYAxisLabel(),
                chartTitle = viewModel.getChartTitle(),
                onError = onError
            )
        }
    }
}

/**
 * ENHANCED CHART CONTROLS COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan controls untuk filter dan konfigurasi chart
 * - Memungkinkan user untuk memilih sensor type dan time range
 *
 * Fitur:
 * - Sensor type selection (suhu, kelembapan, pH, TDS, suhu air)
 * - Time range selection (24 jam, 7 hari, 30 hari, custom)
 * - Custom date range picker integration
 *
 * @author Your Name
 * @since Version 1.0
 * @param selectedSensorType Sensor type yang sedang dipilih
 * @param selectedTimeRange Time range yang sedang dipilih
 * @param customDateRange Custom date range yang aktif
 * @param onSensorTypeChanged Callback ketika sensor type berubah
 * @param onTimeRangeChanged Callback ketika time range berubah
 * @param onCustomRangeClicked Callback untuk membuka date range picker
 */

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
        // SECTION TITLE
        Text(
            "Grafik Sensor",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // SENSOR TYPE FILTER
        Text(
            "Jenis Sensor:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SensorType.entries) { sensorType ->
                FilterChip(
                    selected = selectedSensorType == sensorType,
                    onClick = { onSensorTypeChanged(sensorType) },
                    label = { Text(getSensorTypeDisplayName(sensorType)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // TIME RANGE FILTER DENGAN CUSTOM OPTION
        Text(
            "Rentang Waktu:",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TimeRange.entries) { timeRange ->
                if (timeRange != TimeRange.CUSTOM) {
                    FilterChip(
                        selected = selectedTimeRange == timeRange,
                        onClick = { onTimeRangeChanged(timeRange) },
                        label = {
                            Text(getTimeRangeDisplayName(timeRange))
                        }
                    )
                }
            }

            item {
                CustomDateRangeChip(
                    isSelected = selectedTimeRange == TimeRange.CUSTOM,
                    customDateRange = customDateRange,
                    onClick = onCustomRangeClicked
                )
            }
        }

        if (selectedTimeRange == TimeRange.CUSTOM && customDateRange != null) {
            Text(
                text = "Rentang kustom: ${customDateRange.label}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF388E3C),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // CUSTOM RANGE INFO - Tampilkan info custom range yang aktif
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

/**
 * SAFE SENSOR LINE CHART COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan wrapper aman untuk SensorLineChart dengan validasi data
 * - Mencegah crash ketika data tidak valid atau error terjadi
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints Data points untuk chart
 * @param yAxisLabel Label untuk sumbu Y
 * @param chartTitle Judul chart
 * @param onError Callback untuk menangani error
 * @param modifier Modifier untuk kustomisasi layout
 */

@Composable
fun SafeSensorLineChart(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    chartTitle: String,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // DATA VALIDATION - Filter out invalid data sebelum render
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

/**
 * ERROR MESSAGE CARD COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan error message dalam format card yang konsisten
 * - Menyediakan dismiss functionality untuk user
 *
 * @author Your Name
 * @since Version 1.0
 * @param message Pesan error yang akan ditampilkan
 * @param onDismiss Callback ketika error di-dismiss
 */

@Composable
fun ErrorMessageCard(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)) // Light red background
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
                Icon(Icons.Default.Close, contentDescription = "Close error message")
            }
        }
    }
}

/**
 * LOADING CHART PLACEHOLDER COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI loading selama chart data dimuat
 * - Memberikan feedback visual yang konsisten
 *
 * @author Your Name
 * @since Version 1.0
 */

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

/**
 * CHART ERROR FALLBACK COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI fallback ketika chart gagal dirender
 * - Memberikan informasi error yang informatif kepada user
 *
 * @author Your Name
 * @since Version 1.0
 * @param errorMessage Pesan error yang akan ditampilkan
 * @param dataSize Jumlah data points yang tersedia
 */

@Composable
fun ChartErrorFallback(errorMessage: String, dataSize: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
            .background(Color(0xFFF5F5F5)), // Light gray background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF9800), // Orange
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Chart Tidak Dapat Ditampilkan",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF757575) // Gray
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

// ============ HELPER FUNCTIONS ============

/**
 * Mendapatkan display name untuk SensorType.
 *
 * @param sensorType Jenis sensor
 * @return String nama yang user-friendly
 */
private fun getSensorTypeDisplayName(sensorType: SensorType): String {
    return when (sensorType) {
        SensorType.TEMPERATURE -> "Suhu"
        SensorType.HUMIDITY -> "Kelembapan"
        SensorType.WATER_TEMPERATURE -> "Suhu Air"
        SensorType.PH -> "pH"
        SensorType.TDS -> "TDS"
    }
}

/**
 * Mendapatkan display name untuk TimeRange.
 *
 * @param timeRange Rentang waktu
 * @param customDateRange Custom date range (opsional)
 * @return String nama yang user-friendly
 */
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

/**
 * EMPTY CHART PLACEHOLDER COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika tidak ada data chart yang tersedia
 * - Memberikan feedback yang jelas tentang empty state
 *
 * @author Your Name
 * @since Version 1.0
 */

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
                tint = Color(0xFF9E9E9E), // Gray
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Tidak Ada Data",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF757575), // Gray
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tidak ada data sensor yang tersedia\nuntuk rentang waktu yang dipilih",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E), // Light gray
                textAlign = TextAlign.Center
            )
        }
    }
}
