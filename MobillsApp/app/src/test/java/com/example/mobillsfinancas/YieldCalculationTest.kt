package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YieldCalculationTest {

    @Test
    fun testMercadoPagoAccountDailyYield() {
        // Saldo R$ 96,85 com 105% do CDI (taxa oficial da Conta Mercado Pago)
        val account = AccountItem(
            id = "acc_mp",
            name = "Mercado Pago",
            balance = 96.85,
            cdiRatePercentage = 105.0
        )

        val dailyYield = account.calculateDailyYield()
        // Deve bater com os R$ 0,05 creditados no extrato do Mercado Pago
        assertEquals(0.05, dailyYield, 0.01)
    }

    @Test
    fun testMercadoPagoCofrinhoDailyYield() {
        // Saldo R$ 192,70 com 115% do CDI (taxa oficial de Cofrinho Mercado Pago)
        val cofrinho = AccountItem(
            id = "acc_cofrinho",
            name = "Cofrinho MP",
            balance = 192.70,
            cdiRatePercentage = 115.0
        )

        val dailyYield = cofrinho.calculateDailyYield()
        // R$ 192,70 * 115% CDI deve render ~R$ 0,11 a 0,12 por dia útil
        assertTrue(dailyYield >= 0.11 && dailyYield <= 0.12)
    }

    @Test
    fun testZeroYieldForZeroBalanceOrCdi() {
        val accZeroBal = AccountItem(id = "1", name = "Test", balance = 0.0, cdiRatePercentage = 115.0)
        assertEquals(0.0, accZeroBal.calculateDailyYield(), 0.0)

        val accZeroCdi = AccountItem(id = "2", name = "Test", balance = 1000.0, cdiRatePercentage = 0.0)
        assertEquals(0.0, accZeroCdi.calculateDailyYield(), 0.0)
    }
}
