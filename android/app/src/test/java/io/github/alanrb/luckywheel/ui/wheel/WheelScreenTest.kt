package io.github.alanrb.luckywheel.ui.wheel

import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alanrb.luckywheel.data.AppDatabase
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.data.TestDataStore
import io.github.alanrb.luckywheel.data.seedPrizes
import io.github.alanrb.luckywheel.domain.Clock
import io.github.alanrb.luckywheel.domain.SeededRandomSource
import io.github.alanrb.luckywheel.sound.NoSound
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class WheelScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var prizes: PrizeRepository
    private lateinit var ds: TestDataStore
    /** Spin timing follows Compose's virtual clock, which the tests advance by hand. */
    private val clock = object : Clock {
        override fun nowMs() = compose.mainClock.currentTime
        override fun elapsedMs() = compose.mainClock.currentTime
    }
    private lateinit var vm: WheelViewModel
    private var cornerHolds = 0

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prizes = PrizeRepository(db)
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        runBlocking { prizes.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt")) }
        vm = WheelViewModel(prizes, SettingsRepository(ds.store), SeededRandomSource(11), clock, SavedStateHandle())
        compose.setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            WheelScreen(
                state = state,
                clock = clock,
                sounds = NoSound,
                actions = WheelActions(
                    onSpin = vm::spinWithButton,
                    onFlick = vm::spinWithFlick,
                    onSpinFinished = vm::onSpinAnimationFinished,
                    onDismissResult = vm::dismissResult,
                    onCornerHold = { cornerHolds++ },
                ),
            )
        }
        awaitCondition { vm.state.value.loaded && vm.state.value.slices.size == 4 }
    }

    /** Lets recomposition run one frame and drains the main looper. */
    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    /** Room and DataStore emit on their own threads, so poll while letting frames run. */
    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (true) {
            settle()
            if (condition()) {
                // The UI collects the new state on the next frames.
                repeat(2) { settle() }
                return
            }
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            Thread.sleep(10)
        }
    }

    private fun finishSpin() {
        compose.mainClock.advanceTimeBy(8_000)
        awaitCondition { phase() is SpinPhase.Revealed }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
    }

    private fun phase() = vm.state.value.phase

    @Test
    fun buttonSpin_showsResult() {
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        assertTrue(phase() is SpinPhase.Spinning)
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertIsNotEnabled()

        finishSpin()
        compose.onNodeWithTag(WheelTestTags.RESULT).assertExists()
    }

    @Test
    fun resultTap_dismisses() {
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        finishSpin()
        compose.onNodeWithTag(WheelTestTags.RESULT).performClick()
        settle()
        assertEquals(SpinPhase.Idle, phase())
    }

    @Test
    fun weakFlick_doesNotSpin() {
        compose.onNodeWithTag(WheelTestTags.WHEEL).performTouchInput {
            val c = center
            val r = width / 2f
            swipe(c + Offset(r * 0.6f, -r * 0.1f), c + Offset(r * 0.6f, r * 0.1f), durationMillis = 1_500)
        }
        settle()
        assertEquals(SpinPhase.Idle, phase())
    }

    @Test
    fun strongFlick_spinsInFlickDirection() {
        compose.onNodeWithTag(WheelTestTags.WHEEL).performTouchInput {
            val c = center
            val r = width / 2f
            // Down the right-hand side: clockwise.
            swipe(c + Offset(r * 0.6f, -r * 0.5f), c + Offset(r * 0.6f, r * 0.5f), durationMillis = 80)
        }
        settle()
        val spin = phase() as SpinPhase.Spinning
        assertTrue(spin.plan.endRotation > spin.plan.startRotation)
    }

    @Test
    fun strongFlickCounterClockwise_spinsBackward() {
        compose.onNodeWithTag(WheelTestTags.WHEEL).performTouchInput {
            val c = center
            val r = width / 2f
            swipe(c + Offset(r * 0.6f, r * 0.5f), c + Offset(r * 0.6f, -r * 0.5f), durationMillis = 80)
        }
        settle()
        val spin = phase() as SpinPhase.Spinning
        assertTrue(spin.plan.endRotation < spin.plan.startRotation)
    }

    @Test
    fun outOfPrizes_disablesButton_andClearsAfterRestock() {
        runBlocking { prizes.replaceAll(prizes.prizes.first().map { if (it.isFallback) it else it.copy(quantity = 0) }) }
        awaitCondition { !vm.state.value.spinAllowed }
        compose.onNodeWithTag(WheelTestTags.OUT_OF_PRIZES).assertExists()
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertIsNotEnabled()

        runBlocking { prizes.replaceAll(prizes.prizes.first().map { if (it.name == "Ly giữ nhiệt") it.copy(quantity = 3) else it }) }
        awaitCondition { vm.state.value.spinAllowed }
        compose.onNodeWithTag(WheelTestTags.OUT_OF_PRIZES).assertDoesNotExist()
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).assertIsEnabled()
    }

    @Test
    fun cornerHold3s_opensPin() {
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(3_100)
        settle()
        assertEquals(1, cornerHolds)
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { up() }
    }

    @Test
    fun cornerShortHold_doesNothing() {
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(3_000)
        settle()
        assertEquals(0, cornerHolds)
    }

    @Test
    fun cornerHold_ignoredWhileSpinning() {
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        compose.onNodeWithTag(WheelTestTags.CORNER).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(3_100)
        settle()
        assertEquals(0, cornerHolds)
    }
}
