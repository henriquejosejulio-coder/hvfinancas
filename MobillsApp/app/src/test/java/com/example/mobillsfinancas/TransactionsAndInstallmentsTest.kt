package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.utils.MonthHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsAndInstallmentsTest {

    @Test
    fun testExpenseStatusPaidVsPending() {
        val paidExpense = TransactionItem(
            id = "tx_paid",
            description = "Luz",
            amount = 200.0,
            type = TransactionType.EXPENSE,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026"
        )
        val pendingExpense = TransactionItem(
            id = "tx_pending",
            description = "Aluguel",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026"
        )

        assertEquals(PaymentStatus.PAID, paidExpense.status)
        assertEquals(PaymentStatus.PENDING, pendingExpense.status)
    }

    @Test
    fun testFixedExpenseFlag() {
        val fixedExpense = TransactionItem(
            id = "tx_fixed",
            description = "Internet Imicro",
            amount = 79.90,
            type = TransactionType.EXPENSE,
            isRecurring = true,
            monthYear = "NOVEMBRO 2026"
        )
        assertTrue(fixedExpense.isRecurring)
    }

    @Test
    fun testInstallmentGenerationAcrossSuccessiveMonths() {
        val totalAmount = 300.0
        val totalInstallments = 3
        val installmentAmount = Math.round((totalAmount / totalInstallments) * 100.0) / 100.0
        val baseDescription = "Tênis Novo"
        val startMonth = "NOVEMBRO 2026"

        val generatedItems = mutableListOf<TransactionItem>()
        for (i in 1..totalInstallments) {
            val targetMonth = MonthHelper.addMonths(startMonth, i - 1)
            generatedItems.add(
                TransactionItem(
                    id = "inst_$i",
                    description = "$baseDescription ($i/$totalInstallments)",
                    amount = installmentAmount,
                    type = TransactionType.EXPENSE,
                    paymentType = PaymentType.CREDIT_CARD,
                    monthYear = targetMonth,
                    currentInstallment = i,
                    totalInstallments = totalInstallments
                )
            )
        }

        assertEquals(3, generatedItems.size)
        assertEquals("Tênis Novo (1/3)", generatedItems[0].description)
        assertEquals("NOVEMBRO 2026", generatedItems[0].monthYear)
        assertEquals(100.0, generatedItems[0].amount, 0.001)

        assertEquals("Tênis Novo (2/3)", generatedItems[1].description)
        assertEquals("DEZEMBRO 2026", generatedItems[1].monthYear)
        assertEquals(100.0, generatedItems[1].amount, 0.001)

        assertEquals("Tênis Novo (3/3)", generatedItems[2].description)
        assertEquals("JANEIRO 2027", generatedItems[2].monthYear)
        assertEquals(100.0, generatedItems[2].amount, 0.001)
    }

    @Test
    fun testSalarySumWithMultipleReceiptsAndVacation() {
        val salarioMensal = TransactionItem(
            id = "sal_1",
            description = "salário Go up",
            amount = 2395.76,
            type = TransactionType.INCOME,
            isSalaryReplacement = true,
            monthYear = "NOVEMBRO 2026"
        )
        val feriasPix = TransactionItem(
            id = "sal_2",
            description = "Pix de Go Up Sistemas",
            amount = 2198.46,
            type = TransactionType.INCOME,
            isSalaryReplacement = true,
            monthYear = "NOVEMBRO 2026"
        )
        val arrighi = TransactionItem(
            id = "sal_3",
            description = "Pix de Arrighi C D Ltda",
            amount = 400.0,
            type = TransactionType.INCOME,
            isSalaryReplacement = true,
            monthYear = "NOVEMBRO 2026"
        )
        val extra = TransactionItem(
            id = "ext_1",
            description = "Pix Recebido",
            amount = 100.0,
            type = TransactionType.INCOME,
            isSalaryReplacement = false,
            monthYear = "NOVEMBRO 2026"
        )

        val items = listOf(salarioMensal, feriasPix, arrighi, extra)

        // Salários e férias somados na base
        val totalSalarioBase = items.filter { it.type == TransactionType.INCOME && it.isSalaryReplacement }.sumOf { it.amount }
        assertEquals(4994.22, totalSalarioBase, 0.001)

        // Receitas extras
        val totalExtra = items.filter { it.type == TransactionType.INCOME && !it.isSalaryReplacement }.sumOf { it.amount }
        assertEquals(100.0, totalExtra, 0.001)

        // Receita total do mês
        val receitaTotal = totalSalarioBase + totalExtra
        assertEquals(5094.22, receitaTotal, 0.001)
    }
}
