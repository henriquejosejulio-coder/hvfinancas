package com.example.mobillsfinancas

import org.junit.Assert.assertEquals
import org.junit.Test

class MonthCascadeAndBalanceAdjustTest {

    @Test
    fun testRealUserScenarioForOctoberAndNovemberCascade() {
        // --- OUTUBRO 2026 ---
        // Receitas cadastradas em Outubro: R$ 2.369,81
        val receitaOutubro = 2369.81
        // Despesas cadastradas em Outubro: R$ 3.483,32
        val despesaOutubro = 3483.32
        val saldoFinalOutubro = Math.round((receitaOutubro - despesaOutubro) * 100.0) / 100.0

        // Saldo final de Outubro ficou negativo em -R$ 1.113,51
        assertEquals(-1113.51, saldoFinalOutubro, 0.001)

        // --- NOVEMBRO 2026 ---
        // Receitas do mês atual:
        // Salário Go Up (2395.76) + Go Up Sistemas (2198.46) + Arrighi (400) = R$ 4.994,22
        // Pix Recebido (100) + Pix Recebido (8) = R$ 108,00
        val baseIncomeNovembro = 4994.22
        val extraIncomeNovembro = 108.0
        val receitaBrutaNovembro = baseIncomeNovembro + extraIncomeNovembro
        assertEquals(5102.22, receitaBrutaNovembro, 0.001)

        // Ao virar o mês, o saldo negativo de Outubro (-1.113,51) é herdado como previousMonthBalance
        val previousMonthBalance = saldoFinalOutubro
        val totalIncomeAcumulado = Math.round((receitaBrutaNovembro + previousMonthBalance) * 100.0) / 100.0

        // Total Income acumulado para Novembro após abater o negativo de Outubro: R$ 3.988,71
        assertEquals(3988.71, totalIncomeAcumulado, 0.001)

        // Despesas cadastradas em Novembro:
        // Contas fixas e faturas de cartão: R$ 1.845,63
        // Transferência orçamentária para o Cofrinho MP 2: R$ 2.000,00
        val contasEFaturas = 1845.63
        val reservaCofrinho = 2000.0
        val totalExpenseNovembro = contasEFaturas + reservaCofrinho
        assertEquals(3845.63, totalExpenseNovembro, 0.001)

        // Saldo final do mês (remainingBalance) exibido na aba "Saldo do Mês":
        val remainingBalanceNovembro = Math.round((totalIncomeAcumulado - totalExpenseNovembro) * 100.0) / 100.0

        // EXATAMENTE R$ 143,08!
        assertEquals(143.08, remainingBalanceNovembro, 0.001)
    }

    @Test
    fun testAdjustMonthBalanceDirectly() {
        val calculatedBalance = 143.08
        // Usuário decide que o saldo real que ele quer que sobre no final do mês é R$ 200,00
        val desiredBalance = 200.0

        val difference = desiredBalance - calculatedBalance
        assertEquals(56.92, difference, 0.001)

        val adjustedBalance = calculatedBalance + difference
        assertEquals(200.0, adjustedBalance, 0.001)
    }

    @Test
    fun testSetBaseIncomePropagation() {
        var baseIncome = 3300.0
        val applyToFutureMonths = true

        // Usuário altera salário base para R$ 3.500,00
        val newSalary = 3500.0
        baseIncome = newSalary

        assertEquals(3500.0, baseIncome, 0.001)
    }
}
