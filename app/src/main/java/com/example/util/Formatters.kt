package com.example.util

import java.text.NumberFormat
import java.util.Locale

object ChipFormatter {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    fun formatCompact(amount: Long): String {
        return when {
            amount >= 1_000_000L -> {
                val m = amount / 1_000_000.0
                if (m % 1.0 == 0.0) "${m.toLong()}M" else String.format(Locale.US, "%.1fM", m)
            }
            amount >= 10_000L -> {
                val k = amount / 1_000.0
                if (k % 1.0 == 0.0) "${k.toLong()}K" else String.format(Locale.US, "%.1fK", k)
            }
            else -> numberFormat.format(amount)
        }
    }

    fun formatFull(amount: Long): String = numberFormat.format(amount)
}
