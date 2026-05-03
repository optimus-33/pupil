package com.pupil.app.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pupil.app.core.data.backup.BackupManager
import com.pupil.app.core.data.backup.BackupResult
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.usecase.TransactionUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val transactionUseCases: TransactionUseCases,
    private val backupManager: BackupManager,
    private val application: Application
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = transactionUseCases.getAllPaymentApps()
        .map { apps -> SettingsUiState(apps = apps) }
        .stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

    private val _backupResult = MutableStateFlow<BackupResult?>(null)
    val backupResult: StateFlow<BackupResult?> = _backupResult.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

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

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val result = backupManager.exportBackup(application, uri)
                _backupResult.value = result
            } catch (e: Exception) {
                _backupResult.value = BackupResult(false, "Export error: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = backupManager.importBackup(application, uri)
                _backupResult.value = result
            } catch (e: Exception) {
                _backupResult.value = BackupResult(false, "Import error: ${e.message}")
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun clearBackupResult() {
        _backupResult.value = null
    }
}

data class SettingsUiState(
    val apps: List<PaymentAppConfig> = emptyList()
)
