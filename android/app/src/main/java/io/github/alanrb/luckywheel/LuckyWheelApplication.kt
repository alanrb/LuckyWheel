package io.github.alanrb.luckywheel

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LuckyWheelApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.create(this)
        // PR-08: sample prizes on first launch so the wheel is usable immediately.
        appScope.launch { container.seedPrizesIfEmpty(this@LuckyWheelApplication) }
    }
}
