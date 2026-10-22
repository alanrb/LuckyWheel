package io.github.alanrb.luckywheel

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alanrb.luckywheel.data.AppDatabase
import io.github.alanrb.luckywheel.data.ImageStore
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.data.SystemClock
import io.github.alanrb.luckywheel.data.seedPrizes
import io.github.alanrb.luckywheel.domain.Clock
import io.github.alanrb.luckywheel.domain.PinHasher
import io.github.alanrb.luckywheel.domain.RandomSource
import io.github.alanrb.luckywheel.domain.SecureRandomSource
import io.github.alanrb.luckywheel.sound.SoundEffects
import io.github.alanrb.luckywheel.sound.SoundPoolEffects
import io.github.alanrb.luckywheel.ui.pin.CreatePinViewModel
import io.github.alanrb.luckywheel.ui.pin.PinEntryViewModel
import io.github.alanrb.luckywheel.ui.settings.SettingsViewModel
import io.github.alanrb.luckywheel.ui.wheel.WheelViewModel

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
