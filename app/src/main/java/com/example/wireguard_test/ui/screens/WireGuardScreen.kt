package com.example.wireguard_test.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wireguard_test.viewmodels.WireGuardUiState
import com.example.wireguard_test.viewmodels.WireGuardViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WireGuardScreen(
    viewModel: WireGuardViewModel,
    onRequestVpnPermission: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var connectionDuration by remember { mutableStateOf(0L) }
    var selectedTab by remember { mutableStateOf(0) }

    // Handle VPN permission request
    LaunchedEffect(uiState.needsVpnPermission) {
        if (uiState.needsVpnPermission) {
            onRequestVpnPermission()
        }
    }

    // Connection duration timer
    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            val startTime = System.currentTimeMillis()
            while (uiState.isConnected) {
                delay(1.seconds)
                connectionDuration = System.currentTimeMillis() - startTime
            }
        } else {
            connectionDuration = 0L
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5FA))
    ) {
        // World map background pattern
        WorldMapBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Connection Duration and Test Mode Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isConnected) {
                    Text(
                        text = formatDuration(connectionDuration),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                }

                if (uiState.isUsingDefaults) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "TEST MODE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Location and Status
            Text(
                text = if (uiState.isConnected) {
                    uiState.serverLocation.ifEmpty { "Connected" }
                } else {
                    "Not Connected"
                },
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF5B3FFF)
            )

            Text(
                text = getStatusText(uiState),
                fontSize = 16.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Circular Power Button
            PowerButton(
                isConnected = uiState.isConnected,
                isConnecting = uiState.needsVpnPermission,
                uiState = uiState,
                onClick = {
                    if (uiState.isConnected) {
                        viewModel.disconnect()
                    } else {
                        viewModel.checkAndConnect()
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Speed Stats
            if (uiState.isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SpeedStat(
                        icon = Icons.Default.KeyboardArrowDown,
                        value = "59.8",
                        unit = "mb/s",
                        label = "Download"
                    )

                    SpeedStat(
                        icon = Icons.Default.KeyboardArrowUp,
                        value = "19.8",
                        unit = "mb/s",
                        label = "Upload"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Navigation
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    when (index) {
                        0 -> selectedTab = 0
                        1 -> { /* Profile */ }
                        2 -> { /* Location */ }
                    }
                },
                onSettingsClick = onNavigateToConfig
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun WorldMapBackground() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val dotRadius = 2.dp.toPx()
        val spacing = 40.dp.toPx()

        for (x in 0..size.width.toInt() step spacing.toInt()) {
            for (y in 0..size.height.toInt() step spacing.toInt()) {
                drawCircle(
                    color = Color(0xFFE8E8F0),
                    radius = dotRadius,
                    center = Offset(x.toFloat(), y.toFloat())
                )
            }
        }
    }
}

@Composable
fun PowerButton(
    isConnected: Boolean,
    isConnecting: Boolean,
    uiState: WireGuardUiState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .clickable(
                enabled = !isConnecting,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Gradient Ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(if (isConnecting) rotation else 0f)
        ) {
            val strokeWidth = 8.dp.toPx()
            val gradient = Brush.sweepGradient(
                colors = if (isConnected) {
                    listOf(
                        Color(0xFF5B3FFF),
                        Color(0xFF9C27B0),
                        Color(0xFF5B3FFF)
                    )
                } else {
                    listOf(
                        Color(0xFFE0E0E0),
                        Color(0xFFBDBDBD),
                        Color(0xFFE0E0E0)
                    )
                }
            )

            drawCircle(
                brush = gradient,
                radius = size.minDimension / 2 - strokeWidth / 2,
                style = Stroke(strokeWidth)
            )
        }

        // Inner Circle
        Surface(
            modifier = Modifier
                .size(180.dp),
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (isConnected) Color(0xFF5B3FFF) else Color(0xFFBDBDBD)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your IP",
                    fontSize = 16.sp,
                    color = Color(0xFF999999)
                )

                Text(
                    text = if (isConnected) {
                        uiState.endpoint.ifEmpty { "—.—.—.—" }
                    } else {
                        "—.—.—.—"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
            }
        }
    }
}

@Composable
fun SpeedStat(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = Color(0xFF5B3FFF)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = " $unit",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFF5B3FFF)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home
            IconButton(
                onClick = { onTabSelected(0) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Profile
            IconButton(
                onClick = { onTabSelected(1) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Location
            IconButton(
                onClick = { onTabSelected(2) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            // Settings (gradient background)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF9C27B0),
                                Color(0xFFE91E63)
                            )
                        )
                    )
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun getStatusText(uiState: WireGuardUiState): String {
    return when {
        uiState.isConnected -> "Connected"
        uiState.needsVpnPermission -> "Connecting..."
        else -> "Disconnected"
    }
}

@SuppressLint("DefaultLocale")
private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return String.format("%02d:%02d:%02d", hours, minutes, secs)
}

@SuppressLint("DefaultLocale")
private fun formatSpeed(bytesPerSecond: Long): String {
    val mbps = bytesPerSecond.toDouble() / (1024 * 1024)
    return String.format("%.1f", mbps)
}