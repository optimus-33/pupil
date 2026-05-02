package com.pupil.app.feature.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pupil.app.core.domain.model.PaymentType
import com.pupil.app.core.ui.theme.Teal

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
    var showAppSearch by rememberSaveable { mutableStateOf(false) }

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
        val grouped = uiState.apps.groupBy { it.paymentType }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            grouped.forEach { (type, apps) ->
                item {
                    Text(
                        text = if (type == PaymentType.UPI) "UPI" else "UPI Credit Card",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                    )
                }
                items(apps) { app ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = app.displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(text = app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                            Checkbox(checked = app.enabled, onCheckedChange = { onToggleAppEnabled(app.id, it) })
                        }
                    }
                }
            }
        }

        // Add custom app dialog
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "Add custom payment app", style = MaterialTheme.typography.titleMedium)

                        // Button to search installed apps
                        Button(
                            onClick = { showAppSearch = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                            Text(text = "   Browse installed apps", modifier = Modifier.padding(start = 4.dp))
                        }

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
                                Text(text = "UPI CC")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showDialog = false }, modifier = Modifier.weight(1f)) {
                                Text(text = "Cancel")
                            }
                            Button(
                                onClick = {
                                    if (customName.isNotBlank() && customPackage.isNotBlank()) {
                                        onAddCustomApp(customName, customPackage, customType)
                                        customName = ""
                                        customPackage = ""
                                        showDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Add")
                            }
                        }
                    }
                }
            }
        }

        // App search dialog
        if (showAppSearch) {
            InstalledAppSearchDialog(
                onDismiss = { showAppSearch = false },
                onAppSelected = { displayName, packageName ->
                    customName = displayName
                    customPackage = packageName
                    showAppSearch = false
                }
            )
        }
    }
}

@Composable
private fun InstalledAppSearchDialog(
    onDismiss: () -> Unit,
    onAppSelected: (String, String) -> Unit
) {
    val context = LocalContext.current
    val allApps = remember {
        try {
            val pm = context.packageManager
            // Query apps that can handle upi://pay — shows only UPI-capable apps, zero noise
            val upiIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("upi://pay"))
            val activities = pm.queryIntentActivities(upiIntent, 0)
            activities.map {
                val appName = it.loadLabel(pm).toString()
                val packageName = it.activityInfo.packageName
                InstalledAppInfo(appName, packageName)
            }.sortedBy { it.displayName.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) {
            allApps
        } else {
            val query = searchQuery.lowercase()
            allApps.filter {
                it.displayName.lowercase().contains(query) ||
                it.packageName.lowercase().contains(query)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Search installed apps", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(text = "Search") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (filteredApps.isEmpty()) {
                    Text(
                        text = "No apps found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps.take(50)) { app ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAppSelected(app.displayName, app.packageName) },
                                tonalElevation = 1.dp,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = app.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = androidx.compose.ui.graphics.Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}

private data class InstalledAppInfo(
    val displayName: String,
    val packageName: String
)
