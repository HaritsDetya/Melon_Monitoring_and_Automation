package com.example.melon_monitoring_and_automation.ui.screen.editPlant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.ui.components.ErrorDialog
import com.example.melon_monitoring_and_automation.ui.components.LoadingIndicator
import com.example.melon_monitoring_and_automation.ui.theme.MainGreen
import com.example.melon_monitoring_and_automation.ui.theme.MainText
import com.example.melon_monitoring_and_automation.ui.wrapper.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    navController: NavController,
    viewModel: EditPlantViewModel = hiltViewModel(),
    sharedViewModel: SharedViewModel = hiltViewModel(),
    plantId: String
) {
    val activeGreenhouseId by sharedViewModel.activeGreenhouseId.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var plantedAt by remember { mutableStateOf("") }

    LaunchedEffect(activeGreenhouseId, plantId) {
        if (!activeGreenhouseId.isNullOrEmpty()) {
            viewModel.loadPlant(activeGreenhouseId!!, plantId)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            val plantData = (uiState as UiState.Success).data
            if (plantData != null) {
                name = plantData.name
                variety = plantData.variety
                plantedAt = plantData.plant_date
            } else {
                navController.popBackStack()
            }
        } else if (uiState is UiState.Error) {
            // Handle error
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Data Tanaman") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deletePlant(plantId)
                    }) {
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
                        onDismiss = {
                            viewModel.clearError()
                            navController.popBackStack()
                        }
                    )
                }
                is UiState.Success -> {
                    val plantData = (uiState as UiState.Success).data
                    if (plantData != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nama Tanaman") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Nama Tanaman") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = variety,
                                onValueChange = { variety = it },
                                label = { Text("Jenis Tanaman") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = "Jenis Tanaman") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = plantedAt,
                                onValueChange = { plantedAt = it },
                                label = { Text("Tanggal Tanam") },
                                leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Tanggal Tanam") },
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    val updatedPlant = Plant(
                                        id = plantId,
                                        greenhouse_id = plantData.greenhouse_id,
                                        name = name,
                                        variety = variety,
                                        plant_date = plantedAt
                                    )
                                    viewModel.updatePlant(updatedPlant)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(MainGreen),
                                enabled = name.isNotBlank() && variety.isNotBlank()
                            ) {
                                Text("Simpan Perubahan", color = MainText)
                            }
                        }
                    } else {
                        Text("Tanaman berhasil dihapus.", modifier = Modifier.padding(16.dp))
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
