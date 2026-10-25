package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val indonesianLocale = Locale.forLanguageTag("id-ID")

    /**
     * Formats a Long number into Indonesian Rupiah format without decimals.
     * Example: 35000 -> "Rp 35.000"
     * Example: 1500000 -> "Rp 1.500.000"
     */
    fun formatRupiah(amount: Long, withPrefix: Boolean = true): String {
        val numberFormat = NumberFormat.getNumberInstance(indonesianLocale)
        val formattedNumber = numberFormat.format(amount)
        return if (withPrefix) {
            "Rp $formattedNumber"
        } else {
            formattedNumber
        }
    }

    /**
     * Compact format for space-constrained UI (e.g., charts or small badges)
     * Example: 1500000 -> "Rp 1,5 jt"
     */
    fun formatCompactRupiah(amount: Long): String {
        return when {
            amount >= 1_000_000_000 -> String.format(indonesianLocale, "Rp %.1f M", amount / 1_000_000_000.0)
            amount >= 1_000_000 -> String.format(indonesianLocale, "Rp %.1f jt", amount / 1_000_000.0)
            amount >= 1_000 -> String.format(indonesianLocale, "Rp %.0f rb", amount / 1_000.0)
            else -> "Rp $amount"
        }
    }
}
