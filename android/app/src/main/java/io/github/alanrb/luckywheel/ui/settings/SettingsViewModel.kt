package io.github.alanrb.luckywheel.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.alanrb.luckywheel.data.AppSettings
import io.github.alanrb.luckywheel.data.ImageKind
import io.github.alanrb.luckywheel.data.ImageStore
import io.github.alanrb.luckywheel.data.PinManager
import io.github.alanrb.luckywheel.data.PrizeRepository
import io.github.alanrb.luckywheel.data.SettingsRepository
import io.github.alanrb.luckywheel.domain.ColorMode
import io.github.alanrb.luckywheel.domain.EditorField
import io.github.alanrb.luckywheel.domain.MAX_PRIZES
import io.github.alanrb.luckywheel.domain.PinPolicy
import io.github.alanrb.luckywheel.domain.Prize
import io.github.alanrb.luckywheel.domain.PrizeEditorInput
import io.github.alanrb.luckywheel.domain.PrizeListIssue
import io.github.alanrb.luckywheel.domain.ThemeId
import io.github.alanrb.luckywheel.domain.parsePercentToBp
import io.github.alanrb.luckywheel.domain.percentInputText
import io.github.alanrb.luckywheel.domain.validateEditor
import io.github.alanrb.luckywheel.domain.validatePrizeList
import io.github.alanrb.luckywheel.ui.pin.PIN_SUBMIT_DELAY_MS
import io.github.alanrb.luckywheel.ui.pin.PinKey
import io.github.alanrb.luckywheel.ui.pin.typed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SettingsSection { PRIZES, APPEARANCE, SOUND, DISPLAY, SECURITY }

enum class ChangePinError { CURRENT_WRONG, MISMATCH }

/** ST-05: current PIN, new PIN, new PIN again. */
data class ChangePinState(val step: Int = 0, val input: String = "", val error: ChangePinError? = null, val busy: Boolean = false)

/** The prize editor's fields as typed (PR-01, §4.3). [id] is null for a new prize. */
data class EditorState(
    val id: Long?,
    val name: String,
    val colorMode: ColorMode,
    val customColor: Int,
    val percentText: String,
    val unlimited: Boolean,
    val quantityText: String,
    val isFallback: Boolean,
) {
    val errors: Set<EditorField> = validateEditor(PrizeEditorInput(name, percentText, unlimited, quantityText, isFallback))
    val canDelete: Boolean get() = id != null && !isFallback
}

data class SettingsUiState(
    val loaded: Boolean = false,
    val section: SettingsSection = SettingsSection.PRIZES,
    /** Prizes as saved; the wheel shows these. */
    val stored: List<Prize> = emptyList(),
    /** The owner's edits; saved only as a whole, and only when valid (PR-04). */
    val draft: List<Prize> = emptyList(),
    val justSaved: Boolean = false,
    val editor: EditorState? = null,
    val settings: AppSettings = AppSettings(),
    val changePin: ChangePinState? = null,
    val pinChanged: Boolean = false,
    val imageError: Boolean = false,
) {
    val issues: List<PrizeListIssue> = validatePrizeList(draft)
    val dirty: Boolean = draft.renumbered() != stored.renumbered()
    val canAdd: Boolean = draft.size < MAX_PRIZES
    val totalBp: Int = draft.sumOf { it.probabilityBp }
    val canSave: Boolean = loaded && dirty && issues.isEmpty()
}

/** List position is the slice order (WH-03); keep sortOrder in step so the preview follows edits. */
private fun List<Prize>.renumbered() = mapIndexed { i, p -> if (p.sortOrder == i) p else p.copy(sortOrder = i) }

private const val DEFAULT_CUSTOM_COLOR = 0xFFC62828.toInt()

class SettingsViewModel(
    private val prizes: PrizeRepository,
    private val settings: SettingsRepository,
    private val pins: PinManager,
    private val images: ImageStore,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var nextTempId = -1L
    private var resyncDraft = false
    private var newPin = ""

    init {
        viewModelScope.launch {
            prizes.prizes.collect { list ->
                val resync = resyncDraft
                resyncDraft = false
                _state.update { s -> s.copy(loaded = true, stored = list, draft = if (!s.loaded || resync) list else s.draft) }
            }
        }
        viewModelScope.launch { settings.settings.collect { st -> _state.update { it.copy(settings = st) } } }
    }

    fun selectSection(section: SettingsSection) =
        _state.update { it.copy(section = section, editor = null, changePin = null) }

    // ---- Prizes

    fun openEditor(id: Long?) {
        val s = _state.value
        val editor = if (id == null) {
            if (!s.canAdd) return
            EditorState(null, "", ColorMode.THEME, DEFAULT_CUSTOM_COLOR, "0", unlimited = true, quantityText = "10", isFallback = false)
        } else {
            val p = s.draft.firstOrNull { it.id == id } ?: return
            EditorState(
                id = p.id,
                name = p.name,
                colorMode = p.colorMode,
                customColor = p.customColor ?: DEFAULT_CUSTOM_COLOR,
                percentText = percentInputText(p.probabilityBp),
                unlimited = p.quantity == null,
                quantityText = p.quantity?.toString() ?: "10",
                isFallback = p.isFallback,
            )
        }
        _state.update { it.copy(editor = editor) }
    }

    fun updateEditor(transform: (EditorState) -> EditorState) =
        _state.update { s -> s.copy(editor = s.editor?.let(transform)) }

    fun cancelEditor() = _state.update { it.copy(editor = null) }

    fun commitEditor() {
        val s = _state.value
        val e = s.editor ?: return
        if (e.errors.isNotEmpty()) return
        val bp = parsePercentToBp(e.percentText) ?: return
        val quantity = if (e.isFallback || e.unlimited) null else e.quantityText.trim().toInt()
        val customColor = if (e.colorMode == ColorMode.CUSTOM) e.customColor else null
        val draft = if (e.id == null) {
            if (!s.canAdd) return
            s.draft + Prize(nextTempId--, e.name.trim(), e.colorMode, customColor, bp, quantity, false, s.draft.size)
        } else {
            s.draft.map {
                if (it.id == e.id) {
                    it.copy(name = e.name.trim(), colorMode = e.colorMode, customColor = customColor, probabilityBp = bp, quantity = quantity)
                } else {
                    it
                }
            }
        }
        _state.update { it.copy(draft = draft.renumbered(), editor = null, justSaved = false) }
    }

    /** PR-02: the fallback prize can never be deleted. */
    fun deletePrize(id: Long) = _state.update { s ->
        if (s.draft.any { it.id == id && it.isFallback }) s
        else s.copy(draft = s.draft.filterNot { it.id == id }.renumbered(), editor = null, justSaved = false)
    }

    fun move(from: Int, to: Int) = _state.update { s ->
        if (from !in s.draft.indices || to !in s.draft.indices || from == to) s
        else s.copy(draft = s.draft.toMutableList().apply { add(to, removeAt(from)) }.renumbered(), justSaved = false)
    }

    fun save() {
        val s = _state.value
        if (!s.canSave) return
        viewModelScope.launch {
            resyncDraft = true
            prizes.replaceAll(s.draft)
            _state.update { it.copy(justSaved = true) }
        }
    }

    fun discard() = _state.update { it.copy(draft = it.stored, editor = null, justSaved = false) }

    // ---- Appearance, sound, display (apply immediately)

    fun setTheme(id: ThemeId) = updateSettings { it.copy(themeId = id) }
    fun setDecorations(on: Boolean) = updateSettings { it.copy(showThemeDecorations = on) }
    fun setSoundEnabled(on: Boolean) = updateSettings { it.copy(soundEnabled = on) }
    fun setResultSeconds(seconds: Int) = updateSettings { it.copy(resultDisplaySeconds = seconds) }
    fun setKeepScreenOn(on: Boolean) = updateSettings { it.copy(keepScreenOn = on) }

    fun importLogo(uri: Uri) = importImage(uri, ImageKind.LOGO, { it.logoPath }) { s, p -> s.copy(logoPath = p) }
    fun importBackground(uri: Uri) =
        importImage(uri, ImageKind.BACKGROUND, { it.customBackgroundPath }) { s, p -> s.copy(customBackgroundPath = p) }

    fun removeLogo() = removeImage({ it.logoPath }) { it.copy(logoPath = null) }
    fun removeBackground() = removeImage({ it.customBackgroundPath }) { it.copy(customBackgroundPath = null) }
    fun dismissImageError() = _state.update { it.copy(imageError = false) }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    private fun importImage(
        uri: Uri,
        kind: ImageKind,
        current: (AppSettings) -> String?,
        apply: (AppSettings, String) -> AppSettings,
    ) {
        viewModelScope.launch {
            val path = images.import(uri, kind)
            if (path == null) {
                _state.update { it.copy(imageError = true) }
                return@launch
            }
            val old = current(_state.value.settings)
            settings.update { apply(it, path) }
            images.delete(old)
        }
    }

    private fun removeImage(current: (AppSettings) -> String?, clear: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val old = current(_state.value.settings)
            settings.update(clear)
            images.delete(old)
        }
    }

    // ---- Change PIN

    fun startChangePin() = _state.update { it.copy(changePin = ChangePinState(), pinChanged = false) }

    fun cancelChangePin() = _state.update { it.copy(changePin = null) }

    fun pressChangePin(key: PinKey) {
        val cp = _state.value.changePin ?: return
        if (cp.busy) return
        val input = cp.input.typed(key) ?: return
        val full = input.length == PinPolicy.LENGTH
        _state.update {
            it.copy(changePin = cp.copy(input = input, error = if (key == PinKey.Delete) cp.error else null, busy = full))
        }
        if (full) viewModelScope.launch {
            delay(PIN_SUBMIT_DELAY_MS)
            submitChangePin(cp.step, input)
        }
    }

    private suspend fun submitChangePin(step: Int, pin: String) {
        when (step) {
            0 -> {
                val next = if (pins.matchesCurrent(pin)) ChangePinState(step = 1) else ChangePinState(error = ChangePinError.CURRENT_WRONG)
                _state.update { it.copy(changePin = next) }
            }
            1 -> {
                newPin = pin
                _state.update { it.copy(changePin = ChangePinState(step = 2)) }
            }
            else -> if (pin == newPin) {
                pins.changePin(pin)
                _state.update { it.copy(changePin = null, pinChanged = true) }
            } else {
                _state.update { it.copy(changePin = ChangePinState(step = 1, error = ChangePinError.MISMATCH)) }
            }
        }
    }
}
