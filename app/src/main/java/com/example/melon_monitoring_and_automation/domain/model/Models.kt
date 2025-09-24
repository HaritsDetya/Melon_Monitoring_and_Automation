package com.example.melon_monitoring_and_automation.domain.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String? = "",
    val greenhouses: Map<String, Boolean>? = null
)

data class Greenhouse(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val owner_id: String? = "",
    val devices: Map<String, Any?>? = null,
    val plants: Map<String, Any?>? = null
)

data class Device(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val status: Boolean = false,
    val config: Map<String, Any>? = null,
    val greenhouseId: String = ""
)

data class DeviceStatus(
    val status: Boolean = false
)

data class Plant(
    val id: String = "",
    val name: String = "",
    val type: String = "",
    val planted_at: String = ""
)

data class PlantHistory(
    val height: Double = 0.0,
    val leaf_count: Int = 0,
    val notes: String = ""
)

data class SensorReading(
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val ph: Double = 0.0,
    val ec: Double = 0.0,
    val waterLevel: String? = null,
    val light: Double = 0.0,
    val recorded_at: Long? = null
)
