package com.example.melon_monitoring_and_automation

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.melon_monitoring_and_automation.ui.navigation.AppNavHost
import com.example.melon_monitoring_and_automation.ui.screen.auth.AuthViewModel
import com.example.melon_monitoring_and_automation.ui.theme.HydroponicAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HydroponicAppTheme {
                val authViewModel: AuthViewModel = hiltViewModel()
                val sharedViewModel: SharedViewModel = hiltViewModel()
                val navController = rememberNavController()


                val isLoggedIn by authViewModel.authSuccess.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()

                LaunchedEffect(currentUser) {
                    if (currentUser != null && sharedViewModel.activeGreenhouseId.value == null) {
                        val firstGreenhouseId = currentUser!!.greenhouses?.keys?.firstOrNull()
                        if (firstGreenhouseId != null) {
                            sharedViewModel.setActiveGreenhouse(firstGreenhouseId)
                        }
                    }
                }

                AppNavHost(
                    navController = navController,
                    isLoggedIn = isLoggedIn,
                    currentUser = currentUser
                )
            }
        }
    }
}
