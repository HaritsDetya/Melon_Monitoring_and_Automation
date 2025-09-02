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

    /**
     * Mengambil data sensor secara real-time dari Firebase.
     * Mengembalikan Flow yang akan memancarkan SensorReading setiap kali ada perubahan.
     */
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

    /**
     * Mengambil riwayat data sensor (contoh sederhana, bisa dikembangkan lagi).
     * Untuk riwayat data yang lebih kompleks, Anda mungkin perlu struktur data yang berbeda di Firebase
     * atau menggunakan database lokal/Cloud Firestore.
     */
//    suspend fun getHistoricalSensorData(): List<SensorReading> {
//        return try {
//            val snapshot = getSensorDataRef().get().await()
//            val readings = mutableListOf<SensorReading>()
//            val latestReading = snapshot.getValue(SensorReading::class.java)
//            if (latestReading != null) {
//                readings.add(latestReading)
//            }
//            readings
//        } catch (e: Exception) {
//            println("Error fetching historical data: ${e.message}")
//            emptyList()
//        }
//    }

    /**
     * Mengirim perintah kontrol ke aktuator (misalnya, menghidupkan/mematikan pompa).
     * @param device Nama perangkat (misal: "waterPump", "nutrientPumpA")
     * @param status Status yang diinginkan (misal: true untuk ON, false untuk OFF)
     */
    suspend fun setDeviceStatus(userId: String, systemId: String, device: String, status: Boolean) {
        try {
            getControlRef(userId, systemId).child(device).setValue(status).await()
            println("Perintah kontrol '$device' dengan status '$status' berhasil dikirim")
        } catch (e: Exception){
            println("Gagal mengirim perintah kontrol untuk '$device': ${e.message}")
        }
    }

    /**
     * Mengirim pengaturan otomatis (misalnya, batas pH, jadwal irigasi).
     * @param setting Nama pengaturan (misal: "phThreshold", "irrigationSchedule")
     * @param value Nilai pengaturan
     */
    suspend fun setAutomaticSetting(userId: String, systemId: String, setting: String, value: Any) {
        try {
            getControlRef(userId, systemId).child("autoSetting").child(setting).setValue(value).await()
            println("Pengaturan otomatis '$setting' dengan nilai '$value' berhasil dikirim")
        } catch (e: Exception) {
            println("Gagal mengirim pengaturan otomatis untuk '$setting': ${e.message}")
        }
    }

    /**
     * Menyimpan profil pengguna baru ke Firebase Realtime Database.
     * @param user Objek User yang akan disimpan.
     */

    suspend fun saveUserProfile(user: UserModel) {
        try {
            getUserProfileRef(user.uid).setValue(user).await()
            println("Profil pengguna ${user.username} berhasil disimpan.")
        } catch (e: Exception) {
            println("Gagal menyimpan profil pengguna ${e.message}")
        }
    }

    /**
     * Mengambil profil pengguna dari Firebase Realtime Database.
     * Mengembalikan Flow yang akan memancarkan objek User.
     * @param uid ID pengguna (dari Firebase Auth).
     */

    fun getUserProfile(uid: String): Flow<UserModel?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userProfile = snapshot.getValue(UserModel::class.java)
                trySend(userProfile)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        getUserProfileRef(uid).addValueEventListener(listener)
        awaitClose { getUserProfileRef(uid).removeEventListener(listener) }
    }
}