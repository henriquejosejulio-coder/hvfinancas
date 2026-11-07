package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.CreditCardItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountsAndCardsTest {

    @Test
    fun testCreateCheckingAccountAndPiggyBank() {
        val checking = AccountItem(
            id = "acc_1",
            name = "Banco Inter",
            balance = 1500.0,
            includeInOverallBalance = true,
            isDefault = true
        )
        assertTrue(checking.includeInOverallBalance)
        assertTrue(checking.isDefault)
        assertEquals(1500.0, checking.balance, 0.001)

        val piggyBank = AccountItem(
            id = "acc_piggy",
            name = "Cofrinho 115% CDI",
            balance = 5000.0,
            includeInOverallBalance = false,
            cdiRatePercentage = 115.0
        )
        assertFalse(piggyBank.includeInOverallBalance)
        assertEquals(115.0, piggyBank.cdiRatePercentage, 0.001)
    }

    @Test
    fun testCreateCreditCardWithLimitsAndBillingCycle() {
        val card = CreditCardItem(
            id = "card_inter",
            name = "Cartão Inter Black",
            limit = 10000.0,
            closingDay = 6,
            dueDay = 15,
            linkedAccountId = "acc_inter"
        )

        assertEquals("Cartão Inter Black", card.name)
        assertEquals(10000.0, card.limit, 0.001)
        assertEquals(6, card.closingDay)
        assertEquals(15, card.dueDay)
        assertEquals("acc_inter", card.linkedAccountId)
    }

    @Test
    fun testDirectAccountBalanceAdjustment() {
        var account = AccountItem(
            id = "acc_mp",
            name = "Mercado Pago 2",
            balance = 173.92,
            includeInOverallBalance = true
        )

        // Usuário realizou acerto de saldo para R$ 250,00
        val novoSaldo = 250.0
        account = account.copy(balance = novoSaldo)

        assertEquals(250.0, account.balance, 0.001)
    }

    @Test
    fun testYieldCalculationForMercadoPago115Cdi() {
        // Cofrinho com R$ 10.000,00 rendendo 115% do CDI
        // CDI atual de 14.15% a.a. -> taxa efetiva = 14.15% * 1.15 = 16.2725% a.a.
        val account = AccountItem(
            id = "acc_cofrinho",
            name = "Cofrinho Reserva",
            balance = 10000.0,
            cdiRatePercentage = 115.0
        )

        val dailyYield = account.calculateDailyYield()
        val monthlyYield = account.calculateMonthlyYield()

        // R$ 10.000 a ~16.27% a.a. rende cerca de ~R$ 6,00 a R$ 6,50 por dia útil e ~R$ 120 a R$ 130 por mês
        assertTrue("Rendimento diário deve ser positivo", dailyYield > 0.0)
        assertTrue("Rendimento mensal deve ser positivo", monthlyYield > 0.0)
        assertEquals(dailyYield * 21.0, monthlyYield, 25.0)
    }
}
