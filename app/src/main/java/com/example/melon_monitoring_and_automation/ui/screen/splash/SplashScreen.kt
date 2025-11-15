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
    val authState by viewModel.authSuccess.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    // 🔹 FIX: Enhanced auth handling dengan delay yang lebih baik
    LaunchedEffect(authState, currentUser, isLoading) {
        println("🔹 [SPLASH] Auth Check:")
        println("🔹   - Auth Success: $authState")
        println("🔹   - Current User: ${currentUser?.email}")
        println("🔹   - Loading: $isLoading")

        // Tunggu minimal 1.5 detik untuk splash screen dan pastikan auth check selesai
        delay(1500)

        // Jika masih loading, tunggu sampai selesai
        if (isLoading) {
            println("🔹 [SPLASH] Still loading, waiting...")
            delay(1000)
        }

        when {
            // User sudah login - langsung ke main app
            authState && currentUser != null -> {
                println("🔹 [SPLASH] ✅ User authenticated, going to main app")
                navController.navigate("main_app_graph") {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            // User belum login - ke login screen
            else -> {
                println("🔹 [SPLASH] ❌ User not authenticated, going to login")
                // Beri delay sedikit untuk smooth transition
                delay(500)
                navController.navigate("auth_graph") {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    // 🔹 FIX: Force auth check ketika splash screen dimulai
//    LaunchedEffect(Unit) {
//        println("🔹 [SPLASH] Splash screen started - forcing auth check")
//        viewModel.forceAuthCheck()
//    }

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
            Image(
                painter = painterResource(id = R.drawable.plant),
                contentDescription = "App Logo",
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Melon Hydroponic",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Smart Monitoring System",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color.White.copy(alpha = 0.8f)
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White
            )
        }
    }
}