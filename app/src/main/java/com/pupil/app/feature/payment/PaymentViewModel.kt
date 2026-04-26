package com.pupil.app.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.usecase.TransactionUseCases
import com.pupil.app.core.ui.util.DateUtils
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

    fun selectPaymentType(paymentType: PaymentType) {
        paymentTypeFlow.value = paymentType
        selectedAppFlow.value = null
    }

    fun selectPaymentApp(app: PaymentAppConfig) {
        selectedAppFlow.value = app
    }

    fun saveTransaction(
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
            transactionUseCases.saveTransaction(
                Transaction(
                    merchantName = merchantName.ifBlank { "Unknown merchant" },
                    upiId = upiId,
                    amountPaise = amountPaise,
                    reason = reason,
                    category = category,
                    paymentType = paymentType,
                    paymentApp = paymentAppName,
                    timestamp = System.currentTimeMillis(),
                    isManual = isManual
                )
            )
        }
    }
}
