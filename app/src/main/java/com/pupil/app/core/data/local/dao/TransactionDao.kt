package com.pupil.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pupil.app.core.data.local.entity.CategoryTotalEntity
import com.pupil.app.core.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = :status AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getTransactionsByStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transactionType = :transactionType AND deletedAt IS NULL ORDER BY timestamp DESC")
    fun getTransactionsByType(transactionType: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: Long, deletedAt: Long)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("UPDATE transactions SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, updatedAt: Long)

    @Query("""
        UPDATE transactions SET
        merchantName = :merchantName, upiId = :upiId, amountPaise = :amountPaise,
        reason = :reason, notes = :notes, categoryId = :categoryId,
        transactionType = :transactionType, paymentType = :paymentType,
        paymentApp = :paymentApp, merchantCode = :merchantCode,
        referenceNumber = :referenceNumber, accountId = :accountId,
        timestamp = :timestamp, isManual = :isManual, status = :status,
        updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun update(
        id: Long,
        merchantName: String,
        upiId: String?,
        amountPaise: Long,
        reason: String,
        notes: String?,
        categoryId: Long,
        transactionType: String,
        paymentType: String,
        paymentApp: String,
        merchantCode: String?,
        referenceNumber: String?,
        accountId: Long?,
        timestamp: Long,
        isManual: Boolean,
        status: String,
        updatedAt: Long
    )

    @Query("SELECT IFNULL(SUM(amountPaise), 0) FROM transactions WHERE timestamp BETWEEN :from AND :to AND status = 'COMPLETED' AND deletedAt IS NULL")
    fun getTotalInRange(from: Long, to: Long): Flow<Long>

    @Query("""
        SELECT IFNULL(SUM(amountPaise), 0) FROM transactions
        WHERE timestamp BETWEEN :from AND :to AND status = 'COMPLETED'
        AND transactionType IN (:types) AND deletedAt IS NULL
    """)
    fun getTotalInRangeByType(from: Long, to: Long, types: List<String>): Flow<Long>

    @Query("""
        SELECT c.id AS categoryId, c.name AS category, c.icon AS icon, c.color AS color,
               IFNULL(SUM(t.amountPaise), 0) AS totalPaise
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.timestamp BETWEEN :from AND :to AND t.status = 'COMPLETED' AND t.deletedAt IS NULL
        GROUP BY t.categoryId ORDER BY totalPaise DESC
    """)
    fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotalEntity>>

    @Query("""
        SELECT c.id AS categoryId, c.name AS category, c.icon AS icon, c.color AS color,
               IFNULL(SUM(t.amountPaise), 0) AS totalPaise
        FROM transactions t
        INNER JOIN categories c ON t.categoryId = c.id
        WHERE t.timestamp BETWEEN :from AND :to AND t.status = 'COMPLETED'
        AND t.transactionType = :transactionType AND t.deletedAt IS NULL
        GROUP BY t.categoryId ORDER BY totalPaise DESC
    """)
    fun getCategoryTotalsByTypeInRange(from: Long, to: Long, transactionType: String): Flow<List<CategoryTotalEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL")
    fun getTransactionCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun getTransactionsInRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsForBackup(): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun hardDeleteAll()
}

