package com.example.melon_monitoring_and_automation.ui.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.melon_monitoring_and_automation.SetSystemBars
import com.example.melon_monitoring_and_automation.ui.viewmodel.CreateGreenhouseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGreenhouseScreen(
    viewModel: CreateGreenhouseViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSuccess: (greenhouseId: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val createSuccess by viewModel.createSuccess.collectAsState()

    val primaryColor = Color(0xFF2E7D32)

    SetSystemBars(statusBarColor = primaryColor, darkIcons = false)

    // Handle success
    LaunchedEffect(createSuccess) {
        createSuccess?.let { response ->
            response.greenhouse?.let { greenhouse ->
                onSuccess(greenhouse.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Greenhouse Baru") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                LoadingIndicator()
            } else {
                CreateGreenhouseForm(
                    name = name,
                    location = location,
                    description = description,
                    onNameChange = { name = it },
                    onLocationChange = { location = it },
                    onDescriptionChange = { description = it },
                    onSubmit = {
                        if (name.isNotBlank() && location.isNotBlank()) {
                            viewModel.createGreenhouse(name, location, description.ifBlank { null })
                        }
                    },
                    errorMessage = errorMessage,
                    isFormValid = name.isNotBlank() && location.isNotBlank()
                )
            }
        }
    }
}

@Composable
fun LoadingIndicator() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text("Membuat greenhouse dan data default...")
    }
}

@Composable
fun CreateGreenhouseForm(
    name: String,
    location: String,
    description: String,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSubmit: () -> Unit,
    errorMessage: String?,
    isFormValid: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Informasi Greenhouse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Greenhouse baru akan otomatis dilengkapi dengan:\n" +
                            "• Pengaturan automasi default\n" +
                            "• Kontrol perangkat default\n" +
                            "• Struktur data sensor",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Error Message
        errorMessage?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nama Greenhouse") },
            placeholder = { Text("Contoh: Greenhouse Melon Utama") },
            leadingIcon = {
                Icon(Icons.Default.Nature, contentDescription = "Nama")
            },
            modifier = Modifier.fillMaxWidth(),
            isError = name.isBlank()
        )

        // Location Field
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Lokasi") },
            placeholder = { Text("Contoh: Jalan Raya No. 123, Kota") },
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = "Lokasi")
            },
            modifier = Modifier.fillMaxWidth(),
            isError = location.isBlank()
        )

        // Description Field (Optional)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Deskripsi (Opsional)") },
            placeholder = { Text("Tambahkan deskripsi tentang greenhouse ini") },
            leadingIcon = {
                Icon(Icons.Default.TextFields, contentDescription = "Deskripsi")
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && errorMessage == null
        ) {
            Text("Buat Greenhouse")
        }

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "ℹ️ Data yang akan dibuat otomatis:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text("• Pengaturan automasi: Suhu 25-38°C, 10 tetesan nutrisi")
                Text("• Kontrol perangkat: Kipas dan pompa dalam kondisi mati")
                Text("• Mode automasi: Non-aktif (manual control)")
            }
        }
    }
}