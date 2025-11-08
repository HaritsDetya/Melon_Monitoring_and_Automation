
package com.example.melon_monitoring_and_automation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.screen.control.ControlScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.DashboardScreen
import com.example.melon_monitoring_and_automation.ui.screen.profile.ProfileScreen

// Data class untuk bottom nav items
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Dashboard.route,
        icon = Icons.Default.Home,
        title = "Dashboard"
    ),
    BottomNavItem(
        route = Screen.Control.route,
        icon = Icons.Default.Settings,
        title = "Control"
    ),
    BottomNavItem(
        route = Screen.Profile.route,
        icon = Icons.Default.Person,
        title = "Profile"
    )
)

@Composable
fun MainAppScreen(
    navController: NavController
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            innerNavController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                popUpTo(innerNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // re-selecting the same item
                                launchSingleTop = true
                                // Restore state when re-selecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // Nested NavHost untuk bottom navigation screens
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            // Dashboard Screen
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onGreenhouseClick = { greenhouseId ->
                        // Navigate to greenhouse detail using parent navController
                        navController.navigate("${Screen.GreenhouseDetail.route}/$greenhouseId")
                    }
                )
            }

            // Control Screen
            composable(Screen.Control.route) {
                ControlScreen()
            }

            // Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
        }
    }
}
