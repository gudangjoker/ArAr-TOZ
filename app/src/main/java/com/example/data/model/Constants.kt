package com.example.data.model

object TransactionTypes {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

object WalletSources {
    const val CASH = "Dompet Tunai"
    const val BANK = "Rekening Bank"
    const val EWALLET = "e-Wallet"

    val ALL = listOf(CASH, BANK, EWALLET)
}

object DefaultExpenseCategories {
    const val DAPUR = "Belanja Dapur & Sayur"
    const val BULANAN = "Belanja Bulanan & Supermarket"
    const val ANAK = "Jajan Anak & Sekolah"
    const val LISTRIK = "Listrik, Air & Gas"
    const val RUMAH = "Kebutuhan Rumah Tangga"
    const val KESEHATAN = "Kesehatan & Skincare"
    const val SEDEKAH = "Sedekah & Sosial"
    const val HIBURAN = "Hiburan & Rekreasi"
    const val LAINNYA = "Lain-lain"

    val ALL = listOf(
        DAPUR,
        BULANAN,
        ANAK,
        LISTRIK,
        RUMAH,
        KESEHATAN,
        SEDEKAH,
        HIBURAN,
        LAINNYA
    )

    fun getIconEmoji(category: String): String {
        return when (category) {
            DAPUR -> "🥬"
            BULANAN -> "🛒"
            ANAK -> "🎒"
            LISTRIK -> "💡"
            RUMAH -> "🏠"
            KESEHATAN -> "💊"
            SEDEKAH -> "🤲"
            HIBURAN -> "☕"
            "Gaji Suami / Nafkah" -> "💵"
            "Bonus / Tambahan" -> "🎁"
            "Hasil Usaha" -> "📈"
            else -> "📝"
        }
    }
}

object DefaultIncomeCategories {
    const val NAFKAH = "Gaji Suami / Nafkah"
    const val USAHA = "Hasil Usaha"
    const val BONUS = "Bonus / Tambahan"
    const val LAINNYA = "Pemasukan Lainnya"

    val ALL = listOf(NAFKAH, USAHA, BONUS, LAINNYA)
}

object WishlistCategories {
    const val KITCHEN = "Belanja Dapur & Pasar"
    const val HOUSEHOLD = "Kebutuhan Rumah"
    const val PERSONAL = "Keinginan Pribadi"

    val ALL = listOf(KITCHEN, HOUSEHOLD, PERSONAL)
}

object WishlistPriorities {
    const val HIGH = "Tinggi"
    const val MEDIUM = "Sedang"
    const val LOW = "Rendah"

    val ALL = listOf(HIGH, MEDIUM, LOW)
}

object DebtTypes {
    const val PAYABLE = "HUTANG_SAYA"    // Saya Berhutang (Kewajiban Bayar)
    const val RECEIVABLE = "PIUTANG_SAYA" // Piutang (Orang Berhutang ke Saya)
}
