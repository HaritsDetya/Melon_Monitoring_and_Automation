/**
 * SYSTEM BARS COMPOSABLE - GLOBAL UI CONFIGURATION
 *
 * Tujuan:
 * - Mengatur system bars (status bar dan navigation bar) secara global
 * - Menyediakan konsistensi visual across seluruh aplikasi
 * - Menangani dark/light theme adaptation
 *
 * Features:
 * - Custom status bar color
 * - Auto dark/light icon adaptation
 * - Transparent navigation bar untuk modern look
 * - SideEffect untuk sekali setup
 *
 * @author Your Name
 * @since Version 1.0
 * @param statusBarColor Warna untuk status bar
 * @param darkIcons Whether menggunakan dark icons (true) atau light icons (false)
 */

package com.example.melon_monitoring_and_automation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetSystemBars(
    statusBarColor: Color = Color(0xFF2E7D32), // Default dark green
    darkIcons: Boolean = false
) {
    // SYSTEM UI CONTROLLER - Untuk system bars manipulation
    val systemUiController = rememberSystemUiController()

    // THEME ADAPTATION - Determine icon color berdasarkan theme
    val useDarkIcons = darkIcons || !isSystemInDarkTheme()

    // SIDE EFFECT - Setup system bars sekali saat composition
    SideEffect {
        // SET STATUS BAR COLOR
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons
        )

        // SET NAVIGATION BAR COLOR - Transparent untuk modern look
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }
}