package com.github.luckywheel.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.github.luckywheel.domain.Prize
import com.github.luckywheel.domain.effectiveWheel
import com.github.luckywheel.domain.prize
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrizeRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PrizeRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = PrizeRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun current(): List<Prize> = repo.prizes.first()
    private suspend fun byName(name: String) = current().single { it.name == name }

    private suspend fun seed() = repo.seedIfEmpty(seedPrizes("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt"))

    @Test
    fun seed_matchesPr08() = runTest {
        seed()
        val prizes = current()
        assertEquals(listOf("Chúc may mắn lần sau", "Giảm 10%", "Cà phê sữa đá", "Ly giữ nhiệt"), prizes.map { it.name })
        assertEquals(listOf(5000, 3000, 1500, 500), prizes.map { it.probabilityBp })
        assertEquals(listOf(null, null, null, 10), prizes.map { it.quantity })
        assertEquals(listOf(true, false, false, false), prizes.map { it.isFallback })
    }

    @Test
    fun seedIfEmpty_onlyOnce() = runTest {
        seed()
        seed()
        assertEquals(4, current().size)
    }

    @Test
    fun claim_decrementsOncePerReveal() = runTest {
        seed()
        val tumbler = byName("Ly giữ nhiệt")
        assertTrue(repo.claimPrize(tumbler.id))
        assertEquals(9, byName("Ly giữ nhiệt").quantity)
    }

    @Test
    fun claim_neverBelowZero() = runTest {
        seed()
        val tumbler = byName("Ly giữ nhiệt")
        repo.replaceAll(current().map { if (it.id == tumbler.id) it.copy(quantity = 1) else it })
        assertTrue(repo.claimPrize(tumbler.id))
        assertFalse(repo.claimPrize(tumbler.id))
        assertEquals(0, byName("Ly giữ nhiệt").quantity)
    }

    @Test
    fun claim_unlimitedAndFallback_unchanged() = runTest {
        seed()
        assertTrue(repo.claimPrize(byName("Giảm 10%").id))
        assertTrue(repo.claimPrize(byName("Chúc may mắn lần sau").id))
        assertNull(byName("Giảm 10%").quantity)
        assertNull(byName("Chúc may mắn lần sau").quantity)
    }

    @Test
    fun claim_unknownPrize_false() = runTest {
        seed()
        assertFalse(repo.claimPrize(9_999))
    }

    @Test
    fun restock_makesAvailableAgain() = runTest {
        seed()
        val tumbler = byName("Ly giữ nhiệt")
        repo.replaceAll(current().map { if (it.id == tumbler.id) it.copy(quantity = 0) else it })
        assertFalse(effectiveWheel(current()).outcomes.any { it.prize.id == tumbler.id })
        repo.replaceAll(current().map { if (it.id == tumbler.id) it.copy(quantity = 5) else it })
        assertTrue(effectiveWheel(current()).outcomes.any { it.prize.id == tumbler.id })
    }

    @Test
    fun replaceAll_persistsOrderAndAssignsIds() = runTest {
        seed()
        val (fallback, discount, coffee, tumbler) = current()
        val added = prize(-1, 0, name = "Bánh croissant")
        repo.replaceAll(listOf(coffee, added, fallback, tumbler.copy(probabilityBp = 3500)))
        val after = current()
        assertEquals(listOf("Cà phê sữa đá", "Bánh croissant", "Chúc may mắn lần sau", "Ly giữ nhiệt"), after.map { it.name })
        assertEquals(listOf(0, 1, 2, 3), after.map { it.sortOrder })
        assertTrue(after[1].id > 0)
        assertFalse(after.any { it.id == discount.id })
        assertEquals(3500, after[3].probabilityBp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun replaceAll_withoutFallback_rejected() = runTest {
        seed()
        repo.replaceAll(current().filterNot { it.isFallback })
    }
}
