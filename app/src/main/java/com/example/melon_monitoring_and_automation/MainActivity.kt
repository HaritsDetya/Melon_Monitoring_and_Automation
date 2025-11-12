package com.example.melon_monitoring_and_automation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.melon_monitoring_and_automation.ui.theme.HydroponicAppTheme
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import androidx.core.content.edit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 🔹 FIX: Gunakan property instance, bukan companion object untuk deep link state
    private var pendingDeepLink: Uri? = null
    private var deepLinkProcessed = false

    companion object {
        const val DEEP_LINK_PROCESSED = "deep_link_processed"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep link untuk reset password
        handleIntent(intent)

        setContent {
            HydroponicAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("🔹 [MAIN ACTIVITY] New intent received: ${intent.action}")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        println("🔹 [MAIN ACTIVITY] Handling intent: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val deepLinkUri = intent.data
                if (deepLinkUri != null) {
                    println("🔹 [MAIN ACTIVITY] Deep link received: $deepLinkUri")

                    // 🔹 PERBAIKAN: Reset state untuk deep link baru
                    pendingDeepLink = deepLinkUri
                    deepLinkProcessed = false

                    // Log detail deep link
                    println("🔹 [MAIN ACTIVITY] Scheme: ${deepLinkUri.scheme}")
                    println("🔹 [MAIN ACTIVITY] Host: ${deepLinkUri.host}")
                    println("🔹 [MAIN ACTIVITY] Path: ${deepLinkUri.path}")
                    println("🔹 [MAIN ACTIVITY] Fragment: ${deepLinkUri.fragment?.take(50)}...")

                    // Handle deep link
                    handleDeepLink(intent)
                }
            }
        }
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            println("🔹 [DEEP LINK] === DEEP LINK PROCESSING ===")
            println("🔹 [DEEP LINK] Full URI: $data")

            if (data.toString().contains("supabase.com")) {
                println("🔹 [DEEP LINK] Supabase deep link detected")

                val fragment = data.fragment
                // Simpan deep link, biarkan MainApp yang handle navigation
                pendingDeepLink = data
                deepLinkProcessed = false

                println("🔹 [DEEP LINK] Deep link saved for MainApp processing")
            }
        }
    }

    // 🔹 NEW: Function untuk cek apakah perlu show reset error
    fun shouldShowResetError(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("should_show_reset_error", false).also {
            if (it) {
                sharedPref.edit().putBoolean("should_show_reset_error", false).apply()
            }
        }
    }

    // Function untuk diakses dari Composable
    fun getPendingDeepLinkInstance(): Uri? {
        return if (!deepLinkProcessed) pendingDeepLink else null
    }

    fun markDeepLinkProcessed() {
        deepLinkProcessed = true
        pendingDeepLink = null
        println("🔹 [MAIN ACTIVITY] Deep link marked as processed")
    }

    // Function untuk mendapatkan error deep link
    fun getDeepLinkError(): String? {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("deep_link_error", null).also {
            if (it != null) {
                // Clear error setelah diambil
                sharedPref.edit() { remove("deep_link_error") }
            }
        }
    }
}
