package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class SalaryAndVacationsTest {

    @Test
    fun testTwoSalaryItemsAreBothSummed() {
        val salario = TransactionItem(
            id = "1",
            description = "Salário do Mês",
            amount = 3000.0,
            type = TransactionType.INCOME,
            monthYear = "NOVEMBRO 2026",
            isSalaryReplacement = true
        )
        val ferias = TransactionItem(
            id = "2",
            description = "Férias",
            amount = 2500.0,
            type = TransactionType.INCOME,
            monthYear = "NOVEMBRO 2026",
            isSalaryReplacement = true
        )

        val items = listOf(salario, ferias)

        val salaryItems = items.filter { it.isSalaryReplacement && it.type == TransactionType.INCOME }
        val totalSalaryItems = salaryItems.sumOf { it.amount }
        val baseIncome = 3300.0
        val effectiveBaseInc = if (totalSalaryItems > 0) totalSalaryItems else baseIncome

        var extraInc = 0.0
        for (item in items) {
            if (item.type == TransactionType.INCOME && !item.isSalaryReplacement) {
                extraInc += item.amount
            }
        }

        val totalMonthIncome = effectiveBaseInc + extraInc

        // Deve somar R$ 3.000 + R$ 2.500 = R$ 5.500 (nenhum é descartado!)
        assertEquals(5500.0, totalMonthIncome, 0.001)
        assertEquals(5500.0, effectiveBaseInc, 0.001)
    }

    @Test
    fun testSalaryPlusExtraIncomeAreBothSummed() {
        val salario = TransactionItem(
            id = "1",
            description = "Salário do Mês",
            amount = 3000.0,
            type = TransactionType.INCOME,
            monthYear = "NOVEMBRO 2026",
            isSalaryReplacement = true
        )
        val feriasExtra = TransactionItem(
            id = "2",
            description = "Férias (Extra)",
            amount = 2500.0,
            type = TransactionType.INCOME,
            monthYear = "NOVEMBRO 2026",
            isSalaryReplacement = false
        )

        val items = listOf(salario, feriasExtra)

        val salaryItems = items.filter { it.isSalaryReplacement && it.type == TransactionType.INCOME }
        val totalSalaryItems = salaryItems.sumOf { it.amount }
        val effectiveBaseInc = if (totalSalaryItems > 0) totalSalaryItems else 3300.0

        var extraInc = 0.0
        for (item in items) {
            if (item.type == TransactionType.INCOME && !item.isSalaryReplacement) {
                extraInc += item.amount
            }
        }

        val totalMonthIncome = effectiveBaseInc + extraInc

        // Em ambos os casos deve dar exatamente R$ 5.500
        assertEquals(5500.0, totalMonthIncome, 0.001)
    }
}
