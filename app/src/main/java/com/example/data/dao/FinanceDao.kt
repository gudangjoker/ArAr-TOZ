package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BillEntity
import com.example.data.entity.BudgetCategoryEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.CategoryExpenseSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // === TRANSACTIONS ===
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY dateTimestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateTimestamp BETWEEN :startTime AND :endTime ORDER BY dateTimestamp DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    // Query aggregasi SQL: SUM Pengeluaran dalam rentang waktu (misal: harian atau bulanan)
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND dateTimestamp BETWEEN :startTime AND :endTime")
    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Long>

    // Query aggregasi SQL: SUM Pemasukan dalam rentang waktu
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND dateTimestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Long>

    // Query aggregasi SQL: Total Saldo Keseluruhan
    @Query("SELECT (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME') - (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE')")
    fun getTotalNetBalance(): Flow<Long>

    // Query aggregasi SQL: Pengeluaran per kategori dalam rentang waktu (untuk Amplop Anggaran)
    @Query("""
        SELECT category, COALESCE(SUM(amount), 0) as totalSpent 
        FROM transactions 
        WHERE type = 'EXPENSE' AND dateTimestamp BETWEEN :startTime AND :endTime 
        GROUP BY category
    """)
    fun getCategoryExpenseSummaryBetween(startTime: Long, endTime: Long): Flow<List<CategoryExpenseSummary>>

    // === BUDGET CATEGORIES ===
    @Query("SELECT * FROM budget_categories ORDER BY id ASC")
    fun getAllBudgetCategories(): Flow<List<BudgetCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategory(category: BudgetCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetCategories(categories: List<BudgetCategoryEntity>)

    @Update
    suspend fun updateBudgetCategory(category: BudgetCategoryEntity)

    @Delete
    suspend fun deleteBudgetCategory(category: BudgetCategoryEntity)

    @Query("DELETE FROM budget_categories WHERE id = :id")
    suspend fun deleteBudgetCategoryById(id: Long)

    // === WISHLIST ===
    @Query("SELECT * FROM wishlist ORDER BY isFulfilled ASC, priority DESC, createdAt DESC")
    fun getAllWishlist(): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(item: WishlistEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlists(items: List<WishlistEntity>)

    @Update
    suspend fun updateWishlist(item: WishlistEntity)

    @Delete
    suspend fun deleteWishlist(item: WishlistEntity)

    @Query("UPDATE wishlist SET savedAmount = savedAmount + :amount WHERE id = :id")
    suspend fun allocateSavingsToWishlist(id: Long, amount: Long)

    // === BILLS (TAGIHAN RUTIN) ===
    @Query("SELECT * FROM bills ORDER BY dueDayOfMonth ASC, title ASC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<BillEntity>)

    @Update
    suspend fun updateBill(bill: BillEntity)

    @Delete
    suspend fun deleteBill(bill: BillEntity)

    // === DEBT & LOAN (HUTANG PIUTANG) ===
    @Query("SELECT * FROM debt_loans ORDER BY isSettled ASC, createdAt DESC")
    fun getAllDebtsAndLoans(): Flow<List<DebtLoanEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtLoan(debtLoan: DebtLoanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtLoans(debtLoans: List<DebtLoanEntity>)

    @Update
    suspend fun updateDebtLoan(debtLoan: DebtLoanEntity)

    @Delete
    suspend fun deleteDebtLoan(debtLoan: DebtLoanEntity)

    // === PRICE TRACKER (PANTAU HARGA PASAR) ===
    @Query("SELECT * FROM price_tracker ORDER BY itemName ASC")
    fun getAllPriceTracks(): Flow<List<PriceTrackerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceTrack(track: PriceTrackerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceTracks(tracks: List<PriceTrackerEntity>)

    @Update
    suspend fun updatePriceTrack(track: PriceTrackerEntity)

    @Delete
    suspend fun deletePriceTrack(track: PriceTrackerEntity)

    // === RAW BACKUP / RESTORE SUPPORT ===
    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity>

    @Query("SELECT * FROM budget_categories")
    suspend fun getAllBudgetCategoriesSnapshot(): List<BudgetCategoryEntity>

    @Query("SELECT * FROM wishlist")
    suspend fun getAllWishlistSnapshot(): List<WishlistEntity>

    @Query("SELECT * FROM bills")
    suspend fun getAllBillsSnapshot(): List<BillEntity>

    @Query("SELECT * FROM debt_loans")
    suspend fun getAllDebtsSnapshot(): List<DebtLoanEntity>

    @Query("SELECT * FROM price_tracker")
    suspend fun getAllPriceTracksSnapshot(): List<PriceTrackerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTransactions(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM budget_categories")
    suspend fun clearBudgetCategories()

    @Query("DELETE FROM wishlist")
    suspend fun clearWishlist()

    @Query("DELETE FROM bills")
    suspend fun clearBills()

    @Query("DELETE FROM debt_loans")
    suspend fun clearDebts()

    @Query("DELETE FROM price_tracker")
    suspend fun clearPriceTracks()
}
