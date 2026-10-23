package com.github.luckywheel.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.luckywheel.data.AppDatabase
import com.github.luckywheel.data.FakeClock
import com.github.luckywheel.data.ImageStore
import com.github.luckywheel.data.PinManager
import com.github.luckywheel.data.PrizeRepository
import com.github.luckywheel.data.SettingsRepository
import com.github.luckywheel.data.TestDataStore
import com.github.luckywheel.data.seedPrizes
import com.github.luckywheel.domain.PinHasher
import com.github.luckywheel.sound.NoSound
import com.github.luckywheel.ui.theme.LuckyWheelTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** System Back closes what is open inside Settings before it leaves Settings (and its unsaved draft). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1280dp-h800dp-land-xhdpi")
class SettingsBackTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var ds: TestDataStore
    private lateinit var vm: SettingsViewModel
    private var exits = 0

    @Before
    fun setUp() {
        compose.mainClock.autoAdvance = false
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        val prizes = PrizeRepository(db)
        runBlocking { prizes.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt")) }
        vm = SettingsViewModel(prizes, SettingsRepository(ds.store), PinManager(ds.store, PinHasher(1_000), FakeClock()), ImageStore(context))
        compose.setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            LuckyWheelTheme { SettingsScreen(state, vm, NoSound, onExit = { exits++ }) }
        }
        val deadline = System.currentTimeMillis() + 5_000
        while (!vm.state.value.loaded) {
            check(System.currentTimeMillis() < deadline)
            settle()
            Thread.sleep(10)
        }
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

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        repeat(2) { settle() }
    }

    @Test
    fun back_closesEditor_keepsDraftAndSettings() {
        vm.deletePrize(vm.state.value.draft.single { it.name == "Giảm 10%" }.id)
        vm.openEditor(vm.state.value.draft.first().id)
        repeat(2) { settle() }
        assertNotNull(vm.state.value.editor)

        pressBack()
        assertNull(vm.state.value.editor)
        assertEquals(0, exits)
        assertFalse(compose.activity.isFinishing)
        assertEquals(3, vm.state.value.draft.size)
    }

    @Test
    fun back_cancelsChangePin() {
        vm.selectSection(SettingsSection.SECURITY)
        vm.startChangePin()
        repeat(2) { settle() }
        pressBack()
        assertNull(vm.state.value.changePin)
        assertFalse(compose.activity.isFinishing)
    }
}
