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

    companion object {
        private var _pendingDeepLink: Uri? = null
        const val DEEP_LINK_PROCESSED = "deep_link_processed"

        // Function untuk mengakses pendingDeepLink dari luar
        fun getPendingDeepLink(): Uri? = _pendingDeepLink

        // Function untuk set pendingDeepLink dari luar
        fun setPendingDeepLink(uri: Uri?) {
            _pendingDeepLink = uri
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle deep link untuk reset password
        handleDeepLink(intent)

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
        // Handle deep link ketika app sudah running
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            println("🔹 [DEEP LINK] === DEEP LINK PROCESSING ===")
            println("🔹 [DEEP LINK] Full URI: $data")

            if (data.toString().contains("supabase.com")) {
                println("🔹 [DEEP LINK] Supabase deep link detected")

                val fragment = data.fragment
                if (fragment?.contains("access_token") == true) {
                    println("🔹 [DEEP LINK] Access token found in deep link")

                    // Simpan deep link untuk diproses oleh MainApp
                    setPendingDeepLink(data)

                    // Set flag bahwa ada deep link yang perlu diproses
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean(DEEP_LINK_PROCESSED, false).apply()

                    println("🔹 [DEEP LINK] Deep link saved for processing")

                    // 🔹 FIX: Coba handle sebagai OAuth callback
                    handleAsOAuthCallback(data)

                } else if (fragment?.contains("error") == true) {
                    println("🔹 [DEEP LINK] Error in deep link: $fragment")
                    handleDeepLinkError(fragment)
                }
            }
        }
    }

    // 🔹 NEW: Handle sebagai OAuth callback
    private fun handleAsOAuthCallback(uri: Uri) {
        println("🔹 [DEEP LINK] Handling as OAuth callback")

        // Simpan URI untuk diproses oleh AuthViewModel
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("oauth_callback_uri", uri.toString()).apply()

        triggerResetPasswordNavigation()
    }

    private fun handleDeepLinkError(fragment: String) {
        val decodedFragment = URLDecoder.decode(fragment, "UTF-8")
        println("🔹 [DEEP LINK] Error details: $decodedFragment")

        // Simpan error untuk ditampilkan di UI
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit() { putString("deep_link_error", decodedFragment) }
    }

    // 🔹 NEW: Method untuk trigger navigation ke reset password
    private fun triggerResetPasswordNavigation() {
        println("🔹 [DEEP LINK] Triggering reset password navigation")

        // Simpan flag untuk MainApp bahwa perlu navigate ke reset password
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("should_navigate_to_reset", true).apply()

        // Atau restart activity dengan intent khusus
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("NAVIGATE_TO", "reset_password")
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    // Function untuk menandai bahwa deep link sudah diproses
    fun markDeepLinkProcessed() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit() { putBoolean(DEEP_LINK_PROCESSED, true) }
        setPendingDeepLink(null)
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

    // Function untuk mendapatkan pending deep link (instance method)
    fun getPendingDeepLinkInstance(): Uri? {
        return getPendingDeepLink()
    }

    // Function untuk cek apakah perlu navigate ke reset password
    fun shouldNavigateToResetPassword(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("should_navigate_to_reset", false).also {
            if (it) {
                // Clear flag setelah diambil
                sharedPref.edit().putBoolean("should_navigate_to_reset", false).apply()
            }
        }
    }
}
