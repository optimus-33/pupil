package com.pupil.app.core.domain.repository

import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction)
    fun getTotalInRange(from: Long, to: Long): Flow<Long>
    fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>>
}
