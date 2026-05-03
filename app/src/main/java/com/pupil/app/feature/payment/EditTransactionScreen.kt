package com.pupil.app.feature.payment

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.domain.model.Transaction
import com.pupil.app.core.domain.model.TransactionStatus
import com.pupil.app.core.ui.components.TypeChip
import com.pupil.app.core.ui.util.Formatters
import com.pupil.app.core.ui.util.toPaise
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    existingTransaction: Transaction,
    onBack: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var reasonInput by rememberSaveable { mutableStateOf(existingTransaction.reason) }
    var amountInput by rememberSaveable {
        mutableStateOf(Formatters.formatPaise(existingTransaction.amountPaise))
    }
    var categoryId by rememberSaveable { mutableStateOf(existingTransaction.categoryId) }
    var paymentType by rememberSaveable { mutableStateOf(existingTransaction.paymentType) }
    var paymentApp by rememberSaveable { mutableStateOf(existingTransaction.paymentApp) }
    var customTimestamp by rememberSaveable { mutableStateOf(existingTransaction.timestamp) }
    var status by rememberSaveable { mutableStateOf(existingTransaction.status) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf("") }
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

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

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(text = "Save Changes") },
            text = { Text(text = "Are you sure you want to update this transaction?") },
            confirmButton = {
                Button(onClick = {
                    showConfirmDialog = false
                    val paise = amountInput.toPaise() ?: existingTransaction.amountPaise
                    onSave(
                        existingTransaction.copy(
                            reason = reasonInput,
                            amountPaise = paise,
                            categoryId = categoryId,
                            paymentType = paymentType,
                            paymentApp = paymentApp,
                            timestamp = customTimestamp,
                            status = status,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Edit Transaction") },
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
                // Reason field
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

                // Amount
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { c -> c.isDigit() || c == '.' }.let { str -> if (str.count { c -> c == '.' } > 1) amountInput else str }.take(12) },
                    label = { Text(text = "Amount (\u20B9)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Merchant info (read-only)
                OutlinedTextField(
                    value = existingTransaction.merchantName,
                    onValueChange = { },
                    label = { Text(text = "Merchant / UPI ID") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                // Date/Time picker
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

                // Category
                Text(text = "Category", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf(
                        1L to "Food", 2L to "Transport", 3L to "Groceries",
                        4L to "Shopping", 5L to "Bills", 6L to "Health",
                        7L to "Entertainment", 8L to "Other"
                    )
                    items(categories) { (id, name) ->
                        TypeChip(selected = categoryId == id, onClick = { categoryId = id }, label = name)
                    }
                }

                // Payment type
                Text(text = "Payment type", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(listOf(PaymentType.UPI, PaymentType.UPI_CREDIT_CARD)) { type ->
                        TypeChip(
                            selected = paymentType == type,
                            onClick = { paymentType = type },
                            label = if (type == PaymentType.UPI) "UPI" else "UPI Credit Card"
                        )
                    }
                }

                // Payment app name (editable as text)
                OutlinedTextField(
                    value = paymentApp,
                    onValueChange = { paymentApp = it },
                    label = { Text(text = "Payment App") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Transaction status (editable)
                Text(text = "Status", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(TransactionStatus.values()) { s ->
                        TypeChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = s.statusName
                        )
                    }
                }

                if (localError.isNotBlank()) {
                    Text(text = localError, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (reasonInput.isBlank()) {
                            localError = "Please add a reason for this payment."
                            return@Button
                        }
                        val paise = amountInput.toPaise()
                        if (paise == null || paise <= 0L) {
                            localError = "Enter a valid amount."
                            return@Button
                        }
                        localError = ""
                        showConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Save Changes")
                }
            }
        }
    }
}
