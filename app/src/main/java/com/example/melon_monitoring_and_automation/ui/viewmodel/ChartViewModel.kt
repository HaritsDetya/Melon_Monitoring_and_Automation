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
import com.example.melon_monitoring_and_automation.domain.model.DateRange
import com.example.melon_monitoring_and_automation.domain.model.MonthYear
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
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    // State untuk date picker dan available months
    private val _showDateRangePicker = MutableStateFlow(false)
    val showDateRangePicker: StateFlow<Boolean> = _showDateRangePicker.asStateFlow()

    private val _availableMonths = MutableStateFlow<List<MonthYear>>(emptyList())
    val availableMonths: StateFlow<List<MonthYear>> = _availableMonths.asStateFlow()

    private val _selectedMonth = MutableStateFlow<MonthYear?>(null)
    val selectedMonth: StateFlow<MonthYear?> = _selectedMonth.asStateFlow()

    private val _weeklyRanges = MutableStateFlow<List<DateRange>>(emptyList())
    val weeklyRanges: StateFlow<List<DateRange>> = _weeklyRanges.asStateFlow()

    private val _chartConfig = MutableStateFlow(ChartConfig())
    val chartConfig: StateFlow<ChartConfig> = _chartConfig.asStateFlow()

    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ✅ PERBAIKAN: Tambah state untuk custom date range yang sedang dipilih
    private val _selectedCustomRange = MutableStateFlow<DateRange?>(null)
    val selectedCustomRange: StateFlow<DateRange?> = _selectedCustomRange.asStateFlow()

    // Load available months data
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadAvailableMonths(greenhouseId: String) {
        viewModelScope.launch {
            try {
                println("🔹 [CHART] Loading available months for greenhouse: $greenhouseId")

                // Get distinct months from database
                val months = getAvailableMonthsFromDatabase(greenhouseId)
                _availableMonths.value = months

                // Auto-select latest month
                val latestMonth = months.lastOrNull()
                _selectedMonth.value = latestMonth

                // Generate weekly ranges untuk month yang terpilih
                latestMonth?.let { generateWeeklyRangesForMonth(it) }

                println("🔹 [CHART] Available months loaded: ${months.size} months")

            } catch (e: Exception) {
                println("🔹 [CHART] Error loading available months: ${e.message}")
                _errorMessage.value = "Gagal memuat data bulan tersedia"
            }
        }
    }

    // Generate weekly ranges for selected month
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateWeeklyRangesForMonth(monthYear: MonthYear) {
        viewModelScope.launch {
            try {
                println("🔹 [CHART] Generating weekly ranges for: $monthYear")

                val weeklyRanges = mutableListOf<DateRange>()
                val calendar = Calendar.getInstance()

                // Set to first day of month
                calendar.set(monthYear.year, monthYear.monthValue - 1, 1, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val firstDay = calendar.timeInMillis

                // Set to last day of month
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                val lastDay = calendar.timeInMillis

                // Generate weekly ranges
                var currentWeekStart = firstDay
                while (currentWeekStart <= lastDay) {
                    val weekEnd = minOf(currentWeekStart + 6 * 24 * 60 * 60 * 1000, lastDay)
                    val weekRange = DateRange.createWeeklyRange(currentWeekStart, weekEnd)
                    weeklyRanges.add(weekRange)

                    // Move to next week
                    currentWeekStart = weekEnd + 24 * 60 * 60 * 1000 // Start dari hari setelah weekEnd
                }

                _weeklyRanges.value = weeklyRanges
                println("🔹 [CHART] Generated ${weeklyRanges.size} weekly ranges")

            } catch (e: Exception) {
                println("🔹 [CHART] Error generating weekly ranges: ${e.message}")
                _errorMessage.value = "Gagal menghasilkan rentang mingguan"
            }
        }
    }

    // ✅ PERBAIKAN: Update chart config dengan custom date range
    fun updateCustomDateRange(dateRange: DateRange) {
        _selectedCustomRange.value = dateRange
        _chartConfig.value = _chartConfig.value.copy(
            timeRange = TimeRange.CUSTOM,
            customDateRange = dateRange
        )
        _showDateRangePicker.value = false
        println("🔹 [CHART] Custom date range selected: ${dateRange.label}")
    }

    // ✅ PERBAIKAN: Update sensor type dengan reset custom range jika perlu
    fun updateSensorType(sensorType: SensorType) {
        val currentConfig = _chartConfig.value
        _chartConfig.value = currentConfig.copy(selectedSensorType = sensorType)

        // Jika menggunakan custom range, tetap pertahankan
        if (currentConfig.timeRange != TimeRange.CUSTOM) {
            _selectedCustomRange.value = null
        }
    }

    // ✅ PERBAIKAN: Update time range dengan handle custom range
    fun updateTimeRange(timeRange: TimeRange) {
        val currentConfig = _chartConfig.value

        if (timeRange == TimeRange.CUSTOM) {
            // Jika memilih custom, tapi belum ada range yang dipilih, show picker
            if (_selectedCustomRange.value == null) {
                _showDateRangePicker.value = true
            } else {
                // Jika sudah ada range yang dipilih, update config
                _chartConfig.value = currentConfig.copy(
                    timeRange = timeRange,
                    customDateRange = _selectedCustomRange.value
                )
            }
        } else {
            // Untuk predefined ranges, clear custom range
            _selectedCustomRange.value = null
            _chartConfig.value = currentConfig.copy(
                timeRange = timeRange,
                customDateRange = null
            )
        }
    }

    // Show/hide date range picker
    fun showDateRangePicker() {
        _showDateRangePicker.value = true
    }

    fun hideDateRangePicker() {
        _showDateRangePicker.value = false
    }

    // ✅ Select month
    @RequiresApi(Build.VERSION_CODES.O)
    fun selectMonth(monthYear: MonthYear) {
        _selectedMonth.value = monthYear
        generateWeeklyRangesForMonth(monthYear)
    }

    // PERBAIKAN: Load chart data dengan improved custom range handling
    @RequiresApi(Build.VERSION_CODES.O)
    fun loadChartData(greenhouseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                println("🔹 [CHART] Loading chart data for greenhouse: $greenhouseId")
                println("🔹 [CHART] Current config: ${_chartConfig.value}")

                val config = _chartConfig.value
                val sensorHistory = when (config.timeRange) {
                    TimeRange.CUSTOM -> {
                        config.customDateRange?.let { dateRange ->
                            println("🔹 [CHART] Using custom date range: ${dateRange.startDate} to ${dateRange.endDate}")
                            getSensorHistoryByDateRange(greenhouseId, config.selectedSensorType, dateRange)
                        } ?: run {
                            println("🔹 [CHART] No custom date range set, using default 7 days")
                            getSensorHistoryByHours(greenhouseId, config.selectedSensorType, 24 * 7)
                        }
                    }
                    else -> {
                        val hours = when (config.timeRange) {
                            TimeRange.HOURS_24 -> 24
                            TimeRange.DAYS_7 -> 24 * 7
                            TimeRange.DAYS_30 -> 24 * 30
                            else -> 24 * 7
                        }
                        getSensorHistoryByHours(greenhouseId, config.selectedSensorType, hours)
                    }
                }

                // Transform data
                val chartDataPoints = try {
                    transformToChartDataPoints(sensorHistory, config.timeRange, config.customDateRange)
                } catch (e: Exception) {
                    println("🔹 [CHART] Error transforming data: ${e.message}")
                    _errorMessage.value = "Gagal memproses data chart: ${e.message}"
                    emptyList()
                }

                _chartData.value = chartDataPoints
                println("🔹 [CHART] Chart data loaded: ${chartDataPoints.size} points")

            } catch (e: Exception) {
                println("🔹 [CHART] Unexpected error in loadChartData: ${e.message}")
                _errorMessage.value = "Terjadi kesalahan: ${e.message}"
                _chartData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Helper methods untuk query database
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getSensorHistoryByDateRange(
        greenhouseId: String,
        sensorType: SensorType,
        dateRange: DateRange
    ): List<SensorHistory> {
        return when (val result = repository.getSensorHistoryByDateRange(
            greenhouseId = greenhouseId,
            sensorType = sensorType,
            startDate = dateRange.startDate,
            endDate = dateRange.endDate
        )) {
            is NetworkResult.Success -> {
                println("🔹 [CHART] Date range history loaded: ${result.data?.size ?: 0} records")
                result.data ?: emptyList()
            }
            is NetworkResult.Error -> {
                println("🔹 [CHART] Error loading date range history: ${result.message}")
                _errorMessage.value = "Gagal memuat data history: ${result.message}"
                emptyList()
            }
            else -> emptyList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun getSensorHistoryByHours(
        greenhouseId: String,
        sensorType: SensorType,
        hours: Int
    ): List<SensorHistory> {
        return when (val result = repository.getSensorHistory(
            greenhouseId = greenhouseId,
            sensorType = sensorType,
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
    }

    // Get available months from database
    private suspend fun getAvailableMonthsFromDatabase(greenhouseId: String): List<MonthYear> {
        // Untuk sekarang, generate 6 bulan terakhir
        val months = mutableListOf<MonthYear>()
        val calendar = Calendar.getInstance()

        for (i in 0 until 6) {
            calendar.add(Calendar.MONTH, -i)
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            months.add(MonthYear(month, year))
            calendar.add(Calendar.MONTH, i) // Reset
        }
        return months.reversed()
    }

    // ✅ PERBAIKAN: Improved transform function dengan custom range support
    private fun transformToChartDataPoints(
        sensorHistory: List<SensorHistory>,
        timeRange: TimeRange,
        customDateRange: DateRange? = null
    ): List<ChartDataPoint> {
        if (sensorHistory.isEmpty()) {
            println("🔹 [CHART] No data to transform")
            return emptyList()
        }

        return try {
            println("🔹 [CHART] Transforming ${sensorHistory.size} records for $timeRange")

            // Urutkan data berdasarkan waktu
            val sortedData = sensorHistory.sortedBy { it.recordedAt }

            when (timeRange) {
                TimeRange.HOURS_24 -> transformTo24HourDataPoints(sortedData)
                TimeRange.DAYS_7 -> transformTo7DayDataPoints(sortedData)
                TimeRange.DAYS_30 -> transformTo30DayDataPoints(sortedData)
                TimeRange.CUSTOM -> transformToCustomDataPoints(sortedData, customDateRange)
            }
        } catch (e: Exception) {
            println("🔹 [CHART] Error in transformation: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // ✅ PERBAIKAN: Transform untuk custom date range
    private fun transformToCustomDataPoints(
        data: List<SensorHistory>,
        customDateRange: DateRange?
    ): List<ChartDataPoint> {
        if (data.isEmpty()) return emptyList()

        // Untuk custom range, kita perlu menentukan grouping berdasarkan durasi
        val duration = customDateRange?.let { range ->
            (range.endDate - range.startDate) / (24 * 60 * 60 * 1000) // Durasi dalam hari
        } ?: 7L // Default 7 hari jika tidak ada custom range

        return when {
            duration <= 1 -> transformTo24HourDataPoints(data) // <= 1 hari: per jam
            duration <= 7 -> transformTo7DayDataPoints(data)   // <= 7 hari: per hari
            else -> transformTo30DayDataPoints(data)           // > 7 hari: per hari dengan sampling
        }
    }

    // ✅ PERBAIKAN: Transform untuk 24 jam - sampling yang benar
    private fun transformTo24HourDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Untuk 24 jam, kita butuh maksimal 24 data points
        val targetPoints = 24

        return if (data.size <= targetPoints) {
            // Jika data kurang dari 24, gunakan semua
            data.mapIndexed { index, history ->
                ChartDataPoint(
                    x = index.toFloat(),
                    y = history.value.toFloat(),
                    label = formatTimeForChart(history.recordedAt, TimeRange.HOURS_24),
                    rawValue = history.value
                )
            }
        } else {
            // Jika data lebih dari 24, sampling dengan interval
            val step = data.size / targetPoints
            data.filterIndexed { index, _ -> index % step == 0 }
                .take(targetPoints)
                .mapIndexed { index, history ->
                    ChartDataPoint(
                        x = index.toFloat(),
                        y = history.value.toFloat(),
                        label = formatTimeForChart(history.recordedAt, TimeRange.HOURS_24),
                        rawValue = history.value
                    )
                }
        }
    }

    // ✅ PERBAIKAN: Transform untuk 7 hari - grouping per hari
    private fun transformTo7DayDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Group by date dan ambil rata-rata per hari
        val dailyData = data.groupBy { history ->
            history.recordedAt.substring(0, 10) // YYYY-MM-DD
        }.mapValues { (_, entries) ->
            // Hitung rata-rata nilai per hari
            entries.map { it.value }.average()
        }.toList().sortedBy { it.first } // Urutkan berdasarkan tanggal

        // Ambil 7 hari terakhir atau semua jika kurang
        val chartData = if (dailyData.size > 7) dailyData.takeLast(7) else dailyData

        return chartData.mapIndexed { index, (date, value) ->
            ChartDataPoint(
                x = index.toFloat(),
                y = value.toFloat(),
                label = formatDateForChart(date, TimeRange.DAYS_7),
                rawValue = value
            )
        }
    }

    // ✅ PERBAIKAN: Transform untuk 30 hari - grouping per hari
    private fun transformTo30DayDataPoints(data: List<SensorHistory>): List<ChartDataPoint> {
        // Group by date dan ambil rata-rata per hari
        val dailyData = data.groupBy { history ->
            history.recordedAt.substring(0, 10) // YYYY-MM-DD
        }.mapValues { (_, entries) ->
            // Hitung rata-rata nilai per hari
            entries.map { it.value }.average()
        }.toList().sortedBy { it.first } // Urutkan berdasarkan tanggal

        // Ambil 30 hari terakhir atau semua jika kurang
        val chartData = if (dailyData.size > 30) dailyData.takeLast(30) else dailyData

        return chartData.mapIndexed { index, (date, value) ->
            ChartDataPoint(
                x = index.toFloat(),
                y = value.toFloat(),
                label = formatDateForChart(date, TimeRange.DAYS_30),
                rawValue = value
            )
        }
    }

    private fun formatTimeForChart(
        timestamp: String,
        timeRange: TimeRange,
        customDateRange: DateRange? = null
    ): String {
        return try {
            when (timeRange) {
                TimeRange.HOURS_24 -> {
                    // Format: HH:mm (jam:menit)
                    formatAsTime(timestamp)
                }
                TimeRange.DAYS_7 -> {
                    // Format: DD/MM (tanggal/bulan)
                    formatAsDate(timestamp)
                }
                TimeRange.DAYS_30 -> {
                    // Format: DD/MM (tanggal/bulan)
                    formatAsDate(timestamp)
                }
                TimeRange.CUSTOM -> {
                    // ✅ PERBAIKAN: Format cerdas berdasarkan durasi custom range
                    formatForCustomRange(timestamp, customDateRange)
                }
            }
        } catch (e: Exception) {
            "??"
        }
    }

    // Helper functions untuk formatting yang lebih bersih
    private fun formatAsTime(timestamp: String): String {
        return if (timestamp.length >= 16) {
            timestamp.substring(11, 16) // HH:mm
        } else {
            "??:??"
        }
    }

    private fun formatAsDate(timestamp: String): String {
        return if (timestamp.length >= 10) {
            val monthDay = timestamp.substring(5, 10) // MM-DD
            monthDay.replace("-", "/") // DD/MM
        } else {
            "??/??"
        }
    }

    private fun formatAsDateWithYear(timestamp: String): String {
        return if (timestamp.length >= 10) {
            val year = timestamp.substring(0, 4)
            val month = timestamp.substring(5, 7)
            val day = timestamp.substring(8, 10)
            "$day/$month/$year"
        } else {
            "??/??/??"
        }
    }

    private fun formatForCustomRange(timestamp: String, customDateRange: DateRange?): String {
        // Jika tidak ada custom range, default ke format date
        if (customDateRange == null) return formatAsDate(timestamp)

        // Tentukan format berdasarkan durasi custom range
        val durationInDays = customDateRange.getDurationInDays()

        return when {
            durationInDays <= 1 -> {
                // ≤ 1 hari: tampilkan waktu (HH:mm)
                formatAsTime(timestamp)
            }
            durationInDays <= 7 -> {
                // ≤ 7 hari: tampilkan tanggal (DD/MM)
                formatAsDate(timestamp)
            }
            durationInDays <= 30 -> {
                // ≤ 30 hari: tampilkan tanggal (DD/MM)
                formatAsDate(timestamp)
            }
            else -> {
                // > 30 hari: tampilkan dengan tahun (DD/MM/YY)
                if (timestamp.length >= 10) {
                    val year = timestamp.substring(2, 4) // Last 2 digits of year
                    val month = timestamp.substring(5, 7)
                    val day = timestamp.substring(8, 10)
                    "$day/$month/$year"
                } else {
                    "??/??/??"
                }
            }
        }
    }

    // ✅ PERBAIKAN: Format tanggal untuk chart label
    private fun formatDateForChart(date: String, timeRange: TimeRange): String {
        return try {
            when (timeRange) {
                TimeRange.DAYS_7 -> {
                    // Format: DD/MM (tanggal/bulan)
                    if (date.length >= 10) {
                        val month = date.substring(5, 7)
                        val day = date.substring(8, 10)
                        "$day/$month"
                    } else {
                        "??/??"
                    }
                }
                TimeRange.DAYS_30 -> {
                    // Format: DD/MM (tanggal/bulan)
                    if (date.length >= 10) {
                        val month = date.substring(5, 7)
                        val day = date.substring(8, 10)
                        "$day/$month"
                    } else {
                        "??/??"
                    }
                }
                else -> date
            }
        } catch (e: Exception) {
            "??/??"
        }
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
