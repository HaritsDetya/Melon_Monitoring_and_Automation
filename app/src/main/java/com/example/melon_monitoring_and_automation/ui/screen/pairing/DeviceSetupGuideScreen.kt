package com.example.melon_monitoring_and_automation.ui.screen.pairing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.melon_monitoring_and_automation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSetupGuideScreen(
    onBackClick: () -> Unit,
    onStartPairing: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Setup Perangkat") },
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
            Text(
                text = "Cara Menghubungkan Perangkat IoT",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            GuideStep(
                stepNumber = 1,
                title = "Nyalakan Perangkat",
                description = "Pastikan perangkat IoT sudah dinyalakan dan LED indicator menyala"
            )

            GuideStep(
                stepNumber = 2,
                title = "Cari Informasi Device",
                description = "Cari sticker di belakang perangkat yang berisi:\n• Device ID\n• Kode Pairing 6 digit\n• QR Code (jika ada)"
            )

            GuideStep(
                stepNumber = 3,
                title = "Siapkan Koneksi",
                description = "Pastikan perangkat dan smartphone terhubung ke jaringan WiFi yang sama"
            )

            GuideStep(
                stepNumber = 4,
                title = "Proses Pairing",
                description = "Gunakan fitur scan QR code atau input manual Device ID dan kode pairing"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Start Pairing Button
            Button(
                onClick = onStartPairing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(painter = painterResource(id = R.drawable.qr_code_scanner), contentDescription = "Start Pairing")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mulai Proses Pairing")
            }

            // Troubleshooting Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Troubleshooting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Pastikan perangkat dalam jangkauan WiFi\n" +
                                "• Cek daya baterai perangkat\n" +
                                "• Restart perangkat jika diperlukan\n" +
                                "• Hubungi support jika masalah berlanjut",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun GuideStep(
    stepNumber: Int,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Step Number
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
