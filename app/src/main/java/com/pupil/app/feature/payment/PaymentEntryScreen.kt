package com.pupil.app.feature.payment

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType.Companion.LongPress
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.ui.components.PaymentAppCard
import com.pupil.app.core.ui.components.TypeChip
import com.pupil.app.core.ui.util.InstalledUpiApp
import com.pupil.app.core.ui.util.InstalledUpiAppsResolver
import com.pupil.app.core.ui.util.toPaise
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    upiId: String?,
    merchantName: String?,
    merchantCode: String?,
    viewModel: PaymentViewModel,
    onBack: () -> Unit,
    onPaymentComplete: () -> Unit = onBack
) {
    val context = LocalContext.current
    val paymentType by viewModel.selectedPaymentType.collectAsState()
    val dbPaymentApps by viewModel.paymentApps.collectAsState()
    val selectedDbApp by viewModel.selectedApp.collectAsState()
    val pendingTransactionId by viewModel.pendingTransactionId.collectAsState()
    val viewModelError by viewModel.errorMessage.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    var amountInput by rememberSaveable { mutableStateOf("") }
    var reasonInput by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(8L) }  // Default "Other" category (id=8 after seed)
    var showResultDialog by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf("") }

    // Date/time state
    var customTimestamp by rememberSaveable { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    // Manual entry state: editable UPI ID / mobile and payee name
    val isManualEntry = upiId.isNullOrBlank()
    var manualUpiId by rememberSaveable { mutableStateOf("") }
    var manualPayeeName by rememberSaveable { mutableStateOf("") }
    val effectiveUpiId = if (isManualEntry) manualUpiId.ifBlank { upiId.orEmpty() } else upiId.orEmpty()

    // Installed UPI apps from device
    var installedUpiApps by rememberSaveable { mutableStateOf<List<InstalledUpiApp>>(emptyList()) }
    // Selected installed app info (stored as simple strings to avoid type issues)
    var selectedInstalledAppName by rememberSaveable { mutableStateOf("") }
    var selectedInstalledAppPackage by rememberSaveable { mutableStateOf("") }

    // Discover installed UPI apps
    LaunchedEffect(Unit) {
        installedUpiApps = InstalledUpiAppsResolver.getInstalledUpiApps(context.packageManager)
    }

    // UPI launcher with callback to detect app response (SUCCESS/FAILURE/cancelled)
    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = result.data?.getStringExtra("response") ?: ""
        val status = result.data?.getStringExtra("Status") ?: ""
        when {
            response.contains("SUCCESS", ignoreCase = true) || status == "SUCCESS" -> {
                viewModel.confirmPaymentSuccess()
                onPaymentComplete()
            }
            response.contains("FAILURE", ignoreCase = true) || status == "FAILURE" -> {
                viewModel.confirmPaymentFailed()
            }
            else -> {
                showResultDialog = true
            }
        }
    }

    // Combined apps: When UPI type is selected, show installed apps + DB apps as supplement
    // For UPI_CREDIT_CARD, show DB apps only
    data class AppDisplayItem(val displayName: String, val packageName: String, val isFromDb: Boolean)

    val allUpiApps = if (paymentType == PaymentType.UPI) {
        val installedPackages = installedUpiApps.map { it.packageName }.toSet()
        val dbOnlyApps = dbPaymentApps.filter { it.packageName !in installedPackages }
        installedUpiApps.map { AppDisplayItem(it.displayName, it.packageName, isFromDb = false) } +
        dbOnlyApps.map { AppDisplayItem(it.displayName, it.packageName, isFromDb = true) }
    } else {
        dbPaymentApps.map { AppDisplayItem(it.displayName, it.packageName, isFromDb = true) }
    }

    // Date formatting
    val dateFormat = rememberSaveable { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = rememberSaveable { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    // Show result dialog when pending transaction exists
    LaunchedEffect(pendingTransactionId) {
        if (pendingTransactionId != null && !showResultDialog) {
            showResultDialog = true
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = customTimestamp)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = customTimestamp
                            val hour = get(Calendar.HOUR_OF_DAY)
                            val minute = get(Calendar.MINUTE)
                            timeInMillis = selectedMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                        }
                        customTimestamp = cal.timeInMillis
                    }
                    showDatePicker = false
                }) {
                    Text(text = "Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = customTimestamp }
        val timePickerState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(text = "Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = customTimestamp
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    customTimestamp = cal.timeInMillis
                    showTimePicker = false
                }) {
                    Text(text = "Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    // Payment result dialog
    if (showResultDialog && pendingTransactionId != null) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text(text = "Did the payment go through?") },
            text = {
                Text(
                    "Please confirm whether the payment was successful or failed. " +
                    "If it failed, you can delete this transaction and try again."
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        viewModel.confirmPaymentFailed()
                        showResultDialog = false
                    }) {
                        Text(text = "Payment failed")
                    }
                    Button(onClick = {
                        viewModel.confirmPaymentSuccess()
                        showResultDialog = false
                        onPaymentComplete()
                    }) {
                        Text(text = "Payment succeeded")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.discardPendingTransaction()
                    showResultDialog = false
                    onPaymentComplete()
                }) {
                    Text(text = "Delete this transaction", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Payment details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 'Why' reason field - visually prominent
                Text(
                    text = "Why are you spending this?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text(text = "Groceries, coffee, gift, rent\u2026") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 6,
                    singleLine = false
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' }.let { str -> if (str.count { c -> c == '.' } > 1) amountInput else str }.take(12) },
                    label = { Text(text = "Amount (\u20B9)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Payee Name field - shown for manual entries, helpful for all
                if (isManualEntry) {
                    OutlinedTextField(
                        value = manualPayeeName,
                        onValueChange = { manualPayeeName = it },
                        label = { Text(text = "Payee Name (person / shop / payee)") },
                        placeholder = { Text(text = "e.g. Rahul, Grocery Store") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = if (isManualEntry) manualUpiId else (merchantName ?: upiId.orEmpty()),
                    onValueChange = { if (isManualEntry) manualUpiId = it },
                    label = { Text(text = "Merchant / UPI ID / Mobile") },
                    placeholder = { if (isManualEntry) Text(text = "e.g. name@bank or +919876543210") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = !isManualEntry,
                    singleLine = true
                )

                // Date/Time picker - shown for manual entries with custom date/time
                if (isManualEntry) {
                    Text(text = "Transaction Date & Time", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = dateFormat.format(Date(customTimestamp)))
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = timeFormat.format(Date(customTimestamp)))
                        }
                    }
                }

                Text(text = "Category", style = MaterialTheme.typography.titleSmall)
                // Category ID mapping matching the seeded categories: 1=Food, 2=Transport, 3=Groceries, 4=Shopping, 5=Bills, 6=Health, 7=Entertainment, 8=Other
                val categoryItems = listOf(
                    1L to "Food", 2L to "Transport", 3L to "Groceries", 4L to "Shopping",
                    5L to "Bills", 6L to "Health", 7L to "Entertainment", 8L to "Other"
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categoryItems) { (id, name) ->
                        TypeChip(selected = categoryId == id, onClick = { categoryId = id }, label = name)
                    }
                }

                Text(text = "Payment type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listOf(PaymentType.UPI, PaymentType.UPI_CREDIT_CARD)) { type ->
                        TypeChip(
                            selected = paymentType == type,
                            onClick = { viewModel.selectPaymentType(type) },
                            label = if (type == PaymentType.UPI) "UPI" else "UPI Credit Card"
                        )
                    }
                }

                Text(text = "Select app", style = MaterialTheme.typography.titleSmall)
                if (allUpiApps.isEmpty()) {
                    Text(text = "No apps available for ${paymentType.typeName}.", color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(allUpiApps) { app ->
                            val isSelected = if (app.isFromDb) {
                                selectedDbApp?.packageName == app.packageName
                            } else {
                                selectedInstalledAppPackage == app.packageName
                            }
                            PaymentAppCard(
                                app = PaymentAppConfig(
                                    displayName = app.displayName,
                                    packageName = app.packageName,
                                    paymentType = paymentType,
                                    enabled = true
                                ),
                                selected = isSelected,
                                onClick = {
                                    if (app.isFromDb) {
                                        val dbApp = dbPaymentApps.find { it.packageName == app.packageName }
                                        if (dbApp != null) {
                                            viewModel.selectPaymentApp(dbApp)
                                        }
                                        selectedInstalledAppName = ""
                                        selectedInstalledAppPackage = ""
                                    } else {
                                        selectedInstalledAppName = app.displayName
                                        selectedInstalledAppPackage = app.packageName
                                        viewModel.selectPaymentApp(null)
                                    }
                                }
                            )
                        }
                    }
                }

                val displayError = if (viewModelError.isNotBlank()) viewModelError else localError
                if (displayError.isNotBlank()) {
                    Text(text = displayError, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        hapticFeedback.performHapticFeedback(LongPress)

                        val paise = amountInput.toPaise()
                        if (reasonInput.isBlank()) {
                            localError = "Please add a reason for this payment."
                            return@Button
                        }
                        if (paise == null || paise <= 0L) {
                            localError = "Enter a valid amount."
                            return@Button
                        }
                        localError = ""
                        viewModel.clearError()

                        val paymentAppName = selectedDbApp?.displayName
                            ?: selectedInstalledAppName.ifBlank { null }
                            ?: "Manual"
                        val paymentAppPackage = selectedDbApp?.packageName
                            ?: selectedInstalledAppPackage.ifBlank { null }

                        // Determine final UPI ID and merchant name, handling manual entry and mobile numbers
                        val finalUpiId = if (isManualEntry) manualUpiId.ifBlank { null } else upiId
                        val finalMerchantName = if (isManualEntry) manualPayeeName.ifBlank { null }
                            else merchantName?.takeIf { !it.contains("@") }

                        // Mobile number detection: extract digits, append @upi
                        // If 10 digits → digits@upi; if 12 digits starting with 91 → digits@upi
                        val digitsOnly = finalUpiId?.replace(Regex("[^0-9]"), "") ?: ""
                        val resolvedUpiId = when {
                            digitsOnly.length == 10 -> "${digitsOnly}@upi"
                            digitsOnly.length == 12 && digitsOnly.startsWith("91") -> "${digitsOnly}@upi"
                            else -> finalUpiId
                        }
                        val isMobileNumber = resolvedUpiId != finalUpiId

                        // Derive payee name
                        val resolvedPayeeName = if (isManualEntry && manualPayeeName.isNotBlank()) {
                            manualPayeeName
                        } else if (isMobileNumber) {
                            "Contact (${digitsOnly.takeLast(10)})"
                        } else if (finalMerchantName != null) {
                            finalMerchantName
                        } else if (resolvedUpiId?.contains("@") == true) {
                            resolvedUpiId.substringBefore("@").replaceFirstChar { it.uppercase() }
                        } else {
                            "Payee"
                        }

                        // Save as PENDING first, then open UPI app
                        viewModel.createPendingTransaction(
                            merchantName = resolvedPayeeName,
                            upiId = resolvedUpiId,
                            amountPaise = paise,
                            reason = reasonInput,
                            notes = null,
                            categoryId = categoryId,
                            paymentType = paymentType,
                            paymentAppName = paymentAppName,
                            isManual = finalUpiId.isNullOrBlank(),
                            customTimestamp = if (isManualEntry) customTimestamp else null
                        )

                        if (!resolvedUpiId.isNullOrBlank() && paymentAppPackage != null) {
                            val amountRupees = paise / 100

                            // Build UPI URI using Uri.Builder with mandatory params per UPI Linking Spec v1.6
                            val uri = Uri.Builder()
                                .scheme("upi")
                                .authority("pay")
                                .appendQueryParameter("pa", resolvedUpiId)
                                .appendQueryParameter("pn", resolvedPayeeName)
                                .appendQueryParameter("am", amountRupees.toString())
                                .appendQueryParameter("cu", "INR")
                                .appendQueryParameter("tn", reasonInput)
                                .appendQueryParameter("tr", "PPL${System.currentTimeMillis()}")
                                .appendQueryParameter("mc", merchantCode ?: "0000")
                                .appendQueryParameter("mode", "04")
                                .build()
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage(paymentAppPackage)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                upiLauncher.launch(intent)
                            } else {
                                localError = "$paymentAppName is not installed. Transaction saved as pending."
                            }
                        } else {
                            // Manual entry or no app selected - mark as completed directly
                            viewModel.confirmPaymentSuccess()
                            onPaymentComplete()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Pay & Record")
                }
            }
        }
    }
}
