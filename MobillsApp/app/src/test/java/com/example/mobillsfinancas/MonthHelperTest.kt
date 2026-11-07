package com.example.mobillsfinancas

import com.example.mobillsfinancas.utils.MonthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MonthHelperTest {

    @Test
    fun testGetCurrentMonthYearMatchesCurrentCalendar() {
        val cal = Calendar.getInstance()
        val currentMonthStr = MonthHelper.getCurrentMonthYear()
        val parsed = MonthHelper.parseMonthYear(currentMonthStr)

        assertNotNull("O mês retornado deve ser parseável", parsed)
        assertEquals("Mês retornado deve bater com o Calendar atual", cal.get(Calendar.MONTH), parsed!!.first)
        assertEquals("Ano retornado deve bater com o Calendar atual", cal.get(Calendar.YEAR), parsed.second)
    }

    @Test
    fun testFormatMonthYear() {
        assertEquals("OUTUBRO 2026", MonthHelper.formatMonthYear(9, 2026))
        assertEquals("NOVEMBRO 2026", MonthHelper.formatMonthYear(10, 2026))
        assertEquals("DEZEMBRO 2026", MonthHelper.formatMonthYear(11, 2026))
        assertEquals("JANEIRO 2027", MonthHelper.formatMonthYear(0, 2027))
    }

    @Test
    fun testAddMonthsNavigation() {
        assertEquals("NOVEMBRO 2026", MonthHelper.addMonths("OUTUBRO 2026", 1))
        assertEquals("OUTUBRO 2026", MonthHelper.addMonths("NOVEMBRO 2026", -1))
        assertEquals("JANEIRO 2027", MonthHelper.addMonths("NOVEMBRO 2026", 2))
    }
}
