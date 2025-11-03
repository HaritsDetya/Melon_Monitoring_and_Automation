package com.example.melon_monitoring_and_automation

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
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

                LaunchedEffect(Unit) {
                    intent.data?.let { uri ->
                        if (uri.toString().contains("access_token")) {
                            authViewModel.handleResetPasswordDeepLink(uri)
                            intent.data = null
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    authViewModel.navigateToPasswordReset.collect {
                        navController.navigate("reset_password_in_app")
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
