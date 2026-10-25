package com.example.util

import kotlin.math.roundToLong

/**
 * Mini-calculator parser for transaction input.
 * Evaluates expressions like "15000+8000+12000", "50000 - 15000", "25000*2", "100000/2",
 * as well as parentheses "(10000 + 5000) * 2" and Indonesian decimals "1,5 * 20000" or "0.5 * 50.000".
 */
object MathExpressionEvaluator {

    private val THOUSAND_SEPARATOR_REGEX = Regex("""(?<=\d)[.,](?=\d{3}(?:\D|$))""")

    fun evaluateMathExpression(input: String): Long {
        val sanitized = sanitize(input)
        if (sanitized.isEmpty()) return 0L
        return try {
            val result = parseExpression(sanitized)
            if (result.isFinite() && result > 0.0) {
                result.roundToLong().coerceAtLeast(0L)
            } else {
                0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Checks if the input is a valid mathematical expression with at least one operator.
     */
    fun isCompoundExpression(input: String): Boolean {
        val cleaned = input.replace(" ", "")
        return cleaned.any {
            it == '+' || it == '-' || it == '*' || it == '/' ||
            it == 'x' || it == 'X' || it == ':' || it == '(' || it == ')'
        }
    }

    private fun sanitize(input: String): String {
        return input
            .replace(" ", "")
            .replace("x", "*")
            .replace("X", "*")
            .replace(":", "/")
            // Remove thousand separators (e.g. 15.000 or 15,000) while keeping decimals (e.g. 1.5 or 0,5)
            .replace(THOUSAND_SEPARATOR_REGEX, "")
            // Convert Indonesian decimal comma to dot
            .replace(",", ".")
    }

    private fun parseExpression(expression: String): Double {
        var pos = -1
        var ch = 0

        fun nextChar() {
            pos++
            ch = if (pos < expression.length) expression[pos].code else -1
        }

        fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parseFactor(parseExprRef: () -> Double): Double {
            if (eat('+'.code)) return +parseFactor(parseExprRef)
            if (eat('-'.code)) return -parseFactor(parseExprRef)

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExprRef()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = expression.substring(startPos, pos).toDoubleOrNull() ?: 0.0
            } else {
                return 0.0
            }
            return x
        }

        fun parseTerm(parseExprRef: () -> Double): Double {
            var x = parseFactor(parseExprRef)
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor(parseExprRef)
                    eat('/'.code) -> {
                        val divisor = parseFactor(parseExprRef)
                        x = if (divisor != 0.0) x / divisor else 0.0
                    }
                    else -> return x
                }
            }
        }

        fun parseExpr(): Double {
            var x = parseTerm(::parseExpr)
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm(::parseExpr)
                    eat('-'.code) -> x -= parseTerm(::parseExpr)
                    else -> return x
                }
            }
        }

        nextChar()
        return parseExpr()
    }
}
