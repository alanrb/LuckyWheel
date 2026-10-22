package io.github.alanrb.luckywheel.sound

import io.github.alanrb.luckywheel.domain.ThemeId

/** Theme sound set (TH-03, SP-07, RS-02). Callers decide whether sound is enabled. */
interface SoundEffects {
    fun tick(theme: ThemeId)
    fun win(theme: ThemeId)
    fun lose()
}

object NoSound : SoundEffects {
    override fun tick(theme: ThemeId) = Unit
    override fun win(theme: ThemeId) = Unit
    override fun lose() = Unit
}
