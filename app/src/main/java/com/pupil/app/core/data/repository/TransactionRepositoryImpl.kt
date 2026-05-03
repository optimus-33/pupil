package com.pupil.app.core.data.repository

import com.pupil.app.core.data.local.dao.CategoryDao
import com.pupil.app.core.data.local.dao.TransactionDao
import com.pupil.app.core.data.local.entity.CategoryEntity
import com.pupil.app.core.data.local.entity.TransactionEntity
import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.domain.model.TransactionType
import com.pupil.app.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao,
    private val categoryDao: CategoryDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        combine(
            dao.getAllTransactions(),
            categoryDao.getAllActive()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override suspend fun getTransactionById(id: Long): Transaction? {
        val entity = dao.getTransactionById(id) ?: return null
        val category = categoryDao.getById(entity.categoryId)
        return entity.toDomain(category)
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun insertTransactions(transactions: List<Transaction>) {
        dao.insertAll(transactions.map { it.toEntity() })
    }

    override suspend fun softDeleteTransaction(id: Long, deletedAt: Long) {
        dao.softDelete(id, deletedAt)
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val entity = transaction.toEntity()
        dao.update(
            id = entity.id,
            merchantName = entity.merchantName,
            upiId = entity.upiId,
            amountPaise = entity.amountPaise,
            reason = entity.reason,
            notes = entity.notes,
            categoryId = entity.categoryId,
            transactionType = entity.transactionType,
            paymentType = entity.paymentType,
            paymentApp = entity.paymentApp,
            merchantCode = entity.merchantCode,
            referenceNumber = entity.referenceNumber,
            accountId = entity.accountId,
            timestamp = entity.timestamp,
            isManual = entity.isManual,
            status = entity.status,
            updatedAt = entity.updatedAt
        )
    }

    override suspend fun updateTransactionStatus(id: Long, status: String, updatedAt: Long) {
        dao.updateStatus(id, status, updatedAt)
    }

    override fun getTotalInRange(from: Long, to: Long): Flow<Long> =
        dao.getTotalInRange(from, to)

    override fun getTotalInRangeByType(from: Long, to: Long, types: List<String>): Flow<Long> =
        dao.getTotalInRangeByType(from, to, types)

    override fun getCategoryTotalsInRange(from: Long, to: Long): Flow<List<CategoryTotal>> =
        dao.getCategoryTotalsInRange(from, to).map { entities ->
            entities.map { CategoryTotal(
                categoryId = it.categoryId,
                category = it.category,
                icon = it.icon,
                color = it.color,
                totalPaise = it.totalPaise
            )}
        }

    override fun getCategoryTotalsByTypeInRange(from: Long, to: Long, transactionType: String): Flow<List<CategoryTotal>> =
        dao.getCategoryTotalsByTypeInRange(from, to, transactionType).map { entities ->
            entities.map { CategoryTotal(
                categoryId = it.categoryId,
                category = it.category,
                icon = it.icon,
                color = it.color,
                totalPaise = it.totalPaise
            )}
        }

    override fun getPendingTransactions(): Flow<List<Transaction>> =
        combine(
            dao.getTransactionsByStatus(TransactionStatus.PENDING.statusName),
            categoryDao.getAllActive()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override fun getTransactionsInRange(from: Long, to: Long): Flow<List<Transaction>> =
        combine(
            dao.getTransactionsInRange(from, to),
            categoryDao.getAllActive()
        ) { transactions, categories ->
            val categoryMap = categories.associateBy { it.id }
            transactions.map { entity ->
                entity.toDomain(categoryMap[entity.categoryId])
            }
        }

    override fun getTransactionCount(): Flow<Int> =
        dao.getTransactionCount()
}

private fun TransactionEntity.toDomain(category: CategoryEntity?): Transaction = Transaction(
    id = id,
    merchantName = merchantName,
    upiId = upiId,
    amountPaise = amountPaise,
    reason = reason,
    notes = notes,
    categoryId = categoryId,
    categoryName = category?.name ?: "Other",
    categoryIcon = category?.icon,
    categoryColor = category?.color,
    transactionType = TransactionType.fromTypeName(transactionType),
    paymentType = PaymentType.fromTypeName(paymentType),
    paymentApp = paymentApp,
    merchantCode = merchantCode,
    referenceNumber = referenceNumber,
    accountId = accountId,
    timestamp = timestamp,
    isManual = isManual,
    status = TransactionStatus.fromStatusName(status),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    merchantName = merchantName,
    upiId = upiId,
    amountPaise = amountPaise,
    reason = reason,
    notes = notes,
    categoryId = categoryId,
    transactionType = transactionType.typeName,
    paymentType = paymentType.typeName,
    paymentApp = paymentApp,
    merchantCode = merchantCode,
    referenceNumber = referenceNumber,
    accountId = accountId,
    timestamp = timestamp,
    isManual = isManual,
    status = status.statusName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
