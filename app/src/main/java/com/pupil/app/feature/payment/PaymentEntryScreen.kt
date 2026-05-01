package com.pupil.app.feature.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.ui.components.PaymentAppCard
import com.pupil.app.core.ui.components.TypeChip
import com.pupil.app.core.ui.util.Formatters
import com.pupil.app.core.ui.util.toPaise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    upiId: String?,
    merchantName: String?,
    viewModel: PaymentViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val paymentType by viewModel.selectedPaymentType.collectAsState()
    val paymentApps by viewModel.paymentApps.collectAsState()
    val selectedApp by viewModel.selectedApp.collectAsState()
    var amountInput by rememberSaveable { mutableStateOf("") }
    var reasonInput by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var errorMessage by rememberSaveable { mutableStateOf("") }

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
                Text(text = "Why are you spending this?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text(text = "Groceries, coffee, gift, rent…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Food", "Transport", "Groceries", "Shopping", "Bills", "Health", "Entertainment", "Other")) { item ->
                        TypeChip(selected = category == item, onClick = { category = item }, label = item)
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
                if (paymentApps.isEmpty()) {
                    Text(text = "No configured apps for ${paymentType.typeName}. Check Settings.", color = Color.Gray)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(paymentApps) { app ->
                            PaymentAppCard(
                                app = app,
                                selected = selectedApp?.id == app.id,
                                onClick = { viewModel.selectPaymentApp(app) }
                            )
                        }
                    }
                }
                if (errorMessage.isNotBlank()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
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
                        if (!upiId.isNullOrBlank() && app != null) {
                            val uri = Uri.parse("upi://pay?pa=${Uri.encode(upiId)}&am=${Formatters.formatPaise(paise)}&cu=INR&tn=${Uri.encode(reasonInput)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage(app.packageName)
                            }
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                errorMessage = "${app.displayName} is not installed."
                            }
                        } else {
                            errorMessage = "Transaction recorded."
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
