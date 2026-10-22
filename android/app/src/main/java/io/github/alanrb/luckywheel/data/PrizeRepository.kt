package io.github.alanrb.luckywheel.data

import androidx.room.withTransaction
import io.github.alanrb.luckywheel.domain.ColorMode
import io.github.alanrb.luckywheel.domain.Prize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrizeRepository(private val db: AppDatabase) {
    private val dao = db.prizeDao()

    /** Prizes in display (slice) order. */
    val prizes: Flow<List<Prize>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun seedIfEmpty(seed: List<Prize>) = db.withTransaction {
        if (dao.count() == 0) {
            seed.forEachIndexed { i, p -> dao.insert(p.toEntity(id = 0, sortOrder = i)) }
        }
    }

    /**
     * Saves the owner's edited list: rows with id ≤ 0 are new, rows missing from [prizes] are deleted,
     * and the list order becomes the slice order.
     */
    suspend fun replaceAll(prizes: List<Prize>) {
        require(prizes.count { it.isFallback } == 1) { "exactly one fallback prize is required" }
        db.withTransaction {
            dao.deleteAllExcept(prizes.filter { it.id > 0 }.map { it.id })
            prizes.forEachIndexed { i, p ->
                if (p.id > 0) dao.update(p.toEntity(sortOrder = i)) else dao.insert(p.toEntity(id = 0, sortOrder = i))
            }
        }
    }

    /**
     * Awards a revealed prize in one transaction (PRD §5.4). The fallback and unlimited prizes are always
     * awarded. A limited prize is decremented only if it still has stock; false means it had none.
     */
    suspend fun claimPrize(prizeId: Long): Boolean = db.withTransaction {
        val row = dao.getById(prizeId) ?: return@withTransaction false
        if (row.isFallback || row.quantity == null) true else dao.decrementIfAvailable(prizeId) == 1
    }
}

/** First-launch sample prizes (PR-08); names come from string resources. */
fun seedPrizes(fallbackName: String, discount: String, coffee: String, tumbler: String): List<Prize> {
    fun p(name: String, bp: Int, qty: Int?, fallback: Boolean, order: Int) =
        Prize(0, name, ColorMode.THEME, null, bp, qty, fallback, order)
    return listOf(
        p(fallbackName, 5000, null, true, 0),
        p(discount, 3000, null, false, 1),
        p(coffee, 1500, null, false, 2),
        p(tumbler, 500, 10, false, 3),
    )
}
