package com.example.wireguard_test.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.wireguard_test.ui.screens.WireGuardConfigScreen
import com.example.wireguard_test.ui.screens.WireGuardScreen
import com.example.wireguard_test.viewmodels.WireGuardViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WireGuardApp(
    viewModel: WireGuardViewModel,
    onRequestVpnPermission: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable(
            "main",
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            WireGuardScreen(
                viewModel = viewModel,
                onRequestVpnPermission = onRequestVpnPermission,
                onNavigateToConfig = {
                    navController.navigate("config")
                }
            )
        }

        composable(
            "config",
            enterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            }
        ) {
            WireGuardConfigScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}