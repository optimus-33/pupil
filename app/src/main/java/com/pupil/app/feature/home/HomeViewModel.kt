package com.pupil.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.domain.usecase.TransactionUseCases
import com.pupil.app.core.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {
    private val transactionsFlow = transactionUseCases.getAllTransactions()
    private val todayTotalFlow = transactionUseCases.getTodayTotal(
        DateUtils.startOfDay(System.currentTimeMillis()),
        DateUtils.endOfDay(System.currentTimeMillis())
    )

    // Track transaction pending deletion confirmation
    private val _pendingDeleteId = MutableStateFlow<Long?>(null)
    val pendingDeleteId: StateFlow<Long?> = _pendingDeleteId.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(transactionsFlow, todayTotalFlow) { transactions, todayTotal ->
        HomeUiState(
            transactions = transactions,
            todayTotalPaise = todayTotal
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())

    init {
        // Auto-expire pending transactions older than 10 minutes
        viewModelScope.launch {
            val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L
            transactionUseCases.getPendingTransactions().first().forEach { txn ->
                if (txn.timestamp < tenMinutesAgo) {
                    transactionUseCases.updateTransactionStatus(txn.id, TransactionStatus.FAILED.statusName)
                }
            }
        }
    }

    fun requestDeleteTransaction(id: Long) {
        _pendingDeleteId.value = id
    }

    fun confirmDeleteTransaction() {
        val id = _pendingDeleteId.value ?: return
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(id)
            _pendingDeleteId.value = null
        }
    }

    fun cancelDeleteTransaction() {
        _pendingDeleteId.value = null
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionUseCases.deleteTransaction(id)
        }
    }

    fun markTransactionAsFailed(id: Long) {
        viewModelScope.launch {
            transactionUseCases.updateTransactionStatus(id, TransactionStatus.FAILED.statusName)
        }
    }

    fun markTransactionAsCompleted(id: Long) {
        viewModelScope.launch {
            transactionUseCases.updateTransactionStatus(id, TransactionStatus.COMPLETED.statusName)
        }
    }
}

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val todayTotalPaise: Long = 0L
)

