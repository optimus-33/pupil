package com.hrishikesh.pplr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PPLRTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    LandingPage()
                }
            }
        }
    }
}

@Composable
fun LandingPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "PPLR", style = MaterialTheme.typography.displayLarge)
        Text(text = "Personal UPI Ledger", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(onClick = { /* We will trigger UPI Intent here later */ }) {
            Text("Start New Payment")
        }
    }
}

@Composable
fun PPLRTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(), // Modern dark mode by default
        content = content
    )
}