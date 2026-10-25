package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

private val ChartPalette = listOf(
    Color(0xFF2E7D32), // Emerald
    Color(0xFF1976D2), // Blue
    Color(0xFFE65100), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF00897B), // Teal
    Color(0xFFD81B60), // Pink
    Color(0xFFFDD835), // Yellow
    Color(0xFF5D4037)  // Brown
)

@Composable
fun AnalyticsScreen(
    uiState: DashboardUiState,
    onShareRekapToWhatsApp: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalExpense = uiState.thisMonthExpense
    val lastMonthExpense = uiState.lastMonthExpense
    val totalIncome = uiState.thisMonthIncome
    val isDeficit = totalExpense > totalIncome
    val netSavingsOrDeficit = if (isDeficit) totalExpense - totalIncome else totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0 && !isDeficit) {
        ((netSavingsOrDeficit.toFloat() / totalIncome.toFloat()) * 100).toInt()
    } else 0

    val categoryBreakdown = uiState.budgetEnvelopes
        .filter { it.spentAmount > 0 }
        .sortedByDescending { it.spentAmount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Grafik & Analisis Keuangan 📊",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Laporan belanja bulan ${DateTimeUtils.formatCurrentMonth()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onShareRekapToWhatsApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = GreenBudgetSafe.copy(alpha = 0.15f),
                        contentColor = GreenBudgetSafe
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("analytics_share_rekap_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Bagikan Rekap",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Month-over-Month Comparison Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("month_comparison_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Perbandingan Tren Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bulan Lalu:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatRupiah(lastMonthExpense),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Bulan Berjalan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CurrencyFormatter.formatRupiah(totalExpense),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    if (lastMonthExpense > 0) {
                        val diff = totalExpense - lastMonthExpense
                        val isMoreSaving = diff <= 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isMoreSaving) GreenBudgetSafe.copy(alpha = 0.12f) else CoralAccent.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isMoreSaving) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (isMoreSaving) GreenBudgetSafe else CoralAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isMoreSaving) "Lebih hemat ${CurrencyFormatter.formatRupiah(-diff)} dibanding bulan lalu! 🎉"
                                    else "Lebih tinggi ${CurrencyFormatter.formatRupiah(diff)} dibanding bulan lalu",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMoreSaving) GreenBudgetSafe else CoralAccent
                                )
                            }
                        }
                    }

                    // Savings rate bar / Deficit alert
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isDeficit) "Status Kas Bulan Ini: Defisit" else "Tingkat Simpanan / Sisa Kas:",
                                fontSize = 11.sp,
                                color = if (isDeficit) CoralAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isDeficit) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (isDeficit) {
                                    "- ${CurrencyFormatter.formatRupiah(netSavingsOrDeficit)}"
                                } else {
                                    "$savingsRate% dari Pemasukan"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeficit) CoralAccent else EmeraldPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { if (isDeficit) 1f else (savingsRate / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (isDeficit) CoralAccent else EmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // 2. Visual Donut Chart & Category Breakdown
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("category_chart_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Komposisi Pengeluaran per Kategori",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (categoryBreakdown.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada catatan pengeluaran di bulan ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Visual Donut Chart Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(140.dp)) {
                                var startAngle = -90f
                                categoryBreakdown.forEachIndexed { index, item ->
                                    val sweep = if (totalExpense > 0) (item.spentAmount.toFloat() / totalExpense.toFloat()) * 360f else 0f
                                    val color = ChartPalette[index % ChartPalette.size]
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Total Belanja",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.formatRupiah(totalExpense),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Breakdown Items
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categoryBreakdown.forEachIndexed { index, item ->
                                val color = ChartPalette[index % ChartPalette.size]
                                val percent = if (totalExpense > 0) ((item.spentAmount.toFloat() / totalExpense.toFloat()) * 100).toInt() else 0
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${item.iconEmoji} ${item.categoryName}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Text(
                                            text = "${CurrencyFormatter.formatRupiah(item.spentAmount)} ($percent%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = color,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Share Rekap Kas via WhatsApp Card
        item {
            Card(
                onClick = onShareRekapToWhatsApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analytics_share_rekap_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = GreenBudgetSafe.copy(alpha = 0.12f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = GreenBudgetSafe,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Kirim Rekap Kas ke Pasangan 📲",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Format teks rapi via WhatsApp (saldo, belanja bulan ini, amplop & tagihan)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenBudgetSafe
                    ) {
                        Text(
                            text = "Kirim",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}
