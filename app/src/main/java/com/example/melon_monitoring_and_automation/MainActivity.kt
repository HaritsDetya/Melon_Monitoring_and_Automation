package com.example.melon_monitoring_and_automation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.example.melon_monitoring_and_automation.ui.theme.HydroponicAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 🔹 FIX: Gunakan property instance, bukan companion object untuk deep link state
    private var pendingDeepLink: Uri? = null
    private var deepLinkProcessed = false
    private var deepLinkListener: () -> Unit = {}

    // 🔹 FIX: Tambahkan flag untuk track jika app baru saja di-start
    private var isFirstCreate = true

    fun setDeepLinkListener(listener: () -> Unit) {
        deepLinkListener = listener
    }

    fun clearDeepLinkListener() { // 🔹 Method khusus untuk clear
        deepLinkListener = {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("🔹 [MAIN ACTIVITY] onCreate - First create: $isFirstCreate")

        // Handle deep link untuk reset password
        handleIntent(intent)

        setStatusBarColor()

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

    private fun setStatusBarColor() {
        // Hanya set warna dasar, biarkan Accompanist yang handle dynamic changes
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_dark_green)

        // Untuk edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }

    override fun onResume() {
        super.onResume()
        println("🔹 [MAIN ACTIVITY] onResume")

        // 🔹 FIX: Jika ini pertama kali create, beri tanda bahwa app baru di-start
        if (isFirstCreate) {
            isFirstCreate = false
            println("🔹 [MAIN ACTIVITY] App freshly started, auth should be checked")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("🔹 [MAIN ACTIVITY] New intent received: ${intent.action}")
        handleIntent(intent)
        deepLinkListener()
    }

    // 🔹 PERBAIKAN: Method untuk consume deep link (ambil sekali pakai)
    fun consumePendingDeepLink(): Uri? {
        return if (!deepLinkProcessed && pendingDeepLink != null) {
            val deepLink = pendingDeepLink
            pendingDeepLink = null
            deepLinkProcessed = true

            // 🔹 DEBUG: Log detail deep link
            println("🔹 [MAIN ACTIVITY] ✅ CONSUMED deep link: $deepLink")
            println("🔹 [MAIN ACTIVITY] Fragment: ${deepLink?.fragment?.take(100)}...")

            deepLink
        } else {
            println("🔹 [MAIN ACTIVITY] ❌ No deep link to consume")
            null
        }
    }

    private fun handleIntent(intent: Intent) {
        println("🔹 [MAIN ACTIVITY] Handling intent: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val deepLinkUri = intent.data
                if (deepLinkUri != null) {
                    println("🔹 [MAIN ACTIVITY] 🎯 DEEP LINK RECEIVED: $deepLinkUri")

                    // 🔹 PERBAIKAN: Reset state untuk deep link baru
                    pendingDeepLink = deepLinkUri
                    deepLinkProcessed = false // 🔹 RESET ke false untuk deep link baru

                    println("🔹 [MAIN ACTIVITY] Deep link state RESET - processed: $deepLinkProcessed")
                }
            }
        }
    }

    // 🔹 PERBAIKAN: Jangan otomatis mark processed di handleDeepLink
    private fun handleDeepLink(intent: Intent) {
        val data = intent.data
        if (data != null) {
            println("🔹 [DEEP LINK] === DEEP LINK PROCESSING ===")
            println("🔹 [DEEP LINK] Full URI: $data")

            if (data.toString().contains("supabase.com")) {
                println("🔹 [DEEP LINK] Supabase deep link detected")
                pendingDeepLink = data
                deepLinkProcessed = false // 🔹 JANGAN langsung mark processed!
                println("🔹 [DEEP LINK] Deep link saved for consumption")
            }
        }
    }

    fun getPendingDeepLinkInstance(): Uri? {
        val hasDeepLink = !deepLinkProcessed && pendingDeepLink != null
        println("🔹 [MAIN ACTIVITY] 📋 Get pending deep link: $hasDeepLink")
        return if (!deepLinkProcessed) pendingDeepLink else null
    }

    fun markDeepLinkProcessed() {
        deepLinkProcessed = true
        pendingDeepLink = null
        println("🔹 [MAIN ACTIVITY] 🏁 Deep link MARKED AS PROCESSED")
    }
}
