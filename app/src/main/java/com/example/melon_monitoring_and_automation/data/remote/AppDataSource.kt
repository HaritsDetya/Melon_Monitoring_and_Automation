package com.example.melon_monitoring_and_automation.data.remote

import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.GreenhouseMember
import com.example.melon_monitoring_and_automation.domain.model.NewGreenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.PlantHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {

    // ===================== User =====================

    suspend fun getUserProfileByUserId(userId: String): UserProfile? {
        return supabaseClient.postgrest["user_profiles"]
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<UserProfile>()
    }

    suspend fun registerUserAndGreenhouse(userProfile: UserProfile, newGreenhouse: NewGreenhouse) {
        supabaseClient.postgrest["user_profiles"].insert(userProfile)
        supabaseClient.postgrest["greenhouses"].insert(newGreenhouse)
    }

    suspend fun saveUserProfile(userProfile: UserProfile) {
        supabaseClient.postgrest["user_profiles"]
            .update(userProfile) {
                filter { eq("user_id", userProfile.user_id) }
            }
    }

    // ===================== Greenhouse =====================

    suspend fun getUsersGreenhouses(ownerId: String): List<Greenhouse> {
        return supabaseClient.postgrest["greenhouses"]
            .select {
                filter { eq("owner_id", ownerId) }
            }
            .decodeList<Greenhouse>()
    }

    suspend fun saveGreenhouse(greenhouse: Greenhouse) {
        supabaseClient.postgrest["greenhouses"]
            .update(greenhouse) {
                filter { eq("id", greenhouse.id) }
            }
    }

    suspend fun addGreenhouse(newGreenhouse: NewGreenhouse) {
        supabaseClient.postgrest["greenhouses"].insert(newGreenhouse)
    }

    suspend fun addGreenhouseMember(member: GreenhouseMember) {
        supabaseClient.postgrest["greenhouse_members"].insert(member)
    }

    // ===================== Sensor Data =====================

    fun getLatestSensorData(greenhouseId: String): Flow<SensorReading?> = flow {
        val result = try {
            supabaseClient.postgrest["sensor_readings"]
                .select(){
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("recorded_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SensorReading>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
        emit(result)
    }

    fun getRealtimeSensorData(greenhouseId: String): Flow<SensorReading?> = callbackFlow {

        val result = try {
            supabaseClient.postgrest["sensor_readings"]
                .select(){
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("recorded_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<SensorReading>()
                .firstOrNull()
        } catch (e: Exception) {
            null
        }

        trySend(result)

        val channel = supabaseClient.realtime.channel("sensor_readings_channel")

        try {
            val flow = channel.postgresChangeFlow<PostgresAction.Select>(
                schema = "public"
            ) {
                table = "sensor_readings"
            }.map {
                it.decodeRecord<SensorReading>()
            }.filter {
                it.greenhouse_id == greenhouseId
            }.onEach { sensorReading ->
                trySend(sensorReading)
            }

            channel.subscribe()
            flow.launchIn(this)

            awaitCancellation()
        } finally {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    suspend fun getHistoricalSensorDataFromSupabase(
        greenhouseId: String
    ): List<SensorReading> {
        return supabaseClient.postgrest["sensor_readings"]
            .select {
                filter { eq("greenhouse_id", greenhouseId) }
                order("recorded_at", Order.DESCENDING)
            }
            .decodeList<SensorReading>()
    }

    suspend fun saveSensorReading(sensorReading: SensorReading) {
        supabaseClient.postgrest["sensor_readings"].insert(sensorReading)
    }

    suspend fun editSensorReading(
        readingId: String,
        updatedReading: SensorReading
    ) {
        supabaseClient.postgrest["sensor_readings"]
            .update(updatedReading) {
                filter { eq("id", readingId) }
            }
    }

    suspend fun deleteSensorReading(readingId: String) {
        supabaseClient.postgrest["sensor_readings"]
            .delete {
                filter { eq("id", readingId) }
            }
    }

    // ===================== Device =====================

    fun getDevices(greenhouseId: String): Flow<List<Device>> = flow {
        val devices = try {
            supabaseClient.postgrest["devices"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Device>()
        } catch (e: Exception) {
            emptyList()
        }

        emit(devices)
    }

    fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> = callbackFlow {

        val initialDevices = try {
            supabaseClient.postgrest["devices"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("name", Order.ASCENDING)
                }
                .decodeList<Device>()
        } catch (e: Exception) {
            emptyList()
        }

        trySend(initialDevices)

        val channel = supabaseClient.realtime.channel("devices_control:$greenhouseId")

        try {
            val flow = channel.postgresChangeFlow<PostgresAction>(
                schema = "public"
            ) {
                table = "devices"
                filter = "greenhouse_id=eq.$greenhouseId"
            }.map {
                supabaseClient.postgrest["devices"]
                    .select { filter { eq("greenhouse_id", greenhouseId) } }
                    .decodeList<Device>()
            }.onEach { updatedDevices ->
                trySend(updatedDevices)
            }

            channel.subscribe()
            flow.launchIn(this)

            awaitCancellation()
        } finally {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    suspend fun addDevice(device: Device) {
        supabaseClient.postgrest["devices"].insert(device)
    }

    suspend fun editDevice(deviceId: String, device: Device) {
        supabaseClient.postgrest["devices"]
            .update(device) {
                filter { eq("id", deviceId) }
            }
    }

    suspend fun deleteDevice(deviceId: String) {
        supabaseClient.postgrest["devices"]
            .delete {
                filter { eq("id", deviceId) }
            }
    }

    suspend fun setDeviceStatus(deviceId: String, status: Boolean) {
        supabaseClient.postgrest["devices"]
            .update(
                mapOf("status" to status),
            ) {
                filter { eq("id", deviceId) }
            }
    }

    suspend fun setAutomaticSetting(deviceId: String, setting: String, value: Any) {
        val updates = mapOf(
            "config" to mapOf(setting to value)
        )

        supabaseClient.postgrest["devices"]
            .update(updates) {
                filter { eq("id", deviceId) }
            }
    }

    // ===================== Plant =====================

    fun getPlants(greenhouseId: String): Flow<List<Plant>> = flow {
        val plants = try {
            supabaseClient.postgrest["plants"]
                .select {
                    filter { eq("greenhouse_id", greenhouseId) }
                    order("plant_date", Order.DESCENDING)
                }
                .decodeList<Plant>()
        } catch (e: Exception) {
            emptyList()
        }

        emit(plants)
    }

    fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>> = callbackFlow {
        val channel = supabaseClient.realtime.channel("plants_channel")

        try {
            val flow = channel.postgresChangeFlow<PostgresAction.Insert>(
                schema = "public"
            ) {
                table = "plants"
            }.map {
                it.decodeRecord<Plant>()
            }.filter {
                it.greenhouse_id == greenhouseId
            }.onEach {
                val plants = supabaseClient.postgrest["plants"]
                    .select { filter { eq("greenhouse_id", greenhouseId) } }
                    .decodeList<Plant>()
                trySend(plants)
            }

            channel.subscribe()
            flow.launchIn(this)

            awaitCancellation()
        } finally {
            supabaseClient.realtime.removeChannel(channel)
        }
    }

    suspend fun addPlant(plant: Plant) {
        supabaseClient.postgrest["plants"].insert(plant)
    }

    suspend fun updatePlant(readingId: String, updatedPlant: Plant) {
        supabaseClient.postgrest["plants"]
            .update(updatedPlant) {
                filter { eq("id", readingId) }
            }
    }

    suspend fun deletePlant(plantId: String) {
        supabaseClient.postgrest["plants"]
            .delete {
                filter { eq("id", plantId) }
            }
    }

    suspend fun addPlantHistory(plantHistory: PlantHistory) {
        supabaseClient.postgrest["plant_histories"].insert(plantHistory)
    }
}
