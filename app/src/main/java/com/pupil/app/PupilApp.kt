package com.pupil.app

import android.app.Application
import android.os.Build
import com.pupil.app.core.ui.util.AppLogger
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class PupilApp : Application() {

    companion object {
        private const val TAG = "PupilApp/Init"
        private const val CRASH_DIR = "pupil_crashes"
    }

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(TAG, "App starting. SDK=${Build.VERSION.SDK_INT}, Device=${Build.MODEL}")
        installGlobalCrashHandler()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            // Initialize Firebase manually. Requires google-services.json in app/ directory.
            // For now, the app works without it — Crashlytics will simply not report.
            com.google.firebase.FirebaseApp.initializeApp(this)
            AppLogger.i(TAG, "Firebase initialized")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Firebase not available (add google-services.json to enable)", e)
        }
    }

    private fun installGlobalCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log to logcat
                AppLogger.e("Crash", "UNCAUGHT EXCEPTION in thread: ${thread.name}", throwable)

                // Save crash report to internal storage
                saveCrashReport(throwable)
            } catch (e: Exception) {
                AppLogger.e("Crash", "Failed to save crash report", e)
            } finally {
                // Always pass to default handler (which shows the crash dialog)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        AppLogger.i(TAG, "Global crash handler installed")
    }

    private fun saveCrashReport(throwable: Throwable) {
        val crashDir = File(filesDir, CRASH_DIR)
        if (!crashDir.exists()) crashDir.mkdirs()

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestamp = dateFormat.format(Date())
        val crashFile = File(crashDir, "crash_$timestamp.txt")

        FileWriter(crashFile).use { writer ->
            writer.appendLine("=== PUPIL CRASH REPORT ===")
            writer.appendLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            writer.appendLine("Device: ${Build.MODEL}")
            writer.appendLine("SDK: ${Build.VERSION.SDK_INT}")
            writer.appendLine("App Version: ${packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"}")
            writer.appendLine()
            writer.appendLine("=== EXCEPTION ===")
            writer.appendLine(throwable.toString())
            writer.appendLine()
            writer.appendLine("=== STACK TRACE ===")
            throwable.stackTrace.forEach { writer.appendLine("  $it") }
            throwable.cause?.let { cause ->
                writer.appendLine()
                writer.appendLine("=== CAUSED BY ===")
                writer.appendLine(cause.toString())
                cause.stackTrace.forEach { writer.appendLine("  $it") }
            }
            writer.appendLine()
            writer.appendLine("=== END ===")
        }
        AppLogger.i("Crash", "Crash report saved to ${crashFile.absolutePath}")
    }
}
