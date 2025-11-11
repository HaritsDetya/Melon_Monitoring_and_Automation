package com.example.melon_monitoring_and_automation.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.melon_monitoring_and_automation.data.network.ApiService
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.network.SupabaseManager
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.User
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock.System
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class HydroponicRepository {

    private val apiService = ApiService(SupabaseManager.client.postgrest)
    private val supabaseClient = SupabaseManager.client

    // 🔹 Register user - COMPREHENSIVE FIX
    suspend fun signUpWithEmail(
        username: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            println("🔹 [REPO] Starting registration for: $email")

            // Step 1: Sign up user
            val authResult = SupabaseManager.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            println("🔹 [REPO] Auth result: $authResult")

            // Step 2: Wait and get current user
            delay(1000) // Important: Wait for auth to complete

            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            println("🔹 [REPO] Current user after signup: $currentUser")

            if (currentUser == null) {
                throw Exception("User authentication failed after signup")
            }

            val userIdString = currentUser.id
            println("🔹 [REPO] User ID: $userIdString")

            // Step 3: Create and save user profile
            val newUser = User(
                id = userIdString,
                username = username,
                email = email,
                phoneNumber = phoneNumber,
                createdAt = System.now().toString()
            )

            println("🔹 [REPO] Saving user to database: $newUser")

            // Insert user profile
            SupabaseManager.client.postgrest["users"].insert(newUser)

            // Step 4: Verify the user was saved with phone number
            val savedUser = try {
                SupabaseManager.client.postgrest["users"]
                    .select {
                        filter { eq("id", userIdString) }
                    }
                    .decodeSingle<User>()
            } catch (e: Exception) {
                println("🔹 [REPO] Error verifying user: ${e.message}")
                throw Exception("Failed to verify user creation in database")
            }

            println("🔹 [REPO] User saved successfully: $savedUser")
            println("🔹 [REPO] Phone number in saved user: ${savedUser.phoneNumber}")

            // Step 5: Verify authentication session
            val session = SupabaseManager.client.auth.currentSessionOrNull()
            println("🔹 [REPO] Current session: $session")

            if (session == null) {
                throw Exception("No active session after registration")
            }

            Result.success(savedUser)

        } catch (e: Exception) {
            println("🔹 [REPO] Registration failed: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // 📁 data/repository/HydroponicRepository.kt - PERBAIKAN LOGIN
    suspend fun signInWithEmail(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Starting login for: $email")

                // 🔹 HAPUS withTimeout - biarkan menggunakan default timeout Supabase
                val authResult = SupabaseManager.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                println("🔹 [REPO] Auth signin completed")

                // Tunggu sebentar untuk memastikan auth selesai
                delay(500)

                val currentSession = SupabaseManager.client.auth.currentSessionOrNull()
                println("🔹 [REPO] Current session: ${currentSession != null}")

                if (currentSession == null) {
                    throw Exception("Session tidak ditemukan setelah login")
                }

                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                println("🔹 [REPO] Current user: ${currentUser != null}")

                if (currentUser == null) {
                    throw Exception("User tidak ditemukan setelah login")
                }

                val userIdString = currentUser.id
                println("🔹 [REPO] User ID: $userIdString")

                // Ambil profil user
                val userProfile = SupabaseManager.client.postgrest["users"]
                    .select {
                        filter { eq("id", userIdString) }
                    }
                    .decodeSingle<User>()

                println("🔹 [REPO] User profile found: ${userProfile.email}")
                Result.success(userProfile)

            } catch (e: Exception) {
                println("🔹 [REPO] Login failed: ${e.message}")
                e.printStackTrace()
                Result.failure(e)
            }
        }

    suspend fun deleteUserAccount(): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] === EDGE FUNCTION CALL ===")

                val currentUser = SupabaseManager.client.auth.currentUserOrNull()
                if (currentUser == null) {
                    return@withContext NetworkResult.Error("No authenticated user found", 401)
                }

                val userId = currentUser.id
                val adminSecret = "melon_app_delete_2024_secret"

                println("🔹 [REPO] Calling function for user: $userId")

                // ✅ PERBAIKAN: Kirim user_id dalam body dengan format JSON yang benar
                val response = SupabaseManager.client.functions
                    .invoke("delete-auth-account") {
                        header("x-admin-secret", adminSecret)
                        header("Content-Type", "application/json")
                        setBody("""{"user_id": "$userId"}""") // ← KIRIM BODY DENGAN FORMAT JSON
                    }

                val statusCode = response.status.value
                println("🔹 [REPO] Response status: $statusCode")

                // ✅ Coba baca response body untuk debug
                try {
                    val responseBody = response.body<String>()
                    println("🔹 [REPO] Response body: $responseBody")
                } catch (e: Exception) {
                    println("🔹 [REPO] Cannot read response body: ${e.message}")
                }

                when (statusCode) {
                    200 -> {
                        println("🔹 [REPO] ✅ SUCCESS - Account deleted")
                        SupabaseManager.client.auth.signOut()
                        clearLocalCache()
                        NetworkResult.Success(true)
                    }
                    401 -> {
                        println("🔹 [REPO] ❌ ERROR 401 - Invalid secret")
                        NetworkResult.Error("Security error: Invalid credentials", 401)
                    }
                    404 -> {
                        println("🔹 [REPO] ❌ ERROR 404 - User not found")
                        NetworkResult.Error("User account not found", 404)
                    }
                    500 -> {
                        println("🔹 [REPO] ❌ ERROR 500 - Server error")
                        NetworkResult.Error("Server error, please try again later", 500)
                    }
                    else -> {
                        println("🔹 [REPO] ❌ ERROR $statusCode - Unknown error")
                        NetworkResult.Error("Unexpected error: $statusCode")
                    }
                }

            } catch (e: Exception) {
                println("🔹 [REPO] ❌ EXCEPTION: ${e.message}")
                e.printStackTrace()
                NetworkResult.Error("Network error: ${e.message}")
            }
        }
    }

    // 🔹 Clear cache - FIXED
    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        try {
            println("🔹 [REPO] Clearing local cache")

            // Clear Supabase auth session
            SupabaseManager.client.auth.signOut()

            // Additional cleanup jika perlu
            try {
                // Clear any cached data atau local storage
                SupabaseManager.client.auth.clearSession()
            } catch (e: Exception) {
                println("🔹 [REPO] Additional cleanup error: ${e.message}")
            }

            println("🔹 [REPO] Local cache cleared successfully")
        } catch (e: Exception) {
            println("🔹 [REPO] Error clearing cache: ${e.message}")
            throw e
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            println("🔹 [REPO] Getting current user...")

            val currentAuthUser = SupabaseManager.client.auth.currentUserOrNull()
            println("🔹 [REPO] Current auth user: $currentAuthUser")

            if (currentAuthUser == null) {
                println("🔹 [REPO] No authenticated user found")
                return null
            }

            val userId = currentAuthUser.id
            println("🔹 [REPO] Fetching user profile for ID: $userId")

            // Ambil data user dari tabel users
            val userProfile = SupabaseManager.client.postgrest["users"]
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<User>()

            println("🔹 [REPO] User profile found: $userProfile")
            userProfile

        } catch (e: Exception) {
            println("🔹 [REPO] Error getting current user: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    // Di HydroponicRepository - PERBAIKAN COMPLETE
    suspend fun updateAutomationSettings(settings: AutomationSettings): NetworkResult<AutomationSettings> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Updating automation settings for greenhouse: ${settings.greenhouseId}")

                // Cek apakah settings sudah ada
                val existingSettings = getAutomationSettings(settings.greenhouseId)

                return@withContext if (existingSettings is NetworkResult.Success && existingSettings.data != null) {
                    // Update existing settings - gunakan approach yang berbeda
                    try {
                        println("🔹 [REPO] Updating existing settings...")

                        // Gunakan update dan kemudian fetch data terbaru
                        val updateResult = SupabaseManager.client.postgrest["automation_settings"]
                            .update({
                                set("max_temperature", settings.maxTemperature)
                                set("min_temperature", settings.minTemperature)
                                set("nutrient_droplets", settings.nutrientDroplets)
                                set("updated_at", System.now().toString())
                            }) {
                                filter { eq("greenhouse_id", settings.greenhouseId) }
                            }

                        println("🔹 [REPO] Update executed, fetching updated data...")

                        // Fetch data yang sudah di-update
                        val updatedSettings: AutomationSettings = SupabaseManager.client.postgrest["automation_settings"]
                            .select {
                                filter { eq("greenhouse_id", settings.greenhouseId) }
                            }
                            .decodeSingle()

                        println("🔹 [REPO] Automation settings updated successfully: $updatedSettings")
                        NetworkResult.Success(updatedSettings)

                    } catch (updateError: Exception) {
                        println("🔹 [REPO] Update failed, trying insert: ${updateError.message}")
                        // Jika update gagal, coba insert
                        tryInsertSettings(settings)
                    }
                } else {
                    // Settings tidak ada, buat baru
                    println("🔹 [REPO] Settings not found, creating new...")
                    tryInsertSettings(settings)
                }
            } catch (e: Exception) {
                println("🔹 [REPO] Error in updateAutomationSettings: ${e.message}")
                NetworkResult.Error(e.localizedMessage ?: "Failed to update automation settings")
            }
        }
    }

    // Helper method untuk insert settings
    private suspend fun tryInsertSettings(settings: AutomationSettings): NetworkResult<AutomationSettings> {
        return try {
            println("🔹 [REPO] Inserting new automation settings...")

            val insertResult: AutomationSettings = SupabaseManager.client.postgrest["automation_settings"]
                .insert(settings) {
                    select()
                }
                .decodeSingle()

            println("🔹 [REPO] Automation settings inserted successfully")
            NetworkResult.Success(insertResult)
        } catch (insertError: Exception) {
            println("🔹 [REPO] Error inserting automation settings: ${insertError.message}")
            NetworkResult.Error("Failed to create automation settings: ${insertError.message}")
        }
    }

    // Di HydroponicRepository - PERBAIKAN GET METHOD
    suspend fun getAutomationSettings(greenhouseId: String): NetworkResult<AutomationSettings?> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Getting automation settings for greenhouse: $greenhouseId")

                val result: List<AutomationSettings> = SupabaseManager.client.postgrest["automation_settings"]
                    .select {
                        filter { eq("greenhouse_id", greenhouseId) }
                    }
                    .decodeList()

                println("🔹 [REPO] Automation settings found: ${result.size} records")

                if (result.isNotEmpty()) {
                    NetworkResult.Success(result.first())
                } else {
                    NetworkResult.Success(null)
                }
            } catch (e: Exception) {
                println("🔹 [REPO] Error getting automation settings: ${e.message}")
                NetworkResult.Error(e.localizedMessage ?: "Failed to get automation settings")
            }
        }
    }

    // Method untuk create automation settings - PERBAIKAN
    suspend fun createAutomationSettings(greenhouseId: String): NetworkResult<AutomationSettings> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Creating automation settings for greenhouse: $greenhouseId")

                val defaultSettings = AutomationSettings(
                    id = UUID.randomUUID().toString(),
                    greenhouseId = greenhouseId,
                    maxTemperature = 38.0,
                    minTemperature = 25.0,
                    nutrientDroplets = 10,
                    updatedAt = System.now().toString()
                )

                val result: AutomationSettings = SupabaseManager.client.postgrest["automation_settings"]
                    .insert(defaultSettings) {
                        select()
                    }
                    .decodeSingle()

                println("🔹 [REPO] Automation settings created successfully")
                NetworkResult.Success(result)
            } catch (e: Exception) {
                println("🔹 [REPO] Error creating automation settings: ${e.message}")
                NetworkResult.Error(e.localizedMessage ?: "Failed to create automation settings: ${e.message}")
            }
        }
    }

    // 🔹 Ambil semua greenhouse milik user
    suspend fun getUserGreenhouses(userId: String): NetworkResult<List<Greenhouse>> {
        return apiService.getGreenhousesByUser(userId)
    }

    // 🔹 Ambil data sensor terbaru
    suspend fun getLatestSensorData(greenhouseId: String): NetworkResult<SensorReadings?> {
        return apiService.getLatestSensorReading(greenhouseId)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getSensorHistory(
        greenhouseId: String,
        sensorType: SensorType,
        hours: Int
    ): NetworkResult<List<SensorHistory>> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Fetching sensor history: greenhouse=$greenhouseId, type=$sensorType, hours=$hours")

                // ✅ PERBAIKAN: Gunakan limit dan order yang tepat
                val result: List<SensorHistory> = SupabaseManager.client.postgrest["sensor_history"]
                    .select {
                        filter {
                            eq("greenhouse_id", greenhouseId)
                            eq("sensor_type", sensorType.name)
                            // Hapus filter waktu sementara untuk testing
                            // gt("recorded_at", cutoffTime)
                        }
                        order("recorded_at", Order.ASCENDING) // ✅ Urutkan dari terlama ke terbaru
                        limit(1000) // ✅ Batasi jumlah data
                    }
                    .decodeList()

                println("🔹 [REPO] Sensor history fetched: ${result.size} records")

                // ✅ Debug: Print sample data
                if (result.isNotEmpty()) {
                    println("🔹 [REPO] Sample data - First: ${result.first().recordedAt} = ${result.first().value}")
                    println("🔹 [REPO] Sample data - Last: ${result.last().recordedAt} = ${result.last().value}")
                }

                NetworkResult.Success(result)
            } catch (e: Exception) {
                println("🔹 [REPO] Error fetching sensor history: ${e.message}")
                e.printStackTrace()
                NetworkResult.Error(e.localizedMessage ?: "Failed to fetch sensor history: ${e.message}")
            }
        }
    }

    // 🔹 Ambil status kontrol perangkat (fan, pump, dll)
    suspend fun getControlDevice(greenhouseId: String): NetworkResult<ControlDevices?> {
        return apiService.getControlDevice(greenhouseId)
    }

    // 🔹 Create control device if not exists
    suspend fun createControlDeviceIfNotExists(greenhouseId: String): NetworkResult<ControlDevices> {
        return apiService.createControlDeviceIfNotExists(greenhouseId)
    }

    // --- 🔹 Realtime Control Device Updates - FIXED (coroutine issue) ---
    fun getRealtimeDeviceUpdates(greenhouseId: String): Flow<ControlDevices?> = callbackFlow {
        val channel = supabaseClient.realtime.channel("device_updates_$greenhouseId")

        try {
            channel.subscribe()

            val flow = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "control_devices"
                filter = "greenhouse_id=eq.$greenhouseId"
            }

            val json = Json { ignoreUnknownKeys = true }

            val job = launch {
                flow.collect { change ->
                    try {
                        val recordJson = change.record.toString()
                        val device = json.decodeFromString<ControlDevices>(recordJson)
                        trySend(device)
                    } catch (e: Exception) {
                        trySend(null)
                    }
                }
            }

            awaitClose {
                job.cancel()
            }
        } catch (e: Exception) {
            close(e)
        }
    }.flowOn(Dispatchers.IO)

    // 🔹 Update kontrol perangkat - ✅ FIXED
    suspend fun updateDeviceControl(device: ControlDevices): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Updating device control: ${device.id}")

                SupabaseManager.client.postgrest["control_devices"]
                    .update({
                        set("fan", device.fan)
                        set("pump", device.pump)
                        set("auto_mode", device.autoMode)
                        set("updated_at", System.now().toString())
                    }) {
                        filter { eq("id", device.id) }
                    }

                println("🔹 [REPO] Device control updated successfully")
                NetworkResult.Success(true)
            } catch (e: Exception) {
                println("🔹 [REPO] Error updating device control: ${e.message}")
                NetworkResult.Error(e.localizedMessage ?: "Failed to update device control: ${e.message}")
            }
        }
    }

    // Tambahkan method baru di repository
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getSensorHistoryByDateRange(
        greenhouseId: String,
        sensorType: SensorType,
        startDate: Long,
        endDate: Long
    ): NetworkResult<List<SensorHistory>> {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ PERBAIKAN: Convert timestamps dengan format yang benar
                val startIso = Instant.ofEpochMilli(startDate)
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_INSTANT)

                val endIso = Instant.ofEpochMilli(endDate)
                    .atZone(ZoneId.of("UTC"))
                    .format(DateTimeFormatter.ISO_INSTANT)

                println("🔹 [REPO] Fetching sensor history by date range: $startIso to $endIso")

                val result: List<SensorHistory> = SupabaseManager.client.postgrest["sensor_history"]
                    .select {
                        filter {
                            eq("greenhouse_id", greenhouseId)
                            eq("sensor_type", sensorType.name)
                            gte("recorded_at", startIso)
                            lte("recorded_at", endIso)
                        }
                        order("recorded_at", Order.ASCENDING)
                    }
                    .decodeList()

                println("🔹 [REPO] Date range history fetched: ${result.size} records")
                NetworkResult.Success(result)
            } catch (e: Exception) {
                println("🔹 [REPO] Error fetching date range history: ${e.message}")
                NetworkResult.Error(e.localizedMessage ?: "Failed to fetch date range history")
            }
        }
    }
}