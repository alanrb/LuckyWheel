package io.github.alanrb.luckywheel.ui.settings

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
import io.github.alanrb.luckywheel.domain.ColorMode
import io.github.alanrb.luckywheel.domain.PinHasher
import io.github.alanrb.luckywheel.domain.Prize
import io.github.alanrb.luckywheel.domain.PrizeListIssue
import io.github.alanrb.luckywheel.domain.ThemeId
import io.github.alanrb.luckywheel.domain.effectiveWheel
import io.github.alanrb.luckywheel.ui.pin.PinKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var db: AppDatabase
    private lateinit var prizes: PrizeRepository
    private lateinit var ds: TestDataStore
    private lateinit var settings: SettingsRepository
    private lateinit var pins: PinManager
    private lateinit var vm: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prizes = PrizeRepository(db)
        ds = TestDataStore(tmp.newFile("s.preferences_pb").also { it.delete() })
        settings = SettingsRepository(ds.store)
        pins = PinManager(ds.store, PinHasher(iterations = 1_000), FakeClock())
        runBlocking {
            prizes.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt"))
            pins.createPin("1234")
        }
        vm = SettingsViewModel(prizes, settings, pins, ImageStore(ApplicationProvider.getApplicationContext()))
        await { vm.state.value.loaded }
    }

    @After
    fun tearDown() {
        db.close()
        ds.close()
        Dispatchers.resetMain()
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "condition not met in time" }
            Thread.sleep(10)
        }
    }

    private fun stored(): List<Prize> = runBlocking { prizes.prizes.first() }
    private fun draftId(name: String) = vm.state.value.draft.single { it.name == name }.id

    @Test
    fun draft_startsAsStoredPrizes() {
        val state = vm.state.value
        assertEquals(stored(), state.draft)
        assertFalse(state.dirty)
        assertTrue(state.issues.isEmpty())
    }

    @Test
    fun save_blockedUnlessTotal100() {
        vm.openEditor(draftId("Giảm 10%"))
        vm.updateEditor { it.copy(percentText = "31") }
        vm.commitEditor()
        assertEquals(listOf(PrizeListIssue.TotalOver(100)), vm.state.value.issues)
        assertTrue(vm.state.value.dirty)
        vm.save()
        Thread.sleep(100)
        assertEquals(3000, stored().single { it.name == "Giảm 10%" }.probabilityBp)
        assertFalse(vm.state.value.justSaved)
    }

    @Test
    fun deleteFallback_refused() {
        val fallback = vm.state.value.draft.single { it.isFallback }
        vm.deletePrize(fallback.id)
        assertTrue(vm.state.value.draft.any { it.isFallback })
        vm.openEditor(fallback.id)
        assertFalse(vm.state.value.editor!!.canDelete)
    }

    @Test
    fun addEditReorderSave_persists() {
        vm.openEditor(null)
        vm.updateEditor {
            it.copy(name = "Bánh croissant", percentText = "5", unlimited = false, quantityText = "20",
                colorMode = ColorMode.CUSTOM, customColor = 0xFF00897B.toInt())
        }
        vm.commitEditor()
        vm.openEditor(draftId("Chúc may mắn lần sau"))
        vm.updateEditor { it.copy(percentText = "45") }
        vm.commitEditor()
        vm.move(from = 4, to = 0)
        assertTrue(vm.state.value.issues.isEmpty())
        vm.save()
        await { vm.state.value.justSaved && !vm.state.value.dirty }
        val after = stored()
        assertEquals(listOf("Bánh croissant", "Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt"), after.map { it.name })
        val croissant = after[0]
        assertTrue(croissant.id > 0)
        assertEquals(500, croissant.probabilityBp)
        assertEquals(20, croissant.quantity)
        assertEquals(ColorMode.CUSTOM, croissant.colorMode)
        assertEquals(0xFF00897B.toInt(), croissant.customColor)
        assertEquals(4500, after[1].probabilityBp)
    }

    @Test
    fun discard_restoresStored() {
        vm.deletePrize(draftId("Giảm 10%"))
        assertTrue(vm.state.value.dirty)
        vm.discard()
        assertEquals(stored(), vm.state.value.draft)
        assertFalse(vm.state.value.dirty)
    }

    @Test
    fun restockFromZero_reappearsOnWheel() {
        runBlocking { prizes.replaceAll(stored().map { if (it.name == "Ly giữ nhiệt") it.copy(quantity = 0) else it }) }
        // discard() copies the ViewModel's view of the stored list, so wait for that, not just the database.
        await { vm.state.value.stored.single { it.name == "Ly giữ nhiệt" }.quantity == 0 }
        vm.discard()
        await { vm.state.value.draft.single { it.name == "Ly giữ nhiệt" }.quantity == 0 }
        vm.openEditor(draftId("Ly giữ nhiệt"))
        vm.updateEditor { it.copy(quantityText = "5") }
        vm.commitEditor()
        vm.save()
        await { stored().single { it.name == "Ly giữ nhiệt" }.quantity == 5 }
        assertTrue(effectiveWheel(stored()).outcomes.any { it.prize.name == "Ly giữ nhiệt" })
    }

    @Test
    fun editorErrors_blockCommit() {
        vm.openEditor(null)
        vm.updateEditor { it.copy(name = "", percentText = "abc") }
        vm.commitEditor()
        assertNotNull(vm.state.value.editor)
        assertEquals(4, vm.state.value.draft.size)
    }

    @Test
    fun maxTwelve_addDisabled() {
        repeat(8) { i ->
            vm.openEditor(null)
            vm.updateEditor { it.copy(name = "Giải $i", percentText = "0") }
            vm.commitEditor()
        }
        assertEquals(12, vm.state.value.draft.size)
        assertFalse(vm.state.value.canAdd)
        vm.openEditor(null)
        assertNull(vm.state.value.editor)
    }

    @Test
    fun appearance_appliesImmediately() {
        vm.setTheme(ThemeId.TET)
        vm.setSoundEnabled(false)
        vm.setResultSeconds(25)
        // Each change is its own DataStore edit; wait until all three have landed.
        await {
            val s = runBlocking { settings.settings.first() }
            s.themeId == ThemeId.TET && !s.soundEnabled && s.resultDisplaySeconds == 25
        }
    }

    private fun type(pin: String) = pin.forEach { vm.pressChangePin(PinKey.Digit(it.digitToInt())) }

    @Test
    fun changePin_threeSteps() {
        vm.startChangePin()
        type("9999")
        await { vm.state.value.changePin?.error == ChangePinError.CURRENT_WRONG }
        type("1234")
        await { vm.state.value.changePin?.step == 1 }
        type("5678")
        await { vm.state.value.changePin?.step == 2 }
        type("5678")
        await { vm.state.value.changePin == null && vm.state.value.pinChanged }
        assertTrue(runBlocking { pins.matchesCurrent("5678") })
    }

    @Test
    fun changePin_confirmMismatch_backToNewPin() {
        vm.startChangePin()
        type("1234")
        await { vm.state.value.changePin?.step == 1 }
        type("5678")
        await { vm.state.value.changePin?.step == 2 }
        type("5679")
        await { vm.state.value.changePin?.error == ChangePinError.MISMATCH }
        assertEquals(1, vm.state.value.changePin?.step)
        assertTrue(runBlocking { pins.matchesCurrent("1234") })
    }

    @Test
    fun reorder_previewFollowsDraftOrderBeforeSave() {
        vm.move(from = 3, to = 0)
        val draftNames = vm.state.value.draft.map { it.name }
        assertEquals(listOf("Ly giữ nhiệt", "Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá"), draftNames)
        // PR-07 / WH-03: the preview shows slices in list order.
        assertEquals(draftNames, effectiveWheel(vm.state.value.draft).outcomes.map { it.prize.name })

        vm.deletePrize(draftId("Giảm 10%"))
        vm.openEditor(null)
        vm.updateEditor { it.copy(name = "Bánh", percentText = "0") }
        vm.commitEditor()
        vm.move(from = 3, to = 1)
        val names = vm.state.value.draft.filter { it.probabilityBp > 0 }.map { it.name }
        assertEquals(names, effectiveWheel(vm.state.value.draft).outcomes.map { it.prize.name })
    }
}
