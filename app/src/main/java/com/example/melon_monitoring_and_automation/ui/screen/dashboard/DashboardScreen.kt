package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.R
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import com.example.melon_monitoring_and_automation.ui.components.DateUtils
import com.example.melon_monitoring_and_automation.ui.viewmodel.GreenhouseViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

/**
 * DASHBOARD SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan overview semua greenhouse yang dimiliki user
 * - Menyediakan ringkasan statistik greenhouse (total, aktif, offline)
 * - Memungkinkan navigasi ke detail greenhouse tertentu
 * - Menyediakan real-time monitoring status sensor
 *
 * Fitur:
 * - Summary statistics card dengan total greenhouse dan status
 * - List greenhouse cards dengan data sensor terkini
 * - Loading states untuk initial data loading
 * - Empty state handling ketika tidak ada greenhouse
 * - Status indicators untuk online/offline greenhouse
 *
 * @author Your Name
 * @since Version 1.0
 * @param onGreenhouseClick Callback ketika greenhouse dipilih, menerima greenhouseId
 * @param viewModel ViewModel yang mengelola data greenhouse dan sensor readings
 */

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onGreenhouseClick: (String) -> Unit,
    onAddGreenhouseClick: () -> Unit = {},
    viewModel: GreenhouseViewModel = hiltViewModel()
) {
    // STATE COLLECTION - Collect state dari ViewModel
    val greenhouses by viewModel.greenhouses.collectAsState()
    val sensorReadings by viewModel.sensorReadings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // LOCAL STATE - Refresh animation state
    var isRefreshing by remember { mutableStateOf(false) }

    val darkGreen = Color(0xFF2E7D32)

    // STATUS BAR CONFIGURATION - Set warna status bar
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // REFRESH EFFECT - Simulasi network delay untuk refresh
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(1500) // Simulate network delay
            isRefreshing = false
        }
    }

    // MAIN SCAFFOLD LAYOUT
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo_svg),
                                contentDescription = "App Icon",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // APP TITLE AND SUBTITLE
                        Column {
                            Text(
                                text = "Melon Hydroponic",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Smart Monitoring",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGreenhouseClick,
                containerColor = Color(0xFF4CAF50),
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah Greenhouse"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Light gray background
                .padding(paddingValues) // Gunakan padding dari Scaffold
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // SUMMARY STATS CARD - Hanya tampil jika ada greenhouse
                if (greenhouses.isNotEmpty()) {
                    val totalGreenhouses = greenhouses.size
                    val activeSensors = sensorReadings.values.count { it != null }

                    SummaryStatsCard(
                        totalGreenhouses = totalGreenhouses,
                        activeSensors = activeSensors,
                        totalSensors = greenhouses.size
                    )
                }

                // MAIN CONTENT AREA - Dengan berbagai state handling
                Box(modifier = Modifier.weight(1f)) {
                    DashboardContent(
                        greenhouses = greenhouses,
                        sensorReadings = sensorReadings,
                        isLoading = isLoading,
                        onGreenhouseClick = onGreenhouseClick,
                        onAddGreenhouseClick = onAddGreenhouseClick
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

/**
 * SUMMARY STATS CARD COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan ringkasan statistik greenhouse secara visual
 * - Memberikan quick overview status sistem
 * - Menyediakan at-a-glance information untuk user
 *
 * @author Your Name
 * @since Version 1.0
 * @param totalGreenhouses Jumlah total greenhouse
 * @param activeSensors Jumlah sensor yang aktif/online
 * @param totalSensors Jumlah total sensor
 */

@Composable
private fun SummaryStatsCard(
    totalGreenhouses: Int,
    activeSensors: Int,
    totalSensors: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                value = totalGreenhouses.toString(),
                label = "Greenhouse",
                icon = Icons.Default.Spa,
                color = Color(0xFF4CAF50) // Green
            )

            StatItem(
                value = activeSensors.toString(),
                label = "Aktif",
                icon = Icons.Default.Sensors,
                color = Color(0xFF2196F3) // Blue
            )

            StatItem(
                value = "${totalSensors - activeSensors}",
                label = "Offline",
                icon = Icons.Default.Warning,
                color = Color(0xFFFF9800) // Orange
            )
        }
    }
}

/**
 * DASHBOARD CONTENT COMPOSABLE
 *
 * Tujuan:
 * - Menangani berbagai state content dashboard
 * - Menampilkan UI yang sesuai berdasarkan state data
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouses List greenhouse yang akan ditampilkan
 * @param sensorReadings Map pembacaan sensor per greenhouse
 * @param isLoading Boolean status loading data
 * @param onGreenhouseClick Callback ketika greenhouse dipilih
 */

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun DashboardContent(
    greenhouses: List<Greenhouse>,
    sensorReadings: Map<String, SensorReadings>,
    isLoading: Boolean,
    onGreenhouseClick: (String) -> Unit,
    onAddGreenhouseClick: () -> Unit = {}
) {
    when {
        isLoading && greenhouses.isEmpty() -> LoadingState()
        greenhouses.isEmpty() -> EmptyState(onAddGreenhouseClick = onAddGreenhouseClick)
        else -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cellCount = if (maxWidth > 600.dp) 2 else 1

                GreenhouseResponsiveList(
                    greenhouses = greenhouses,
                    sensorReadings = sensorReadings,
                    onGreenhouseClick = onGreenhouseClick,
                    cellCount = cellCount
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun GreenhouseResponsiveList(
    greenhouses: List<Greenhouse>,
    sensorReadings: Map<String, SensorReadings>,
    onGreenhouseClick: (String) -> Unit,
    cellCount: Int
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Greenhouse Saya",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            color = Color(0xFF333333)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(cellCount),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 80.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

/**
 * LOADING STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI loading selama data awal dimuat
 * - Memberikan feedback visual kepada user
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF4CAF50))
            Text(
                text = "Memuat greenhouse...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

/**
 * EMPTY STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika tidak ada greenhouse yang tersedia
 * - Memberikan petunjuk kepada user untuk menambahkan greenhouse
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun EmptyState(
    onAddGreenhouseClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Spa,
                contentDescription = "No Greenhouses",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Text(
                text = "Belum Ada Greenhouse",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Text(
                text = "Hubungi administrator untuk menambahkan greenhouse",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            // Tambah Button untuk Create Greenhouse
            Button(
                onClick = onAddGreenhouseClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tambah Greenhouse")
            }
        }
    }
}

/**
 * GREENHOUSE LIST COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan daftar greenhouse dalam bentuk cards
 * - Menyediakan akses cepat ke detail setiap greenhouse
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouses List greenhouse yang akan ditampilkan
 * @param sensorReadings Map pembacaan sensor untuk setiap greenhouse
 * @param onGreenhouseClick Callback ketika greenhouse dipilih
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun GreenhouseList(
    greenhouses: List<Greenhouse>,
    sensorReadings: Map<String, SensorReadings>,
    onGreenhouseClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // SECTION TITLE
        Text(
            text = "Greenhouse Saya",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            color = Color(0xFF333333)
        )

        // GREENHOUSE CARDS LIST
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            greenhouses.forEach { greenhouse ->
                val readings = sensorReadings[greenhouse.id]
                GreenhouseCard(
                    greenhouse = greenhouse,
                    sensorReadings = readings,
                    onClick = { onGreenhouseClick(greenhouse.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * STAT ITEM COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan satu item statistik dalam summary card
 * - Menyediakan visual yang konsisten untuk metrics
 *
 * @author Your Name
 * @since Version 1.0
 * @param value Nilai yang ditampilkan
 * @param label Label untuk nilai
 * @param icon Icon yang merepresentasikan statistik
 * @param color Warna tema untuk icon dan background
 */

@Composable
fun StatItem(
    value: String,
    label: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ICON WITH BACKGROUND CIRCLE
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        // VALUE TEXT
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        // LABEL TEXT
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

/**
 * GREENHOUSE CARD COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan informasi greenhouse dalam format card
 * - Menyediakan status sensor terkini dan navigasi ke detail
 *
 * Fitur:
 * - Nama dan lokasi greenhouse
 * - Status indicator (online/offline)
 * - Sensor readings (suhu, kelembapan, pH)
 * - Last updated timestamp
 * - Navigation hint
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouse Objek greenhouse yang akan ditampilkan
 * @param sensorReadings Pembacaan sensor terkini
 * @param onClick Callback ketika card diklik
 * @param modifier Modifier untuk kustomisasi layout
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GreenhouseCard(
    greenhouse: Greenhouse,
    sensorReadings: SensorReadings?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInactive = sensorReadings == null
    val backgroundColor = if (isInactive) Color(0xFFF0F0F0) else Color.White

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInactive) 0.dp else 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // HEADER - Greenhouse name dan status indicator
            GreenhouseHeader(greenhouse, sensorReadings)

            Spacer(modifier = Modifier.height(16.dp))

            // CONTENT - Berdasarkan ketersediaan data sensor
//            if (sensorReadings != null) {
//                SensorReadingsContent(sensorReadings)
//            } else {
//                NoSensorDataContent()
//            }
            SensorReadingsContent(sensorReadings)

            // FOOTER - Navigation hint
            NavigationHint()
        }
    }
}

/**
 * GREENHOUSE HEADER COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan header information greenhouse card
 * - Menyediakan status indicator visual
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouse Objek greenhouse
 * @param sensorReadings Data sensor untuk menentukan status
 */

@Composable
private fun GreenhouseHeader(
    greenhouse: Greenhouse,
    sensorReadings: SensorReadings?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = greenhouse.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = greenhouse.location,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // STATUS INDICATOR LOGIC
        val statusColor = if (sensorReadings != null) {
            // Jika ada data, asumsikan online (Hijau)
            Color(0xFF4CAF50)
        } else {
            // Jika data null, warnanya Abu-abu (Offline/No Data)
            Color.LightGray
        }

        // STATUS INDICATOR DOT
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color = statusColor)
        )
    }
}

/**
 * SENSOR READINGS CONTENT COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan pembacaan sensor ketika data tersedia
 * - Menyediakan visualisasi data sensor yang mudah dibaca
 *
 * @author Your Name
 * @since Version 1.0
 * @param sensorReadings Data sensor yang akan ditampilkan
 */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun SensorReadingsContent(sensorReadings: SensorReadings?) {
    // TIGA SENSOR READINGS UTAMA
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ) {
        SensorReadingItem(
            value = sensorReadings?.temperature,
            label = "Suhu",
            icon = Icons.Default.Thermostat,
            color = if (sensorReadings?.temperature != null) Color(0xFFF44336) else Color.Gray, // Warna jadi abu jika null
            unit = "°C"
        )

        SensorReadingItem(
            value = sensorReadings?.humidity,
            label = "Kelembapan",
            icon = Icons.Default.WaterDrop,
            color = if (sensorReadings?.humidity != null) Color(0xFF2196F3) else Color.Gray,
            unit = "%"
        )

        SensorReadingItem(
            value = sensorReadings?.ph,
            label = "pH",
            icon = Icons.Default.Sensors,
            color = if (sensorReadings?.ph != null) Color(0xFF9C27B0) else Color.Gray,
            unit = "pH"
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // LAST UPDATED TIMESTAMP LOGIC
    val formattedTime = if (sensorReadings?.recordedAt != null) {
        "Update: ${DateUtils.formatToLocalTime(sensorReadings.recordedAt)}"
    } else {
        "Menunggu data sensor..."
    }

    Text(
        text = formattedTime,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center // Center text agar lebih rapi saat pesan panjang
    )
}

/**
 * NO SENSOR DATA CONTENT COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika tidak ada data sensor yang tersedia
 * - Memberikan feedback yang jelas tentang status data
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun NoSensorDataContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Data sensor tidak tersedia",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/**
 * NAVIGATION HINT COMPOSABLE
 *
 * Tujuan:
 * - Memberikan visual hint untuk navigasi ke detail
 * - Meningkatkan user experience dengan clear CTAs
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun NavigationHint() {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Klik untuk melihat detail →",
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF4CAF50),
        fontWeight = FontWeight.Medium
    )
}

/**
 * SENSOR READING ITEM COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan satu pembacaan sensor individual
 * - Menyediakan format yang konsisten untuk semua sensor types
 *
 * @author Your Name
 * @since Version 1.0
 * @param value Nilai sensor (nullable Double)
 * @param label Nama sensor
 * @param icon Icon yang merepresentasikan sensor
 * @param color Warna tema untuk icon
 * @param unit Unit pengukuran
 */

@Composable
fun SensorReadingItem(
    value: Double?,
    label: String,
    icon: ImageVector,
    color: Color,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // SENSOR ICON
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        // VALUE DENGAN FORMATTING
        Text(
            text = value?.let { "%.1f".format(it) } ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (value != null) Color(0xFF333333) else Color.Gray // Text abu-abu jika kosong
        )

        // SENSOR LABEL
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        // MEASUREMENT UNIT
        Text(
            text = if (value != null) unit else "",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
