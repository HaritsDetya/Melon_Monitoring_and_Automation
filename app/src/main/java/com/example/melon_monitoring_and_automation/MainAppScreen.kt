/**
 * MAIN APP SCREEN COMPOSABLE
 *
 * Tujuan:
 * - Main screen untuk user yang sudah terautentikasi
 * - Menyediakan bottom navigation antara Dashboard, Control, dan Profile
 * - Mengelola nested navigation dalam main app flow
 * - Menyediakan consistent UI structure dengan bottom bar
 *
 * Features:
 * - Bottom Navigation dengan 3 tab utama
 * - Nested Navigation Host untuk setiap tab
 * - State preservation antara tab switches
 * - Consistent padding dan layout structure
 *
 * @author Your Name
 * @since Version 1.0
 * @param navController Parent navigator untuk cross-screen navigation
 */

package com.example.melon_monitoring_and_automation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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

/**
 * BOTTOM NAVIGATION ITEM DATA CLASS
 * Representasi item dalam bottom navigation bar
 *
 * @property route Navigation route untuk item
 * @property icon Icon yang ditampilkan
 * @property title Judul yang ditampilkan
 */
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)

/**
 * BOTTOM NAVIGATION ITEMS
 * Daftar item yang ditampilkan di bottom navigation bar
 */
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
    // INNER NAVIGATION - Navigation untuk tab-specific flows
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    /**
     * MAIN LAYOUT - Scaffold dengan Bottom Navigation
     */
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            selectedTextColor = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                            indicatorColor = androidx.compose.ui.graphics.Color(0xFFC8E6C9),
                            unselectedIconColor = androidx.compose.ui.graphics.Color.Gray,
                            unselectedTextColor = androidx.compose.ui.graphics.Color.Gray
                        ),
                        onClick = {
                            // Navigation dengan state preservation
                            innerNavController.navigate(item.route) {
                                popUpTo(innerNavController.graph.findStartDestination().id) {
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
    ) { innerPadding ->
        /**
         * INNER NAVIGATION HOST - Navigation untuk setiap tab
         */
        NavHost(
            navController = innerNavController,
            startDestination = Screen.Dashboard.route,
            modifier = androidx.compose.ui.Modifier.padding(innerPadding)
        ) {
            // DASHBOARD SCREEN - Main monitoring screen
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onGreenhouseClick = { greenhouseId ->
                        // Navigate ke greenhouse detail menggunakan parent navController
                        navController.navigate("${Screen.GreenhouseDetail.route}/$greenhouseId")
                    },
                    onAddGreenhouseClick = {
                        navController.navigate(Screen.CreateGreenhouse.route)
                    }
                )
            }

            // CONTROL SCREEN - Device control dan management
            composable(Screen.Control.route) {
                ControlScreen(
                    onManageDevicesClick = {
                        // Navigate to device list menggunakan parent navController
                        navController.navigate(Screen.DeviceManagement.route)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // PROFILE SCREEN - User profile dan account management
            composable(Screen.Profile.route) {
                ProfileScreen(navController = navController)
            }
        }
    }
}
