package com.pupil.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pupil.app.core.domain.model.PaymentType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onToggleAppEnabled: (Long, Boolean) -> Unit,
    onAddCustomApp: (String, String, PaymentType) -> Unit,
    onBack: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var customName by rememberSaveable { mutableStateOf("") }
    var customPackage by rememberSaveable { mutableStateOf("") }
    var customType by rememberSaveable { mutableStateOf(PaymentType.UPI) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Button(onClick = { showDialog = true }) {
                Text(text = "Add custom app")
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val grouped = uiState.apps.groupBy { it.paymentType }
                grouped.forEach { (type, apps) ->
                    Text(text = type.typeName, style = MaterialTheme.typography.titleMedium)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        items(apps) { app ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(text = app.displayName, style = MaterialTheme.typography.bodyLarge)
                                        Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Checkbox(checked = app.enabled, onCheckedChange = { onToggleAppEnabled(app.id, it) })
                                }
                            }
                        }
                    }
                }
            }
            if (showDialog) {
                Dialog(onDismissRequest = { showDialog = false }) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(text = "Add custom payment app", style = MaterialTheme.typography.titleMedium)
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text(text = "Display name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = customPackage,
                                onValueChange = { customPackage = it },
                                label = { Text(text = "Package name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { customType = PaymentType.UPI }, modifier = Modifier.weight(1f)) {
                                    Text(text = "UPI")
                                }
                                Button(onClick = { customType = PaymentType.UPI_CREDIT_CARD }, modifier = Modifier.weight(1f)) {
                                    Text(text = "UPI Credit Card")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { showDialog = false }) {
                                    Text(text = "Cancel")
                                }
                                Button(onClick = {
                                    if (customName.isNotBlank() && customPackage.isNotBlank()) {
                                        onAddCustomApp(customName, customPackage, customType)
                                        customName = ""
                                        customPackage = ""
                                        showDialog = false
                                    }
                                }) {
                                    Text(text = "Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
