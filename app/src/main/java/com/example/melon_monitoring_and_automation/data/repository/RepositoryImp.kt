package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.data.local.DeviceDao
import com.example.melon_monitoring_and_automation.data.local.GreenhouseDao
import com.example.melon_monitoring_and_automation.data.local.PlantDao
import com.example.melon_monitoring_and_automation.data.local.SensorReadingDao
import com.example.melon_monitoring_and_automation.data.local.UserProfileDao
import com.example.melon_monitoring_and_automation.data.local.toEntity
import com.example.melon_monitoring_and_automation.data.local.toModel
import com.example.melon_monitoring_and_automation.data.remote.SupabaseDataSource
import com.example.melon_monitoring_and_automation.di.DispatcherProvider
import com.example.melon_monitoring_and_automation.domain.model.Device
import com.example.melon_monitoring_and_automation.domain.model.Greenhouse
import com.example.melon_monitoring_and_automation.domain.model.GreenhouseMember
import com.example.melon_monitoring_and_automation.domain.model.NewGreenhouse
import com.example.melon_monitoring_and_automation.domain.model.Plant
import com.example.melon_monitoring_and_automation.domain.model.PlantHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydroponicRepositoryImpl @Inject constructor(
    private val supabaseDataSource: SupabaseDataSource,
    private val userDao: UserProfileDao,
    private val greenhouseDao: GreenhouseDao,
    private val sensorHistoryDao: SensorReadingDao,
    private val deviceDao: DeviceDao,
    private val plantDao: PlantDao,
    private val dispatcherProvider: DispatcherProvider
) : HydroponicRepository {

    // ===================== USER =====================
    override suspend fun getUserProfile(uid: String): UserProfile? {
        return supabaseDataSource.getUserProfileByUserId(uid)
    }

    override suspend fun saveUserProfile(userProfile: UserProfile) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.saveUserProfile(userProfile)
            userDao.insert(userProfile.toEntity())
        }
    }

    override suspend fun registerUserAndGreenhouse(userProfile: UserProfile, newGreenhouse: NewGreenhouse) {
        supabaseDataSource.registerUserAndGreenhouse(userProfile, newGreenhouse)
    }

    // ===================== GREENHOUSE =====================
    override fun getUserGreenhouses(ownerId: String): Flow<List<Greenhouse>> = flow {
        // 1. Emit data dari cache lokal (jika ada)
        emit(greenhouseDao.getGreenhousesByOwnerId(ownerId).map { it.toModel() })

        // 2. Ambil data terbaru dari Supabase
        val remoteGreenhouses = supabaseDataSource.getUsersGreenhouses(ownerId)

        // 3. Update cache lokal dengan data terbaru
        withContext(dispatcherProvider.io) {
            greenhouseDao.clearAll() // Atau update satu per satu
            greenhouseDao.insertAll(remoteGreenhouses.map { it.toEntity() })
        }

        // 4. Emit data terbaru dari Supabase
        emit(remoteGreenhouses)
    }.flowOn(dispatcherProvider.io)

    override suspend fun saveGreenhouse(greenhouse: Greenhouse) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.saveGreenhouse(greenhouse)
            greenhouseDao.insert(greenhouse.toEntity())
        }
    }

    override suspend fun addGreenhouse(newGreenhouse: NewGreenhouse) {
        supabaseDataSource.addGreenhouse(newGreenhouse)
    }

    // Fungsi ini tidak lagi relevan di Supabase karena relasi owner sudah di handle
    override suspend fun addGreenhouseToUser(uid: String, greenhouseId: String) {
        // Implementasi ini tidak diperlukan jika relasi dihandle oleh `owner_id`
    }

    override suspend fun addGreenhouseMember(member: GreenhouseMember) {
        supabaseDataSource.addGreenhouseMember(member)
    }

    // ===================== SENSOR =====================
    override fun getLatestSensorData(greenhouseId: String): Flow<SensorReading?> =
        supabaseDataSource.getLatestSensorData(greenhouseId)
            .onEach { reading ->
                if (reading != null) {
                    withContext(dispatcherProvider.io) {
                        sensorHistoryDao.insert(reading.toEntity())
                    }
                }
            }
            .flowOn(dispatcherProvider.io)

    override suspend fun getHistoricalSensorData(greenhouseId: String): List<SensorReading> {
        val remoteData = supabaseDataSource.getHistoricalSensorDataFromSupabase(greenhouseId)
        withContext(dispatcherProvider.io) {
            sensorHistoryDao.clearAll()
            sensorHistoryDao.insertAll(remoteData.map { it.toEntity() })
        }
        return remoteData
    }

    override suspend fun saveSensorReading(sensorReading: SensorReading) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.saveSensorReading(sensorReading)
            sensorHistoryDao.insert(sensorReading.toEntity())
        }
    }

    override suspend fun addSensorReading(sensorReading: SensorReading) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.saveSensorReading(sensorReading)
        }
    }

    override suspend fun editSensorReading(readingId: String, updatedReading: SensorReading) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.editSensorReading(readingId, updatedReading)
        }
    }

    override suspend fun deleteSensorReading(readingId: String) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.deleteSensorReading(readingId)
        }
    }

    // ===================== DEVICE =====================
    override fun getGreenhouseDevices(greenhouseId: String): Flow<List<Device>> =
        supabaseDataSource.getGreenhouseDevices(greenhouseId)
            .onEach { devices ->
                withContext(dispatcherProvider.io) {
                    deviceDao.insertAll(devices.map { it.toEntity() })
                }
            }
            .flowOn(dispatcherProvider.io)

    override suspend fun updateDeviceStatus(deviceId: String, status: Boolean) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.setDeviceStatus(deviceId, status)
            deviceDao.updateStatus(deviceId, status)
        }
    }

    override suspend fun addDevice(device: Device) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.addDevice(device)
            deviceDao.insert(device.toEntity())
        }
    }


    override suspend fun editDevice(readingId: String, device: Device) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.editDevice(readingId, device)
        }
    }

    override suspend fun deleteDevice(deviceId: String) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.deleteDevice(deviceId)
        }
    }

    override suspend fun setAutomaticSetting(deviceId: String, setting: String, value: Any) {
        supabaseDataSource.setAutomaticSetting(deviceId, setting, value)
    }

    // ===================== PLANT =====================
    override fun getGreenhousePlants(greenhouseId: String): Flow<List<Plant>> =
        supabaseDataSource.getPlants(greenhouseId)
            .onEach { plants ->
                withContext(dispatcherProvider.io) {
                    plantDao.insertAll(plants.map { it.toEntity() })
                }
            }
            .flowOn(dispatcherProvider.io)

    override suspend fun addPlant(plant: Plant) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.addPlant(plant)
            plantDao.insert(plant.toEntity())
        }
    }

    override suspend fun updatePlant(readingId: String, updatedPlant: Plant) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.updatePlant(readingId, updatedPlant)
        }
    }

    override suspend fun deletePlant(plantId: String) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.deletePlant(plantId)
        }
    }

    override suspend fun addPlantHistory(plantHistory: PlantHistory) {
        withContext(dispatcherProvider.io) {
            supabaseDataSource.addPlantHistory(plantHistory)
        }
    }

    override suspend fun clearLocalCache() {
        withContext(dispatcherProvider.io) {
            userDao.clearAll()
            greenhouseDao.clearAll()
            sensorHistoryDao.clearAll()
            deviceDao.clearAll()
            plantDao.clearAll()
        }
    }
}
