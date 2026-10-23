package com.github.luckywheel.data

import com.github.luckywheel.domain.ThemeId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var ds: TestDataStore
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        ds = TestDataStore(tmp.newFile("settings.preferences_pb").also { it.delete() })
        repo = SettingsRepository(ds.store)
    }

    @After
    fun tearDown() = ds.close()

    @Test
    fun defaults_matchPrd() = runTest {
        assertEquals(
            AppSettings(
                themeId = ThemeId.DEFAULT,
                logoPath = null,
                customBackgroundPath = null,
                showThemeDecorations = true,
                soundEnabled = true,
                resultDisplaySeconds = 10,
                keepScreenOn = true,
            ),
            repo.settings.first(),
        )
    }

    @Test
    fun update_roundTrips() = runTest {
        val changed = AppSettings(
            themeId = ThemeId.HALLOWEEN,
            logoPath = "/data/logo.png",
            customBackgroundPath = "/data/bg.jpg",
            showThemeDecorations = false,
            soundEnabled = false,
            resultDisplaySeconds = 42,
            keepScreenOn = false,
        )
        repo.update { changed }
        assertEquals(changed, repo.settings.first())
        repo.update { it.copy(logoPath = null, customBackgroundPath = null) }
        assertEquals(changed.copy(logoPath = null, customBackgroundPath = null), SettingsRepository(ds.restart()).settings.first())
    }

    @Test
    fun resultSeconds_clampedTo3to60() = runTest {
        repo.update { it.copy(resultDisplaySeconds = 1) }
        assertEquals(3, repo.settings.first().resultDisplaySeconds)
        repo.update { it.copy(resultDisplaySeconds = 99) }
        assertEquals(60, repo.settings.first().resultDisplaySeconds)
    }
}
