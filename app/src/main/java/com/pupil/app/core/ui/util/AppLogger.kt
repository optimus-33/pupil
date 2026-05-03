package com.pupil.app.core.ui.util

import android.util.Log

/**
 * Centralized logging utility for the Pupil app.
 * All log entries are prefixed with "PupilApp/" for easy filtering in logcat.
 *
 * Usage:
 *   AppLogger.d("ViewModel", "User clicked save")
 *   AppLogger.e("Network", "Failed to connect", exception)
 */
object AppLogger {
    private const val TAG_PREFIX = "PupilApp"

    private fun tag(section: String): String = "$TAG_PREFIX/$section"

    /**
     * Debug log — for detailed development-time information.
     */
    fun d(section: String, message: String) {
        Log.d(tag(section), message)
    }

    /**
     * Info log — for normal operational messages.
     */
    fun i(section: String, message: String) {
        Log.i(tag(section), message)
    }

    /**
     * Warning log — for recoverable issues.
     */
    fun w(section: String, message: String, throwable: Throwable? = null) {
        Log.w(tag(section), message, throwable)
    }

    /**
     * Error log — for failures that need attention.
     */
    fun e(section: String, message: String, throwable: Throwable? = null) {
        Log.e(tag(section), message, throwable)
    }
}
