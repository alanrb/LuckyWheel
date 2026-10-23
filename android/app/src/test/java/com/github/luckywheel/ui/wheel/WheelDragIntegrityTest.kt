package com.github.luckywheel.ui.wheel

import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.luckywheel.data.AppDatabase
import com.github.luckywheel.data.PrizeRepository
import com.github.luckywheel.data.SettingsRepository
import com.github.luckywheel.data.TestDataStore
import com.github.luckywheel.data.seedPrizes
import com.github.luckywheel.domain.Clock
import com.github.luckywheel.domain.SeededRandomSource
import com.github.luckywheel.sound.NoSound
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
import org.robolectric.annotation.GraphicsMode
import kotlin.math.cos
import kotlin.math.sin

/**
 * The wheel must come to rest on the slice that is announced (PRD §3.4, §11 "tap while flicking"),
 * checked on the rendered pixels rather than the ViewModel state.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WheelDragIntegrityTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var prizes: PrizeRepository
    private lateinit var ds: TestDataStore
    private val clock = object : Clock {
        override fun nowMs() = compose.mainClock.currentTime
        override fun elapsedMs() = compose.mainClock.currentTime
    }
    private lateinit var vm: WheelViewModel

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
                actions = WheelActions(vm::spinWithButton, vm::spinWithFlick, vm::onSpinAnimationFinished, vm::dismissResult, {}),
            )
        }
        awaitCondition { vm.state.value.loaded && vm.state.value.slices.size == 4 }
        // Decorations off so nothing but the wheel is sampled.
        runBlocking { SettingsRepository(ds.store).update { it.copy(showThemeDecorations = false) } }
        awaitCondition { !vm.state.value.settings.showThemeDecorations }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
    }

    private fun settle() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
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

    /** Index of the slice whose colour is nearest to the pixel just above the hub, under the pointer. */
    private fun sliceUnderPointerOnScreen(): Int {
        val view = compose.activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        compose.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        val wheel = compose.onNodeWithTag(WheelTestTags.WHEEL).fetchSemanticsNode().boundsInRoot
        val s = minOf(wheel.width, wheel.height)
        val r = s / 2 - s * 0.022f - s * 0.014f
        val argb = bmp.getPixel(wheel.center.x.toInt(), (wheel.center.y - 0.2f * r).toInt())
        val slices = vm.state.value.slices
        fun ch(c: Int, sh: Int) = (c shr sh) and 0xFF
        fun dist(c: Int): Int {
            val dr = ch(c, 16) - ch(argb, 16); val dg = ch(c, 8) - ch(argb, 8); val db = ch(c, 0) - ch(argb, 0)
            return dr * dr + dg * dg + db * db
        }
        return slices.indices.minBy { dist(slices[it].color) }
    }

    private fun finishAndDismiss(): Int {
        val spin = vm.state.value.phase as SpinPhase.Spinning
        compose.mainClock.advanceTimeBy(8_000)
        awaitCondition { vm.state.value.phase is SpinPhase.Revealed }
        compose.onNodeWithTag(WheelTestTags.RESULT).performClick()
        awaitCondition { vm.state.value.phase == SpinPhase.Idle }
        assertEquals(4, vm.state.value.slices.size)
        return spin.plan.outcomeIndex
    }

    @Test
    fun control_buttonOnly_pointerRestsOnRevealedSlice() {
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        val outcome = finishAndDismiss()
        assertEquals(outcome, sliceUnderPointerOnScreen())
    }

    private fun wheelPoint(deg: Double): Offset {
        val wheel = compose.onNodeWithTag(WheelTestTags.WHEEL).fetchSemanticsNode().boundsInRoot
        val rr = wheel.width / 2f * 0.6f
        return wheel.center + Offset((cos(Math.toRadians(deg)) * rr).toFloat(), (sin(Math.toRadians(deg)) * rr).toFloat())
    }

    @Test
    fun quayTappedWhileDragging_isIgnored_andLaterSpinRestsOnRevealedSlice() {
        val button = compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).fetchSemanticsNode().boundsInRoot
        val points = (0..18).map { wheelPoint(-45.0 + 5 * it) }
        compose.onRoot().performTouchInput {
            down(0, points[0])
            points.drop(1).forEach { moveTo(0, it, delayMillis = 40) }
            // A second finger taps QUAY while the first is still turning the wheel.
            down(1, button.center)
            up(1)
            moveTo(0, wheelPoint(50.0), delayMillis = 40)
            up(0)
        }
        settle()
        assertEquals(SpinPhase.Idle, vm.state.value.phase)
        // Weak release: the wheel springs back to rest.
        compose.mainClock.advanceTimeBy(600)
        settle()
        assertEquals(SpinPhase.Idle, vm.state.value.phase)

        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        val outcome = finishAndDismiss()
        assertEquals("slice under the pointer must be the revealed one", outcome, sliceUnderPointerOnScreen())
    }

    @Test
    fun spinStartingMidDrag_dropsTheStaleDragOffset() {
        val points = (0..18).map { wheelPoint(-45.0 + 5 * it) }
        compose.onRoot().performTouchInput {
            down(0, points[0])
            points.drop(1).forEach { moveTo(0, it, delayMillis = 40) }
        }
        // Whatever route starts a spin mid-drag, the wheel must show the planned rotation.
        compose.runOnUiThread { vm.spinWithButton() }
        settle()
        assertTrue(vm.state.value.phase is SpinPhase.Spinning)
        compose.onRoot().performTouchInput { up(0) }
        settle()
        val outcome = finishAndDismiss()
        assertEquals("slice under the pointer must be the revealed one", outcome, sliceUnderPointerOnScreen())

        // And the next spin is not thrown off either.
        compose.onNodeWithTag(WheelTestTags.SPIN_BUTTON).performClick()
        settle()
        val next = finishAndDismiss()
        assertEquals(next, sliceUnderPointerOnScreen())
    }
}
