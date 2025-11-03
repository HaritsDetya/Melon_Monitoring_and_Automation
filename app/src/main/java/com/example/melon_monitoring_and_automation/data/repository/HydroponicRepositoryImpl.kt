package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.di.DispatcherProvider
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

//@Singleton
//class HydroponicRepositoryImpl @Inject constructor(
//    private val firebaseDataSource: FirebaseDataSource,
//    private val userDao: UserDao,
//    private val greenhouseDao: GreenhouseDao,
//    private val sensorHistoryDao: SensorHistoryDao,
//    private val deviceDao: DeviceDao,
//    private val plantDao: PlantDao,
//    private val dispatcherProvider: DispatcherProvider
//) : HydroponicRepository {
//
//    // USER
//    override fun getUserProfile(uid: String): Flow<User?> =
//        firebaseDataSource.getUserProfile(uid)
//
//    override suspend fun saveUserProfile(user: User) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.saveUserProfile(user)
//            userDao.insert(user.toEntity())
//        }
//    }
//
//    override suspend fun clearLocalCache() {
//        withContext(dispatcherProvider.io) {
//            userDao.clearAll()
//            greenhouseDao.clearAll()
//            sensorHistoryDao.clearAll()
//            deviceDao.clearAll()
//            plantDao.clearAll()
//        }
//    }
//
//    // GREENHOUSE
//    override fun getUserGreenhouses(uid: String): Flow<List<Greenhouse>> = flow {
//        emit(greenhouseDao.getAllGreenhouses().map { it.toModel() })
//
//        firebaseDataSource.getUserProfile(uid)
//            .flatMapConcat { user ->
//                val greenhouseIds = user?.greenhouses?.keys?.toList() ?: emptyList()
//                firebaseDataSource.getUsersGreenhouses(greenhouseIds)
//            }
//            .catch { emit(emptyList()) } // Handle error
//            .collect { greenhouses ->
//                withContext(dispatcherProvider.io) {
//                    // This is the crucial part: clear old data to avoid inconsistency
//                    greenhouseDao.clearAll()
//                    greenhouseDao.insertAll(greenhouses.map { it.toEntity() })
//                }
//                emit(greenhouses)
//            }
//    }.flowOn(dispatcherProvider.io)
//
//    override suspend fun saveGreenhouse(greenhouseId: String, greenhouse: Greenhouse) {
//        firebaseDataSource.saveGreenhouse(greenhouseId, greenhouse)
//        greenhouseDao.insert(greenhouse.toEntity())
//    }
//
//    override suspend fun addGreenhouseToUser(uid: String, greenhouseId: String) {
//        firebaseDataSource.addGreenhouseToUser(uid, greenhouseId)
//    }
//
//    // SENSOR
//    override fun getLatestSensorData(greenhouseId: String): Flow<Pair<String, SensorReading>?> =
//        firebaseDataSource.getRealtimeSensorData(greenhouseId)
//            .onEach { pair ->
//                if (pair != null) {
//                    val (key, reading) = pair
//                    val timestamp = reading.recorded_at ?: key.toLongOrNull() ?: System.currentTimeMillis()
//                    withContext(dispatcherProvider.io) {
//                        sensorHistoryDao.insertSensorReading(
//                            reading.toEntity(greenhouseId, timestamp)
//                        )
//                    }
//                }
//            }
//            .flowOn(dispatcherProvider.io)
//
//    override fun getHistoricalSensorData(greenhouseId: String): Flow<List<Pair<Long, SensorReading>>> = flow {
//        firebaseDataSource.getHistoricalSensorDataFromFirebase(greenhouseId)
//            .collect { allData ->
//                withContext(dispatcherProvider.io) {
//                    sensorHistoryDao.clearAll()
//                    sensorHistoryDao.insertAll(allData.map { it.second.toEntity(greenhouseId, it.first) })
//                }
//                emit(allData)
//            }
//    }.flowOn(dispatcherProvider.io)
//
//    override suspend fun saveSensorReading(greenhouseId: String, sensorReading: SensorReading) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.saveSensorReading(greenhouseId, sensorReading)
//            sensorHistoryDao.insertSensorReading(sensorReading.toEntity(greenhouseId, sensorReading.recorded_at ?: System.currentTimeMillis()))
//        }
//    }
//
//    override suspend fun addSensorReading(greenhouseId: String, sensorReading: SensorReading) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.addSensorReading(greenhouseId, sensorReading)
//        }
//    }
//
//    override suspend fun editSensorReading(greenhouseId: String, readingId: String, updatedReading: SensorReading) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.editSensorReading(greenhouseId, readingId, updatedReading)
//        }
//    }
//
//    override suspend fun deleteSensorReading(greenhouseId: String, readingId: String) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.deleteSensorReading(greenhouseId, readingId)
//        }
//    }
//
//    // DEVICE
//
//    override fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> = flow {
//        val cached = deviceDao.getDevicesByGreenhouse(greenhouseId).map { it.toModel() }
//        emit(cached)
//
//        firebaseDataSource.getGreenhouseDevices(greenhouseId)
//            .collect { devices ->
//                val entities = devices.map { it.toEntity(greenhouseId, it.status) }
//                deviceDao.insertAll(entities)
//                emit(devices)
//            }
//    }.flowOn(dispatcherProvider.io)
//
//    override suspend fun updateDeviceStatus(greenhouseId: String, deviceId: String, status: Boolean) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.setDeviceStatus(greenhouseId, deviceId, status)
//            deviceDao.updateStatus(deviceId, status)
//        }
//    }
//
//    override suspend fun addDevice(greenhouseId: String, device: Device) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.addDevice(greenhouseId, device)
//        }
//    }
//
//    override suspend fun editDevice(greenhouseId: String, deviceId: String, device: Device) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.editDevice(greenhouseId, device)
//        }
//    }
//
//    override suspend fun deleteDevice(greenhouseId: String, deviceId: String) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.deleteDevice(greenhouseId, deviceId)
//        }
//    }
//
//    // Plant
//
//    override fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>> = flow {
//        val cachedPlants = plantDao.getPlantsByGreenhouse(greenhouseId).map { it.toModel() }
//        emit(cachedPlants)
//
//        firebaseDataSource.getGreenhousePlants(greenhouseId)
//            .collect { plants ->
//                withContext(dispatcherProvider.io) {
//                    plantDao.insertAll(plants.map { it.toEntity(greenhouseId) })
//                }
//                emit(plants)
//            }
//    }.flowOn(dispatcherProvider.io)
//
//    override suspend fun addPlant(greenhouseId: String, plant: Plant) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.addPlant(greenhouseId, plant)
//            plantDao.insert(plant.toEntity(greenhouseId))
//        }
//    }
//
//    override suspend fun updatePlant(greenhouseId: String, plantId: String, updatePlant: Plant) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.updatePlant(greenhouseId, plantId, updatePlant)
//        }
//    }
//
//    override suspend fun deletePlant(greenhouseId: String, plantId: String) {
//        withContext(dispatcherProvider.io) {
//            firebaseDataSource.deletePlant(greenhouseId, plantId)
//        }
//    }
//
//    override suspend fun setAutomaticSetting(greenhouseId: String, deviceId: String, setting: String, value: Any) {
//        firebaseDataSource.setAutomaticSetting(greenhouseId, deviceId, setting, value)
//    }
//}
