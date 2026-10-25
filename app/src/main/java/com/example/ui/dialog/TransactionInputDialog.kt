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
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var transactionType by remember { mutableStateOf(TransactionTypes.EXPENSE) }
    var expressionInput by remember { mutableStateOf("") }
    var selectedWallet by remember { mutableStateOf(WalletSources.CASH) }
    var selectedCategory by remember { mutableStateOf(DefaultExpenseCategories.DAPUR) }
    var noteInput by remember { mutableStateOf("") }

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

            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isExpense = transactionType == TransactionTypes.EXPENSE
                Surface(
                    onClick = {
                        transactionType = TransactionTypes.EXPENSE
                        selectedCategory = DefaultExpenseCategories.DAPUR
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_expense_btn"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isExpense) CoralAccent else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (isExpense) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💸 Pengeluaran",
                            fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Medium,
                            color = if (isExpense) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    onClick = {
                        transactionType = TransactionTypes.INCOME
                        selectedCategory = DefaultIncomeCategories.NAFKAH
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("type_income_btn"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isExpense) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = if (!isExpense) 4.dp else 0.dp
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💰 Pemasukan",
                            fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isExpense) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Mini-Calculator Amount Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Kalkulator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nominal (Mendukung Kalkulator)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (isCompound) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary,
                            contentColor = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "Kalkulator Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = expressionInput,
                    onValueChange = { expressionInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_input_field"),
                    placeholder = { Text("Contoh: 15000+8000+12000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    DefaultExpenseCategories.ALL
                } else {
                    DefaultIncomeCategories.ALL
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableCategories.forEach { category ->
                        val isSelected = selectedCategory == category
                        val emoji = DefaultExpenseCategories.getIconEmoji(category)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = { Text("$emoji $category", fontSize = 12.sp) },
                            modifier = Modifier.testTag("category_chip_$category")
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
