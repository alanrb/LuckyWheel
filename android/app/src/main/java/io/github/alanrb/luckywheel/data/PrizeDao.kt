package io.github.alanrb.luckywheel.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrizeDao {
    @Query("SELECT * FROM prizes ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<PrizeEntity>>

    @Query("SELECT * FROM prizes WHERE id = :id")
    suspend fun getById(id: Long): PrizeEntity?

    @Query("SELECT COUNT(*) FROM prizes")
    suspend fun count(): Int

    @Insert
    suspend fun insert(prize: PrizeEntity): Long

    @Update
    suspend fun update(prize: PrizeEntity)

    @Query("DELETE FROM prizes WHERE id NOT IN (:ids)")
    suspend fun deleteAllExcept(ids: List<Long>)

    /** Guarded decrement: never takes a limited quantity below 0 (PRD §5.4). Returns rows changed. */
    @Query("UPDATE prizes SET quantity = quantity - 1 WHERE id = :id AND quantity IS NOT NULL AND quantity > 0")
    suspend fun decrementIfAvailable(id: Long): Int
}
