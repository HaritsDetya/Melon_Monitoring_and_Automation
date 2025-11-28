/**
 * SPLASH SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Screen pertama yang ditampilkan saat aplikasi dibuka
 * - Melakukan pengecekan status autentikasi user
 * - Mengarahkan user ke halaman yang sesuai berdasarkan status login
 * - Menampilkan UI sambil melakukan initialization checks
 *
 * Fitur:
 * - Auto-redirect berdasarkan auth state
 * - Loading indicator dengan delay minimal
 * - Gradient background yang menarik
 * - Logo dan branding aplikasi
 *
 * @author Your Name
 * @since Version 1.0
 * @param navController Navigator untuk berpindah antar screen
 * @param viewModel ViewModel yang menangani logic autentikasi
 */

package com.example.melon_monitoring_and_automation.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.melon_monitoring_and_automation.R
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    // STATE MANAGEMENT - Collect auth state dari ViewModel
    val authState by viewModel.authSuccess.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // NAVIGATION LOGIC - Handle redirect berdasarkan auth status
    LaunchedEffect(authState, currentUser, isLoading) {
        println("🔹 [SPLASH] Auth Check:")
        println("🔹   - Auth Success: $authState")
        println("🔹   - Current User: ${currentUser?.email}")
        println("🔹   - Loading: $isLoading")

        // Minimum display time untuk splash screen (1.5 detik)
        delay(1500)

        // Jika masih loading, tunggu sampai selesai
        if (isLoading) {
            println("🔹 [SPLASH] Still loading, waiting...")
            delay(1000)
        }

        // DECISION LOGIC - Tentukan halaman tujuan
        when {
            // SCENARIO 1: User sudah login -> Direct ke main app
            authState && currentUser != null -> {
                println("🔹 [SPLASH] ✅ User authenticated, going to main app")
                navController.navigate("main_app_graph") {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            // SCENARIO 2: User belum login -> Direct ke auth flow
            else -> {
                println("🔹 [SPLASH] ❌ User not authenticated, going to login")
                delay(500) // Smooth transition delay
                navController.navigate("auth_graph") {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    // UI COMPOSITION - Splash screen visual elements
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // APP LOGO
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "App Logo - Melon Hydroponic",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // APP TITLE
            Text(
                text = "Melon Hydroponic",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // APP TAGLINE
            Text(
                text = "Smart Monitoring System",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // LOADING INDICATOR
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White
            )
        }
    }
}
