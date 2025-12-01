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

/**
 * DATE RANGE PICKER COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan interface pemilihan rentang tanggal untuk filtering data historis
 * - Memungkinkan pemilihan bulan dan minggu tertentu
 * - Menampilkan overlay dialog untuk experience yang terpusat
 *
 * Fitur:
 * - Pilihan bulan dari available months
 * - Pilihan minggu berdasarkan bulan terpilih
 * - Custom date range selection
 * - Visual feedback untuk pilihan aktif
 * - Responsive design untuk berbagai screen size
 *
 * @author Your Name
 * @since Version 1.0
 * @param showDateRangePicker Boolean kontrol visibilitas picker
 * @param availableMonths List bulan-tahun yang tersedia
 * @param selectedMonth Bulan-tahun yang sedang dipilih
 * @param weeklyRanges List rentang tanggal mingguan
 * @param selectedTimeRange Rentang waktu yang dipilih
 * @param customDateRange Rentang tanggal kustom
 * @param onMonthSelected Callback ketika bulan dipilih
 * @param onDateRangeSelected Callback ketika rentang tanggal dipilih
 * @param onClose Callback ketika picker ditutup
 * @param modifier Modifier untuk kustomisasi layout
 */

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
                    // HEADER SECTION - Title dan close button
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

                    // MONTH SELECTION SECTION
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

                    // WEEKLY RANGES SECTION - Hanya tampil jika bulan terpilih
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
                        // EMPTY STATE - Tidak ada data untuk bulan terpilih
                        Text(
                            "Tidak ada data untuk bulan ini",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // SELECTED RANGE INFO
                    if (customDateRange != null) {
                        Text(
                            "Rentang terpilih: ${customDateRange.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    // USAGE INSTRUCTION
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

/**
 * MONTH CHIP COMPOSABLE
 *
 * Tujuan:
 * - Merepresentasikan satu bulan dalam bentuk chip yang dapat diklik
 * - Memberikan visual feedback untuk state terpilih/tidak terpilih
 *
 * @author Your Name
 * @since Version 1.0
 * @param month Objek MonthYear yang direpresentasikan
 * @param isSelected Boolean status seleksi
 * @param onSelected Callback ketika chip dipilih
 */

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
                if (isSelected) Color(0xFF388E3C) // Hijau ketika dipilih
                else Color(0xFFF5F5F5) // Abu-abu terang ketika tidak dipilih
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

/**
 * WEEK RANGE CHIP COMPOSABLE
 *
 * Tujuan:
 * - Merepresentasikan rentang mingguan dalam bentuk chip
 * - Menampilkan label tanggal yang user-friendly
 *
 * @author Your Name
 * @since Version 1.0
 * @param dateRange Objek DateRange yang direpresentasikan
 * @param isSelected Boolean status seleksi
 * @param onSelected Callback ketika chip dipilih
 */

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
                if (isSelected) Color(0xFF4CAF50) // Hijau lebih terang ketika dipilih
                else Color(0xFFE8F5E9) // Hijau sangat terang ketika tidak dipilih
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

/**
 * CUSTOM DATE RANGE CHIP COMPOSABLE
 *
 * Tujuan:
 * - Menyediakan opsi pemilihan rentang tanggal kustom
 * - Mengintegrasikan dengan date picker system
 *
 * @author Your Name
 * @since Version 1.0
 * @param isSelected Boolean status seleksi
 * @param customDateRange Rentang tanggal kustom
 * @param onClick Callback ketika chip diklik
 */

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
