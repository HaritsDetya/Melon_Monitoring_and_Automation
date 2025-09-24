package com.example.melon_monitoring_and_automation.data.repository

import android.util.Log
import com.example.melon_monitoring_and_automation.data.local.DeviceDao
import com.example.melon_monitoring_and_automation.data.local.GreenhouseDao
import com.example.melon_monitoring_and_automation.data.local.SensorHistoryDao
import com.example.melon_monitoring_and_automation.data.local.UserDao
import com.example.melon_monitoring_and_automation.data.local.toDomain
import com.example.melon_monitoring_and_automation.data.local.toEntity
import com.example.melon_monitoring_and_automation.data.local.toModel
import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
import com.example.melon_monitoring_and_automation.di.DispatcherProvider
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydroponicRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseDataSource,
    private val userDao: UserDao,
    private val greenhouseDao: GreenhouseDao,
    private val sensorHistoryDao: SensorHistoryDao,
    private val deviceDao: DeviceDao,
    private val dispatcherProvider: DispatcherProvider
) : HydroponicRepository {

    suspend fun shareGreenhouseAccess(greenhouseId: String, memberEmail: String) {
        val memberUid = firebaseDataSource.getUidByEmail(memberEmail)
        if (memberUid != null) {
            // Tambahkan member ke greenhouse
            firebaseDataSource.addGreenhouseMember(greenhouseId, memberUid)
            // Tambahkan greenhouse ke profil member
            firebaseDataSource.addGreenhouseToUser(memberUid, greenhouseId)
        } else {
            // Tangani kasus email tidak ditemukan
        }
    }

    // USER
    override fun getUserProfile(uid: String): Flow<User?> =
        firebaseDataSource.getUserProfile(uid)

    override suspend fun saveUserProfile(user: User) {
        firebaseDataSource.saveUserProfile(user)
        userDao.insert(user.toEntity())
    }

    override suspend fun clearLocalCache() {
        userDao.clearAll()
        greenhouseDao.clearAll()
        sensorHistoryDao.clearAll()
        deviceDao.clearAll()
    }

    // GREENHOUSE
    override fun getUserGreenhouses(uid: String): Flow<List<Greenhouse>> = flow {
        emit(greenhouseDao.getAllGreenhouses().map { it.toModel() })

        firebaseDataSource.getUserProfile(uid)
            .flatMapConcat { user ->
                val greenhouseIds = user?.greenhouses?.keys?.toList() ?: emptyList()
                firebaseDataSource.getUsersGreenhouses(greenhouseIds)
            }
            .collect { greenhouses ->
                greenhouseDao.insertAll(greenhouses.map { it.toEntity() })
                emit(greenhouses)
            }
    }.flowOn(dispatcherProvider.io)

    override suspend fun saveGreenhouse(greenhouseId: String, greenhouse: Greenhouse) {
        firebaseDataSource.saveGreenhouse(greenhouseId, greenhouse)
        greenhouseDao.insert(greenhouse.toEntity())
    }

    override suspend fun addGreenhouseToUser(uid: String, greenhouseId: String) {
        firebaseDataSource.addGreenhouseToUser(uid, greenhouseId)
    }

    // SENSOR
    override fun getLatestSensorData(greenhouseId: String): Flow<Pair<String, SensorReading>?> =
        firebaseDataSource.getRealtimeSensorData(greenhouseId)
            .onEach { pair ->
                if (pair != null) {
                    val (key, reading) = pair
                    // ✅ PERBAIKAN: Gunakan recorded_at yang sudah Long, atau Push ID key
                    val timestamp = reading.recorded_at ?: key.substring(1, 14).toLongOrNull() ?: System.currentTimeMillis()

                    sensorHistoryDao.insertSensorReading(
                        reading.toEntity(greenhouseId, timestamp)
                    )
                }
            }
            .flowOn(dispatcherProvider.io)

    // ✅ Perbaikan: Mengubah signature agar cocok dengan interface
    override fun getHistoricalSensorData(
        greenhouseId: String
    ): Flow<List<Pair<Long, SensorReading>>> = flow {
        // Ambil semua data dari Firebase
        firebaseDataSource.getHistoricalSensorDataFromFirebase(greenhouseId)
            .collect { allData ->
                // Emit semua data ke use case
                emit(allData)
            }
    }.flowOn(dispatcherProvider.io)

    // ✅ Perbaikan: Tidak perlu timestamp dari client. Firebase akan meng-generate Push ID.
    override suspend fun saveSensorReading(greenhouseId: String, sensorReading: SensorReading) {
        firebaseDataSource.saveSensorReading(greenhouseId, sensorReading)
        // Note: Untuk menyimpan ke cache, kita perlu mendapatkan Push ID yang dibuat oleh Firebase.
        // Ini bisa dilakukan dengan mengamati data dari Firebase setelah penulisan, atau
        // menggunakan Firebase Functions untuk memproses penulisan.
    }


    // DEVICE
    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    override fun getDeviceStatus(greenhouseId: String, deviceId: String): Flow<Boolean?> =
        firebaseDataSource.getDeviceStatus(greenhouseId, deviceId)

    // ✅ Perbaikan: Tidak perlu diubah, sudah sesuai.
    override fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> = flow {
        val cached = deviceDao.getDevicesByGreenhouse(greenhouseId).map { it.toModel() }
        emit(cached)

        firebaseDataSource.getGreenhouseDevices(greenhouseId)
            .collect { devices ->
                // Untuk setiap device dari Firebase, kita perlu tahu statusnya (dari device_status node)
                // dan meng-update-nya ke cache lokal. Ini akan dilakukan dengan flow yang terpisah.
                // Untuk saat ini, kita akan simpan device tanpa status.
                val entities = devices.map { it.toEntity(greenhouseId, false) } // ✅ Perbaikan: Berikan greenhouseId dan status default
                deviceDao.insertAll(entities)
                emit(devices)
            }
    }.flowOn(dispatcherProvider.io)

    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    override suspend fun updateDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean) {
        firebaseDataSource.setDeviceStatus(greenhouseId, deviceId, status)
        deviceDao.updateStatus(deviceId, status)
    }

    // ✅ Perbaikan: Menambahkan greenhouseId sebagai parameter
    override suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any) {
        firebaseDataSource.setAutomaticSetting(greenhouseId, deviceId, setting, value)
    }
}
