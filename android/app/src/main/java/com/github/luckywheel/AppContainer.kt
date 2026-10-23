package com.github.luckywheel

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.github.luckywheel.data.AppDatabase
import com.github.luckywheel.data.ImageStore
import com.github.luckywheel.data.PinManager
import com.github.luckywheel.data.PrizeRepository
import com.github.luckywheel.data.SettingsRepository
import com.github.luckywheel.data.SystemClock
import com.github.luckywheel.data.seedPrizes
import com.github.luckywheel.domain.Clock
import com.github.luckywheel.domain.PinHasher
import com.github.luckywheel.domain.RandomSource
import com.github.luckywheel.domain.SecureRandomSource
import com.github.luckywheel.sound.SoundEffects
import com.github.luckywheel.sound.SoundPoolEffects
import com.github.luckywheel.ui.pin.CreatePinViewModel
import com.github.luckywheel.ui.pin.PinEntryViewModel
import com.github.luckywheel.ui.settings.SettingsGate
import com.github.luckywheel.ui.settings.SettingsViewModel
import com.github.luckywheel.ui.wheel.WheelViewModel

/** Manual dependency injection: one instance per process, swapped for fakes in tests. */
class AppContainer(
    val prizeRepository: PrizeRepository,
    val settingsRepository: SettingsRepository,
    val pinManager: PinManager,
    val imageStore: ImageStore,
    val random: RandomSource,
    val clock: Clock,
    val sounds: SoundEffects,
) {
    val wheelViewModelFactory = viewModelFactory {
        initializer { WheelViewModel(prizeRepository, settingsRepository, random, clock, createSavedStateHandle()) }
    }
    val pinEntryViewModelFactory = viewModelFactory { initializer { PinEntryViewModel(pinManager) } }
    val createPinViewModelFactory = viewModelFactory { initializer { CreatePinViewModel(pinManager) } }
    /** In-memory only: Settings needs the PIN again after a restart. */
    val settingsGate = SettingsGate()

    val settingsViewModelFactory = viewModelFactory {
        initializer { SettingsViewModel(prizeRepository, settingsRepository, pinManager, imageStore) }
    }

    suspend fun seedPrizesIfEmpty(context: Context) {
        prizeRepository.seedIfEmpty(
            seedPrizes(
                context.getString(R.string.seed_fallback),
                context.getString(R.string.seed_discount),
                context.getString(R.string.seed_coffee),
                context.getString(R.string.seed_tumbler),
            ),
        )
    }

    companion object {
        fun create(context: Context): AppContainer {
            val app = context.applicationContext
            val dataStore = PreferenceDataStoreFactory.create { app.preferencesDataStoreFile("settings") }
            return AppContainer(
                prizeRepository = PrizeRepository(AppDatabase.create(app)),
                settingsRepository = SettingsRepository(dataStore),
                pinManager = PinManager(dataStore, PinHasher(), SystemClock),
                imageStore = ImageStore(app),
                random = SecureRandomSource(),
                clock = SystemClock,
                sounds = SoundPoolEffects(app),
            )
        }
    }
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("AppContainer not provided") }
