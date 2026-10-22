package io.github.alanrb.luckywheel.domain

const val MAX_PRIZES = 12
const val MAX_NAME_LENGTH = 24

/** Problems that block saving the prize list (PR-04, PR-05). */
sealed interface PrizeListIssue {
    data class TotalShort(val missingBp: Int) : PrizeListIssue
    data class TotalOver(val excessBp: Int) : PrizeListIssue
    data object TooManyPrizes : PrizeListIssue
    data object TooFewNonZero : PrizeListIssue
}

fun validatePrizeList(prizes: List<Prize>): List<PrizeListIssue> = buildList {
    val total = prizes.sumOf { it.probabilityBp }
    if (total < TOTAL_BP) add(PrizeListIssue.TotalShort(TOTAL_BP - total))
    if (total > TOTAL_BP) add(PrizeListIssue.TotalOver(total - TOTAL_BP))
    if (prizes.size > MAX_PRIZES) add(PrizeListIssue.TooManyPrizes)
    if (prizes.count { it.probabilityBp > 0 } < 2) add(PrizeListIssue.TooFewNonZero)
}

enum class EditorField { NAME, PERCENT, QUANTITY }

/** Raw text from the prize editor, before it becomes a [Prize]. */
data class PrizeEditorInput(
    val name: String,
    val percentText: String,
    val unlimited: Boolean,
    val quantityText: String,
    val isFallback: Boolean,
)

private val QUANTITY_PATTERN = Regex("^\\d+$")

fun validateEditor(input: PrizeEditorInput): Set<EditorField> = buildSet {
    val name = input.name.trim()
    if (name.isEmpty() || name.length > MAX_NAME_LENGTH) add(EditorField.NAME)
    if (parsePercentToBp(input.percentText) == null) add(EditorField.PERCENT)
    if (!input.isFallback && !input.unlimited &&
        (!QUANTITY_PATTERN.matches(input.quantityText.trim()) || input.quantityText.trim().toIntOrNull() == null)
    ) {
        add(EditorField.QUANTITY)
    }
}
