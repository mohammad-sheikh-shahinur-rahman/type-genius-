package com.example.data.dictionary

import kotlin.math.abs
import kotlin.math.roundToLong

object OfflineMathEvaluator {

    data class MathResult(
        val expressionWithoutEquals: String,
        val resultString: String,
        val equationString: String
    )

    private val digitNormalizeMap = mapOf(
        // Bengali
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9',
        // Devanagari
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9',
        // Arabic-Indic
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '×' to '*', '÷' to '/'
    )

    private val asciiToBengaliDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    fun evaluateIfTriggered(rawToken: String): MathResult? {
        val trimmed = rawToken.trim()
        if (!trimmed.endsWith("=") || trimmed.length < 3) return null
        val exprOriginal = trimmed.dropLast(1).trim()
        if (exprOriginal.isEmpty()) return null

        val normalized = exprOriginal.map { digitNormalizeMap[it] ?: it }.joinToString("")
        if (!normalized.any { it in listOf('+', '-', '*', '/', '%') }) return null
        if (!normalized.all { it.isDigit() || it in charArrayOf('+', '-', '*', '/', '%', '.', '(', ')', ' ') }) {
            return null
        }

        val value = try {
            ExpressionParser(normalized).parse()
        } catch (_: Exception) {
            return null
        }

        if (value.isNaN() || value.isInfinite()) return null

        val formattedAscii = formatNumber(value)
        val hasBengaliDigits = exprOriginal.any { it in '০'..'৯' }
        val resultLocalized = if (hasBengaliDigits) {
            formattedAscii.map { asciiToBengaliDigits[it] ?: it }.joinToString("")
        } else {
            formattedAscii
        }

        return MathResult(
            expressionWithoutEquals = exprOriginal,
            resultString = resultLocalized,
            equationString = "$exprOriginal=$resultLocalized"
        )
    }

    private fun formatNumber(value: Double): String {
        val rounded = value.roundToLong()
        return if (abs(value - rounded.toDouble()) < 1e-9) {
            rounded.toString()
        } else {
            val s = "%.4f".format(java.util.Locale.US, value)
            s.trimEnd('0').trimEnd('.')
        }
    }

    private class ExpressionParser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            pos++
            ch = if (pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            while (ch == ' '.code) nextChar()
            if (pos < input.length) throw IllegalArgumentException("Unexpected char: ${ch.toChar()}")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> {
                        val divisor = parseFactor()
                        if (abs(divisor) < 1e-12) throw ArithmeticException("Division by zero")
                        x /= divisor
                    }
                    eat('%'.code) -> {
                        val divisor = parseFactor()
                        if (abs(divisor) < 1e-12) throw ArithmeticException("Modulo by zero")
                        x %= divisor
                    }
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            val startPos = pos
            val x: Double
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = input.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected token")
            }
            return x
        }
    }
}
