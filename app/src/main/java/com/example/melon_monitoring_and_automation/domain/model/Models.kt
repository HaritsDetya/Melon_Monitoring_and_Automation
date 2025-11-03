package com.example.melon_monitoring_and_automation.domain.model

import kotlinx.serialization.Serializable

// --- 1. ENTITAS UTAMA ---

@Serializable
data class Greenhouse(
    val id: String,
    val owner_id: String,
    val name: String,
    val location: String,
    val created_at: String? = null
)

@Serializable
data class NewGreenhouse(
    val owner_id: String,
    val name: String,
    val location: String
)

@Serializable
data class GreenhouseMember(
    val greenhouse_id: String,
    val user_id: String,
    val role: String = "owner",
    val created_at: String? = null
)

@Serializable
data class UserProfile(
    val user_id: String,
    val username: String,
    val full_name: String? = null
)

// --- 2. ENTITAS DEVICE & KONTROL ---

@Serializable
data class Device(
    val id: String? = null,
    val greenhouse_id: String,
    val name: String,
    val type: String,
    val status: Boolean,
    val schedule: String? = null
)

// --- 3. ENTITAS TANAMAN DAN RIWAYAT ---

@Serializable
data class Plant(
    val id: String? = null,
    val greenhouse_id: String,
    val name: String,
    val plant_date: String,
    val variety: String
)

@Serializable
data class PlantHistory(
    val id: String,
    val plant_id: String,
    val greenhouse_id: String,
    val leaf_count: Int,
    val height_cm: Float,
    val notes: String? = null,
    val recorded_at: String? = null
)

// --- 4. ENTITAS SENSOR REALTIME ---

@Serializable
data class SensorReading(
    val id: String? = null,
    val greenhouse_id: String,
    val humidity: Float? = null,
    val ph: Float? = null,
    val temperature: Float? = null,
    val tds: Float? = null,
    val recorded_at: String? = null
)
