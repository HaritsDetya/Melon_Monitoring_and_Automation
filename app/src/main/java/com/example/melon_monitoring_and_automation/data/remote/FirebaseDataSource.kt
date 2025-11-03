package com.example.melon_monitoring_and_automation.data.remote

import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
//class FirebaseDataSource @Inject constructor(
//    private val database: DatabaseReference
//) {
//
//    suspend fun addGreenhouseMember(greenhouseId: String, memberUid: String) {
//        database.child("greenhouses").child(greenhouseId).child("members").child(memberUid).setValue(true).await()
//    }
//
//    private fun getGreenhouseRef(greenhouseId: String): DatabaseReference =
//        database.child("greenhouses").child(greenhouseId)
//
//    private fun getDeviceRef(greenhouseId: String, deviceId: String): DatabaseReference =
//        database.child("greenhouses").child(greenhouseId).child("devices").child(deviceId)
//
//    private fun getSensorReadingsRef(greenhouseId: String): DatabaseReference =
//        database.child("sensor_readings").child(greenhouseId)
//
//    private fun getGreenhouseDevicesRef(greenhouseId: String): DatabaseReference =
//        database.child("greenhouses").child(greenhouseId).child("devices")
//
//    private fun getGreenhousePlantsRef(greenhouseId: String): DatabaseReference =
//        database.child("greenhouses").child(greenhouseId).child("plants")
//
//    private fun getUserProfileRef(uid: String): DatabaseReference =
//        database.child("users").child(uid)
//
//    // ===================== User =====================
//
//    suspend fun getUidByEmail(email: String): String? {
//        val snapshot = database.child("users")
//            .orderByChild("email")
//            .equalTo(email)
//            .get()
//            .await()
//        return snapshot.children.firstOrNull()?.key
//    }
//
//    fun getUserProfile(uid: String): Flow<User?> = callbackFlow {
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                trySend(snapshot.getValue(User::class.java))
//            }
//            override fun onCancelled(error: DatabaseError) {
//                close(error.toException())
//            }
//        }
//        getUserProfileRef(uid).addValueEventListener(listener)
//        awaitClose { getUserProfileRef(uid).removeEventListener(listener) }
//    }
//
//    suspend fun registerUserAndGreenhouse(user: User, greenhouse: Greenhouse) {
//        val updates = mutableMapOf<String, Any>()
//
//        // Add user data
//        user.toMap().let { updates["users/${user.uid}"] = it }
//
//        // Add greenhouse and its data
//        greenhouse.toMap().let { updates["greenhouses/${greenhouse.id}"] = it }
//
//        // Add greenhouse ID to user's list
//        updates["users/${user.uid}/greenhouses/${greenhouse.id}"] = true
//
//        // Add default sensor reading
//        val sensorReading = SensorReading(recorded_at = System.currentTimeMillis())
//        updates["sensor_readings/${greenhouse.id}/${System.currentTimeMillis()}"] = sensorReading
//
//        database.updateChildren(updates).await()
//    }
//
//    suspend fun saveUserProfile(user: User) {
//        database.child("users").child(user.uid).setValue(user).await()
//    }
//
//    // ===================== Greenhouse =====================
//
//    fun getUsersGreenhouses(greenhouseIds: List<String>): Flow<List<Greenhouse>> = callbackFlow {
//        val listeners = mutableMapOf<String, ValueEventListener>()
//
//        if (greenhouseIds.isEmpty()) {
//            trySend(emptyList())
//            awaitClose {}
//            return@callbackFlow
//        }
//        val allGreenhouses = mutableListOf<Greenhouse>()
//        val totalListeners = greenhouseIds.size
//        var receivedCount = 0
//
//        greenhouseIds.forEach { greenhouseId ->
//            val ref = getGreenhouseRef(greenhouseId)
//            val listener = object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val greenhouse = snapshot.getValue(Greenhouse::class.java)?.copy(id = snapshot.key ?: "")
//                    if (greenhouse != null) {
//                        val existingIndex = allGreenhouses.indexOfFirst { it.id == greenhouse.id }
//                        if (existingIndex != -1) {
//                            allGreenhouses[existingIndex] = greenhouse
//                        } else {
//                            allGreenhouses.add(greenhouse)
//                        }
//                        receivedCount++
//                        if (receivedCount == totalListeners) {
//                            trySend(allGreenhouses.toList())
//                        }
//                    } else {
//                        allGreenhouses.removeAll { it.id == snapshot.key }
//                        receivedCount++
//                        trySend(allGreenhouses.toList())
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {
//                    close(error.toException())
//                }
//            }
//            ref.addValueEventListener(listener)
//            listeners[greenhouseId] = listener
//        }
//        awaitClose {
//            listeners.forEach { (id, listener) ->
//                getGreenhouseRef(id).removeEventListener(listener)
//            }
//        }
//    }
//
//    suspend fun saveGreenhouse(greenhouseId: String, greenhouse: Greenhouse) {
//        getGreenhouseRef(greenhouseId).setValue(greenhouse).await()
//    }
//
//    suspend fun addGreenhouseToUser(uid: String, greenhouseId: String) {
//        database.child("users/$uid/greenhouses").child(greenhouseId).setValue(true).await()
//    }
//
//    // =====================
//    // Sensor Data
//    // =====================
//
//    fun getRealtimeSensorData(greenhouseId: String): Flow<Pair<String, SensorReading>?> =
//        callbackFlow {
//            val ref = getSensorReadingsRef(greenhouseId).orderByKey().limitToLast(1)
//            val listener = object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val latestNode = snapshot.children.firstOrNull()
//                    val key = latestNode?.key
//                    val reading = latestNode?.getValue(SensorReading::class.java)
//                    if (key != null && reading != null) {
//                        trySend(key to reading)
//                    } else {
//                        trySend(null)
//                    }
//                }
//                override fun onCancelled(error: DatabaseError) {
//                    close(error.toException())
//                }
//            }
//            ref.addValueEventListener(listener)
//            awaitClose { ref.removeEventListener(listener) }
//        }
//
//    fun getHistoricalSensorDataFromFirebase(
//        greenhouseId: String
//    ): Flow<List<Pair<Long, SensorReading>>> = callbackFlow {
//        val ref = getSensorReadingsRef(greenhouseId)
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val list = snapshot.children.mapNotNull { child ->
//                    val reading = child.getValue(SensorReading::class.java)
//                    if (reading != null && reading.recorded_at != null) {
//                        val timestamp = reading.recorded_at
//                        timestamp to reading
//                    } else {
//                        null
//                    }
//                }
//                trySend(list.sortedByDescending { it.first })
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                close(error.toException())
//            }
//        }
//        ref.addValueEventListener(listener)
//        awaitClose { ref.removeEventListener(listener) }
//    }
//
//    suspend fun saveSensorReading(
//        greenhouseId: String,
//        sensorReading: SensorReading
//    ) {
//        getSensorReadingsRef(greenhouseId).push().setValue(sensorReading).await()
//    }
//
//    suspend fun addSensorReading(greenhouseId: String, sensorReading: SensorReading) {
//        database.child("sensor_readings").child(greenhouseId)
//            .push()
//            .setValue(sensorReading)
//            .await()
//    }
//
//    suspend fun editSensorReading(
//        greenhouseId: String,
//        readingId: String,
//        updatedReading: SensorReading
//    ) {
//        database.child("sensor_readings").child(greenhouseId).child(readingId)
//            .setValue(updatedReading)
//            .await()
//    }
//
//    suspend fun deleteSensorReading(greenhouseId: String, readingId: String) {
//        database.child("sensor_readings").child(greenhouseId).child(readingId)
//            .removeValue()
//            .await()
//    }
//
//    // =====================
//    // Device
//    // =====================
//
//    fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> = callbackFlow {
//        val ref = getGreenhouseDevicesRef(greenhouseId)
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val devices = snapshot.children.mapNotNull { child ->
//                    try {
//                        child.getValue(Device::class.java)?.copy(id = child.key ?: "")
//                    } catch (e: DatabaseException) {
//                        println("Error deserializing device: ${e.message}")
//                        null
//                    }
//                }
//                trySend(devices)
//            }
//            override fun onCancelled(error: DatabaseError) {
//                trySend(emptyList())
//                close(error.toException())
//            }
//        }
//        ref.addValueEventListener(listener)
//        awaitClose { ref.removeEventListener(listener) }
//    }
//
//    suspend fun setDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean) {
//        getDeviceRef(greenhouseId, deviceId).child("status").setValue(status).await()
//    }
//
//    suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any) {
//        database.child("greenhouses/$greenhouseId/devices/$deviceId/config").child(setting).setValue(value).await()
//    }
//
//    suspend fun addDevice(greenhouseId: String, device: Device) {
//        database.child("greenhouses").child(greenhouseId).child("devices").child(device.id)
//            .setValue(device)
//            .await()
//    }
//    suspend fun editDevice(greenhouseId: String, device: Device) {
//        val updates = mapOf(
//            "name" to device.name,
//            "type" to device.type,
//            "status" to device.status
//        )
//
//        database.child("greenhouses").child(greenhouseId).child("devices").child(device.id)
//            .setValue(updates)
//            .await()
//    }
//    suspend fun deleteDevice(greenhouseId: String, deviceId: String) {
//        database.child("greenhouses").child(greenhouseId).child("devices").child(deviceId)
//            .removeValue()
//            .await()
//    }
//
//    // =====================
//    // Plant
//    // =====================
//
//    fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>> = callbackFlow {
//        val ref = getGreenhousePlantsRef(greenhouseId)
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val plants = snapshot.children.mapNotNull { child ->
//                    try {
//                        child.getValue(Plant::class.java)?.copy(id = child.key ?: "")
//                    } catch (e: DatabaseException) {
//                        println("Error deserializing plant: ${e.message}")
//                        null
//                    }
//                }
//                trySend(plants)
//            }
//            override fun onCancelled(error: DatabaseError) {
//                close(error.toException())
//            }
//        }
//        ref.addValueEventListener(listener)
//        awaitClose { ref.removeEventListener(listener) }
//    }
//
//    suspend fun addPlant(greenhouseId: String, plant: Plant) {
//        database.child("greenhouses").child(greenhouseId).child("plants").child(plant.id)
//            .setValue(plant)
//            .await()
//    }
//
//    suspend fun updatePlant(
//        greenhouseId: String,
//        plantId: String,
//        updatedPlant: Plant
//    ) {
//        val updates = mapOf(
//            "name" to updatedPlant.name,
//            "type" to updatedPlant.type,
//            "planted_at" to updatedPlant.planted_at
//        )
//
//        database.child("greenhouses").child(greenhouseId).child("plants").child(plantId)
//            .setValue(updates)
//            .await()
//    }
//
//    suspend fun deletePlant(greenhouseId: String, plantId: String) {
//        database.child("greenhouses").child(greenhouseId).child("plants").child(plantId)
//            .removeValue()
//            .await()
//    }
//}
