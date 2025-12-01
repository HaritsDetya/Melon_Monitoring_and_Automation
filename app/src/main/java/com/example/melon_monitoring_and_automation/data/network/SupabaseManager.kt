/**
 * SUPABASE MANAGER - CLIENT CONFIGURATION
 *
 * Tujuan:
 * - Menyediakan centralized Supabase client configuration
 * - Mengelola Supabase client instance sebagai singleton
 * - Mengkonfigurasi semua Supabase modules (PostgREST, Realtime, Auth, Functions)
 *
 * Configuration:
 * - URL dan API Key untuk Supabase project
 * - Serializer configuration (KotlinX Serialization)
 * - Module installations untuk extended functionality
 *
 * Security Note:
 * - Anon key digunakan untuk client-side operations
 * - Untuk production, consider menggunakan Row Level Security (RLS)
 *
 * @author Your Name
 * @since Version 1.0
 */

package com.example.melon_monitoring_and_automation.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer

/**
 * SUPABASE MANAGER OBJECT
 * Singleton object untuk Supabase client management
 */
object SupabaseManager {
    // SUPABASE CONFIGURATION - Project credentials
    private const val SUPABASE_URL = "https://aniututpufnxevcdxiio.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV0dXRwdWZueGV2Y2R4aWlvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk3MTYzOTMsImV4cCI6MjA3NTI5MjM5M30.zszt_8P1WNugBVr3FyhqTYmmF3BAOQ2RwFxiCt4bm50" // Legacy anon key

    /**
     * SUPABASE CLIENT INSTANCE
     * Configured client dengan semua necessary modules
     */
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // MODULE INSTALLATIONS - Enable Supabase features
        install(Postgrest)    // Database REST API
        install(Realtime)     // Real-time subscriptions
        install(Auth)         // Authentication
        install(Functions)    // Edge Functions

        // SERIALIZER CONFIGURATION - JSON serialization
        defaultSerializer = KotlinXSerializer()
    }
}
