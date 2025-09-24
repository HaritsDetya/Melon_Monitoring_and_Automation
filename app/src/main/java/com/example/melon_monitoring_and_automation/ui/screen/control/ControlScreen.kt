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
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val devicesWithStatus by viewModel.devicesWithStatus.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()

    LaunchedEffect(activeGreenhouseId) {
        if (activeGreenhouseId != null) {
            viewModel.loadGreenhouseDevices(activeGreenhouseId!!)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kontrol Sistem") }) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Kontrol Manual", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (activeGreenhouseId != null) {
                if (isLoading) {
                    LoadingIndicator()
                } else {
                    devicesWithStatus.forEach { device ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(device.name)
                            Switch(
                                checked = device.status,
                                onCheckedChange = { isChecked ->
                                    val ghId = activeGreenhouseId
                                    if (ghId != null) {
                                        viewModel.setDeviceStatus(ghId, device.id, isChecked)
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Text("Pilih Greenhouse untuk mengontrol perangkat.")
            }
        }
    }
}
