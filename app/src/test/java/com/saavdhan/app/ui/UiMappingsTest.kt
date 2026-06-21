package com.saavdhan.app.ui

import androidx.compose.ui.graphics.Color
import com.saavdhan.app.domain.model.RiskLevel
import com.saavdhan.app.ui.theme.RiskCriticalDark
import com.saavdhan.app.ui.theme.RiskCriticalLight
import org.junit.Assert.assertEquals
import org.junit.Test

class UiMappingsTest {

    @Test
    fun testColorForTheme_lightMode() {
        assertEquals(RiskCriticalLight, RiskLevel.CRITICAL.colorForTheme(isDark = false))
        assertEquals(Color.White, RiskLevel.CRITICAL.onColorForTheme(isDark = false))

        // Explicitly test SUSPICIOUS contrast in light mode
        assertEquals(Color(0xFF212121), RiskLevel.SUSPICIOUS.onColorForTheme(isDark = false))
    }

    @Test
    fun testColorForTheme_darkMode() {
        // In dark mode, it should use the pastel variants for readability
        assertEquals(RiskCriticalDark, RiskLevel.CRITICAL.colorForTheme(isDark = true))
        // And dark text for contrast against the bright pastel
        assertEquals(Color(0xFF212121), RiskLevel.CRITICAL.onColorForTheme(isDark = true))

        // SUSPICIOUS must also use dark text in dark mode
        assertEquals(Color(0xFF212121), RiskLevel.SUSPICIOUS.onColorForTheme(isDark = true))
    }
}
