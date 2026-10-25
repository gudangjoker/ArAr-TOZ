package com.example.ui.dialog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

@Composable
fun EditBudgetDialog(
    categoryName: String,
    currentLimit: Long,
    iconEmoji: String,
    onDismissRequest: () -> Unit,
    onSaveLimit: (newLimitExp: String) -> Unit
) {
    var limitExp by remember { mutableStateOf(currentLimit.toString()) }
    val calculatedLimit by remember(limitExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(limitExp) }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("edit_budget_dialog"),
        title = {
            Text(
                text = "Atur Batas Anggaran $iconEmoji",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Pos: $categoryName",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Tentukan kuota pengeluaran maksimal per bulan untuk pos ini. Status warna (Hijau, Kuning, Merah) akan otomatis menyesuaikan pengeluaran belanja.",
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
                        "Rp 300 rb" to 300_000L,
                        "Rp 500 rb" to 500_000L,
                        "Rp 1 jt" to 1_000_000L,
                        "Rp 1,5 jt" to 1_500_000L,
                        "Rp 2 jt" to 2_000_000L
                    ).forEach { (label, value) ->
                        Surface(
                            onClick = { limitExp = value.toString() },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = limitExp,
                    onValueChange = { limitExp = it },
                    label = { Text("Batas Bulanan (Bisa Kalkulator)") },
                    placeholder = { Text("Contoh: 1500000 atau 1200000+300000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )
                if (calculatedLimit > 0) {
                    Text(
                        text = "= ${CurrencyFormatter.formatRupiah(calculatedLimit)} per bulan",
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
                    if (calculatedLimit > 0) {
                        onSaveLimit(limitExp)
                        onDismissRequest()
                    }
                },
                enabled = calculatedLimit > 0,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_budget_limit_btn")
            ) {
                Text("Simpan Batas")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text("Batal")
            }
        }
    )
}
