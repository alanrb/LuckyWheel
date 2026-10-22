package io.github.alanrb.luckywheel.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.alanrb.luckywheel.domain.ThemeId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Owner settings (PRD §7.2, minus the PIN keys, which [PinManager] owns). */
data class AppSettings(
    val themeId: ThemeId = ThemeId.DEFAULT,
    val logoPath: String? = null,
    val customBackgroundPath: String? = null,
    val showThemeDecorations: Boolean = true,
    val soundEnabled: Boolean = true,
    val resultDisplaySeconds: Int = DEFAULT_RESULT_SECONDS,
    val keepScreenOn: Boolean = true,
) {
    companion object {
        const val DEFAULT_RESULT_SECONDS = 10
        val RESULT_SECONDS_RANGE = 3..60
    }
}

/** DataStore keys, named as in PRD §7.2. */
internal object PrefKeys {
    val pinHash = stringPreferencesKey("pinHash")
    val pinSalt = stringPreferencesKey("pinSalt")
    val failedPinAttempts = intPreferencesKey("failedPinAttempts")
    val pinLockedUntil = longPreferencesKey("pinLockedUntil")
    val themeId = stringPreferencesKey("themeId")
    val logoPath = stringPreferencesKey("logoPath")
    val customBackgroundPath = stringPreferencesKey("customBackgroundPath")
    val showThemeDecorations = booleanPreferencesKey("showThemeDecorations")
    val soundEnabled = booleanPreferencesKey("soundEnabled")
    val resultDisplaySeconds = intPreferencesKey("resultDisplaySeconds")
    val keepScreenOn = booleanPreferencesKey("keepScreenOn")
}

internal fun DataStore<Preferences>.safeData(): Flow<Preferences> =
    data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.safeData().map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[PrefKeys.themeId] = s.themeId.name
            prefs.putOrRemove(PrefKeys.logoPath, s.logoPath)
            prefs.putOrRemove(PrefKeys.customBackgroundPath, s.customBackgroundPath)
            prefs[PrefKeys.showThemeDecorations] = s.showThemeDecorations
            prefs[PrefKeys.soundEnabled] = s.soundEnabled
            prefs[PrefKeys.resultDisplaySeconds] = s.resultDisplaySeconds.coerceIn(AppSettings.RESULT_SECONDS_RANGE)
            prefs[PrefKeys.keepScreenOn] = s.keepScreenOn
        }
    }

    private fun Preferences.toSettings() = AppSettings(
        themeId = ThemeId.entries.firstOrNull { it.name == this[PrefKeys.themeId] } ?: ThemeId.DEFAULT,
        logoPath = this[PrefKeys.logoPath],
        customBackgroundPath = this[PrefKeys.customBackgroundPath],
        showThemeDecorations = this[PrefKeys.showThemeDecorations] ?: true,
        soundEnabled = this[PrefKeys.soundEnabled] ?: true,
        resultDisplaySeconds = (this[PrefKeys.resultDisplaySeconds] ?: AppSettings.DEFAULT_RESULT_SECONDS)
            .coerceIn(AppSettings.RESULT_SECONDS_RANGE),
        keepScreenOn = this[PrefKeys.keepScreenOn] ?: true,
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.putOrRemove(
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value == null) remove(key) else this[key] = value
    }
}
