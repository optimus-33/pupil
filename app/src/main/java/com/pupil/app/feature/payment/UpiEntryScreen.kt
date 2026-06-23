package com.pupil.app.feature.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpiEntryScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var upiIdInput by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Enter UPI ID") },
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter the UPI ID (VPA) you want to pay",
                    style = MaterialTheme.typography.bodyLarge
                )

                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = {
                        upiIdInput = it
                        error = ""
                    },
                    label = { Text(text = "UPI ID (e.g. name@bank)") },
                    placeholder = { Text(text = "example@upi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (error.isNotBlank()) {
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val trimmed = upiIdInput.trim()
                        if (trimmed.isBlank()) {
                            error = "Please enter a UPI ID"
                            return@Button
                        }
                        if (!trimmed.contains("@")) {
                            error = "Invalid UPI ID. Must contain '@' (e.g. name@bank)"
                            return@Button
                        }
                        onContinue(trimmed)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}
