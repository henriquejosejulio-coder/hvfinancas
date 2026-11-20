package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AuditLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AuditFilterTest {

    @Test
    fun testFilterByToday() {
        val now = System.currentTimeMillis()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val logToday = AuditLogEntry(
            id = "1",
            timestamp = now,
            dateTimeStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(now)),
            action = "CRIOU_LANCAMENTO",
            summary = "Compra Mercado"
        )
        val logYesterday = AuditLogEntry(
            id = "2",
            timestamp = now - (26 * 60 * 60 * 1000L),
            dateTimeStr = "12/11/2026 10:00:00",
            action = "CRIOU_LANCAMENTO",
            summary = "Compra Farmacia"
        )

        val list = listOf(logToday, logYesterday)
        val filteredToday = list.filter { it.timestamp >= startOfToday }

        assertEquals(1, filteredToday.size)
        assertEquals("Compra Mercado", filteredToday[0].summary)
    }

    @Test
    fun testFilterByYesterday() {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val now = System.currentTimeMillis()
        val logToday = AuditLogEntry(id = "1", timestamp = now, summary = "Hoje")
        val logYesterday = AuditLogEntry(id = "2", timestamp = startOfYesterday + 3600000L, summary = "Ontem")
        val logOld = AuditLogEntry(id = "3", timestamp = startOfYesterday - 3600000L, summary = "Anteontem")

        val list = listOf(logToday, logYesterday, logOld)
        val filteredYesterday = list.filter { it.timestamp in startOfYesterday until startOfToday }

        assertEquals(1, filteredYesterday.size)
        assertEquals("Ontem", filteredYesterday[0].summary)
    }

    @Test
    fun testFilterBySevenDays() {
        val startOfSevenDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val now = System.currentTimeMillis()
        val logToday = AuditLogEntry(id = "1", timestamp = now, summary = "Hoje")
        val log3DaysAgo = AuditLogEntry(id = "2", timestamp = now - (3 * 86400000L), summary = "3 dias atras")
        val log15DaysAgo = AuditLogEntry(id = "3", timestamp = now - (15 * 86400000L), summary = "15 dias atras")

        val list = listOf(logToday, log3DaysAgo, log15DaysAgo)
        val filtered = list.filter { it.timestamp >= startOfSevenDays }

        assertEquals(2, filtered.size)
    }

    @Test
    fun testFilterByCustomDateRange() {
        val startMs = com.example.mobillsfinancas.ui.screens.parseDateToStartOfDayMs("10/11/2026")!!
        val endMs = com.example.mobillsfinancas.ui.screens.parseDateToEndOfDayMs("15/11/2026")!!

        val logBefore = AuditLogEntry(id = "1", timestamp = com.example.mobillsfinancas.ui.screens.parseDateToStartOfDayMs("09/11/2026")!!, summary = "Antes")
        val logStart = AuditLogEntry(id = "2", timestamp = startMs + 1000L, summary = "No inicio")
        val logMiddle = AuditLogEntry(id = "3", timestamp = com.example.mobillsfinancas.ui.screens.parseDateToStartOfDayMs("12/11/2026")!! + 50000L, summary = "No meio")
        val logEnd = AuditLogEntry(id = "4", timestamp = endMs - 1000L, summary = "No fim")
        val logAfter = AuditLogEntry(id = "5", timestamp = com.example.mobillsfinancas.ui.screens.parseDateToEndOfDayMs("16/11/2026")!!, summary = "Depois")

        val list = listOf(logBefore, logStart, logMiddle, logEnd, logAfter)
        val filtered = list.filter { it.timestamp in startMs..endMs }

        assertEquals(3, filtered.size)
        assertEquals("No inicio", filtered[0].summary)
        assertEquals("No meio", filtered[1].summary)
        assertEquals("No fim", filtered[2].summary)
    }
}
