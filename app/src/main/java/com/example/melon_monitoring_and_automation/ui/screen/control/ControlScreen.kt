package com.example.melon_monitoring_and_automation.ui.screen.control

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    viewModel: ControlViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel()
) {
    val devicesState by viewModel.devices.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(activeGreenhouseId) {
        if (!activeGreenhouseId.isNullOrEmpty()) {
            viewModel.loadGreenhouseDevices(activeGreenhouseId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kontrol Sistem") },
                actions = {
                    if (!activeGreenhouseId.isNullOrEmpty()) {
                        IconButton(onClick = { navController.navigate(Screen.AddDevice.route) }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Perangkat")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(text = "Kontrol Manual", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (!activeGreenhouseId.isNullOrEmpty()) {
                when (devicesState) {
                    is UiState.Loading -> {
                        LoadingIndicator()
                    }
                    is UiState.Error -> {
                        Text(
                            text = (devicesState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    is UiState.Success -> {
                        val devices = (devicesState as UiState.Success).data
                        if (devices.isNotEmpty()) {
                            LazyColumn {
                                items(devices) { device ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            device.name,
                                            modifier = Modifier.clickable {
                                                navController.navigate("${Screen.EditDevice.route}/${device.id}")
                                            }
                                        )
                                        Switch(
                                            checked = device.status ?: false,
                                            onCheckedChange = { isChecked ->
                                                scope.launch {
                                                    viewModel.setDeviceStatus(device.id!!, isChecked)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Text("Tidak ada perangkat yang ditemukan.")
                        }
                    }
                    else -> {
                        Text("Pilih Greenhouse untuk mengontrol perangkat.")
                    }
                }
            } else {
                Text("Pilih Greenhouse untuk mengontrol perangkat.")
            }
        }
    }
}
