package com.pupil.app

import android.net.Uri
import androidx.compose.runtime.Composable
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
}

@Composable
fun PupilNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home) {
        composable(Screen.Home) {
            val viewModel: HomeViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            HomeScreen(
                uiState = uiState,
                onScanPay = { navController.navigate(Screen.QRScan) },
                onManualEntry = { navController.navigate("${Screen.PaymentEntry}?upiId=&merchantName=") },
                onOpenReports = { navController.navigate(Screen.Reports) },
                onOpenSettings = { navController.navigate(Screen.Settings) },
                onDeleteTransaction = viewModel::deleteTransaction,
                onMarkCompleted = viewModel::markTransactionAsCompleted,
                onMarkFailed = viewModel::markTransactionAsFailed,
                onEnterUpiId = { navController.navigate(Screen.UpiEntry) },
                onPickContact = { navController.navigate(Screen.ContactsPicker) }
            )
        }
        composable(Screen.QRScan) {
            QRScanScreen(
                onBack = { navController.popBackStack() },
                onContinue = { upiId ->
                    navController.navigate("${Screen.PaymentEntry}?upiId=${Uri.encode(upiId)}&merchantName=")
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
            route = "${Screen.PaymentEntry}?upiId={upiId}&merchantName={merchantName}",
            arguments = listOf(
                navArgument("upiId") { type = NavType.StringType; defaultValue = "" },
                navArgument("merchantName") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val upiId = backStackEntry.arguments?.getString("upiId")?.takeIf { it.isNotBlank() }
            val paymentViewModel: PaymentViewModel = hiltViewModel()
            PaymentEntryScreen(
                upiId = upiId,
                merchantName = backStackEntry.arguments?.getString("merchantName")?.takeIf { it.isNotBlank() },
                viewModel = paymentViewModel,
                onBack = { navController.popBackStack() },
                onPaymentComplete = {
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
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
            SettingsScreen(
                uiState = uiState,
                onToggleAppEnabled = viewModel::setAppEnabled,
                onAddCustomApp = viewModel::addCustomApp,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
