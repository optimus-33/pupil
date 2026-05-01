package com.pupil.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.usecase.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = transactionUseCases.getAllPaymentApps()
        .map { apps -> SettingsUiState(apps = apps) }
        .stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    fun setAppEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            transactionUseCases.setPaymentAppEnabled(id, enabled)
        }
    }

    fun addCustomApp(displayName: String, packageName: String, paymentType: PaymentType) {
        viewModelScope.launch {
            transactionUseCases.addPaymentAppConfig(
                PaymentAppConfig(
                    displayName = displayName,
                    packageName = packageName,
                    paymentType = paymentType,
                    enabled = true
                )
            )
        }
    }
}

data class SettingsUiState(
    val apps: List<PaymentAppConfig> = emptyList()
)
