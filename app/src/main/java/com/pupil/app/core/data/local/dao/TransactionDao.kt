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
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun getTransactionsByStatus(status: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)
    @Query("SELECT IFNULL(SUM(amountPaise), 0) FROM transactions WHERE timestamp BETWEEN :from AND :to AND status = 'COMPLETED'")
    fun getTotalInRange(from: Long, to: Long): Flow<Long>

    @Query("SELECT category, SUM(amountPaise) AS totalPaise FROM transactions WHERE timestamp BETWEEN :from AND :to AND status = 'COMPLETED' GROUP BY category ORDER BY totalPaise DESC")
    fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotalEntity>>
}

