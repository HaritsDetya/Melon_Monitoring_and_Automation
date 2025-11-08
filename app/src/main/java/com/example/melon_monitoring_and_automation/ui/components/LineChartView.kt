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

@Composable
fun SensorLineChart(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    chartTitle: String,
    modifier: Modifier = Modifier
) {
    // State untuk menangani error
    var chartError by remember { mutableStateOf<String?>(null) }

    // Validasi data points
    val validDataPoints = remember(dataPoints) {
        dataPoints.filter {
            !it.y.isNaN() && !it.y.isInfinite()
        }
    }

    // Reset error ketika data berubah
    LaunchedEffect(validDataPoints) {
        if (validDataPoints.isEmpty() && dataPoints.isNotEmpty()) {
            chartError = "Data chart tidak valid"
        } else {
            chartError = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Chart Title
            Text(
                text = chartTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF388E3C),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Chart Content berdasarkan state
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
                    // Render chart normal
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

@Composable
private fun ChartContent(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onError: (String) -> Unit
) {
    Column {
        // Chart Canvas
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

        // X-axis labels
        SimpleXAxisLabels(dataPoints = dataPoints)
    }
}

@Composable
private fun SafeChartCanvas(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onError: (String) -> Unit
) {
    var canvasError by remember { mutableStateOf(false) }

    if (canvasError) {
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

@Composable
private fun ChartCanvas(
    dataPoints: List<ChartDataPoint>,
    yAxisLabel: String,
    onCanvasError: () -> Unit
) {
    // Hitung nilai untuk chart dengan error handling
    val chartValues = remember(dataPoints) {
        try {
            val maxY = dataPoints.maxOfOrNull { it.y } ?: 100f
            val minY = dataPoints.minOfOrNull { it.y } ?: 0f
            val yRange = (maxY - minY).coerceAtLeast(1f)
            Triple(maxY, minY, yRange)
        } catch (e: Exception) {
            onCanvasError()
            Triple(100f, 0f, 100f)
        }
    }

    val (maxY, minY, yRange) = chartValues

    Canvas(modifier = Modifier.fillMaxSize()) {
        try {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 40.dp.toPx()

            // Draw Y-axis
            drawLine(
                color = Color.Gray,
                start = Offset(padding, padding),
                end = Offset(padding, canvasHeight - padding),
                strokeWidth = 2f
            )

            // Draw X-axis
            drawLine(
                color = Color.Gray,
                start = Offset(padding, canvasHeight - padding),
                end = Offset(canvasWidth - padding, canvasHeight - padding),
                strokeWidth = 2f
            )

            // Draw Y-axis labels
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

            // Draw Y-axis unit
            drawContext.canvas.nativeCanvas.drawText(
                yAxisLabel,
                padding - 20.dp.toPx(),
                20.dp.toPx(),
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 12.dp.toPx()
                }
            )

            // Draw chart line and points
            if (dataPoints.isNotEmpty()) {
                val xStep = if (dataPoints.size > 1) {
                    (canvasWidth - 2 * padding) / (dataPoints.size - 1)
                } else {
                    0f
                }

                // Draw line
                val path = androidx.compose.ui.graphics.Path().apply {
                    dataPoints.forEachIndexed { index, dataPoint ->
                        val x = padding + (index * xStep)
                        val y = canvasHeight - padding - ((dataPoint.y - minY) / yRange) * (canvasHeight - 2 * padding)

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

                // Draw data points
                dataPoints.forEachIndexed { index, dataPoint ->
                    val x = padding + (index * xStep)
                    val y = canvasHeight - padding - ((dataPoint.y - minY) / yRange) * (canvasHeight - 2 * padding)

                    drawCircle(
                        color = Color(0xFF388E3C),
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        } catch (e: Exception) {
            // Jika ada error saat drawing, trigger error callback
            onCanvasError()
        }
    }
}

@Composable
private fun SimpleXAxisLabels(dataPoints: List<ChartDataPoint>) {
    if (dataPoints.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Show first, middle, and last labels only
        val indices = listOf(0, dataPoints.size / 2, dataPoints.size - 1)
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
            .background(Color(0xFFFFF8E1)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "⚠️ Error Chart",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF57C00)
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

@Composable
private fun EmptyChartState(
    chartTitle: String,
    dataSize: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFF5F5F5)),
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