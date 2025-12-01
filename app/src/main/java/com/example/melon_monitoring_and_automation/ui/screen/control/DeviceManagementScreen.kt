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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.domain.model.DeviceType
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import com.example.melon_monitoring_and_automation.ui.viewmodel.DeviceManagementViewModel

/**
 * DEVICE MANAGEMENT SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan interface untuk mengelola perangkat IoT greenhouse
 * - Memungkinkan pairing, unpairing, dan monitoring device
 * - Menampilkan status real-time perangkat IoT
 *
 * Fitur:
 * - Daftar semua perangkat IoT yang terhubung
 * - Status online/offline device
 * - Device pairing dengan kode QR/pairing code
 * - Informasi detail device dan telemetry
 * - Filter device berdasarkan greenhouse
 *
 * @author Your Name
 * @since Version 1.0
 * @param viewModel ViewModel yang mengelola state device management
 * @param onBackClick Callback untuk kembali ke screen sebelumnya
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagementScreen(
    viewModel: DeviceManagementViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    // Collect state from ViewModel
    val devices by viewModel.devices.collectAsState()
    val greenhouses by viewModel.greenhouses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedGreenhouse by viewModel.selectedGreenhouse.collectAsState()

    var showPairingDialog by remember { mutableStateOf(false) }
    var showQRScanner by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF2E7D32)

    // Status bar configuration
    SetSystemBars(statusBarColor = primaryColor, darkIcons = false)

    // Load data on startup
    LaunchedEffect(Unit) {
        viewModel.loadUserGreenhouses()
        viewModel.loadDevices()
    }

    // Error handling
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Show snackbar or error dialog
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Perangkat IoT") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
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
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showPairingDialog = true },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Perangkat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // Greenhouse Filter
            GreenhouseFilterSection(
                greenhouses = greenhouses,
                selectedGreenhouse = selectedGreenhouse,
                onGreenhouseSelected = { viewModel.selectGreenhouse(it) }
            )

            // Content based on loading state
            if (isLoading && devices.isEmpty()) {
                LoadingState()
            } else {
                if (devices.isEmpty()) {
                    EmptyState(
                        onAddDeviceClick = { showPairingDialog = true }
                    )
                } else {
                    DevicesList(
                        devices = devices,
                        onDeviceClick = { device ->
                            // TODO: Navigate to device detail
                        },
                        onUnpairDevice = { device ->
                            viewModel.unpairDevice(device.id)
                        }
                    )
                }
            }
        }
    }

    // Pairing Dialog
    if (showPairingDialog) {
        DevicePairingDialog(
            onDismiss = { showPairingDialog = false },
            onPairWithCode = { deviceId, pairingCode, greenhouseId ->
                viewModel.pairDevice(deviceId, greenhouseId, pairingCode)
                showPairingDialog = false
            },
            onScanQR = { showQRScanner = true },
            greenhouses = greenhouses
        )
    }

    // QR Scanner (placeholder)
    if (showQRScanner) {
        // TODO: Implement QR Scanner
        QRScannerPlaceholder(
            onDismiss = { showQRScanner = false },
            onQRScanned = { qrData ->
                // Parse QR data and pair device
                showQRScanner = false
                showPairingDialog = false
            }
        )
    }
}

/**
 * GREENHOUSE FILTER SECTION COMPOSABLE
 *
 * Tujuan:
 * - Memfilter device berdasarkan greenhouse tertentu
 * - Menampilkan dropdown selector untuk memilih greenhouse
 *
 * @author Your Name
 * @since Version 1.0
 * @param greenhouses List greenhouse yang tersedia
 * @param selectedGreenhouse Greenhouse yang sedang dipilih
 * @param onGreenhouseSelected Callback ketika greenhouse dipilih
 */

@Composable
fun GreenhouseFilterSection(
    greenhouses: List<Greenhouse>,
    selectedGreenhouse: Greenhouse?,
    onGreenhouseSelected: (Greenhouse?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Filter Greenhouse",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedGreenhouse?.name ?: "Semua Greenhouse",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.Settings, contentDescription = "Filter")
                }
            }

            // Filter options (simplified)
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    // All greenhouses option
                    Text(
                        "Semua Greenhouse",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onGreenhouseSelected(null)
                                expanded = false
                            }
                            .padding(8.dp),
                        fontWeight = if (selectedGreenhouse == null) FontWeight.Bold else FontWeight.Normal
                    )

                    // Individual greenhouse options
                    greenhouses.forEach { greenhouse ->
                        Text(
                            greenhouse.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onGreenhouseSelected(greenhouse)
                                    expanded = false
                                }
                                .padding(8.dp),
                            fontWeight = if (selectedGreenhouse?.id == greenhouse.id) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

/**
 * DEVICES LIST COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan daftar perangkat IoT dalam format card
 * - Menyediakan actions untuk setiap device (unpair, details)
 *
 * @author Your Name
 * @since Version 1.0
 * @param devices List device IoT yang akan ditampilkan
 * @param onDeviceClick Callback ketika device di-click
 * @param onUnpairDevice Callback untuk unpair device
 */

@Composable
fun DevicesList(
    devices: List<IoTDevice>,
    onDeviceClick: (IoTDevice) -> Unit,
    onUnpairDevice: (IoTDevice) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                onClick = { onDeviceClick(device) },
                onUnpair = { onUnpairDevice(device) }
            )
        }
    }
}

/**
 * DEVICE CARD COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan informasi device dalam format card
 * - Menampilkan status online/offline, jenis device, dan informasi pairing
 *
 * @author Your Name
 * @since Version 1.0
 * @param device Device IoT yang akan ditampilkan
 * @param onClick Callback ketika card di-click
 * @param onUnpair Callback untuk unpair device
 */

@Composable
fun DeviceCard(
    device: IoTDevice,
    onClick: () -> Unit,
    onUnpair: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device.deviceName ?: device.deviceId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        device.deviceType.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Status indicator
                DeviceStatusIndicator(device = device)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Device info row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = "Device ID",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    device.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Serial number row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.QrCode,
                    contentDescription = "Serial Number",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "SN: ${device.serialNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last seen and actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Terakhir dilihat: ${device.lastSeen?.take(10) ?: "Tidak diketahui"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                if (device.isPaired) {
                    TextButton(onClick = onUnpair) {
                        Text("Lepas")
                    }
                }
            }
        }
    }
}

/**
 * DEVICE STATUS INDICATOR COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan status online/offline device dengan visual indicator
 * - Menggunakan warna dan icon yang sesuai
 *
 * @author Your Name
 * @since Version 1.0
 * @param device Device IoT yang statusnya akan ditampilkan
 */

@Composable
fun DeviceStatusIndicator(device: IoTDevice) {
    val isOnline = device.lastSeen != null // Simple online check based on lastSeen

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            if (isOnline) "Online" else "Offline",
            style = MaterialTheme.typography.bodySmall,
            color = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
    }
}

/**
 * DEVICE PAIRING DIALOG COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan dialog untuk pairing device baru
 * - Mendukung pairing dengan kode manual dan QR scan
 *
 * @author Your Name
 * @since Version 1.0
 * @param onDismiss Callback ketika dialog ditutup
 * @param onPairWithCode Callback untuk pairing dengan kode manual
 * @param onScanQR Callback untuk memulai QR scanner
 * @param greenhouses List greenhouse untuk pairing target
 */

@Composable
fun DevicePairingDialog(
    onDismiss: () -> Unit,
    onPairWithCode: (String, String, String) -> Unit,
    onScanQR: () -> Unit,
    greenhouses: List<Greenhouse>
) {
    var deviceId by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var selectedGreenhouseId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair Perangkat Baru") },
        text = {
            Column {
                Text("Pilih metode pairing:")
                Spacer(modifier = Modifier.height(16.dp))

                // QR Scan Option
                Button(
                    onClick = onScanQR,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = "QR Code")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan QR Code")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Atau gunakan kode manual:")
                Spacer(modifier = Modifier.height(8.dp))

                // Manual Pairing Form
                OutlinedTextField(
                    value = deviceId,
                    onValueChange = { deviceId = it },
                    label = { Text("Device ID") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Masukkan Device ID") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { pairingCode = it },
                    label = { Text("Kode Pairing") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Masukkan 6-digit kode") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Greenhouse selector
                if (greenhouses.isNotEmpty()) {
                    OutlinedTextField(
                        value = selectedGreenhouseId,
                        onValueChange = { selectedGreenhouseId = it },
                        label = { Text("Greenhouse Target") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Pilih greenhouse") },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = "Pilih Greenhouse")
                        }
                    )
                }

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (deviceId.isNotBlank() && pairingCode.isNotBlank() && selectedGreenhouseId.isNotBlank()) {
                        onPairWithCode(deviceId, pairingCode, selectedGreenhouseId)
                    } else {
                        errorMessage = "Harap isi semua field"
                    }
                }
            ) {
                Text("Pair Device")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * QR SCANNER PLACEHOLDER COMPOSABLE
 *
 * Tujuan:
 * - Placeholder untuk QR scanner implementation
 * - TODO: Implement actual QR scanner menggunakan ZXing atau ML Kit
 *
 * @author Your Name
 * @since Version 1.0
 * @param onDismiss Callback ketika scanner ditutup
 * @param onQRScanned Callback ketika QR berhasil di-scan
 */

@Composable
fun QRScannerPlaceholder(
    onDismiss: () -> Unit,
    onQRScanned: (String) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scan QR Code") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("QR Scanner Placeholder\n\nTODO: Implement QR Scanner", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Simulate QR scan for demo
                    onQRScanned("{\"deviceId\":\"DEMO123\",\"pairingCode\":\"123456\"}")
                }
            ) {
                Text("Simulate Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * LOADING STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan loading indicator selama data dimuat
 *
 * @author Your Name
 * @since Version 1.0
 */

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Memuat perangkat...")
        }
    }
}

/**
 * EMPTY STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI ketika tidak ada device yang terdaftar
 * - Menyediakan call-to-action untuk menambah device
 *
 * @author Your Name
 * @since Version 1.0
 * @param onAddDeviceClick Callback untuk menambah device baru
 */

@Composable
fun EmptyState(
    onAddDeviceClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.DeviceHub,
                contentDescription = "No Devices",
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Belum ada perangkat",
                style = MaterialTheme.typography.titleMedium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tambahkan perangkat IoT untuk memulai monitoring",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddDeviceClick) {
                Text("Tambah Perangkat Pertama")
            }
        }
    }
}