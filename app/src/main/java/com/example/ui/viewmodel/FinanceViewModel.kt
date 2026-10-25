package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.download.AiModelDownloadManager
import com.example.ai.engine.JarvisAiEngine
import com.example.data.AppDatabase
import com.example.data.entity.BillEntity
import com.example.data.entity.BudgetCategoryEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.DebtTypes
import com.example.data.model.DefaultExpenseCategories
import com.example.data.model.DefaultIncomeCategories
import com.example.data.model.TransactionTypes
import com.example.data.model.WalletSources
import com.example.data.model.WishlistCategories
import com.example.data.model.WishlistPriorities
import com.example.data.repository.FinanceRepository
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.MathExpressionEvaluator
import com.example.util.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    val downloadManager = AiModelDownloadManager(application)
    val aiEngine = JarvisAiEngine(application, repository, downloadManager)

    init {
        viewModelScope.launch {
            // aiEngine.initializeLlm() // Fitur Si Cici dibekukan per permintaan user
            // Clean up unconfigured default seed categories (monthlyLimit <= 0) from legacy installs
            repository.cleanupUnconfiguredDefaultBudgetCategories()
        }
    }

    private val _timeFilter = MutableStateFlow(TimeFilter.THIS_MONTH)
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedWallet = MutableStateFlow<String?>(null)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _reminderEnabled = MutableStateFlow(ReminderScheduler.isReminderEnabled(application))

    // Helper flows from repository
    private val allTransactions = repository.allTransactions
    private val wishlistFlow = repository.allWishlist
    private val budgetCategoriesFlow = repository.allBudgetCategories
    private val billsFlow = repository.allBills
    private val debtsFlow = repository.allDebtsAndLoans
    private val pricesFlow = repository.allPriceTracks

    val uiState: StateFlow<DashboardUiState> = combine(
        listOf(
            allTransactions,
            budgetCategoriesFlow,
            wishlistFlow,
            billsFlow,
            debtsFlow,
            pricesFlow,
            _timeFilter,
            _selectedCategory,
            _selectedWallet,
            _reminderEnabled,
            _userMessage
        )
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val transactions = params[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = params[1] as List<BudgetCategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val wishlists = params[2] as List<WishlistEntity>
        @Suppress("UNCHECKED_CAST")
        val bills = params[3] as List<BillEntity>
        @Suppress("UNCHECKED_CAST")
        val debts = params[4] as List<DebtLoanEntity>
        @Suppress("UNCHECKED_CAST")
        val prices = params[5] as List<PriceTrackerEntity>
        val timeFilt = params[6] as TimeFilter
        val catFilt = params[7] as String?
        val wallFilt = params[8] as String?
        val reminderOn = params[9] as Boolean
        val message = params[10] as String?

        // Dynamic boundaries calculated fresh on every emission (never freezes across midnight/months)
        val now = DateTimeUtils.getCurrentJakartaTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)
        val endOfDay = DateTimeUtils.getEndOfDay(now)
        val startOfMonth = DateTimeUtils.getStartOfMonth(now)
        val endOfMonth = DateTimeUtils.getEndOfMonth(now)
        val startOfLastMonth = DateTimeUtils.getStartOfLastMonth(now)
        val endOfLastMonth = DateTimeUtils.getEndOfLastMonth(now)

        // Exact in-memory calculations from the single source of truth
        val todayExp = transactions.filter {
            it.type == TransactionTypes.EXPENSE && it.dateTimestamp in startOfDay..endOfDay
        }.sumOf { it.amount }

        val monthExp = transactions.filter {
            it.type == TransactionTypes.EXPENSE && it.dateTimestamp in startOfMonth..endOfMonth
        }.sumOf { it.amount }

        val monthInc = transactions.filter {
            it.type == TransactionTypes.INCOME && it.dateTimestamp in startOfMonth..endOfMonth
        }.sumOf { it.amount }

        val lastMonthExp = transactions.filter {
            it.type == TransactionTypes.EXPENSE && it.dateTimestamp in startOfLastMonth..endOfLastMonth
        }.sumOf { it.amount }

        val netBal = transactions.sumOf {
            if (it.type == TransactionTypes.INCOME) it.amount else -it.amount
        }

        // Filtered transactions for the Dashboard list
        val filtered = transactions.filter { tx ->
            val timeMatch = when (timeFilt) {
                TimeFilter.TODAY -> tx.dateTimestamp in startOfDay..endOfDay
                TimeFilter.THIS_MONTH -> tx.dateTimestamp in startOfMonth..endOfMonth
                TimeFilter.ALL -> true
            }
            val categoryMatch = catFilt == null || tx.category.trim().equals(catFilt.trim(), ignoreCase = true)
            val walletMatch = wallFilt == null || tx.walletSource.trim().equals(wallFilt.trim(), ignoreCase = true)
            timeMatch && categoryMatch && walletMatch
        }

        // Synchronized budget envelopes - spending amounts are guaranteed to match monthExp!
        val monthlyExpenses = transactions.filter {
            it.type == TransactionTypes.EXPENSE && it.dateTimestamp in startOfMonth..endOfMonth
        }
        val spendingByCategory = monthlyExpenses.groupBy { it.category.trim().lowercase() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Pos anggaran murni dari kategori yang dibuat pengguna (dengan pemetaan case & whitespace insensitive)
        val envelopes = categories.map { cat ->
            val spent = spendingByCategory[cat.categoryName.trim().lowercase()] ?: 0L
            com.example.data.model.BudgetEnvelopeItem(
                id = cat.id,
                categoryName = cat.categoryName,
                monthlyLimit = cat.monthlyLimit,
                spentAmount = spent,
                iconEmoji = cat.iconEmoji,
                colorHex = cat.colorHex
            )
        }

        val (remHour, remMin) = ReminderScheduler.getReminderTime(getApplication())
        val reminderTimeStr = String.format("%02d:%02d WIB", remHour, remMin)

        DashboardUiState(
            totalNetBalance = netBal,
            todayExpense = todayExp,
            thisMonthExpense = monthExp,
            thisMonthIncome = monthInc,
            lastMonthExpense = lastMonthExp,
            budgetEnvelopes = envelopes,
            allTransactions = transactions,
            filteredTransactions = filtered,
            wishlistItems = wishlists,
            budgetCategories = categories,
            bills = bills,
            debtLoans = debts,
            priceTracks = prices,
            timeFilter = timeFilt,
            selectedCategory = catFilt,
            selectedWallet = wallFilt,
            isDailyReminderActive = reminderOn,
            reminderTimeString = reminderTimeStr,
            isLoading = false,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun parseMathExpression(input: String): Long {
        return MathExpressionEvaluator.evaluateMathExpression(input)
    }

    fun setTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedWallet(wallet: String?) {
        _selectedWallet.value = wallet
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setUserMessage(msg: String) {
        _userMessage.value = msg
    }

    // === TRANSACTION ACTIONS ===
    fun addTransaction(
        type: String,
        amountInput: String,
        category: String,
        walletSource: String,
        note: String,
        dateTimestamp: Long = DateTimeUtils.getCurrentJakartaTimeMillis()
    ) {
        viewModelScope.launch {
            val evaluatedAmount = parseMathExpression(amountInput)
            if (evaluatedAmount <= 0) {
                _userMessage.value = "Nominal transaksi harus lebih dari Rp 0"
                return@launch
            }
            val newTx = TransactionEntity(
                type = type,
                amount = evaluatedAmount,
                category = category.trim(),
                walletSource = walletSource.trim(),
                dateTimestamp = dateTimestamp,
                note = note.trim()
            )
            repository.insertTransaction(newTx)
            _userMessage.value = "Transaksi berhasil dicatat!"
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            _userMessage.value = "Transaksi berhasil dihapus"
        }
    }

    // === BUDGET CATEGORY ACTIONS ===
    fun addBudgetCategory(name: String, limitInput: String, iconEmoji: String = "💰") {
        viewModelScope.launch {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                _userMessage.value = "Nama pos anggaran wajib diisi"
                return@launch
            }
            val limit = parseMathExpression(limitInput)
            if (limit <= 0) {
                _userMessage.value = "Batas anggaran harus lebih dari Rp 0"
                return@launch
            }
            val duplicate = uiState.value.budgetCategories.any {
                it.categoryName.equals(trimmedName, ignoreCase = true)
            }
            if (duplicate) {
                _userMessage.value = "Pos anggaran '$trimmedName' sudah ada"
                return@launch
            }
            repository.insertBudgetCategory(
                BudgetCategoryEntity(
                    categoryName = trimmedName,
                    monthlyLimit = limit,
                    iconEmoji = iconEmoji
                )
            )
            _userMessage.value = "Pos anggaran '$trimmedName' berhasil dibuat!"
        }
    }

    fun updateBudgetCategory(
        id: Long,
        newName: String,
        newLimitInput: String,
        newIconEmoji: String
    ) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) {
                _userMessage.value = "Nama pos anggaran wajib diisi"
                return@launch
            }
            val limit = parseMathExpression(newLimitInput)
            if (limit <= 0) {
                _userMessage.value = "Batas anggaran harus lebih dari Rp 0"
                return@launch
            }
            val existing = uiState.value.budgetCategories.find { it.id == id }
            if (existing != null) {
                repository.updateBudgetCategoryWithTransactions(
                    oldCategory = existing,
                    newName = trimmedName,
                    newLimit = limit,
                    newIconEmoji = newIconEmoji
                )
                _userMessage.value = "Pos anggaran '$trimmedName' berhasil diperbarui!"
            }
        }
    }

    fun deleteBudgetCategoryById(id: Long) {
        viewModelScope.launch {
            val existing = uiState.value.budgetCategories.find { it.id == id }
            if (existing != null) {
                repository.deleteBudgetCategory(existing)
                _userMessage.value = "Pos anggaran '${existing.categoryName}' telah dihapus"
            }
        }
    }

    fun deleteBudgetCategory(category: BudgetCategoryEntity) {
        viewModelScope.launch {
            repository.deleteBudgetCategory(category)
            _userMessage.value = "Pos anggaran '${category.categoryName}' dihapus"
        }
    }

    fun setBudgetLimit(categoryName: String, newLimitInput: String, iconEmoji: String = "💰") {
        val existing = uiState.value.budgetCategories.find { it.categoryName == categoryName }
        if (existing != null) {
            updateBudgetCategory(existing.id, categoryName, newLimitInput, iconEmoji)
        } else {
            addBudgetCategory(categoryName, newLimitInput, iconEmoji)
        }
    }

    // === WISHLIST & DAFTAR BELANJA ACTIONS ===
    fun addWishlistItem(
        name: String,
        targetAmountInput: String = "",
        actualPriceInput: String = "",
        initialSavedInput: String = "0",
        isSavingGoal: Boolean = false,
        categoryType: String = WishlistCategories.KITCHEN,
        priority: String = WishlistPriorities.MEDIUM,
        quantityOrUnit: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _userMessage.value = "Nama barang belanja tidak boleh kosong"
                return@launch
            }
            val target = if (targetAmountInput.isNotBlank()) parseMathExpression(targetAmountInput) else 0L
            val actual = if (actualPriceInput.isNotBlank()) parseMathExpression(actualPriceInput) else 0L
            val saved = if (isSavingGoal && initialSavedInput.isNotBlank()) parseMathExpression(initialSavedInput).coerceAtLeast(0L) else 0L
            val item = WishlistEntity(
                name = name.trim(),
                targetAmount = target,
                actualPrice = actual,
                savedAmount = saved,
                isSavingGoal = isSavingGoal,
                categoryType = categoryType,
                priority = priority,
                quantityOrUnit = quantityOrUnit.trim(),
                note = note.trim(),
                isFulfilled = if (isSavingGoal && target > 0) saved >= target else false
            )
            repository.insertWishlist(item)
            _userMessage.value = if (isSavingGoal) "Target impian ditambahkan!" else "Barang ditambahkan ke daftar belanja!"
        }
    }

    fun allocateSavings(wishlistId: Long, addAmountInput: String) {
        viewModelScope.launch {
            val amount = parseMathExpression(addAmountInput)
            if (amount <= 0) {
                _userMessage.value = "Nominal alokasi harus lebih dari Rp 0"
                return@launch
            }
            val item = uiState.value.wishlistItems.find { it.id == wishlistId } ?: return@launch
            val newSaved = item.savedAmount + amount
            val isFulfilled = if (item.targetAmount > 0) newSaved >= item.targetAmount else false
            repository.updateWishlist(
                item.copy(
                    savedAmount = newSaved,
                    isFulfilled = isFulfilled
                )
            )
            if (isFulfilled && !item.isFulfilled) {
                _userMessage.value = "🎉 Alhamdulillah! Target '${item.name}' tercapai!"
            } else {
                _userMessage.value = "Tabungan untuk '${item.name}' bertambah Rp $amount!"
            }
        }
    }

    fun toggleWishlistFulfilled(
        item: WishlistEntity,
        actualPriceInput: String? = null,
        recordAsTransaction: Boolean = true,
        targetPosCategory: String? = null,
        walletSource: String = WalletSources.CASH
    ) {
        viewModelScope.launch {
            val newFulfilled = !item.isFulfilled
            val actual = if (actualPriceInput != null && actualPriceInput.isNotBlank()) {
                parseMathExpression(actualPriceInput)
            } else if (item.actualPrice > 0) {
                item.actualPrice
            } else {
                item.targetAmount
            }
            val posCategory = (targetPosCategory ?: item.categoryType.ifBlank { "Belanja" }).trim()
            val updated = item.copy(
                isFulfilled = newFulfilled,
                actualPrice = if (actual > 0) actual else item.actualPrice,
                categoryType = posCategory
            )
            repository.updateWishlist(updated)

            val noteTag = "(#W${item.id})"

            if (newFulfilled && recordAsTransaction && actual > 0) {
                // Hapus transaksi lama jika sebelumnya sudah pernah dibuat untuk item ini (mencegah duplikasi)
                val oldTx = uiState.value.allTransactions.find { it.note.contains(noteTag) }
                if (oldTx != null) {
                    repository.deleteTransaction(oldTx)
                }

                // Catat transaksi pengeluaran baru ke pos anggaran yang dipilih
                repository.insertTransaction(
                    TransactionEntity(
                        type = TransactionTypes.EXPENSE,
                        amount = actual,
                        category = posCategory,
                        walletSource = walletSource,
                        dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                        note = "Belanja: ${item.name}${if (item.quantityOrUnit.isNotBlank()) " (${item.quantityOrUnit})" else ""} $noteTag".trim()
                    )
                )
                _userMessage.value = "✅ '${item.name}' lunas! ${CurrencyFormatter.formatRupiah(actual)} otomatis terpotong dari pos '$posCategory' & saldo kas."
            } else if (!newFulfilled) {
                // Ketika status lunas dibatalkan (uncheck), kembalikan uang ke pos anggaran & saldo dengan menghapus transaksi belanjanya
                val existingTx = uiState.value.allTransactions.find { it.note.contains(noteTag) }
                if (existingTx != null) {
                    repository.deleteTransaction(existingTx)
                    _userMessage.value = "↩️ Status lunas '${item.name}' dibatalkan. Dana ${CurrencyFormatter.formatRupiah(existingTx.amount)} dikembalikan ke pos '$posCategory' & saldo kas."
                } else {
                    _userMessage.value = "'${item.name}' ditandai belum dibeli"
                }
            }
        }
    }

    fun completeShoppingBatch(
        items: List<WishlistEntity>,
        walletSource: String = WalletSources.CASH
    ) {
        viewModelScope.launch {
            var totalDeducted = 0L
            items.forEach { item ->
                val actual = if (item.actualPrice > 0) item.actualPrice else item.targetAmount
                val posCategory = item.categoryType.ifBlank { "Belanja" }.trim()
                val updated = item.copy(
                    isFulfilled = true,
                    actualPrice = if (actual > 0) actual else item.actualPrice
                )
                repository.updateWishlist(updated)

                if (actual > 0) {
                    val noteTag = "(#W${item.id})"
                    val oldTx = uiState.value.allTransactions.find { it.note.contains(noteTag) }
                    if (oldTx != null) {
                        repository.deleteTransaction(oldTx)
                    }
                    repository.insertTransaction(
                        TransactionEntity(
                            type = TransactionTypes.EXPENSE,
                            amount = actual,
                            category = posCategory,
                            walletSource = walletSource,
                            dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                            note = "Belanja: ${item.name}${if (item.quantityOrUnit.isNotBlank()) " (${item.quantityOrUnit})" else ""} $noteTag".trim()
                        )
                    )
                    totalDeducted += actual
                }
            }
            _userMessage.value = "🎉 Belanja selesai! ${items.size} item lunas (${CurrencyFormatter.formatRupiah(totalDeducted)} terpotong dari pos & saldo kas)."
        }
    }

    fun updateActualPrice(item: WishlistEntity, actualPriceInput: String) {
        viewModelScope.launch {
            val price = parseMathExpression(actualPriceInput)
            repository.updateWishlist(item.copy(actualPrice = price))
            _userMessage.value = "Harga sebenarnya diperbarui: Rp $price"
        }
    }

    fun deleteWishlist(item: WishlistEntity) {
        viewModelScope.launch {
            repository.deleteWishlist(item)
            _userMessage.value = "Item dihapus dari daftar"
        }
    }

    // === BILLS (TAGIHAN RUTIN) ACTIONS ===
    fun addBill(
        title: String,
        amountInput: String,
        dueDay: Int,
        category: String = DefaultExpenseCategories.LISTRIK,
        walletSource: String = WalletSources.BANK,
        note: String = ""
    ) {
        viewModelScope.launch {
            if (title.isBlank()) {
                _userMessage.value = "Nama tagihan tidak boleh kosong"
                return@launch
            }
            val amount = parseMathExpression(amountInput)
            val bill = BillEntity(
                title = title.trim(),
                estimatedAmount = amount,
                dueDayOfMonth = dueDay.coerceIn(1, 31),
                category = category,
                walletSource = walletSource,
                lastPaidMonthYear = "",
                note = note.trim()
            )
            repository.insertBill(bill)
            _userMessage.value = "Tagihan '${bill.title}' berhasil ditambahkan!"
        }
    }

    fun payBillNow(
        bill: BillEntity,
        paidAmountInput: String? = null,
        walletSource: String = bill.walletSource
    ) {
        viewModelScope.launch {
            val amount = if (!paidAmountInput.isNullOrBlank()) {
                parseMathExpression(paidAmountInput)
            } else bill.estimatedAmount
            val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()
            val updated = bill.copy(
                lastPaidMonthYear = currentMonthKey,
                walletSource = walletSource
            )
            repository.updateBill(updated)
            repository.insertTransaction(
                TransactionEntity(
                    type = TransactionTypes.EXPENSE,
                    amount = amount,
                    category = bill.category,
                    walletSource = walletSource,
                    dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                    note = "Bayar Tagihan: ${bill.title}".trim()
                )
            )
            _userMessage.value = "✅ Tagihan '${bill.title}' lunas bulan ini & tercatat di pengeluaran!"
        }
    }

    fun toggleBillStatusManual(bill: BillEntity) {
        viewModelScope.launch {
            val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()
            val isPaidThisMonth = bill.lastPaidMonthYear == currentMonthKey
            val updated = bill.copy(
                lastPaidMonthYear = if (isPaidThisMonth) "" else currentMonthKey
            )
            repository.updateBill(updated)
            _userMessage.value = if (!isPaidThisMonth) "✅ Tagihan '${bill.title}' ditandai Lunas" else "Status tagihan '${bill.title}' diubah ke Belum Dibayar"
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            _userMessage.value = "Tagihan '${bill.title}' dihapus"
        }
    }

    // === DEBT & LOAN (HUTANG PIUTANG) ACTIONS ===
    fun addDebtLoan(
        personName: String,
        type: String,
        amountInput: String,
        dueDateString: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            if (personName.isBlank()) {
                _userMessage.value = "Nama orang/kerabat tidak boleh kosong"
                return@launch
            }
            val amount = parseMathExpression(amountInput)
            if (amount <= 0) {
                _userMessage.value = "Nominal harus lebih dari Rp 0"
                return@launch
            }
            val item = DebtLoanEntity(
                personName = personName.trim(),
                type = type,
                totalAmount = amount,
                paidAmount = 0L,
                dueDateString = dueDateString.trim(),
                note = note.trim(),
                isSettled = false
            )
            repository.insertDebtLoan(item)
            _userMessage.value = if (type == DebtTypes.PAYABLE) "Catatan hutang tersimpan" else "Catatan piutang tersimpan"
        }
    }

    fun recordDebtPayment(
        item: DebtLoanEntity,
        paymentAmountInput: String,
        recordAsTransaction: Boolean = false,
        walletSource: String = WalletSources.CASH
    ) {
        viewModelScope.launch {
            val payment = parseMathExpression(paymentAmountInput)
            if (payment <= 0) return@launch
            val newPaid = (item.paidAmount + payment).coerceAtMost(item.totalAmount)
            val isSettled = newPaid >= item.totalAmount
            val updated = item.copy(paidAmount = newPaid, isSettled = isSettled)
            repository.updateDebtLoan(updated)
            if (recordAsTransaction) {
                val isPayable = item.type == DebtTypes.PAYABLE
                repository.insertTransaction(
                    TransactionEntity(
                        type = if (isPayable) TransactionTypes.EXPENSE else TransactionTypes.INCOME,
                        amount = payment,
                        category = if (isPayable) DefaultExpenseCategories.LAINNYA else DefaultIncomeCategories.LAINNYA,
                        walletSource = walletSource,
                        dateTimestamp = DateTimeUtils.getCurrentJakartaTimeMillis(),
                        note = "${if (isPayable) "Cicil/Bayar Hutang ke" else "Terima Piutang dari"}: ${item.personName}"
                    )
                )
            }
            _userMessage.value = if (isSettled) "✅ Lunas! '${item.personName}' selesai" else "Cicilan ${CurrencyFormatter.formatRupiah(payment)} tercatat"
        }
    }

    fun toggleDebtSettled(item: DebtLoanEntity) {
        viewModelScope.launch {
            val newSettled = !item.isSettled
            val updated = item.copy(
                isSettled = newSettled,
                paidAmount = if (newSettled) item.totalAmount else 0L
            )
            repository.updateDebtLoan(updated)
            _userMessage.value = if (newSettled) "✅ Ditandai Lunas" else "Status diubah ke Belum Lunas"
        }
    }

    fun deleteDebtLoan(item: DebtLoanEntity) {
        viewModelScope.launch {
            repository.deleteDebtLoan(item)
            _userMessage.value = "Catatan hutang/piutang dihapus"
        }
    }

    // === PRICE TRACKER ACTIONS ===
    fun addPriceTrack(
        itemName: String,
        priceInput: String,
        unit: String = "1 kg",
        storeLocation: String = "",
        note: String = ""
    ) {
        viewModelScope.launch {
            if (itemName.isBlank()) {
                _userMessage.value = "Nama bahan/barang tidak boleh kosong"
                return@launch
            }
            val price = parseMathExpression(priceInput)
            val nowStr = DateTimeUtils.formatShortDate(System.currentTimeMillis())
            val item = PriceTrackerEntity(
                itemName = itemName.trim(),
                latestPrice = price,
                previousPrice = price,
                unit = unit.trim(),
                storeLocation = storeLocation.trim(),
                lastUpdatedDate = nowStr,
                note = note.trim()
            )
            repository.insertPriceTrack(item)
            _userMessage.value = "Harga pasar '${item.itemName}' tercatat!"
        }
    }

    fun updatePriceTrack(
        item: PriceTrackerEntity,
        newPriceInput: String,
        storeLocation: String = item.storeLocation,
        note: String = item.note
    ) {
        viewModelScope.launch {
            val newPrice = parseMathExpression(newPriceInput)
            val nowStr = DateTimeUtils.formatShortDate(System.currentTimeMillis())
            val updated = item.copy(
                previousPrice = item.latestPrice,
                latestPrice = newPrice,
                storeLocation = storeLocation.trim(),
                lastUpdatedDate = nowStr,
                note = note.trim()
            )
            repository.updatePriceTrack(updated)
            _userMessage.value = "Harga '${item.itemName}' diperbarui menjadi ${CurrencyFormatter.formatRupiah(newPrice)}"
        }
    }

    fun deletePriceTrack(item: PriceTrackerEntity) {
        viewModelScope.launch {
            repository.deletePriceTrack(item)
            _userMessage.value = "Pantauan harga dihapus"
        }
    }

    fun generateWhatsAppRekapText(): String {
        val state = uiState.value
        val dateStr = DateTimeUtils.formatDateWithDay(System.currentTimeMillis())
        val currentMonth = DateTimeUtils.formatCurrentMonth()
        val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()
        val unpaidBills = state.bills.filter { it.lastPaidMonthYear != currentMonthKey }

        val sb = StringBuilder()
        sb.append("📋 *Laporan Kas ArAr TOZ*\n")
        sb.append("🗓️ $dateStr\n\n")
        sb.append("💰 *Ringkasan Saldo Saat Ini:*\n")
        sb.append("• Total Sisa Kas: ${CurrencyFormatter.formatRupiah(state.totalNetBalance)}\n")
        sb.append("• Pengeluaran Hari Ini: ${CurrencyFormatter.formatRupiah(state.todayExpense)}\n")
        sb.append("• Pengeluaran $currentMonth: ${CurrencyFormatter.formatRupiah(state.thisMonthExpense)}\n")
        sb.append("• Pemasukan $currentMonth: ${CurrencyFormatter.formatRupiah(state.thisMonthIncome)}\n\n")

        if (state.budgetEnvelopes.isNotEmpty()) {
            sb.append("✉️ *Pos Anggaran ($currentMonth):*\n")
            state.budgetEnvelopes.forEach { env ->
                val statusEmoji = when {
                    env.isSafe -> "🟢 Aman"
                    env.isWarning -> "🟡 Waspada"
                    else -> "🔴 Over Budget"
                }
                sb.append("• ${env.iconEmoji} ${env.categoryName}: ${CurrencyFormatter.formatRupiah(env.spentAmount)} / ${CurrencyFormatter.formatRupiah(env.monthlyLimit)} ($statusEmoji)\n")
            }
            sb.append("\n")
        }

        if (unpaidBills.isNotEmpty()) {
            sb.append("📅 *Tagihan Rutin Belum Dibayar Bulan Ini:*\n")
            unpaidBills.take(4).forEach { b ->
                sb.append("• ${b.title} (Tgl ${b.dueDayOfMonth}): ~${CurrencyFormatter.formatRupiah(b.estimatedAmount)}\n")
            }
            if (unpaidBills.size > 4) {
                sb.append("• dan ${unpaidBills.size - 4} tagihan lainnya\n")
            }
            sb.append("\n")
        }

        sb.append("Semoga berkah & terkendali selalu rezeki keluarga kita 🤲❤️")
        return sb.toString()
    }

    suspend fun getJsonBackupContent(): String {
        return repository.exportDataToJson()
    }

    fun restoreBackup(jsonContent: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreDataFromJson(jsonContent)
            result.fold(
                onSuccess = { count ->
                    _userMessage.value = "Data berhasil dipulihkan! ($count catatan)"
                    onComplete(true, "Berhasil memulihkan $count catatan.")
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Format file tidak valid"
                    _userMessage.value = "Gagal memulihkan data: $errMsg"
                    onComplete(false, errMsg)
                }
            )
        }
    }

    fun toggleDailyReminder(enabled: Boolean) {
        ReminderScheduler.setReminderEnabled(getApplication(), enabled)
        _reminderEnabled.value = enabled
        _userMessage.value = if (enabled) {
            "Pengingat harian aktif sebelum jam 9 malam (20:30 WIB) 🔔"
        } else {
            "Pengingat harian dimatikan"
        }
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllUserData(clearEnvelopes = true)
            _userMessage.value = "🗑️ Seluruh data transaksi, pos anggaran, tagihan, hutang, dan belanja telah dibersihkan!"
            onComplete()
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    val repository = FinanceRepository(db.financeDao())
                    return FinanceViewModel(application, repository) as T
                }
            }
    }
}
