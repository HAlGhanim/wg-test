package com.example.wireguard_test.ui

import androidx.compose.runtime.*
import com.example.wireguard_test.ui.screens.WireGuardScreen
import com.example.wireguard_test.viewmodels.WireGuardViewModel

@Composable
fun WireGuardApp(
    viewModel: WireGuardViewModel,
    onRequestVpnPermission: () -> Unit
) {
    WireGuardScreen(
        viewModel = viewModel,
        onRequestVpnPermission = onRequestVpnPermission
    )
}