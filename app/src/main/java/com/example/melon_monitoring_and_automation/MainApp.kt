package com.example.melon_monitoring_and_automation

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.melon_monitoring_and_automation.ui.screen.dashboard.GreenhouseDetailScreen
import com.example.melon_monitoring_and_automation.ui.screen.splash.SplashScreen
import com.example.melon_monitoring_and_automation.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context as? MainActivity

    val authSuccess by authViewModel.authSuccess.collectAsState()
    val authState by authViewModel.authSuccess.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isLoading by authViewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(authSuccess, currentUser, isLoading) {
        println("🔹 [MAIN APP] State Update:")
        println("🔹   - isLoading: $isLoading")
        println("🔹   - authSuccess: $authSuccess")
        println("🔹   - currentUser: ${currentUser?.email}")
        println("🔹 [MAIN APP] Current route: ${navController.currentBackStackEntry?.destination?.route}")
    }

    // Cek jika perlu navigate ke reset password dari deep link
    LaunchedEffect(Unit) {
        if (activity?.shouldNavigateToResetPassword() == true) {
            println("🔹 [MAIN APP] Navigating to reset password from deep link")
            navController.navigate("reset_password") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(1000) // Tunggu setup selesai
        val currentSession = authViewModel.getCurrentSession()
        val currentUser = authViewModel.getCurrentUser()

        println("🔹 [MAIN APP] Session on start: ${currentSession != null}")
        println("🔹 [MAIN APP] User on start: ${currentUser?.email ?: "null"}")
    }

    // 🔹 FIX: Handle deep link navigation
    LaunchedEffect(Unit) {
        // Check jika ada deep link yang perlu dihandle
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val shouldNavigateToReset = sharedPref.getBoolean("should_navigate_to_reset", false)

        if (shouldNavigateToReset) {
            println("🔹 [MAIN APP] Deep link navigation triggered")
            navController.navigate(Screen.ResetPassword.route)
            // Clear flag
            sharedPref.edit().putBoolean("should_navigate_to_reset", false).apply()
        }

        // Check intent extras
        val activity = context as? ComponentActivity
        val navigateTo = activity?.intent?.getStringExtra("NAVIGATE_TO")
        if (navigateTo == "RESET_PASSWORD") {
            println("🔹 [MAIN APP] Intent navigation to ResetPassword")
            navController.navigate(Screen.ResetPassword.route)
            // Clear intent
            activity.intent.removeExtra("NAVIGATE_TO")
        }
    }

    // 🔹 FIX: Simplified auth state handling - hanya di MainApp
    LaunchedEffect(authState, currentUser, isLoading) {
        println("🔹 [MAIN APP] State Update:")
        println("🔹   - isLoading: $isLoading")
        println("🔹   - authSuccess: $authState")
        println("🔹   - currentUser: ${currentUser?.email ?: "null"}")

        if (!isLoading) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            println("🔹 [MAIN APP] Current route: $currentRoute")

            when {
                authState && currentUser != null -> {
                    // User authenticated - navigate to main app
                    if (currentRoute != "main_app_graph" && currentRoute != "main_tabs" &&
                        !currentRoute.isNullOrEmpty() && currentRoute.contains("auth")) {
                        println("🔹 [MAIN APP] ✅ User authenticated, navigating to main app")
                        navController.navigate("main_app_graph") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    }
                }
                !authState && currentUser == null -> {
                    // User not authenticated - navigate to auth
                    if (currentRoute != "auth_graph" && currentRoute != Screen.Login.route &&
                        !currentRoute.isNullOrEmpty() && currentRoute.contains("main_app")) {
                        println("🔹 [MAIN APP] ❌ User not authenticated, navigating to auth")
                        navController.navigate("auth_graph") {
                            popUpTo("main_app_graph") { inclusive = true }
                        }
                    }
                }
            }
        }
    }

    // 🔹 FIX: Initial auth check - dengan delay untuk memastikan setup selesai
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] Initializing app...")
        delay(1000) // Tunggu setup selesai
        authViewModel.enableAutoCheck()
        authViewModel.checkAuthStatus()
    }

    // 🔹 FIXED: Start with Splash screen
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route // Start with splash
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        // Auth Navigation
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

        // Main App Navigation
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
        }
    }
}
