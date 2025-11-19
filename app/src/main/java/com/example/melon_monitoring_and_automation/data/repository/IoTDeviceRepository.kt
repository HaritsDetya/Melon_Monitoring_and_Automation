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

    // Pair device dengan user/greenhouse
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
                            eq("is_paired", false)
                        }
                    }
                    .decodeSingle()

                println("🔹 [DEVICE REPO] Device paired successfully: ${result.deviceId}")
                NetworkResult.Success(result)

            } catch (e: Exception) {
                println("🔹 [DEVICE REPO] Pairing failed: ${e.message}")
                NetworkResult.Error("Pairing gagal: ${e.message}")
            }
        }
    }

    // Get devices by greenhouse
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

            } catch (e: Exception) {
                println("🔹 [DEVICE REPO] Error getting devices: ${e.message}")
                NetworkResult.Error("Gagal mengambil devices: ${e.message}")
            }
        }
    }

    // Get all user's devices
    suspend fun getUserDevices(userId: String): NetworkResult<List<IoTDevice>> {
        return withContext(Dispatchers.IO) {
            try {
                // First get user's greenhouses, then get devices for each greenhouse
                val greenhousesResult = getGreenhousesByUser(userId)

                if (greenhousesResult is NetworkResult.Success) {
                    val allDevices = mutableListOf<IoTDevice>()

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

            } catch (e: Exception) {
                NetworkResult.Error("Gagal mengambil devices: ${e.message}")
            }
        }
    }

    // Helper function to get user's greenhouses
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
            } catch (e: Exception) {
                NetworkResult.Error("Gagal mengambil greenhouse")
            }
        }
    }
}
