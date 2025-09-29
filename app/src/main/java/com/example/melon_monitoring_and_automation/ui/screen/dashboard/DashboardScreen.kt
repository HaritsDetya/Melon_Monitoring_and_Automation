package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.components.GreenhouseSelector
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.components.SensorCard
import com.example.melon_monitoring_and_automation.ui.screen.auth.AuthViewModel
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val greenhouses by viewModel.greenhouses.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()
    val sensorState by viewModel.sensorDataState.collectAsState()
    val plants by viewModel.plants.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserGreenhouses()
    }

    LaunchedEffect(activeGreenhouseId) {
        if (activeGreenhouseId != null) {
            viewModel.loadLatestSensorData(activeGreenhouseId!!)
            viewModel.loadGreenhousePlants(activeGreenhouseId!!)
        }
    }

    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("id", "ID")))
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Selamat Datang, ${currentUser?.username ?: "Pengguna"}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MainText
                        )
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MainText.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MainText
                        )
                    }
                }
            )
        }
    ) { paddingValue ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValue)
                .padding(16.dp, 16.dp, 16.dp, 0.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {

            if (greenhouses.isNotEmpty()) {
                GreenhouseSelector(
                    greenhouses = greenhouses,
                    activeGreenhouseId = activeGreenhouseId,
                    onGreenhouseSelected = { id ->
                        sharedViewModel.setActiveGreenhouse(id)
                    },
                    navController = navController
                )
            } else {
                Text(
                    text = "Anda belum memiliki Greenhouse. Silakan tambah satu.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                Button(onClick = { navController.navigate("add_greenhouse") }) {
                    Text("Tambah Greenhouse Baru")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { navController.navigate("add_sensor_data") }) {
                    Text("Tambah Data Sensor")
                }
                OutlinedButton(onClick = { navController.navigate("add_plant") }) {
                    Text("Tambah Tanaman")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (activeGreenhouseId != null) {
                when (sensorState) {
                    is UiState.Loading -> LoadingIndicator()
                    is UiState.Error -> {
                        val message = (sensorState as UiState.Error).message
                        Text(text = message, color = MaterialTheme.colorScheme.error)
                    }
                    is UiState.Success -> {
                        val data = (sensorState as UiState.Success<Pair<Long, SensorReading>>).data
                        HydroponicDashboardContent(data,plants)
                    }
                }
            } else {
                Text(
                    text = "Pilih atau tambahkan Greenhouse.",
                    modifier = Modifier.padding(top = 32.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun HydroponicDashboardContent(data: Pair<Long, SensorReading>, plants: List<Plant>) {
    val (timestamp, sensorReading) = data
    val (plant) = plants
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bagian data sensor

        item {
            Text(
                text = "Data Sensor",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            SensorCard(
                title = "Suhu & Kelembapan",
                value = "${sensorReading.temperature}°C / ${sensorReading.humidity}%",
                desc = "Kondisi Lingkungan"
            )
        }
        item {
            SensorCard(
                title = "pH Air",
                value = String.format("%.2f", sensorReading.ph),
                desc = "Kadar keasaman larutan"
            )
        }
        item {
            SensorCard(
                title = "EC (Nutrisi)",
                value = "${String.format("%.2f", sensorReading.ec)} mS/cm",
                desc = "Konsentrarsi nutrisi"
            )
        }
        item {
            SensorCard(
                title = "Intensitas Lampu",
                value = "${sensorReading.light} lux",
                desc = "Status kekuatan pencahayaan"
            )
        }
        item {
            SensorCard(
                title = "Diperbarui terakhir",
                value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp)),
                desc = "Waktu pembaruan"
            )
        }

        // Bagian data tanaman
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data Tanaman",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            SensorCard(
                title = "Nama Tanaman",
                value = plant.name,
                desc = "ID Tanaman: ${plant.id}"
            )
        }

        item {
            SensorCard(
                title = "Tipe",
                value = plant.type,
                desc = "Ditanam pada: ${plant.planted_at}"
            )
        }
    }
}
