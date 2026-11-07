package com.example.mobillsfinancas

import com.example.mobillsfinancas.utils.MonthHelper
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentsAndInvoiceTest {

    @Test
    fun testTargetInvoiceMonthBeforeAndAfterClosingDay() {
        // Dia de fechamento: 6
        // Compra no dia 3 de Novembro -> deve cair em NOVEMBRO 2026
        assertEquals("NOVEMBRO 2026", MonthHelper.getTargetInvoiceMonth("03/11/2026", 6))

        // Compra no dia 6 de Novembro (dia do fechamento) -> ainda cai em NOVEMBRO 2026
        assertEquals("NOVEMBRO 2026", MonthHelper.getTargetInvoiceMonth("06/11/2026", 6))

        // Compra no dia 7 de Novembro (após o fechamento) -> fatura fechou, cai no mês seguinte: DEZEMBRO 2026
        assertEquals("DEZEMBRO 2026", MonthHelper.getTargetInvoiceMonth("07/11/2026", 6))

        // Compra no final de Dezembro (após dia 6) -> virada de ano para JANEIRO 2027!
        assertEquals("JANEIRO 2027", MonthHelper.getTargetInvoiceMonth("25/12/2026", 6))
    }

    @Test
    fun testInstallmentAmountCalculations() {
        // Compra total de R$ 1.200,00 parcelada em 10x
        val total = 1200.0
        val installments = 10
        val perInstallment = Math.round((total / installments) * 100.0) / 100.0
        assertEquals(120.0, perInstallment, 0.001)

        // Compra com dízima: R$ 100,00 em 3x = R$ 33,33
        val totalImpar = 100.0
        val parcelado3x = Math.round((totalImpar / 3) * 100.0) / 100.0
        assertEquals(33.33, parcelado3x, 0.001)
    }

    @Test
    fun testMonthHelperSorting() {
        val unsorted = listOf("DEZEMBRO 2026", "OUTUBRO 2026", "JANEIRO 2027", "NOVEMBRO 2026")
        val sorted = MonthHelper.sortMonths(unsorted)
        val expected = listOf("OUTUBRO 2026", "NOVEMBRO 2026", "DEZEMBRO 2026", "JANEIRO 2027")
        assertEquals(expected, sorted)
    }
}
