package com.example.ui.dialog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.BillEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.model.DebtTypes
import com.example.data.model.DefaultExpenseCategories
import com.example.data.model.WalletSources
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

@Composable
fun AddBillDialog(
    onDismissRequest: () -> Unit,
    onSaveBill: (title: String, amountExp: String, dueDay: Int, category: String, wallet: String, note: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountExp by remember { mutableStateOf("") }
    var dueDayStr by remember { mutableStateOf("10") }
    var selectedCategory by remember { mutableStateOf(DefaultExpenseCategories.LISTRIK) }
    var selectedWallet by remember { mutableStateOf(WalletSources.BANK) }
    var note by remember { mutableStateOf("") }

    val calculatedAmount by remember(amountExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(amountExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("add_bill_dialog"),
        title = {
            Text("Tambah Jadwal Tagihan Rutin 📅", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nama Tagihan *") },
                    placeholder = { Text("Cth: Listrik PLN, Wi-Fi, Air PDAM, BPJS") },
                    modifier = Modifier.fillMaxWidth().testTag("bill_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = amountExp,
                        onValueChange = { amountExp = it },
                        label = { Text("Perkiraan Nominal (Bisa Hitung)") },
                        placeholder = { Text("Cth: 250000 atau 150000+100000") },
                        modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (calculatedAmount > 0) {
                        Text(
                            text = "= ${CurrencyFormatter.formatRupiah(calculatedAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDayStr,
                    onValueChange = { dueDayStr = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("Jatuh Tempo (Tanggal 1-31)") },
                    placeholder = { Text("10") },
                    modifier = Modifier.fillMaxWidth().testTag("bill_due_day_input"),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Column {
                    Text("Pos Kategori:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            DefaultExpenseCategories.LISTRIK,
                            DefaultExpenseCategories.ANAK,
                            DefaultExpenseCategories.SEDEKAH,
                            DefaultExpenseCategories.RUMAH,
                            DefaultExpenseCategories.LAINNYA
                        ).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column {
                    Text("Sumber Dompet:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WalletSources.ALL.forEach { w ->
                            FilterChip(
                                selected = selectedWallet == w,
                                onClick = { selectedWallet = w },
                                label = { Text(w, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / No. Pelanggan (Opsional)") },
                    placeholder = { Text("Cth: No Pelanggan 5321-xxxx") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val day = dueDayStr.toIntOrNull() ?: 1
                        onSaveBill(title, amountExp, day, selectedCategory, selectedWallet, note)
                        onDismissRequest()
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_bill_btn")
            ) {
                Text("Simpan Tagihan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun PayBillDialog(
    bill: BillEntity,
    onDismissRequest: () -> Unit,
    onConfirmPayment: (actualAmountExp: String, walletSource: String) -> Unit
) {
    var amountExp by remember { mutableStateOf(bill.estimatedAmount.toString()) }
    var selectedWallet by remember { mutableStateOf(bill.walletSource) }

    val calculatedAmount by remember(amountExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(amountExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("pay_bill_dialog"),
        title = {
            Text("Bayar Tagihan: ${bill.title} 💳", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Akan otomatis dicatat sebagai pengeluaran bulan ini dan menandai tagihan lunas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountExp,
                    onValueChange = { amountExp = it },
                    label = { Text("Nominal Pembayaran Real (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                if (calculatedAmount > 0) {
                    Text(
                        text = "= ${CurrencyFormatter.formatRupiah(calculatedAmount)}",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Column {
                    Text("Bayar Menggunakan:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WalletSources.ALL.forEach { w ->
                            FilterChip(
                                selected = selectedWallet == w,
                                onClick = { selectedWallet = w },
                                label = { Text(w, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmPayment(amountExp, selectedWallet)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Konfirmasi Bayar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun AddDebtLoanDialog(
    onDismissRequest: () -> Unit,
    onSaveDebtLoan: (personName: String, type: String, amountExp: String, dueDate: String, note: String) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DebtTypes.PAYABLE) }
    var amountExp by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val calculatedAmount by remember(amountExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(amountExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("add_debt_dialog"),
        title = {
            Text(
                text = if (selectedType == DebtTypes.PAYABLE) "Catat Hutang Saya (Kewajiban) ⚠️" else "Catat Piutang (Uang di Orang) 🤝",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == DebtTypes.PAYABLE,
                        onClick = { selectedType = DebtTypes.PAYABLE },
                        label = { Text("Saya Berhutang") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == DebtTypes.RECEIVABLE,
                        onClick = { selectedType = DebtTypes.RECEIVABLE },
                        label = { Text("Piutang Saya") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = { Text(if (selectedType == DebtTypes.PAYABLE) "Hutang Kepada Siapa? *" else "Nama Orang yang Meminjam? *") },
                    placeholder = { Text("Cth: Tante Rina, Mas Budi, Bu RT") },
                    modifier = Modifier.fillMaxWidth().testTag("debt_person_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = amountExp,
                        onValueChange = { amountExp = it },
                        label = { Text("Total Nominal (Rp) *") },
                        placeholder = { Text("Cth: 200000 atau 100000+100000") },
                        modifier = Modifier.fillMaxWidth().testTag("debt_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (calculatedAmount > 0) {
                        Text(
                            text = "= ${CurrencyFormatter.formatRupiah(calculatedAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Janji Jatuh Tempo / Tanggal (Opsional)") },
                    placeholder = { Text("Cth: Akhir bulan, Tgl 25 Okt") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan / Keperluan (Opsional)") },
                    placeholder = { Text("Cth: Titip beli seragam sekolah") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (personName.isNotBlank() && calculatedAmount > 0) {
                        onSaveDebtLoan(personName, selectedType, amountExp, dueDate, note)
                        onDismissRequest()
                    }
                },
                enabled = personName.isNotBlank() && calculatedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == DebtTypes.PAYABLE) CoralAccent else EmeraldPrimary),
                modifier = Modifier.testTag("save_debt_btn")
            ) {
                Text("Simpan Catatan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun PayDebtDialog(
    item: DebtLoanEntity,
    onDismissRequest: () -> Unit,
    onConfirmPayment: (paymentExp: String, recordAsTransaction: Boolean, walletSource: String) -> Unit
) {
    val remaining = (item.totalAmount - item.paidAmount).coerceAtLeast(0L)
    var paymentExp by remember { mutableStateOf(remaining.toString()) }
    var recordAsTx by remember { mutableStateOf(true) }
    var selectedWallet by remember { mutableStateOf(WalletSources.CASH) }

    val calculatedPayment by remember(paymentExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(paymentExp) }
    }

    val isPayable = item.type == DebtTypes.PAYABLE

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("pay_debt_dialog"),
        title = {
            Text(
                text = if (isPayable) "Cicil / Bayar Hutang ke ${item.personName}" else "Terima Cicilan Piutang dari ${item.personName}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sisa yang belum lunas: ${CurrencyFormatter.formatRupiah(remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = paymentExp,
                    onValueChange = { paymentExp = it },
                    label = { Text("Nominal yang Dibayarkan (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                if (calculatedPayment > 0) {
                    Text(
                        text = "= ${CurrencyFormatter.formatRupiah(calculatedPayment)}",
                        fontWeight = FontWeight.Bold,
                        color = if (isPayable) CoralAccent else EmeraldPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordAsTx,
                        onCheckedChange = { recordAsTx = it },
                        colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPayable) "Catat sebagai pengeluaran kas" else "Catat sebagai pemasukan kas",
                        fontSize = 12.sp
                    )
                }

                if (recordAsTx) {
                    Column {
                        Text("Dompet yang Digunakan:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WalletSources.ALL.forEach { w ->
                                FilterChip(
                                    selected = selectedWallet == w,
                                    onClick = { selectedWallet = w },
                                    label = { Text(w, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (calculatedPayment > 0) {
                        onConfirmPayment(paymentExp, recordAsTx, selectedWallet)
                        onDismissRequest()
                    }
                },
                enabled = calculatedPayment > 0,
                colors = ButtonDefaults.buttonColors(containerColor = if (isPayable) CoralAccent else EmeraldPrimary)
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}

@Composable
fun AddPriceTrackDialog(
    onDismissRequest: () -> Unit,
    onSavePriceTrack: (name: String, priceExp: String, unit: String, store: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceExp by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("1 kg") }
    var store by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val calculatedPrice by remember(priceExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(priceExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("add_price_track_dialog"),
        title = {
            Text("Catat Patokan Harga Pasar 🛒", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Bahan / Barang *") },
                    placeholder = { Text("Cth: Minyak 2L, Telur 1kg, Beras 5kg") },
                    modifier = Modifier.fillMaxWidth().testTag("price_track_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = priceExp,
                        onValueChange = { priceExp = it },
                        label = { Text("Harga Saat Ini (Rp) *") },
                        placeholder = { Text("Cth: 34000") },
                        modifier = Modifier.fillMaxWidth().testTag("price_track_amount_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (calculatedPrice > 0) {
                        Text(
                            text = "= ${CurrencyFormatter.formatRupiah(calculatedPrice)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenBudgetSafe,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Satuan (Cth: 1 kg, 2 Liter, 1 ikat)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = store,
                    onValueChange = { store = it },
                    label = { Text("Nama Toko / Pasar (Opsional)") },
                    placeholder = { Text("Cth: Warung Bu Siti, Pasar Minggu") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Kualitas / Merk (Opsional)") },
                    placeholder = { Text("Cth: Merk Sania promo tgl muda") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && calculatedPrice > 0) {
                        onSavePriceTrack(name, priceExp, unit, store, note)
                        onDismissRequest()
                    }
                },
                enabled = name.isNotBlank() && calculatedPrice > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Simpan Pantauan Harga")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) { Text("Batal") }
        }
    )
}
