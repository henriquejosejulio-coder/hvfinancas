package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.CategoryItem
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesAndBudgetsFullTest {

    @Test
    fun testCategoryCreationAndBudgetLimit() {
        val catMercado = CategoryItem(
            id = "cat_merc",
            name = "Mercado",
            iconEmoji = "🛒",
            colorHex = 0xFF4CAF50,
            isExpense = true,
            budgetLimit = 850.0,
            reserveInBudget = true
        )
        val catTransporte = CategoryItem(
            id = "cat_tran",
            name = "Transporte",
            iconEmoji = "🚗",
            colorHex = 0xFF2196F3,
            isExpense = true,
            budgetLimit = 200.0,
            reserveInBudget = true
        )

        assertEquals(850.0, catMercado.budgetLimit, 0.001)
        assertTrue(catMercado.reserveInBudget)
        assertEquals(200.0, catTransporte.budgetLimit, 0.001)

        val totalTetos = listOf(catMercado, catTransporte).filter { it.reserveInBudget }.sumOf { it.budgetLimit }
        assertEquals(1050.0, totalTetos, 0.001)
    }

    @Test
    fun testBudgetConsumptionByExpenses() {
        val catMercado = CategoryItem(
            id = "cat_merc",
            name = "Mercado",
            iconEmoji = "🛒",
            colorHex = 0xFF4CAF50,
            budgetLimit = 850.0
        )

        val compra1 = TransactionItem(
            id = "c1",
            description = "Supermercado Armação",
            amount = 93.56,
            type = TransactionType.EXPENSE,
            category = catMercado.name,
            monthYear = "NOVEMBRO 2026"
        )
        val compra2 = TransactionItem(
            id = "c2",
            description = "Supermercados BH",
            amount = 39.59,
            type = TransactionType.EXPENSE,
            category = catMercado.name,
            monthYear = "NOVEMBRO 2026"
        )

        val gastosMercado = listOf(compra1, compra2).filter { it.category == catMercado.name }.sumOf { it.amount }
        assertEquals(133.15, gastosMercado, 0.001)

        val saldoRestanteDoTeto = catMercado.budgetLimit - gastosMercado
        assertEquals(716.85, saldoRestanteDoTeto, 0.001)
    }
}
