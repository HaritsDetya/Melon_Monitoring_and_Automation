/**
 * MAIN ACTIVITY
 *
 * Tujuan:
 * - Entry point utama aplikasi Android
 * - Menangani deep links untuk reset password flow
 * - Mengatur status bar color dan window configuration
 * - Mengelola lifecycle aplikasi dan intent handling
 *
 * Features:
 * - Deep link processing untuk Supabase auth
 * - Status bar customization
 * - Hilt dependency injection setup
 * - Compose UI integration
 *
 * Deep Link Handling:
 * - Menangani intent dengan action ACTION_VIEW
 * - Memproses URI dengan scheme "app" dan host "supabase.com"
 * - Mengelola state deep link untuk reset password
 *
 * @author Your Name
 * @since Version 1.0
 */

package com.example.melon_monitoring_and_automation

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.melon_monitoring_and_automation.ui.theme.HydroponicAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // DEEP LINK STATE MANAGEMENT
    private var pendingDeepLink: Uri? = null
    private var deepLinkProcessed = false
    private var deepLinkListener: () -> Unit = {}

    // APP LIFECYCLE TRACKING
    private var isFirstCreate = true

    /**
     * SET DEEP LINK LISTENER
     * Mengatur listener untuk menangani deep link events
     *
     * @param listener Callback function yang akan dipanggil ketika deep link diproses
     */
    fun setDeepLinkListener(listener: () -> Unit) {
        deepLinkListener = listener
    }

    /**
     * CLEAR DEEP LINK LISTENER
     * Membersihkan deep link listener untuk prevent memory leaks
     */
    fun clearDeepLinkListener() { // 🔹 Method khusus untuk clear
        deepLinkListener = {}
    }

    /**
     * ON CREATE
     * Lifecycle method yang dipanggil ketika activity dibuat
     * Setup initial configuration dan UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("🔹 [MAIN ACTIVITY] onCreate - First create: $isFirstCreate")

        // Handle deep link untuk reset password
        handleIntent(intent)

        setStatusBarColor()

        // SETUP COMPOSE UI
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

    /**
     * SET STATUS BAR COLOR
     * Mengatur warna dan behavior status bar
     * Menggunakan warna hijau gelap untuk konsistensi branding
     */
    private fun setStatusBarColor() {
        // Set dasar status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_dark_green)

        // Edge-to-edge display untuk Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
    }

    /**
     * ON RESUME
     * Lifecycle method yang dipanggil ketika activity menjadi visible
     * Track app startup state
     */
    override fun onResume() {
        super.onResume()
        println("🔹 [MAIN ACTIVITY] onResume")

        // Track first app launch untuk auth checking
        if (isFirstCreate) {
            isFirstCreate = false
            println("🔹 [MAIN ACTIVITY] App freshly started, auth should be checked")
        }
    }

    /**
     * ON NEW INTENT
     * Lifecycle method yang dipanggil ketika activity menerima intent baru
     * Handle deep links dari email reset password
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("🔹 [MAIN ACTIVITY] New intent received: ${intent.action}")
        handleIntent(intent)
        deepLinkListener() // Notify listeners tentang deep link baru
    }

    /**
     * CONSUME PENDING DEEP LINK
     * Mengambil pending deep link untuk diproses sekali pakai
     *
     * @return Uri deep link atau null jika tidak ada
     */
    fun consumePendingDeepLink(): Uri? {
        return if (!deepLinkProcessed && pendingDeepLink != null) {
            val deepLink = pendingDeepLink
            pendingDeepLink = null
            deepLinkProcessed = true

            // DEBUG: Log detail deep link
            println("🔹 [MAIN ACTIVITY] ✅ CONSUMED deep link: $deepLink")
            println("🔹 [MAIN ACTIVITY] Fragment: ${deepLink?.fragment?.take(100)}...")

            deepLink
        } else {
            println("🔹 [MAIN ACTIVITY] ❌ No deep link to consume")
            null
        }
    }

    /**
     * HANDLE INTENT
     * Memproses incoming intent untuk deep link detection
     *
     * @param intent Intent yang diterima dari system
     */
    private fun handleIntent(intent: Intent) {
        println("🔹 [MAIN ACTIVITY] Handling intent: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_VIEW -> {
                val deepLinkUri = intent.data
                if (deepLinkUri != null) {
                    println("🔹 [MAIN ACTIVITY] 🎯 DEEP LINK RECEIVED: $deepLinkUri")

                    // Reset state untuk deep link baru
                    pendingDeepLink = deepLinkUri
                    deepLinkProcessed = false // Reset ke false untuk deep link baru

                    println("🔹 [MAIN ACTIVITY] Deep link state RESET - processed: $deepLinkProcessed")
                }
            }
        }
    }

    /**
     * GET PENDING DEEP LINK INSTANCE
     * Mendapatkan pending deep link tanpa mengonsumsi/mark processed
     *
     * @return Uri deep link atau null jika tidak ada
     */
    fun getPendingDeepLinkInstance(): Uri? {
        val hasDeepLink = !deepLinkProcessed && pendingDeepLink != null
        println("🔹 [MAIN ACTIVITY] 📋 Get pending deep link: $hasDeepLink")
        return if (!deepLinkProcessed) pendingDeepLink else null
    }

    /**
     * MARK DEEP LINK PROCESSED
     * Manual mark deep link sebagai sudah diproses
     * Digunakan untuk clear state setelah processing selesai
     */
    fun markDeepLinkProcessed() {
        deepLinkProcessed = true
        pendingDeepLink = null
        println("🔹 [MAIN ACTIVITY] 🏁 Deep link MARKED AS PROCESSED")
    }
}
