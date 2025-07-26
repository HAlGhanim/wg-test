package com.example.wireguard_test.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Simple Tunnel implementation
class SimpleTunnel(private val name: String) : Tunnel {
    override fun getName(): String = name
    override fun onStateChange(newState: Tunnel.State) {
        // Handle state changes if needed
    }
}

// IMPORTANT: These are test values. In production, never hardcode private keys!
// The client private key should be generated and stored securely.
data class WireGuardUiState(
    val isConnected: Boolean = false,
    val privateKey: String = "aNP/gGmQjINr+iRCNNWJgxnSEPU32ut+Yd/YUJwJWXo=",
    val publicKey: String = "F3mXBDCS1jG1kPTdDzReu42W3auQb3MUEcagSSI1mkw=",
    val address: String = "10.0.0.2/32",
    val dns: String = "1.1.1.1",
    val peerPublicKey: String = "4WB1RuKGq5MRgC47vojZJStwfpOPhzYpBMfwveMtyUI=",
    val endpoint: String = "142.93.170.233:51820",
    val allowedIps: String = "0.0.0.0/0",
    val errorMessage: String = ""
)

class WireGuardViewModel(application: Application) : AndroidViewModel(application) {
    private val backend: Backend = GoBackend(application)
    private val tunnel: Tunnel = SimpleTunnel("wg0")

    private val _uiState = MutableStateFlow(WireGuardUiState())
    val uiState: StateFlow<WireGuardUiState> = _uiState.asStateFlow()

    fun generateKeyPair() {
        val keyPair = KeyPair()
        _uiState.value = _uiState.value.copy(
            privateKey = keyPair.privateKey.toBase64(),
            publicKey = keyPair.publicKey.toBase64()
        )
    }

    fun updatePrivateKey(value: String) {
        _uiState.value = _uiState.value.copy(privateKey = value)
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(address = value)
    }

    fun updateDns(value: String) {
        _uiState.value = _uiState.value.copy(dns = value)
    }

    fun updatePeerPublicKey(value: String) {
        _uiState.value = _uiState.value.copy(peerPublicKey = value)
    }

    fun updateEndpoint(value: String) {
        _uiState.value = _uiState.value.copy(endpoint = value)
    }

    fun updateAllowedIps(value: String) {
        _uiState.value = _uiState.value.copy(allowedIps = value)
    }

    fun connect() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = "")
                val config = createConfig()

                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }

                _uiState.value = _uiState.value.copy(isConnected = true)
                showToast("WireGuard tunnel connected")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}",
                    isConnected = false
                )
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }

                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    errorMessage = ""
                )
                showToast("WireGuard tunnel disconnected")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}"
                )
                e.printStackTrace()
            }
        }
    }

    private fun createConfig(): Config {
        val state = _uiState.value
        val builder = Config.Builder()

        // Interface configuration
        val interfaceBuilder = Interface.Builder()

        if (state.privateKey.isNotEmpty()) {
            interfaceBuilder.parsePrivateKey(state.privateKey)
        }

        state.address.split(",").forEach { addr ->
            if (addr.trim().isNotEmpty()) {
                interfaceBuilder.parseAddresses(addr.trim())
            }
        }

        state.dns.split(",").forEach { dns ->
            if (dns.trim().isNotEmpty()) {
                interfaceBuilder.parseDnsServers(dns.trim())
            }
        }

        interfaceBuilder.parseListenPort("51820")
        builder.setInterface(interfaceBuilder.build())

        // Peer configuration
        val peerBuilder = Peer.Builder()

        if (state.peerPublicKey.isNotEmpty()) {
            peerBuilder.parsePublicKey(state.peerPublicKey)
        }

        if (state.endpoint.isNotEmpty()) {
            peerBuilder.parseEndpoint(state.endpoint)
        }

        state.allowedIps.split(",").forEach { ip ->
            if (ip.trim().isNotEmpty()) {
                peerBuilder.parseAllowedIPs(ip.trim())
            }
        }

        peerBuilder.parsePersistentKeepalive("25")
        builder.addPeer(peerBuilder.build())

        return builder.build()
    }

    private fun showToast(message: String) {
        Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
    }
}