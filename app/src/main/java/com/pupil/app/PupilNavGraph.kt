package com.pupil.app

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pupil.app.feature.home.HomeScreen
import com.pupil.app.feature.home.HomeViewModel
import com.pupil.app.feature.payment.PaymentEntryScreen
import com.pupil.app.feature.payment.PaymentViewModel
import com.pupil.app.feature.payment.QRScanScreen
import com.pupil.app.feature.reports.ReportsScreen
import com.pupil.app.feature.reports.ReportsViewModel
import com.pupil.app.feature.settings.SettingsScreen
import com.pupil.app.feature.settings.SettingsViewModel

object Screen {
    const val Home = "home"
    const val QRScan = "qr_scan"
    const val PaymentEntry = "payment_entry"
    const val Reports = "reports"
    const val Settings = "settings"
}

@Composable
fun PupilNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = Screen.Home) {
        composable(Screen.Home) {
            val viewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                uiState = viewModel.uiState,
                onScanPay = { navController.navigate(Screen.QRScan) },
                onManualEntry = { navController.navigate("${Screen.PaymentEntry}?upiId=&merchantName=") },
                onOpenReports = { navController.navigate(Screen.Reports) },
                onOpenSettings = { navController.navigate(Screen.Settings) }
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
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reports) {
            val viewModel: ReportsViewModel = hiltViewModel()
            ReportsScreen(
                uiState = viewModel.uiState,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                uiState = viewModel.uiState,
                onToggleAppEnabled = viewModel::setAppEnabled,
                onAddCustomApp = viewModel::addCustomApp,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
