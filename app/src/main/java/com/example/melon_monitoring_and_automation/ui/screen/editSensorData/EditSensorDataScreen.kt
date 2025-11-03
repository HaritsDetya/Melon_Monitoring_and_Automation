package com.example.melon_monitoring_and_automation.ui.screen.editSensorData

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.theme.MainGreen
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSensorDataScreen(
    navController: NavController,
    viewModel: EditSensorDataViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    sensorReadingId: String
) {
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var temperature by remember { mutableStateOf("") }
    var humidity by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf("") }
    var tds by remember { mutableStateOf("") }

    LaunchedEffect(activeGreenhouseId, sensorReadingId) {
        if (!activeGreenhouseId.isNullOrEmpty()) {
            viewModel.loadSensorReading(activeGreenhouseId!!, sensorReadingId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val reading = (uiState as UiState.Success).data
            if (reading != null) {
                temperature = reading.temperature?.toString() ?: ""
                humidity = reading.humidity?.toString() ?: ""
                ph = reading.ph?.toString() ?: ""
                tds = reading.tds?.toString() ?: ""
            } else {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Data Sensor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteSensorReading(sensorReadingId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingIndicator()
                }
                is UiState.Error -> {
                    val errorMessage = (uiState as UiState.Error).message
                    ErrorDialog(
                        message = errorMessage,
                        onDismiss = { viewModel.clearError() }
                    )
                }
                is UiState.Success -> {
                    val readingData = (uiState as UiState.Success).data
                    if (readingData != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OutlinedTextField(
                                value = temperature,
                                onValueChange = { temperature = it },
                                label = { Text("Suhu (°C)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = humidity,
                                onValueChange = { humidity = it },
                                label = { Text("Kelembapan (%)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = ph,
                                onValueChange = { ph = it },
                                label = { Text("pH Air") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = tds,
                                onValueChange = { tds = it },
                                label = { Text("TDS (ppm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    val updatedReading = SensorReading(
                                        id = sensorReadingId,
                                        greenhouse_id = readingData.greenhouse_id,
                                        temperature = temperature.toFloatOrNull(),
                                        humidity = humidity.toFloatOrNull(),
                                        ph = ph.toFloatOrNull(),
                                        tds = tds.toFloatOrNull()
                                    )
                                    viewModel.updateSensorReading(sensorReadingId, updatedReading)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(MainGreen),
                                enabled = true
                            ) {
                                Text("Simpan Perubahan", color = MainText)
                            }
                        }
                    } else {
                        Text("Perangkat berhasil dihapus.", modifier = Modifier.padding(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("Kembali ke Dashboard")
                        }
                    }
                }
                else -> {
                    Text("Memuat data...")
                }
            }
        }
    }
}
