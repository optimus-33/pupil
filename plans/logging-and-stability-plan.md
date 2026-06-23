# Logging & App Stability Plan

## 1. Button Text Fix (Already Done)

Changed [`PaymentEntryScreen.kt:529`](../app/src/main/java/com/pupil/app/feature/payment/PaymentEntryScreen.kt:529):
- **Before**: `Text(text = "Pay & Record")` — always showed "Pay & Record"
- **After**: `Text(text = if (isManualEntry) "Record" else "Pay & Record")` — shows "Record" when logging manually, "Pay & Record" when scanning/barcode entry

Build verified ✅

---

## 2. How to View Android Logs on Phone

### Option A: Log Viewer App on Phone (Easiest)

Install one of these free apps from Play Store:

| App | Package | Notes |
|-----|---------|-------|
| **Logcat Viewer** | `com.logcatviewer.logcat` | Simple, no root needed |
| **CatLog** | `com.nolanlawson.logcat` | Popular, search/filter support |
| **Logcat Reader** | `com.dp.logcatapp` | Material Design, dark theme |

These apps read the Android system log (logcat) — they can show logs from ALL apps including Pupil. Filter by tag or PID.

### Option B: ADB from Computer (Best)

```bash
# Filter only Pupil app logs
adb logcat --pid=$(adb shell pidof -s com.pupil.app)

# Filter by tag
adb logcat -s PupilApp

# Save to file
adb logcat -d > crash_logs.txt
```

### Option C: Wireless Debugging (ADB over Wi-Fi, No USB Cable)

Android 11+ supports wireless debugging from VS Code:
1. Phone: Developer options → Enable "Wireless debugging"
2. VS Code: Install "Android Debug Bridge" extension
3. Pair using QR code or code

---

## 3. Current Logging Audit

**The app has virtually NO logging** — only 2 `Log.e()` calls in [`QRScanScreen.kt`](../app/src/main/java/com/pupil/app/feature/payment/QRScanScreen.kt):

- Line 104: `Log.e("QRScanScreen", "Failed to read image", e)` — gallery read error
- Line 176: `Log.e("QRScanScreen", "Camera bind failed", error)` — camera bind error

**Everywhere else**: `try/catch` blocks swallow exceptions silently with no logs. This makes crash diagnosis nearly impossible.

---

## 4. Comprehensive Logging Strategy

### 4a. Add a Logging Utility Class

Create [`app/src/main/java/com/pupil/app/core/ui/util/AppLogger.kt`]:

```kotlin
object AppLogger {
    private const val APP_TAG = "PupilApp"

    fun d(tag: String, message: String) {
        Log.d("$APP_TAG/$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$APP_TAG/$tag", message, throwable)
    }

    fun i(tag: String, message: String) {
        Log.i("$APP_TAG/$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$APP_TAG/$tag", message, throwable)
    }
}
```

Using a consistent `PupilApp/` prefix lets you filter logs easily.

### 4b. Log at Every Layer

**DAOs** — Log database operations:
- `TransactionDao`: Log insert/update/delete counts, query sizes
- `CategoryDao`: Log category lookups
- `AccountDao`: Log balance computations

**Repositories** — Log data flow:
- `TransactionRepositoryImpl`: Log when `combine()` emits, category resolution
- `PaymentAppConfigRepositoryImpl`: Log config changes

**ViewModels** — Log user actions:
- `HomeViewModel`: Log delete/mark-completed/mark-failed actions
- `PaymentViewModel`: Log createPendingTransaction calls with params
- `SettingsViewModel`: Log export/import start/complete/errors

**Screens** — Log navigation and UI events:
- `PaymentEntryScreen`: Log UPI intent launch, app selection, button clicks
- `QRScanScreen`: Log QR detection results, barcode values
- `BackupManager`: Log export/import progress, file sizes

### 4c. Critical Points to Log

| Scenario | What to Log | Tag |
|----------|-------------|-----|
| App start | Database version, migration status | `PupilApp/Init` |
| Navigation | Screen transitions with args | `PupilApp/Nav` |
| UPI payment | Intent URI, app package, response extras | `PupilApp/UPI` |
| QR scan | Raw detected value, parsed UPI ID, merchant code | `PupilApp/QR` |
| Database | insert/update/delete row counts | `PupilApp/DB` |
| Backup | File URI, JSON size, row counts | `PupilApp/Backup` |
| Errors | Full stacktrace + context | `PupilApp/Error` |

---

## 5. Crash Reporting — Add Firebase Crashlytics

### Why
Logcat is reactive (you need to be watching). Crashlytics is proactive.

### Implementation Steps

**Step 1**: Add to [`app/build.gradle.kts`](../app/build.gradle.kts):
```kotlin
// Root-level
id("com.google.gms.google-services") version "4.4.0" apply false
id("com.google.firebase.crashlytics") version "2.9.9" apply false

// App-level
id("com.google.gms.google-services")
id("com.google.firebase.crashlytics")

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics")
}
```

**Step 2**: Create [`app/google-services.json`](../app/google-services.json) from Firebase Console

**Step 3**: Add custom crash logging:
```kotlin
FirebaseCrashlytics.getInstance().log("User navigated to PaymentEntry")
FirebaseCrashlytics.getInstance().setCustomKey("upi_id", resolvedUpiId)
```

---

## 6. Common Crash Causes to Investigate

These are the most likely crash sources on first launch:

### 6a. `lateinit var database` in PupilDatabase.kt (RACE CONDITION)

In [`PupilDatabase.kt:167-183`](../app/src/main/java/com/pupil/app/core/data/local/PupilDatabase.kt:167):
```kotlin
lateinit var database: PupilDatabase
// callback references database.paymentAppConfigDao() inside a coroutine
// database is assigned AFTER callback is created
```

While `build()` is synchronous, the coroutine inside `onCreate` runs on `Dispatchers.IO` and might execute after `build()` returns but before other DB calls. This is technically safe because `build()` assigns `database` before returning.

**Fix**: Move the launch inside the callback into a proper initialization:

```kotlin
override fun onCreate(db: SupportSQLiteDatabase) {
    super.onCreate(db)
    instance?.let { dbInstance ->
        CoroutineScope(Dispatchers.IO).launch {
            dbInstance.paymentAppConfigDao().insertAll(defaultApps)
        }
    }
}
```

### 6b. Missing `Instant` or Timezone issues on older Android versions

If any ViewModel or Screen uses `java.time.Instant` or `java.time.LocalDateTime`, these require API 26+. If the app's `minSdk` is lower, this will crash.

**Check**: [`app/build.gradle.kts`](../app/build.gradle.kts) `minSdk` value. If < 26 and we're using `java.time.*`, we need desugaring.

### 6c. Camera Permission Crash

If user denies camera permission and QRScanScreen tries to bind the camera without checking, it crashes.

**Fix**: Already has permission check at line 72, but verify it properly handles denial.

### 6d. Hilt Injection before Database Ready

If a ViewModel tries to access the database during injection/init before Room is fully initialized, it can crash.

**Fix**: Lazy initialization or `withContext(Dispatchers.IO)` for first DB access.

---

## 7. Additional Improvements for App Stability

### 7a. Global Error Handler
In [`PupilApp.kt`](../app/src/main/java/com/pupil/app/PupilApp.kt):
```kotlin
class PupilApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("PupilApp/Crash", "Uncaught in ${thread.name}", throwable)
            // Save crash to a file for next launch
            saveCrashReport(throwable)
        }
    }
}
```

### 7b. Safe Navigation with try/catch
In [`PupilNavGraph.kt`](../app/src/main/java/com/pupil/app/PupilNavGraph.kt), wrap navigation calls:
```kotlin
try {
    navController.navigate(route)
} catch (e: Exception) {
    AppLogger.e("Nav", "Navigation failed to $route", e)
}
```

### 7c. Input Validation on All Forms
- Amount: must be > 0 and valid number
- UPI ID: must contain `@` before launching intent
- Phone: must be 10 digits after stripping country code

### 7d. Database Integrity Check
Add a database integrity check on app startup:
```kotlin
database.query("PRAGMA integrity_check").moveToFirst().getString(0)
```
If it returns "ok", database is healthy. If not, show a dialog to backup and reset.

---

## 8. Implementation Roadmap

| # | Task | Effort | Depends On |
|---|------|--------|------------|
| 1 | Add `AppLogger` utility class | Small | None |
| 2 | Add logging to all `catch` blocks (15 locations) | Medium | #1 |
| 3 | Add lifecycle logging to all ViewModels | Medium | #1 |
| 4 | Add UPI intent/response logging in PaymentEntryScreen | Small | #1 |
| 5 | Add database operation logging in DAOs | Medium | #1 |
| 6 | Fix `lateinit var` race condition in PupilDatabase | Small | None |
| 7 | Add global uncaught exception handler | Small | None |
| 8 | Add Firebase Crashlytics (optional but recommended) | Medium | Firebase project |
| 9 | Input validation for all payment forms | Small | None |
| 10 | Safe navigation with try/catch | Small | None |
