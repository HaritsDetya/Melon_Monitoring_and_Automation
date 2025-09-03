package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.data.local.AppDatabase
import com.example.melon_monitoring_and_automation.data.local.SensorHistoryEntity
import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydroponicRepository @Inject constructor (
    private val firebaseDataSource: FirebaseDataSource,
    private val localDatabase: AppDatabase
) {
    fun getRealtimeHydroponicData(userId: String, systemId: String): Flow<HydroponicData> {
        return firebaseDataSource.getRealtimeSensorData(userId, systemId).map { sensorReading ->
            localDatabase.sensorHistoryDao().insertSensorReading(
                SensorHistoryEntity(
                    temperature = sensorReading.temperature,
                    humidity = sensorReading.humidity,
                    ph = sensorReading.ph,
                    ec = sensorReading.ec,
                    waterLevel = sensorReading.waterLevel,
                    timestamp = sensorReading.timestamp
                )
            )
            HydroponicData(
                temperature = sensorReading.temperature,
                humidity = sensorReading.humidity,
                ph = sensorReading.ph,
                ec = sensorReading.ec,
                waterLevel = sensorReading.waterLevel,
                timestamp = sensorReading.timestamp
            )
        }
    }

    fun getHistoricalHydroponicData(starTime: Long, endTime: Long): Flow<List<HydroponicData>> {
        return localDatabase.sensorHistoryDao().getSensorReadingInDataRange(starTime, endTime)
            .map { entities ->
                entities.map { entity ->
                    HydroponicData(
                        temperature = entity.temperature,
                        humidity = entity.humidity,
                        ph = entity.ph,
                        ec = entity.ec,
                        waterLevel = entity.waterLevel,
                        timestamp = entity.timestamp
                    )
                }
            }
    }

    suspend fun setDeviceControl(userId: String, systemId: String, device: String, status: Boolean) {
        firebaseDataSource.setDeviceStatus(userId, systemId, device, status)
    }

    suspend fun setAutomaticSetting(userId: String, systemId: String, setting: String, value: Any) {
        firebaseDataSource.setAutomaticSetting(userId, systemId, setting, value)
    }

    suspend fun saveUserProfile(user: UserModel) {
        firebaseDataSource.saveUserProfile(user)
    }

    suspend fun saveSystemData(userId: String, systemId: String) {
        firebaseDataSource.saveSystemData(userId, systemId)
    }

    fun getUserProfile(uid: String): Flow<UserModel?> {
        return firebaseDataSource.getUserProfile(uid)
    }
}
