package com.pupil.app.core.domain.repository

import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun insertTransactions(transactions: List<Transaction>)
    suspend fun softDeleteTransaction(id: Long, deletedAt: Long)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun updateTransactionStatus(id: Long, status: String, updatedAt: Long = System.currentTimeMillis())
    fun getTotalInRange(from: Long, to: Long): Flow<Long>
    fun getTotalInRangeByType(from: Long, to: Long, types: List<String>): Flow<Long>
    fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>>
    fun getCategoryTotalsByTypeInRange(from: Long, to: Long, transactionType: String): Flow<List<CategoryTotal>>
    fun getPendingTransactions(): Flow<List<Transaction>>
    fun getTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>>
    fun getTransactionCount(): Flow<Int>
}
