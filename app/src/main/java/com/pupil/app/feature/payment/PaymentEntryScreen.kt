package com.pupil.app.feature.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pupil.app.core.domain.model.PaymentAppConfig
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.ui.components.PaymentAppCard
import com.pupil.app.core.ui.components.TypeChip
import com.pupil.app.core.ui.util.Formatters
import com.pupil.app.core.ui.util.toPaise
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    upiId: String?,
    merchantName: String?,
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val paymentType = viewModel.selectedPaymentType.value
    val paymentApps = viewModel.paymentApps.value
    val selectedApp = viewModel.selectedApp.value
    val coroutineScope = rememberCoroutineScope()
    var amountInput by rememberSaveable { mutableStateOf("") }
    var reasonInput by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

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
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text = "Reason (why are you spending this?)", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text(text = "Groceries, coffee, gift, rent...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 5,
                    singleLine = false
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.take(12) },
                    label = { Text(text = "Amount (₹)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = merchantName ?: upiId.orEmpty(),
                    onValueChange = { },
                    label = { Text(text = "Merchant / UPI ID") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                Text(text = "Category", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf("Food", "Transport", "Groceries", "Shopping", "Bills", "Health", "Entertainment", "Other").forEach { item ->
                        TypeChip(selected = category == item, onClick = { category = item }, label = item)
                    }
                }
                Text(text = "Payment type", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(PaymentType.UPI, PaymentType.UPI_CREDIT_CARD).forEach { type ->
                        TypeChip(selected = paymentType == type, onClick = { viewModel.selectPaymentType(type) }, label = if (type == PaymentType.UPI) "UPI" else "UPI Credit Card")
                    }
                }
                Text(text = "Select app", style = MaterialTheme.typography.titleSmall)
                if (paymentApps.isEmpty()) {
                    Text(text = "No configured apps for ${paymentType.typeName}. Check settings.", color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(paymentApps) { app ->
                            PaymentAppCard(app = app, selected = selectedApp?.id == app.id, onClick = { viewModel.selectPaymentApp(app) })
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                if (!errorMessage.isNullOrBlank()) {
                    Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = {
                    val paise = amountInput.toPaise()
                    if (reasonInput.isBlank()) {
                        errorMessage = "Please add a reason for this payment."
                        return@Button
                    }
                    if (paise == null || paise <= 0L) {
                        errorMessage = "Enter a valid amount."
                        return@Button
                    }
                    val app = selectedApp
                    val shouldLaunch = !upiId.isNullOrBlank() && app != null
                    val paymentAppName = app?.displayName ?: "Manual"
                    viewModel.saveTransaction(
                        merchantName = merchantName.orEmpty().ifBlank { upiId.orEmpty().ifBlank { "Manual entry" } },
                        upiId = upiId,
                        amountPaise = paise,
                        reason = reasonInput,
                        category = category,
                        paymentType = paymentType,
                        paymentAppName = paymentAppName,
                        isManual = upiId.isNullOrBlank()
                    )
                    if (shouldLaunch) {
                        val uri = Uri.parse("upi://pay?pa=${Uri.encode(upiId)}&am=${Formatters.formatPaise(paise)}&cu=INR&tn=${Uri.encode(reasonInput)}")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage(app.packageName)
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            errorMessage = "Selected app is not installed."
                        }
                    } else {
                        coroutineScope.launch {
                            errorMessage = "Transaction recorded locally."
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Pay & Record")
                }
            }
        }
    }
}
