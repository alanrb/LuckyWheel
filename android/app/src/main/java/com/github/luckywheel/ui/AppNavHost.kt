package com.github.luckywheel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.luckywheel.LocalAppContainer
import com.github.luckywheel.ui.pin.CreatePinRoute
import com.github.luckywheel.ui.settings.SettingsRoute
import com.github.luckywheel.ui.theme.AppColors
import com.github.luckywheel.ui.wheel.WheelRoute

object Routes {
    const val CREATE_PIN = "createPin"
    const val WHEEL = "wheel"
    const val SETTINGS = "settings"
}

/** First launch goes through PIN creation (ST-02); Settings is only reachable from the wheel's PIN prompt. */
@Composable
fun AppNavHost() {
    val container = LocalAppContainer.current
    val hasPin by container.pinManager.hasPin.collectAsStateWithLifecycle(initialValue = null)
    val known = hasPin ?: run {
        Box(Modifier.fillMaxSize().background(AppColors.CreatePinBottom))
        return
    }
    val start = rememberSaveable { if (known) Routes.WHEEL else Routes.CREATE_PIN }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start) {
        composable(Routes.CREATE_PIN) {
            CreatePinRoute(onDone = {
                nav.navigate(Routes.WHEEL) { popUpTo(Routes.CREATE_PIN) { inclusive = true } }
            })
        }
        composable(Routes.WHEEL) {
            WheelRoute(onOpenSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } })
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onExit = {
                if (!nav.popBackStack(Routes.WHEEL, inclusive = false)) {
                    nav.navigate(Routes.WHEEL) { popUpTo(0) }
                }
            })
        }
    }
}
