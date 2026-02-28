package com.algotrader.app.util

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object FormatUtils {
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun formatCurrency(value: Double): String = currencyFormat.format(value)

    fun formatPercent(value: Double): String = "${if (value >= 0) "+" else ""}${"%.2f".format(value)}%"

    fun formatNumber(value: Double): String = numberFormat.format(value)

    fun formatCompactNumber(value: Double): String = when {
        value >= 1_000_000_000 -> "${"%.1f".format(value / 1_000_000_000)}B"
        value >= 1_000_000 -> "${"%.1f".format(value / 1_000_000)}M"
        value >= 1_000 -> "${"%.1f".format(value / 1_000)}K"
        else -> "%.2f".format(value)
    }

    fun formatVolume(volume: Long): String = when {
        volume >= 1_000_000_000 -> "${"%.1f".format(volume / 1_000_000_000.0)}B"
        volume >= 1_000_000 -> "${"%.1f".format(volume / 1_000_000.0)}M"
        volume >= 1_000 -> "${"%.1f".format(volume / 1_000.0)}K"
        else -> volume.toString()
    }

    fun formatDate(instant: Instant): String =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateFormatter)

    fun formatDateTime(instant: Instant): String =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(dateTimeFormatter)

    fun formatTime(instant: Instant): String =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(timeFormatter)

    fun formatDate(dateTime: LocalDateTime): String = dateTime.format(dateFormatter)

    fun formatPnl(value: Double): String {
        val sign = if (value >= 0) "+" else ""
        return "$sign${formatCurrency(value)}"
    }

    fun formatRatio(value: Double): String = "%.2f".format(value)
}
