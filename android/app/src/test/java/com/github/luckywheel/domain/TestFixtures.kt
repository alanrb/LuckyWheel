package com.github.luckywheel.domain

import java.util.Random

/** Deterministic random source for tests. */
class SeededRandomSource(seed: Long) : RandomSource {
    private val random = Random(seed)
    override fun nextInt(bound: Int): Int = random.nextInt(bound)
}

/** Replays [values] in order, so a test can choose exactly what the draw sees. */
class ScriptedRandomSource(vararg values: Int) : RandomSource {
    private val queue = ArrayDeque(values.toList())
    var calls = 0
        private set

    override fun nextInt(bound: Int): Int {
        calls++
        val v = queue.removeFirstOrNull() ?: 0
        require(v in 0 until bound) { "scripted value $v outside 0 until $bound" }
        return v
    }
}

fun prize(
    id: Long,
    bp: Int,
    qty: Int? = null,
    fallback: Boolean = false,
    name: String = "P$id",
    sortOrder: Int = id.toInt(),
    colorMode: ColorMode = ColorMode.THEME,
    customColor: Int? = null,
) = Prize(
    id = id,
    name = name,
    colorMode = colorMode,
    customColor = customColor,
    probabilityBp = bp,
    quantity = qty,
    isFallback = fallback,
    sortOrder = sortOrder,
)

/** PR-08 sample prizes. */
fun seedSample() = listOf(
    prize(1, 5000, fallback = true, name = "Chúc may mắn lần sau"),
    prize(2, 3000, name = "Giảm 10%"),
    prize(3, 1500, name = "Cà phê sữa đá"),
    prize(4, 500, qty = 10, name = "Ly giữ nhiệt"),
)
