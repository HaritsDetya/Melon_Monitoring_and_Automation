package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel = hiltViewModel(),
    userId: String,
    systemId: String
) {
    LaunchedEffect(userId, systemId) {
        if (userId.isNotEmpty() && systemId.isNotEmpty()) {
            viewModel.fetchControlData(userId, systemId)
        }
    }
    val waterPumpStatus by viewModel.waterPumpStatus.collectAsState()
    val nutrientPumpAStatus by viewModel.nutrientPumpAStatus.collectAsState()

    val phThreshold by viewModel.phThreshold.collectAsState()
    val irrigationInterval by viewModel.irrigationInterval.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kontrol Sistem") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Kontrol Manual", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pompa Air Utama")
                Switch(
                    checked = waterPumpStatus,
                    onCheckedChange = { isChecked ->
                        viewModel.setWaterPumpStatus(userId, systemId, isChecked)
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pompa Nutrisi A")
                Switch(
                    checked = nutrientPumpAStatus,
                    onCheckedChange = { isChecked ->
                        viewModel.setNutrientPumpAStatus(userId, systemId, isChecked)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Pengaturan Otomatis", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = phThreshold.toString(),
                onValueChange = { newValue ->
                    newValue.toDoubleOrNull()?.let {
                        viewModel.setPhThreshold(it)
                    }
                },
                label = { Text("Ambang Batas pH Otomatis") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Interval Irigasi Otomatis (menit)")
            Slider(
                value = irrigationInterval.toFloat(),
                onValueChange = { newValue ->
                    viewModel.setIrrigationInterval(newValue.toInt())
                },
                valueRange = 1f..60f,
                steps = 58,
                modifier = Modifier.fillMaxWidth()
            )
            Text(text = "Setiap $irrigationInterval menit")

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.saveAutomaticSettings(userId, systemId) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Pengaturan Otomatis")
            }
        }
    }
}
