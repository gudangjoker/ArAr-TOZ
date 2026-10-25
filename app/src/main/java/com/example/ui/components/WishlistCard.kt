package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.WishlistEntity
import com.example.data.model.WishlistCategories
import com.example.data.model.WishlistPriorities
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.theme.RedBudgetAlert
import com.example.util.CurrencyFormatter

@Composable
fun WishlistCard(
    item: WishlistEntity,
    onToggleFulfilled: () -> Unit,
    onAllocateSavingsClick: () -> Unit,
    onEditActualPriceClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKitchen = item.categoryType == WishlistCategories.KITCHEN
    val isPersonal = item.categoryType == WishlistCategories.PERSONAL
    val priorityColor = when (item.priority) {
        WishlistPriorities.HIGH -> CoralAccent
        WishlistPriorities.MEDIUM -> AmberWarning
        else -> Color(0xFF757575)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("wishlist_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isFulfilled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isFulfilled) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Checkbox + Title + Category Badge + Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox: Barang sudah dibeli / lunas
                Checkbox(
                    checked = item.isFulfilled,
                    onCheckedChange = {
                        if (!item.isFulfilled && item.actualPrice <= 0L && item.targetAmount <= 0L) {
                            onEditActualPriceClick()
                        } else {
                            onToggleFulfilled()
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = GreenBudgetSafe,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("checkbox_bought_${item.id}")
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Item Name + Quantity / Unit
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isFulfilled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (item.isFulfilled) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (item.quantityOrUnit.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = item.quantityOrUnit,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (item.note.isNotBlank()) {
                        Text(
                            text = item.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("delete_wishlist_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Row 2: Badges (Belanja Dapur vs Impian + Prioritas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sumber Pos Anggaran Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldPrimary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "✉️ Pos: ${item.categoryType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Is Saving Goal Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isSavingGoal) EmeraldPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (item.isSavingGoal) "🎯 Target Tabungan" else "⚡ Belanja Langsung",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isSavingGoal) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Priority Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = priorityColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = item.priority,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            // Row 3: Price Comparison (Estimasi vs Harga Sebenarnya)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estimasi
                    Column {
                        Text(
                            text = "Estimasi Harga:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (item.targetAmount > 0) CurrencyFormatter.formatRupiah(item.targetAmount) else "Belum diisi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.targetAmount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Harga Sebenarnya (Real)
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Harga Sebenarnya:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = onEditActualPriceClick,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Edit harga beli",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Text(
                            text = if (item.actualPrice > 0) CurrencyFormatter.formatRupiah(item.actualPrice) else "+ Masukkan harga",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.actualPrice > 0) GreenBudgetSafe else EmeraldPrimary
                        )
                    }
                }
            }

            // Comparison badge if both Estimasi & Harga Sebenarnya are present
            if (item.targetAmount > 0 && item.actualPrice > 0) {
                val diff = item.targetAmount - item.actualPrice
                val (badgeText, badgeColor, badgeBg) = when {
                    diff > 0 -> Triple("Hemat ${CurrencyFormatter.formatRupiah(diff)} dari estimasi! 💰", GreenBudgetSafe, GreenBudgetSafe.copy(alpha = 0.12f))
                    diff < 0 -> Triple("Lebih mahal ${CurrencyFormatter.formatRupiah(-diff)} dari estimasi", RedBudgetAlert, RedBudgetAlert.copy(alpha = 0.12f))
                    else -> Triple("Tepat sesuai estimasi harga", EmeraldPrimary, EmeraldPrimary.copy(alpha = 0.12f))
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // Saving Goal Section (Hanya tampil jika item merupakan Target Tabungan)
            if (item.isSavingGoal && item.targetAmount > 0) {
                val progressFraction = (item.savedAmount.toFloat() / item.targetAmount.toFloat()).coerceIn(0f, 1f)
                val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "saving_prog")
                val remaining = (item.targetAmount - item.savedAmount).coerceAtLeast(0L)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldPrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tabungan: ${CurrencyFormatter.formatRupiah(item.savedAmount)} (${(progressFraction * 100).toInt()}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        FilledTonalButton(
                            onClick = onAllocateSavingsClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("allocate_btn_${item.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Alokasi Tabungan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Bought status banner
            if (item.isFulfilled) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenBudgetSafe.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = GreenBudgetSafe, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Sudah Dibeli / Terpenuhi" + if (item.actualPrice > 0) " (${CurrencyFormatter.formatRupiah(item.actualPrice)})" else "",
                            color = GreenBudgetSafe,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
