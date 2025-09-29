package com.example.melon_monitoring_and_automation.ui.screen.addSensorData

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.theme.MainGreen
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSensorDataScreen(
    navController: NavController,
    viewModel: AddSensorDataViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    var temp by remember { mutableStateOf("") }
    var hum by remember { mutableStateOf("") }
    var ph by remember { mutableStateOf("") }
    var ec by remember { mutableStateOf("") }
    var light by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()

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
            if (isLoading) {
                LoadingIndicator()
            } else {
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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hum,
                        onValueChange = { hum = it },
                        label = { Text("Kelembaban (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ph,
                        onValueChange = { ph = it },
                        label = { Text("pH Air") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ec,
                        onValueChange = { ec = it },
                        label = { Text("EC (mS/cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = light,
                        onValueChange = { light = it },
                        label = { Text("Intensitas Lampu (lux)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            activeGreenhouseId?.let {
                                viewModel.addSensorReading(
                                    it,
                                    temp.toDoubleOrNull() ?: 0.0,
                                    hum.toDoubleOrNull() ?: 0.0,
                                    ph.toDoubleOrNull() ?: 0.0,
                                    ec.toDoubleOrNull() ?: 0.0,
                                    light.toDoubleOrNull() ?: 0.0
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(MainGreen),
                        enabled = temp.isNotBlank() && hum.isNotBlank() && ph.isNotBlank() && ec.isNotBlank() && light.isNotBlank()
                    ) {
                        Text("Tambah Data Sensor", color = MainText)
                    }
                    if (errorMessage != null) {
                        ErrorDialog(message = errorMessage!!, onDismiss = { viewModel.clearError() })
                    }
                }
            }
        }
    }
}
