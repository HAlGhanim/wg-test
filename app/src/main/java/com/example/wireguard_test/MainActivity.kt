package com.example.wireguard_test

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.wireguard_test.ui.WireGuardApp
import com.example.wireguard_test.ui.theme.WireguardtestTheme
import com.example.wireguard_test.viewmodels.WireGuardViewModel

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: WireGuardViewModel

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // User returned from VPN permission dialog
        val granted = result.resultCode == RESULT_OK
        viewModel.onVpnPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[WireGuardViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            WireguardtestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WireGuardApp(
                        viewModel = viewModel,
                        onRequestVpnPermission = {
                            requestVpnPermission()
                        }
                    )
                }
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // Already have permission
            viewModel.onVpnPermissionResult(true)
        }
    }
}