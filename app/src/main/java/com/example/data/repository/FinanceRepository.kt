package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.entity.BillEntity
import com.example.data.entity.BudgetCategoryEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.BackupData
import com.example.data.model.BudgetEnvelopeItem
import com.example.util.DateTimeUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class FinanceRepository(private val dao: FinanceDao) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    // All transactions
    val allTransactions: Flow<List<TransactionEntity>> = dao.getAllTransactions()

    // Aggregates for current month and today
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsBetween(startTime, endTime)

    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Long> =
        dao.getTotalExpenseBetween(startTime, endTime)

    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Long> =
        dao.getTotalIncomeBetween(startTime, endTime)

    val totalNetBalance: Flow<Long> = dao.getTotalNetBalance()

    // All budget categories
    val allBudgetCategories: Flow<List<BudgetCategoryEntity>> = dao.getAllBudgetCategories()

    // Budget Envelopes combining categories with current month spending
    fun getBudgetEnvelopesThisMonth(): Flow<List<BudgetEnvelopeItem>> {
        val now = DateTimeUtils.getCurrentJakartaTimeMillis()
        val startOfMonth = DateTimeUtils.getStartOfMonth(now)
        val endOfMonth = DateTimeUtils.getEndOfMonth(now)
        return combine(
            dao.getAllBudgetCategories(),
            dao.getCategoryExpenseSummaryBetween(startOfMonth, endOfMonth)
        ) { categories, summaries ->
            val spendingMap = summaries.associate { it.category to it.totalSpent }
            val existingCategoryNames = categories.map { it.categoryName }.toSet()

            val envelopeList = categories.map { cat ->
                val spent = spendingMap[cat.categoryName] ?: 0L
                BudgetEnvelopeItem(
                    id = cat.id,
                    categoryName = cat.categoryName,
                    monthlyLimit = cat.monthlyLimit,
                    spentAmount = spent,
                    iconEmoji = cat.iconEmoji,
                    colorHex = cat.colorHex
                )
            }.toMutableList()

            // Include unbudgeted categories from transactions
            var unbudgetedId = -100L
            summaries.forEach { summary ->
                if (summary.category !in existingCategoryNames && summary.totalSpent > 0) {
                    envelopeList.add(
                        BudgetEnvelopeItem(
                            id = unbudgetedId--,
                            categoryName = summary.category,
                            monthlyLimit = 0L,
                            spentAmount = summary.totalSpent,
                            iconEmoji = com.example.data.model.DefaultExpenseCategories.getIconEmoji(summary.category),
                            colorHex = "#78909C"
                        )
                    )
                }
            }

            envelopeList
        }
    }

    // Wishlist items
    val allWishlist: Flow<List<WishlistEntity>> = dao.getAllWishlist()

    // Routine Bills
    val allBills: Flow<List<BillEntity>> = dao.getAllBills()

    // Debts and Loans
    val allDebtsAndLoans: Flow<List<DebtLoanEntity>> = dao.getAllDebtsAndLoans()

    // Market Price Tracker
    val allPriceTracks: Flow<List<PriceTrackerEntity>> = dao.getAllPriceTracks()

    // === WRITE ACTIONS ===
    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        withContext(Dispatchers.IO) {
            dao.insertTransaction(transaction)
        }

    suspend fun updateTransaction(transaction: TransactionEntity) =
        withContext(Dispatchers.IO) {
            dao.updateTransaction(transaction)
        }

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        withContext(Dispatchers.IO) {
            dao.deleteTransaction(transaction)
        }

    suspend fun insertBudgetCategory(category: BudgetCategoryEntity): Long =
        withContext(Dispatchers.IO) {
            dao.insertBudgetCategory(category)
        }

    suspend fun updateBudgetCategory(category: BudgetCategoryEntity) =
        withContext(Dispatchers.IO) {
            dao.updateBudgetCategory(category)
        }

    suspend fun deleteBudgetCategory(category: BudgetCategoryEntity) =
        withContext(Dispatchers.IO) {
            dao.deleteBudgetCategory(category)
        }

    suspend fun insertWishlist(item: WishlistEntity): Long =
        withContext(Dispatchers.IO) {
            dao.insertWishlist(item)
        }

    suspend fun updateWishlist(item: WishlistEntity) =
        withContext(Dispatchers.IO) {
            dao.updateWishlist(item)
        }

    suspend fun deleteWishlist(item: WishlistEntity) =
        withContext(Dispatchers.IO) {
            dao.deleteWishlist(item)
        }

    suspend fun allocateSavingsToWishlist(id: Long, amount: Long) =
        withContext(Dispatchers.IO) {
            dao.allocateSavingsToWishlist(id, amount)
        }

    // Bills
    suspend fun insertBill(bill: BillEntity): Long = withContext(Dispatchers.IO) {
        dao.insertBill(bill)
    }

    suspend fun updateBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        dao.updateBill(bill)
    }

    suspend fun deleteBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        dao.deleteBill(bill)
    }

    // Debts & Loans
    suspend fun insertDebtLoan(debtLoan: DebtLoanEntity): Long = withContext(Dispatchers.IO) {
        dao.insertDebtLoan(debtLoan)
    }

    suspend fun updateDebtLoan(debtLoan: DebtLoanEntity) = withContext(Dispatchers.IO) {
        dao.updateDebtLoan(debtLoan)
    }

    suspend fun deleteDebtLoan(debtLoan: DebtLoanEntity) = withContext(Dispatchers.IO) {
        dao.deleteDebtLoan(debtLoan)
    }

    // Price Tracker
    suspend fun insertPriceTrack(track: PriceTrackerEntity): Long = withContext(Dispatchers.IO) {
        dao.insertPriceTrack(track)
    }

    suspend fun updatePriceTrack(track: PriceTrackerEntity) = withContext(Dispatchers.IO) {
        dao.updatePriceTrack(track)
    }

    suspend fun deletePriceTrack(track: PriceTrackerEntity) = withContext(Dispatchers.IO) {
        dao.deletePriceTrack(track)
    }

    // Quick helper for assistant summaries
    suspend fun getQuickFinancialSummary(): Map<String, Any> = withContext(Dispatchers.IO) {
        val startDay = DateTimeUtils.getStartOfDay()
        val endDay = DateTimeUtils.getEndOfDay()
        val startMonth = DateTimeUtils.getStartOfMonth()
        val endMonth = DateTimeUtils.getEndOfMonth()

        val netBalance = dao.getTotalNetBalance().first()
        val todayExpense = dao.getTotalExpenseBetween(startDay, endDay).first()
        val monthExpense = dao.getTotalExpenseBetween(startMonth, endMonth).first()
        val monthIncome = dao.getTotalIncomeBetween(startMonth, endMonth).first()
        val bills = dao.getAllBillsSnapshot()
        val currentMonthKey = DateTimeUtils.getCurrentMonthYearKey()
        val unpaidBills = bills.filter { it.lastPaidMonthYear != currentMonthKey }

        mapOf(
            "netBalance" to netBalance,
            "todayExpense" to todayExpense,
            "monthExpense" to monthExpense,
            "monthIncome" to monthIncome,
            "unpaidBillsCount" to unpaidBills.size,
            "unpaidBills" to unpaidBills.map { mapOf("title" to it.title, "amount" to it.estimatedAmount, "dueDay" to it.dueDayOfMonth) }
        )
    }

    // === BACKUP & RESTORE ===
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val transactions = dao.getAllTransactionsSnapshot()
        val budgetCategories = dao.getAllBudgetCategoriesSnapshot()
        val wishlist = dao.getAllWishlistSnapshot()
        val bills = dao.getAllBillsSnapshot()
        val debts = dao.getAllDebtsSnapshot()
        val priceTracks = dao.getAllPriceTracksSnapshot()
        val now = DateTimeUtils.getCurrentJakartaTimeMillis()

        val backupData = BackupData(
            appVersion = "2.0",
            exportedAtJakarta = DateTimeUtils.formatDateTimeJakarta(now),
            exportedTimestamp = now,
            transactions = transactions,
            budgetCategories = budgetCategories,
            wishlist = wishlist,
            bills = bills,
            debts = debts,
            priceTracks = priceTracks
        )
        gson.toJson(backupData)
    }

    suspend fun restoreDataFromJson(jsonContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(jsonContent, BackupData::class.java)
                ?: return@withContext Result.failure(Exception("Format JSON kosong atau tidak valid"))

            if (backup.transactions.isNotEmpty()) {
                dao.clearTransactions()
                dao.insertAllTransactions(backup.transactions)
            }
            if (backup.budgetCategories.isNotEmpty()) {
                dao.clearBudgetCategories()
                dao.insertBudgetCategories(backup.budgetCategories)
            }
            if (backup.wishlist.isNotEmpty()) {
                dao.clearWishlist()
                dao.insertWishlists(backup.wishlist)
            }
            if (backup.bills.isNotEmpty()) {
                dao.clearBills()
                dao.insertBills(backup.bills)
            }
            if (backup.debts.isNotEmpty()) {
                dao.clearDebts()
                dao.insertDebtLoans(backup.debts)
            }
            if (backup.priceTracks.isNotEmpty()) {
                dao.clearPriceTracks()
                dao.insertPriceTracks(backup.priceTracks)
            }

            val totalRestored = backup.transactions.size + backup.budgetCategories.size +
                backup.wishlist.size + backup.bills.size + backup.debts.size + backup.priceTracks.size
            Result.success(totalRestored)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        dao.clearTransactions()
        dao.clearWishlist()
        dao.clearBills()
        dao.clearDebts()
        dao.clearPriceTracks()
    }
}
