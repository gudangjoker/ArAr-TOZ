package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BillEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.model.DebtTypes
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.theme.RedBudgetAlert
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils

@Composable
fun BillsAndDebtsScreen(
    uiState: DashboardUiState,
    onAddBillClick: () -> Unit,
    onPayBillClick: (BillEntity) -> Unit,
    onToggleBillStatus: (BillEntity) -> Unit,
    onDeleteBill: (BillEntity) -> Unit,
    onAddDebtClick: () -> Unit,
    onPayDebtClick: (DebtLoanEntity) -> Unit,
    onToggleDebtSettled: (DebtLoanEntity) -> Unit,
    onDeleteDebt: (DebtLoanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("bills_and_debts_screen")
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tagihan Rutin", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_bills")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Handshake, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hutang & Piutang", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_debts")
            )
        }

        if (selectedTab == 0) {
            val totalBillsEstimated = uiState.bills.sumOf { it.estimatedAmount }
            val paidBillsCount = uiState.bills.count { it.lastPaidMonthYear == currentMonthKey }
            val totalBillsCount = uiState.bills.size
            val unpaidBills = uiState.bills.filter { it.lastPaidMonthYear != currentMonthKey }
            val totalUnpaidEstimated = unpaidBills.sumOf { it.estimatedAmount }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bills_summary_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tagihan ${DateTimeUtils.formatCurrentMonth()}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (unpaidBills.isEmpty() && totalBillsCount > 0) GreenBudgetSafe.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$paidBillsCount dari $totalBillsCount Lunas",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unpaidBills.isEmpty() && totalBillsCount > 0) GreenBudgetSafe
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Perkiraan Total:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(CurrencyFormatter.formatRupiah(totalBillsEstimated), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Sisa Belum Dibayar:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalUnpaidEstimated),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (totalUnpaidEstimated > 0) RedBudgetAlert else GreenBudgetSafe
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddBillClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_bill_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Jadwal Tagihan Rutin", fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.bills.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada jadwal tagihan. Tambahkan tagihan seperti Listrik, Wi-Fi, Air, atau SPP.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.bills, key = { it.id }) { bill ->
                        val isPaidThisMonth = bill.lastPaidMonthYear == currentMonthKey
                        BillCardItem(
                            bill = bill,
                            isPaidThisMonth = isPaidThisMonth,
                            onPayClick = { onPayBillClick(bill) },
                            onToggleStatus = { onToggleBillStatus(bill) },
                            onDelete = { onDeleteBill(bill) }
                        )
                    }
                }
            }
        } else {
            var debtFilter by remember { mutableStateOf("SEMUA") }
            val payableList = uiState.debtLoans.filter { it.type == DebtTypes.PAYABLE }
            val receivableList = uiState.debtLoans.filter { it.type == DebtTypes.RECEIVABLE }
            val totalHutangSaya = payableList.filter { !it.isSettled }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }
            val totalPiutangSaya = receivableList.filter { !it.isSettled }.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }

            val displayedList = uiState.debtLoans.filter {
                when (debtFilter) {
                    "HUTANG" -> it.type == DebtTypes.PAYABLE
                    "PIUTANG" -> it.type == DebtTypes.RECEIVABLE
                    "BELUM_LUNAS" -> !it.isSettled
                    else -> true
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debts_summary_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Ringkasan Kewajiban & Piutang", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Hutang Saya (Kewajiban):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalHutangSaya),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalHutangSaya > 0) CoralAccent else GreenBudgetSafe
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Piutang (Uang di Orang):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalPiutangSaya),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddDebtClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_debt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Hutang / Piutang Baru", fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "SEMUA" to "Semua",
                            "HUTANG" to "Hutang Saya",
                            "PIUTANG" to "Piutang",
                            "BELUM_LUNAS" to "Belum Lunas"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = debtFilter == key,
                                onClick = { debtFilter = key },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.testTag("filter_debt_$key")
                            )
                        }
                    }
                }

                if (displayedList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada catatan hutang/piutang di filter ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(displayedList, key = { it.id }) { item ->
                        DebtLoanCardItem(
                            item = item,
                            onPayClick = { onPayDebtClick(item) },
                            onToggleSettled = { onToggleDebtSettled(item) },
                            onDelete = { onDeleteDebt(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillCardItem(
    bill: BillEntity,
    isPaidThisMonth: Boolean,
    onPayClick: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_item_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaidThisMonth) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPaidThisMonth) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPaidThisMonth,
                    onCheckedChange = { onToggleStatus() },
                    colors = CheckboxDefaults.colors(checkedColor = GreenBudgetSafe)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isPaidThisMonth) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "Jatuh tempo: Setiap tanggal ${bill.dueDayOfMonth} • ${bill.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bill.note.isNotBlank()) {
                        Text(
                            text = bill.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Perkiraan Nominal:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatRupiah(bill.estimatedAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaidThisMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (!isPaidThisMonth) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("pay_bill_btn_${bill.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bayar Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenBudgetSafe.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GreenBudgetSafe, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lunas Bulan Ini", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenBudgetSafe)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DebtLoanCardItem(
    item: DebtLoanEntity,
    onPayClick: () -> Unit,
    onToggleSettled: () -> Unit,
    onDelete: () -> Unit
) {
    val isPayable = item.type == DebtTypes.PAYABLE
    val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
    val progress = (item.paidAmount.toFloat() / item.totalAmount.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("debt_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSettled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSettled) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSettled,
                    onCheckedChange = { onToggleSettled() },
                    colors = CheckboxDefaults.colors(checkedColor = GreenBudgetSafe)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (item.isSettled) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPayable) CoralAccent.copy(alpha = 0.15f) else EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isPayable) "Hutang Saya" else "Piutang",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPayable) CoralAccent else EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (item.dueDateString.isNotBlank()) {
                        Text(
                            text = "Jatuh tempo: ${item.dueDateString}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isPayable) CoralAccent else EmeraldPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Terbayar: ${CurrencyFormatter.formatRupiah(item.paidAmount)} / ${CurrencyFormatter.formatRupiah(item.totalAmount)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Sisa: ${CurrencyFormatter.formatRupiah(remaining)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSettled) GreenBudgetSafe else if (isPayable) CoralAccent else EmeraldPrimary
                    )
                }
                if (!item.isSettled) {
                    Button(
                        onClick = onPayClick,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPayable) CoralAccent else EmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("pay_debt_btn_${item.id}")
                    ) {
                        Text(if (isPayable) "Cicil / Bayar" else "Terima Cicilan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GreenBudgetSafe.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Lunas ✨",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenBudgetSafe,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
