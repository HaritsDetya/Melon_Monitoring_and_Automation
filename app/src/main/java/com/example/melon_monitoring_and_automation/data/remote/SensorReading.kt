package com.example.melon_monitoring_and_automation.data.remote

data class SensorReading (
    val temperature: Double = 0.0, //Suhu
    val humidity: Double = 0.0, // Kelembapan
    val ph: Double = 0.0, // Kadar pH air
    val ec: Double = 0.0, // Electrical Conductivity (konsentrasi nutrisi)
    val waterLevel: String = "Normal", // Level air
    val timestamp: Long = System.currentTimeMillis() // Waktu pembacaan
)