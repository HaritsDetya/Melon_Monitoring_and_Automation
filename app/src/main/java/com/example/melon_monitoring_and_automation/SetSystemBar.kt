package com.example.melon_monitoring_and_automation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun SetSystemBars(
    statusBarColor: Color = Color(0xFF2E7D32),
    darkIcons: Boolean = false
) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = darkIcons || !isSystemInDarkTheme()

    SideEffect {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = useDarkIcons
        )

        // Optional: Set navigation bar color juga
        systemUiController.setNavigationBarColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }
}
