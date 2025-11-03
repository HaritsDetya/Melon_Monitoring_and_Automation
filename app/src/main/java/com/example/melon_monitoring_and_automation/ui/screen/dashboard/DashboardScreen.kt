package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.ui.components.GreenhouseSelector
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.components.PlantCard
import com.example.melon_monitoring_and_automation.ui.components.SensorCard
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
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
    navController: NavController
) {
    val greenhouses by viewModel.greenhouses.collectAsState()
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()
    val sensorState = viewModel.sensorDataState.collectAsState().value
    val plantsState = viewModel.plants.collectAsState().value

    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserGreenhouses()
    }

    LaunchedEffect(greenhouses) {
        if (greenhouses.isNotEmpty()) {
            val currentId = activeGreenhouseId
            if (currentId == null || greenhouses.find { it.id == currentId } == null) {
                sharedViewModel.setActiveGreenhouse(greenhouses.first().id)
            }
        }
    }

    LaunchedEffect(activeGreenhouseId) {
        if (!activeGreenhouseId.isNullOrEmpty()) {
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
        if (greenhouses.isNotEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                GreenhouseSelector(
                    greenhouses = greenhouses,
                    activeGreenhouseId = activeGreenhouseId,
                    onGreenhouseSelected = { id ->
                        sharedViewModel.setActiveGreenhouse(id)
                    },
                    navController = navController
                )
                Spacer(modifier = Modifier.height(16.dp))
                when {
                    sensorState is UiState.Loading || plantsState is UiState.Loading -> {
                        LoadingIndicator()
                    }
                    sensorState is UiState.Error -> {
                        Text(text = sensorState.message, color = MaterialTheme.colorScheme.error)
                    }
                    plantsState is UiState.Error -> {
                        Text(text = plantsState.message, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {
                        HydroponicDashboardContent(
                            sensorState = sensorState as UiState.Success,
                            plantsState = plantsState as UiState.Success,
                            navController = navController
                        )
                    }
                }
            }
        }else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValue)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Anda belum memiliki Greenhouse. Silakan tambah satu.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { navController.navigate(Screen.AddGreenhouse.route) }) {
                    Text("Tambah Greenhouse Baru")
                }
            }
        }
    }
}

@Composable
fun HydroponicDashboardContent(
    sensorState: UiState.Success<SensorReading?>,
    plantsState: UiState.Success<List<Plant>>,
    navController: NavController
) {
    val sensorReading = sensorState.data
    val plants = plantsState.data

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Bagian Data Sensor ---
        item {
            Text(
                text = "Data Sensor",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (sensorReading != null) {
            item {
                SensorCard(
                    title = "Suhu & Kelembapan",
                    value = "${sensorReading.temperature?.let { "$it°C" } ?: "-"} / ${sensorReading.humidity?.let { "$it%" } ?: "-"} ",
                    desc = "Kondisi Lingkungan"
                )
            }
            item {
                SensorCard(
                    title = "pH Air",
                    value = sensorReading.ph?.let { String.format("%.2f", it) } ?: "-",
                    desc = "Kadar keasaman larutan"
                )
            }
            item {
                SensorCard(
                    title = "TDS (Nutrisi)",
                    value = sensorReading.tds?.let { "${String.format("%.2f", it)} ppm" } ?: "-",
                    desc = "Konsentrasi nutrisi"
                )
            }
            item {
                val recordedAt = sensorReading.recorded_at?.let {
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it.toLongOrNull() ?: System.currentTimeMillis()))
                } ?: "-"
                SensorCard(
                    title = "Diperbarui terakhir",
                    value = recordedAt,
                    desc = "Waktu pembaruan"
                )
            }
        } else {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Belum ada data sensor untuk greenhouse ini.", modifier = Modifier.padding(16.dp))
                    Button(onClick = { navController.navigate(Screen.AddSensorData.route) }) {
                        Text("Tambah Data Sensor")
                    }
                }
            }
        }

        // --- Bagian Data Tanaman ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Data Tanaman",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (plants.isNotEmpty()) {
            items(
                items = plants,
                key = { plant -> plant.id ?: plant.hashCode() }
            ) { plant ->
                PlantCard(
                    plant = plant,
                    onEdit = { plantId -> navController.navigate("${Screen.EditPlant.route}/$plantId") },
                    onDelete = { plantId -> /* TODO: Implementasi logika hapus */ }
                )
            }
        } else {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Belum ada tanaman di greenhouse ini.", modifier = Modifier.padding(16.dp))
                    Button(onClick = { navController.navigate(Screen.AddPlant.route) }) {
                        Text("Tambah Tanaman")
                    }
                }
            }
        }
    }
}
