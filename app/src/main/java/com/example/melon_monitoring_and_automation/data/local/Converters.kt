package com.example.melon_monitoring_and_automation.data.local

import androidx.room.TypeConverter
import kotlinx.datetime.Instant

class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toInstant(value: String?): Instant? {
        return value?.let { Instant.parse(it) }
    }

    // Jika perlu konverter untuk enum atau tipe data kompleks lainnya
}