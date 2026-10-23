package io.github.alanrb.luckywheel.ui.settings

/**
 * Whether the owner has entered the PIN in this process (ST-01, hard priority 3). It lives only in
 * memory, so a restarted app always asks for the PIN again even if navigation state is restored.
 */
class SettingsGate {
    var unlocked: Boolean = false
        private set

    /** Settings has launched the system photo picker; leaving the app for it is not "leaving". */
    var pickerOpen: Boolean = false

    fun unlock() {
        unlocked = true
    }

    fun lock() {
        unlocked = false
        pickerOpen = false
    }
}
