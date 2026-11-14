package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YieldCalculationTest {

    @Test
    fun testMercadoPagoAccountDailyYieldMatchesStatement() {
        // Saldo real de R$ 368,20 com 105% do CDI (Conta Mercado Pago)
        val account = AccountItem(
            id = "acc_mp",
            name = "Mercado Pago 2",
            balance = 368.20,
            cdiRatePercentage = 105.0
        )

        val dailyYield = account.calculateDailyYield()
        // Deve bater exatamente com os R$ 0,15 creditados no extrato real do Mercado Pago
        assertEquals(0.15, dailyYield, 0.001)
    }

    @Test
    fun testMercadoPagoCofrinhoDailyYieldMatchesStatement() {
        // Saldo real de R$ 1.940,96 com 115% do CDI (Cofrinho Mercado Pago)
        val cofrinho = AccountItem(
            id = "acc_cofrinho",
            name = "Cofrinho MP 2",
            balance = 1940.96,
            cdiRatePercentage = 115.0
        )

        val dailyYield = cofrinho.calculateDailyYield()
        // Deve bater exatamente com os R$ 0,88 creditados no extrato real do Mercado Pago
        assertEquals(0.88, dailyYield, 0.001)
    }

    @Test
    fun testMercadoPagoAccountDailyYield() {
        // Saldo R$ 96,85 com 105% do CDI
        val account = AccountItem(
            id = "acc_mp",
            name = "Mercado Pago",
            balance = 96.85,
            cdiRatePercentage = 105.0
        )

        val dailyYield = account.calculateDailyYield()
        assertEquals(0.04, dailyYield, 0.01)
    }

    @Test
    fun testMercadoPagoCofrinhoDailyYield() {
        // Saldo R$ 192,70 com 115% do CDI
        val cofrinho = AccountItem(
            id = "acc_cofrinho",
            name = "Cofrinho MP",
            balance = 192.70,
            cdiRatePercentage = 115.0
        )

        val dailyYield = cofrinho.calculateDailyYield()
        assertEquals(0.09, dailyYield, 0.01)
    }

    @Test
    fun testZeroYieldForZeroBalanceOrCdi() {
        val accZeroBal = AccountItem(id = "1", name = "Test", balance = 0.0, cdiRatePercentage = 115.0)
        assertEquals(0.0, accZeroBal.calculateDailyYield(), 0.0)

        val accZeroCdi = AccountItem(id = "2", name = "Test", balance = 1000.0, cdiRatePercentage = 0.0)
        assertEquals(0.0, accZeroCdi.calculateDailyYield(), 0.0)
    }

    @Test
    fun testBcbCdiServiceNetFormula() {
        // Testa a função utilitária do BcbCdiService com a taxa diária oficial
        val netCofrinho = com.example.mobillsfinancas.services.BcbCdiService.calculateDailyNetYield(
            balance = 1940.96,
            cdiRatePercentage = 115.0,
            dailyCdiRatePercent = 0.050788,
            taxRatePercentage = 22.5
        )
        assertEquals(0.88, netCofrinho, 0.001)

        val netContaLivre = com.example.mobillsfinancas.services.BcbCdiService.calculateDailyNetYield(
            balance = 368.20,
            cdiRatePercentage = 105.0,
            dailyCdiRatePercent = 0.050788,
            taxRatePercentage = 22.5
        )
        assertEquals(0.15, netContaLivre, 0.001)
    }
}
