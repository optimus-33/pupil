package com.pupil.app.core.domain.usecase

import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.repository.PaymentAppConfigRepository
import com.pupil.app.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow

data class TransactionUseCases(
    val getAllTransactions: GetAllTransactionsUseCase,
    val getTodayTotal: GetTotalInRangeUseCase,
    val getWeeklyTotal: GetTotalInRangeUseCase,
    val getMonthlyTotal: GetTotalInRangeUseCase,
    val getCategoryTotalsInRange: GetCategoryTotalsInRangeUseCase,
    val saveTransaction: SaveTransactionUseCase,
    val deleteTransaction: SoftDeleteTransactionUseCase,
    val updateTransactionStatus: UpdateTransactionStatusUseCase,
    val updateTransaction: UpdateTransactionUseCase,
    val getTransactionById: GetTransactionByIdUseCase,
    val getPendingTransactions: GetPendingTransactionsUseCase,
    val getPaymentAppsByType: GetPaymentAppsByTypeUseCase,
    val getAllPaymentApps: GetAllPaymentAppsUseCase,
    val setPaymentAppEnabled: SetPaymentAppEnabledUseCase,
    val addPaymentAppConfig: AddPaymentAppConfigUseCase,
    val getTransactionsInRange: GetTransactionsInRangeUseCase,
    val getTransactionCount: GetTransactionCountUseCase,
    val insertTransactions: InsertTransactionsUseCase,
    val getTotalInRangeByType: GetTotalInRangeByTypeUseCase
)

class GetAllTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAllTransactions()
}

class GetTotalInRangeUseCase(private val repository: TransactionRepository) {
    operator fun invoke(from: Long, to: Long): Flow<Long> = repository.getTotalInRange(from, to)
}

class GetCategoryTotalsInRangeUseCase(private val repository: TransactionRepository) {
    operator fun invoke(from: Long, to: Long): Flow<List<CategoryTotal>> = repository.getCategoryTotalsInRange(from, to)
}

class SaveTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): Long = repository.insertTransaction(transaction)
}

class SoftDeleteTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long) = repository.softDeleteTransaction(id, System.currentTimeMillis())
}

class UpdateTransactionStatusUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long, status: String) = repository.updateTransactionStatus(id, status)
}

class UpdateTransactionUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction) = repository.updateTransaction(transaction)
}

class GetTransactionByIdUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(id: Long): Transaction? = repository.getTransactionById(id)
}

class GetPendingTransactionsUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getPendingTransactions()
}

class GetPaymentAppsByTypeUseCase(private val repository: PaymentAppConfigRepository) {
    operator fun invoke(paymentType: PaymentType): Flow<List<PaymentAppConfig>> = repository.getPaymentAppsByType(paymentType)
}

class GetAllPaymentAppsUseCase(private val repository: PaymentAppConfigRepository) {
    operator fun invoke(): Flow<List<PaymentAppConfig>> = repository.getPaymentApps()
}

class SetPaymentAppEnabledUseCase(private val repository: PaymentAppConfigRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean) = repository.setAppEnabled(id, enabled)
}

class AddPaymentAppConfigUseCase(private val repository: PaymentAppConfigRepository) {
    suspend operator fun invoke(config: PaymentAppConfig) = repository.addPaymentAppConfig(config)
}

class GetTransactionsInRangeUseCase(private val repository: TransactionRepository) {
    operator fun invoke(from: Long, to: Long): Flow<List<Transaction>> = repository.getTransactionsInRange(from, to)
}

class GetTransactionCountUseCase(private val repository: TransactionRepository) {
    operator fun invoke(): Flow<Int> = repository.getTransactionCount()
}

class InsertTransactionsUseCase(private val repository: TransactionRepository) {
    suspend operator fun invoke(transactions: List<Transaction>) = repository.insertTransactions(transactions)
}

class GetTotalInRangeByTypeUseCase(private val repository: TransactionRepository) {
    operator fun invoke(from: Long, to: Long, types: List<String>): Flow<Long> = repository.getTotalInRangeByType(from, to, types)
}
