package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.WishlistCategories
import com.example.ui.components.WishlistCard
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBudgetSafe
import com.example.ui.viewmodel.DashboardUiState
import com.example.util.CurrencyFormatter

enum class ShoppingViewFilter(val label: String) {
    ALL("Semua"),
    KITCHEN("Belanja Dapur"),
    SAVING_GOAL("Target Tabungan"),
    UNBOUGHT("Belum Dibeli"),
    BOUGHT("Sudah Dibeli")
}

@Composable
fun WishlistScreen(
    uiState: DashboardUiState,
    onToggleFulfilled: (WishlistEntity) -> Unit,
    onAllocateSavingsClick: (WishlistEntity) -> Unit,
    onEditActualPriceClick: (WishlistEntity) -> Unit,
    onDeleteWishlist: (WishlistEntity) -> Unit,
    onAddNewWishlist: () -> Unit,
    onAddPriceTrackClick: () -> Unit,
    onEditPriceTrackClick: (PriceTrackerEntity) -> Unit,
    onDeletePriceTrack: (PriceTrackerEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var screenTab by remember { mutableIntStateOf(0) }
    var activeFilter by remember { mutableStateOf(ShoppingViewFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("wishlist_screen")
    ) {
        TabRow(
            selectedTabIndex = screenTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = EmeraldPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = screenTab == 0,
                onClick = { screenTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daftar Belanja", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_shopping_list")
            )
            Tab(
                selected = screenTab == 1,
                onClick = { screenTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pantau Harga Pasar", fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("tab_price_tracker")
            )
        }

        if (screenTab == 0) {
            val filteredItems = uiState.wishlistItems.filter { item ->
                when (activeFilter) {
                    ShoppingViewFilter.ALL -> true
                    ShoppingViewFilter.KITCHEN -> item.categoryType == WishlistCategories.KITCHEN || !item.isSavingGoal
                    ShoppingViewFilter.SAVING_GOAL -> item.isSavingGoal
                    ShoppingViewFilter.UNBOUGHT -> !item.isFulfilled
                    ShoppingViewFilter.BOUGHT -> item.isFulfilled
                }
            }

            val totalItems = uiState.wishlistItems.size
            val boughtCount = uiState.wishlistItems.count { it.isFulfilled }
            val totalActualSpent = uiState.wishlistItems.filter { it.isFulfilled && it.actualPrice > 0 }.sumOf { it.actualPrice }
            val totalEstimated = uiState.wishlistItems.sumOf { it.targetAmount }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wishlist_summary_card"),
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenBudgetSafe,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Status Belanjaan:",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "$boughtCount dari $totalItems terbeli",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Total Estimasi Patokan:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (totalEstimated > 0) CurrencyFormatter.formatRupiah(totalEstimated) else "Rp 0",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Terbeli Real:",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatRupiah(totalActualSpent),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GreenBudgetSafe
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddNewWishlist,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_wishlist_screen_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tambah Belanja Dapur / Wishlist", fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShoppingViewFilter.entries.forEach { filter ->
                            val isSelected = activeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { activeFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("filter_${filter.name}")
                            )
                        }
                    }
                }

                if (filteredItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🛒", fontSize = 42.sp)
                                Text(
                                    text = "Belum ada item di kategori ini",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bunda bisa menambahkan daftar belanja sayur, bumbu dapur harian, atau target barang impian kapan saja.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredItems, key = { it.id }) { item ->
                        WishlistCard(
                            item = item,
                            onToggleFulfilled = { onToggleFulfilled(item) },
                            onAllocateSavingsClick = { onAllocateSavingsClick(item) },
                            onEditActualPriceClick = { onEditActualPriceClick(item) },
                            onDelete = { onDeleteWishlist(item) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("price_track_info_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Buku Patokan Harga Pasar 🏷️",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Mencatat harga beli terakhir bahan pangan (minyak, telur, beras, gas) agar bunda tahu harga wajar dan bisa membandingkan saat belanja di warung atau pasar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Button(
                        onClick = onAddPriceTrackClick,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("add_price_track_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Catat Patokan Harga Bahan Pokok", fontWeight = FontWeight.Bold)
                    }
                }

                if (uiState.priceTracks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Belum ada patokan harga tercatat.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.priceTracks, key = { it.id }) { track ->
                        PriceTrackCardItem(
                            track = track,
                            onEditClick = { onEditPriceTrackClick(track) },
                            onDeleteClick = { onDeletePriceTrack(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriceTrackCardItem(
    track: PriceTrackerEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val diff = track.latestPrice - track.previousPrice
    val (trendText, trendColor, trendIcon) = when {
        track.previousPrice == 0L || diff == 0L -> Triple("Harga Stabil", MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.TrendingFlat)
        diff < 0 -> Triple("Turun ${CurrencyFormatter.formatRupiah(-diff)}", GreenBudgetSafe, Icons.Default.TrendingDown)
        else -> Triple("Naik ${CurrencyFormatter.formatRupiah(diff)}", CoralAccent, Icons.Default.TrendingUp)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("price_track_item_${track.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.itemName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (track.storeLocation.isNotBlank() || track.lastUpdatedDate.isNotBlank()) {
                        Text(
                            text = listOf(track.storeLocation, track.lastUpdatedDate).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Edit", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${CurrencyFormatter.formatRupiah(track.latestPrice)} / ${track.unit}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                    if (track.note.isNotBlank()) {
                        Text(
                            text = track.note,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = trendColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = trendIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(trendText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = trendColor)
                    }
                }
            }
        }
    }
}
