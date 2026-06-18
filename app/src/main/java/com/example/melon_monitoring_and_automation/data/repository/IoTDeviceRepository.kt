/**
 * IOT DEVICE REPOSITORY - DEVICE MANAGEMENT LAYER
 *
 * Tujuan:
 * - Menyediakan specialized data operations untuk IoT device management
 * - Mengelola device pairing, listing, dan user-device relationships
 * - Menangani business logic khusus untuk IoT device operations
 *
 * Features:
 * - Device Pairing dengan pairing code validation
 * - Device Listing berdasarkan greenhouse dan user
 * - Device Status Management
 * - User-Device Relationship Management
 *
 * @author Your Name
 * @since Version 1.0
 * @param context Android Context untuk system operations
 */

package com.example.melon_monitoring_and_automation.data.repository

import android.content.Context
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.network.SupabaseManager
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class IoTDeviceRepository(
    private val context: Context
) {
    private val supabaseClient = SupabaseManager.client

    /**
     * PAIR DEVICE
     * Mem-pair device IoT dengan greenhouse tertentu
     * Validates pairing code dan device availability
     *
     * @param deviceId Unique device identifier
     * @param pairingCode Code pairing untuk validasi
     * @param greenhouseId ID greenhouse target untuk pairing
     * @return NetworkResult dengan paired device data
     */
    suspend fun pairDevice(
        deviceId: String,
        pairingCode: String,
        greenhouseId: String
    ): NetworkResult<IoTDevice> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [DEVICE REPO] Pairing device: $deviceId with greenhouse: $greenhouseId")

                val result: IoTDevice = supabaseClient.postgrest["iot_devices"]
                    .update({
                        set("greenhouse_id", greenhouseId)
                        set("is_paired", true)
                        set("paired_at", Clock.System.now().toString())
                    }) {
                        filter {
                            eq("device_id", deviceId)
                            eq("pairing_code", pairingCode)
                            eq("is_paired", false) // Hanya device yang belum paired
                        }
                    }
                    .decodeSingle()

                println("🔹 [DEVICE REPO] Device paired successfully: ${result.deviceId}")
                NetworkResult.Success(result)

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [DEVICE REPO] Pairing failed: ${e.message}")
                NetworkResult.Error("Pairing gagal: ${e.message}")
            }
        }
    }

    /**
     * GET GREENHOUSE DEVICES
     * Mengambil semua devices yang ter-pair dengan greenhouse tertentu
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan list of IoT devices
     */
    suspend fun getGreenhouseDevices(greenhouseId: String): NetworkResult<List<IoTDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔹 [DEVICE REPO] Getting devices for greenhouse: $greenhouseId")

                val result: List<IoTDevice> = supabaseClient.postgrest["iot_devices"]
                    .select {
                        filter { eq("greenhouse_id", greenhouseId) }
                    }
                    .decodeList()

                println("🔹 [DEVICE REPO] Found ${result.size} devices")
                NetworkResult.Success(result)

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                println("🔹 [DEVICE REPO] Error getting devices: ${e.message}")
                NetworkResult.Error("Gagal mengambil devices: ${e.message}")
            }
        }
    }

    /**
     * GET USER DEVICES
     * Mengambil semua devices yang dimiliki oleh user tertentu
     * Melalui relationship user → greenhouses → devices
     *
     * @param userId ID user pemilik devices
     * @return NetworkResult dengan list of all user's devices
     */
    suspend fun getUserDevices(userId: String): NetworkResult<List<IoTDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                // STEP 1: Get semua greenhouse milik user
                val greenhousesResult = getGreenhousesByUser(userId)

                if (greenhousesResult is NetworkResult.Success) {
                    val allDevices = mutableListOf<IoTDevice>()

                    // STEP 2: Untuk setiap greenhouse, ambil devices-nya
                    greenhousesResult.data.forEach { greenhouse ->
                        val devicesResult = getGreenhouseDevices(greenhouse.id)
                        if (devicesResult is NetworkResult.Success) {
                            allDevices.addAll(devicesResult.data)
                        }
                    }

                    NetworkResult.Success(allDevices)
                } else {
                    NetworkResult.Error("Gagal mengambil data greenhouse")
                }

            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                NetworkResult.Error("Gagal mengambil devices: ${e.message}")
            }
        }
    }

    /**
     * GET GREENHOUSES BY USER - Helper Method
     * Internal helper untuk mengambil greenhouse milik user
     *
     * @param userId ID user pemilik greenhouse
     * @return NetworkResult dengan list of user's greenhouses
     */
    private suspend fun getGreenhousesByUser(userId: String): NetworkResult<List<com.example.melon_monitoring_and_automation.domain.model.Greenhouse>> {
        return withContext(Dispatchers.IO) {
            try {
                val result: List<com.example.melon_monitoring_and_automation.domain.model.Greenhouse> =
                    supabaseClient.postgrest["greenhouses"]
                        .select {
                            filter { eq("owner_id", userId) }
                        }
                        .decodeList()

                NetworkResult.Success(result)
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
    throw e
} catch (e: Exception) {
                NetworkResult.Error("Gagal mengambil greenhouse")
            }
        }
    }
}