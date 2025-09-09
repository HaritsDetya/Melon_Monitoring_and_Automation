package com.example.melon_monitoring_and_automation.data.remote

data class SensorReading (
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val ph: Double = 0.0,
    val ec: Double = 0.0,
    val waterLevel: String = "Normal",
    val timestamp: Long = System.currentTimeMillis()
)
