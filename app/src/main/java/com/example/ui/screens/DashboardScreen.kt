package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.TransactionEntity
import com.example.data.model.DefaultExpenseCategories
import com.example.data.model.WalletSources
import com.example.ui.components.BudgetEnvelopeCard
import com.example.ui.components.TransactionCard
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.TimeFilter
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onTimeFilterChange: (TimeFilter) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onWalletFilterChange: (String?) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditBudgetLimit: (categoryName: String, currentLimit: Long, iconEmoji: String) -> Unit,
    onNavigateToEnvelopes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = DateTimeUtils.getCurrentJakartaTimeMillis()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: Brand Banner, Greeting, Jakarta Date & Time
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_arar_toz_logo),
                        contentDescription = "Logo ArAr TOZ",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ArAr ",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "TOZ",
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                color = Color(0xFFE5A22D)
                            )
                        }
                        Text(
                            text = "Catatan Kas & Belanja Keluarga",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Halo Bunda ✨",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${DateTimeUtils.formatDateWithDay(now)} (WIB)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isDailyReminderActive) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.12f),
                            modifier = Modifier.testTag("reminder_active_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = uiState.reminderTimeString,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Summary Hero Card: Saldo Bersih & Bulanan
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_hero_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmeraldPrimary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Saldo Bersih",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.totalNetBalance),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.25f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Hari Ini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.todayExpense),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = GreenBudgetSafe,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Pemasukan Bln Ini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.thisMonthIncome),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = CoralAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Belanja Bln Ini",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                )
                            }
                            Text(
                                text = CurrencyFormatter.formatRupiah(uiState.thisMonthExpense),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }


        // 3. Top Envelopes Preview
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pos Amplop Anggaran ✉️",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kontrol kuota belanja bulan ${DateTimeUtils.formatCurrentMonth()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onNavigateToEnvelopes) {
                        Text("Lihat Semua", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                val topEnvelopes = uiState.budgetEnvelopes.take(3)
                if (topEnvelopes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Belum ada pos anggaran. Ketuk 'Lihat Semua' untuk membuat pos amplop baru.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    topEnvelopes.forEach { env ->
                        BudgetEnvelopeCard(
                            envelope = env,
                            onEditLimit = {
                                onEditBudgetLimit(env.categoryName, env.monthlyLimit, env.iconEmoji)
                            }
                        )
                    }
                }
            }
        }

        // 4. Filter Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Catatan Belanja 🧾",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.selectedCategory != null || uiState.selectedWallet != null) {
                        TextButton(
                            onClick = {
                                onCategoryFilterChange(null)
                                onWalletFilterChange(null)
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Filter", fontSize = 12.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TimeFilter.entries.forEach { filter ->
                        val isSelected = uiState.timeFilter == filter
                        Surface(
                            onClick = { onTimeFilterChange(filter) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.weight(1f).testTag("time_filter_${filter.name}")
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedCategory == null,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("Semua Pos", fontSize = 11.sp) }
                    )
                    DefaultExpenseCategories.ALL.forEach { cat ->
                        val isSelected = uiState.selectedCategory == cat
                        val emoji = DefaultExpenseCategories.getIconEmoji(cat)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategoryFilterChange(if (isSelected) null else cat) },
                            label = { Text("$emoji $cat", fontSize = 11.sp) }
                        )
                    }
                    WalletSources.ALL.forEach { wallet ->
                        val isSelected = uiState.selectedWallet == wallet
                        FilterChip(
                            selected = isSelected,
                            onClick = { onWalletFilterChange(if (isSelected) null else wallet) },
                            label = { Text("💳 $wallet", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }

        // 5. Transaction List or Empty State
        if (uiState.filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🍃", fontSize = 42.sp)
                        Text(
                            text = "Belum ada transaksi di filter ini",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ketuk tombol '+ Catat' di bawah untuk mencatat transaksi keuangan Anda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.filteredTransactions, key = { it.id }) { tx ->
                TransactionCard(
                    transaction = tx,
                    onDelete = { onDeleteTransaction(tx) }
                )
            }
        }
    }
}
