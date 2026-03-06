package com.algotrader.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: TradingViewModel) {
    val creds = viewModel.getCredentials()

    var apiKey by remember { mutableStateOf(creds.apiKey) }
    var apiSecret by remember { mutableStateOf(creds.apiSecret) }
    var host by remember { mutableStateOf(creds.openDHost) }
    var port by remember { mutableStateOf(creds.openDPort.toString()) }
    var saved by remember { mutableStateOf(false) }
    var portError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Moomoo / Futu OpenD API",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it; saved = false },
            label = { Text("API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = apiSecret,
            onValueChange = { apiSecret = it; saved = false },
            label = { Text("API Secret") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            value = host,
            onValueChange = { host = it; saved = false },
            label = { Text("OpenD Host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = port,
            onValueChange = { port = it; saved = false; portError = false },
            label = { Text("OpenD Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = portError,
            supportingText = if (portError) ({ Text("Invalid port number") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val portInt = port.toIntOrNull()
                if (portInt == null) {
                    portError = true
                } else {
                    viewModel.saveCredentials(apiKey, apiSecret, host, portInt)
                    saved = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
        if (saved) {
            Text(
                text = "Settings saved.",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
