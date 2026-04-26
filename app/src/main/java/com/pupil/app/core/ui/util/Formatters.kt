package com.pupil.app.core.ui.util

import java.util.Locale

object Formatters {
    fun formatPaise(amountPaise: Long): String {
        val absPaise = kotlin.math.abs(amountPaise)
        val rupees = absPaise / 100
        val remainder = absPaise % 100
        return if (remainder == 0L) {
            "%d".format(Locale.getDefault(), rupees)
        } else {
            "%d.%02d".format(Locale.getDefault(), rupees, remainder)
        }
    }
}

fun String.toPaise(): Long? {
    val normalized = trim().replace("[^0-9.]".toRegex(), "")
    if (normalized.isBlank()) return null
    val parts = normalized.split('.')
    val rupees = parts.getOrNull(0)?.toLongOrNull() ?: return null
    val paisePart = parts.getOrNull(1)?.padEnd(2, '0')?.take(2)?.toLongOrNull() ?: 0L
    return rupees * 100 + paisePart
}
