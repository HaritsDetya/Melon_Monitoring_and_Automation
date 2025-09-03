package com.example.melon_monitoring_and_automation.data.remote

import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    private fun getSensorDataRef(userId: String, systemId: String): DatabaseReference {
        return database.getReference("users/$userId/systems/$systemId/sensorData")
    }

    private fun getControlRef(userId: String, systemId: String): DatabaseReference {
        return database.getReference("users/$userId/systems/$systemId/sensorData")
    }

    private fun getUserProfileRef(uid: String): DatabaseReference {
        return database.getReference("userProfiles/$uid")
    }

    fun getRealtimeSensorData(userId: String, systemId: String): Flow<SensorReading> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensorReading = snapshot.getValue(SensorReading::class.java)
                if (sensorReading != null) {
                    trySend(sensorReading)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        getSensorDataRef(userId, systemId).addValueEventListener(listener)
        awaitClose{ getSensorDataRef(userId, systemId).removeEventListener(listener) }
    }

    suspend fun setDeviceStatus(userId: String, systemId: String, device: String, status: Boolean) {
        try {
            getControlRef(userId, systemId).child(device).setValue(status).await()
            println("Perintah kontrol '$device' dengan status '$status' berhasil dikirim")
        } catch (e: Exception){
            println("Gagal mengirim perintah kontrol untuk '$device': ${e.message}")
        }
    }

    suspend fun setAutomaticSetting(userId: String, systemId: String, setting: String, value: Any) {
        try {
            getControlRef(userId, systemId).child("autoSetting").child(setting).setValue(value).await()
            println("Pengaturan otomatis '$setting' dengan nilai '$value' berhasil dikirim")
        } catch (e: Exception) {
            println("Gagal mengirim pengaturan otomatis untuk '$setting': ${e.message}")
        }
    }

    suspend fun saveUserProfile(user: UserModel) {
        try {
            getUserProfileRef(user.uid).setValue(user).await()
            println("Profil pengguna ${user.username} berhasil disimpan.")
        } catch (e: Exception) {
            println("Gagal menyimpan profil pengguna ${e.message}")
        }
    }

    suspend fun saveSystemData(userId: String, systemId: String) {
        try {
            val defaultSystemData = mapOf(
                "sensorData" to mapOf(
                    "ec" to 0.0,
                    "humidity" to 0.0,
                    "ph" to 0.0,
                    "temperature" to 0.0,
                    "waterLevel" to 0.0,
                    "timestamp" to System.currentTimeMillis()
                ),
                "control" to mapOf(
                    "waterPump" to false,
                    "nutrientPumpA" to false,
                    "phThreshold" to 6.0,
                    "irrigationInterval" to 15
                )
            )
            database.getReference("users/$userId/systems/$systemId").setValue(defaultSystemData).await()
        } catch (e: Exception) {
            println("Gagal menyimpan data sistem awal: ${e.message}")
        }
    }

    fun getUserProfile(uid: String): Flow<UserModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userProfile = snapshot.getValue(UserModel::class.java)
                trySend(userProfile)
            }

            override fun onCancelled(error: DatabaseError) {
                println("Firebase database error: ${error.message}")
            }
        }
        getUserProfileRef(uid).addValueEventListener(listener)
        awaitClose { getUserProfileRef(uid).removeEventListener(listener) }
    }
}
