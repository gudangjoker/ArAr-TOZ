package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CurrencyFormatter
import com.example.util.DateTimeUtils
import com.example.util.MathExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ArAr TOZ", appName)
  }

  @Test
  fun `mini calculator math evaluator supports basic arithmetic`() {
    val result1 = MathExpressionEvaluator.evaluateMathExpression("15000+8000+12000")
    assertEquals(35_000L, result1)

    val result2 = MathExpressionEvaluator.evaluateMathExpression("50000 - 15000")
    assertEquals(35_000L, result2)

    val result3 = MathExpressionEvaluator.evaluateMathExpression("12500 * 4")
    assertEquals(50_000L, result3)
  }

  @Test
  fun `currency formatter formats rupiah without decimal zeros`() {
    val formatted = CurrencyFormatter.formatRupiah(35_000L)
    assertTrue(formatted.startsWith("Rp"))
    assertTrue(formatted.contains("35.000"))
  }

  @Test
  fun `date time utils returns valid month year key`() {
    val monthYearKey = DateTimeUtils.getCurrentMonthYearKey()
    assertTrue(monthYearKey.contains("-"))
    assertEquals(7, monthYearKey.length) // MM-yyyy
  }
}
