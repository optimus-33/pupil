package com.pupil.app.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.domain.usecase.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {
    private val paymentTypeFlow = MutableStateFlow(PaymentType.UPI)
    private val selectedAppFlow = MutableStateFlow<PaymentAppConfig?>(null)

    val paymentApps: StateFlow<List<PaymentAppConfig>> = paymentTypeFlow.flatMapLatest {
        transactionUseCases.getPaymentAppsByType(it)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedPaymentType: StateFlow<PaymentType> = paymentTypeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, PaymentType.UPI)
    val selectedApp: StateFlow<PaymentAppConfig?> = selectedAppFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _pendingTransactionId = MutableStateFlow<Long?>(null)
    val pendingTransactionId: StateFlow<Long?> = _pendingTransactionId.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    fun selectPaymentType(paymentType: PaymentType) {
        paymentTypeFlow.value = paymentType
        selectedAppFlow.value = null
    }

    fun selectPaymentApp(app: PaymentAppConfig) {
        selectedAppFlow.value = app
    }

    /**
     * Creates a pending transaction and returns its ID.
     * The UPI app launch should happen after this.
     */
    fun createPendingTransaction(
        merchantName: String,
        upiId: String?,
        amountPaise: Long,
        reason: String,
        category: String,
        paymentType: PaymentType,
        paymentAppName: String,
        isManual: Boolean
    ) {
        viewModelScope.launch {
            val id = transactionUseCases.saveTransaction(
                Transaction(
                    merchantName = merchantName.ifBlank { upiId ?: "Unknown merchant" },
                    upiId = upiId,
                    amountPaise = amountPaise,
                    reason = reason,
                    category = category,
                    paymentType = paymentType,
                    paymentApp = paymentAppName,
                    timestamp = System.currentTimeMillis(),
                    isManual = isManual,
                    status = TransactionStatus.PENDING
                )
            )
            _pendingTransactionId.value = id
        }
    }

    /**
     * Mark the pending transaction as completed (payment succeeded)
     */
    fun confirmPaymentSuccess() {
        val id = _pendingTransactionId.value ?: return
        viewModelScope.launch {
            transactionUseCases.updateTransactionStatus(id, TransactionStatus.COMPLETED.statusName)
            _pendingTransactionId.value = null
        }
    }

    /**
     * Mark the pending transaction as failed (payment failed)
     */
    fun confirmPaymentFailed() {
        val id = _pendingTransactionId.value ?: return
        viewModelScope.launch {
            transactionUseCases.updateTransactionStatus(id, TransactionStatus.FAILED.statusName)
            _pendingTransactionId.value = null
        }
    }

    /**
     * Delete the pending transaction entirely (user chose to discard)
     */
    fun discardPendingTransaction() {
        val id = _pendingTransactionId.value ?: return
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(id)
            _pendingTransactionId.value = null
        }
    }

    fun clearError() {
        _errorMessage.value = ""
    }
}

