package com.example.melon_monitoring_and_automation.ui.components

import androidx.compose.ui.graphics.Color

// Di PasswordValidator.kt - TAMBAHKAN fungsi helper
object PasswordValidator {

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

    fun getStrengthColor(strength: String): Color {
        return when (strength) {
            "Kuat" -> Color(0xFF388E3C)
            "Sedang" -> Color(0xFFF57C00)
            else -> Color(0xFFD32F2F)
        }
    }

    fun isPasswordStrong(password: String): Boolean {
        if (password.length < 6) return false
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigits = password.any { it.isDigit() }
        return hasUpperCase && hasLowerCase && hasDigits
    }

    fun getPasswordRequirements(password: String): List<Pair<String, Boolean>> {
        return listOf(
            "Minimal 6 karakter" to (password.length >= 6),
            "Mengandung huruf besar" to password.any { it.isUpperCase() },
            "Mengandung huruf kecil" to password.any { it.isLowerCase() },
            "Mengandung angka" to password.any { it.isDigit() }
        )
    }

    // 🔹 TAMBAHKAN: Fungsi untuk mendapatkan strength score (0-100)
    fun getPasswordStrengthScore(password: String): Int {
        if (password.isEmpty()) return 0

        var score = 0

        // Length score
        score += when {
            password.length >= 12 -> 30
            password.length >= 8 -> 20
            password.length >= 6 -> 10
            else -> 0
        }

        // Character variety score
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 20
        if (password.any { it.isDigit() }) score += 20
        if (password.any { !it.isLetterOrDigit() }) score += 10

        return score.coerceAtMost(100)
    }
}
