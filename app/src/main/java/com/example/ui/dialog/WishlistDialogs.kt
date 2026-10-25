package com.example.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.example.data.entity.WishlistEntity
import com.example.data.model.WishlistCategories
import com.example.data.model.WishlistPriorities
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

@Composable
fun AddWishlistDialog(
    onDismissRequest: () -> Unit,
    onSaveWishlist: (
        name: String,
        targetAmountExp: String,
        actualPriceExp: String,
        initialSavedExp: String,
        isSavingGoal: Boolean,
        categoryType: String,
        priority: String,
        quantityOrUnit: String,
        note: String
    ) -> Unit
) {
    var isSavingGoal by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var quantityOrUnit by remember { mutableStateOf("") }
    var targetExp by remember { mutableStateOf("") }
    var actualExp by remember { mutableStateOf("") }
    var initialSavedExp by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(WishlistCategories.KITCHEN) }
    var selectedPriority by remember { mutableStateOf(WishlistPriorities.HIGH) }
    var note by remember { mutableStateOf("") }

    val calculatedTarget by remember(targetExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(targetExp) }
    }
    val calculatedActual by remember(actualExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(actualExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("add_wishlist_dialog"),
        title = {
            Text(
                text = if (isSavingGoal) "Target Impian / Tabungan 🎯" else "Daftar Belanja Dapur / Rutin 🥬",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = {
                            isSavingGoal = false
                            selectedCategory = WishlistCategories.KITCHEN
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isSavingGoal) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🥬 Belanja Dapur",
                                fontSize = 12.sp,
                                fontWeight = if (!isSavingGoal) FontWeight.Bold else FontWeight.Normal,
                                color = if (!isSavingGoal) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            isSavingGoal = true
                            selectedCategory = WishlistCategories.HOUSEHOLD
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSavingGoal) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎯 Target Impian",
                                fontSize = 12.sp,
                                fontWeight = if (isSavingGoal) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSavingGoal) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Barang / Belanjaan *") },
                    placeholder = { Text(if (isSavingGoal) "Misal: Air Fryer, Mesin Cuci" else "Misal: Telur 1kg, Sayur Bayam, Minyak") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wishlist_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantityOrUnit,
                    onValueChange = { quantityOrUnit = it },
                    label = { Text("Jumlah / Satuan (Opsional)") },
                    placeholder = { Text("Contoh: 1 kg, 2 ikat, 1 pack, 1 unit") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Column {
                    OutlinedTextField(
                        value = targetExp,
                        onValueChange = { targetExp = it },
                        label = { Text("Estimasi Harga (Opsional)") },
                        placeholder = { Text("Patokan harga (cth: 35000 atau 15000+20000)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wishlist_target_input"),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (calculatedTarget > 0) {
                        Text(
                            text = "= ${CurrencyFormatter.formatRupiah(calculatedTarget)} (sebagai estimasi pembanding)",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                Column {
                    OutlinedTextField(
                        value = actualExp,
                        onValueChange = { actualExp = it },
                        label = { Text("Harga Sebenarnya / Beli (Opsional)") },
                        placeholder = { Text("Isi jika sudah tahu harga realnya") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        singleLine = true
                    )
                    if (calculatedActual > 0) {
                        Text(
                            text = "= ${CurrencyFormatter.formatRupiah(calculatedActual)} (harga real)",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenBudgetSafe,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }

                if (isSavingGoal) {
                    OutlinedTextField(
                        value = initialSavedExp,
                        onValueChange = { initialSavedExp = it },
                        label = { Text("Tabungan Awal Terkumpul (Opsional)") },
                        placeholder = { Text("0") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }

                Column {
                    Text(
                        text = "Kategori Pos:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        WishlistCategories.ALL.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Prioritas:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        WishlistPriorities.ALL.forEach { prio ->
                            FilterChip(
                                selected = selectedPriority == prio,
                                onClick = { selectedPriority = prio },
                                label = { Text(prio, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / Keterangan Toko (Opsional)") },
                    placeholder = { Text("Misal: Beli di pasar induk lapak bu Ani") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSaveWishlist(
                            name,
                            targetExp,
                            actualExp,
                            initialSavedExp.ifBlank { "0" },
                            isSavingGoal,
                            selectedCategory,
                            selectedPriority,
                            quantityOrUnit,
                            note
                        )
                        onDismissRequest()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_wishlist_btn")
            ) {
                Text(if (isSavingGoal) "Simpan Target" else "Simpan ke Daftar Belanja")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun EditActualPriceDialog(
    wishlist: WishlistEntity,
    onDismissRequest: () -> Unit,
    onSaveActualPrice: (actualPriceExp: String, recordAsExpense: Boolean) -> Unit
) {
    var priceExp by remember { mutableStateOf(if (wishlist.actualPrice > 0) wishlist.actualPrice.toString() else "") }
    var recordAsExpense by remember { mutableStateOf(true) }
    val calculatedPrice by remember(priceExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(priceExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("edit_actual_price_dialog"),
        title = {
            Text(
                text = "Catat Harga Beli Sebenarnya 🛒",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Barang: ${wishlist.name} ${if (wishlist.quantityOrUnit.isNotBlank()) "(${wishlist.quantityOrUnit})" else ""}",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (wishlist.targetAmount > 0) {
                    Text(
                        text = "Estimasi patokan awal: ${CurrencyFormatter.formatRupiah(wishlist.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = priceExp,
                    onValueChange = { priceExp = it },
                    label = { Text("Harga Sebenarnya (Bisa Kalkulator)") },
                    placeholder = { Text("Contoh: 32000 atau 15000+17000") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                if (calculatedPrice > 0) {
                    Text(
                        text = "= ${CurrencyFormatter.formatRupiah(calculatedPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GreenBudgetSafe
                    )
                    if (wishlist.targetAmount > 0) {
                        val diff = wishlist.targetAmount - calculatedPrice
                        Text(
                            text = if (diff > 0) "Lebih hemat ${CurrencyFormatter.formatRupiah(diff)} dari estimasi! 💰"
                            else if (diff < 0) "Lebih mahal ${CurrencyFormatter.formatRupiah(-diff)} dari estimasi ⚠️"
                            else "Tepat sesuai estimasi harga ✨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (diff >= 0) GreenBudgetSafe else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordAsExpense,
                        onCheckedChange = { recordAsExpense = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Sekaligus catat ke pengeluaran belanja dapur & tandai sudah dibeli",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveActualPrice(priceExp, recordAsExpense)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Simpan Harga")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun AllocateSavingsDialog(
    wishlist: WishlistEntity,
    onDismissRequest: () -> Unit,
    onAllocate: (amountExp: String) -> Unit
) {
    var amountExp by remember { mutableStateOf("") }
    val calculatedAmount by remember(amountExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(amountExp) }
    }
    val remainingNeeded = (wishlist.targetAmount - wishlist.savedAmount).coerceAtLeast(0L)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("allocate_savings_dialog"),
        title = {
            Text(
                text = "Alokasikan Tabungan 🎯",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Untuk '${wishlist.name}'",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Terkumpul: ${CurrencyFormatter.formatRupiah(wishlist.savedAmount)} dari ${CurrencyFormatter.formatRupiah(wishlist.targetAmount)} (Kurang ${CurrencyFormatter.formatRupiah(remainingNeeded)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "+20 rb" to 20_000L,
                        "+50 rb" to 50_000L,
                        "+100 rb" to 100_000L,
                        "+200 rb" to 200_000L,
                        "Lunas" to remainingNeeded
                    ).forEach { (label, value) ->
                        Surface(
                            onClick = {
                                amountExp = value.toString()
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = amountExp,
                    onValueChange = { amountExp = it },
                    label = { Text("Nominal Tambahan (Rp)") },
                    placeholder = { Text("Contoh: 50000 atau 25000+25000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("allocate_amount_input")
                )
                if (calculatedAmount > 0) {
                    Text(
                        text = "Akan ditambahkan: ${CurrencyFormatter.formatRupiah(calculatedAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (calculatedAmount > 0) {
                        onAllocate(amountExp)
                        onDismissRequest()
                    }
                },
                enabled = calculatedAmount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_allocate_btn")
            ) {
                Text("Tambahkan Tabungan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}
