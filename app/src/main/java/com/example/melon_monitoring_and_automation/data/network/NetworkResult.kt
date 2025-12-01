/**
 * NETWORK RESULT SEALED CLASS
 *
 * Tujuan:
 * - Menyediakan type-safe result handling untuk network operations
 * - Mengemas success data dan error information secara konsisten
 * - Memfasilitasi clean error handling di UI layer
 *
 * Pattern:
 * - Success: Mengandung data hasil network call
 * - Error: Mengandung error message dan optional error code
 *
 * @author Your Name
 * @since Version 1.0
 */

package com.example.melon_monitoring_and_automation.data.network

/**
 * NETWORK RESULT SEALED CLASS
 * Representasi hasil dari network operation
 *
 * @param T Type data yang di-return pada success case
 */
sealed class NetworkResult<out T> {
    /**
     * SUCCESS CASE
     * Mengandung data hasil network call yang successful
     *
     * @param data Data hasil network operation
     */
    data class Success<out T>(val data: T) : NetworkResult<T>()

    /**
     * ERROR CASE
     * Mengandung informasi error ketika network operation gagal
     *
     * @param message Error message untuk display
     * @param code Optional error code untuk debugging
     */
    data class Error(val message: String, val code: Int? = null) : NetworkResult<Nothing>()
}
