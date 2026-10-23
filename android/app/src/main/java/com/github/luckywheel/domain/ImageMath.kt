package com.github.luckywheel.domain

import kotlin.math.roundToInt

/** Size that fits [maxLongSide] on the long side, keeping aspect ratio; never upscales (LG-02). */
fun scaledSize(width: Int, height: Int, maxLongSide: Int): Pair<Int, Int> {
    val long = maxOf(width, height)
    if (long <= maxLongSide) return width to height
    val scale = maxLongSide.toDouble() / long
    return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
}
