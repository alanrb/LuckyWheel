package com.github.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrizeValidationTest {

    @Test
    fun total9999_short1bp() {
        val issues = validatePrizeList(listOf(prize(1, 5000, fallback = true), prize(2, 4999)))
        assertEquals(listOf(PrizeListIssue.TotalShort(1)), issues)
    }

    @Test
    fun total10001_over1bp() {
        val issues = validatePrizeList(listOf(prize(1, 5001, fallback = true), prize(2, 5000)))
        assertEquals(listOf(PrizeListIssue.TotalOver(1)), issues)
    }

    @Test
    fun thirteenPrizes_tooMany() {
        val prizes = listOf(prize(1, 10_000 - 1200, fallback = true)) + (2L..13L).map { prize(it, 100) }
        assertEquals(listOf(PrizeListIssue.TooManyPrizes), validatePrizeList(prizes))
    }

    @Test
    fun oneNonZero_tooFew() {
        val prizes = listOf(prize(1, 10_000, fallback = true), prize(2, 0))
        assertEquals(listOf(PrizeListIssue.TooFewNonZero), validatePrizeList(prizes))
    }

    @Test
    fun valid_seed_noIssues() {
        assertTrue(validatePrizeList(seedSample()).isEmpty())
    }

    private fun input(
        name: String = "Bánh",
        pct: String = "10",
        unlimited: Boolean = false,
        qty: String = "3",
        fallback: Boolean = false,
    ) = PrizeEditorInput(name, pct, unlimited, qty, fallback)

    @Test
    fun editor_valid_noErrors() {
        assertEquals(emptySet<EditorField>(), validateEditor(input()))
    }

    @Test
    fun editor_blankName_percent101_percent3Decimals_negativeQty_flagged() {
        assertEquals(setOf(EditorField.NAME), validateEditor(input(name = "   ")))
        assertEquals(setOf(EditorField.NAME), validateEditor(input(name = "x".repeat(25))))
        assertEquals(setOf(EditorField.PERCENT), validateEditor(input(pct = "101")))
        assertEquals(setOf(EditorField.PERCENT), validateEditor(input(pct = "1,234")))
        assertEquals(setOf(EditorField.QUANTITY), validateEditor(input(qty = "-1")))
        assertEquals(setOf(EditorField.QUANTITY), validateEditor(input(qty = "")))
    }

    @Test
    fun editor_unlimitedOrFallback_ignoresQuantity() {
        assertEquals(emptySet<EditorField>(), validateEditor(input(unlimited = true, qty = "abc")))
        assertEquals(emptySet<EditorField>(), validateEditor(input(fallback = true, qty = "abc")))
    }
}
