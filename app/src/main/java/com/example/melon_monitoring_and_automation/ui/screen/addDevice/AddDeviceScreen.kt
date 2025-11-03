package com.example.melon_monitoring_and_automation.ui.screen.addDevice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun AddDeviceScreen(
    navController: NavController,
    viewModel: AddDeviceViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    var name by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(false) }
    var type by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()
    val activeGreenhouseId by dashboardViewModel.activeGreenhouseId.collectAsState()

    val isFormEnabled = uiState !is UiState.Loading
    val isButtonEnabled = isFormEnabled && name.isNotBlank() && type.isNotBlank() && activeGreenhouseId != null

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Perangkat Baru") },
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
                        onDismiss = { viewModel.resetState() }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama Perangkat") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Nama Perangkat") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("Tipe Perangkat (e.g. pump, light)") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Tipe Perangkat") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = isFormEnabled
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { activeGreenhouseId?.let { viewModel.addDevice(it, name, status, type) } },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(MainGreen),
                            enabled = isButtonEnabled
                        ) {
                            if (uiState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Tambah Perangkat", color = MainText)
                            }
                        }
                    }
                }
            }
        }
    }
}
