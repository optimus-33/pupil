PPLR (Pupil) — Personal UPI Ledger
PPLR is a privacy-first, metadata-rich wrapper for UPI transactions. It bridges the gap between instant payments and granular financial tracking by allowing users to attach private notes and custom categories to every transaction before handing off the payment to a PSP app.

🎯 The Problem
Standard UPI applications (GPay, PhonePe, BHIM) excel at moving money but fail at contextual logging. Users often forget why a specific amount was debited, and bank statements only provide cryptic merchant strings. PPLR solves this by acting as a Pre-Processor (Metadata) and Post-Processor (Reconciliation).

🛠 How It Works
Metadata Capture: The user scans a QR or enters a VPA in PPLR. They add a Personal Note and Category (e.g., "Seeds - Yavatmal Farm" or "Hostel Electricity").

Intent Hand-off: PPLR triggers a upi://pay Intent, handing off the actual secure transaction to a certified app like BHIM or GPay.

Async Reconciliation: PPLR runs a background BroadcastReceiver to intercept the bank's confirmation SMS.

The Match: The app matches the SMS (Amount + Timestamp) with the "Pending" local entry and updates the ledger.

🚀 Tech Stack
Language: Kotlin (100%)

UI: Jetpack Compose (Material 3)

Architecture: MVVM + Clean Architecture

Database: Room Persistence (Encrypted)

Processing: WorkManager (Background SMS Sync)

Vision: Google ML Kit (QR Code Scanning)

Dev Env: GitHub Codespaces + Tailscale for remote ADB

📂 Project Structure
Plaintext
app/
├── data/              # Room Entities, DAOs, and SMS Repository
├── domain/            # UseCases (e.g., MatchSmsToTransaction)
├── presentation/      # UI Components (Compose Screens, ViewModels)
├── receiver/          # SmsBroadcastReceiver logic
└── di/                # Hilt Dependency Injection modules
🔐 Privacy & Security
Zero Backend: All transaction data and personal notes stay on the device in an encrypted SQLite database.

No PII Sharing: PPLR does not request Internet permission for transaction data; it is a strictly local ledger.

Sovereignty: Export your data anytime as a CSV for local analysis in Excel or Python.

🏗 Setup for Developers (Codespaces)
Launch the GitHub Codespace.

Install Tailscale in the terminal to bridge your physical Android device.

Enable Wireless Debugging on your phone.

Run ./gradlew assembleDebug to build the APK.

adb install to your device.

Contribution
This is a personal project built for utility and engineering exploration.
