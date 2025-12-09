/**
 * API SERVICE - NETWORK LAYER
 *
 * Tujuan:
 * - Menyediakan abstraction layer untuk semua Supabase API calls
 * - Menangani network operations dengan error handling
 * - Mengelola data fetching dan manipulation
 *
 * Features:
 * - CRUD operations untuk Greenhouses, Sensor Readings, Control Devices
 * - Comprehensive error handling dengan NetworkResult
 * - Type-safe database queries dengan Supabase PostgREST
 * - Optimized queries dengan filtering dan ordering
 *
 * @author Your Name
 * @since Version 1.0
 * @param postgrest Supabase PostgREST client untuk database operations
 */

package com.example.melon_monitoring_and_automation.data.network

import com.example.melon_monitoring_and_automation.domain.model.AutomationSettings
import com.example.melon_monitoring_and_automation.domain.model.ControlDevices
import com.example.melon_monitoring_and_automation.domain.model.CreateGreenhouseRequest
import com.example.melon_monitoring_and_automation.domain.model.CreateGreenhouseResponse
import com.example.melon_monitoring_and_automation.domain.model.DeviceCommand
import com.example.melon_monitoring_and_automation.domain.model.DeviceStatusUpdate
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.IoTDevice
import com.example.melon_monitoring_and_automation.domain.model.SendDeviceCommand
import com.example.melon_monitoring_and_automation.domain.model.SensorReadings
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock.System

class ApiService(private val postgrest: Postgrest) {

    /**
     * GET GREENHOUSES BY USER
     * Mengambil semua greenhouse yang dimiliki oleh user tertentu
     *
     * @param userId ID user pemilik greenhouse
     * @return NetworkResult dengan list of Greenhouses atau error
     */
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

    /**
     * GET LATEST SENSOR READING
     * Mengambil pembacaan sensor terbaru untuk greenhouse tertentu
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan SensorReadings atau null jika tidak ada data
     */
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

    /**
     * GET CONTROL DEVICE STATE
     * Mengambil state control device untuk greenhouse tertentu
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan ControlDevices atau null jika tidak ada
     */
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

    /**
     * CREATE CONTROL DEVICE IF NOT EXISTS
     * Membuat entry control device baru jika belum ada
     * Idempotent operation - aman untuk dipanggil multiple times
     *
     * @param greenhouseId ID greenhouse untuk device baru
     * @return NetworkResult dengan ControlDevices yang baru dibuat atau existing
     */
    suspend fun createControlDeviceIfNotExists(greenhouseId: String): NetworkResult<ControlDevices> {
        return try {
            // Check jika device sudah ada
            val existing = getControlDevice(greenhouseId)

            if (existing is NetworkResult.Success && existing.data != null) {
                return NetworkResult.Success(existing.data)
            }

            // Buat device baru dengan default values
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

    /**
     * IOT DEVICES MANAGEMENT
     * Fungsi-fungsi untuk mengelola perangkat IoT
     */

    /**
     * GET IOT DEVICES BY GREENHOUSE
     * Mengambil semua device IoT yang terhubung dengan greenhouse tertentu
     *
     * @param greenhouseId ID greenhouse target
     * @return NetworkResult dengan list of IoT devices
     */
    suspend fun getIoTDevicesByGreenhouse(greenhouseId: String): NetworkResult<List<IoTDevice>> {
        return try {
            val result: List<IoTDevice> = postgrest["iot_devices"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("created_at", order = Order.DESCENDING)
                }
                .decodeList()
            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch IoT devices")
        }
    }

    /**
     * GET IOT DEVICE BY ID
     * Mengambil detail device IoT berdasarkan ID
     *
     * @param deviceId ID device target
     * @return NetworkResult dengan IoT device atau null
     */
    suspend fun getIoTDeviceById(deviceId: String): NetworkResult<IoTDevice?> {
        return try {
            val result: List<IoTDevice> = postgrest["iot_devices"]
                .select {
                    filter { eq("id", deviceId) }
                }
                .decodeList()
            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch IoT device")
        }
    }

    /**
     * GET IOT DEVICE BY DEVICE ID
     * Mengambil device IoT berdasarkan device_id (hardware ID)
     *
     * @param deviceId Hardware device ID
     * @return NetworkResult dengan IoT device atau null
     */
    suspend fun getIoTDeviceByDeviceId(deviceId: String): NetworkResult<IoTDevice?> {
        return try {
            val result: List<IoTDevice> = postgrest["iot_devices"]
                .select {
                    filter { eq("device_id", deviceId) }
                }
                .decodeList()
            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch IoT device by device ID")
        }
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
        return try {
            // Verifikasi pairing code terlebih dahulu
            val device = getIoTDeviceById(deviceId)
            if (device is NetworkResult.Success && device.data != null) {
                if (device.data.pairingCode != pairingCode) {
                    return NetworkResult.Error("Invalid pairing code")
                }

                if (device.data.isPaired) {
                    return NetworkResult.Error("Device already paired")
                }
            } else {
                return NetworkResult.Error("Device not found")
            }

            // Update device pairing status dengan map
            val updateData = mapOf<String, Any?>(
                "greenhouse_id" to greenhouseId,
                "is_paired" to true,
                "paired_at" to System.now().toString(),
                "last_seen" to System.now().toString()
            )

            val result: IoTDevice = postgrest["iot_devices"]
                .update(updateData) {
                    filter { eq("id", deviceId) }
                    select()
                }
                .decodeSingle()

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to pair device: ${e.message}")
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
        return try {
            // Gunakan mapOf untuk menghindari ambiguity
            val updateData = mapOf<String, Any?>(
                "greenhouse_id" to null,
                "is_paired" to false,
                "paired_at" to null
            )

            val result: IoTDevice = postgrest["iot_devices"]
                .update(updateData) {
                    filter { eq("id", deviceId) }
                    select()
                }
                .decodeSingle()

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to unpair device: ${e.message}")
        }
    }

    /**
     * UPDATE DEVICE STATUS
     * Mengupdate status device (last_seen, firmware_version)
     *
     * @param deviceId ID device target
     * @param statusUpdate DeviceStatusUpdate object
     * @return NetworkResult dengan updated device
     */
    suspend fun updateDeviceStatus(
        deviceId: String,
        statusUpdate: DeviceStatusUpdate
    ): NetworkResult<IoTDevice> {
        return try {
            // Gunakan mutable map untuk menghindari ambiguity
            val updateData = mutableMapOf<String, Any?>(
                "last_seen" to statusUpdate.lastSeen
            )

            // Tambahkan firmware_version hanya jika tidak null
            statusUpdate.firmwareVersion?.let {
                updateData["firmware_version"] = it
            }

            val result: IoTDevice = postgrest["iot_devices"]
                .update(updateData) {
                    filter { eq("id", deviceId) }
                    select()
                }
                .decodeSingle()

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to update device status: ${e.message}")
        }
    }

    /**
     * SEND DEVICE COMMAND
     * Mengirim perintah ke device IoT
     *
     * @param command SendDeviceCommand object
     * @return NetworkResult dengan created command
     */
    suspend fun sendDeviceCommand(command: SendDeviceCommand): NetworkResult<DeviceCommand> {
        return try {
            val commandData = mapOf(
                "device_id" to command.deviceId,
                "command" to command.command,
                "payload" to command.payload,
                "is_executed" to false,
                "created_at" to System.now().toString()
            )

            val result: DeviceCommand = postgrest["device_commands"]
                .insert(commandData) {
                    select()
                }
                .decodeSingle()

            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to send device command: ${e.message}")
        }
    }

    /**
     * GET PENDING DEVICE COMMANDS
     * Mengambil perintah yang belum dieksekusi untuk device tertentu
     *
     * @param deviceId ID device target
     * @return NetworkResult dengan list of pending commands
     */
    suspend fun getPendingDeviceCommands(deviceId: String): NetworkResult<List<DeviceCommand>> {
        return try {
            val result: List<DeviceCommand> = postgrest["device_commands"]
                .select {
                    filter {
                        eq("device_id", deviceId)
                        eq("is_executed", false)
                    }
                    order("created_at", order = Order.ASCENDING)
                }
                .decodeList()
            NetworkResult.Success(result)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to fetch pending commands")
        }
    }

    /**
     * MARK COMMAND AS EXECUTED
     * Menandai perintah device telah dieksekusi
     *
     * @param commandId ID command target
     * @return NetworkResult dengan boolean success status
     */
    suspend fun markCommandAsExecuted(commandId: String): NetworkResult<Boolean> {
        return try {
            val updateData = mapOf<String, Any?>(
                "is_executed" to true,
                "executed_at" to System.now().toString()
            )

            postgrest["device_commands"]
                .update(updateData) {
                    filter { eq("id", commandId) }
                }
            NetworkResult.Success(true)
        } catch (e: Exception) {
            NetworkResult.Error(e.localizedMessage ?: "Failed to mark command as executed: ${e.message}")
        }
    }

    /**
     * CREATE NEW GREENHOUSE
     * Membuat greenhouse baru dengan data default otomatis
     *
     * @param request CreateGreenhouseRequest object
     * @return NetworkResult dengan response data
     */
    suspend fun createGreenhouse(request: CreateGreenhouseRequest): NetworkResult<CreateGreenhouseResponse> {
        return try {
            val currentUser = SupabaseManager.client.auth.currentUserOrNull()
            if (currentUser == null) {
                return NetworkResult.Error("User not authenticated")
            }

            println("🔹 [API] Creating greenhouse for user: ${currentUser.id}")
            println("🔹 [API] Greenhouse data: ${request.name}, ${request.location}")

            // Prepare greenhouse data
            val greenhouseData = mapOf(
                "name" to request.name,
                "location" to request.location,
                "owner_id" to currentUser.id,
                "description" to request.description,
                "created_at" to System.now().toString()
            )

            // Insert greenhouse
            val greenhouse: Greenhouse = postgrest["greenhouses"]
                .insert(greenhouseData) {
                    select()
                }
                .decodeSingle()

            println("🔹 [API] Greenhouse created: ${greenhouse.id}")

            // Wait for trigger to create default data
            delay(1000)

            // Get created default data
            val settingsResult = getAutomationSettingsForGreenhouse(greenhouse.id)
            val controlDeviceResult = getControlDeviceForGreenhouse(greenhouse.id)

            // Handle the results
            val settings = if (settingsResult is NetworkResult.Success) {
                settingsResult.data
            } else {
                null
            }

            val controlDevice = if (controlDeviceResult is NetworkResult.Success) {
                controlDeviceResult.data
            } else {
                null
            }

            // Log results
            println("🔹 [API] Settings created: ${settings != null}")
            println("🔹 [API] Control device created: ${controlDevice != null}")

            NetworkResult.Success(
                CreateGreenhouseResponse(
                    success = true,
                    message = "Greenhouse created successfully",
                    greenhouse = greenhouse,
                    settings = settings,
                    controlDevice = controlDevice
                )
            )
        } catch (e: Exception) {
            println("🔹 [API] Error creating greenhouse: ${e.message}")
            e.printStackTrace()
            NetworkResult.Error(e.localizedMessage ?: "Failed to create greenhouse: ${e.message}")
        }
    }

    /**
     * GET AUTOMATION SETTINGS FOR GREENHOUSE
     * Helper untuk mendapatkan settings setelah greenhouse dibuat
     */
    private suspend fun getAutomationSettingsForGreenhouse(greenhouseId: String): NetworkResult<AutomationSettings?> {
        return try {
            println("🔹 [API] Getting automation settings for greenhouse: $greenhouseId")

            val result: List<AutomationSettings> = postgrest["automation_settings"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                }
                .decodeList()

            println("🔹 [API] Found ${result.size} automation settings")

            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            println("🔹 [API] Error getting automation settings: ${e.message}")
            NetworkResult.Error(e.localizedMessage ?: "Failed to get automation settings")
        }
    }

    /**
     * GET CONTROL DEVICE FOR GREENHOUSE
     * Helper untuk mendapatkan control device setelah greenhouse dibuat
     */
    private suspend fun getControlDeviceForGreenhouse(greenhouseId: String): NetworkResult<ControlDevices?> {
        return try {
            println("🔹 [API] Getting control device for greenhouse: $greenhouseId")

            val result: List<ControlDevices> = postgrest["control_devices"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                }
                .decodeList()

            println("🔹 [API] Found ${result.size} control devices")

            NetworkResult.Success(result.firstOrNull())
        } catch (e: Exception) {
            println("🔹 [API] Error getting control device: ${e.message}")
            NetworkResult.Error(e.localizedMessage ?: "Failed to get control device")
        }
    }
}
