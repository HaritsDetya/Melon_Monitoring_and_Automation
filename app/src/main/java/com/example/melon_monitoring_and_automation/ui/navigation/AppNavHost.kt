package com.example.melon_monitoring_and_automation.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import com.example.melon_monitoring_and_automation.ui.screen.auth.LoginScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.RegisterScreen
import com.example.melon_monitoring_and_automation.ui.screen.control.ControlScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.DashboardScreen
import com.example.melon_monitoring_and_automation.ui.screen.history.HistoryScreen
import com.example.melon_monitoring_and_automation.SharedViewModel
import com.example.melon_monitoring_and_automation.domain.model.User
import com.example.melon_monitoring_and_automation.ui.screen.addDevice.AddDeviceScreen
import com.example.melon_monitoring_and_automation.ui.screen.addGreenhouse.AddGreenhouseScreen
import com.example.melon_monitoring_and_automation.ui.screen.addPlant.AddPlantScreen
import com.example.melon_monitoring_and_automation.ui.screen.addSensorData.AddSensorDataScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Control : Screen("control", "Kontrol", Icons.Default.Settings)
    object History : Screen("history", "Riwayat", Icons.Default.List)
    object Login : Screen("login", "Login")
    object Register : Screen("register", "Register")

    object AddGreenhouse : Screen("add_greenhouse", "Tambah Greenhouse", Icons.Default.Add)
    object AddDevice : Screen("add_device", "Tambah Perangkat", Icons.Default.Add)
    object AddPlant : Screen("add_plant", "Tambah Tanaman", Icons.Default.Add)
    object AddSensorData : Screen("add_sensor_data", "Tambah Data Sensor", Icons.Default.Add)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavHost(
    navController: NavHostController,
    isLoggedIn: Boolean,
    currentUser: User?
) {
    val startDestination = if (isLoggedIn) "main_app_graph" else Screen.Login.route
    val sharedViewModel: SharedViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        navigation(
            startDestination = Screen.Dashboard.route,
            route = "main_app_graph"
        ) {
            composable(Screen.Dashboard.route) {
                MainScreen(
                    navController = navController
                ) {
                    DashboardScreen(
                        sharedViewModel = sharedViewModel,
                        authViewModel = hiltViewModel(),
                        navController = navController
                    )
                }
            }
            composable(Screen.Control.route) {
                MainScreen(
                    navController = navController,
                ) {
                    ControlScreen(
                        sharedViewModel = sharedViewModel,
                        navController = navController
                    )
                }
            }
            composable(Screen.History.route) {
                MainScreen(
                    navController = navController,
                ) {
                    HistoryScreen(sharedViewModel = sharedViewModel)
                }
            }
            composable(Screen.AddGreenhouse.route) {
                AddGreenhouseScreen(navController = navController)
            }
            composable(Screen.AddSensorData.route) {
                AddSensorDataScreen(navController = navController)
            }
            composable(Screen.AddPlant.route) {
                AddPlantScreen(navController = navController)
            }
            composable(Screen.AddDevice.route) {
                AddDeviceScreen(navController = navController)
            }
        }
    }
}

@Composable
fun MainScreen(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = { BottomNavigationBar(navController = navController) }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content(Modifier.fillMaxSize())
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(Screen.Dashboard, Screen.Control, Screen.History)
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        items.forEach { screen ->
            NavigationBarItem(
                icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                label = { Text(screen.title) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
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
