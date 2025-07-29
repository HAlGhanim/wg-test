package com.example.wireguard_test.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wireguard_test.viewmodels.WireGuardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireGuardConfigScreen(
    viewModel: WireGuardViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Info card about test configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Default test configuration loaded. These values work with the test server at 142.93.170.233",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Interface Configuration
            Text(
                text = "Interface Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Private Key
            OutlinedTextField(
                value = uiState.privateKey,
                onValueChange = { viewModel.updatePrivateKey(it) },
                label = { Text("Private Key") },
                placeholder = { Text("Generate or enter your private key") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected,
                trailingIcon = {
                    TextButton(
                        onClick = { viewModel.generateKeyPair() },
                        enabled = !uiState.isConnected
                    ) {
                        Text("Generate")
                    }
                }
            )

            // Show public key if generated
            if (uiState.publicKey.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "Your Public Key:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = uiState.publicKey,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address
            OutlinedTextField(
                value = uiState.address,
                onValueChange = { viewModel.updateAddress(it) },
                label = { Text("Address (CIDR)") },
                placeholder = { Text("e.g., 10.0.0.2/32") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // DNS
            OutlinedTextField(
                value = uiState.dns,
                onValueChange = { viewModel.updateDns(it) },
                label = { Text("DNS Servers") },
                placeholder = { Text("e.g., 1.1.1.1, 8.8.8.8") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Peer Configuration
            Text(
                text = "Peer Configuration",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Peer Public Key
            OutlinedTextField(
                value = uiState.peerPublicKey,
                onValueChange = { viewModel.updatePeerPublicKey(it) },
                label = { Text("Peer Public Key") },
                placeholder = { Text("Server's base64 public key") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Endpoint
            OutlinedTextField(
                value = uiState.endpoint,
                onValueChange = { viewModel.updateEndpoint(it) },
                label = { Text("Endpoint") },
                placeholder = { Text("e.g., vpn.example.com:51820") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Allowed IPs
            OutlinedTextField(
                value = uiState.allowedIps,
                onValueChange = { viewModel.updateAllowedIps(it) },
                label = { Text("Allowed IPs") },
                placeholder = { Text("e.g., 0.0.0.0/0") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isConnected
            )

            if (!uiState.isConnected) {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Reset to defaults button
                    OutlinedButton(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Text("Reset to Test Config")
                    }

                    // Save button
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    ) {
                        Text("Save Configuration")
                    }
                }
            }
        }
    }
}