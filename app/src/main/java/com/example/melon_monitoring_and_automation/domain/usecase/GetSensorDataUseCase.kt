package com.example.melon_monitoring_and_automation.domain.usecase

import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.HydroponicData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRealtimeHydroponicDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(userId: String, systemId: String): Flow<HydroponicData> {
        return repository.getRealtimeHydroponicData(userId, systemId)
    }
}

class GetHistoricalHydroponicDataUseCase @Inject constructor(
    private val repository: HydroponicRepository
) {
    operator fun invoke(startTime: Long, endTime: Long): Flow<List<HydroponicData>> {
        return repository.getHistoricalHydroponicData(startTime, endTime)
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
