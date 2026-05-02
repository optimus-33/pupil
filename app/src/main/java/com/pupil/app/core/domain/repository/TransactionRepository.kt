package com.pupil.app.core.domain.repository

import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun updateTransactionStatus(id: Long, status: String)
    fun getTotalInRange(from: Long, to: Long): Flow<Long>
    fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>>
    fun getPendingTransactions(): Flow<List<Transaction>>
}

