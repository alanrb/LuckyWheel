package io.github.alanrb.luckywheel.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.alanrb.luckywheel.ui.wheel.WheelRoute

object Routes {
    const val WHEEL = "wheel"
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.WHEEL) {
        composable(Routes.WHEEL) { WheelRoute() }
    }
}
