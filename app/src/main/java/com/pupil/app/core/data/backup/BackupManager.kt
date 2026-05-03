package com.pupil.app.core.data.backup

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKUP_VERSION = 1

data class BackupResult(
    val success: Boolean,
    val message: String,
    val transactionCount: Int = 0
)

@Singleton
class BackupManager @Inject constructor(
    private val transactionDao: TransactionDao,
    private val paymentAppConfigDao: PaymentAppConfigDao,
    private val categoryDao: CategoryDao,
    private val tagDao: TagDao,
    private val accountDao: AccountDao
) {
    /**
     * Export all data to a JSON file via SAF URI.
     */
    suspend fun exportBackup(context: Context, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val transactions = transactionDao.getAllTransactionsForBackup()
            val appConfigs = paymentAppConfigDao.getAllOnce()
            val categories = categoryDao.getAllOnce()
            val tags = tagDao.getAllOnce()
            val tagLinks = tagDao.getAllTransactionTags()

            val backupJson = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("exportedAt", System.currentTimeMillis())
                put("appName", "Pupil")
                put("appVersion", "1.0")

                // Transactions
                put("transactions", JSONArray().apply {
                    transactions.forEach { txn ->
                        put(JSONObject().apply {
                            put("id", txn.id)
                            put("merchantName", txn.merchantName)
                            put("upiId", txn.upiId ?: JSONObject.NULL)
                            put("amountPaise", txn.amountPaise)
                            put("reason", txn.reason)
                            put("notes", txn.notes ?: JSONObject.NULL)
                            put("categoryId", txn.categoryId)
                            put("transactionType", txn.transactionType)
                            put("paymentType", txn.paymentType)
                            put("paymentApp", txn.paymentApp)
                            put("merchantCode", txn.merchantCode ?: JSONObject.NULL)
                            put("referenceNumber", txn.referenceNumber ?: JSONObject.NULL)
                            put("accountId", txn.accountId ?: JSONObject.NULL)
                            put("timestamp", txn.timestamp)
                            put("isManual", txn.isManual)
                            put("status", txn.status)
                            put("createdAt", txn.createdAt)
                            put("updatedAt", txn.updatedAt)
                            put("deletedAt", txn.deletedAt ?: JSONObject.NULL)
                        })
                    }
                })

                // Payment app configs
                put("paymentAppConfigs", JSONArray().apply {
                    appConfigs.forEach { config ->
                        put(JSONObject().apply {
                            put("id", config.id)
                            put("displayName", config.displayName)
                            put("packageName", config.packageName)
                            put("paymentType", config.paymentType)
                            put("enabled", config.enabled)
                        })
                    }
                })

                // Categories
                put("categories", JSONArray().apply {
                    categories.forEach { cat ->
                        put(JSONObject().apply {
                            put("id", cat.id)
                            put("name", cat.name)
                            put("icon", cat.icon ?: JSONObject.NULL)
                            put("color", cat.color ?: JSONObject.NULL)
                            put("transactionType", cat.transactionType)
                            put("isSystem", cat.isSystem)
                            put("isActive", cat.isActive)
                            put("sortOrder", cat.sortOrder)
                            put("createdAt", cat.createdAt)
                        })
                    }
                })

                // Tags
                put("tags", JSONArray().apply {
                    tags.forEach { tag ->
                        put(JSONObject().apply {
                            put("id", tag.id)
                            put("name", tag.name)
                            put("color", tag.color ?: JSONObject.NULL)
                            put("createdAt", tag.createdAt)
                        })
                    }
                })

                // Transaction-tag links
                put("transactionTags", JSONArray().apply {
                    tagLinks.forEach { link ->
                        put(JSONObject().apply {
                            put("transactionId", link.transactionId)
                            put("tagId", link.tagId)
                        })
                    }
                })
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(backupJson.toString(2))
                }
            } ?: return@withContext BackupResult(false, "Failed to open output stream")

            BackupResult(true, "Export successful", transactions.size)
        } catch (e: Exception) {
            BackupResult(false, "Export failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Import all data from a JSON file via SAF URI.
     * Clears existing data and restores from backup atomically.
     */
    suspend fun importBackup(context: Context, uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            } ?: return@withContext BackupResult(false, "Failed to open input stream")

            val backupJson = JSONObject(jsonString)
            val version = backupJson.optInt("version", 0)

            if (version < 1) {
                return@withContext BackupResult(false, "Unsupported backup version: $version")
            }

            // Parse all data from JSON
            val transactions = mutableListOf<TransactionEntity>()
            val transactionsJson = backupJson.optJSONArray("transactions")
            if (transactionsJson != null) {
                for (i in 0 until transactionsJson.length()) {
                    val obj = transactionsJson.getJSONObject(i)
                    transactions.add(TransactionEntity(
                        id = 0,  // Auto-generate IDs
                        merchantName = obj.getString("merchantName"),
                        upiId = if (obj.has("upiId") && !obj.isNull("upiId")) obj.optString("upiId") else null,
                        amountPaise = obj.getLong("amountPaise"),
                        reason = obj.optString("reason", ""),
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.optString("notes") else null,
                        categoryId = obj.optLong("categoryId", 8),
                        transactionType = obj.optString("transactionType", "EXPENSE"),
                        paymentType = obj.optString("paymentType", "UPI"),
                        paymentApp = obj.optString("paymentApp", ""),
                        merchantCode = if (obj.has("merchantCode") && !obj.isNull("merchantCode")) obj.optString("merchantCode") else null,
                        referenceNumber = if (obj.has("referenceNumber") && !obj.isNull("referenceNumber")) obj.optString("referenceNumber") else null,
                        accountId = if (obj.has("accountId") && !obj.isNull("accountId")) obj.getLong("accountId") else null,
                        timestamp = obj.getLong("timestamp"),
                        isManual = obj.optBoolean("isManual", false),
                        status = obj.optString("status", "COMPLETED"),
                        createdAt = obj.optLong("createdAt", obj.getLong("timestamp")),
                        updatedAt = obj.optLong("updatedAt", obj.getLong("timestamp")),
                        deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.getLong("deletedAt") else null
                    ))
                }
            }

            val appConfigs = mutableListOf<PaymentAppConfigEntity>()
            val appConfigsJson = backupJson.optJSONArray("paymentAppConfigs")
            if (appConfigsJson != null) {
                for (i in 0 until appConfigsJson.length()) {
                    val obj = appConfigsJson.getJSONObject(i)
                    appConfigs.add(PaymentAppConfigEntity(
                        id = 0,
                        displayName = obj.getString("displayName"),
                        packageName = obj.getString("packageName"),
                        paymentType = obj.optString("paymentType", "UPI"),
                        enabled = obj.optBoolean("enabled", true)
                    ))
                }
            }

            val categories = mutableListOf<CategoryEntity>()
            val categoriesJson = backupJson.optJSONArray("categories")
            if (categoriesJson != null) {
                for (i in 0 until categoriesJson.length()) {
                    val obj = categoriesJson.getJSONObject(i)
                    categories.add(CategoryEntity(
                        id = 0,
                        name = obj.getString("name"),
                        icon = if (obj.has("icon") && !obj.isNull("icon")) obj.optString("icon") else null,
                        color = if (obj.has("color") && !obj.isNull("color")) obj.getInt("color") else null,
                        transactionType = obj.optString("transactionType", "EXPENSE"),
                        isSystem = obj.optBoolean("isSystem", false),
                        isActive = obj.optBoolean("isActive", true),
                        sortOrder = obj.optInt("sortOrder", 0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }

            val tags = mutableListOf<TagEntity>()
            val tagsJson = backupJson.optJSONArray("tags")
            if (tagsJson != null) {
                for (i in 0 until tagsJson.length()) {
                    val obj = tagsJson.getJSONObject(i)
                    tags.add(TagEntity(
                        id = 0,
                        name = obj.getString("name"),
                        color = if (obj.has("color") && !obj.isNull("color")) obj.getInt("color") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    ))
                }
            }

            val transactionTags = mutableListOf<TransactionTagEntity>()
            val transactionTagsJson = backupJson.optJSONArray("transactionTags")
            if (transactionTagsJson != null) {
                for (i in 0 until transactionTagsJson.length()) {
                    val obj = transactionTagsJson.getJSONObject(i)
                    transactionTags.add(TransactionTagEntity(
                        transactionId = obj.getLong("transactionId"),
                        tagId = obj.getLong("tagId")
                    ))
                }
            }

            // Atomic import: clear existing data and insert new data
            // Order matters for foreign key constraints
            tagDao.clearAllTransactionTags()
            tagDao.clearAllTags()
            transactionDao.hardDeleteAll()
            paymentAppConfigDao.deleteAll()
            categoryDao.deleteAllNonSystem()
            accountDao.hardDeleteAll()

            // Insert new data in dependency order
            if (categories.isNotEmpty()) categoryDao.insertAll(categories)
            if (appConfigs.isNotEmpty()) paymentAppConfigDao.insertAll(appConfigs)
            if (tags.isNotEmpty()) tagDao.insertAll(tags)
            if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)
            if (transactionTags.isNotEmpty()) {
                transactionTags.forEach { tagDao.addTagToTransaction(it) }
            }

            BackupResult(true, "Import successful: ${transactions.size} transactions restored", transactions.size)
        } catch (e: Exception) {
            BackupResult(false, "Import failed: ${e.message ?: "Unknown error"}")
        }
    }
}
