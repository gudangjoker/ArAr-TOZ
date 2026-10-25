package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.BillEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.WishlistEntity
import com.example.ui.dialog.AddBillDialog
import com.example.ui.dialog.AddDebtLoanDialog
import com.example.ui.dialog.AddPriceTrackDialog
import com.example.ui.dialog.AddWishlistDialog
import com.example.ui.dialog.AllocateSavingsDialog
import com.example.ui.dialog.EditActualPriceDialog
import com.example.data.model.BudgetEnvelopeItem
import com.example.ui.dialog.BudgetEnvelopeDialog
import com.example.ui.dialog.PayBillDialog
import com.example.ui.dialog.PayDebtDialog
import com.example.ui.dialog.TransactionInputDialog
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.BillsAndDebtsScreen
import com.example.ui.screens.BudgetEnvelopeScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.JarvisChatScreen
import com.example.ui.screens.WishlistScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NavTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
fun MainAppScreen(
    viewModel: FinanceViewModel,
    initialOpenQuickAdd: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // Dialog state
    var showTransactionDialog by rememberSaveable { mutableStateOf(initialOpenQuickAdd) }
    var envelopeToEdit by remember { mutableStateOf<BudgetEnvelopeItem?>(null) }
    var showCreateBudgetDialog by rememberSaveable { mutableStateOf(false) }
    var showAddWishlistDialog by rememberSaveable { mutableStateOf(false) }
    var wishlistToAllocate by remember { mutableStateOf<WishlistEntity?>(null) }
    var wishlistToEditPrice by remember { mutableStateOf<WishlistEntity?>(null) }
    var showAddBillDialog by rememberSaveable { mutableStateOf(false) }
    var billToPay by remember { mutableStateOf<BillEntity?>(null) }
    var showAddDebtDialog by rememberSaveable { mutableStateOf(false) }
    var debtToPay by remember { mutableStateOf<DebtLoanEntity?>(null) }
    var showAddPriceTrackDialog by rememberSaveable { mutableStateOf(false) }
    var showConfirmRestoreDialog by remember { mutableStateOf<String?>(null) }

    val shareRekapToWhatsApp = {
        try {
            val shareText = viewModel.generateWhatsAppRekapText()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Bagikan Rekap ke Pasangan / WhatsApp")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan rekap: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    // SAF Export
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonContent = viewModel.getJsonBackupContent()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonContent.toByteArray(Charsets.UTF_8))
                        }
                    }
                    Toast.makeText(context, "Backup berhasil disimpan!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal menyimpan backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF Import
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }
                    if (!jsonString.isNullOrBlank()) {
                        showConfirmRestoreDialog = jsonString
                    } else {
                        Toast.makeText(context, "File JSON kosong!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val navTabs = listOf(
        NavTabItem("Ringkasan", Icons.Filled.Home, Icons.Outlined.Home, "nav_tab_dashboard"),
        NavTabItem("Amplop", Icons.Filled.Drafts, Icons.Outlined.Drafts, "nav_tab_envelopes"),
        NavTabItem("Tagihan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_bills"),
        NavTabItem("Belanja", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart, "nav_tab_wishlist"),
        NavTabItem("Cadangan", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_tab_settings")
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_app_scaffold"),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        floatingActionButton = {
            // FAB Catat hanya tampil ringkas pada Halaman Ringkasan (selectedTabIndex == 0)
            if (selectedTabIndex == 0) {
                SmallFloatingActionButton(
                    onClick = { showTransactionDialog = true },
                    shape = CircleShape,
                    containerColor = EmeraldPrimary,
                    contentColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_quick_add_transaction")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Catat transaksi",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> DashboardScreen(
                    uiState = uiState,
                    onTimeFilterChange = { viewModel.setTimeFilter(it) },
                    onCategoryFilterChange = { viewModel.setSelectedCategory(it) },
                    onWalletFilterChange = { viewModel.setSelectedWallet(it) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onEditBudgetLimit = { cat, limit, emoji ->
                        val env = uiState.budgetEnvelopes.find { it.categoryName == cat }
                        if (env != null) {
                            envelopeToEdit = env
                        } else {
                            viewModel.setBudgetLimit(cat, limit.toString(), emoji)
                        }
                    },
                    onNavigateToEnvelopes = { selectedTabIndex = 1 }
                )
                1 -> BudgetEnvelopeScreen(
                    uiState = uiState,
                    onEditEnvelope = { env ->
                        envelopeToEdit = env
                    },
                    onAddNewCategory = {
                        showCreateBudgetDialog = true
                    },
                    onShareRekapToWhatsApp = shareRekapToWhatsApp
                )
                2 -> BillsAndDebtsScreen(
                    uiState = uiState,
                    onAddBillClick = { showAddBillDialog = true },
                    onPayBillClick = { bill -> billToPay = bill },
                    onToggleBillStatus = { bill -> viewModel.toggleBillStatusManual(bill) },
                    onDeleteBill = { bill -> viewModel.deleteBill(bill) },
                    onAddDebtClick = { showAddDebtDialog = true },
                    onPayDebtClick = { debt -> debtToPay = debt },
                    onToggleDebtSettled = { debt -> viewModel.toggleDebtSettled(debt) },
                    onDeleteDebt = { debt -> viewModel.deleteDebtLoan(debt) }
                )
                3 -> WishlistScreen(
                    uiState = uiState,
                    onToggleFulfilled = { item ->
                        viewModel.toggleWishlistFulfilled(item)
                    },
                    onAllocateSavingsClick = { item ->
                        wishlistToAllocate = item
                    },
                    onEditActualPriceClick = { item ->
                        wishlistToEditPrice = item
                    },
                    onDeleteWishlist = { item ->
                        viewModel.deleteWishlist(item)
                    },
                    onAddNewWishlist = {
                        showAddWishlistDialog = true
                    },
                    onAddPriceTrackClick = {
                        showAddPriceTrackDialog = true
                    },
                    onEditPriceTrackClick = { track ->
                        viewModel.updatePriceTrack(
                            item = track,
                            newPriceInput = (track.latestPrice + 1000).toString()
                        )
                    },
                    onDeletePriceTrack = { track ->
                        viewModel.deletePriceTrack(track)
                    },
                    onCompleteShoppingBatch = { items, wallet ->
                        viewModel.completeShoppingBatch(items, wallet)
                    }
                )
                4 -> BackupSettingsScreen(
                    uiState = uiState,
                    onExportJsonClick = {
                        val defaultFilename = "DompetRT_Backup_${System.currentTimeMillis()}.json"
                        exportJsonLauncher.launch(defaultFilename)
                    },
                    onImportJsonClick = {
                        importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    onToggleReminder = { enabled ->
                        viewModel.toggleDailyReminder(enabled)
                    },
                    onClearAllDataClick = {
                        viewModel.clearAllData()
                    },
                    onShareRekapToWhatsApp = shareRekapToWhatsApp
                )
            }
        }
    }

    // Modal Bottom Sheet: Quick Transaction Input
    if (showTransactionDialog) {
        TransactionInputDialog(
            onDismissRequest = { showTransactionDialog = false },
            customCategories = uiState.budgetEnvelopes.map { it.categoryName to it.iconEmoji },
            onSaveTransaction = { type, amountExp, category, wallet, note ->
                viewModel.addTransaction(
                    type = type,
                    amountInput = amountExp,
                    category = category,
                    walletSource = wallet,
                    note = note
                )
            }
        )
    }

    // Dialog: Tambah Pos Anggaran Baru
    if (showCreateBudgetDialog) {
        BudgetEnvelopeDialog(
            initialEnvelope = null,
            onDismissRequest = { showCreateBudgetDialog = false },
            onSave = { name, limitExp, iconEmoji ->
                viewModel.addBudgetCategory(name, limitExp, iconEmoji)
                showCreateBudgetDialog = false
            }
        )
    }

    // Dialog: Edit Pos Anggaran
    envelopeToEdit?.let { env ->
        BudgetEnvelopeDialog(
            initialEnvelope = env,
            onDismissRequest = { envelopeToEdit = null },
            onSave = { name, limitExp, iconEmoji ->
                viewModel.updateBudgetCategory(env.id, name, limitExp, iconEmoji)
                envelopeToEdit = null
            },
            onDelete = {
                viewModel.deleteBudgetCategoryById(env.id)
                envelopeToEdit = null
            }
        )
    }

    // Dialog: Add Shopping Item / Wishlist
    if (showAddWishlistDialog) {
        AddWishlistDialog(
            onDismissRequest = { showAddWishlistDialog = false },
            availableBudgetCategories = uiState.budgetEnvelopes.map { it.categoryName to it.iconEmoji },
            onSaveWishlist = { name, targetExp, actualExp, initialSavedExp, isSavingGoal, catType, prio, quantityOrUnit, note ->
                viewModel.addWishlistItem(
                    name = name,
                    targetAmountInput = targetExp,
                    actualPriceInput = actualExp,
                    initialSavedInput = initialSavedExp,
                    isSavingGoal = isSavingGoal,
                    categoryType = catType,
                    priority = prio,
                    quantityOrUnit = quantityOrUnit,
                    note = note
                )
            }
        )
    }

    // Dialog: Edit Actual Price
    wishlistToEditPrice?.let { item ->
        EditActualPriceDialog(
            wishlist = item,
            availableBudgetCategories = uiState.budgetEnvelopes.map { it.categoryName to it.iconEmoji },
            onDismissRequest = { wishlistToEditPrice = null },
            onSaveActualPrice = { actualPriceExp, recordAsExpense, posCategory, walletSource ->
                viewModel.toggleWishlistFulfilled(
                    item = item,
                    actualPriceInput = actualPriceExp,
                    recordAsTransaction = recordAsExpense,
                    targetPosCategory = posCategory,
                    walletSource = walletSource
                )
            }
        )
    }

    // Dialog: Allocate Savings to Wishlist
    wishlistToAllocate?.let { wishlist ->
        AllocateSavingsDialog(
            wishlist = wishlist,
            onDismissRequest = { wishlistToAllocate = null },
            onAllocate = { addAmountExp ->
                viewModel.allocateSavings(wishlist.id, addAmountExp)
            }
        )
    }

    // Dialog: Add Bill
    if (showAddBillDialog) {
        AddBillDialog(
            onDismissRequest = { showAddBillDialog = false },
            onSaveBill = { title, amountExp, dueDay, category, wallet, note ->
                viewModel.addBill(title, amountExp, dueDay, category, wallet, note)
            }
        )
    }

    // Dialog: Pay Bill
    billToPay?.let { bill ->
        PayBillDialog(
            bill = bill,
            onDismissRequest = { billToPay = null },
            onConfirmPayment = { actualAmountExp, walletSource ->
                viewModel.payBillNow(bill, actualAmountExp, walletSource)
            }
        )
    }

    // Dialog: Add Debt / Loan
    if (showAddDebtDialog) {
        AddDebtLoanDialog(
            onDismissRequest = { showAddDebtDialog = false },
            onSaveDebtLoan = { person, type, amountExp, dueDate, note ->
                viewModel.addDebtLoan(person, type, amountExp, dueDate, note)
            }
        )
    }

    // Dialog: Pay / Cicil Debt
    debtToPay?.let { debt ->
        PayDebtDialog(
            item = debt,
            onDismissRequest = { debtToPay = null },
            onConfirmPayment = { paymentExp, recordAsTx, wallet ->
                viewModel.recordDebtPayment(debt, paymentExp, recordAsTx, wallet)
            }
        )
    }

    // Dialog: Add Price Track
    if (showAddPriceTrackDialog) {
        AddPriceTrackDialog(
            onDismissRequest = { showAddPriceTrackDialog = false },
            onSavePriceTrack = { name, priceExp, unit, store, note ->
                viewModel.addPriceTrack(name, priceExp, unit, store, note)
            }
        )
    }

    // Confirm Restore Dialog
    showConfirmRestoreDialog?.let { jsonToRestore ->
        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = null },
            title = { Text("Konfirmasi Pemulihan Data", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Apakah Bunda yakin ingin memulihkan data dari file JSON ini? Seluruh data catatan belanja, pos amplop, dan wishlist akan diperbarui dari cadangan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreBackup(jsonToRestore) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        showConfirmRestoreDialog = null
                    }
                ) {
                    Text("Ya, Pulihkan Data", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
