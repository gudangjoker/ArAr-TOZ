package com.example.data.model

data class CategoryExpenseSummary(
    val category: String,
    val totalSpent: Long
)

enum class BudgetEnvelopeStatus {
    GREEN,  // < 70%
    YELLOW, // 70% - 90%
    RED     // > 90% or exceeded
}

data class BudgetEnvelopeItem(
    val id: Long,
    val categoryName: String,
    val monthlyLimit: Long,
    val spentAmount: Long,
    val iconEmoji: String,
    val colorHex: String
) {
    val remainingAmount: Long
        get() = monthlyLimit - spentAmount

    val percentage: Float
        get() = if (monthlyLimit > 0) (spentAmount.toFloat() / monthlyLimit.toFloat()) else 0f

    val percentageDisplay: Int
        get() = (percentage * 100).toInt()

    val status: BudgetEnvelopeStatus
        get() = when {
            monthlyLimit <= 0L -> if (spentAmount > 0L) BudgetEnvelopeStatus.YELLOW else BudgetEnvelopeStatus.GREEN
            percentage < 0.70f -> BudgetEnvelopeStatus.GREEN
            percentage <= 0.90f -> BudgetEnvelopeStatus.YELLOW
            else -> BudgetEnvelopeStatus.RED
        }

    val isSafe: Boolean
        get() = status == BudgetEnvelopeStatus.GREEN

    val isWarning: Boolean
        get() = status == BudgetEnvelopeStatus.YELLOW

    val isAlert: Boolean
        get() = status == BudgetEnvelopeStatus.RED
}
