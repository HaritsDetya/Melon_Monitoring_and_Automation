package com.example.melon_monitoring_and_automation.ui.components

import androidx.compose.ui.graphics.Color

/**
 * PASSWORD VALIDATOR OBJECT
 *
 * Tujuan:
 * - Menyediakan utility functions untuk validasi kekuatan password
 * - Memberikan feedback visual tentang keamanan password
 * - Menentukan kriteria password yang acceptable untuk sistem
 *
 * Fitur:
 * - Strength assessment berdasarkan multiple criteria
 * - Color coding untuk visual feedback
 * - Requirements checklist
 * - Scoring system 0-100
 *
 * @author Your Name
 * @since Version 1.0
 */

object PasswordValidator {

    /**
     * Menentukan kekuatan password berdasarkan kriteria keamanan.
     *
     * Kriteria penilaian:
     * - Panjang minimal 6 karakter
     * - Mengandung huruf besar
     * - Mengandung huruf kecil
     * - Mengandung angka
     * - Mengandung karakter spesial
     *
     * @param password Password yang akan divalidasi
     * @return String kekuatan: "Lemah", "Sedang", atau "Kuat"
     */
    fun getPasswordStrength(password: String): String {
        if (password.length < 6) return "Lemah"

        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigits = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }

        val strength = listOf(hasUpperCase, hasLowerCase, hasDigits, hasSpecial).count { it }

        return when {
            strength >= 3 && password.length >= 8 -> "Kuat"
            strength >= 2 -> "Sedang"
            else -> "Lemah"
        }
    }

    /**
     * Mengembalikan warna yang merepresentasikan kekuatan password.
     *
     * Warna yang digunakan:
     * - Kuat: Hijau (#388E3C)
     * - Sedang: Orange (#F57C00)
     * - Lemah: Merah (#D32F2F)
     *
     * @param strength Kekuatan password dari getPasswordStrength
     * @return Color yang sesuai dengan kekuatan
     */
    fun getStrengthColor(strength: String): Color {
        return when (strength) {
            "Kuat" -> Color(0xFF388E3C)
            "Sedang" -> Color(0xFFF57C00)
            else -> Color(0xFFD32F2F)
        }
    }

    /**
     * Memeriksa apakah password memenuhi kriteria keamanan minimal.
     *
     * Kriteria minimal:
     * - Panjang ≥ 6 karakter
     * - Mengandung huruf besar dan kecil
     * - Mengandung angka
     *
     * @param password Password yang akan divalidasi
     * @return true jika password dianggap kuat secara minimal
     */
    fun isPasswordStrong(password: String): Boolean {
        if (password.length < 6) return false
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigits = password.any { it.isDigit() }
        return hasUpperCase && hasLowerCase && hasDigits
    }

    /**
     * Mendapatkan list requirement password dan status pemenuhannya.
     *
     * Requirements yang diperiksa:
     * - Minimal 6 karakter
     * - Mengandung huruf besar
     * - Mengandung huruf kecil
     * - Mengandung angka
     *
     * @param password Password yang akan divalidasi
     * @return List of Pair<String, Boolean> (requirement, status)
     */
    fun getPasswordRequirements(password: String): List<Pair<String, Boolean>> {
        return listOf(
            "Minimal 6 karakter" to (password.length >= 6),
            "Mengandung huruf besar" to password.any { it.isUpperCase() },
            "Mengandung huruf kecil" to password.any { it.isLowerCase() },
            "Mengandung angka" to password.any { it.isDigit() }
        )
    }

    /**
     * Menghitung score kekuatan password dalam persentase (0-100).
     *
     * Komponen penilaian:
     * - Panjang: hingga 30 points (12+ karakter = 30 points)
     * - Huruf besar: 20 points
     * - Huruf kecil: 20 points
     * - Angka: 20 points
     * - Karakter spesial: 10 points
     *
     * @param password Password yang akan dinilai
     * @return Integer score antara 0-100
     */
    fun getPasswordStrengthScore(password: String): Int {
        if (password.isEmpty()) return 0

        var score = 0

        // Score berdasarkan panjang
        score += when {
            password.length >= 12 -> 30
            password.length >= 8 -> 20
            password.length >= 6 -> 10
            else -> 0
        }

        // Score berdasarkan variasi karakter
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 20
        if (password.any { it.isDigit() }) score += 20
        if (password.any { !it.isLetterOrDigit() }) score += 10

        return score.coerceAtMost(100)
    }
}
