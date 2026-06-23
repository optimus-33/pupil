# Pupil — Complete Project Context

## 1. App Overview

**Pupil** is a personal spend tracker Android app with the core principle: **"record why, not just where."** It lets you log every payment with a reason (e.g., "coffee with Priya", "groceries at DMart"), not just where you spent. It integrates with UPI payment apps so you can pay and record simultaneously.

### Core Flow
1. User taps **+** FAB on Home → bottom sheet with 4 options
2. User pays via QR scan / UPI ID entry / Contact number / Manual log
3. Payment is recorded as **PENDING** in Room database
4. UPI app opens via `upi://pay` deep link intent
5. User completes/cancels payment → status updated to **COMPLETED** or **FAILED**
6. All transactions shown on Home, grouped by date

---

## 2. Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 1.9.22 |
| UI | Jetpack Compose (Material 3) | BOM 2024.04.00 |
| DI | Dagger Hilt | 2.48 |
| Database | Room | 2.6.1 |
| Camera | CameraX | 1.3.2 |
| QR Scanning | ML Kit Barcode Scanning | 17.2.0 |
| Navigation | Navigation Compose | 2.7.7 |
| Icons | Material Icons Extended | via BOM |
| Build | Gradle + AGP | 8.7 / 8.2.2 |
| compileSdk / minSdk / targetSdk | 34 / 24 / 34 |

**Important constraint:** Compose Compiler 1.5.8 is required for Kotlin 1.9.22. Do NOT bump Kotlin without also bumping the Compose compiler version.

---

## 3. Architecture: MVVM + Clean Architecture

```
com.pupil.app/
├── core/
│   ├── data/
│   │   ├── local/
│   │   │   ├── dao/          # Room DAOs (TransactionDao, PaymentAppConfigDao)
│   │   │   ├── entity/       # Room entities (TransactionEntity, PaymentAppConfigEntity)
│   │   │   └── PupilDatabase.kt
│   │   └── repository/       # Repository implementations
│   ├── domain/
│   │   ├── model/            # Domain models (Transaction, PaymentType, etc.)
│   │   ├── repository/       # Repository interfaces
│   │   └── usecase/          # Use cases
│   ├── di/AppModule.kt       # Hilt DI module
│   └── ui/
│       ├── components/       # Shared composables (TransactionCard, PaymentAppCard, etc.)
│       ├── theme/            # Material 3 theme (Colors, Typography)
│       └── util/             # Formatters, DateUtils, UpiParser, InstalledUpiAppsResolver
├── feature/
│   ├── home/                 # Home screen (HomeScreen, HomeViewModel)
│   ├── payment/              # Payment flow (PaymentEntryScreen, QRScanScreen, etc.)
│   ├── reports/              # Reports screen
│   └── settings/             # Settings screen (SettingsScreen, SettingsViewModel)
├── PupilNavGraph.kt          # Navigation graph (all routes)
├── PupilApp.kt               # @HiltAndroidApp Application class
└── MainComposeActivity.kt    # Single activity entry point
```

---

## 4. Database Schema

### Table: `transactions` (Room entity: `TransactionEntity`)

| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK, auto) | Auto-generated |
| merchantName | String | Payee name (e.g., "Rahul", "Grocery Store") |
| upiId | String? | UPI ID or mobile number |
| amountPaise | Long | Amount in paise (₹1 = 100 paise) |
| reason | String | Why you spent (e.g., "Sunday breakfast") |
| category | String | Category enum (Food, Transport, etc.) |
| paymentType | String | "UPI" or "UPI_CREDIT_CARD" |
| paymentApp | String | App name used (e.g., "Google Pay") |
| timestamp | Long | Unix epoch millis |
| isManual | Boolean | True if cash/manual entry |
| status | String | "PENDING", "COMPLETED", or "FAILED" |

### Table: `payment_app_configs` (Room entity: `PaymentAppConfigEntity`)

| Column | Type | Description |
|--------|------|-------------|
| id | Long (PK, auto) | Auto-generated |
| displayName | String | Human-readable name |
| packageName | String | Android package name |
| paymentType | String | "UPI" or "UPI_CREDIT_CARD" |
| enabled | Boolean | Whether the app is active |

**Database version:** 2 (uses `fallbackToDestructiveMigration()`)

### Pre-seeded apps (in `PupilDatabase.kt` `onCreate` callback):
- Google Pay (`com.google.android.apps.nbu.paisa.user`) — UPI
- PhonePe (`com.phonepe.app`) — UPI
- Paytm (`net.one97.paytm`) — UPI
- HDFC Bank (`com.snapwork.hdfc`) — UPI_CREDIT_CARD
- Axis Mobile (`com.axis.mobile`) — UPI_CREDIT_CARD
- ICICI iMobile (`com.csam.icici.bank.imobile`) — UPI_CREDIT_CARD

---

## 5. Route Map (`PupilNavGraph.kt`)

| Route | Screen | Purpose |
|-------|--------|---------|
| `home` | `HomeScreen` | Main screen with transaction list, FAB, bottom sheet |
| `qr_scan` | `QRScanScreen` | Camera-based QR scan + gallery picker |
| `upi_entry` | `UpiEntryScreen` | Manual UPI ID entry |
| `contacts_picker` | `ContactsPickerScreen` | Phone contacts picker with search |
| `payment_entry?upiId={upiId}&merchantName={merchantName}` | `PaymentEntryScreen` | Amount, reason, category, app selection, Pay button |
| `reports` | `ReportsScreen` | Weekly/monthly totals + category breakdown |
| `settings` | `SettingsScreen` | Payment app management, add custom apps |
| `edit_transaction/{transactionId}` | `EditTransactionScreen` | Edit existing transaction fields |

---

## 6. Screens Detail

### 6a. HomeScreen (`HomeScreen.kt` + `HomeViewModel.kt`)
- **State:** `HomeUiState(transactions: List<Transaction>, todayTotalPaise: Long)`
- **Data:** Combines `getAllTransactions()` (Flow) + `getTodayTotal()` (Flow) via `combine`
- **Init:** Auto-expires PENDING transactions older than 10 minutes → FAILED
- **Delete flow:** `requestDeleteTransaction(id)` → sets `pendingDeleteId` → shows AlertDialog → `confirmDeleteTransaction()` or `cancelDeleteTransaction()`
- **Features:**
  - Top bar with today's date + total spend
  - "Reports" and "Settings" text buttons in top bar
  - Quick stats summary card (primary container) showing today's total + pending count
  - Transaction list grouped by date (`DateUtils.formatDateGroup`)
  - Empty state with emoji + descriptive text
  - FAB → ModalBottomSheet with 4 options (Scan QR, Enter UPI ID, Mobile Number, Log Manually)
  - Each TransactionCard shows: merchant name, reason, amount, date/time, category badge, payment app, status badge (pending=orange, failed=red)
  - Action buttons on each card: Edit (gray pen), Delete (gray X), Mark Completed (teal Refresh), Mark Failed (red Cancel)

### 6b. QRScanScreen (`QRScanScreen.kt`)
- **Camera:** CameraX with `PreviewView`, lifecycle bound via `ProcessCameraProvider`
- **QR Detection:** ML Kit `BarcodeScanning.getClient()` in `ImageAnalysis.Analyzer`
- **Gallery fallback:** `ActivityResultContracts.GetContent("image/*")` → `scanBitmapForQr()`
- **UPI parsing:** `UpiParser.extractUpiId()` — looks for `pa` query param in `upi://pay?pa=...` URIs, or finds token containing `@`
- **Cleanup:** `DisposableEffect(Unit)` calls `cameraProvider.unbindAll()` on dispose
- **Result:** Detected VPA shown in overlay, "Continue" button navigates to PaymentEntryScreen

### 6c. UpiEntryScreen (`UpiEntryScreen.kt`)
- Simple text field for UPI ID input
- Validates presence of `@` symbol
- Navigates to PaymentEntryScreen with the entered UPI ID

### 6d. ContactsPickerScreen (`ContactsPickerScreen.kt`)
- Queries `ContactsContract.CommonDataKinds.Phone.CONTENT_URI`
- Strips spaces, hyphens, parentheses from numbers but **PRESERVES** `+` prefix
- Search bar filters by name or phone number
- On selection: navigates to PaymentEntryScreen with phone number as both `upiId` and `merchantName`

### 6e. PaymentEntryScreen (`PaymentEntryScreen.kt` + `PaymentViewModel.kt`)
- **Parameters:** `upiId: String?`, `merchantName: String?` (from navigation args)
- **Payment type toggle:** UPI / UPI Credit Card (chips)
- **App selection:** Shows installed UPI apps (from `InstalledUpiAppsResolver`) + DB apps in a LazyRow
- **Manual entry mode** (when `upiId` is null/blank):
  - Merchant/UPI ID field is editable
  - Payee Name field visible
  - Date/Time pickers shown
- **Fields:** Reason (large), Amount (with decimal support), Category chips, Date/Time pickers
- **Pay & Record button flow:**
  1. Haptic feedback (LongPress)
  2. Validate reason and amount
  3. Determine final UPI ID — handle mobile numbers via regex:
     ```kotlin
     val digitsOnly = finalUpiId?.replace(Regex("[^0-9]"), "") ?: ""
     val isMobileNumber = digitsOnly.length >= 10 && (has non-digit characters)
     val resolvedUpiId = if (isMobileNumber) digitsOnly.takeLast(10) else finalUpiId
     ```
  4. Derive payee name from: manual entry → mobile number → merchantName → UPI local part
  5. Save as PENDING via `PaymentViewModel.createPendingTransaction()`
  6. Open UPI app via `Intent(ACTION_VIEW, "upi://pay?pa=...&pn=...&am=...&cu=INR&tn=...")` with `setPackage(packageName)`
  7. If no app selected or manual entry: mark as COMPLETED immediately
- **Pending transaction flow:** After payment, user navigates back to Home where they can mark as COMPLETED or FAILED

### 6f. EditTransactionScreen (`EditTransactionScreen.kt` + `EditTransactionViewModel.kt`)
- Pre-filled fields from existing transaction
- Editable: reason, amount (digits + one decimal point), category, payment type, payment app, status, date/time
- Read-only: merchant name / UPI ID
- Confirmation dialog before saving
- Uses `TransactionUseCases.updateTransaction()` via Repository → DAO `update()` method

### 6g. SettingsScreen (`SettingsScreen.kt` + `SettingsViewModel.kt`)
- Lists DB payment apps grouped by type (UPI / UPI Credit Card)
- Toggle apps on/off via checkbox
- "Add custom app" button opens dialog with:
  - "Browse installed apps" button → searches apps via `queryIntentActivities(upi://pay)` — only UPI-capable apps
  - Manual name + package name fields
  - UPI / UPI Credit Card type selection
- New apps are added to DB via `PaymentAppConfigDao.insertAll()`

### 6h. ReportsScreen (`ReportsScreen.kt` + `ReportsViewModel.kt`)
- Shows weekly total (Monday to now)
- Shows monthly total (1st to now)
- Category breakdown (grouped by category for current month)
- Only counts COMPLETED transactions (`WHERE status = 'COMPLETED'` in DAO)

---

## 7. UPI Payment Flow — Deep Dive

### How UPI deep links work
When the user taps "Pay & Record", Pupil builds this URI:
```
upi://pay?pa={UPI_ID}&pn={PAYEE_NAME}&am={AMOUNT}&cu=INR&tn={REASON}
```
Then creates an `Intent(Intent.ACTION_VIEW, uri)` and calls `context.startActivity(intent)`.

### Why different UPI apps behave differently

| App | Behavior | Why |
|-----|----------|-----|
| **Amazon Pay** | Works ✅ | Launches directly to payment screen for the UPI ID |
| **Google Pay** | Works but may show errors ⚠️ | GPay validates the `pa` (UPI ID) parameter strictly. If the ID format is invalid or doesn't exist, it shows "Something went wrong" |
| **PhonePe** | Blocks / shows error ❌ | PhonePe requires the UPI ID to be registered with them. If you scan a QR for a VPA not on PhonePe network, it will refuse. PhonePe also has the strictest intent filter matching |

### Key issues:
1. **No `SIGNATURE` intent extra:** Pupil doesn't add `SIGNATURE` or `TRANSACTION_ID` extras. Some apps (PhonePe, Paytm) require additional parameters like `mode=04` (web intent flow)
2. **Package name matching:** The app selects from installed packages that can handle `upi://pay` intents. Some apps register for this intent, some don't
3. **No `merchantCode`:** Some UPI apps expect a merchant-specific parameter
4. **No callback URL:** Pupil doesn't receive a callback when payment completes — user must manually mark COMPLETED/FAILED
5. **Pending transaction management:** Transactions stay PENDING indefinitely until user manually marks them (auto-expire only happens at app startup for >10 min stale ones)

### InstalledUpiAppsResolver (`InstalledUpiAppsResolver.kt`)
Uses `PackageManager.queryIntentActivities(Intent(ACTION_VIEW, "upi://pay"), 0)` to find all apps that can handle UPI intents. This is used both in PaymentEntryScreen (to show app selection) and SettingsScreen (for "Browse installed apps").

---

## 8. Known Issues & Bugs

### Critical / Crashing
1. **Crash on fresh install (first launch):** If `pupil_db` doesn't exist yet, `onCreate` runs pre-seed inserts in a coroutine. If user navigates to Home before inserts complete, the `combine(Flow, Flow)` might crash if one flow emits null. Need to verify.
2. **Camera crash on permission denied:** If user denies camera permission after granting once, QRScanScreen tries to bind camera and crashes.
3. **PhonePe redirect fails silently:** User taps Pay & Record → PhonePe opens → shows error → user comes back → transaction stuck in PENDING.

### Functional Bugs
4. **No payment result callback:** After UPI app opens, there's no way for Pupil to know if payment succeeded. User must manually mark as COMPLETED/FAILED.
5. **Double save risk:** If user taps Pay & Record multiple times quickly, multiple PENDING transactions are created.
6. **Effective UPI flow broken:** Variable `effectiveUpiId` (line 96 in PaymentEntryScreen.kt) is **never used** in the Pay & Record handler — it was supposed to resolve manual vs QR vs contact UPI ID but the actual handler uses `finalUpiId` independently.
7. **Gallery QR for individual vs merchant:** `UpiParser.extractUpiId()` extracts the UPI ID (e.g., `person@bank`), but `merchantName` is always passed as empty string from QR scan. The Pay & Record handler derives payee name from UPI local part, but never shows merchant name separately.
8. **Edit merchant name not possible:** EditTransactionScreen has merchant name as read-only. If user wants to rename a payee, they can't.
9. **Settings: "Add custom app" doesn't refresh app list:** New app added to DB but LazyColumn doesn't update because the Flow hasn't emitted again.

### UX Issues
10. **HomeScreen quick stats card shows "₹0" when todayTotalPaise is 0:** Condition `if (uiState.todayTotalPaise > 0)` hides it, but when `pendingCount > 0`, the card shows with only pending count. If both are 0, card hides entirely — inconsistent.
11. **No category management:** Categories are hardcoded: Food, Transport, Groceries, Shopping, Bills, Health, Entertainment, Other. User can't add custom categories.
12. **No export/backup:** All data only in Room DB. No CSV export, no cloud sync.

---

## 9. How to Debug

### 9a. Add log statements

The app already has some `Log` calls in `QRScanScreen.kt`:
```kotlin
import android.util.Log
Log.e("QRScanScreen", "Camera bind failed", error)
```

**To add more logging:**
```kotlin
import android.util.Log
private const val TAG = "PupilPay"

// In any function:
Log.d(TAG, "Opening UPI app: $packageName with URI: $uri")
Log.e(TAG, "Failed to start activity", exception)
```

### 9b. View logs via Logcat

**Option 1: Android Studio Logcat**
1. Open Android Studio
2. Connect device / start emulator
3. Click **Logcat** tab (bottom panel) or press `Alt+6`
4. Filter by package: `com.pupil.app`
5. Filter by tag: `PupilPay` or `QRScanScreen`
6. Set log level: Verbose (V) / Debug (D) / Info (I) / Warning (W) / Error (E)

**Option 2: Command-line**
```bash
adb logcat -s "PupilPay:*" "QRScanScreen:*" "*:E"
adb logcat --pid=$(adb shell pidof -s com.pupil.app)
```

### 9c. Best logging points for UPI debugging

Add logs at these critical points in `PaymentEntryScreen.kt`:

1. **Before building UPI intent** — log resolvedUpiId, resolvedPayeeName, amount, packageName:
   ```kotlin
   Log.d(TAG, "Pay & Record: resolvedUpiId=$resolvedUpiId, package=$paymentAppPackage, amount=$paise")
   ```

2. **After `resolveActivity` check** — log whether intent would resolve:
   ```kotlin
   val resolved = intent.resolveActivity(context.packageManager)
   Log.d(TAG, "Intent resolveActivity: ${resolved != null}")
   if (resolved == null) {
       Log.w(TAG, "No activity found for $paymentAppPackage with UPI intent")
   }
   ```

3. **Catch exceptions on `startActivity`**:
   ```kotlin
   try {
       context.startActivity(intent)
   } catch (e: Exception) {
       Log.e(TAG, "startActivity failed", e)
       localError = "Failed to open $paymentAppName: ${e.message}"
   }
   ```

### 9d. Getting crash stack traces

When the app crashes:
1. Connect to Android Studio and watch Logcat
2. Filter by `FATAL EXCEPTION` or `AndroidRuntime`
3. Or run: `adb logcat -b crash`
4. Or use Google Play Console if published

---

## 10. Next Steps — Suggested Priority

### Phase 1: Stabilize UPI Flow
1. **Fix `effectiveUpiId` not being used** — either remove the unused variable or restructure the Pay button to use it consistently
2. **Add try-catch around `startActivity(intent)`** — prevent crash if app doesn't handle intent properly
3. **Add debounce to Pay button** — prevent double-save (disable button after first click, re-enable after timeout)
4. **Log all UPI intent parameters** — diagnose why GPay/PhonePe fail

### Phase 2: Payment Status Callback
5. **Implement result callback** — Use `ActivityResultContracts.StartActivityForResult` instead of `context.startActivity(intent)` to get called back
6. **Or use `Notification`-based detection** — Listen for UPI app's notification with payment success/failure
7. **Show result dialog** — After returning from UPI app, show "Did payment succeed?" dialog (YES/NO/Retry)

### Phase 3: Feature Improvements
8. **Edit merchant name** — Make merchant name editable in EditTransactionScreen
9. **Custom categories** — Add category management in Settings
10. **Export to CSV** — Share/export transactions
11. **Search/filter transactions** — Add search bar on Home screen
12. **Recurring transactions** — Auto-create repeating transactions

### Phase 4: Polish
13. **Animated transitions** — Add compose animations between screens
14. **Dark mode toggle** — Add manual theme switch in Settings
15. **Material 3 dynamic theming** — Use Monet colors on Android 12+
16. **Widget** — Home screen widget showing today's total

---

## 11. Key Files Reference

| File | Path | Lines | Purpose |
|------|------|-------|---------|
| AppModule | `app/src/main/java/com/pupil/app/core/di/AppModule.kt` | 77 | Hilt DI wiring |
| PupilDatabase | `app/src/main/java/com/pupil/app/core/data/local/PupilDatabase.kt` | 53 | Room DB, version 2, pre-seeded apps |
| TransactionDao | `app/src/main/java/com/pupil/app/core/data/local/dao/TransactionDao.kt` | 59 | All SQL queries |
| TransactionRepositoryImpl | `app/src/main/java/com/pupil/app/core/data/repository/TransactionRepositoryImpl.kt` | 94 | Repository with `toDomain()`/`toEntity()` mappers |
| TransactionUseCases | `app/src/main/java/com/pupil/app/core/domain/usecase/TransactionUseCases.kt` | 77 | 13 use cases bundled |
| PaymentEntryScreen | `app/src/main/java/com/pupil/app/feature/payment/PaymentEntryScreen.kt` | 498 | Main payment flow screen |
| PaymentViewModel | `app/src/main/java/com/pupil/app/feature/payment/PaymentViewModel.kt` | 118 | Pending tx management |
| HomeScreen | `app/src/main/java/com/pupil/app/feature/home/HomeScreen.kt` | 377 | Main screen |
| HomeViewModel | `app/src/main/java/com/pupil/app/feature/home/HomeViewModel.kt` | 93 | State management + auto-expire |
| PupilNavGraph | `app/src/main/java/com/pupil/app/PupilNavGraph.kt` | 165 | All routes |
| UiComponents | `app/src/main/java/com/pupil/app/core/ui/components/UiComponents.kt` | 217 | TransactionCard, PaymentAppCard, badges |
| InstalledUpiAppsResolver | `app/src/main/java/com/pupil/app/core/ui/util/InstalledUpiAppsResolver.kt` | 36 | Queries UPI-capable apps |
| UpiParser | `app/src/main/java/com/pupil/app/core/ui/util/UpiParser.kt` | 23 | Extracts UPI ID from QR raw values |
| Formatters | `app/src/main/java/com/pupil/app/core/ui/util/Formatters.kt` | 25 | `formatPaise()` + `toPaise()` |
| DateUtils | `app/src/main/java/com/pupil/app/core/ui/util/DateUtils.kt` | 71 | Date formatting + range calculations |
| AndroidManifest | `app/src/main/AndroidManifest.xml` | 34 | Permissions + queries |
| build.gradle.kts | `app/build.gradle.kts` | 88 | Dependencies |

---

## 12. Build & Run

```bash
# Clean build
cd /workspaces/pupil && ./gradlew clean assembleDebug

# Fast build (incremental)
cd /workspaces/pupil && ./gradlew assembleDebug

# Run on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Run with logcat
adb logcat -s "PupilPay:*" "QRScanScreen:*" "*:E"
```
