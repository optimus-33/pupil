# Pupil App — Database Schema Review & Future-Proofing Plan

> **Status**: v2 — Revised based on review feedback
> **Goal**: One-time schema redesign that makes the database feature-rich enough that future features never require schema redesign.

---

## 1. What's Wrong With the Current Schema

### Current `transactions` Table (v2)
```
id, merchantName, upiId, amountPaise, reason, category,
paymentType, paymentApp, timestamp, isManual, status
```

### Critical Issues

| # | Issue | Risk | Fix |
|---|-------|------|-----|
| 1 | `fallbackToDestructiveMigration()` | **Data loss** on ANY schema change | Replace with `addMigrations()` |
| 2 | No `createdAt`/`updatedAt`/`deletedAt` | No audit trail, no soft-delete | Add columns |
| 3 | No `transactionType` | Cannot distinguish expense vs income vs refund vs transfer | Add `transactionType` field |
| 4 | No `referenceNumber` | Cannot reconcile UPI payments with bank SMS | Add now (retroactively hard) |
| 5 | No `merchantCode` | Cannot do merchant-level analytics | Add now (already parsing QR) |
| 6 | No `notes` field | Reason conflated with additional notes | Add now |
| 7 | Categories as free-text string | Data quality issues, no user-defined categories | New `categories` table + FK |
| 8 | No `accounts` table | No multi-account/balance tracking | New table |
| 9 | No `tags` system | Cannot filter across categories | Normalized `tags` + `transaction_tags` |
| 10 | No indices | Performance degrades with scale | Add indices including composite |
| 11 | No backup/export | User cannot migrate data | JSON backup/restore with versioning |

---

## 2. Definitive Schema — Version 3 (Migration from v2)

### 2.1 `transactions` — Augmented (additive only, no column removal)

```sql
-- v2 to v3 migration: all ALTER TABLE ADD COLUMN (safe, non-destructive)

ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE';
-- Values: 'EXPENSE' | 'INCOME' | 'REFUND' | 'TRANSFER'

ALTER TABLE transactions ADD COLUMN referenceNumber TEXT;
-- UPI transaction reference (e.g., 4057xxxx from bank SMS) — critical for reconciliation

ALTER TABLE transactions ADD COLUMN merchantCode TEXT;
-- UPI merchant code from QR (mc param) — enables merchant analytics

ALTER TABLE transactions ADD COLUMN notes TEXT;
-- Additional context beyond reason

ALTER TABLE transactions ADD COLUMN categoryId INTEGER REFERENCES categories(id);
-- Normalized category instead of free-text string

ALTER TABLE transactions ADD COLUMN accountId INTEGER REFERENCES accounts(id);
-- Link to account for balance tracking

ALTER TABLE transactions ADD COLUMN recurringId INTEGER REFERENCES recurring_transactions(id);
-- Link to recurring template (future)

ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0;
ALTER TABLE transactions ADD COLUMN deletedAt INTEGER;

-- Populate audit timestamps for existing rows
UPDATE transactions SET createdAt = timestamp, updatedAt = timestamp WHERE createdAt = 0;
```

### 2.2 `categories` — New (normalized, pre-seeded)

```sql
CREATE TABLE categories (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL UNIQUE,
    icon            TEXT,                          -- Icon resource name
    color           INTEGER,                       -- ARGB color for UI
    transactionType TEXT NOT NULL DEFAULT 'EXPENSE', -- Which flow this category belongs to
    isSystem        INTEGER NOT NULL DEFAULT 0,    -- System categories cannot be deleted
    isActive        INTEGER NOT NULL DEFAULT 1,
    sortOrder       INTEGER NOT NULL DEFAULT 0,
    createdAt       INTEGER NOT NULL
);

-- Seed from current hardcoded categories
INSERT INTO categories (name, transactionType, isSystem, sortOrder, createdAt)
VALUES
    ('Food', 'EXPENSE', 1, 1, 1714000000000),
    ('Transport', 'EXPENSE', 1, 2, 1714000000000),
    ('Groceries', 'EXPENSE', 1, 3, 1714000000000),
    ('Shopping', 'EXPENSE', 1, 4, 1714000000000),
    ('Bills', 'EXPENSE', 1, 5, 1714000000000),
    ('Health', 'EXPENSE', 1, 6, 1714000000000),
    ('Entertainment', 'EXPENSE', 1, 7, 1714000000000),
    ('Other', 'EXPENSE', 1, 8, 1714000000000),
    ('Salary', 'INCOME', 1, 1, 1714000000000),
    ('Freelance', 'INCOME', 1, 2, 1714000000000),
    ('Cashback', 'INCOME', 1, 3, 1714000000000);
```

### 2.3 `accounts` — New

```sql
CREATE TABLE accounts (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT    NOT NULL,            -- "HDFC Savings", "Wallet", "Cash"
    type            TEXT    NOT NULL,            -- 'SAVINGS'|'CREDIT_CARD'|'WALLET'|'CASH'
    institutionName TEXT,                        -- "HDFC Bank", "State Bank of India"
    accountSuffix   TEXT,                        -- Last 4 digits masked
    cardLastFour    TEXT,                        -- For credit cards
    currency        TEXT    NOT NULL DEFAULT 'INR',
    openingBalance  INTEGER NOT NULL DEFAULT 0,  -- In paise (initial balance)
    creditLimit     INTEGER,                     -- For credit cards (in paise)
    icon            TEXT,
    color           INTEGER,
    isActive        INTEGER NOT NULL DEFAULT 1,
    sortOrder       INTEGER NOT NULL DEFAULT 0,
    createdAt       INTEGER NOT NULL,
    updatedAt       INTEGER NOT NULL,
    deletedAt       INTEGER
);

-- Balance is COMPUTED, not stored:
-- SELECT openingBalance + SUM(
--     CASE WHEN t.transactionType = 'INCOME' THEN t.amountPaise
--          WHEN t.transactionType = 'EXPENSE' THEN -t.amountPaise
--          ELSE 0 END
-- ) FROM transactions t WHERE t.accountId = ? AND t.deletedAt IS NULL AND t.status = 'COMPLETED'
```

### 2.4 `tags` + `transaction_tags` — New (normalized, not JSON)

```sql
CREATE TABLE tags (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    color       INTEGER,
    createdAt   INTEGER NOT NULL
);

CREATE TABLE transaction_tags (
    transactionId INTEGER NOT NULL,
    tagId         INTEGER NOT NULL,
    PRIMARY KEY (transactionId, tagId),
    FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
);
```

### 2.5 Indices — Performance

```sql
-- Composite report index (covers 90% of report queries)
CREATE INDEX idx_transactions_reports
ON transactions(status, categoryId, timestamp DESC)
WHERE deletedAt IS NULL;

-- Individual query indices
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp DESC);
CREATE INDEX idx_transactions_account ON transactions(accountId);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_createdAt ON transactions(createdAt);
CREATE INDEX idx_transactions_type ON transactions(transactionType);
```

---

## 3. Backup & Restore System

### 3.1 JSON Format — With Version & Encryption Header

```json
{
  "version": 1,
  "exportedAt": 1714000000000,
  "appVersion": "1.0",
  "encrypted": false,
  "encryptionMethod": null,
  "data": {
    "transactions": [ ... ],
    "accounts": [ ... ],
    "paymentAppConfigs": [ ... ],
    "categories": [ ... ],
    "tags": [ ... ],
    "transactionTags": [ ... ]
  }
}
```

- `encrypted` and `encryptionMethod` defined NOW so old unencrypted backups stay forward-compatible
- Phase 1: Add AES-256 optional encryption

### 3.2 Export Flow

```
User taps "Export Backup" in Settings
  → DAO queries all tables → serialize to JSON
  → Write to user-selected location (SAF document picker)
  → Show share sheet → user saves/emails/uploads
```

### 3.3 Import Flow

```
User taps "Import Backup" in Settings
  → SAF document picker → read file → parse JSON
  → Validate version compatibility
  → Wrap all inserts in @Transaction (atomic rollback on failure)
  → On success: refresh all flows → show confirmation
  → On failure: rollback → show error with details
```

### 3.4 Auto-backup (Phase 1)
- WorkManager `PeriodicWorkRequest` (weekly)
- Exports to app-internal storage
- Optional: Google Drive upload
- Settings toggle: "Auto-backup: Weekly / Monthly / Off"

---

## 4. Migration Strategy — v2 to v3

### 4.1 Stop Using Destructive Migration

```kotlin
// REMOVE THIS — will delete user data
.fallbackToDestructiveMigration()

// ADD THIS
.addMigrations(MIGRATION_2_3)
```

### 4.2 Full MIGRATION_2_3

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Add columns to transactions
        db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'")
        db.execSQL("ALTER TABLE transactions ADD COLUMN referenceNumber TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN merchantCode TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN recurringId INTEGER")
        db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")

        // 2. Set audit timestamps for existing rows
        db.execSQL("UPDATE transactions SET createdAt = timestamp, updatedAt = timestamp WHERE createdAt = 0")

        // 3. Create categories table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS categories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                icon TEXT,
                color INTEGER,
                transactionType TEXT NOT NULL DEFAULT 'EXPENSE',
                isSystem INTEGER NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL DEFAULT 1,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL
            )
        """)

        // 4. Seed categories
        val seedCategories = listOf(
            "'Food'", "'Transport'", "'Groceries'", "'Shopping'",
            "'Bills'", "'Health'", "'Entertainment'", "'Other'",
            "'Salary'", "'Freelance'", "'Cashback'"
        )
        seedCategories.forEachIndexed { index, name ->
            val type = if (index < 8) "'EXPENSE'" else "'INCOME'"
            db.execSQL("INSERT INTO categories (name, transactionType, isSystem, sortOrder, createdAt) VALUES ($name, $type, 1, ${index + 1}, 1714000000000)")
        }

        // 5. Migrate existing category strings to categoryId
        db.execSQL("""
            UPDATE transactions SET categoryId = (
                SELECT id FROM categories WHERE LOWER(categories.name) = LOWER(transactions.category) LIMIT 1
            )
        """)
        // Any remaining unmatched default to 'Other' (id = 8)
        db.execSQL("UPDATE transactions SET categoryId = 8 WHERE categoryId IS NULL")

        // 6. Create accounts table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                institutionName TEXT,
                accountSuffix TEXT,
                cardLastFour TEXT,
                currency TEXT NOT NULL DEFAULT 'INR',
                openingBalance INTEGER NOT NULL DEFAULT 0,
                creditLimit INTEGER,
                icon TEXT,
                color INTEGER,
                isActive INTEGER NOT NULL DEFAULT 1,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                deletedAt INTEGER
            )
        """)

        // 7. Create tags + transaction_tags tables
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                color INTEGER,
                createdAt INTEGER NOT NULL
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS transaction_tags (
                transactionId INTEGER NOT NULL,
                tagId INTEGER NOT NULL,
                PRIMARY KEY (transactionId, tagId),
                FOREIGN KEY (transactionId) REFERENCES transactions(id) ON DELETE CASCADE,
                FOREIGN KEY (tagId) REFERENCES tags(id) ON DELETE CASCADE
            )
        """)

        // 8. Create indices
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_reports ON transactions(status, categoryId, timestamp DESC) WHERE deletedAt IS NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp DESC)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_account ON transactions(accountId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_createdAt ON transactions(createdAt)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_type ON transactions(transactionType)")
    }
}
```

---

## 5. Entity & Domain Model Changes

### 5.1 Updated `TransactionEntity`

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val notes: String?,
    val categoryId: Long,           // FK to categories table
    val transactionType: String,    // EXPENSE | INCOME | REFUND | TRANSFER
    val paymentType: String,
    val paymentApp: String,
    val merchantCode: String?,
    val referenceNumber: String?,
    val accountId: Long?,
    val timestamp: Long,
    val isManual: Boolean,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?
)
```

### 5.2 Updated Domain `Transaction`

```kotlin
data class Transaction(
    val id: Long = 0,
    val merchantName: String,
    val upiId: String?,
    val amountPaise: Long,
    val reason: String,
    val notes: String?,
    val category: Category,
    val transactionType: TransactionType,
    val paymentType: PaymentType,
    val paymentApp: String,
    val merchantCode: String?,
    val referenceNumber: String?,
    val account: Account?,
    val timestamp: Long,
    val isManual: Boolean = false,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val tags: List<Tag> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
```

---

## 6. App Size Optimization

| Item | Current Size | Optimization | Target |
|------|-------------|-------------|--------|
| `material-icons-extended` | ~12MB | Remove, use specific SVGs in res/drawable | ~200KB |
| ML Kit barcode-scanning | ~8MB | Keep (needed) | ~8MB |
| CameraX | ~5MB | Keep (needed) | ~5MB |
| Hilt + Room + Compose | ~6MB | Keep | ~6MB |
| Unused resources | ~3MB | isShrinkResources + lint | ~500KB |
| Unused Kotlin stdlib | ~2MB | R8 optimization | ~1MB |
| **Total** | **~36MB** | | **~18-21MB** |

### Build Config

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 7. Implementation Roadmap

### Phase 0 — IMMEDIATE (Protect existing data)

| # | Task | Files |
|---|------|-------|
| 1 | Add MIGRATION_2_3, replace fallbackToDestructiveMigration | [`PupilDatabase.kt`](app/src/main/java/com/pupil/app/core/data/local/PupilDatabase.kt) |
| 2 | Update TransactionEntity with new fields | [`TransactionEntity.kt`](app/src/main/java/com/pupil/app/core/data/local/entity/TransactionEntity.kt) |
| 3 | Create CategoryEntity, AccountEntity, TagEntity, TransactionTagEntity | New entity files |
| 4 | Create CategoryDao, AccountDao, TagDao, update TransactionDao | New DAO files |
| 5 | Update domain models | New files in core/domain/model/ |
| 6 | Update repository mapping (entity to domain) | [`TransactionRepositoryImpl.kt`](app/src/main/java/com/pupil/app/core/data/repository/TransactionRepositoryImpl.kt) |
| 7 | Update TransactionUseCases for new fields | [`TransactionUseCases.kt`](app/src/main/java/com/pupil/app/core/domain/usecase/TransactionUseCases.kt) |
| 8 | Update UI screens for categoryId | PaymentEntryScreen, EditTransactionScreen, HomeScreen |
| 9 | **Implement JSON backup/restore** | New BackupManager class |
| 10 | **Add backup/restore UI in Settings** | [`SettingsScreen.kt`](app/src/main/java/com/pupil/app/feature/settings/SettingsScreen.kt) |
| 11 | Verify build + test migration | Build + manual test |

### Phase 1 — Short-term

| # | Task |
|---|------|
| 12 | Reduce APK size (icon subset, ProGuard, shrinkResources) |
| 13 | Add optional AES-256 encryption to backup |
| 14 | Add WorkManager auto-backup scheduling |
| 15 | Accounts table UI: account picker in payment flow |
| 16 | Handle transactionType in payment entry (expense vs income toggle) |
| 17 | Tags UI: add tag management in transaction entry |

### Phase 2 — Medium-term

| # | Task |
|---|------|
| 18 | Paging 3 integration for transaction list |
| 19 | Recurring transactions table + UI |
| 20 | Budgets table + budget tracking UI |
| 21 | Location capture on transaction entry |

---

## 8. Key Principles (Non-Negotiable)

1. **Store smallest unit**: Monetary values as Long in paise. Display formatting is a UI concern only.
2. **Never delete, always soft-delete**: deletedAt field. All queries filter WHERE deletedAt IS NULL.
3. **Audit trail**: Every row has createdAt and updatedAt.
4. **Additive migrations only**: Never remove columns — mark deprecated and ignore in code. Keeps backward compatibility with old backup files.
5. **Index every query path**: Before writing any query, ensure WHERE/ORDER BY columns are indexed.
6. **Normalize tags**: Junction table (transaction_tags), not JSON string. JSON arrays cannot be queried or indexed.
7. **Versioned backups**: JSON export includes schema version and encryption header for forward compatibility.
8. **Single source of truth**: Room database is the SSOT. ViewModels derive state from Flow emissions only.
