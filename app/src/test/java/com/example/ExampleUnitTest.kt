package com.example

import com.example.data.model.BudgetEnvelopeItem
import com.example.data.model.BudgetEnvelopeStatus
import com.example.util.CurrencyFormatter
import com.example.util.MathExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMathExpressionEvaluator_simpleAddition() {
        val result = MathExpressionEvaluator.evaluateMathExpression("15000+8000+12000")
        assertEquals(35000L, result)
    }

    @Test
    fun testMathExpressionEvaluator_withSpacesAndMultiplication() {
        val result = MathExpressionEvaluator.evaluateMathExpression("25000 * 2 + 10000")
        assertEquals(60000L, result)
    }

    @Test
    fun testMathExpressionEvaluator_withSubtractionAndDivision() {
        val result = MathExpressionEvaluator.evaluateMathExpression("100000 - 30000 / 2")
        assertEquals(85000L, result)
    }

    @Test
    fun testMathExpressionEvaluator_withParentheses() {
        val result1 = MathExpressionEvaluator.evaluateMathExpression("(10000 + 5000) * 2")
        assertEquals(30000L, result1)

        val result2 = MathExpressionEvaluator.evaluateMathExpression("(100000 - 30000) / 2")
        assertEquals(35000L, result2)
    }

    @Test
    fun testMathExpressionEvaluator_withIndonesianThousandSeparators() {
        val result = MathExpressionEvaluator.evaluateMathExpression("15.000 + 5.000 + 1.500.000")
        assertEquals(1520000L, result)
    }

    @Test
    fun testMathExpressionEvaluator_withDecimalsAndFractions() {
        // 1,5 kg ayam @ Rp 20.000
        val result1 = MathExpressionEvaluator.evaluateMathExpression("1,5 * 20000")
        assertEquals(30000L, result1)

        // 0.5 kg daging @ Rp 120.000
        val result2 = MathExpressionEvaluator.evaluateMathExpression("0.5 * 120.000")
        assertEquals(60000L, result2)
    }

    @Test
    fun testMathExpressionEvaluator_invalidOrNegativeInput() {
        assertEquals(0L, MathExpressionEvaluator.evaluateMathExpression(""))
        assertEquals(0L, MathExpressionEvaluator.evaluateMathExpression("abc"))
        assertEquals(0L, MathExpressionEvaluator.evaluateMathExpression("10000 - 50000"))
    }

    @Test
    fun testCurrencyFormatter_rupiah() {
        val formatted = CurrencyFormatter.formatRupiah(35000L)
        assertTrue(formatted.contains("35.000") || formatted.contains("35,000"))
        assertTrue(formatted.startsWith("Rp"))
    }

    @Test
    fun testBudgetEnvelopeStatus_calculations() {
        val safeItem = BudgetEnvelopeItem(
            id = 1,
            categoryName = "Sayur",
            monthlyLimit = 1_000_000L,
            spentAmount = 500_000L,
            iconEmoji = "🥬",
            colorHex = "#000000"
        )
        assertEquals(BudgetEnvelopeStatus.GREEN, safeItem.status)

        val warningItem = BudgetEnvelopeItem(
            id = 2,
            categoryName = "Jajan",
            monthlyLimit = 1_000_000L,
            spentAmount = 800_000L,
            iconEmoji = "🎒",
            colorHex = "#000000"
        )
        assertEquals(BudgetEnvelopeStatus.YELLOW, warningItem.status)

        val alertItem = BudgetEnvelopeItem(
            id = 3,
            categoryName = "Listrik",
            monthlyLimit = 1_000_000L,
            spentAmount = 950_000L,
            iconEmoji = "💡",
            colorHex = "#000000"
        )
        assertEquals(BudgetEnvelopeStatus.RED, alertItem.status)
    }
}
