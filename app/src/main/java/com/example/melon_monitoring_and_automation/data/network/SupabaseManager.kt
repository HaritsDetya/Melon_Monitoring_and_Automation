package com.example.melon_monitoring_and_automation.data.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer

object SupabaseManager {
    private const val SUPABASE_URL = "https://aniututpufnxevcdxiio.supabase.co"
    private const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFuaXV0dXRwdWZueGV2Y2R4aWlvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk3MTYzOTMsImV4cCI6MjA3NTI5MjM5M30.zszt_8P1WNugBVr3FyhqTYmmF3BAOQ2RwFxiCt4bm50"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        install(Auth)
        defaultSerializer = KotlinXSerializer()
    }
}
