package com.example.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEnvelopeItem
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator

private val PRESET_ICONS = listOf(
    "🥬", "🛒", "🍎", "🍔", "☕",
    "🏠", "💡", "💧", "🔥", "📱",
    "🎒", "🎓", "👶", "🧸", "💊",
    "🏥", "💄", "👔", "🚗", "⛽",
    "🛵", "🤲", "🎁", "✈️", "💰", "📝"
)

private val QUICK_AMOUNTS = listOf(
    "Rp 200 rb" to 200_000L,
    "Rp 300 rb" to 300_000L,
    "Rp 500 rb" to 500_000L,
    "Rp 1 jt" to 1_000_000L,
    "Rp 1,5 jt" to 1_500_000L,
    "Rp 2 jt" to 2_000_000L,
    "Rp 3 jt" to 3_000_000L,
    "Rp 5 jt" to 5_000_000L
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetEnvelopeDialog(
    initialEnvelope: BudgetEnvelopeItem? = null,
    onDismissRequest: () -> Unit,
    onSave: (name: String, limitExp: String, iconEmoji: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val isEditMode = initialEnvelope != null

    var name by remember { mutableStateOf(initialEnvelope?.categoryName ?: "") }
    var selectedEmoji by remember { mutableStateOf(initialEnvelope?.iconEmoji ?: "💰") }
    var limitExp by remember {
        mutableStateOf(
            if (initialEnvelope != null && initialEnvelope.monthlyLimit > 0) {
                initialEnvelope.monthlyLimit.toString()
            } else ""
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val calculatedLimit by remember(limitExp) {
        derivedStateOf { MathExpressionEvaluator.evaluateMathExpression(limitExp) }
    }

    val isNameValid = name.trim().isNotBlank()
    val isLimitValid = calculatedLimit > 0
    val canSave = isNameValid && isLimitValid

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("budget_envelope_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = selectedEmoji, fontSize = 20.sp)
                    }
                }
                Text(
                    text = if (isEditMode) "Kelola Pos Anggaran" else "Tambah Pos Anggaran Baru",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Nama Pos Anggaran
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Pos Anggaran *") },
                        placeholder = { Text("Contoh: Belanja Sayur, Skincare") },
                        isError = name.isNotBlank() && !isNameValid,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_budget_name"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (name.isBlank()) {
                        Text(
                            text = "Nama pos anggaran wajib diisi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 2. Pemilihan Ikon Pos (Visual Icon Picker)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Pilih Ikon Pos",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PRESET_ICONS.forEach { emoji ->
                            val isSelected = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) EmeraldPrimary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) EmeraldPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedEmoji = emoji }
                                    .testTag("icon_picker_$emoji"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // 3. Batas Nominal Anggaran
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = limitExp,
                        onValueChange = { limitExp = it },
                        label = { Text("Batas Kuota Anggaran Bulanan *") },
                        placeholder = { Text("Contoh: 500000 atau 250000*2") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_budget_limit"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Real-time Rupiah calculation preview
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (calculatedLimit > 0) EmeraldPrimary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (calculatedLimit > 0) "Nominal: ${CurrencyFormatter.formatRupiah(calculatedLimit)} / bulan"
                            else "Masukkan nominal batas anggaran yang valid (> Rp 0)",
                            fontSize = 12.sp,
                            fontWeight = if (calculatedLimit > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (calculatedLimit > 0) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_AMOUNTS.forEach { (label, value) ->
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
                }

                // 4. Periode Anggaran
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Periode: Bulanan (WIB) — Kuota direset otomatis setiap awal bulan",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 5. Tombol Hapus (Khusus Mode Edit)
                if (isEditMode && onDelete != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_delete_budget_pos")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hapus Pos Anggaran Ini", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(name.trim(), calculatedLimit.toString(), selectedEmoji)
                    }
                },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_save_budget_pos")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEditMode) "Simpan Perubahan" else "Simpan Pos Anggaran",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("btn_cancel_budget_pos")
            ) {
                Text("Batal")
            }
        }
    )

    // Konfirmasi Hapus Pos Anggaran
    if (showDeleteConfirm && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Hapus Pos Anggaran?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Apakah Bunda yakin ingin menghapus pos anggaran '$name'?\n\nCatatan riwayat transaksi belanja Anda tetap aman dan tidak akan terhapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_confirm_delete_pos")
                ) {
                    Text("Ya, Hapus Pos", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
