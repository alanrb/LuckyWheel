package io.github.alanrb.luckywheel.ui

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alanrb.luckywheel.AppContainer
import io.github.alanrb.luckywheel.LocalAppContainer
import io.github.alanrb.luckywheel.data.AppDatabase
import io.github.alanrb.luckywheel.data.FakeClock
import io.github.alanrb.luckywheel.data.ImageStore
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.data.TestDataStore
import io.github.alanrb.luckywheel.domain.Clock
import io.github.alanrb.luckywheel.domain.PinHasher
import io.github.alanrb.luckywheel.domain.SeededRandomSource
import io.github.alanrb.luckywheel.sound.NoSound
import io.github.alanrb.luckywheel.ui.pin.PIN_ENTRY_TAG
import io.github.alanrb.luckywheel.ui.pin.pinKeyTag
import io.github.alanrb.luckywheel.ui.settings.SETTINGS_TAG
import io.github.alanrb.luckywheel.ui.settings.SettingsRoute
import io.github.alanrb.luckywheel.ui.theme.LuckyWheelTheme
import io.github.alanrb.luckywheel.ui.wheel.WheelTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** Hard priority 3 / ST-01: Settings is never shown without the PIN, including after a restart. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class SettingsGateTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var ds: TestDataStore
    private lateinit var container: AppContainer
    private var exits = 0

    private val uiClock = object : Clock {
        override fun nowMs() = compose.mainClock.currentTime
        override fun elapsedMs() = compose.mainClock.currentTime
    }

    /** A new container is what a new process gets: same storage, nothing held in memory. */
    private fun newContainer() = AppContainer(
        prizeRepository = PrizeRepository(db),
        settingsRepository = SettingsRepository(ds.store),
        pinManager = PinManager(ds.store, PinHasher(iterations = 1_000), FakeClock()),
        imageStore = ImageStore(ApplicationProvider.getApplicationContext()),
        random = SeededRandomSource(5),
        clock = uiClock,
        sounds = NoSound,
    )

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        container = newContainer()
        runBlocking {
            container.seedPrizesIfEmpty(context)
            container.pinManager.createPin("1234")
        }
    }

    private fun showApp() = compose.setContent {
        CompositionLocalProvider(LocalAppContainer provides container) {
            LuckyWheelTheme { AppNavHost() }
        }
    }

    /** What a restored back stack does after process death: compose the Settings destination directly. */
    private fun showRestoredSettingsDestination() = compose.setContent {
        CompositionLocalProvider(LocalAppContainer provides container) {
            LuckyWheelTheme { SettingsRoute(onExit = { exits++ }) }
        }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        compose.waitForIdle()
    }

    private fun exists(tag: String) = compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 8_000
        while (true) {
            settle()
            if (condition()) {
                repeat(2) { settle() }
                return
            }
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            Thread.sleep(10)
        }
    }

    private fun unlockSettings() {
        awaitCondition { exists(WheelTestTags.SPIN_BUTTON) }
        // The corner only works once the wheel has loaded and is idle; QUAY is enabled at the same moment.
        awaitCondition { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(WheelTestTags.SPIN_BUTTON) and androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(3_100)
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { up() }
        awaitCondition { exists(PIN_ENTRY_TAG) }
        "1234".forEach {
            compose.onNodeWithTag(pinKeyTag(it.toString())).performClick()
            settle()
        }
        awaitCondition { exists(SETTINGS_TAG) }
    }

    private fun letTransitionsFinish() {
        compose.mainClock.advanceTimeBy(1_000)
        repeat(3) { settle() }
    }

    @Test
    fun restoredSettingsDestination_inNewProcess_exitsWithoutShowingSettings() {
        // A new process has a fresh container whose gate was never unlocked.
        showRestoredSettingsDestination()
        repeat(5) { settle() }
        assertFalse("Settings shown without the PIN", exists(SETTINGS_TAG))
        assertTrue("Settings destination did not leave", exits >= 1)
    }

    @Test
    fun settingsDestination_afterPinThisProcess_isShown() {
        container.settingsGate.unlock()
        showRestoredSettingsDestination()
        awaitCondition { exists(SETTINGS_TAG) }
        assertTrue(exits == 0)
    }

    @Test
    fun leavingTheApp_locksSettings() {
        showApp()
        unlockSettings()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        awaitCondition { exists(WheelTestTags.SPIN_BUTTON) }
        letTransitionsFinish()
        assertFalse("Settings stayed open after the app went to the background", exists(SETTINGS_TAG))
    }
}
