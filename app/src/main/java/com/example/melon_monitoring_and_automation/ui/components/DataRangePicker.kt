package com.example.melon_monitoring_and_automation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.melon_monitoring_and_automation.domain.model.DateRange
import com.example.melon_monitoring_and_automation.domain.model.MonthYear
import com.example.melon_monitoring_and_automation.domain.model.TimeRange

@Composable
fun DateRangePicker(
    showDateRangePicker: Boolean,
    availableMonths: List<MonthYear>,
    selectedMonth: MonthYear?,
    weeklyRanges: List<DateRange>,
    selectedTimeRange: TimeRange,
    customDateRange: DateRange?,
    onMonthSelected: (MonthYear) -> Unit,
    onDateRangeSelected: (DateRange) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (showDateRangePicker) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pilih Rentang Waktu",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF388E3C)
                        )
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Month Selection
                    Text(
                        "Pilih Bulan:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableMonths) { month ->
                            MonthChip(
                                month = month,
                                isSelected = selectedMonth == month,
                                onSelected = onMonthSelected
                            )
                        }
                    }

                    // Weekly Ranges - hanya tampilkan jika bulan terpilih
                    if (selectedMonth != null && weeklyRanges.isNotEmpty()) {
                        Text(
                            "Pilih Minggu:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(weeklyRanges) { dateRange ->
                                WeekRangeChip(
                                    dateRange = dateRange,
                                    isSelected = customDateRange?.startDate == dateRange.startDate &&
                                            customDateRange?.endDate == dateRange.endDate,
                                    onSelected = onDateRangeSelected
                                )
                            }
                        }
                    } else if (selectedMonth != null) {
                        Text(
                            "Tidak ada data untuk bulan ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Selected Range Info
                    if (customDateRange != null) {
                        Text(
                            "Rentang terpilih: ${customDateRange.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // Instruction
                    Text(
                        "Pilih bulan terlebih dahulu, lalu pilih minggu yang diinginkan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MonthChip(
    month: MonthYear,
    isSelected: Boolean,
    onSelected: (MonthYear) -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF388E3C)
                else Color(0xFFF5F5F5)
            )
            .clickable { onSelected(month) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            month.displayName,
            color = if (isSelected) Color.White else Color.Black,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun WeekRangeChip(
    dateRange: DateRange,
    isSelected: Boolean,
    onSelected: (DateRange) -> Unit
) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) Color(0xFF4CAF50)
                else Color(0xFFE8F5E9)
            )
            .clickable { onSelected(dateRange) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            dateRange.label,
            color = if (isSelected) Color.White else Color(0xFF388E3C),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// Custom Filter Chip untuk Custom Date Range
@Composable
fun CustomDateRangeChip(
    isSelected: Boolean,
    customDateRange: DateRange?,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                customDateRange?.label ?: "Rentang Kustom",
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Custom Range",
                modifier = Modifier.size(16.dp)
            )
        },
        modifier = Modifier.padding(end = 8.dp)
    )
}