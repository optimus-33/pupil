package com.pupil.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pupil.app.core.ui.components.TransactionCard
import com.pupil.app.core.ui.util.DateUtils
import com.pupil.app.core.ui.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onScanPay: () -> Unit,
    onManualEntry: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteTransaction: ((Long) -> Unit)? = null,
    onMarkCompleted: ((Long) -> Unit)? = null,
    onMarkFailed: ((Long) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = DateUtils.formatTodayDate(), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Today ₹${Formatters.formatPaise(uiState.todayTotalPaise)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    Button(onClick = onOpenReports) {
                        Text(text = "Reports")
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Button(onClick = onOpenSettings) {
                        Text(text = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanPay) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Pay")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Button(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Log manually")
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (uiState.transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No transactions yet. Tap Pay or Log manually to begin.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                } else {
                    val grouped = uiState.transactions.groupBy { DateUtils.formatDateGroup(it.timestamp) }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        grouped.forEach { (dateLabel, items) ->
                            item {
                                Text(text = dateLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            }
                            items(items) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    onDelete = onDeleteTransaction,
                                    onMarkCompleted = onMarkCompleted,
                                    onMarkFailed = onMarkFailed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

