package com.pupil.app.core.ui.util

import android.net.Uri

object UpiParser {
    fun extractUpiId(rawValue: String?): String? {
        if (rawValue.isNullOrBlank()) return null
        val normalized = rawValue.trim()
        if (normalized.contains("upi://", ignoreCase = true)) {
            val uri = try {
                Uri.parse(normalized)
            } catch (ex: Exception) {
                null
            }
            val pa = uri?.getQueryParameter("pa")
            if (!pa.isNullOrBlank()) return pa
        }
        val candidate = normalized.split(Regex("[\\s,;]+"))
            .firstOrNull { it.contains("@") }
            ?.trim()
        return candidate
    }
}
