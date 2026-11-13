package com.example.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object CurrencyFormatter {
    private val indianSymbols = DecimalFormatSymbols(Locale("en", "IN"))

    fun formatRupees(amount: Double, showPlusSign: Boolean = false): String {
        val safeAmount = if (amount.isFinite()) amount else 0.0
        val roundedCents = (safeAmount * 100.0).roundToLong()
        val isNegative = roundedCents < 0L
        val absValue = abs(roundedCents) / 100.0

        val hasFraction = (abs(roundedCents) % 100L) != 0L
        val pattern = if (hasFraction) "##,##,##0.00" else "##,##,##0"
        val formatter = DecimalFormat(pattern, indianSymbols)
        val formattedNumber = formatter.format(absValue)

        return when {
            isNegative -> "-₹$formattedNumber"
            showPlusSign && roundedCents > 0L -> "+₹$formattedNumber"
            else -> "₹$formattedNumber"
        }
    }

    fun formatCompactNumber(amount: Double): String {
        val safe = if (amount.isFinite()) abs(amount) else 0.0
        return when {
            safe >= 1_00_00_000 -> String.format(Locale.US, "%.1fCr", safe / 1_00_00_000)
            safe >= 1_00_000 -> String.format(Locale.US, "%.1fL", safe / 1_00_000)
            safe >= 1_000 -> String.format(Locale.US, "%.1fk", safe / 1_000)
            else -> safe.roundToLong().toString()
        }
    }

    fun parseValidTransactionAmount(input: String): Double? {
        val cleaned = input.trim().replace(",", "").replace("₹", "")
        if (cleaned.isEmpty()) return null
        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (!parsed.isFinite() || parsed <= 0.0 || parsed > 99_99_99_999.0) return null
        // Round to 2 decimal places cleanly
        return (parsed * 100.0).roundToLong() / 100.0
    }

    fun parseValidOpeningBalance(input: String): Double? {
        val cleaned = input.trim().replace(",", "").replace("₹", "")
        if (cleaned.isEmpty()) return null
        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (!parsed.isFinite() || parsed < 0.0 || parsed > 99_99_99_999.0) return null
        return (parsed * 100.0).roundToLong() / 100.0
    }
}
