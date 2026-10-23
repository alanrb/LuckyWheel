package io.github.alanrb.luckywheel.ui

import android.os.Looper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
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
import io.github.alanrb.luckywheel.ui.pin.CREATE_PIN_TAG
import io.github.alanrb.luckywheel.ui.pin.PIN_ENTRY_TAG
import io.github.alanrb.luckywheel.ui.pin.pinKeyTag
import io.github.alanrb.luckywheel.ui.settings.BACK_TO_WHEEL_TAG
import io.github.alanrb.luckywheel.ui.settings.SETTINGS_IDLE_TIMEOUT_MS
import io.github.alanrb.luckywheel.ui.settings.SETTINGS_TAG
import io.github.alanrb.luckywheel.ui.theme.LuckyWheelTheme
import io.github.alanrb.luckywheel.ui.wheel.WheelTestTags
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class PinFlowTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var ds: TestDataStore
    private lateinit var container: AppContainer

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        val uiClock = object : Clock {
            override fun nowMs() = compose.mainClock.currentTime
            override fun elapsedMs() = compose.mainClock.currentTime
        }
        container = AppContainer(
            prizeRepository = PrizeRepository(db),
            settingsRepository = SettingsRepository(ds.store),
            pinManager = PinManager(ds.store, PinHasher(iterations = 1_000), FakeClock()),
            imageStore = ImageStore(context),
            random = SeededRandomSource(5),
            clock = uiClock,
            sounds = NoSound,
        )
        runBlocking { container.seedPrizesIfEmpty(context) }
        compose.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                LuckyWheelTheme { AppNavHost() }
            }
        }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
    }

    /** One frame, any due main-looper work (VM delays), and Compose idle. */
    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))
        compose.waitForIdle()
    }

    private fun exists(tag: String) = compose.onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun awaitTag(tag: String) = awaitCondition { exists(tag) }

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

    private fun typePin(pin: String) {
        pin.forEach {
            compose.onNodeWithTag(pinKeyTag(it.toString())).performClick()
            settle()
        }
        // Wait for the short submit pause and the hash check.
        repeat(6) { settle() }
    }

    private fun createPin() {
        awaitTag(CREATE_PIN_TAG)
        typePin("1234")
        compose.onNodeWithText("Nhập lại để xác nhận").assertExists()
        typePin("1234")
        awaitTag(WheelTestTags.SPIN_BUTTON)
        // Let the navigation cross-fade finish.
        compose.mainClock.advanceTimeBy(1_000)
        settle()
    }

    private fun holdCorner() {
        // The corner only works once the wheel has loaded and is idle; QUAY is enabled at the same moment.
        awaitCondition { compose.onAllNodes(androidx.compose.ui.test.hasTestTag(WheelTestTags.SPIN_BUTTON) and androidx.compose.ui.test.isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(3_100)
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { up() }
        awaitTag(PIN_ENTRY_TAG)
    }

    @Test
    fun firstLaunch_createsPin_thenShowsWheel() {
        createPin()
        compose.onNodeWithTag(CREATE_PIN_TAG).assertDoesNotExist()
    }

    @Test
    fun createPin_mismatch_restarts() {
        awaitTag(CREATE_PIN_TAG)
        typePin("1234")
        typePin("5678")
        awaitCondition { compose.onAllNodes(androidx.compose.ui.test.hasText("Hai lần nhập không khớp. Vui lòng tạo lại.")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nhập mã PIN mới").assertExists()
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertDoesNotExist()
    }

    @Test
    fun settingsBlocked_withoutCorrectPin() {
        createPin()
        holdCorner()
        typePin("0000")
        awaitCondition { compose.onAllNodes(androidx.compose.ui.test.hasText("Sai mã PIN. Còn 4 lần thử.")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(SETTINGS_TAG).assertDoesNotExist()

        typePin("1234")
        awaitTag(SETTINGS_TAG)
        compose.onNodeWithTag(PIN_ENTRY_TAG).assertDoesNotExist()
    }

    @Test
    fun cancelPin_returnsToWheel() {
        createPin()
        holdCorner()
        compose.onNodeWithTag(io.github.alanrb.luckywheel.ui.pin.PIN_CANCEL_TAG).performClick()
        awaitCondition { !exists(PIN_ENTRY_TAG) }
        compose.onNodeWithTag(SETTINGS_TAG).assertDoesNotExist()
    }

    @Test
    fun settings_backAndIdleTimeoutReturnToWheel() {
        createPin()
        holdCorner()
        typePin("1234")
        awaitTag(SETTINGS_TAG)
        compose.onNodeWithTag(BACK_TO_WHEEL_TAG).performClick()
        awaitCondition { !exists(SETTINGS_TAG) }
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertExists()

        holdCorner()
        typePin("1234")
        awaitTag(SETTINGS_TAG)
        compose.mainClock.advanceTimeBy(SETTINGS_IDLE_TIMEOUT_MS + 1_000)
        awaitCondition { !exists(SETTINGS_TAG) }
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertExists()
    }
}
