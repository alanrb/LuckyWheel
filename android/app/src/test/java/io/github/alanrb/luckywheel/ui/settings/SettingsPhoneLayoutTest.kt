package io.github.alanrb.luckywheel.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.alanrb.luckywheel.data.AppDatabase
import io.github.alanrb.luckywheel.data.FakeClock
import io.github.alanrb.luckywheel.data.ImageStore
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.data.TestDataStore
import io.github.alanrb.luckywheel.data.seedPrizes
import io.github.alanrb.luckywheel.domain.PinHasher
import io.github.alanrb.luckywheel.sound.NoSound
import io.github.alanrb.luckywheel.ui.theme.LuckyWheelTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** PRD §9: Settings must stay usable on a phone (used for testing). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h914dp-port-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsPhoneLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var ds: TestDataStore

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        val prizes = PrizeRepository(db)
        runBlocking { prizes.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt")) }
        val vm = SettingsViewModel(
            prizes, SettingsRepository(ds.store), PinManager(ds.store, PinHasher(1_000), FakeClock()), ImageStore(context),
        )
        compose.setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            LuckyWheelTheme { SettingsScreen(state, vm, NoSound, onExit = {}) }
        }
        val deadline = System.currentTimeMillis() + 5_000
        while (!vm.state.value.loaded) {
            check(System.currentTimeMillis() < deadline)
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            Thread.sleep(10)
        }
        repeat(3) {
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
        }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
    }

    @Test
    fun prizeNames_haveRoomOnAPhone() {
        listOf("Chúc may mắn lần sau", "Cà phê sữa đá", "Giải mặc định").forEach { text ->
            compose.onNodeWithText(text, useUnmergedTree = true).assertWidthIsAtLeast(80.dp)
        }
    }

    @Test
    fun headerSubtitle_hiddenOrReadable() {
        val subtitles = compose.onAllNodesWithText("Vòng quay may mắn", useUnmergedTree = true).fetchSemanticsNodes()
        subtitles.forEach { node ->
            val widthDp = node.size.width / node.layoutInfo.density.density
            assertTrue("subtitle squeezed to $widthDp dp", widthDp >= 120f)
        }
        compose.onNodeWithText("Quay lại vòng quay", useUnmergedTree = true).assertWidthIsAtLeast(100.dp)
    }
}
