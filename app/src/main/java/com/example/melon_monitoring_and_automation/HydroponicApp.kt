package com.example.melon_monitoring_and_automation

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest

@HiltAndroidApp
class HydroponicApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
