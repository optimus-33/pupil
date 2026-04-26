package com.pupil.app.core.data.repository

import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.local.entity.TransactionEntity
import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> =
        dao.getAllTransactions().map { entities ->
            entities.map { entity ->
                Transaction(
                    id = entity.id,
                    merchantName = entity.merchantName,
                    upiId = entity.upiId,
                    amountPaise = entity.amountPaise,
                    reason = entity.reason,
                    category = entity.category,
                    paymentType = PaymentType.fromTypeName(entity.paymentType),
                    paymentApp = entity.paymentApp,
                    timestamp = entity.timestamp,
                    isManual = entity.isManual
                )
            }
        }

    override suspend fun insertTransaction(transaction: Transaction) {
        dao.insert(
            TransactionEntity(
                id = transaction.id,
                merchantName = transaction.merchantName,
                upiId = transaction.upiId,
                amountPaise = transaction.amountPaise,
                reason = transaction.reason,
                category = transaction.category,
                paymentType = transaction.paymentType.typeName,
                paymentApp = transaction.paymentApp,
                timestamp = transaction.timestamp,
                isManual = transaction.isManual
            )
        )
    }

    override fun getTotalInRange(from: Long, to: Long): Flow<Long> = dao.getTotalInRange(from, to)

    override fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>> =
        dao.getCategoryTotalsInRange(from, to).map { entities ->
            entities.map { CategoryTotal(category = it.category, totalPaise = it.totalPaise) }
        }
}
