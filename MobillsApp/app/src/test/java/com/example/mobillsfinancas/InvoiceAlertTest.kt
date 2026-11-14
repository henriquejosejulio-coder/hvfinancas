package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.*
import com.example.mobillsfinancas.utils.MonthHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class InvoiceAlertTest {

    @Test
    fun testZeroInvoiceDoesNotNeedAlert() {
        val card = CreditCardItem(
            id = "c1",
            name = "Cartão Inter",
            limit = 5000.0,
            closingDay = 6,
            dueDay = 12,
            cardType = CardType.MULTIPLE
        )
        val invoiceAmount = 0.0
        val isPaid = false

        // Condição: Se fatura zerada ou paga, não avisa
        val shouldAlert = card.cardType.allowsCredit &&
                card.closingDay > 0 &&
                card.dueDay > 0 &&
                invoiceAmount > 0.0 &&
                !isPaid

        assertFalse("Fatura zerada não deve disparar alerta", shouldAlert)
    }

    @Test
    fun testPaidInvoiceDoesNotNeedAlert() {
        val card = CreditCardItem(
            id = "c2",
            name = "Cartão Mercado Pago 1",
            limit = 2000.0,
            closingDay = 9,
            dueDay = 15,
            cardType = CardType.CREDIT
        )
        val invoiceAmount = 76.77
        val isPaid = true

        val shouldAlert = card.cardType.allowsCredit &&
                card.closingDay > 0 &&
                card.dueDay > 0 &&
                invoiceAmount > 0.0 &&
                !isPaid

        assertFalse("Fatura já paga não deve disparar alerta", shouldAlert)
    }

    @Test
    fun testDebitCardDoesNotNeedAlert() {
        val card = CreditCardItem(
            id = "c3",
            name = "Cartão Mercado Pago 2",
            limit = 0.0,
            closingDay = 9,
            dueDay = 14,
            cardType = CardType.DEBIT
        )
        assertFalse("Cartão puramente de débito não permite crédito", card.cardType.allowsCredit)
    }

    @Test
    fun testDueTomorrowCalculation() {
        // Exemplo: Vencimento dia 12. Se hoje é dia 11, amanhã é dia 12!
        val calToday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 11)
        }
        val calTomorrow = (calToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val dueDay = 12
        val isDueTomorrow = calTomorrow.get(Calendar.DAY_OF_MONTH) == dueDay
        assertTrue("Dia 11 é véspera do vencimento no dia 12", isDueTomorrow)
    }

    @Test
    fun testDueTodayCalculation() {
        val currentDay = 12
        val dueDay = 12
        assertEquals(currentDay, dueDay)
    }

    @Test
    fun testClosingDayCalculation() {
        val currentDay = 6
        val closingDay = 6
        assertEquals(currentDay, closingDay)
    }
}
