package com.example.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.model.DefaultExpenseCategories
import com.example.data.model.DefaultIncomeCategories
import com.example.data.model.TransactionTypes
import com.example.data.model.WalletSources
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionInputDialog(
    onDismissRequest: () -> Unit,
    onSaveTransaction: (type: String, amountExpression: String, category: String, wallet: String, note: String) -> Unit,
    customCategories: List<Pair<String, String>> = emptyList(),
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var transactionType by remember { mutableStateOf(TransactionTypes.EXPENSE) }
    var expressionInput by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(WalletSources.CASH) }
    var noteInput by remember { mutableStateOf("") }

    val availableExpenseCategories = remember(customCategories) {
        if (customCategories.isNotEmpty()) {
            customCategories + listOf("Lainnya" to "📦")
        } else {
            listOf(
                "Umum" to "📝",
                "Belanja" to "🛒",
                "Makanan" to "🍲",
                "Transport" to "🛵",
                "Lainnya" to "📦"
            )
        }
    }
    val availableIncomeCategories = remember {
        DefaultIncomeCategories.ALL.map { it to DefaultExpenseCategories.getIconEmoji(it) }
    }

    var selectedCategory by remember {
        mutableStateOf(availableExpenseCategories.firstOrNull()?.first ?: "Umum")
    }

    LaunchedEffect(transactionType) {
        val currentList = if (transactionType == TransactionTypes.EXPENSE) availableExpenseCategories else availableIncomeCategories
        if (currentList.none { it.first == selectedCategory }) {
            selectedCategory = currentList.firstOrNull()?.first ?: ""
        }
    }

    val evaluatedAmount by remember(expressionInput) {
        derivedStateOf {
            MathExpressionEvaluator.evaluateMathExpression(expressionInput)
        }
    }
    val isCompound by remember(expressionInput) {
        derivedStateOf {
            MathExpressionEvaluator.isCompoundExpression(expressionInput)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.testTag("transaction_input_sheet"),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Catat Transaksi Baru",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("close_dialog_btn")
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                }
            }

            // Type Selector (Pengeluaran vs Pemasukan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp)
            ) {
                Surface(
                    onClick = { transactionType = TransactionTypes.EXPENSE },
                    shape = RoundedCornerShape(10.dp),
                    color = if (transactionType == TransactionTypes.EXPENSE) CoralAccent else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_expense_btn")
                ) {
                    Text(
                        text = "Pengeluaran",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = if (transactionType == TransactionTypes.EXPENSE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Surface(
                    onClick = { transactionType = TransactionTypes.INCOME },
                    shape = RoundedCornerShape(10.dp),
                    color = if (transactionType == TransactionTypes.INCOME) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_income_btn")
                ) {
                    Text(
                        text = "Pemasukan",
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = if (transactionType == TransactionTypes.INCOME) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Amount Input with smart math calculator
            Column {
                OutlinedTextField(
                    value = expressionInput,
                    onValueChange = { expressionInput = it },
                    label = { Text("Nominal / Hitungan Otomatis") },
                    placeholder = { Text("Ketik 15000+23000 atau nominal langsung") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = EmeraldPrimary)
                    },
                    trailingIcon = {
                        if (expressionInput.isNotBlank()) {
                            IconButton(onClick = { expressionInput = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Hapus")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_expression_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isCompound) {
                    Text(
                        text = "⚡ Perhitungan penjumlahan otomatis aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Terhitung:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatRupiah(evaluatedAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (transactionType == TransactionTypes.EXPENSE) CoralAccent else EmeraldPrimary,
                        modifier = Modifier.testTag("calculated_total_display")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "+10 rb" to 10_000L,
                        "+20 rb" to 20_000L,
                        "+50 rb" to 50_000L,
                        "+100 rb" to 100_000L
                    ).forEach { (label, value) ->
                        Surface(
                            onClick = {
                                expressionInput = if (expressionInput.isBlank() || evaluatedAmount == 0L) {
                                    value.toString()
                                } else {
                                    "$expressionInput+$value"
                                }
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            modifier = Modifier.testTag("quick_add_$value")
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Wallet Chips
            Column {
                Text(
                    text = "Sumber Dana / Dompet:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WalletSources.ALL.forEach { wallet ->
                        val isSelected = selectedWallet == wallet
                        val iconStr = when (wallet) {
                            WalletSources.CASH -> "💵"
                            WalletSources.BANK -> "🏦"
                            else -> "📱"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWallet = wallet },
                            label = { Text("$iconStr $wallet") },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("wallet_chip_$wallet")
                        )
                    }
                }
            }

            // Categories Chips
            Column {
                Text(
                    text = if (transactionType == TransactionTypes.EXPENSE) "Pos Belanja / Kategori:" else "Sumber Pemasukan:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                val availableCategories = if (transactionType == TransactionTypes.EXPENSE) {
                    availableExpenseCategories
                } else {
                    availableIncomeCategories
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { (catName, emoji) ->
                        val isSelected = selectedCategory == catName
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catName },
                            label = { Text("$emoji $catName", fontSize = 12.sp) },
                            modifier = Modifier.testTag("category_chip_$catName")
                        )
                    }
                }
            }

            // Note Field
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                label = { Text("Catatan / Keterangan (Opsional)") },
                placeholder = { Text("Misal: Beli sayur pasar, bayar galon, dll") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_input_field"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Submit Button
            Button(
                onClick = {
                    if (evaluatedAmount > 0) {
                        onSaveTransaction(
                            transactionType,
                            expressionInput,
                            selectedCategory,
                            selectedWallet,
                            noteInput
                        )
                        onDismissRequest()
                    }
                },
                enabled = evaluatedAmount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_transaction_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transactionType == TransactionTypes.EXPENSE) CoralAccent else EmeraldPrimary
                )
            ) {
                Text(
                    text = "Simpan ${if (transactionType == TransactionTypes.EXPENSE) "Pengeluaran" else "Pemasukan"} (${CurrencyFormatter.formatRupiah(evaluatedAmount)})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
