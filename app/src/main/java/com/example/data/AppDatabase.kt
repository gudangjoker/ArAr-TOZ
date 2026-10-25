package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FinanceDao
import com.example.data.entity.BillEntity
import com.example.data.entity.BudgetCategoryEntity
import com.example.data.entity.DebtLoanEntity
import com.example.data.entity.PriceTrackerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.entity.WishlistEntity
import com.example.data.model.DefaultExpenseCategories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        BudgetCategoryEntity::class,
        WishlistEntity::class,
        BillEntity::class,
        DebtLoanEntity::class,
        PriceTrackerEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope? = null): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dompet_rumah_tangga.db"
                )
                    .addCallback(DatabaseCallback(scope ?: CoroutineScope(Dispatchers.IO)))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.financeDao())
                }
            }
        }

        suspend fun populateInitialData(dao: FinanceDao) {
            // Pos amplop anggaran kategori dasar (bersih tanpa transaksi palsu)
            val defaultBudgets = listOf(
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.DAPUR,
                    monthlyLimit = 0L,
                    iconEmoji = "🥬",
                    colorHex = "#2E7D32"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.BULANAN,
                    monthlyLimit = 0L,
                    iconEmoji = "🛒",
                    colorHex = "#00838F"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.ANAK,
                    monthlyLimit = 0L,
                    iconEmoji = "🎒",
                    colorHex = "#F57C00"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.LISTRIK,
                    monthlyLimit = 0L,
                    iconEmoji = "💡",
                    colorHex = "#FBC02D"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.RUMAH,
                    monthlyLimit = 0L,
                    iconEmoji = "🏠",
                    colorHex = "#5E35B1"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.KESEHATAN,
                    monthlyLimit = 0L,
                    iconEmoji = "💊",
                    colorHex = "#D81B60"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.SEDEKAH,
                    monthlyLimit = 0L,
                    iconEmoji = "🤲",
                    colorHex = "#43A047"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.HIBURAN,
                    monthlyLimit = 0L,
                    iconEmoji = "☕",
                    colorHex = "#8D6E63"
                ),
                BudgetCategoryEntity(
                    categoryName = DefaultExpenseCategories.LAINNYA,
                    monthlyLimit = 0L,
                    iconEmoji = "📝",
                    colorHex = "#546E7A"
                )
            )
            dao.insertBudgetCategories(defaultBudgets)
        }
    }
}
