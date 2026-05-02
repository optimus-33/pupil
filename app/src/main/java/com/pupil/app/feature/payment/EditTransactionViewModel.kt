package com.pupil.app.feature.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.usecase.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {

    private val _transaction = MutableStateFlow<Transaction?>(null)
    val transaction: StateFlow<Transaction?> = _transaction

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _transaction.value = transactionUseCases.getTransactionById(id)
                if (_transaction.value == null) {
                    _error.value = "Transaction not found"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load transaction: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                transactionUseCases.updateTransaction(transaction)
            } catch (e: Exception) {
                _error.value = "Failed to update transaction: ${e.message}"
            }
        }
    }
}
