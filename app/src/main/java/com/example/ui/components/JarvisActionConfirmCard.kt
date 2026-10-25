package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.engine.JarvisActionType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe

@Composable
fun JarvisActionConfirmCard(
    actionType: JarvisActionType,
    details: String,
    isSuccess: Boolean = true,
    modifier: Modifier = Modifier
) {
    val (icon, title, iconBg) = when (actionType) {
        JarvisActionType.ADD_TRANSACTION -> Triple(
            Icons.Default.ReceiptLong,
            "Pencatatan Kas Sukses",
            GreenBudgetSafe.copy(alpha = 0.15f)
        )
        JarvisActionType.ADD_WISHLIST -> Triple(
            Icons.Default.PlaylistAddCheck,
            "Wishlist / Belanja Ditambahkan",
            EmeraldPrimary.copy(alpha = 0.15f)
        )
        JarvisActionType.PAY_BILL -> Triple(
            Icons.Default.Payments,
            "Pembayaran Tagihan Dilunasi",
            GreenBudgetSafe.copy(alpha = 0.15f)
        )
        JarvisActionType.GET_SUMMARY -> Triple(
            Icons.Default.Savings,
            "Ringkasan Kas Tersedia",
            MaterialTheme.colorScheme.primaryContainer
        )
        JarvisActionType.CHAT -> Triple(
            Icons.Default.CheckCircle,
            "Respon Si Cici",
            MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("jarvis_action_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSuccess) icon else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isSuccess) GreenBudgetSafe else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = GreenBudgetSafe.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "CRUD SQLite Berhasil",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenBudgetSafe,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
