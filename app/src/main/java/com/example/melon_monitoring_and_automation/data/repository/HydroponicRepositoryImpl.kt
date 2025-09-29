package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.data.local.DeviceDao
import com.example.melon_monitoring_and_automation.data.local.GreenhouseDao
import com.example.melon_monitoring_and_automation.data.local.SensorHistoryDao
import com.example.melon_monitoring_and_automation.data.local.UserDao
import com.example.melon_monitoring_and_automation.data.local.toEntity
import com.example.melon_monitoring_and_automation.data.local.toModel
import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
import com.example.melon_monitoring_and_automation.di.DispatcherProvider
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
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
                    val timestamp = reading.recorded_at ?: key.substring(1, 14).toLongOrNull() ?: System.currentTimeMillis()

                    sensorHistoryDao.insertSensorReading(
                        reading.toEntity(greenhouseId, timestamp)
                    )
                }
            }
            .flowOn(dispatcherProvider.io)

    override fun getHistoricalSensorData(
        greenhouseId: String
    ): Flow<List<Pair<Long, SensorReading>>> = flow {
        firebaseDataSource.getHistoricalSensorDataFromFirebase(greenhouseId)
            .collect { allData ->
                emit(allData)
            }
    }.flowOn(dispatcherProvider.io)

    override suspend fun saveSensorReading(greenhouseId: String, sensorReading: SensorReading) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.saveSensorReading(greenhouseId, sensorReading)
            sensorHistoryDao.insertSensorReading(sensorReading.toEntity(greenhouseId, sensorReading.recorded_at ?: System.currentTimeMillis()))
        }
    }


    // DEVICE

    override fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> = flow {
        val cached = deviceDao.getDevicesByGreenhouse(greenhouseId).map { it.toModel() }
        emit(cached)

        firebaseDataSource.getGreenhouseDevices(greenhouseId)
            .collect { devices ->
                val entities = devices.map { it.toEntity(greenhouseId, it.status) }
                deviceDao.insertAll(entities)
                emit(devices)
            }
    }.flowOn(dispatcherProvider.io)

    override suspend fun updateDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.setDeviceStatus(greenhouseId, deviceId, status)
            deviceDao.updateStatus(deviceId, status)
        }
    }

    override suspend fun addDevice(greenhouseId: String, device: Device) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.addDevice(greenhouseId, device)
        }
    }

    override suspend fun editDevice(greenhouseId: String, device: Device) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.editDevice(greenhouseId, device)
        }
    }

    override suspend fun deleteDevice(greenhouseId: String, deviceId: String) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.deleteDevice(greenhouseId, deviceId)
        }
    }

    // Plant

    override fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>> = flow {
        val cached = emptyList<Plant>()
        emit(cached)

        firebaseDataSource.getGreenhousePlants(greenhouseId)
            .collect { plants ->
                emit(plants)
            }
    }.flowOn(dispatcherProvider.io)

    override suspend fun addPlant(greenhouseId: String, plant: Plant) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.addPlant(greenhouseId, plant)
        }
    }

    override suspend fun editPlant(greenhouseId: String, plant: Plant) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.editPlant(greenhouseId, plant)
        }
    }

    override suspend fun deletePlant(greenhouseId: String, plantId: String) {
        withContext(dispatcherProvider.io) {
            firebaseDataSource.deletePlant(greenhouseId, plantId)
        }
    }

    override suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any) {
        firebaseDataSource.setAutomaticSetting(greenhouseId, deviceId, setting, value)
    }
}
