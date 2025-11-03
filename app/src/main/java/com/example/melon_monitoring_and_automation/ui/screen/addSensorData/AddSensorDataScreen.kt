package com.example.melon_monitoring_and_automation.ui.screen.addSensorData

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.DashboardViewModel
import com.example.melon_monitoring_and_automation.ui.theme.MainGreen
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSensorDataScreen(
    navController: NavController,
    viewModel: AddSensorDataViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    var temp by remember { mutableStateOf("") }
    var hum by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf("") }
    var tds by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val activeGreenhouseId by dashboardViewModel.activeGreenhouseId.collectAsState()

    val isFormEnabled = uiState !is UiState.Loading
    val isFormValid = temp.isNotBlank() && hum.isNotBlank() && ph.isNotBlank() && tds.isNotBlank()
    val isButtonEnabled = isFormEnabled && isFormValid && !activeGreenhouseId.isNullOrEmpty()

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Data Sensor") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                is UiState.Loading -> {
                    LoadingIndicator()
                }
                is UiState.Error -> {
                    val errorMessage = (uiState as UiState.Error).message
                    ErrorDialog(message = errorMessage) {
                        viewModel.resetState()
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = temp,
                            onValueChange = { temp = it },
                            label = { Text("Suhu (°C)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        OutlinedTextField(
                            value = hum,
                            onValueChange = { hum = it },
                            label = { Text("Kelembaban (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        OutlinedTextField(
                            value = ph,
                            onValueChange = { ph = it },
                            label = { Text("pH Air") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        OutlinedTextField(
                            value = tds,
                            onValueChange = { tds = it },
                            label = { Text("TDS (ppm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = {
                                activeGreenhouseId?.let {
                                    viewModel.addSensorReading(
                                        greenhouseId = it,
                                        temperature = temp.toFloatOrNull(),
                                        humidity = hum.toFloatOrNull(),
                                        ph = ph.toFloatOrNull(),
                                        tds = tds.toFloatOrNull()
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(MainGreen),
                            enabled = isButtonEnabled
                        ) {
                            Text("Tambah Data Sensor", color = MainText)
                        }
                    }
                }
            }
        }
    }
}
