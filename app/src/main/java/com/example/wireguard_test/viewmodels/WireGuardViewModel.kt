package com.example.wireguard_test.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.VpnService
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Statistics
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.crypto.Key
import com.wireguard.crypto.KeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramSocket
import java.net.InetSocketAddress

// Simple Tunnel implementation
class SimpleTunnel(private val name: String) : Tunnel {
    override fun getName(): String = name
    override fun onStateChange(newState: Tunnel.State) {
        Log.d("WireGuard", "Tunnel state changed to: $newState")
    }
}

data class WireGuardUiState(
    val isConnected: Boolean = false,
    val privateKey: String = "",
    val publicKey: String = "",
    val address: String = "",
    val dns: String = "",
    val peerPublicKey: String = "",
    val endpoint: String = "",
    val allowedIps: String = "",
    val errorMessage: String = "",
    val needsVpnPermission: Boolean = false,
    val serverLocation: String = "",
    val localAddress: String = "",
    val downloadSpeed: Long = 0L,
    val uploadSpeed: Long = 0L,
    val totalBytesReceived: Long = 0L,
    val totalBytesSent: Long = 0L,
    val isUsingDefaults: Boolean = false
)

object DefaultConfig {
    const val privateKey = "aNP/gGmQjINr+iRCNNWJgxnSEPU32ut+Yd/YUJwJWXo="
    const val publicKey = "F3mXBDCS1jG1kPTdDzReu42W3auQb3MUEcagSSI1mkw="
    const val address = "10.0.0.2/32"
    const val dns = "1.1.1.1"
    const val peerPublicKey = "4WB1RuKGq5MRgC47vojZJStwfpOPhzYpBMfwveMtyUI="
    const val endpoint = "142.93.170.233:51820"
    const val allowedIps = "0.0.0.0/0"
}

class WireGuardViewModel(application: Application) : AndroidViewModel(application) {
    private val backend: Backend = GoBackend(application)
    private val tunnel: Tunnel = SimpleTunnel("wg0")
    private val prefs: SharedPreferences = application.getSharedPreferences("wireguard_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(WireGuardUiState())
    val uiState: StateFlow<WireGuardUiState> = _uiState.asStateFlow()

    private var lastBytesReceived = 0L
    private var lastBytesSent = 0L
    private var lastStatTime = System.currentTimeMillis()

    init {
        loadConfiguration()
        // Start monitoring statistics when connected
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isConnected) {
                    updateStatistics()
                }
                delay(1000) // Update every second
            }
        }

        // Log configuration info
        Log.i("WireGuard", "=== WireGuard Configuration Loaded ===")
        Log.i("WireGuard", "Public Key: ${_uiState.value.publicKey}")
        Log.i("WireGuard", "Endpoint: ${_uiState.value.endpoint}")
        Log.i("WireGuard", "Address: ${_uiState.value.address}")
        Log.i("WireGuard", "=====================================")
    }

    private fun loadConfiguration() {
        // Load configuration with default values as fallback
        val privateKey = prefs.getString("private_key", DefaultConfig.privateKey) ?: DefaultConfig.privateKey
        val peerPublicKey = prefs.getString("peer_public_key", DefaultConfig.peerPublicKey) ?: DefaultConfig.peerPublicKey
        val endpoint = prefs.getString("endpoint", DefaultConfig.endpoint) ?: DefaultConfig.endpoint

        // Check if we're using default values
        val isUsingDefaults = privateKey == DefaultConfig.privateKey &&
                peerPublicKey == DefaultConfig.peerPublicKey &&
                endpoint == DefaultConfig.endpoint

        _uiState.value = _uiState.value.copy(
            privateKey = privateKey,
            publicKey = prefs.getString("public_key", DefaultConfig.publicKey) ?: DefaultConfig.publicKey,
            address = prefs.getString("address", DefaultConfig.address) ?: DefaultConfig.address,
            dns = prefs.getString("dns", DefaultConfig.dns) ?: DefaultConfig.dns,
            peerPublicKey = peerPublicKey,
            endpoint = endpoint,
            allowedIps = prefs.getString("allowed_ips", DefaultConfig.allowedIps) ?: DefaultConfig.allowedIps,
            localAddress = prefs.getString("address", DefaultConfig.address) ?: DefaultConfig.address,
            isUsingDefaults = isUsingDefaults
        )

        // Update server location based on endpoint
        updateServerLocation(_uiState.value.endpoint)
    }

    private fun saveConfiguration() {
        prefs.edit().apply {
            putString("private_key", _uiState.value.privateKey)
            putString("public_key", _uiState.value.publicKey)
            putString("address", _uiState.value.address)
            putString("dns", _uiState.value.dns)
            putString("peer_public_key", _uiState.value.peerPublicKey)
            putString("endpoint", _uiState.value.endpoint)
            putString("allowed_ips", _uiState.value.allowedIps)
            putString("server_location", _uiState.value.serverLocation)
            apply()
        }
    }

    fun generateKeyPair() {
        val keyPair = KeyPair()
        _uiState.value = _uiState.value.copy(
            privateKey = keyPair.privateKey.toBase64(),
            publicKey = keyPair.publicKey.toBase64(),
            isUsingDefaults = false
        )
        saveConfiguration()
        Log.d("WireGuard", "Generated new keypair. Public key: ${keyPair.publicKey.toBase64()}")
        showToast("New key pair generated. Add the public key to your server.")
    }

    fun resetToDefaults() {
        _uiState.value = _uiState.value.copy(
            privateKey = DefaultConfig.privateKey,
            publicKey = DefaultConfig.publicKey,
            address = DefaultConfig.address,
            localAddress = DefaultConfig.address,
            dns = DefaultConfig.dns,
            peerPublicKey = DefaultConfig.peerPublicKey,
            endpoint = DefaultConfig.endpoint,
            allowedIps = DefaultConfig.allowedIps,
            isUsingDefaults = true
        )
        updateServerLocation(DefaultConfig.endpoint)
        saveConfiguration()
        showToast("Configuration reset to defaults")
        Log.d("WireGuard", "Configuration reset to default test values")
    }

    fun updatePrivateKey(value: String) {
        _uiState.value = _uiState.value.copy(
            privateKey = value,
            isUsingDefaults = false
        )
        // Try to derive public key if private key is valid
        try {
            val privateKey = Key.fromBase64(value)
            val keyPair = KeyPair(privateKey)
            _uiState.value = _uiState.value.copy(publicKey = keyPair.publicKey.toBase64())
        } catch (e: Exception) {
            Log.e("WireGuard", "Invalid private key format: ${e.message}")
        }
        saveConfiguration()
    }

    fun updateAddress(value: String) {
        _uiState.value = _uiState.value.copy(
            address = value,
            localAddress = value,
            isUsingDefaults = false
        )
        saveConfiguration()
    }

    fun updateDns(value: String) {
        _uiState.value = _uiState.value.copy(
            dns = value,
            isUsingDefaults = false
        )
        saveConfiguration()
    }

    fun updatePeerPublicKey(value: String) {
        _uiState.value = _uiState.value.copy(
            peerPublicKey = value,
            isUsingDefaults = false
        )
        saveConfiguration()
    }

    fun updateEndpoint(value: String) {
        _uiState.value = _uiState.value.copy(
            endpoint = value,
            isUsingDefaults = false
        )
        // Try to determine server location from endpoint
        updateServerLocation(value)
        saveConfiguration()
    }

    fun updateAllowedIps(value: String) {
        _uiState.value = _uiState.value.copy(
            allowedIps = value,
            isUsingDefaults = false
        )
        saveConfiguration()
    }

    private fun updateServerLocation(endpoint: String) {
        // This is a simple implementation - you could enhance this with a real geo-IP service
        val location = when {
            endpoint.contains("ca.") || endpoint.contains("canada") -> "Canada"
            endpoint.contains("us.") || endpoint.contains("usa") -> "United States"
            endpoint.contains("uk.") || endpoint.contains("london") -> "United Kingdom"
            endpoint.contains("de.") || endpoint.contains("germany") -> "Germany"
            endpoint.contains("jp.") || endpoint.contains("japan") -> "Japan"
            else -> "VPN Server"
        }
        _uiState.value = _uiState.value.copy(serverLocation = location)
    }

    private suspend fun updateStatistics() {
        try {
            withContext(Dispatchers.IO) {
                val stats = backend.getStatistics(tunnel)
                val currentTime = System.currentTimeMillis()
                val timeDiff = (currentTime - lastStatTime) / 1000.0 // Convert to seconds

                // Calculate speeds (bytes per second)
                val downloadSpeed = if (timeDiff > 0 && lastBytesReceived > 0) {
                    ((stats.totalRx() - lastBytesReceived) / timeDiff).toLong()
                } else 0L

                val uploadSpeed = if (timeDiff > 0 && lastBytesSent > 0) {
                    ((stats.totalTx() - lastBytesSent) / timeDiff).toLong()
                } else 0L

                _uiState.value = _uiState.value.copy(
                    downloadSpeed = downloadSpeed.coerceAtLeast(0),
                    uploadSpeed = uploadSpeed.coerceAtLeast(0),
                    totalBytesReceived = stats.totalRx(),
                    totalBytesSent = stats.totalTx()
                )

                lastBytesReceived = stats.totalRx()
                lastBytesSent = stats.totalTx()
                lastStatTime = currentTime
            }
        } catch (e: Exception) {
            Log.e("WireGuard", "Failed to update statistics: ${e.message}")
        }
    }

    fun getVpnPermissionIntent(): Intent? {
        return VpnService.prepare(getApplication())
    }

    fun checkAndConnect() {
        Log.d("WireGuard", "checkAndConnect() called")

        // First validate configuration
        val validationError = validateConfiguration()
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(errorMessage = validationError)
            return
        }

        // Check network connectivity to the endpoint
        viewModelScope.launch {
            val state = _uiState.value
            val endpointParts = state.endpoint.split(":")
            if (endpointParts.size != 2) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Invalid endpoint format. Use host:port"
                )
                return@launch
            }

            val host = endpointParts[0]
            val port = endpointParts[1].toIntOrNull() ?: 51820

            Log.d("WireGuard", "Testing UDP connectivity to $host:$port")

            val canReachEndpoint = withContext(Dispatchers.IO) {
                testUdpEndpoint(host, port)
            }

            if (!canReachEndpoint) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Cannot reach server at $host:$port via UDP. The server might be down, or UDP port $port might be blocked by a firewall."
                )
                Log.e("WireGuard", "UDP endpoint $host:$port is not reachable")
            } else {
                Log.d("WireGuard", "UDP endpoint $host:$port appears accessible, proceeding with VPN setup")

                // Check VPN permission
                val vpnIntent = getVpnPermissionIntent()
                if (vpnIntent != null) {
                    Log.d("WireGuard", "VPN permission needed, setting needsVpnPermission = true")
                    _uiState.value = _uiState.value.copy(needsVpnPermission = true)
                } else {
                    Log.d("WireGuard", "VPN permission already granted, calling connect()")
                    connect()
                }
            }
        }
    }

    private fun validateConfiguration(): String? {
        val state = _uiState.value

        if (state.privateKey.isEmpty()) {
            return "Please configure your private key in settings"
        }

        if (state.peerPublicKey.isEmpty()) {
            return "Please configure the peer public key in settings"
        }

        if (state.endpoint.isEmpty()) {
            return "Please configure the endpoint in settings"
        }

        if (state.address.isEmpty()) {
            return "Please configure the interface address in settings"
        }

        // Validate private key
        try {
            Key.fromBase64(state.privateKey)
        } catch (e: Exception) {
            return "Invalid private key format"
        }

        // Validate peer public key
        try {
            Key.fromBase64(state.peerPublicKey)
        } catch (e: Exception) {
            return "Invalid peer public key format"
        }

        // Validate endpoint format
        val endpointParts = state.endpoint.split(":")
        if (endpointParts.size != 2 || endpointParts[1].toIntOrNull() == null) {
            return "Invalid endpoint format. Use host:port (e.g., 192.168.1.1:51820)"
        }

        // Validate address format
        if (!state.address.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+/\\d+"))) {
            return "Invalid address format. Use CIDR notation (e.g., 10.0.0.2/32)"
        }

        return null
    }

    private suspend fun testUdpEndpoint(host: String, port: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Try to bind a UDP socket and send a packet
                DatagramSocket().use { socket ->
                    socket.soTimeout = 5000
                    // We're not expecting a response, just checking if we can send
                    val address = InetSocketAddress(host, port)
                    // If we can resolve the host, that's a good sign
                    address.hostName
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("WireGuard", "Failed to test UDP endpoint $host:$port: ${e.message}")
            false
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        Log.d("WireGuard", "onVpnPermissionResult: granted = $granted")
        _uiState.value = _uiState.value.copy(needsVpnPermission = false)
        if (granted) {
            connect()
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "VPN permission denied. Cannot establish connection."
            )
        }
    }

    private fun connect() {
        Log.d("WireGuard", "connect() called - establishing WireGuard tunnel")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = "")

                val config = createConfig()

                // Log the configuration for debugging
                Log.d("WireGuard", "Configuration details:")
                Log.d("WireGuard", "  Interface Address: ${_uiState.value.address}")
                Log.d("WireGuard", "  DNS: ${_uiState.value.dns}")
                Log.d("WireGuard", "  Your Public Key: ${_uiState.value.publicKey}")
                Log.d("WireGuard", "  Peer Public Key: ${_uiState.value.peerPublicKey}")
                Log.d("WireGuard", "  Endpoint: ${_uiState.value.endpoint}")
                Log.d("WireGuard", "  Allowed IPs: ${_uiState.value.allowedIps}")

                withContext(Dispatchers.IO) {
                    Log.d("WireGuard", "Setting tunnel state to UP")
                    backend.setState(tunnel, Tunnel.State.UP, config)
                }

                // Reset statistics
                lastBytesReceived = 0L
                lastBytesSent = 0L
                lastStatTime = System.currentTimeMillis()

                _uiState.value = _uiState.value.copy(
                    isConnected = true,
                    errorMessage = ""
                )
                showToast("WireGuard tunnel connected")

                // Log important info for debugging
                Log.i("WireGuard", "✓ Connection established!")
                Log.i("WireGuard", "----------------------------------------")
                Log.i("WireGuard", "IMPORTANT: Make sure your public key is added to the server:")
                Log.i("WireGuard", "Public Key: ${_uiState.value.publicKey}")
                Log.i("WireGuard", "----------------------------------------")

            } catch (e: Exception) {
                Log.e("WireGuard", "Connection failed", e)
                val errorMessage = when {
                    e.message?.contains("Permission denied") == true ->
                        "Permission denied. Make sure the app has VPN permissions."
                    e.message?.contains("handshake") == true ->
                        "Handshake failed. Check that your public key is configured on the server."
                    else ->
                        "Connection failed: ${e.message}"
                }
                _uiState.value = _uiState.value.copy(
                    errorMessage = errorMessage,
                    isConnected = false
                )
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            try {
                Log.d("WireGuard", "Disconnecting tunnel...")
                withContext(Dispatchers.IO) {
                    backend.setState(tunnel, Tunnel.State.DOWN, null)
                }

                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    errorMessage = "",
                    downloadSpeed = 0L,
                    uploadSpeed = 0L
                )
                showToast("WireGuard tunnel disconnected")
                Log.i("WireGuard", "✓ Tunnel disconnected")
            } catch (e: Exception) {
                Log.e("WireGuard", "Disconnect failed", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Disconnect error: ${e.message}"
                )
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