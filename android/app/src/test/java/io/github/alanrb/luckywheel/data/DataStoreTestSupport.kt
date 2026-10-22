package io.github.alanrb.luckywheel.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.alanrb.luckywheel.domain.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File

/** A preferences DataStore on [file] whose scope can be cancelled to simulate an app restart. */
class TestDataStore(private val file: File) {
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var store: DataStore<Preferences> = open()
        private set

    private fun open() = PreferenceDataStoreFactory.create(scope = scope) { file }

    fun restart(): DataStore<Preferences> {
        scope.cancel()
        Thread.sleep(50)
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        store = open()
        return store
    }

    fun close() = scope.cancel()
}

class FakeClock(var now: Long = 1_000_000L, var elapsed: Long = 50_000L) : Clock {
    override fun nowMs() = now
    override fun elapsedMs() = elapsed
    fun advance(ms: Long) {
        now += ms
        elapsed += ms
    }
}
