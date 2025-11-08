// ui/navigation/Screen.kt
package com.example.melon_monitoring_and_automation.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ResetPassword : Screen("reset_password")

    // Main App Screens - untuk bottom navigation
    object Dashboard : Screen("dashboard")
    object Control : Screen("control")
    object Profile : Screen("profile")
    object GreenhouseDetail : Screen("greenhouse_detail") // Outside bottom nav
}