package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetEnvelopeItem
import com.example.data.model.BudgetEnvelopeStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.theme.RedBudgetAlert
import com.example.util.CurrencyFormatter

@Composable
fun BudgetEnvelopeCard(
    envelope: BudgetEnvelopeItem,
    onEditLimit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressAnimated by animateFloatAsState(
        targetValue = envelope.percentage.coerceIn(0f, 1f),
        label = "budget_progress"
    )

    val (statusColor, statusText, statusBg) = when {
        envelope.monthlyLimit <= 0L && envelope.spentAmount > 0L -> Triple(
            AmberWarning,
            "Tanpa Kuota",
            AmberWarning.copy(alpha = 0.15f)
        )
        envelope.monthlyLimit <= 0L -> Triple(
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Belum Diatur",
            MaterialTheme.colorScheme.surfaceVariant
        )
        envelope.status == BudgetEnvelopeStatus.GREEN -> Triple(
            GreenBudgetSafe,
            "Aman (<70%)",
            GreenBudgetSafe.copy(alpha = 0.12f)
        )
        envelope.status == BudgetEnvelopeStatus.YELLOW -> Triple(
            AmberWarning,
            "Waspada (70-90%)",
            AmberWarning.copy(alpha = 0.15f)
        )
        else -> Triple(
            RedBudgetAlert,
            if (envelope.remainingAmount < 0) "Kelewat Kuota!" else "Kritis (>90%)",
            RedBudgetAlert.copy(alpha = 0.15f)
        )
    }

    val animatedColor by animateColorAsState(targetValue = statusColor, label = "color_anim")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_envelope_card_${envelope.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Emoji + Category + Edit Button + Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(statusBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = envelope.iconEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = envelope.categoryName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Batas: ${CurrencyFormatter.formatRupiah(envelope.monthlyLimit)}/bln",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = animatedColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onEditLimit,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("edit_budget_${envelope.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Ubah batas anggaran",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Visual Progress Bar
            LinearProgressIndicator(
                progress = { progressAnimated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .testTag("budget_progress_bar_${envelope.id}"),
                color = animatedColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            // Numbers: Terpakai vs Sisa / Lebih
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Terpakai:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${CurrencyFormatter.formatRupiah(envelope.spentAmount)} (${envelope.percentageDisplay}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val remaining = envelope.remainingAmount
                    Text(
                        text = if (remaining >= 0) "Sisa Kuota:" else "Kelebihan:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (remaining >= 0) {
                            CurrencyFormatter.formatRupiah(remaining)
                        } else {
                            "- ${CurrencyFormatter.formatRupiah(-remaining)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remaining >= 0) GreenBudgetSafe else RedBudgetAlert
                    )
                }
            }
        }
    }
}
