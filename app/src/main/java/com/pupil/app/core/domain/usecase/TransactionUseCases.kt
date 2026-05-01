package com.pupil.app.core.domain.usecase

import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

data class TransactionUseCases(
    val getAllTransactions: GetAllTransactionsUseCase,
    val getTodayTotal: GetTotalInRangeUseCase,
    val getWeeklyTotal: GetTotalInRangeUseCase,
    val getMonthlyTotal: GetTotalInRangeUseCase,
    val getCategoryTotalsInRange: GetCategoryTotalsInRangeUseCase,
    val saveTransaction: SaveTransactionUseCase,
    val deleteTransaction: DeleteTransactionUseCase,
    val updateTransactionStatus: UpdateTransactionStatusUseCase,
    val getPendingTransactions: GetPendingTransactionsUseCase,
    val getPaymentAppsByType: GetPaymentAppsByTypeUseCase,
    val getAllPaymentApps: GetAllPaymentAppsUseCase,
    val setPaymentAppEnabled: SetPaymentAppEnabledUseCase,
    val addPaymentAppConfig: AddPaymentAppConfigUseCase
)

class GetAllTransactionsUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAllTransactions()
}

class GetTotalInRangeUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    operator fun invoke(from: Long, to: Long): Flow<Long> = repository.getTotalInRange(from, to)
}

class GetCategoryTotalsInRangeUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    operator fun invoke(from: Long, to: Long): Flow<List<CategoryTotal>> = repository.getCategoryTotalsInRange(from, to)
}

class SaveTransactionUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    suspend operator fun invoke(transaction: Transaction): Long = repository.insertTransaction(transaction)
}

class DeleteTransactionUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    suspend operator fun invoke(id: Long) = repository.deleteTransaction(id)
}

class UpdateTransactionStatusUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    suspend operator fun invoke(id: Long, status: String) = repository.updateTransactionStatus(id, status)
}

class GetPendingTransactionsUseCase(private val repository: com.pupil.app.core.domain.repository.TransactionRepository) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getPendingTransactions()
}

class GetPaymentAppsByTypeUseCase(private val repository: com.pupil.app.core.domain.repository.PaymentAppConfigRepository) {
    operator fun invoke(paymentType: PaymentType): Flow<List<PaymentAppConfig>> = repository.getPaymentAppsByType(paymentType)
}

class GetAllPaymentAppsUseCase(private val repository: com.pupil.app.core.domain.repository.PaymentAppConfigRepository) {
    operator fun invoke(): Flow<List<PaymentAppConfig>> = repository.getPaymentApps()
}

class SetPaymentAppEnabledUseCase(private val repository: com.pupil.app.core.domain.repository.PaymentAppConfigRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean) = repository.setAppEnabled(id, enabled)
}

class AddPaymentAppConfigUseCase(private val repository: com.pupil.app.core.domain.repository.PaymentAppConfigRepository) {
    suspend operator fun invoke(config: PaymentAppConfig) = repository.addPaymentAppConfig(config)
}

