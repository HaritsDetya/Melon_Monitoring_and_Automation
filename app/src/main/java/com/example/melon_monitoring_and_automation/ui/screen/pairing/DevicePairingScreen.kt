package com.example.melon_monitoring_and_automation.ui.screen.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.R
import com.example.melon_monitoring_and_automation.ui.viewmodel.DevicePairingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    onDevicePaired: (String) -> Unit
) {
    val viewModel: DevicePairingViewModel = hiltViewModel()
    val context = LocalContext.current

    // Collect states
    val pairingState by viewModel.pairingState.collectAsStateWithLifecycle()
    val deviceId by viewModel.deviceId.collectAsStateWithLifecycle()
    val pairingCode by viewModel.pairingCode.collectAsStateWithLifecycle()
    val selectedGreenhouseId by viewModel.selectedGreenhouseId.collectAsStateWithLifecycle()

    // Handle pairing success
    LaunchedEffect(pairingState) {
        if (pairingState is DevicePairingViewModel.PairingState.Success) {
            val successState = pairingState as DevicePairingViewModel.PairingState.Success
            selectedGreenhouseId?.let { greenhouseId ->
                onDevicePaired(greenhouseId)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hubungkan Perangkat") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.qr_code_scanner),
                        contentDescription = "QR Scanner",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Scan QR code yang ada di perangkat IoT Anda",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            navController.navigate("qr_scanner")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Buka QR Scanner")
                    }
                }
            }

            // Manual Entry Option
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Input Manual",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = deviceId,
                        onValueChange = viewModel::onDeviceIdChange,
                        label = { Text("Device ID") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: DEV-123456") }
                    )

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = viewModel::onPairingCodeChange,
                        label = { Text("Kode Pairing (6 digit)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Contoh: 123456") }
                    )

                    // Loading state
                    if (pairingState is DevicePairingViewModel.PairingState.Loading) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Error state
                    if (pairingState is DevicePairingViewModel.PairingState.Error) {
                        val errorState = pairingState as DevicePairingViewModel.PairingState.Error
                        Text(
                            text = errorState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = { viewModel.pairDevice() },
                        enabled = pairingState !is DevicePairingViewModel.PairingState.Loading &&
                                deviceId.isNotBlank() && pairingCode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Hubungkan Perangkat")
                    }
                }
            }
        }
    }
}
