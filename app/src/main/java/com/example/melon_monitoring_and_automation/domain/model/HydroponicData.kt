package com.example.melon_monitoring_and_automation.domain.model

data class HydroponicData(
    val temperature: Double,
    val humidity: Double,
    val ph: Double,
    val ec: Double,
    val waterLevel: String,
    val timestamp: Long
)
