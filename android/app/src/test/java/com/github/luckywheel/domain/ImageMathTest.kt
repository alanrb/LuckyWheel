package com.github.luckywheel.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageMathTest {

    @Test
    fun largeLandscape_downscaledToLongSide() {
        assertEquals(1024 to 683, scaledSize(6000, 4000, 1024))
    }

    @Test
    fun largePortrait_downscaledToLongSide() {
        assertEquals(576 to 1024, scaledSize(3000, 5333, 1024))
    }

    @Test
    fun small_unchanged() {
        assertEquals(800 to 600, scaledSize(800, 600, 1024))
    }
}
