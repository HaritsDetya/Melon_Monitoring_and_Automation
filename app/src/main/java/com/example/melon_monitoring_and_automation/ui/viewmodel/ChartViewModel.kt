package com.example.melon_monitoring_and_automation.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.melon_monitoring_and_automation.data.network.NetworkResult
import com.example.melon_monitoring_and_automation.data.repository.HydroponicRepository
import com.example.melon_monitoring_and_automation.domain.model.ChartConfig
import com.example.melon_monitoring_and_automation.domain.model.ChartDataPoint
import com.example.melon_monitoring_and_automation.domain.model.ChartType
import com.example.melon_monitoring_and_automation.domain.model.SensorHistory
import com.example.melon_monitoring_and_automation.domain.model.SensorType
import com.example.melon_monitoring_and_automation.domain.model.TimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    private val _chartConfig = MutableStateFlow(ChartConfig())
    val chartConfig: StateFlow<ChartConfig> = _chartConfig.asStateFlow()

    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Load chart data dengan error handling yang robust
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadChartData(greenhouseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                println("🔹 [CHART] Loading chart data for greenhouse: $greenhouseId")

                val config = _chartConfig.value
                val hours = when (config.timeRange) {
                    TimeRange.HOURS_24 -> 24
                    TimeRange.DAYS_7 -> 24 * 7
                    TimeRange.DAYS_30 -> 24 * 30
                }

                // Get sensor history dengan safe call
                val sensorHistory = when (val result = repository.getSensorHistory(
                    greenhouseId = greenhouseId,
                    sensorType = config.selectedSensorType,
                    hours = hours
                )) {
                    is NetworkResult.Success -> {
                        println("🔹 [CHART] Sensor history loaded: ${result.data?.size ?: 0} records")
                        result.data ?: emptyList()
                    }
                    is NetworkResult.Error -> {
                        println("🔹 [CHART] Error loading sensor history: ${result.message}")
                        _errorMessage.value = "Gagal memuat data sensor: ${result.message}"
                        emptyList()
                    }
                    else -> emptyList()
                }

                // Transform data dengan exception handling
                val chartDataPoints = try {
                    transformToChartDataPoints(sensorHistory, config.timeRange)
                } catch (e: Exception) {
                    println("🔹 [CHART] Error transforming data: ${e.message}")
                    _errorMessage.value = "Gagal memproses data chart: ${e.message}"
                    emptyList()
                }

                _chartData.value = chartDataPoints
                println("🔹 [CHART] Chart data transformation completed: ${chartDataPoints.size} points")

            } catch (e: Exception) {
                println("🔹 [CHART] Unexpected error in loadChartData: ${e.message}")
                _errorMessage.value = "Terjadi kesalahan: ${e.message}"
                _chartData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🔹 SIMPLIFIED DATA TRANSFORMATION - FOCUS ON STABILITY
    private fun transformToChartDataPoints(
        sensorHistory: List<SensorHistory>,
        timeRange: TimeRange
    ): List<ChartDataPoint> {
        if (sensorHistory.isEmpty()) return emptyList()

        return try {
            // Simple transformation - just use the data as is
            sensorHistory.mapIndexed { index, history ->
                ChartDataPoint(
                    x = index.toFloat(),
                    y = history.value.toFloat(),
                    label = getSimpleLabel(index, sensorHistory.size),
                    rawValue = history.value
                )
            }
        } catch (e: Exception) {
            println("🔹 [CHART] Error in simple transformation: ${e.message}")
            emptyList()
        }
    }

    private fun getSimpleLabel(index: Int, totalSize: Int): String {
        return when (_chartConfig.value.timeRange) {
            TimeRange.HOURS_24 -> "${index + 1}"
            TimeRange.DAYS_7 -> "H${index + 1}"
            TimeRange.DAYS_30 -> "T${index + 1}"
        }
    }

    private fun transformTo24HourDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Take last 24 points or use all if less than 24
        val recentData = if (data.size > 24) data.takeLast(24) else data

        return recentData.mapIndexed { index, history ->
            ChartDataPoint(
                x = index.toFloat(),
                y = history.value.toFloat(),
                label = getTimeLabel(history.recordedAt, TimeRange.HOURS_24),
                rawValue = history.value
            )
        }
    }

    private fun transformTo7DayDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Group by day and take averages - FIXED: handle empty groups
        val dailyAverages = data.groupBy { entry ->
            // Simple grouping by taking first 10 characters (YYYY-MM-DD)
            if (entry.recordedAt.length >= 10) entry.recordedAt.substring(0, 10) else "unknown"
        }.mapValues { (_, entries) ->
            entries.map { it.value }.average()
        }

        return dailyAverages.entries.take(7).mapIndexed { index, (date, value) ->
            ChartDataPoint(
                x = index.toFloat(),
                y = value.toFloat(),
                label = if (date.length >= 10) date.substring(8, 10) else "??", // Day only
                rawValue = value
            )
        }
    }

    private fun transformTo30DayDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Similar to 7 days but take 30 entries - FIXED: handle empty groups
        val dailyAverages = data.groupBy { entry ->
            if (entry.recordedAt.length >= 10) entry.recordedAt.substring(0, 10) else "unknown"
        }.mapValues { (_, entries) ->
            entries.map { it.value }.average()
        }

        return dailyAverages.entries.take(30).mapIndexed { index, (date, value) ->
            ChartDataPoint(
                x = index.toFloat(),
                y = value.toFloat(),
                label = if (date.length >= 10) date.substring(5, 10) else "??/??", // MM-DD format
                rawValue = value
            )
        }
    }

    private fun getTimeLabel(timestamp: String, timeRange: TimeRange): String {
        return try {
            when (timeRange) {
                TimeRange.HOURS_24 -> {
                    // Format: HH:mm
                    if (timestamp.length >= 16) timestamp.substring(11, 16) else "${timestamp.length}"
                }
                TimeRange.DAYS_7 -> {
                    // Format: DD
                    if (timestamp.length >= 10) timestamp.substring(8, 10) else "??"
                }
                TimeRange.DAYS_30 -> {
                    // Format: MM-DD
                    if (timestamp.length >= 10) timestamp.substring(5, 10) else "??/??"
                }
            }
        } catch (e: Exception) {
            "??"
        }
    }

    // Update configuration methods
    fun updateSensorType(sensorType: SensorType) {
        _chartConfig.value = _chartConfig.value.copy(selectedSensorType = sensorType)
    }

    fun updateTimeRange(timeRange: TimeRange) {
        _chartConfig.value = _chartConfig.value.copy(timeRange = timeRange)
    }

    fun updateChartType(chartType: ChartType) {
        _chartConfig.value = _chartConfig.value.copy(chartType = chartType)
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    // Get Y-axis label based on sensor type
    fun getYAxisLabel(): String {
        return when (_chartConfig.value.selectedSensorType) {
            SensorType.TEMPERATURE -> "°C"
            SensorType.HUMIDITY -> "%"
            SensorType.WATER_TEMPERATURE -> "°C"
            SensorType.PH -> "pH"
            SensorType.TDS -> "ppm"
        }
    }

    // Get chart title based on sensor type
    fun getChartTitle(): String {
        return when (_chartConfig.value.selectedSensorType) {
            SensorType.TEMPERATURE -> "Suhu Udara"
            SensorType.HUMIDITY -> "Kelembapan"
            SensorType.WATER_TEMPERATURE -> "Suhu Air"
            SensorType.PH -> "pH Air"
            SensorType.TDS -> "TDS (Nutrisi)"
        }
    }
}
