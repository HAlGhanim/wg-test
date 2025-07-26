package com.example.wireguard_test.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.wireguard_test.viewmodels.WireGuardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireGuardScreen(
    viewModel: WireGuardViewModel,
    onRequestVpnPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle VPN permission request
    LaunchedEffect(uiState.needsVpnPermission) {
        if (uiState.needsVpnPermission) {
            onRequestVpnPermission()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "WireGuard VPN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.isConnected)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.isConnected) "Connected" else "Disconnected",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (uiState.isConnected)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )

                Switch(
                    checked = uiState.isConnected,
                    onCheckedChange = {
                        if (it) viewModel.checkAndConnect() else viewModel.disconnect()
                    }
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
            placeholder = { Text("Base64 encoded private key") },
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

        Spacer(modifier = Modifier.height(32.dp))

        // Connect/Disconnect Button
        Button(
            onClick = {
                if (uiState.isConnected) viewModel.disconnect() else viewModel.checkAndConnect()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isConnected)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (uiState.isConnected) "Disconnect" else "Connect",
                modifier = Modifier.padding(8.dp)
            )
        }

        // Error message
        if (uiState.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}