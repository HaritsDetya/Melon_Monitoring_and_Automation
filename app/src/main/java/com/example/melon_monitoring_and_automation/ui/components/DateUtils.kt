package com.example.melon_monitoring_and_automation.ui.components

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatToLocalTime(
        utcTimeString: String?,
        pattern: String = "dd MMM yyyy, HH:mm"
    ): String {
        if (utcTimeString.isNullOrEmpty()) return "-"

        return try {
            // 1. Parse string UTC dari Supabase ke Instant
            val instant = Instant.parse(utcTimeString)

            // 2. Konversi ke Zona Waktu HP Pengguna (Otomatis deteksi WIB/WITA/WIT)
            val zonedDateTime = instant.atZone(ZoneId.systemDefault())

            // 3. Format menjadi tulisan yang mudah dibaca (Bahasa Indonesia)
            val formatter = DateTimeFormatter.ofPattern(pattern, Locale("id", "ID"))

            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            utcTimeString // Jika gagal parse, kembalikan string aslinya
        }
    }
}