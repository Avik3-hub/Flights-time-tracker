package com.example.flightlog.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {
    @Test fun preservesLegacyDarkPreference() {
        assertEquals(AppTheme.AMOLED, AppTheme.fromPreferences(null, true))
        assertEquals(AppTheme.CLASSIC, AppTheme.fromPreferences(null, false))
    }

    @Test fun explicitChoiceWinsOverOldSetting() {
        assertEquals(AppTheme.BLUE, AppTheme.fromPreferences("BLUE", true))
        assertEquals(AppTheme.CLASSIC, AppTheme.fromPreferences("CLASSIC", true))
        assertEquals(AppTheme.SYSTEM, AppTheme.fromPreferences("SYSTEM", false))
        assertEquals(AppTheme.AMOLED, AppTheme.fromPreferences("AMOLED", false))
    }

    @Test fun unknownSavedChoiceHasSafeFallback() {
        assertEquals(AppTheme.AMOLED, AppTheme.fromPreferences("obsolete", true))
        assertEquals(AppTheme.CLASSIC, AppTheme.fromPreferences("", false))
    }

    @Test fun onlySystemChoiceTracksAndroidNightMode() {
        assertEquals(AppTheme.BLUE, AppTheme.SYSTEM.resolve(false))
        assertEquals(AppTheme.AMOLED, AppTheme.SYSTEM.resolve(true))
        for (theme in listOf(AppTheme.CLASSIC, AppTheme.BLUE, AppTheme.AMOLED)) {
            assertEquals(theme, theme.resolve(false))
            assertEquals(theme, theme.resolve(true))
        }
    }
}
