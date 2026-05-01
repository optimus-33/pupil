package com.pupil.app.core.data.repository

import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.local.entity.TransactionEntity
import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun updateTransactionStatus(id: Long, status: String) {
        dao.updateStatus(id, status)
    }

    override fun getTotalInRange(from: Long, to: Long): Flow<Long> = dao.getTotalInRange(from, to)

    override fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>> =
        dao.getCategoryTotalsInRange(from, to).map { entities ->
            entities.map { CategoryTotal(category = it.category, totalPaise = it.totalPaise) }
        }

    override fun getPendingTransactions(): Flow<List<Transaction>> =
        dao.getTransactionsByStatus(TransactionStatus.PENDING.statusName).map { entities ->
            entities.map { it.toDomain() }
        }
}

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    merchantName = merchantName,
    upiId = upiId,
    amountPaise = amountPaise,
    reason = reason,
    category = category,
    paymentType = PaymentType.fromTypeName(paymentType),
    paymentApp = paymentApp,
    timestamp = timestamp,
    isManual = isManual,
    status = TransactionStatus.fromStatusName(status)
)

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    merchantName = merchantName,
    upiId = upiId,
    amountPaise = amountPaise,
    reason = reason,
    category = category,
    paymentType = paymentType.typeName,
    paymentApp = paymentApp,
    timestamp = timestamp,
    isManual = isManual,
    status = status.statusName
)

