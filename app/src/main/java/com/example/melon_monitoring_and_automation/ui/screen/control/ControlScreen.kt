package com.example.melon_monitoring_and_automation.ui.screen.control

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.ui.viewmodel.ControlViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    viewModel: ControlViewModel = hiltViewModel()
) {
    var selectedGreenhouse by remember { mutableStateOf<String?>(null) }
    val greenhouses by viewModel.greenhouses.collectAsState()
    val controlDevices by viewModel.controlDevices.collectAsState()
    val automationSettings by viewModel.automationSettings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()

    // Handle success messages
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            // Show success message or snackbar
            viewModel.clearUpdateSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Device Control") })
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

            // Greenhouse Selector
            GreenhouseSelector(
                greenhouses = greenhouses,
                selectedGreenhouse = selectedGreenhouse,
                onGreenhouseSelected = { selectedGreenhouse = it }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
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
                    }
                } ?: run {
                    // No greenhouse selected
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Blower Control",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto/Manual Mode Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mode Auto")
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
                Text("Blower Manual")
                Switch(
                    checked = controlDevice.fan,
                    onCheckedChange = onBlowerToggle,
                    enabled = !controlDevice.autoMode
                )
            }

            // Temperature thresholds info
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Suhu Auto: ${automationSettings.minTemperature}°C - ${automationSettings.maxTemperature}°C",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Temperature Settings Button
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { showTempSettings = true }
            ) {
                Text("Atur Suhu Auto")
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
                onTemperatureThresholdsChange(min, max)
                showTempSettings = false
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
                "Pompa Nutrisi AB Mix",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Droplet Counter
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Kurangi",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Droplet Display
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$currentDroplets",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color(0xFF4CAF50)
                    )
                    Text("droplet", style = MaterialTheme.typography.bodySmall)
                }

                // Plus Button
                IconButton(
                    onClick = {
                        if (currentDroplets < 50) {
                            currentDroplets++
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Tambah",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OK Button
            Button(
                onClick = { onDropletsChange(currentDroplets) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("OK")
            }
        }
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
                "Pompa Utama",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF388E3C)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status Pompa")
                Switch(
                    checked = controlDevice.pump,
                    onCheckedChange = onPumpToggle
                )
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
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Atur Suhu Auto") },
        text = {
            Column {
                Text("Atur batas suhu untuk mode auto:")
                Spacer(modifier = Modifier.height(16.dp))

                // Min Temperature
                Text("Suhu Minimum (°C):")
                OutlinedTextField(
                    value = minTemp,
                    onValueChange = onMinTempChange,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Max Temperature
                Text("Suhu Maksimum (°C):")
                OutlinedTextField(
                    value = maxTemp,
                    onValueChange = onMaxTempChange,
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
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