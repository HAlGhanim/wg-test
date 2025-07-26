package com.example.wireguard_test.ui


import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wireguard_test.ui.screens.WireGuardScreen
import com.example.wireguard_test.viewmodels.WireGuardViewModel

@Composable
fun WireGuardApp() {
    val viewModel: WireGuardViewModel = viewModel()
    WireGuardScreen(viewModel = viewModel)
}