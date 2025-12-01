package com.example.melon_monitoring_and_automation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.melon_monitoring_and_automation.domain.model.ChartDataPoint

/**
 * SENSOR LINE CHART COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan data sensor time-series dalam bentuk grafik garis
 * - Menyediakan visualisasi data yang mudah dipahami untuk monitoring
 * - Menangani berbagai state data (loading, error, empty, success)
 *
 * Fitur:
 * - Validasi data otomatis (NaN, Infinite values)
 * - Error handling yang graceful
 * - Empty state visualization
 * - Customizable styling dan labels
 * - Responsive design
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints List data points yang akan ditampilkan
 * @param yAxisLabel Label untuk sumbu Y (unit pengukuran)
 * @param chartTitle Judul chart yang deskriptif
 * @param modifier Modifier untuk kustomisasi layout
 */

@Composable
fun SensorLineChart(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    chartTitle: String,
    modifier: Modifier = Modifier
) {
    // LOCAL STATE MANAGEMENT - Error handling selama rendering
    var chartError by remember { mutableStateOf<String?>(null) }

    // DATA VALIDATION - Filter out invalid data points
    val validDataPoints = remember(dataPoints) {
        dataPoints.filter {
            !it.y.isNaN() && !it.y.isInfinite()
        }
    }

    // ERROR RESET - Reset error ketika data berubah
    LaunchedEffect(validDataPoints) {
        if (validDataPoints.isEmpty() && dataPoints.isNotEmpty()) {
            chartError = "Data chart tidak valid"
        } else {
            chartError = null
        }
    }

    // MAIN CHART CARD
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // CHART TITLE
            Text(
                text = chartTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // CHART CONTENT - Berdasarkan state current
            when {
                chartError != null -> {
                    ChartErrorState(
                        errorMessage = chartError!!,
                        dataSize = dataPoints.size,
                        validSize = validDataPoints.size
                    )
                }
                validDataPoints.isEmpty() -> {
                    EmptyChartState(
                        chartTitle = chartTitle,
                        dataSize = dataPoints.size
                    )
                }
                else -> {
                    // RENDER CHART NORMAL - Dengan data yang valid
                    ChartContent(
                        dataPoints = validDataPoints,
                        yAxisLabel = yAxisLabel,
                        onError = { error -> chartError = error }
                    )
                }
            }
        }
    }
}

/**
 * CHART CONTENT COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan canvas chart dan label sumbu
 * - Mengkoordinasikan rendering chart dengan error handling
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints List data points yang valid
 * @param yAxisLabel Label untuk sumbu Y
 * @param onError Callback untuk error handling
 */

@Composable
private fun ChartContent(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onError: (String) -> Unit
) {
    Column {
        // CHART CANVAS AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            SafeChartCanvas(
                dataPoints = dataPoints,
                yAxisLabel = yAxisLabel,
                onError = onError
            )
        }

        // X-AXIS LABELS
        SimpleXAxisLabels(dataPoints = dataPoints)
    }
}

/**
 * SAFE CHART CANVAS COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan wrapper aman untuk chart canvas dengan error boundary
 * - Mencegah crash ketika rendering gagal
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints Data points untuk rendering
 * @param yAxisLabel Label sumbu Y
 * @param onError Callback error handling
 */

@Composable
private fun SafeChartCanvas(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onError: (String) -> Unit
) {
    var canvasError by remember { mutableStateOf(false) }

    if (canvasError) {
        // FALLBACK UI - Ketika canvas error
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Grafik tidak dapat dirender",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    } else {
        ChartCanvas(
            dataPoints = dataPoints,
            yAxisLabel = yAxisLabel,
            onCanvasError = { canvasError = true }
        )
    }
}

/**
 * CHART CANVAS COMPOSABLE
 *
 * Tujuan:
 * - Melakukan rendering grafik garis dan elemen-elemennya
 * - Menghitung scaling dan positioning data points
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints Data points untuk digambar
 * @param yAxisLabel Label sumbu Y
 * @param onCanvasError Callback ketika canvas error
 */

@Composable
private fun ChartCanvas(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onCanvasError: () -> Unit
) {
    // CHART VALUES CALCULATION - Dengan error handling
    val chartValues = remember(dataPoints) {
        try {
            if (dataPoints.isEmpty()) {
                Triple(100f, 0f, 100f) // Default values untuk empty data
            } else {
                val maxY = dataPoints.maxOf { it.y }
                val minY = dataPoints.minOf { it.y }
                val yRange = (maxY - minY).coerceAtLeast(1f) // Pastikan range minimal 1
                Triple(maxY, minY, yRange)
            }
        } catch (e: Exception) {
            println("🔹 [CHART] Error calculating chart values: ${e.message}")
            onCanvasError()
            Triple(100f, 0f, 100f) // Fallback values
        }
    }

    val (maxY, minY, yRange) = chartValues

    // CANVAS RENDERING
    Canvas(modifier = Modifier.fillMaxSize()) {
        try {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 50.dp.toPx() // Padding untuk label sumbu

            // DRAW Y-AXIS
            drawLine(
                color = Color.Gray,
                start = Offset(padding, padding),
                end = Offset(padding, canvasHeight - padding),
                strokeWidth = 2f
            )

            // DRAW X-AXIS
            drawLine(
                color = Color.Gray,
                start = Offset(padding, canvasHeight - padding),
                end = Offset(canvasWidth - padding, canvasHeight - padding),
                strokeWidth = 2f
            )

            // DRAW Y-AXIS LABELS - Dengan nilai aktual
            val yStep = (canvasHeight - 2 * padding) / 4
            for (i in 0..4) {
                val yValue = minY + (yRange * (4 - i) / 4)
                val yPos = padding + (yStep * i)

                drawContext.canvas.nativeCanvas.drawText(
                    "%.1f".format(yValue),
                    padding - 35.dp.toPx(),
                    yPos + 5.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 12.dp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // DRAW Y-AXIS UNIT
            drawContext.canvas.nativeCanvas.drawText(
                yAxisLabel,
                padding - 20.dp.toPx(),
                20.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 12.dp.toPx()
                }
            )

            // DRAW CHART LINE AND POINTS - Hanya jika ada data
            if (dataPoints.isNotEmpty()) {
                val xStep = if (dataPoints.size > 1) {
                    (canvasWidth - 2 * padding) / (dataPoints.size - 1)
                } else {
                    canvasWidth - 2 * padding // Jika hanya 1 data point
                }

                // DRAW CHART LINE
                val path = androidx.compose.ui.graphics.Path().apply {
                    dataPoints.forEachIndexed { index, dataPoint ->
                        val x = padding + (index * xStep)
                        // Hitung posisi Y dengan scaling yang benar
                        val y = if (yRange > 0) {
                            canvasHeight - padding - ((dataPoint.y - minY) / yRange) * (canvasHeight - 2 * padding)
                        } else {
                            canvasHeight / 2f // Jika semua nilai sama
                        }

                        if (index == 0) {
                            moveTo(x, y)
                        } else {
                            lineTo(x, y)
                        }
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // DRAW DATA POINTS
                dataPoints.forEachIndexed { index, dataPoint ->
                    val x = padding + (index * xStep)
                    val y = if (yRange > 0) {
                        canvasHeight - padding - ((dataPoint.y - minY) / yRange) * (canvasHeight - 2 * padding)
                    } else {
                        canvasHeight / 2f
                    }

                    drawCircle(
                        color = Color(0xFF388E3C),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            } else {
                // EMPTY DATA MESSAGE
                drawContext.canvas.nativeCanvas.drawText(
                    "No data available",
                    canvasWidth / 2,
                    canvasHeight / 2,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 16.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        } catch (e: Exception) {
            println("🔹 [CHART] Canvas drawing error: ${e.message}")
            onCanvasError()
        }
    }
}

/**
 * SIMPLE X-AXIS LABELS COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan label sederhana untuk sumbu X
 * - Mengoptimalkan space dengan menampilkan label penting saja
 *
 * @author Your Name
 * @since Version 1.0
 * @param dataPoints Data points untuk menentukan label
 */

@Composable
private fun SimpleXAxisLabels(dataPoints: List<ChartDataPoint>) {
    if (dataPoints.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 50.dp), // Match chart padding
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Tampilkan label pertama, tengah, dan terakhir
        val indices = when {
            dataPoints.size <= 3 -> dataPoints.indices.toList()
            else -> listOf(0, dataPoints.size / 2, dataPoints.size - 1)
        }

        indices.forEach { index ->
            if (index < dataPoints.size) {
                Text(
                    text = dataPoints[index].label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * CHART ERROR STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI yang informatif ketika chart gagal dirender
 * - Memberikan context tentang masalah yang terjadi
 *
 * @author Your Name
 * @since Version 1.0
 * @param errorMessage Pesan error yang deskriptif
 * @param dataSize Jumlah total data points
 * @param validSize Jumlah data points yang valid
 */

@Composable
private fun ChartErrorState(
    errorMessage: String,
    dataSize: Int,
    validSize: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFFFF8E1)), // Light yellow background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚠️ Error Chart",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF57C00) // Orange
            )
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Data: $validSize/$dataSize valid",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * EMPTY CHART STATE COMPOSABLE
 *
 * Tujuan:
 * - Menampilkan UI yang informatif ketika tidak ada data yang valid
 * - Memberikan feedback visual yang jelas tentang empty state
 *
 * @author Your Name
 * @since Version 1.0
 * @param chartTitle Judul chart untuk context
 * @param dataSize Jumlah total data points
 */

@Composable
private fun EmptyChartState(
    chartTitle: String,
    dataSize: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFF5F5F5)), // Light gray background
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "📊 $chartTitle",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            Text(
                "Tidak ada data yang valid",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Total data: $dataSize",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}
