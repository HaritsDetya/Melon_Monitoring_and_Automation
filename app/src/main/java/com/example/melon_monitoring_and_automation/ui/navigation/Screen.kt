/**
 * NAVIGATION SCREEN DEFINITIONS
 *
 * Tujuan:
 * - Mendefinisikan semua screen routes dalam aplikasi
 * - Menyediakan type-safe navigation dengan sealed class
 * - Mengorganisir screen hierarchy dan grouping
 *
 * Structure:
 * - Auth Screens (Splash, Login, Register, Reset Password)
 * - Main App Screens (Dashboard, Control, Profile)
 * - Additional Screens (Greenhouse Detail, Change Password)
 * - IoT Device Screens (Setup Guide, Pairing, QR Scanner, Device List)
 *
 * @author Your Name
 * @since Version 1.0
 */

package com.example.melon_monitoring_and_automation.ui.navigation

/**
 * SCREEN SEALED CLASS
 * Representasi semua possible screens dalam aplikasi
 *
 * @property route Unique route identifier untuk setiap screen
 */
sealed class Screen(val route: String) {
    // AUTH SCREENS - Authentication flow
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ResetPassword : Screen("reset_password")

    // MAIN APP SCREENS - Bottom navigation items
    object Dashboard : Screen("dashboard")
    object Control : Screen("control")
    object Profile : Screen("profile")

    // ADDITIONAL SCREENS - Outside bottom navigation
    object GreenhouseDetail : Screen("greenhouse_detail")
    object ChangePassword : Screen("change_password")

    object DeviceManagement : Screen("device_management")

    // IOT DEVICE SCREENS - Device pairing dan management
    object DeviceSetupGuide : Screen("device_setup_guide")
    object DevicePairing : Screen("device_pairing")
    object QRScanner : Screen("qr_scanner")
    object DeviceList : Screen("device_list")
}