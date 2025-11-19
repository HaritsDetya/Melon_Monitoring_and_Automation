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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel = hiltViewModel(),
    onManageDevicesClick: () -> Unit = {}
) {
    var selectedGreenhouse by remember { mutableStateOf<String?>(null) }
    val greenhouses by viewModel.greenhouses.collectAsState()
    val controlDevices by viewModel.controlDevices.collectAsState()
    val automationSettings by viewModel.automationSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    val darkGreen = Color(0xFF2E7D32)

    // Set status bar
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // Handle success messages
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            // Show success message or snackbar
            viewModel.clearUpdateSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontrol Perangkat") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkGreen,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Tambahkan button untuk manage devices
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
            // Error Message
            if (!errorMessage.isNullOrEmpty()) {
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
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearErrorMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }
            }

            // 🔹 PERBAIKAN: Gunakan Column dengan verticalScroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Greenhouse Selector
                GreenhouseSelector(
                    greenhouses = greenhouses,
                    selectedGreenhouse = selectedGreenhouse,
                    onGreenhouseSelected = { selectedGreenhouse = it }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    selectedGreenhouse?.let { greenhouseId ->
                        val controlDevice = controlDevices[greenhouseId]
                        val settings = automationSettings[greenhouseId]

                        if (controlDevice != null && settings != null) {
                            // Blower Control Section
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

                            // Nutrient Pump Control Section
                            NutrientPumpControlSection(
                                automationSettings = settings,
                                onDropletsChange = { droplets ->
                                    viewModel.setNutrientDroplets(greenhouseId, droplets)
                                }
                            )

                            // Additional Pump Control
                            PumpControlSection(
                                controlDevice = controlDevice,
                                onPumpToggle = { enabled ->
                                    viewModel.togglePump(greenhouseId, enabled)
                                }
                            )

                            // 🔹 TAMBAHKAN: Spacer untuk memberikan ruang di bagian bawah
                            Spacer(modifier = Modifier.height(32.dp))
                        } else {
                            // Data tidak tersedia
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
                    } ?: run {
                        // No greenhouse selected
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
                }
            }
        }
    }
}

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
            Text(
                "Kontrol Blower",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto/Manual Mode Switch
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

            // Manual Blower Control (disabled in auto mode)
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

            // Temperature thresholds info
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

            // Temperature Settings Button
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

    // Temperature Settings Dialog
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

            // Droplet Counter
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
                    // Minus Button
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

                    // Droplet Display
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

                    // Plus Button
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

            // OK Button
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

    // Confirmation Dialog
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

            // Status indicator
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

    // Validasi input
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

                // Error message
                tempError?.let { error ->
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Min Temperature
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

                // Max Temperature
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
