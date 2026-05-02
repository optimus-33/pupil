package com.pupil.app.core.ui.util

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri

data class InstalledUpiApp(
    val displayName: String,
    val packageName: String
)

object InstalledUpiAppsResolver {

    /**
     * Queries the device PackageManager for all installed apps that can handle
     * a UPI payment intent (upi:// scheme).
     */
    fun getInstalledUpiApps(packageManager: PackageManager): List<InstalledUpiApp> {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay"))
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        return resolveInfos
            .filter { it.activityInfo != null && it.activityInfo.packageName.isNotBlank() }
            .distinctBy { it.activityInfo.packageName }
            .map { resolveInfo ->
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val packageName = resolveInfo.activityInfo.packageName
                InstalledUpiApp(
                    displayName = appName,
                    packageName = packageName
                )
            }
            .sortedBy { it.displayName }
    }
}
