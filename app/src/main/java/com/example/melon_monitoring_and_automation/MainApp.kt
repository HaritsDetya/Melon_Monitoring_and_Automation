/**
 * MAIN APP COMPOSABLE - ROOT COMPONENT
 *
 * Tujuan:
 * - Root component utama aplikasi yang mengatur navigation graph
 * - Menangani deep link processing untuk reset password flow
 * - Mengelola global state seperti authentication status
 * - Mengatur system bars (status bar) untuk konsistensi UI
 *
 * Architecture:
 * - Single Activity dengan Multiple Composable Screens
 * - Nested Navigation Graphs untuk organized routing
 * - State management dengan ViewModel dan Compose State
 * - Deep link handling dengan Activity integration
 *
 * Navigation Structure:
 * - Splash Screen (Initial)
 * - Auth Graph (Login, Register, Reset Password)
 * - Main App Graph (Dashboard, Control, Profile + Nested Screens)
 *
 * @author Your Name
 * @since Version 1.0
 */

package com.example.melon_monitoring_and_automation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.screen.auth.LoginScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.RegisterScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.ResetPasswordScreen
import com.example.melon_monitoring_and_automation.ui.screen.control.DeviceManagementScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.CreateGreenhouseScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.GreenhouseDetailScreen
import com.example.melon_monitoring_and_automation.ui.screen.profile.ChangePasswordScreen
import com.example.melon_monitoring_and_automation.ui.screen.splash.SplashScreen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun MainApp() {
    // NAVIGATION & DEPENDENCY SETUP
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? MainActivity

    // STATE MANAGEMENT - Collect state dari ViewModel
    val authSuccess by authViewModel.authSuccess.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

    // DEEP LINK STATE - Track processing status
    var hasProcessedDeepLink by remember { mutableStateOf(false) }

    // UI CONFIGURATION - Global status bar color
    val darkGreen = Color(0xFF2E7D32)
    SetSystemBars(statusBarColor = darkGreen, darkIcons = false)

    // APP INITIALIZATION - Setup awal aplikasi
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] 🚀 App starting...")
        authViewModel.enableAutoCheck()
        delay(2000)
        println("🔹 [MAIN APP] ✅ App initialization completed")
    }

    // DEEP LINK PROCESSING - Handle initial deep links
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] 🚀 INITIAL DEEP LINK CHECK STARTED")
        delay(1000)

        val pendingDeepLink = activity?.getPendingDeepLinkInstance()
        println("🔹 [MAIN APP] 🔍 Initial deep link check: $pendingDeepLink")

        if (pendingDeepLink != null && !hasProcessedDeepLink) {
            processDeepLink(pendingDeepLink, navController, activity) {
                hasProcessedDeepLink = true
            }
        } else {
            println("🔹 [MAIN APP] ℹ️ No initial deep link to process")
        }
    }

    // AUTH STATE HANDLING - Redirect berdasarkan authentication status
    LaunchedEffect(authSuccess, currentUser, isLoading) {
        println("🔹 [MAIN APP] Auth State - Success: $authSuccess, User: ${currentUser?.email}, Loading: $isLoading")

        if (!isLoading) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            println("🔹 [MAIN APP] Current Route: $currentRoute")

            // Handle navigation berdasarkan auth status
            if (currentRoute == Screen.Splash.route || currentRoute?.startsWith("auth") == true) {
                when {
                    authSuccess && currentUser != null -> {
                        println("🔹 [MAIN APP] ✅ User authenticated, navigating to main app")
                        delay(1000)
                        navController.navigate("main_app_graph") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    else -> {
                        if (currentRoute == Screen.Splash.route) {
                            println("🔹 [MAIN APP] ❌ User not authenticated, staying in auth flow")
                        }
                    }
                }
            }
        }
    }

    // DEEP LINK LISTENER - Handle new deep links dari Activity
    DisposableEffect(activity) {
        val listener = {
            println("🔹 [MAIN APP] 📢 Activity notified new deep link!")
            val pendingDeepLink = activity?.getPendingDeepLinkInstance()
            if (pendingDeepLink != null && !hasProcessedDeepLink) {
                processDeepLink(pendingDeepLink, navController, activity) {
                    hasProcessedDeepLink = true
                }
            }
        }

        activity?.setDeepLinkListener(listener)

        onDispose {
            activity?.clearDeepLinkListener()
        }
    }

    // INITIAL AUTH CHECK - Check auth status saat app start
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] Initializing app...")
        delay(1000)
        authViewModel.enableAutoCheck()
        authViewModel.checkAuthStatus()
    }

    /**
     * MAIN NAVIGATION GRAPH
     * Struktur hierarchical navigation untuk organized routing
     */
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // SPLASH SCREEN - Initial loading screen
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // AUTH NAVIGATION GRAPH - Authentication flow
        navigation(
            startDestination = Screen.Login.route,
            route = "auth_graph"
        ) {
            composable(Screen.Login.route) {
                LoginScreen(navController = navController)
            }
            composable(Screen.Register.route) {
                RegisterScreen(navController = navController)
            }
            composable(Screen.ResetPassword.route) {
                ResetPasswordScreen(navController = navController)
            }
        }

        // MAIN APP NAVIGATION GRAPH - Authenticated user flow
        navigation(
            startDestination = "main_tabs",
            route = "main_app_graph"
        ) {
            // Main Tabs Screen dengan Bottom Navigation
            composable("main_tabs") {
                MainAppScreen(navController = navController)
            }

            composable(Screen.DeviceManagement.route) {
                DeviceManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // GREENHOUSE DETAIL SCREEN - Detail monitoring untuk greenhouse spesifik
            composable("${Screen.GreenhouseDetail.route}/{greenhouseId}") { backStackEntry ->
                val greenhouseId = backStackEntry.arguments?.getString("greenhouseId") ?: ""
                GreenhouseDetailScreen(
                    greenhouseId = greenhouseId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            // CHANGE PASSWORD SCREEN - Password management untuk logged-in user
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(navController = navController)
            }

            composable(Screen.CreateGreenhouse.route) {
                CreateGreenhouseScreen(
                    onBackClick = { navController.popBackStack() },
                    onSuccess = { greenhouseId ->
                        // Navigate ke greenhouse detail atau kembali ke dashboard
                        navController.navigate("${Screen.GreenhouseDetail.route}/$greenhouseId") {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                )
            }
        }
    }
}

/**
 * PROCESS DEEP LINK - Helper Function
 * Memproses deep link untuk reset password flow
 *
 * @param pendingDeepLink URI deep link yang diterima
 * @param navController Navigator untuk screen routing
 * @param activity MainActivity instance untuk state management
 * @param onProcessed Callback ketika processing selesai
 */
private fun processDeepLink(
    pendingDeepLink: Uri,
    navController: NavController,
    activity: MainActivity?,
    onProcessed: () -> Unit
) {
    println("🔹 [MAIN APP] ✅ PROCESSING DEEP LINK")
    onProcessed()

    val fragment = pendingDeepLink.fragment
    when {
        fragment?.contains("access_token") == true -> {
            println("🔹 [MAIN APP] ✅ Valid reset password deep link - NAVIGATING")
            navController.navigate(Screen.ResetPassword.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
        fragment?.contains("error") == true -> {
            println("🔹 [MAIN APP] ❌ Error deep link")
            activity?.markDeepLinkProcessed()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }
}
