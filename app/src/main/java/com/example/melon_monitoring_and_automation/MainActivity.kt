package com.example.melon_monitoring_and_automation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.melon_monitoring_and_automation.ui.screen.auth.AuthViewModel
import com.example.melon_monitoring_and_automation.ui.screen.auth.LoginScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.RegisterScreen
import com.example.melon_monitoring_and_automation.ui.screen.control.ControlScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.DashboardScreen
import com.example.melon_monitoring_and_automation.ui.screen.history.HistoryScreen
import com.example.melon_monitoring_and_automation.ui.theme.HydroponicAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HydroponicAppTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val currentUser by authViewModel.currentUser.collectAsState()
                var initialAuthCheckCompleted by remember { mutableStateOf(false) }

                LaunchedEffect(currentUser) {
                    initialAuthCheckCompleted = true
                }

                if (!initialAuthCheckCompleted) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(
                        bottomBar = { BottomNavigationBar(navController = navController) }
                    ) { paddingValues ->
                        AppNavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route,
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Control : Screen("control", "Kontrol", Icons.Default.Settings)
    object History : Screen("history", "Riwayat", Icons.Default.List)
    object Login : Screen("login", "Login", Icons.Default.Home)
    object Register : Screen("register", "Register", Icons.Default.Home)
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }
        composable(Screen.Dashboard.route) { DashboardScreen() }
        composable(Screen.Control.route) { ControlScreen() }
        composable(Screen.History.route) { HistoryScreen() }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Control, Screen.History)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
