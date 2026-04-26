package com.pupil.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.usecase.TransactionUseCases
import com.pupil.app.core.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    transactionUseCases: TransactionUseCases
) : ViewModel() {
    private val transactionsFlow = transactionUseCases.getAllTransactions()
    private val todayTotalFlow = transactionUseCases.getTodayTotal(
        DateUtils.startOfDay(System.currentTimeMillis()),
        DateUtils.endOfDay(System.currentTimeMillis())
    )

    val uiState: StateFlow<HomeUiState> = combine(transactionsFlow, todayTotalFlow) { transactions, todayTotal ->
        HomeUiState(
            transactions = transactions,
            todayTotalPaise = todayTotal
        )
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, HomeUiState())
}

data class HomeUiState(
    val transactions: List<Transaction> = emptyList(),
    val todayTotalPaise: Long = 0L
)
