package com.example

import com.example.data.model.BackupData
import com.example.data.model.BudgetEnvelopeItem
import com.example.data.model.BudgetEnvelopeStatus
import com.example.util.DateTimeUtils
import com.example.util.MathExpressionEvaluator
import com.google.gson.Gson
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEnvelopeReconstructionTest {

    @Test
    fun testEnvelopeValidation_rejectsBlankName() {
        val blankName1 = "   "
        val blankName2 = ""
        val validName = "  Belanja Dapur  "

        assertFalse(blankName1.trim().isNotBlank())
        assertFalse(blankName2.trim().isNotBlank())
        assertTrue(validName.trim().isNotBlank())
        assertEquals("Belanja Dapur", validName.trim())
    }

    @Test
    fun testEnvelopeValidation_evaluatesLimitExpression() {
        val simpleLimit = "500000"
        val expressionLimit = "250000 * 2 + 100000"
        val zeroLimit = "0"
        val negativeOrInvalid = "-50000"

        assertEquals(500000L, MathExpressionEvaluator.evaluateMathExpression(simpleLimit))
        assertEquals(600000L, MathExpressionEvaluator.evaluateMathExpression(expressionLimit))
        assertEquals(0L, MathExpressionEvaluator.evaluateMathExpression(zeroLimit))
        assertEquals(0L, MathExpressionEvaluator.evaluateMathExpression(negativeOrInvalid))

        assertTrue(MathExpressionEvaluator.evaluateMathExpression(simpleLimit) > 0)
        assertTrue(MathExpressionEvaluator.evaluateMathExpression(expressionLimit) > 0)
        assertFalse(MathExpressionEvaluator.evaluateMathExpression(zeroLimit) > 0)
        assertFalse(MathExpressionEvaluator.evaluateMathExpression(negativeOrInvalid) > 0)
    }

    @Test
    fun testEnvelopeItem_spendingAndRemainingCalculations() {
        val envelope = BudgetEnvelopeItem(
            id = 101L,
            categoryName = "Belanja Mingguan",
            monthlyLimit = 1_500_000L,
            spentAmount = 600_000L,
            iconEmoji = "🛒",
            colorHex = "#2E7D32"
        )

        assertEquals(900_000L, envelope.remainingAmount)
        assertEquals(40, (envelope.percentage * 100).toInt())
        assertEquals(BudgetEnvelopeStatus.GREEN, envelope.status)
        assertTrue(envelope.isSafe)
        assertFalse(envelope.isWarning)
        assertFalse(envelope.isAlert)
    }

    @Test
    fun testEnvelopeItem_overBudgetRemaining() {
        val envelope = BudgetEnvelopeItem(
            id = 102L,
            categoryName = "Skincare Bunda",
            monthlyLimit = 500_000L,
            spentAmount = 650_000L,
            iconEmoji = "💄",
            colorHex = "#D81B60"
        )

        assertEquals(-150_000L, envelope.remainingAmount)
        assertEquals(BudgetEnvelopeStatus.RED, envelope.status)
        assertTrue(envelope.isAlert)
    }

    @Test
    fun testEnvelopeItem_warningThreshold() {
        val envelope = BudgetEnvelopeItem(
            id = 103L,
            categoryName = "Jajan Anak",
            monthlyLimit = 1_000_000L,
            spentAmount = 850_000L,
            iconEmoji = "🎒",
            colorHex = "#F57C00"
        )

        assertEquals(150_000L, envelope.remainingAmount)
        assertEquals(BudgetEnvelopeStatus.YELLOW, envelope.status)
        assertTrue(envelope.isWarning)
    }

    @Test
    fun testWishlistDeduction_affectsBudgetEnvelopeAndBalance() {
        val initialBalance = 2_000_000L
        val envelopeLimit = 500_000L
        var spentAmount = 100_000L

        val envelope = BudgetEnvelopeItem(
            id = 1L,
            categoryName = "Bahan Dapur",
            monthlyLimit = envelopeLimit,
            spentAmount = spentAmount,
            iconEmoji = "🍳",
            colorHex = "#FF5722"
        )
        assertEquals(400_000L, envelope.remainingAmount)

        // Simulating shopping item lunas: 75_000
        val wishlistItemPrice = 75_000L
        spentAmount += wishlistItemPrice
        val updatedBalance = initialBalance - wishlistItemPrice

        val updatedEnvelope = envelope.copy(spentAmount = spentAmount)

        assertEquals(175_000L, updatedEnvelope.spentAmount)
        assertEquals(325_000L, updatedEnvelope.remainingAmount)
        assertEquals(1_925_000L, updatedBalance)
    }

    @Test
    fun testBackupDataSerialization_roundTripValidatesLists() {
        val backup = BackupData(
            appVersion = "1.0.1",
            exportedAtJakarta = "25 Okt 2026, 16:00 WIB",
            exportedTimestamp = 1730000000000L,
            transactions = emptyList(),
            budgetCategories = emptyList(),
            wishlist = emptyList()
        )
        val json = Gson().toJson(backup)
        val deserialized = Gson().fromJson(json, BackupData::class.java)

        assertEquals("1.0.1", deserialized.appVersion)
        assertEquals(1730000000000L, deserialized.exportedTimestamp)
        assertTrue(deserialized.transactions.isEmpty())
        assertTrue(deserialized.bills.isEmpty())
        assertTrue(deserialized.debts.isEmpty())
    }

    @Test
    fun testCategoryNormalization_caseAndWhitespaceInsensitiveMatching() {
        val rawTxCategory1 = "  Belanja Dapur "
        val rawTxCategory2 = "belanja dapur"
        val rawTxCategory3 = "BELANJA DAPUR"
        val posEnvelopeCategory = "Belanja Dapur"

        val normalizedKey = posEnvelopeCategory.trim().lowercase()

        assertEquals(normalizedKey, rawTxCategory1.trim().lowercase())
        assertEquals(normalizedKey, rawTxCategory2.trim().lowercase())
        assertEquals(normalizedKey, rawTxCategory3.trim().lowercase())

        // Map lookup simulation
        val spendingMap = mapOf(
            rawTxCategory1.trim().lowercase() to 150_000L
        )

        val spent = spendingMap[posEnvelopeCategory.trim().lowercase()] ?: 0L
        assertEquals(150_000L, spent)
    }

    @Test
    fun testIndonesianTimezones_abbreviations() {
        val wib = TimeZone.getTimeZone("Asia/Jakarta")
        val wita = TimeZone.getTimeZone("Asia/Makassar")
        val wit = TimeZone.getTimeZone("Asia/Jayapura")

        assertEquals("WIB", DateTimeUtils.getTimeZoneAbbreviation(wib))
        assertEquals("WITA", DateTimeUtils.getTimeZoneAbbreviation(wita))
        assertEquals("WIT", DateTimeUtils.getTimeZoneAbbreviation(wit))
    }
}
