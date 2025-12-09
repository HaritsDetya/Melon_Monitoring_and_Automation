/**
 * HYDROPONIC REPOSITORY - DATA ACCESS LAYER
 *
 * Tujuan:
 * - Menyediakan abstraction layer untuk semua data operations
 * - Mengelola komunikasi antara domain layer dan network layer
 * - Menangani business logic untuk data manipulation
 * - Mengelola authentication, greenhouse, sensor, dan device operations
 *
 * Architecture:
 * - Repository Pattern dengan clean separation of concerns
 * - Coroutines untuk async operations
 * - NetworkResult untuk standardized error handling
 * - Supabase integration untuk backend operations
 *
 * Features:
 * - User Authentication (Sign Up, Sign In, Delete Account)
 * - Greenhouse Management (CRUD operations)
 * - Sensor Data Operations (Real-time dan Historical)
 * - Device Control (Automation, Control Devices)
 * - Real-time Updates dengan Supabase Realtime
 *
 * @author Your Name
 * @since Version 1.0
 * @param context Android Context untuk system operations
 */

package com.example.melon_monitoring_and_automation.data.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.melon_monitoring_and_automation.data.network.ApiService
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.network.SupabaseManager
import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.CreateGreenhouseRequest
import com.example.melon_monitoring_and_automation.domain.model.CreateGreenhouseResponse
import com.example.melon_monitoring_and_automation.domain.model.DeviceCommand
import com.example.melon_monitoring_and_automation.domain.model.DeviceStatusUpdate
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import com.example.melon_monitoring_and_automation.domain.model.SendDeviceCommand
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

class HydroponicRepository(
    private val context: Context
) {

    // DEPENDENCY INJECTION - API Service dan Supabase Client
    private val apiService = ApiService(SupabaseManager.client.postgrest)
    private val supabaseClient = SupabaseManager.client

    /**
     * SIGN UP WITH EMAIL
     * Mendaftarkan user baru dengan email dan password
     * Comprehensive process dengan multiple validation steps
     *
     * @param username Username untuk user baru
     * @param email Email address untuk registrasi
     * @param password Password untuk akun baru
     * @param phoneNumber Nomor telepon user
     * @return Result<User> dengan user data atau exception
     */
    suspend fun signUpWithEmail(
        username: String,
        email: String,
        password: String,
        phoneNumber: String
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            println("🔹 [REPO] Starting registration for: $email")

            // STEP 1: Sign up user dengan Supabase Auth
            val authResult = SupabaseManager.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            println("🔹 [REPO] Auth result: $authResult")

            // STEP 2: Wait untuk memastikan auth process completed
            delay(1000)

            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            println("🔹 [REPO] Current user after signup: $currentUser")

            if (currentUser == null) {
                throw Exception("User authentication failed after signup")
            }

            val userIdString = currentUser.id
            println("🔹 [REPO] User ID: $userIdString")

            // STEP 3: Create user profile object
            val newUser = User(
                id = userIdString,
                username = username,
                email = email,
                phoneNumber = phoneNumber,
                createdAt = System.now().toString()
            )

            println("🔹 [REPO] Saving user to database: $newUser")

            // STEP 4: Insert user profile ke database
            SupabaseManager.client.postgrest["users"].insert(newUser)

            // STEP 5: Verify user was saved dengan phone number
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

            // STEP 6: Verify authentication session
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

    /**
     * SIGN IN WITH EMAIL
     * Authentikasi user dengan email dan password
     * Simplified approach tanpa timeout untuk better reliability
     *
     * @param email Email address user
     * @param password Password user
     * @return Result<User> dengan user profile atau exception
     */
    suspend fun signInWithEmail(email: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Starting login for: $email")

                // STEP 1: Sign in dengan Supabase Auth
                val authResult = SupabaseManager.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                println("🔹 [REPO] Auth signin completed")

                // STEP 2: Wait untuk memastikan auth process completed
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

                // STEP 3: Ambil user profile dari database
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

    /**
     * DELETE USER ACCOUNT
     * Menghapus akun user secara permanen menggunakan Edge Function
     * Secure process dengan admin secret validation
     *
     * @return NetworkResult<Boolean> dengan status deletion
     */
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

    /**
     * CLEAR LOCAL CACHE
     * Membersihkan semua cached data dan session
     * Digunakan saat logout atau account deletion
     */
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

    /**
     * GET CURRENT USER
     * Mengambil data user yang sedang login
     * Combines auth user dengan profile data dari database
     *
     * @return User object atau null jika tidak ada user yang login
     */
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

    /**
     * UPDATE AUTOMATION SETTINGS
     * Mengupdate settings automation untuk greenhouse tertentu
     * Idempotent operation - handles both create dan update
     *
     * @param settings AutomationSettings object dengan updated values
     * @return NetworkResult dengan updated settings atau error
     */
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

    /**
     * TRY INSERT SETTINGS - Helper Method
     * Internal helper untuk insert new automation settings
     *
     * @param settings AutomationSettings untuk di-insert
     * @return NetworkResult dengan inserted settings
     */
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

    /**
     * GET AUTOMATION SETTINGS
     * Mengambil automation settings untuk greenhouse tertentu
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan settings atau null jika tidak ada
     */
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

    /**
     * CREATE AUTOMATION SETTINGS
     * Membuat automation settings default untuk greenhouse baru
     *
     * @param greenhouseId ID greenhouse untuk settings baru
     * @return NetworkResult dengan created settings
     */
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

    /**
     * GET USER GREENHOUSES
     * Mengambil semua greenhouse yang dimiliki oleh user
     * Delegate ke ApiService untuk actual implementation
     *
     * @param userId ID user pemilik greenhouse
     * @return NetworkResult dengan list of greenhouses
     */
    suspend fun getUserGreenhouses(userId: String): NetworkResult<List<Greenhouse>> {
        return apiService.getGreenhousesByUser(userId)
    }

    /**
     * GET LATEST SENSOR DATA
     * Mengambil pembacaan sensor terbaru untuk greenhouse
     * Delegate ke ApiService untuk actual implementation
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan latest sensor readings atau null
     */
    suspend fun getLatestSensorData(greenhouseId: String): NetworkResult<SensorReadings?> {
        return apiService.getLatestSensorReading(greenhouseId)
    }

    /**
     * GET SENSOR HISTORY
     * Mengambil historical sensor data untuk chart visualization
     *
     * @param greenhouseId ID greenhouse target
     * @param sensorType Jenis sensor (Temperature, Humidity, dll)
     * @param hours Jumlah jam history yang diambil
     * @return NetworkResult dengan list of sensor history
     */
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

    /**
     * GET CONTROL DEVICE
     * Mengambil status control device untuk greenhouse
     * Delegate ke ApiService untuk actual implementation
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan control device atau null
     */
    suspend fun getControlDevice(greenhouseId: String): NetworkResult<ControlDevices?> {
        return apiService.getControlDevice(greenhouseId)
    }

    /**
     * CREATE CONTROL DEVICE IF NOT EXISTS
     * Membuat control device entry jika belum ada
     * Idempotent operation - safe untuk multiple calls
     *
     * @param greenhouseId ID greenhouse untuk device baru
     * @return NetworkResult dengan created atau existing device
     */
    suspend fun createControlDeviceIfNotExists(greenhouseId: String): NetworkResult<ControlDevices> {
        return apiService.createControlDeviceIfNotExists(greenhouseId)
    }

    /**
     * GET REALTIME DEVICE UPDATES
     * Membuat Flow untuk real-time device updates
     * Menggunakan Supabase Realtime untuk live updates
     *
     * @param greenhouseId ID greenhouse untuk subscribe updates
     * @return Flow<ControlDevices?> dengan real-time updates
     */
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

    /**
     * UPDATE DEVICE CONTROL
     * Mengupdate status control device (fan, pump, auto mode)
     *
     * @param device ControlDevices object dengan updated values
     * @return NetworkResult dengan boolean success status
     */
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

    /**
     * GET SENSOR HISTORY BY DATE RANGE
     * Mengambil sensor history berdasarkan range tanggal spesifik
     * Untuk advanced chart filtering dan analysis
     *
     * @param greenhouseId ID greenhouse target
     * @param sensorType Jenis sensor yang di-query
     * @param startDate Start timestamp dalam milliseconds
     * @param endDate End timestamp dalam milliseconds
     * @return NetworkResult dengan filtered sensor history
     */
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

    /**
     * IOT DEVICES MANAGEMENT
     * Fungsi-fungsi untuk mengelola perangkat IoT
     */

    /**
     * GET IOT DEVICES BY GREENHOUSE
     * Mengambil semua device IoT yang terhubung dengan greenhouse
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan list of IoT devices
     */
    suspend fun getIoTDevicesByGreenhouse(greenhouseId: String): NetworkResult<List<IoTDevice>> {
        return apiService.getIoTDevicesByGreenhouse(greenhouseId)
    }

    /**
     * GET IOT DEVICE BY ID
     * Mengambil detail device IoT berdasarkan ID
     *
     * @param deviceId ID device target
     * @return NetworkResult dengan IoT device atau null
     */
    suspend fun getIoTDeviceById(deviceId: String): NetworkResult<IoTDevice?> {
        return apiService.getIoTDeviceById(deviceId)
    }

    /**
     * PAIR IOT DEVICE
     * Melakukan pairing device IoT dengan greenhouse
     *
     * @param deviceId ID device yang akan dipair
     * @param greenhouseId ID greenhouse target
     * @param pairingCode Kode pairing untuk verifikasi
     * @return NetworkResult dengan paired device
     */
    suspend fun pairIoTDevice(
        deviceId: String,
        greenhouseId: String,
        pairingCode: String
    ): NetworkResult<IoTDevice> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Pairing device: $deviceId with greenhouse: $greenhouseId")

                val result = apiService.pairIoTDevice(deviceId, greenhouseId, pairingCode)

                if (result is NetworkResult.Success) {
                    println("🔹 [REPO] Device paired successfully: ${result.data.deviceName}")
                } else {
                    println("🔹 [REPO] Device pairing failed: ${(result as NetworkResult.Error).message}")
                }

                result
            } catch (e: Exception) {
                println("🔹 [REPO] Error pairing device: ${e.message}")
                NetworkResult.Error("Failed to pair device: ${e.message}")
            }
        }
    }

    /**
     * UNPAIR IOT DEVICE
     * Melepas pairing device IoT dari greenhouse
     *
     * @param deviceId ID device yang akan dilepas
     * @return NetworkResult dengan unpaired device
     */
    suspend fun unpairIoTDevice(deviceId: String): NetworkResult<IoTDevice> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Unpairing device: $deviceId")

                val result = apiService.unpairIoTDevice(deviceId)

                if (result is NetworkResult.Success) {
                    println("🔹 [REPO] Device unpaired successfully")
                }

                result
            } catch (e: Exception) {
                println("🔹 [REPO] Error unpairing device: ${e.message}")
                NetworkResult.Error("Failed to unpair device: ${e.message}")
            }
        }
    }

    /**
     * UPDATE DEVICE STATUS
     * Mengupdate status device (last_seen, firmware_version)
     *
     * @param deviceId ID device target
     * @param firmwareVersion Versi firmware terbaru (optional)
     * @return NetworkResult dengan updated device
     */
    suspend fun updateDeviceStatus(
        deviceId: String,
        firmwareVersion: String? = null
    ): NetworkResult<IoTDevice> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Updating device status: $deviceId")

                val statusUpdate = DeviceStatusUpdate(
                    lastSeen = System.now().toString(),
                    firmwareVersion = firmwareVersion
                )

                val result = apiService.updateDeviceStatus(deviceId, statusUpdate)

                if (result is NetworkResult.Success) {
                    println("🔹 [REPO] Device status updated successfully")
                }

                result
            } catch (e: Exception) {
                println("🔹 [REPO] Error updating device status: ${e.message}")
                NetworkResult.Error("Failed to update device status: ${e.message}")
            }
        }
    }

    /**
     * SEND DEVICE COMMAND
     * Mengirim perintah ke device IoT
     *
     * @param deviceId ID device target
     * @param command Jenis perintah
     * @param payload Data tambahan (optional)
     * @return NetworkResult dengan created command
     */
    suspend fun sendDeviceCommand(
        deviceId: String,
        command: String,
        payload: String? = null
    ): NetworkResult<DeviceCommand> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Sending command to device: $deviceId - $command")

                val commandData = SendDeviceCommand(
                    deviceId = deviceId,
                    command = command,
                    payload = payload
                )

                val result = apiService.sendDeviceCommand(commandData)

                if (result is NetworkResult.Success) {
                    println("🔹 [REPO] Command sent successfully: ${result.data.id}")
                }

                result
            } catch (e: Exception) {
                println("🔹 [REPO] Error sending device command: ${e.message}")
                NetworkResult.Error("Failed to send device command: ${e.message}")
            }
        }
    }

    /**
     * GET PENDING DEVICE COMMANDS
     * Mengambil perintah yang belum dieksekusi untuk device
     *
     * @param deviceId ID device target
     * @return NetworkResult dengan list of pending commands
     */
    suspend fun getPendingDeviceCommands(deviceId: String): NetworkResult<List<DeviceCommand>> {
        return apiService.getPendingDeviceCommands(deviceId)
    }

    /**
     * MARK COMMAND AS EXECUTED
     * Menandai perintah device telah dieksekusi
     *
     * @param commandId ID command target
     * @return NetworkResult dengan boolean success status
     */
    suspend fun markCommandAsExecuted(commandId: String): NetworkResult<Boolean> {
        return apiService.markCommandAsExecuted(commandId)
    }

    /**
     * GET REALTIME SENSOR UPDATES (REPLACEMENT FOR TELEMETRY)
     * Mendengarkan update langsung dari tabel sensor_readings
     */
    fun getRealtimeSensorUpdates(greenhouseId: String): Flow<SensorReadings?> = callbackFlow {
        // Ubah nama channel agar unik
        val channel = supabaseClient.realtime.channel("sensor_updates_$greenhouseId")

        try {
            channel.subscribe()

            // Ubah target table menjadi 'sensor_readings'
            val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "sensor_readings"
                filter = "greenhouse_id=eq.$greenhouseId"
            }

            val json = Json { ignoreUnknownKeys = true }

            val job = launch {
                flow.collect { change ->
                    try {
                        val recordJson = change.record.toString()
                        // Decode langsung ke SensorReadings
                        val reading = json.decodeFromString<SensorReadings>(recordJson)
                        trySend(reading)
                    } catch (e: Exception) {
                        println("🔹 [REPO] Error decoding sensor reading: ${e.message}")
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

    /**
     * VALIDATE PAIRING CODE
     * Memvalidasi kode pairing untuk device IoT
     *
     * @param deviceId ID device target
     * @param pairingCode Kode pairing untuk divalidasi
     * @return NetworkResult dengan boolean valid status
     */
    suspend fun validatePairingCode(deviceId: String, pairingCode: String): NetworkResult<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Validating pairing code for device: $deviceId")

                val deviceResult = apiService.getIoTDeviceById(deviceId)

                return@withContext when (deviceResult) {
                    is NetworkResult.Success -> {
                        if (deviceResult.data == null) {
                            NetworkResult.Error("Device not found")
                        } else if (deviceResult.data.pairingCode == pairingCode) {
                            if (deviceResult.data.isPaired) {
                                NetworkResult.Error("Device already paired")
                            } else {
                                NetworkResult.Success(true)
                            }
                        } else {
                            NetworkResult.Error("Invalid pairing code")
                        }
                    }
                    is NetworkResult.Error -> deviceResult
                }
            } catch (e: Exception) {
                println("🔹 [REPO] Error validating pairing code: ${e.message}")
                NetworkResult.Error("Failed to validate pairing code: ${e.message}")
            }
        }
    }

    /**
     * SCAN AND REGISTER DEVICE
     * Mendaftarkan device baru ke sistem (untuk admin/setup)
     *
     * @param deviceData Data device baru
     * @return NetworkResult dengan registered device
     */
    suspend fun scanAndRegisterDevice(deviceData: IoTDevice): NetworkResult<IoTDevice> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Registering new device: ${deviceData.deviceId}")

                // Cek apakah device sudah terdaftar
                val existingDevice = apiService.getIoTDeviceByDeviceId(deviceData.deviceId)

                if (existingDevice is NetworkResult.Success && existingDevice.data != null) {
                    return@withContext NetworkResult.Success(existingDevice.data)
                }

                // Insert device baru
                val result: IoTDevice = SupabaseManager.client.postgrest["iot_devices"]
                    .insert(deviceData) {
                        select()
                    }
                    .decodeSingle()

                println("🔹 [REPO] Device registered successfully: ${result.id}")
                NetworkResult.Success(result)
            } catch (e: Exception) {
                println("🔹 [REPO] Error registering device: ${e.message}")
                NetworkResult.Error("Failed to register device: ${e.message}")
            }
        }
    }

    /**
     * CREATE NEW GREENHOUSE
     * Membuat greenhouse baru dengan data default otomatis
     *
     * @param name Nama greenhouse
     * @param location Lokasi greenhouse
     * @param description Deskripsi optional
     * @return NetworkResult dengan response data
     */
    suspend fun createGreenhouse(
        name: String,
        location: String,
        description: String? = null
    ): NetworkResult<CreateGreenhouseResponse> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [REPO] Creating new greenhouse: $name")

                val request = CreateGreenhouseRequest(
                    name = name,
                    location = location,
                    description = description
                )

                val result = apiService.createGreenhouse(request)

                if (result is NetworkResult.Success) {
                    println("🔹 [REPO] Greenhouse created successfully: ${result.data.greenhouse?.id}")

                    // Reload user greenhouses list
                    val currentUser = getCurrentUser()
                    currentUser?.let { user ->
                        getUserGreenhouses(user.id) // This will update the state
                    }
                }

                result
            } catch (e: Exception) {
                println("🔹 [REPO] Error creating greenhouse: ${e.message}")
                NetworkResult.Error("Failed to create greenhouse: ${e.message}")
            }
        }
    }
}
