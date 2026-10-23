package com.github.luckywheel.domain

const val TOTAL_BP = 10_000

/** One visible slice of the wheel with its effective weight. */
data class Outcome(val prize: Prize, val weightBp: Int)

data class EffectiveWheel(val outcomes: List<Outcome>, val spinAllowed: Boolean)

/**
 * Effective probabilities (PRD §5.2): available real prizes keep their weight, the weight of every
 * out-of-stock real prize moves to the fallback, and outcomes with weight 0 are not on the wheel.
 * Spinning is allowed only while at least one real prize is available (§3.6).
 */
fun effectiveWheel(prizes: List<Prize>): EffectiveWheel {
    val ordered = prizes.sortedWith(compareBy<Prize> { it.sortOrder }.thenBy { it.id })
    val movedToFallback = ordered.filter { it.isOutOfStock }.sumOf { it.probabilityBp }
    var anyReal = false
    val outcomes = ordered.mapNotNull { p ->
        when {
            p.isFallback -> (p.probabilityBp + movedToFallback).takeIf { it > 0 }?.let { Outcome(p, it) }
            p.probabilityBp > 0 && !p.isOutOfStock -> {
                anyReal = true
                Outcome(p, p.probabilityBp)
            }
            else -> null
        }
    }
    return EffectiveWheel(outcomes, anyReal)
}

/** Walks the cumulative weights: the first outcome whose cumulative weight exceeds [drawn] wins. */
fun outcomeIndexFor(outcomes: List<Outcome>, drawn: Int): Int {
    var cumulative = 0
    outcomes.forEachIndexed { i, o ->
        cumulative += o.weightBp
        if (drawn < cumulative) return i
    }
    return outcomes.lastIndex
}

/** Weighted draw (PRD §5.3) using integer arithmetic only. */
fun drawOutcomeIndex(outcomes: List<Outcome>, random: RandomSource): Int {
    val total = outcomes.sumOf { it.weightBp }
    require(total > 0) { "no outcome has weight" }
    return outcomeIndexFor(outcomes, random.nextInt(total))
}
