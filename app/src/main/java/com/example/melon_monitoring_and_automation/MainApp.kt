package com.example.melon_monitoring_and_automation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.melon_monitoring_and_automation.ui.navigation.Screen
import com.example.melon_monitoring_and_automation.ui.screen.auth.LoginScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.RegisterScreen
import com.example.melon_monitoring_and_automation.ui.screen.auth.ResetPasswordScreen
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.GreenhouseDetailScreen
import com.example.melon_monitoring_and_automation.ui.screen.profile.ChangePasswordScreen
import com.example.melon_monitoring_and_automation.ui.screen.splash.SplashScreen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? MainActivity

    val authSuccess by authViewModel.authSuccess.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

    // 🔹 PERBAIKAN: State untuk menangani multiple deep links
    var processedDeepLinks by remember { mutableStateOf<Set<String>>(emptySet()) }

    // 🔹 PERBAIKAN: Improved deep link handling
    LaunchedEffect(Unit) {
        delay(1500) // Tunggu app lebih stabil

        // Cek deep link untuk reset password
        val pendingDeepLink = activity?.getPendingDeepLinkInstance()
        if (pendingDeepLink != null && !processedDeepLinks.contains(pendingDeepLink.toString())) {
            println("🔹 [MAIN APP] Processing pending deep link: $pendingDeepLink")

            val fragment = pendingDeepLink.fragment
            when {
                // Valid reset password link
                fragment?.contains("access_token") == true -> {
                    println("🔹 [MAIN APP] ✅ Valid reset password deep link")

                    // Mark as processed
                    processedDeepLinks = processedDeepLinks + pendingDeepLink.toString()

                    // Navigate ke ResetPasswordScreen
                    navController.navigate(Screen.ResetPassword.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
                // Error link
                fragment?.contains("error") == true -> {
                    println("🔹 [MAIN APP] ❌ Error deep link: $fragment")

                    // Mark as processed
                    processedDeepLinks = processedDeepLinks + pendingDeepLink.toString()

                    // Parse error message
                    val errorMessage = parseDeepLinkError(fragment)
                    println("🔹 [MAIN APP] Error message: $errorMessage")

                    // Set error message di ViewModel
                    authViewModel.setErrorMessage(errorMessage)

                    // Navigate ke Login dengan error
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
            activity?.markDeepLinkProcessed()
        }
    }

    // 🔹 FIX: Simplified auth state handling
    LaunchedEffect(authSuccess, currentUser, isLoading) {
        if (!isLoading) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route

            println("🔹 [MAIN APP] Auth State:")
            println("🔹   - Route: $currentRoute")
            println("🔹   - Auth Success: $authSuccess")
            println("🔹   - Current User: ${currentUser?.email}")
            println("🔹   - Loading: $isLoading")

            // Only handle navigation if we're in splash screen
            if (currentRoute == Screen.Splash.route) {
                when {
                    authSuccess && currentUser != null -> {
                        println("🔹 [MAIN APP] ✅ User authenticated, going to main app")
                        navController.navigate("main_app_graph") {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                    else -> {
                        println("🔹 [MAIN APP] ❌ User not authenticated, going to auth")
                        navController.navigate("auth_graph") {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    // 🔹 FIX: Initial auth check
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] Initializing app...")
        delay(500)
        authViewModel.enableAutoCheck()
        authViewModel.checkAuthStatus()
    }

    // 🔹 FIXED: Clean navigation graph
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen - biarkan handle auth decision
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Auth Navigation Graph
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

        navigation(
            startDestination = "main_tabs",
            route = "main_app_graph"
        ) {
            composable("main_tabs") {
                MainAppScreen(navController = navController)
            }
            composable("${Screen.GreenhouseDetail.route}/{greenhouseId}") { backStackEntry ->
                val greenhouseId = backStackEntry.arguments?.getString("greenhouseId") ?: ""
                GreenhouseDetailScreen(
                    greenhouseId = greenhouseId,
                    onBackClick = { navController.popBackStack() }
                )
            }
            // ✅ TAMBAHKAN INI - Route untuk Change Password
            composable("change_password") {
                ChangePasswordScreen(navController = navController)
            }
        }
    }
}

// 🔹 TAMBAHKAN: Fungsi untuk parse error deep link
private fun parseDeepLinkError(errorFragment: String): String {
    return when {
        errorFragment.contains("otp_expired") -> "Link reset password sudah kadaluarsa. Silakan request link baru."
        errorFragment.contains("access_denied") -> "Akses ditolak. Link reset password tidak valid."
        errorFragment.contains("invalid") -> "Link reset password tidak valid."
        errorFragment.contains("Email+link+is+invalid+or+has+expired") -> "Link reset password sudah kadaluarsa atau tidak valid. Silakan request link baru."
        else -> "Terjadi error dengan link reset password. Silakan request link baru."
    }
}
