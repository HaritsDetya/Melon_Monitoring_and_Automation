package com.example.melon_monitoring_and_automation.domain.usecase

import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.ControlData
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class GetRealtimeHydroponicDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(userId: String, systemId: String): Flow<HydroponicData> {
        return repository.getRealtimeHydroponicData(userId, systemId)
    }
}

class GetRealtimeControlDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(userId: String, systemId: String) : Flow<ControlData> {
        return repository.getRealtimeControlData(userId, systemId)
    }
}

class GetHistoricalHydroponicDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(userId: String, systemId: String, startTime: Long, endTime: Long): Flow<List<HydroponicData>> {
        return repository.getHistoricalHydroponicDataFromLocal(startTime, endTime)
            .onEach {
                val firebaseData = repository.getHistoricalHydroponicDataFromFirebase(userId, systemId, startTime, endTime).first()
                repository.syncHistoricalDataToLocal(firebaseData)
            }
    }
}

class SetDeviceControlUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(userId: String, systemId: String, device: String, status: Boolean) {
        repository.setDeviceControl(userId, systemId, device, status)
    }
}

class SetAutomaticSettingUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    suspend operator fun invoke(userId: String, systemId: String, setting: String, value: Any) {
        repository.setAutomaticSetting(userId, systemId, setting, value)
    }
}
