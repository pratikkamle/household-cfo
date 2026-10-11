package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    /**
     * Formats an amount using the Indian numbering system (e.g. ₹3,66,612)
     */
    fun formatInr(amount: Double, includeSymbol: Boolean = true, showDecimals: Boolean = false): String {
        val isNegative = amount < 0
        val absAmount = abs(amount)

        val longPart = absAmount.toLong()
        val decimalPart = ((absAmount - longPart) * 100).toInt()

        val longStr = longPart.toString()
        val formattedNumber = if (longStr.length <= 3) {
            longStr
        } else {
            val lastThree = longStr.substring(longStr.length - 3)
            val rest = longStr.substring(0, longStr.length - 3)
            val groups = mutableListOf<String>()
            var i = rest.length
            while (i > 0) {
                val start = maxOf(0, i - 2)
                groups.add(0, rest.substring(start, i))
                i -= 2
            }
            groups.joinToString(",") + "," + lastThree
        }

        val fullFormatted = if (showDecimals && decimalPart > 0) {
            val decStr = if (decimalPart < 10) "0$decimalPart" else decimalPart.toString()
            "$formattedNumber.$decStr"
        } else {
            formattedNumber
        }

        val symbol = if (includeSymbol) "₹" else ""
        return if (isNegative) "-$symbol$fullFormatted" else "$symbol$fullFormatted"
    }

    /**
     * Compact format: ₹3.67 L, ₹41.2 K, ₹1.82 Cr
     */
    fun formatCompact(amount: Double): String {
        val abs = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 10000000 -> sign + "₹" + String.format(Locale.US, "%.2f Cr", abs / 10000000.0)
            abs >= 100000 -> sign + "₹" + String.format(Locale.US, "%.2f L", abs / 100000.0)
            abs >= 1000 -> sign + "₹" + String.format(Locale.US, "%.1f K", abs / 1000.0)
            else -> formatInr(amount)
        }
    }

    /**
     * Formats day of month with proper ordinal suffix (1st, 2nd, 3rd, 5th, etc.)
     */
    fun formatDayOfMonth(day: Int): String {
        return when {
            day in 11..13 -> "${day}th"
            day % 10 == 1 -> "${day}st"
            day % 10 == 2 -> "${day}nd"
            day % 10 == 3 -> "${day}rd"
            else -> "${day}th"
        }
    }
}
