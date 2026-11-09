package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.CreditCardItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.data.isInvoicePaidForCard
import com.example.mobillsfinancas.data.isInvoicePayment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoicePaymentTest {

    @Test
    fun testCreditCardInvoiceCalculationAndPayment() {
        var checkingAccount = AccountItem(
            id = "acc_inter",
            name = "Conta Inter",
            balance = 4794.22,
            includeInOverallBalance = true
        )

        val card = CreditCardItem(
            id = "card_inter",
            name = "Cartão Inter",
            limit = 5000.0,
            closingDay = 6,
            dueDay = 15
        )

        // Compras no cartão no mês de NOVEMBRO 2026
        val item1 = TransactionItem(
            id = "tx_1",
            description = "Supermercado Armação",
            amount = 93.56,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = card.name,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026"
        )
        val item2 = TransactionItem(
            id = "tx_2",
            description = "Google YouTube",
            amount = 26.90,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = card.name,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026"
        )

        val cardItems = listOf(item1, item2)

        // Calcula total da fatura
        val invoiceTotal = cardItems.sumOf { it.amount }
        assertEquals(120.46, invoiceTotal, 0.001)

        // Antes do pagamento, fatura não está paga e consome o limite
        assertFalse(isInvoicePaidForCard(card, cardItems, cardItems))
        assertEquals(5000.0 - 120.46, card.limit - invoiceTotal, 0.001)

        // Executa pagamento de fatura:
        // 1. Debita da conta corrente
        checkingAccount = checkingAccount.copy(balance = checkingAccount.balance - invoiceTotal)
        assertEquals(4673.76, checkingAccount.balance, 0.001)

        // 2. Marca itens da fatura como PAID
        val updatedCardItems = cardItems.map { it.copy(status = PaymentStatus.PAID) }
        assertTrue(updatedCardItems.all { it.status == PaymentStatus.PAID })

        // 3. Registra comprovante do pagamento de fatura FORA DO TETO (includeInBudget = false)
        val paymentReceipt = TransactionItem(
            id = "tx_pay_invoice",
            description = "Pagamento de Fatura - ${card.name}",
            amount = invoiceTotal,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.DEBIT_CARD,
            accountOrCardName = checkingAccount.name,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026",
            includeInBudget = false // Não duplica despesas
        )

        assertEquals("Pagamento de Fatura - Cartão Inter", paymentReceipt.description)
        assertEquals(120.46, paymentReceipt.amount, 0.001)
        assertEquals(PaymentStatus.PAID, paymentReceipt.status)
        assertFalse(paymentReceipt.includeInBudget)
        assertTrue(isInvoicePayment(paymentReceipt))

        // 4. Verifica status de fatura paga na tela de cartões
        val allMonthItems = listOf(paymentReceipt) + updatedCardItems
        val isPaid = isInvoicePaidForCard(card, allMonthItems, updatedCardItems)
        assertTrue(isPaid)

        // 5. Restauração do limite disponível após pagamento da fatura
        val pendingPurchases = updatedCardItems.filter { it.status != PaymentStatus.PAID }.sumOf { it.amount }
        val consumedLimit = if (isPaid) pendingPurchases else invoiceTotal
        val availableLimit = (card.limit - consumedLimit).coerceAtLeast(0.0)
        assertEquals(5000.0, availableLimit, 0.001) // Limite 100% restaurado!

        // 6. TOTAL DESPESAS DO MÊS: O pagamento de fatura NÃO duplica as despesas no teto!
        val totalMonthlyExpense = allMonthItems.filter {
            (it.type == TransactionType.EXPENSE && it.includeInBudget && !isInvoicePayment(it)) ||
            (it.type == TransactionType.TRANSFER && it.includeInBudget)
        }.sumOf { it.amount }
        // Deve ser exatamente R$ 120,46 (apenas as compras), NUNCA R$ 240,92!
        assertEquals(120.46, totalMonthlyExpense, 0.001)
    }
}
