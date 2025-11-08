package com.example.melon_monitoring_and_automation.data.network

import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.Clock.System

class ApiService(private val postgrest: Postgrest) {

    // 🔹 Get all greenhouses by user - ✅ OK
    suspend fun getGreenhousesByUser(userId: String): NetworkResult<List<Greenhouse>> {
        return try {
            val result: List<Greenhouse> = postgrest["greenhouses"]
                .select {
                    filter { eq("owner_id", userId) }
                }
                .decodeList()
            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    // 🔹 Get latest sensor reading - ✅ OK
    suspend fun getLatestSensorReading(greenhouseId: String): NetworkResult<SensorReadings?> {
        return try {
            val result: List<SensorReadings> = postgrest["sensor_readings"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("recorded_at", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeList()
            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch sensor reading")
        }
    }

    // 🔹 Get control device state - ✅ OK
    suspend fun getControlDevice(greenhouseId: String): NetworkResult<ControlDevices?> {
        return try {
            val result: List<ControlDevices> = postgrest["control_devices"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                }
                .decodeList()
            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to get control device state")
        }
    }

    // 🔹 Update device state - ✅ FIXED (simplified)
    suspend fun updateControlDevice(deviceId: String, updatedDevice: ControlDevices): NetworkResult<Unit> {
        return try {
            postgrest["control_devices"]
                .update({
                    set("fan", updatedDevice.fan)
                    set("pump", updatedDevice.pump)
                    set("auto_mode", updatedDevice.autoMode)
                    set("updated_at", System.now().toString())
                }) {
                    filter { eq("id", deviceId) }
                }
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to update device state: ${e.message}")
        }
    }

    // 🔹 Get sensor history - ✅ OK
    suspend fun getSensorHistory(greenhouseId: String): NetworkResult<List<SensorHistory>> {
        return try {
            val result: List<SensorHistory> = postgrest["sensor_history"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("recorded_at", order = Order.DESCENDING)
                }
                .decodeList()
            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch sensor history")
        }
    }

    // 🔹 Create new control device entry - ✅ FIXED (simplified approach)
    suspend fun createControlDeviceIfNotExists(greenhouseId: String): NetworkResult<ControlDevices> {
        return try {
            // Check if device exists
            val existing = getControlDevice(greenhouseId)

            if (existing is NetworkResult.Success && existing.data != null) {
                return NetworkResult.Success(existing.data)
            }

            // Create new device with simplified approach
            val newDevice = mapOf(
                "greenhouse_id" to greenhouseId,
                "fan" to false,
                "pump" to false,
                "auto_mode" to false,
                "updated_at" to System.now().toString()
            )

            val result: ControlDevices = postgrest["control_devices"]
                .insert(newDevice) {
                    select()
                }
                .decodeSingle()

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to create control device: ${e.message}")
        }
    }
}
