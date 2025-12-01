package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.R
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.ui.viewmodel.ControlViewModel

/**
 * CONTROL SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan interface untuk mengontrol perangkat IoT greenhouse
 * - Memungkinkan kontrol manual dan otomatis untuk blower, pompa, dan sistem nutrisi
 * - Menampilkan status real-time perangkat dan pengaturan automasi
 *
 * Fitur:
 * - Greenhouse selector untuk memilih greenhouse yang akan dikontrol
 * - Blower control dengan mode manual dan otomatis
 * - Nutrient pump control dengan adjustable droplet settings
 * - Main pump control untuk sirkulasi air
 * - Temperature threshold settings untuk automasi
 * - Real-time device status updates
 *
 * @author Your Name
 * @since Version 1.0
 * @param viewModel ViewModel yang mengelola state dan logic kontrol perangkat
 * @param onManageDevicesClick Callback untuk navigasi ke device management screen
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel = hiltViewModel(),
    onManageDevicesClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    // LOCAL STATE MANAGEMENT
    var selectedGreenhouse by remember { mutableStateOf<String?>(null) }

    // VIEWMODEL STATE COLLECTION
    val greenhouses by viewModel.greenhouses.collectAsState()
    val controlDevices by viewModel.controlDevices.collectAsState()
    val automationSettings by viewModel.automationSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    val darkGreen = Color(0xFF2E7D32)

    // STATUS BAR CONFIGURATION
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // SUCCESS MESSAGE HANDLER
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            // Show success message or snackbar
            viewModel.clearUpdateSuccess()
        }
    }

    // MAIN SCAFFOLD LAYOUT
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontrol Perangkat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Device Management Button
                    IconButton(onClick = onManageDevicesClick) {
                        Icon(
                            Icons.Default.DeviceHub,
                            contentDescription = "Kelola Perangkat",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ERROR MESSAGE DISPLAY
            if (!errorMessage.isNullOrEmpty()) {
                ErrorMessageCard(
                    message = errorMessage!!,
                    onDismiss = { viewModel.clearErrorMessage() }
                )
            }

            // MAIN CONTENT AREA - Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // GREENHOUSE SELECTOR SECTION
                GreenhouseSelector(
                    greenhouses = greenhouses,
                    selectedGreenhouse = selectedGreenhouse,
                    onGreenhouseSelected = { selectedGreenhouse = it }
                )

                // CONTENT BASED ON LOADING STATE
                if (isLoading) {
                    LoadingState()
                } else {
                    selectedGreenhouse?.let { greenhouseId ->
                        val controlDevice = controlDevices[greenhouseId]
                        val settings = automationSettings[greenhouseId]

                        if (controlDevice != null && settings != null) {
                            // CONTROL SECTIONS FOR SELECTED GREENHOUSE
                            ControlSections(
                                greenhouseId = greenhouseId,
                                controlDevice = controlDevice,
                                settings = settings,
                                viewModel = viewModel
                            )
                        } else {
                            // NO DATA AVAILABLE STATE
                            NoDataAvailableState()
                        }
                    } ?: run {
                        // NO GREENHOUSE SELECTED STATE
                        NoGreenhouseSelectedState()
                    }
                }
            }
        }
    }
}

/**
 * ERROR MESSAGE CARD COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan error message dalam format yang konsisten
 * - Menyediakan dismiss functionality untuk user
 *
 * @author Your Name
 * @since Version 1.0
 * @param message Pesan error yang akan ditampilkan
 * @param onDismiss Callback ketika error di-dismiss
 */

@Composable
private fun ErrorMessageCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

/**
 * LOADING STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI loading selama data dimuat
 * - Memberikan feedback visual kepada user
 *
 * @author Your Name
 * @since Version 1.0
 */

//@Composable
//private fun LoadingState() {
//    Box(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(200.dp),
//        contentAlignment = Alignment.Center
//    ) {
//        CircularProgressIndicator()
//    }
//}

/**
 * NO DATA AVAILABLE STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika data perangkat tidak tersedia
 * - Memberikan feedback yang jelas tentang status data
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun NoDataAvailableState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Data perangkat tidak tersedia untuk greenhouse ini",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

/**
 * NO GREENHOUSE SELECTED STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika belum ada greenhouse yang dipilih
 * - Memberikan instruksi yang jelas kepada user
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
private fun NoGreenhouseSelectedState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Pilih greenhouse untuk mengontrol perangkat",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
    }
}

/**
 * CONTROL SECTIONS COMPOSABLE
 *
 * Tujuan:
 * - Mengelompokkan semua section kontrol perangkat
 * - Menyediakan structured layout untuk berbagai jenis kontrol
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouseId ID greenhouse yang sedang dipilih
 * @param controlDevice Data kontrol perangkat greenhouse
 * @param settings Pengaturan automasi greenhouse
 * @param viewModel ViewModel untuk handling actions
 */

@Composable
private fun ControlSections(
    greenhouseId: String,
    controlDevice: ControlDevices,
    settings: AutomationSettings,
    viewModel: ControlViewModel
) {
    // BLOWER CONTROL SECTION
    BlowerControlSection(
        controlDevice = controlDevice,
        automationSettings = settings,
        onBlowerToggle = { enabled ->
            viewModel.toggleBlower(greenhouseId, enabled)
        },
        onAutoModeToggle = { autoMode ->
            viewModel.setAutoMode(greenhouseId, autoMode)
        },
        onTemperatureThresholdsChange = { minTemp, maxTemp ->
            viewModel.updateTemperatureThresholds(greenhouseId, minTemp, maxTemp)
        }
    )

    // NUTRIENT PUMP CONTROL SECTION
    NutrientPumpControlSection(
        automationSettings = settings,
        onDropletsChange = { droplets ->
            viewModel.setNutrientDroplets(greenhouseId, droplets)
        }
    )

    // MAIN PUMP CONTROL SECTION
    PumpControlSection(
        controlDevice = controlDevice,
        onPumpToggle = { enabled ->
            viewModel.togglePump(greenhouseId, enabled)
        }
    )

    // BOTTOM SPACER
    Spacer(modifier = Modifier.height(32.dp))
}

/**
 * GREENHOUSE SELECTOR COMPOSABLE
 *
 * Tujuan:
 * - Memungkinkan user memilih greenhouse yang akan dikontrol
 * - Menampilkan daftar greenhouse dalam format radio button
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouses List greenhouse yang tersedia
 * @param selectedGreenhouse ID greenhouse yang sedang dipilih
 * @param onGreenhouseSelected Callback ketika greenhouse dipilih
 */

@Composable
fun GreenhouseSelector(
    greenhouses: List<Greenhouse>,
    selectedGreenhouse: String?,
    onGreenhouseSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Pilih Greenhouse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (greenhouses.isEmpty()) {
                Text(
                    "Tidak ada greenhouse tersedia",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                greenhouses.forEach { greenhouse ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGreenhouseSelected(greenhouse.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedGreenhouse == greenhouse.id,
                            onClick = { onGreenhouseSelected(greenhouse.id) }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                greenhouse.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                greenhouse.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * BLOWER CONTROL SECTION COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan kontrol untuk sistem blower/kipas greenhouse
 * - Mendukung mode manual dan otomatis berdasarkan suhu
 * - Memungkinkan pengaturan threshold suhu untuk automasi
 *
 * Fitur:
 * - Auto/manual mode switch
 * - Manual blower control (disabled dalam auto mode)
 * - Temperature threshold settings
 * - Real-time status display
 *
 * @author Your Name
 * @since Version 1.0
 * @param controlDevice Data kontrol perangkat blower
 * @param automationSettings Pengaturan automasi suhu
 * @param onBlowerToggle Callback untuk toggle blower manual
 * @param onAutoModeToggle Callback untuk toggle mode automasi
 * @param onTemperatureThresholdsChange Callback untuk update threshold suhu
 */

@Composable
fun BlowerControlSection(
    controlDevice: ControlDevices,
    automationSettings: AutomationSettings,
    onBlowerToggle: (Boolean) -> Unit,
    onAutoModeToggle: (Boolean) -> Unit,
    onTemperatureThresholdsChange: (Double, Double) -> Unit
) {
    var showTempSettings by remember { mutableStateOf(false) }
    var minTemp by remember { mutableStateOf(automationSettings.minTemperature.toString()) }
    var maxTemp by remember { mutableStateOf(automationSettings.maxTemperature.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // SECTION HEADER
            Text(
                "Kontrol Blower",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AUTO/MANUAL MODE SWITCH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Mode Otomatis",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Blower akan menyala/mati berdasarkan suhu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = controlDevice.autoMode,
                    onCheckedChange = onAutoModeToggle
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MANUAL BLOWER CONTROL (disabled in auto mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Kontrol Manual Blower",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (controlDevice.autoMode) "Non-aktif dalam mode otomatis" else "Hidup/matikan blower manual",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (controlDevice.autoMode) Color.Gray else Color(0xFF388E3C)
                    )
                }
                Switch(
                    checked = controlDevice.fan,
                    onCheckedChange = onBlowerToggle,
                    enabled = !controlDevice.autoMode
                )
            }

            // TEMPERATURE THRESHOLDS INFO CARD
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "⚙️ Pengaturan Suhu Otomatis",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Blower menyala saat: ${automationSettings.maxTemperature}°C",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Blower mati saat: ${automationSettings.minTemperature}°C",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // TEMPERATURE SETTINGS BUTTON
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showTempSettings = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
            ) {
                Text("Atur Batas Suhu")
            }
        }
    }

    // TEMPERATURE SETTINGS DIALOG
    if (showTempSettings) {
        TemperatureSettingsDialog(
            minTemp = minTemp,
            maxTemp = maxTemp,
            onMinTempChange = { minTemp = it },
            onMaxTempChange = { maxTemp = it },
            onSave = {
                val min = minTemp.toDoubleOrNull() ?: automationSettings.minTemperature
                val max = maxTemp.toDoubleOrNull() ?: automationSettings.maxTemperature
                if (min < max) {
                    onTemperatureThresholdsChange(min, max)
                    showTempSettings = false
                }
                // TODO: Show error if min >= max
            },
            onDismiss = { showTempSettings = false }
        )
    }
}

/**
 * NUTRIENT PUMP CONTROL SECTION COMPOSABLE
 *
 * Tujuan:
 * - Mengontrol pompa nutrisi AB Mix dengan adjustable droplet settings
 * - Memungkinkan pengaturan jumlah tetesan per siklus
 * - Menyediakan visual feedback untuk current settings
 *
 * Fitur:
 * - Droplet counter dengan increment/decrement buttons
 * - Range validation (1-50 tetesan)
 * - Confirmation dialog sebelum save
 * - Visual droplet display
 *
 * @author Your Name
 * @since Version 1.0
 * @param automationSettings Pengaturan automasi termasuk nutrient droplets
 * @param onDropletsChange Callback untuk update jumlah tetesan
 */

@Composable
fun NutrientPumpControlSection(
    automationSettings: AutomationSettings,
    onDropletsChange: (Int) -> Unit
) {
    var currentDroplets by remember { mutableStateOf(automationSettings.nutrientDroplets) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECTION HEADER
            Text(
                "🏭 Pompa Nutrisi AB Mix",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Atur jumlah tetesan nutrisi per siklus",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DROPLET COUNTER INTERFACE
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Jumlah Tetesan",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MINUS BUTTON
                    IconButton(
                        onClick = {
                            if (currentDroplets > 1) {
                                currentDroplets--
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = currentDroplets > 1
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (currentDroplets > 1) Color(0xFF388E3C) else Color.LightGray,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Kurangi",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }

                    // DROPLET DISPLAY
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                color = Color(0xFFE8F5E8),
                                shape = MaterialTheme.shapes.medium
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$currentDroplets",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color(0xFF388E3C),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "tetes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF388E3C)
                            )
                        }
                    }

                    // PLUS BUTTON
                    IconButton(
                        onClick = {
                            if (currentDroplets < 50) {
                                currentDroplets++
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        enabled = currentDroplets < 50
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = if (currentDroplets < 50) Color(0xFF388E3C) else Color.LightGray,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Tambah",
                                modifier = Modifier.size(24.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Range: 1-50 tetesan",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SAVE BUTTON
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = currentDroplets != automationSettings.nutrientDroplets
            ) {
                Text("Simpan Pengaturan Tetesan")
            }
        }
    }

    // CONFIRMATION DIALOG
    if (showConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Konfirmasi Pengaturan") },
            text = {
                Text("Anda akan mengatur pompa nutrisi menjadi $currentDroplets tetesan per siklus. Lanjutkan?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDropletsChange(currentDroplets)
                        showConfirmDialog = false
                    }
                ) {
                    Text("Ya, Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

/**
 * PUMP CONTROL SECTION COMPOSABLE
 *
 * Tujuan:
 * - Mengontrol pompa sirkulasi air utama greenhouse
 * - Menyediakan toggle switch untuk on/off status
 * - Menampilkan visual status indicator
 *
 * @author Your Name
 * @since Version 1.0
 * @param controlDevice Data kontrol perangkat pompa
 * @param onPumpToggle Callback untuk toggle status pompa
 */

@Composable
fun PumpControlSection(
    controlDevice: ControlDevices,
    onPumpToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // SECTION HEADER
            Text(
                "💧 Pompa Utama",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Kontrol pompa sirkulasi air utama",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(16.dp))

            // PUMP TOGGLE SWITCH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Status Pompa",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (controlDevice.pump) "Pompa menyala" else "Pompa mati",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (controlDevice.pump) Color(0xFF388E3C) else Color.Gray
                    )
                }
                Switch(
                    checked = controlDevice.pump,
                    onCheckedChange = onPumpToggle
                )
            }

            // STATUS INDICATOR
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (controlDevice.pump) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (controlDevice.pump) Color(0xFF4CAF50) else Color(0xFFF44336),
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (controlDevice.pump) "✅ Pompa sedang berjalan" else "⭕ Pompa dalam keadaan mati",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * TEMPERATURE SETTINGS DIALOG COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan dialog untuk mengatur threshold suhu automasi
 * - Memvalidasi input temperature ranges
 * - Menampilkan error messages untuk invalid input
 *
 * Validasi:
 * - Min dan max harus angka valid
 * - Min harus < max
 * - Temperature range: 0°C - 50°C
 *
 * @author Your Name
 * @since Version 1.0
 * @param minTemp Current minimum temperature value
 * @param maxTemp Current maximum temperature value
 * @param onMinTempChange Callback ketika min temp berubah
 * @param onMaxTempChange Callback ketika max temp berubah
 * @param onSave Callback ketika settings disimpan
 * @param onDismiss Callback ketika dialog ditutup
 */

@Composable
fun TemperatureSettingsDialog(
    minTemp: String,
    maxTemp: String,
    onMinTempChange: (String) -> Unit,
    onMaxTempChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var tempError by remember { mutableStateOf<String?>(null) }

    // INPUT VALIDATION FUNCTION
    fun validateInput(): Boolean {
        val min = minTemp.toDoubleOrNull()
        val max = maxTemp.toDoubleOrNull()

        return when {
            min == null || max == null -> {
                tempError = "Masukkan angka yang valid"
                false
            }
            min >= max -> {
                tempError = "Suhu minimum harus lebih kecil dari suhu maksimum"
                false
            }
            min < 0 || max > 50 -> {
                tempError = "Suhu harus antara 0°C dan 50°C"
                false
            }
            else -> {
                tempError = null
                true
            }
        }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🌡️ Atur Batas Suhu Otomatis") },
        text = {
            Column {
                Text("Atur rentang suhu untuk kontrol blower otomatis:")
                Spacer(modifier = Modifier.height(16.dp))

                // ERROR MESSAGE DISPLAY
                tempError?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // MIN TEMPERATURE INPUT
                Text("Suhu Minimum (°C):", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = minTemp,
                    onValueChange = {
                        onMinTempChange(it)
                        tempError = null // Clear error when user types
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contoh: 25") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MAX TEMPERATURE INPUT
                Text("Suhu Maksimum (°C):", fontWeight = FontWeight.Medium)
                OutlinedTextField(
                    value = maxTemp,
                    onValueChange = {
                        onMaxTempChange(it)
                        tempError = null // Clear error when user types
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Contoh: 38") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Blower akan menyala saat suhu ≥ maksimum dan mati saat suhu ≤ minimum",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateInput()) {
                        onSave()
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
