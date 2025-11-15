package com.example.melon_monitoring_and_automation

import android.net.Uri
import androidx.compose.runtime.*
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

    // 🔹 PERBAIKAN: State untuk track apakah sudah process deep link
    var hasProcessedDeepLink by remember { mutableStateOf(false) }

    // 🔹 FIX: Enhanced initial app startup dengan session restoration
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] 🚀 App starting...")

        // Step 1: Enable auto-check dan check auth status
        authViewModel.enableAutoCheck()

        // Step 2: Beri waktu untuk supabase client initialize dan auth check
        delay(2000)

        println("🔹 [MAIN APP] ✅ App initialization completed")
    }

    // 🔹 FIX: Enhanced deep link handling
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] 🚀 INITIAL DEEP LINK CHECK STARTED")
        delay(1000) // Tunggu lebih lama untuk pastikan auth check selesai

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

    // 🔹 FIX: Enhanced auth state handling
    LaunchedEffect(authSuccess, currentUser, isLoading) {
        println("🔹 [MAIN APP] Auth State - Success: $authSuccess, User: ${currentUser?.email}, Loading: $isLoading")

        if (!isLoading) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            println("🔹 [MAIN APP] Current Route: $currentRoute")

            // Handle navigation based on auth state - hanya jika di splash atau auth flow
            if (currentRoute == Screen.Splash.route || currentRoute?.startsWith("auth") == true) {
                when {
                    authSuccess && currentUser != null -> {
                        println("🔹 [MAIN APP] ✅ User authenticated, navigating to main app")
                        delay(1000) // Beri waktu untuk smooth transition
                        navController.navigate("main_app_graph") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    else -> {
                        if (currentRoute == Screen.Splash.route) {
                            println("🔹 [MAIN APP] ❌ User not authenticated, staying in auth flow")
                            // Biarkan SplashScreen yang handle navigation ke auth
                        }
                    }
                }
            }
        }
    }

    // 🔹 PERBAIKAN: Gunakan callback dari Activity untuk new deep links
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
            activity?.clearDeepLinkListener() // 🔹 Gunakan method clear
        }
    }

    // 🔹 FIX: Initial auth check yang lebih robust
    LaunchedEffect(Unit) {
        println("🔹 [MAIN APP] Initializing app...")
        delay(1000) // Beri waktu lebih untuk initialization
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

// 🔹 EKSTRAK: Fungsi helper untuk process deep link
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
            // Untuk error, bisa langsung mark processed
            activity?.markDeepLinkProcessed()
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }
}
