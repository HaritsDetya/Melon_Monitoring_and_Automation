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

    // Additional Screens - Outside bottom nav
    object GreenhouseDetail : Screen("greenhouse_detail")
    object ChangePassword : Screen("change_password")

    // 🔹 NEW: IoT Device Pairing Screens
    object DeviceSetupGuide : Screen("device_setup_guide")
    object DevicePairing : Screen("device_pairing")
    object QRScanner : Screen("qr_scanner")
    object DeviceList : Screen("device_list")
}