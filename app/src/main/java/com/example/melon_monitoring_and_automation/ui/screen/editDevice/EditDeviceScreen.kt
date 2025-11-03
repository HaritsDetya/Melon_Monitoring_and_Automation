package com.example.melon_monitoring_and_automation.ui.screen.editDevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.theme.MainGreen
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceScreen(
    navController: NavController,
    viewModel: EditDeviceViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    deviceId: String
) {
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(false) }

    LaunchedEffect(activeGreenhouseId, deviceId) {
        if (!activeGreenhouseId.isNullOrEmpty()) {
            viewModel.loadDevice(activeGreenhouseId!!, deviceId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val deviceData = (uiState as UiState.Success).data
            if (deviceData != null) {
                name = deviceData.name
                type = deviceData.type
                status = deviceData.status ?: false
            } else {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Perangkat") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.deleteDevice(deviceId) }) {
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
                is UiState.Loading -> LoadingIndicator()
                is UiState.Error -> {
                    val errorMessage = (uiState as UiState.Error).message
                    ErrorDialog(
                        message = errorMessage,
                        onDismiss = {
                            viewModel.clearError()
                            navController.popBackStack()
                        }
                    )
                }
                is UiState.Success -> {
                    val deviceData = (uiState as UiState.Success).data
                    if (deviceData != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Perangkat") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Nama Perangkat") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = type,
                                onValueChange = { type = it },
                                label = { Text("Tipe Perangkat") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Tipe Perangkat") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Status Perangkat:", style = MaterialTheme.typography.bodyLarge)
                                Switch(
                                    checked = status,
                                    onCheckedChange = { isChecked ->
                                        status = isChecked
                                        viewModel.setDeviceStatus(deviceId, isChecked)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    val updatedDevice = Device(
                                        id = deviceId,
                                        greenhouse_id = deviceData.greenhouse_id,
                                        name = name,
                                        type = type,
                                        status = status
                                    )
                                    viewModel.updateDevice(updatedDevice)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(MainGreen),
                                enabled = name.isNotBlank() && type.isNotBlank()
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
