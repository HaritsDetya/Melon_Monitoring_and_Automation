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

/**
 * CHART VIEWMODEL CLASS
 *
 * Tujuan:
 * - Mengelola data dan state untuk chart visualization
 * - Menangani date range selection dan data filtering
 * - Menyediakan transformed data untuk chart rendering
 * - Mengkoordinasikan antara UI controls dan data fetching
 *
 * Responsibilities:
 * - Date range picker state management
 * - Sensor type filtering
 * - Time range selection (24 jam, 7 hari, 30 hari, custom)
 * - Data transformation untuk chart formatting
 * - Error handling untuk chart data operations
 *
 * @author Your Name
 * @since Version 1.0
 * @property repository Repository untuk data hydroponic
 * @requires Android O (API 26) untuk date/time operations
 */

@HiltViewModel
class ChartViewModel @Inject constructor(
    private val repository: HydroponicRepository
) : ViewModel() {

    // ============ DATE PICKER STATE FLOWS ============

    /**
     * State flow untuk visibility date range picker.
     *
     * Diupdate ketika user membuka/menutup date picker.
     */
    private val _showDateRangePicker = MutableStateFlow(false)
    val showDateRangePicker: StateFlow<Boolean> = _showDateRangePicker.asStateFlow()

    /**
     * State flow untuk available months data.
     *
     * Berisi list bulan-tahun yang tersedia untuk dipilih.
     * Di-load dari database atau generated secara default.
     */
    private val _availableMonths = MutableStateFlow<List<MonthYear>>(emptyList())
    val availableMonths: StateFlow<List<MonthYear>> = _availableMonths.asStateFlow()

    /**
     * State flow untuk selected month.
     *
     * Bulan-tahun yang sedang dipilih user.
     * Auto-select ke latest month pada initialization.
     */
    private val _selectedMonth = MutableStateFlow<MonthYear?>(null)
    val selectedMonth: StateFlow<MonthYear?> = _selectedMonth.asStateFlow()

    /**
     * State flow untuk weekly ranges.
     *
     * List rentang tanggal mingguan untuk bulan yang terpilih.
     * Di-generate secara otomatis berdasarkan selected month.
     */
    private val _weeklyRanges = MutableStateFlow<List<DateRange>>(emptyList())
    val weeklyRanges: StateFlow<List<DateRange>> = _weeklyRanges.asStateFlow()

    // ============ CHART CONFIGURATION STATE FLOWS ============

    /**
     * State flow untuk chart configuration.
     *
     * Berisi semua setting chart (sensor type, time range, custom range).
     * Diupdate ketika user mengubah filter options.
     */
    private val _chartConfig = MutableStateFlow(ChartConfig())
    val chartConfig: StateFlow<ChartConfig> = _chartConfig.asStateFlow()

    /**
     * State flow untuk chart data points.
     *
     * Berisi data yang sudah ditransform untuk chart rendering.
     * Diupdate ketika configuration berubah atau data di-refresh.
     */
    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()

    /**
     * State flow untuk loading state.
     *
     * True ketika chart data sedang dimuat dari repository.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * State flow untuk error messages.
     *
     * Berisi pesan error terkait chart operations.
     * Dapat di-clear dengan clearErrorMessage().
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * State flow untuk selected custom range.
     *
     * Rentang tanggal kustom yang sedang dipilih user.
     * Digunakan ketika time range adalah CUSTOM.
     */
    private val _selectedCustomRange = MutableStateFlow<DateRange?>(null)
    val selectedCustomRange: StateFlow<DateRange?> = _selectedCustomRange.asStateFlow()

    // ============ PUBLIC METHODS - DATE RANGE MANAGEMENT ============

    /**
     * Load available months data dari database.
     *
     * @param greenhouseId ID greenhouse untuk memuat data months
     *
     * Flow:
     * 1. Get distinct months dari database
     * 2. Update availableMonths state
     * 3. Auto-select latest month
     * 4. Generate weekly ranges untuk month yang terpilih
     */
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

    /**
     * Generate weekly ranges untuk month yang dipilih.
     *
     * @param monthYear MonthYear object untuk generate ranges
     *
     * Menghasilkan list DateRange yang merepresentasikan minggu-minggu
     * dalam bulan tertentu.
     */
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

    /**
     * Update chart config dengan custom date range.
     *
     * @param dateRange DateRange object yang dipilih user
     *
     * Diupdate ketika user memilih custom date range dari picker.
     */
    fun updateCustomDateRange(dateRange: DateRange) {
        _selectedCustomRange.value = dateRange
        _chartConfig.value = _chartConfig.value.copy(
            timeRange = TimeRange.CUSTOM,
            customDateRange = dateRange
        )
        _showDateRangePicker.value = false
        println("🔹 [CHART] Custom date range selected: ${dateRange.label}")
    }

    /**
     * Update sensor type dengan reset custom range jika perlu.
     *
     * @param sensorType SensorType baru yang dipilih
     *
     * Jika menggunakan custom range, tetap pertahankan range tersebut.
     */
    fun updateSensorType(sensorType: SensorType) {
        val currentConfig = _chartConfig.value
        _chartConfig.value = currentConfig.copy(selectedSensorType = sensorType)

        // Jika menggunakan custom range, tetap pertahankan
        if (currentConfig.timeRange != TimeRange.CUSTOM) {
            _selectedCustomRange.value = null
        }
    }

    /**
     * Update time range dengan handle custom range.
     *
     * @param timeRange TimeRange baru yang dipilih
     *
     * Handle khusus untuk CUSTOM time range:
     * - Jika memilih CUSTOM tapi belum ada range, show picker
     * - Jika sudah ada range, update config langsung
     */
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

    // ============ DATE PICKER CONTROLS ============

    /**
     * Show date range picker.
     *
     * Dipanggil ketika user mengklik custom range chip.
     */
    fun showDateRangePicker() {
        _showDateRangePicker.value = true
    }

    /**
     * Hide date range picker.
     *
     * Dipanggil ketika user menutup picker tanpa memilih.
     */
    fun hideDateRangePicker() {
        _showDateRangePicker.value = false
    }

    /**
     * Select month untuk date range picker.
     *
     * @param monthYear MonthYear yang dipilih user
     *
     * Update selected month dan generate weekly ranges untuk month tersebut.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun selectMonth(monthYear: MonthYear) {
        _selectedMonth.value = monthYear
        generateWeeklyRangesForMonth(monthYear)
    }

    // ============ CHART DATA MANAGEMENT ============

    /**
     * Load chart data berdasarkan current configuration.
     *
     * @param greenhouseId ID greenhouse untuk memuat data
     *
     * Flow:
     * 1. Set loading state ke true
     * 2. Determine query parameters berdasarkan config
     * 3. Fetch data dari repository
     * 4. Transform data untuk chart rendering
     * 5. Update chartData state
     * 6. Handle errors appropriately
     */
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

                // Transform data untuk chart rendering
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

    // ============ HELPER METHODS - DATA QUERY ============

    /**
     * Get sensor history by date range dari repository.
     *
     * @param greenhouseId ID greenhouse
     * @param sensorType Jenis sensor
     * @param dateRange Rentang tanggal
     * @return List SensorHistory atau empty list jika error
     */
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

    /**
     * Get sensor history by hours dari repository.
     *
     * @param greenhouseId ID greenhouse
     * @param sensorType Jenis sensor
     * @param hours Jumlah jam history
     * @return List SensorHistory atau empty list jika error
     */
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

    // ============ PRIVATE HELPER METHODS ============

    /**
     * Get available months dari database (temporary implementation).
     *
     * @param greenhouseId ID greenhouse
     * @return List MonthYear untuk 6 bulan terakhir
     */
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

    // ============ HELPER METHODS - DATA TRANSFORMATION ============

    /**
     * Transform sensor history ke chart data points.
     *
     * @param sensorHistory List data sensor history
     * @param timeRange Time range untuk menentukan formatting
     * @param customDateRange Custom date range (opsional)
     * @return List ChartDataPoint untuk rendering
     */
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

    /**
     * Transform untuk custom date range dengan smart formatting.
     *
     * @param data List data sensor
     * @param customDateRange Custom date range
     * @return List ChartDataPoint dengan formatting yang sesuai
     */
    private fun transformToCustomDataPoints(
        data: List<SensorHistory>,
        customDateRange: DateRange?
    ): List<ChartDataPoint> {
        if (data.isEmpty()) return emptyList()

        // Tentukan grouping berdasarkan durasi custom range
        val duration = customDateRange?.let { range ->
            (range.endDate - range.startDate) / (24 * 60 * 60 * 1000) // Durasi dalam hari
        } ?: 7L // Default 7 hari jika tidak ada custom range

        return when {
            duration <= 1 -> transformTo24HourDataPoints(data) // <= 1 hari: per jam
            duration <= 7 -> transformTo7DayDataPoints(data)   // <= 7 hari: per hari
            else -> transformTo30DayDataPoints(data)           // > 7 hari: per hari dengan sampling
        }
    }

    /**
     * TRANSFORM TO 24 HOUR DATA POINTS METHOD
     *
     * Tujuan:
     * - Mengubah data sensor history menjadi data points untuk chart 24 jam
     * - Melakukan sampling yang optimal untuk mencegah overcrowding chart
     * - Memastikan data ditampilkan dengan rapi dalam 24 points maksimal
     *
     * Algorithm:
     * - Jika data ≤ 24 points: gunakan semua data points
     * - Jika data > 24 points: lakukan sampling dengan interval step
     * - Step = total data / target points (24)
     *
     * @author Your Name
     * @since Version 1.0
     * @param data List SensorHistory yang akan ditransformasi
     * @return List ChartDataPoint untuk rendering chart 24 jam
     *
     * Contoh:
     * - Input: 48 data points → Output: 24 data points (setiap 2 data diambil 1)
     * - Input: 12 data points → Output: 12 data points (semua digunakan)
     */
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

    /**
     * TRANSFORM TO 7 DAY DATA POINTS METHOD
     *
     * Tujuan:
     * - Mengelompokkan data sensor per hari untuk chart 7 hari
     * - Menghitung rata-rata nilai harian untuk setiap hari
     * - Menampilkan maksimal 7 hari terakhir data
     *
     * Algorithm:
     * 1. Group data by date (YYYY-MM-DD)
     * 2. Hitung rata-rata nilai per hari
     * 3. Urutkan berdasarkan tanggal
     * 4. Ambil 7 hari terakhir
     * 5. Transform ke ChartDataPoint
     *
     * @author Your Name
     * @since Version 1.0
     * @param data List SensorHistory yang akan ditransformasi
     * @return List ChartDataPoint untuk rendering chart 7 hari
     *
     * Contoh:
     * - Input: 100 data points dalam 10 hari → Output: 7 data points (7 hari terakhir)
     * - Setiap point merepresentasikan rata-rata harian
     */
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

    /**
     * TRANSFORM TO 30 DAY DATA POINTS METHOD
     *
     * Tujuan:
     * - Mengelompokkan data sensor per hari untuk chart 30 hari
     * - Menghitung rata-rata nilai harian untuk setiap hari
     * - Menampilkan maksimal 30 hari terakhir data
     *
     * Algorithm:
     * 1. Group data by date (YYYY-MM-DD)
     * 2. Hitung rata-rata nilai per hari
     * 3. Urutkan berdasarkan tanggal
     * 4. Ambil 30 hari terakhir
     * 5. Transform ke ChartDataPoint
     *
     * @author Your Name
     * @since Version 1.0
     * @param data List SensorHistory yang akan ditransformasi
     * @return List ChartDataPoint untuk rendering chart 30 hari
     *
     * Contoh:
     * - Input: 900 data points dalam 45 hari → Output: 30 data points (30 hari terakhir)
     * - Setiap point merepresentasikan rata-rata harian
     */
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

    /**
     * FORMAT TIME FOR CHART METHOD
     *
     * Tujuan:
     * - Memformat timestamp untuk ditampilkan sebagai label chart
     * - Menyesuaikan format berdasarkan time range yang dipilih
     * - Menyediakan fallback formatting jika terjadi error
     *
     * Format Rules:
     * - HOURS_24: HH:mm (14:30, 09:45)
     * - DAYS_7: DD/MM (25/12, 01/01)
     * - DAYS_30: DD/MM (25/12, 01/01)
     * - CUSTOM: Format cerdas berdasarkan durasi range
     *
     * @author Your Name
     * @since Version 1.0
     * @param timestamp String timestamp dari database (format: YYYY-MM-DD HH:mm:ss)
     * @param timeRange TimeRange yang aktif untuk menentukan formatting
     * @param customDateRange DateRange kustom (opsional, untuk TimeRange.CUSTOM)
     * @return String label yang sudah diformat untuk chart
     *
     * @throws Exception jika format timestamp tidak valid, return "??" sebagai fallback
     */
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
            "??" // Fallback untuk error formatting
        }
    }

// ============ FORMATTING HELPER FUNCTIONS ============

    /**
     * FORMAT AS TIME HELPER FUNCTION
     *
     * Tujuan:
     * - Mengekstrak bagian waktu dari timestamp (HH:mm)
     * - Menyediakan fallback jika format timestamp tidak valid
     *
     * @author Your Name
     * @since Version 1.0
     * @param timestamp String timestamp (format: YYYY-MM-DD HH:mm:ss)
     * @return String waktu dalam format HH:mm atau "??:??" jika error
     *
     * Contoh:
     * - Input: "2024-01-15 14:30:25" → Output: "14:30"
     * - Input: "invalid" → Output: "??:??"
     */
    private fun formatAsTime(timestamp: String): String {
        return if (timestamp.length >= 16) {
            timestamp.substring(11, 16) // HH:mm
        } else {
            "??:??"
        }
    }

    /**
     * FORMAT AS DATE HELPER FUNCTION
     *
     * Tujuan:
     * - Mengekstrak bagian tanggal dari timestamp (DD/MM)
     * - Mengubah format dari YYYY-MM-DD menjadi DD/MM
     * - Menyediakan fallback jika format timestamp tidak valid
     *
     * @author Your Name
     * @since Version 1.0
     * @param timestamp String timestamp (format: YYYY-MM-DD HH:mm:ss)
     * @return String tanggal dalam format DD/MM atau "??/??" jika error
     *
     * Contoh:
     * - Input: "2024-01-15 14:30:25" → Output: "15/01"
     * - Input: "2024-12-25 00:00:00" → Output: "25/12"
     */
    private fun formatAsDate(timestamp: String): String {
        return if (timestamp.length >= 10) {
            val monthDay = timestamp.substring(5, 10) // MM-DD
            monthDay.replace("-", "/") // DD/MM
        } else {
            "??/??"
        }
    }

    /**
     * FORMAT AS DATE WITH YEAR HELPER FUNCTION
     *
     * Tujuan:
     * - Mengekstrak bagian tanggal dengan tahun dari timestamp (DD/MM/YYYY)
     * - Mengubah format dari YYYY-MM-DD menjadi DD/MM/YYYY
     * - Menyediakan fallback jika format timestamp tidak valid
     *
     * @author Your Name
     * @since Version 1.0
     * @param timestamp String timestamp (format: YYYY-MM-DD HH:mm:ss)
     * @return String tanggal dalam format DD/MM/YYYY atau "??/??/??" jika error
     *
     * Contoh:
     * - Input: "2024-01-15 14:30:25" → Output: "15/01/2024"
     * - Input: "2023-12-25 00:00:00" → Output: "25/12/2023"
     */
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

    /**
     * FORMAT FOR CUSTOM RANGE HELPER FUNCTION
     *
     * Tujuan:
     * - Memformat timestamp secara cerdas berdasarkan durasi custom range
     * - Menentukan format yang optimal berdasarkan panjang rentang waktu
     * - Menyediakan user experience yang konsisten untuk berbagai durasi
     *
     * Format Rules Berdasarkan Durasi:
     * - ≤ 1 hari: Format waktu (HH:mm) - detail per jam
     * - ≤ 7 hari: Format tanggal (DD/MM) - detail per hari
     * - ≤ 30 hari: Format tanggal (DD/MM) - overview harian
     * - > 30 hari: Format tanggal dengan tahun (DD/MM/YY) - overview bulanan
     *
     * @author Your Name
     * @since Version 1.0
     * @param timestamp String timestamp yang akan diformat
     * @param customDateRange DateRange kustom untuk menentukan durasi
     * @return String label yang diformat sesuai durasi custom range
     *
     * Contoh:
     * - Durasi 12 jam → Format: "14:30"
     * - Durasi 3 hari → Format: "15/01"
     * - Durasi 45 hari → Format: "15/01/24"
     */
    private fun formatForCustomRange(timestamp: String, customDateRange: DateRange?): String {
        // Jika tidak ada custom range, default ke format date
        if (customDateRange == null) return formatAsDate(timestamp)

        // Tentukan format berdasarkan durasi custom range
        val durationInDays = customDateRange.getDurationInDays()

        return when {
            durationInDays <= 1 -> {
                // ≤ 1 hari: tampilkan waktu (HH:mm) - detail tinggi
                formatAsTime(timestamp)
            }
            durationInDays <= 7 -> {
                // ≤ 7 hari: tampilkan tanggal (DD/MM) - detail menengah
                formatAsDate(timestamp)
            }
            durationInDays <= 30 -> {
                // ≤ 30 hari: tampilkan tanggal (DD/MM) - overview harian
                formatAsDate(timestamp)
            }
            else -> {
                // > 30 hari: tampilkan dengan tahun (DD/MM/YY) - overview bulanan
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

    /**
     * FORMAT DATE FOR CHART HELPER FUNCTION
     *
     * Tujuan:
     * - Memformat date string khusus untuk chart labels
     * - Menyesuaikan format berdasarkan time range
     * - Menyediakan error handling yang robust
     *
     * @author Your Name
     * @since Version 1.0
     * @param date String date dalam format YYYY-MM-DD
     * @param timeRange TimeRange untuk menentukan formatting
     * @return String label yang diformat atau "??/??" jika error
     *
     * Contoh:
     * - Input: "2024-01-15", DAYS_7 → Output: "15/01"
     * - Input: "2024-12-25", DAYS_30 → Output: "25/12"
     * - Input: "invalid", DAYS_7 → Output: "??/??"
     */
    private fun formatDateForChart(date: String, timeRange: TimeRange): String {
        return try {
            when (timeRange) {
                TimeRange.DAYS_7 -> {
                    // Format: DD/MM (tanggal/bulan) untuk chart 7 hari
                    if (date.length >= 10) {
                        val month = date.substring(5, 7)
                        val day = date.substring(8, 10)
                        "$day/$month"
                    } else {
                        "??/??"
                    }
                }
                TimeRange.DAYS_30 -> {
                    // Format: DD/MM (tanggal/bulan) untuk chart 30 hari
                    if (date.length >= 10) {
                        val month = date.substring(5, 7)
                        val day = date.substring(8, 10)
                        "$day/$month"
                    } else {
                        "??/??"
                    }
                }
                else -> date // Untuk time range lain, return as-is
            }
        } catch (e: Exception) {
            "??/??" // Fallback untuk error formatting
        }
    }

    // ============ UTILITY METHODS ============

    /**
     * Clear error message state.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Get Y-axis label berdasarkan sensor type.
     *
     * @return String label untuk sumbu Y chart
     */
    fun getYAxisLabel(): String {
        return when (_chartConfig.value.selectedSensorType) {
            SensorType.TEMPERATURE -> "°C"
            SensorType.HUMIDITY -> "%"
            SensorType.WATER_TEMPERATURE -> "°C"
            SensorType.PH -> "pH"
            SensorType.TDS -> "ppm"
        }
    }

    /**
     * Get chart title berdasarkan sensor type.
     *
     * @return String judul chart yang deskriptif
     */
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
