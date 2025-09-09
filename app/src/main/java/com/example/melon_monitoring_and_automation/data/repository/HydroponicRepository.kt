package com.example.melon_monitoring_and_automation.data.repository

import com.example.melon_monitoring_and_automation.data.local.AppDatabase
import com.example.melon_monitoring_and_automation.data.local.SensorHistoryEntity
import com.example.melon_monitoring_and_automation.data.local.UserModel
import com.example.melon_monitoring_and_automation.data.remote.FirebaseDataSource
import com.example.melon_monitoring_and_automation.data.remote.SensorReading
import com.example.melon_monitoring_and_automation.domain.model.ControlData
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydroponicRepository @Inject constructor (
    private val firebaseDataSource: FirebaseDataSource,
    private val localDatabase: AppDatabase
) {
    fun getRealtimeHydroponicData(userId: String, systemId: String): Flow<HydroponicData> {
        return firebaseDataSource.getRealtimeSensorData(userId, systemId)
            .onEach { sensorReading ->
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
            }
            .map { sensorReading ->
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

    fun getRealtimeControlData(userId: String, systemId: String): Flow<ControlData> {
        return firebaseDataSource.getRealtimeControlData(userId, systemId)
    }

    fun getHistoricalHydroponicDataFromFirebase(userId: String, systemId: String, startTime: Long, endTime: Long): Flow<List<HydroponicData>> {
        return firebaseDataSource.getHistoricalSensorData(userId, systemId, startTime, endTime)
            .map { readings ->
                readings.map { reading ->
                    HydroponicData(
                        temperature = reading.temperature,
                        humidity = reading.humidity,
                        ph = reading.ph,
                        ec = reading.ec,
                        waterLevel = reading.waterLevel,
                        timestamp = reading.timestamp
                    )
                }
            }
    }

    fun getHistoricalHydroponicDataFromLocal(startTime: Long, endTime: Long): Flow<List<HydroponicData>> {
        return localDatabase.sensorHistoryDao().getSensorReadingInDataRange(startTime, endTime)
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

    suspend fun syncHistoricalDataToLocal(data: List<HydroponicData>) {
        data.forEach { hydroponicData ->
            localDatabase.sensorHistoryDao().insertSensorReading(
                SensorHistoryEntity(
                    temperature = hydroponicData.temperature,
                    humidity = hydroponicData.humidity,
                    ph = hydroponicData.ph,
                    ec = hydroponicData.ec,
                    waterLevel = hydroponicData.waterLevel,
                    timestamp = hydroponicData.timestamp
                )
            )
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

    suspend fun saveSensorReadingHistory(userId: String, systemId: String, data: HydroponicData) {
        val sensorReading = SensorReading(
            temperature = data.temperature,
            humidity = data.humidity,
            ph = data.ph,
            ec = data.ec,
            waterLevel = data.waterLevel,
            timestamp = data.timestamp
        )
        firebaseDataSource.saveSensorReadingHistory(userId, systemId, sensorReading)
    }

    fun getUserProfile(uid: String): Flow<UserModel?> {
        return firebaseDataSource.getUserProfile(uid)
    }
}
