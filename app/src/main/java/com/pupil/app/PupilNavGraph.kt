package com.pupil.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pupil.app.feature.home.HomeScreen
import com.pupil.app.feature.home.HomeViewModel
import com.pupil.app.feature.payment.ContactsPickerScreen
import com.pupil.app.feature.payment.EditTransactionScreen
import com.pupil.app.feature.payment.EditTransactionViewModel
import com.pupil.app.feature.payment.PaymentEntryScreen
import com.pupil.app.feature.payment.PaymentViewModel
import com.pupil.app.feature.payment.QRScanScreen
import com.pupil.app.feature.payment.UpiEntryScreen
import com.pupil.app.feature.reports.ReportsScreen
import com.pupil.app.feature.reports.ReportsViewModel
import com.pupil.app.feature.settings.SettingsScreen
import com.pupil.app.feature.settings.SettingsViewModel

object Screen {
    const val Home = "home"
    const val QRScan = "qr_scan"
    const val UpiEntry = "upi_entry"
    const val ContactsPicker = "contacts_picker"
    const val PaymentEntry = "payment_entry"
    const val Reports = "reports"
    const val Settings = "settings"
    const val EditTransaction = "edit_transaction/{transactionId}"

    fun editTransaction(transactionId: Long) = "edit_transaction/$transactionId"
}

@Composable
fun PupilNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home) {
        composable(Screen.Home) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val pendingDeleteId by viewModel.pendingDeleteId.collectAsState()
            HomeScreen(
                uiState = uiState,
                pendingDeleteId = pendingDeleteId,
                onScanPay = { navController.navigate(Screen.QRScan) },
                onManualEntry = { navController.navigate("${Screen.PaymentEntry}?upiId=&merchantName=") },
                onOpenReports = { navController.navigate(Screen.Reports) },
                onOpenSettings = { navController.navigate(Screen.Settings) },
                onDeleteTransaction = viewModel::requestDeleteTransaction,
                onMarkCompleted = viewModel::markTransactionAsCompleted,
                onMarkFailed = viewModel::markTransactionAsFailed,
                onConfirmDelete = viewModel::confirmDeleteTransaction,
                onCancelDelete = viewModel::cancelDeleteTransaction,
                onEnterUpiId = { navController.navigate(Screen.UpiEntry) },
                onPickContact = { navController.navigate(Screen.ContactsPicker) },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.editTransaction(transactionId))
                }
            )
        }
        composable(Screen.QRScan) {
            QRScanScreen(
                onBack = { navController.popBackStack() },
                onContinue = { upiId, merchantCode ->
                    navController.navigate(
                        "${Screen.PaymentEntry}?upiId=${Uri.encode(upiId)}&merchantName=&merchantCode=${merchantCode?.let { Uri.encode(it) } ?: ""}"
                    )
                }
            )
        }
        composable(Screen.UpiEntry) {
            UpiEntryScreen(
                onBack = { navController.popBackStack() },
                onContinue = { upiId ->
                    navController.navigate("${Screen.PaymentEntry}?upiId=${Uri.encode(upiId)}&merchantName=")
                }
            )
        }
        composable(Screen.ContactsPicker) {
            ContactsPickerScreen(
                onBack = { navController.popBackStack() },
                onContactSelected = { phoneNumber ->
                    navController.navigate("${Screen.PaymentEntry}?upiId=${Uri.encode(phoneNumber)}&merchantName=${Uri.encode(phoneNumber)}")
                }
            )
        }
        composable(
            route = "${Screen.PaymentEntry}?upiId={upiId}&merchantName={merchantName}&merchantCode={merchantCode}",
            arguments = listOf(
                navArgument("upiId") { type = NavType.StringType; defaultValue = "" },
                navArgument("merchantName") { type = NavType.StringType; defaultValue = "" },
                navArgument("merchantCode") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val upiId = backStackEntry.arguments?.getString("upiId")?.takeIf { it.isNotBlank() }
            val paymentViewModel: PaymentViewModel = hiltViewModel()
            PaymentEntryScreen(
                upiId = upiId,
                merchantName = backStackEntry.arguments?.getString("merchantName")?.takeIf { it.isNotBlank() },
                merchantCode = backStackEntry.arguments?.getString("merchantCode")?.takeIf { it.isNotBlank() },
                viewModel = paymentViewModel,
                onBack = { navController.popBackStack() },
                onPaymentComplete = {
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
        }
        composable(
            route = Screen.EditTransaction,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: return@composable
            val editViewModel: EditTransactionViewModel = hiltViewModel()
            val transaction by editViewModel.transaction.collectAsState()
            val isLoading by editViewModel.isLoading.collectAsState()
            val errorMessage by editViewModel.error.collectAsState()

            LaunchedEffect(transactionId) {
                editViewModel.loadTransaction(transactionId)
            }

            val existingTransaction = transaction
            if (isLoading) {
                // Could show a loading indicator, but skip for simplicity
            }
            if (errorMessage != null) {
                // Could show error, but skip for simplicity
            }
            if (existingTransaction != null) {
                EditTransactionScreen(
                    existingTransaction = existingTransaction,
                    onBack = { navController.popBackStack() },
                    onSave = { updatedTransaction ->
                        editViewModel.updateTransaction(updatedTransaction)
                        navController.popBackStack(Screen.Home, inclusive = false)
                    }
                )
            }
        }
        composable(Screen.Reports) {
            val viewModel: ReportsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            ReportsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val isExporting by viewModel.isExporting.collectAsState()
            val isImporting by viewModel.isImporting.collectAsState()
            val backupResult by viewModel.backupResult.collectAsState()
            SettingsScreen(
                uiState = uiState,
                onToggleAppEnabled = viewModel::setAppEnabled,
                onAddCustomApp = viewModel::addCustomApp,
                onExportBackup = viewModel::exportBackup,
                onImportBackup = viewModel::importBackup,
                isExporting = isExporting,
                isImporting = isImporting,
                backupResult = backupResult,
                onClearBackupResult = viewModel::clearBackupResult,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
