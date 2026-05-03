package com.pupil.app.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pupil.app.core.data.local.dao.AccountDao
import com.pupil.app.core.data.local.dao.CategoryDao
import com.pupil.app.core.data.local.dao.PaymentAppConfigDao
import com.pupil.app.core.data.local.dao.TagDao
import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.local.entity.AccountEntity
import com.pupil.app.core.data.local.entity.CategoryEntity
import com.pupil.app.core.data.local.entity.PaymentAppConfigEntity
import com.pupil.app.core.data.local.entity.TagEntity
import com.pupil.app.core.data.local.entity.TransactionEntity
import com.pupil.app.core.data.local.entity.TransactionTagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        PaymentAppConfigEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        TagEntity::class,
        TransactionTagEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PupilDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentAppConfigDao(): PaymentAppConfigDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add new columns to transactions table
                db.execSQL("ALTER TABLE transactions ADD COLUMN notes TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'EXPENSE'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN merchantCode TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN referenceNumber TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER DEFAULT NULL")

                // 2. Create categories table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT,
                        `color` INTEGER,
                        `transactionType` TEXT NOT NULL DEFAULT 'EXPENSE',
                        `isSystem` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL
                    )
                """)

                // 3. Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `institutionName` TEXT,
                        `accountSuffix` TEXT,
                        `cardLastFour` TEXT,
                        `currency` TEXT NOT NULL DEFAULT 'INR',
                        `openingBalance` INTEGER NOT NULL DEFAULT 0,
                        `creditLimit` INTEGER,
                        `icon` TEXT,
                        `color` INTEGER,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `sortOrder` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `deletedAt` INTEGER DEFAULT NULL
                    )
                """)

                // 4. Create tags table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                """)

                // 5. Create transaction_tags junction table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `transaction_tags` (
                        `transactionId` INTEGER NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`, `tagId`),
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE
                    )
                """)

                // 6. Seed default categories
                val now = System.currentTimeMillis()
                // Expense categories
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Food', 'restaurant', null, 'EXPENSE', 1, 1, 1, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Transport', 'directions_car', null, 'EXPENSE', 1, 1, 2, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Groceries', 'shopping_cart', null, 'EXPENSE', 1, 1, 3, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Shopping', 'shopping_bag', null, 'EXPENSE', 1, 1, 4, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Bills & Utilities', 'receipt', null, 'EXPENSE', 1, 1, 5, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Health', 'local_hospital', null, 'EXPENSE', 1, 1, 6, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Entertainment', 'movie', null, 'EXPENSE', 1, 1, 7, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Other', 'category', null, 'EXPENSE', 1, 1, 100, $now)")
                // Income categories
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Salary', 'account_balance', null, 'INCOME', 1, 1, 1, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Freelance', 'work', null, 'INCOME', 1, 1, 2, $now)")
                db.execSQL("INSERT INTO categories (name, icon, color, transactionType, isSystem, isActive, sortOrder, createdAt) VALUES ('Cashback', 'currency_rupee', null, 'INCOME', 1, 1, 3, $now)")

                // 7. Migrate old category strings to new categoryId
                // Map old category strings to the seeded category IDs
                // The old 'category' column held string values like "Food", "Transport", etc.
                // We default to "Other" (id=8 for EXPENSE) if no match found
                db.execSQL("""
                    UPDATE transactions SET categoryId = (
                        CASE
                            WHEN category = 'Food' THEN 1
                            WHEN category = 'Transport' THEN 2
                            WHEN category = 'Groceries' THEN 3
                            WHEN category = 'Shopping' THEN 4
                            WHEN category = 'Bills & Utilities' THEN 5
                            WHEN category = 'Health' THEN 6
                            WHEN category = 'Entertainment' THEN 7
                            ELSE 8
                        END
                    )
                """)

                // 8. Set createdAt and updatedAt to timestamp for existing data
                db.execSQL("UPDATE transactions SET createdAt = timestamp, updatedAt = timestamp")

                // 9. Create indices
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions(timestamp DESC)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_status ON transactions(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions(categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transactionType ON transactions(transactionType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_createdAt ON transactions(createdAt)")

                // 10. Room tolerates extra columns in the table (the old `category` column remains).
                //     No need to drop it — Room only validates entity-defined columns exist.
            }
        }

        fun buildDatabase(context: Context): PupilDatabase {
            lateinit var database: PupilDatabase
            val callback = object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        database.paymentAppConfigDao().insertAll(
                            listOf(
                                PaymentAppConfigEntity(displayName = "Google Pay", packageName = "com.google.android.apps.nbu.paisa.user", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "PhonePe", packageName = "com.phonepe.app", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "Paytm", packageName = "net.one97.paytm", paymentType = "UPI"),
                                PaymentAppConfigEntity(displayName = "HDFC Bank", packageName = "com.snapwork.hdfc", paymentType = "UPI_CREDIT_CARD"),
                                PaymentAppConfigEntity(displayName = "Axis Mobile", packageName = "com.axis.mobile", paymentType = "UPI_CREDIT_CARD"),
                                PaymentAppConfigEntity(displayName = "ICICI iMobile", packageName = "com.csam.icici.bank.imobile", paymentType = "UPI_CREDIT_CARD")
                            )
                        )
                    }
                }
            }
            database = Room.databaseBuilder(context, PupilDatabase::class.java, "pupil_db")
                .addCallback(callback)
                .addMigrations(MIGRATION_2_3)
                .build()
            return database
        }
    }
}

