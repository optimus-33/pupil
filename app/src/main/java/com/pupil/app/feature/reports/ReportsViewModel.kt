package com.pupil.app.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.CategoryTotal
import com.pupil.app.core.domain.usecase.TransactionUseCases
import com.pupil.app.core.ui.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    transactionUseCases: TransactionUseCases
) : ViewModel() {
    private val weekTotal = transactionUseCases.getWeeklyTotal(
        DateUtils.startOfWeek(System.currentTimeMillis()),
        System.currentTimeMillis()
    )
    private val monthTotal = transactionUseCases.getMonthlyTotal(
        DateUtils.startOfMonth(System.currentTimeMillis()),
        System.currentTimeMillis()
    )
    private val categoryTotals = transactionUseCases.getCategoryTotalsInRange(
        DateUtils.startOfMonth(System.currentTimeMillis()),
        System.currentTimeMillis()
    )

    val uiState: StateFlow<ReportsUiState> = combine(weekTotal, monthTotal, categoryTotals) { week, month, breakdown ->
        ReportsUiState(
            weekTotalPaise = week,
            monthTotalPaise = month,
            categoryBreakdown = breakdown
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, ReportsUiState())
}

data class ReportsUiState(
    val weekTotalPaise: Long = 0L,
    val monthTotalPaise: Long = 0L,
    val categoryBreakdown: List<CategoryTotal> = emptyList()
)
