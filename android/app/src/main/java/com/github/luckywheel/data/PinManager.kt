package com.github.luckywheel.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.github.luckywheel.domain.Clock
import com.github.luckywheel.domain.Lockout
import com.github.luckywheel.domain.PinHasher
import com.github.luckywheel.domain.PinPolicy
import com.github.luckywheel.domain.afterFailure
import com.github.luckywheel.domain.remainingSeconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

sealed interface PinCheck {
    data object Ok : PinCheck
    data class Wrong(val attemptsLeft: Int) : PinCheck
    data class Locked(val seconds: Int) : PinCheck
}

/** Salted PIN storage and the wrong-attempt lock (ST-02..ST-05). The lock is stored, so it survives restarts. */
class PinManager(
    private val dataStore: DataStore<Preferences>,
    private val hasher: PinHasher,
    private val clock: Clock,
) {
    val hasPin: Flow<Boolean> = dataStore.safeData().map { it[PrefKeys.pinHash] != null }

    suspend fun createPin(pin: String) = storePin(pin)

    suspend fun changePin(newPin: String) = storePin(newPin)

    suspend fun check(pin: String): PinCheck {
        val prefs = dataStore.safeData().first()
        val lock = prefs.lockout()
        val now = clock.nowMs()
        lock.remainingSeconds(now).let { if (it > 0) return PinCheck.Locked(it) }
        if (verify(prefs, pin)) {
            dataStore.edit { it[PrefKeys.failedPinAttempts] = 0 }
            return PinCheck.Ok
        }
        val next = lock.afterFailure(now)
        dataStore.edit {
            it[PrefKeys.failedPinAttempts] = next.failedAttempts
            it[PrefKeys.pinLockedUntil] = next.lockedUntilMs
        }
        val locked = next.remainingSeconds(now)
        return if (locked > 0) PinCheck.Locked(locked) else PinCheck.Wrong(PinPolicy.MAX_ATTEMPTS - next.failedAttempts)
    }

    suspend fun lockedSeconds(): Int = dataStore.safeData().first().lockout().remainingSeconds(clock.nowMs())

    /** For changing the PIN from inside Settings; does not count towards the lock. */
    suspend fun matchesCurrent(pin: String): Boolean = verify(dataStore.safeData().first(), pin)

    private suspend fun verify(prefs: Preferences, pin: String): Boolean {
        val hash = prefs[PrefKeys.pinHash] ?: return false
        val salt = prefs[PrefKeys.pinSalt] ?: return false
        return withContext(Dispatchers.Default) { hasher.verify(pin, salt, hash) }
    }

    private suspend fun storePin(pin: String) {
        require(pin.length == PinPolicy.LENGTH && pin.all(Char::isDigit)) { "PIN must be ${PinPolicy.LENGTH} digits" }
        val salt = hasher.newSalt()
        val hash = withContext(Dispatchers.Default) { hasher.hash(pin, salt) }
        dataStore.edit {
            it[PrefKeys.pinSalt] = salt
            it[PrefKeys.pinHash] = hash
            it[PrefKeys.failedPinAttempts] = 0
            it[PrefKeys.pinLockedUntil] = 0L
        }
    }

    private fun Preferences.lockout() = Lockout(this[PrefKeys.failedPinAttempts] ?: 0, this[PrefKeys.pinLockedUntil] ?: 0L)
}
