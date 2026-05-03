package com.pupil.app.core.ui.util

import android.net.Uri
import com.pupil.app.core.ui.util.AppLogger

object UpiParser {
    fun extractUpiId(rawValue: String?): String? {
        if (rawValue.isNullOrBlank()) return null
        val normalized = rawValue.trim()
        if (normalized.contains("upi://", ignoreCase = true)) {
            val uri = try {
                Uri.parse(normalized)
            } catch (ex: Exception) {
                AppLogger.w("UpiParser", "Failed to parse UPI URI: ${normalized.take(50)}", ex)
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

    /**
     * Extracts the merchant code (mc) parameter from a UPI QR raw value.
     * Returns null if not present (e.g., for individual/person-to-person QR codes).
     */
    fun extractMerchantCode(rawValue: String?): String? {
        if (rawValue.isNullOrBlank()) return null
        val normalized = rawValue.trim()
        if (!normalized.contains("upi://", ignoreCase = true)) return null
        return try {
            Uri.parse(normalized).getQueryParameter("mc")
        } catch (ex: Exception) {
            AppLogger.w("UpiParser", "Failed to extract merchant code from: ${normalized.take(50)}", ex)
            null
        }
    }
}
