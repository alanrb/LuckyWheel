package com.github.luckywheel.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.luckywheel.domain.ColorMode
import com.github.luckywheel.domain.Prize

/** Room row for a prize (PRD §7.1). A null [quantity] means unlimited. */
@Entity(tableName = "prizes")
data class PrizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorMode: String,
    val customColor: Int?,
    val probabilityBp: Int,
    val quantity: Int?,
    val isFallback: Boolean,
    val sortOrder: Int,
)

fun PrizeEntity.toDomain() = Prize(
    id = id,
    name = name,
    colorMode = ColorMode.entries.firstOrNull { it.name == colorMode } ?: ColorMode.THEME,
    customColor = customColor,
    probabilityBp = probabilityBp,
    quantity = quantity,
    isFallback = isFallback,
    sortOrder = sortOrder,
)

fun Prize.toEntity(id: Long = this.id, sortOrder: Int = this.sortOrder) = PrizeEntity(
    id = id,
    name = name,
    colorMode = colorMode.name,
    customColor = customColor,
    probabilityBp = probabilityBp,
    quantity = if (isFallback) null else quantity,
    isFallback = isFallback,
    sortOrder = sortOrder,
)
