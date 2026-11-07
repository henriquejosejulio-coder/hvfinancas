package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetAndTotalsTest {

    @Test
    fun testBudgetPlanningCalculations() {
        // Receitas do mês: Salário R$ 3.000 + Férias R$ 2.102,22 = R$ 5.102,22
        val salario = TransactionItem(id = "1", description = "Salário", amount = 3000.0, type = TransactionType.INCOME, monthYear = "NOVEMBRO 2026", isSalaryReplacement = true)
        val ferias = TransactionItem(id = "2", description = "Férias", amount = 2102.22, type = TransactionType.INCOME, monthYear = "NOVEMBRO 2026", isSalaryReplacement = true)

        // Despesas fixas e cartão: R$ 1.845,63
        val contasFixas = TransactionItem(id = "3", description = "Contas Fixas e Cartões", amount = 1845.63, type = TransactionType.EXPENSE, monthYear = "NOVEMBRO 2026")

        // Meta Cofrinho / Reserva: R$ 2.000,00 (Transferência orçamentária)
        val cofrinhoReserva = TransactionItem(id = "4", description = "Reserva Cofrinho", amount = 2000.0, type = TransactionType.TRANSFER, monthYear = "NOVEMBRO 2026", includeInBudget = true)

        // Mercado e tetos reservados: R$ 1.050,00
        val mercado = TransactionItem(id = "5", description = "Mercado", amount = 1050.0, type = TransactionType.EXPENSE, monthYear = "NOVEMBRO 2026")

        val items = listOf(salario, ferias, contasFixas, cofrinhoReserva, mercado)

        val totalIncome = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = items.filter { it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.includeInBudget) }.sumOf { it.amount }
        val sobrouLivre = totalIncome - totalExpense

        assertEquals(5102.22, totalIncome, 0.001)
        assertEquals(4895.63, totalExpense, 0.001)
        assertEquals(206.59, sobrouLivre, 0.001)
    }

    @Test
    fun testBudgetPlanningWithOtherAccountsBalance() {
        // Receitas do mês: R$ 5.102,22
        val totalIncome = 5102.22
        // Despesas planejadas (contas + cofrinho + mercado): R$ 4.895,63
        val totalExpense = 4895.63

        // Saldo disponível em outras contas correntes (Mercado Pago 2): R$ 173,92
        val otherAccountsBalance = 173.92

        // Receita total real disponível = Receitas do mês + Saldo livre em outras contas
        val effectiveIncome = totalIncome + otherAccountsBalance
        assertEquals(5276.14, effectiveIncome, 0.001)

        // Sobrou real para gastar comigo:
        val sobrouLivreFinal = effectiveIncome - totalExpense
        assertEquals(380.51, sobrouLivreFinal, 0.001)
    }

    @Test
    fun testTransferBetweenAccountsDoesNotDoubleCountInSobrouParaGastar() {
        val totalIncome = 5102.22
        val totalExpense = 4895.63
        val salarySurplus = totalIncome - totalExpense // 206.59

        // Cenário 1: Antes de transferir (Mercado Pago com saldo inicial R$ 173,92)
        var otherAccountsBalance = 173.92
        var transfersToOther = 0.0
        var duplicateTransfer = if (salarySurplus > 0) minOf(transfersToOther, salarySurplus) else 0.0
        var netOther = (otherAccountsBalance - duplicateTransfer).coerceAtLeast(0.0)
        var freeToSpend = (totalIncome + netOther) - totalExpense
        assertEquals(380.51, freeToSpend, 0.001)

        // Cenário 2: Usuário transfere R$ 206,59 do Inter para o Mercado Pago (para fechar os 380 certinho)
        // Saldo do Mercado Pago sobe para R$ 380,51
        otherAccountsBalance = 173.92 + 206.59 // 380.51
        transfersToOther = 206.59
        duplicateTransfer = if (salarySurplus > 0) minOf(transfersToOther, salarySurplus) else 0.0
        assertEquals(206.59, duplicateTransfer, 0.001)
        netOther = (otherAccountsBalance - duplicateTransfer).coerceAtLeast(0.0)
        assertEquals(173.92, netOther, 0.001)
        freeToSpend = (totalIncome + netOther) - totalExpense
        // DEVE CONTINUAR EXATAMENTE R$ 380,51 (e NUNCA R$ 587,10)
        assertEquals(380.51, freeToSpend, 0.001)

        // Cenário 3: Usuário transfere parcialmente R$ 100,00
        otherAccountsBalance = 173.92 + 100.0 // 273.92
        transfersToOther = 100.0
        duplicateTransfer = if (salarySurplus > 0) minOf(transfersToOther, salarySurplus) else 0.0
        netOther = (otherAccountsBalance - duplicateTransfer).coerceAtLeast(0.0)
        freeToSpend = (totalIncome + netOther) - totalExpense
        assertEquals(380.51, freeToSpend, 0.001)
    }

    @Test
    fun testBankAvailableBalanceCalculation() {
        // Conta Inter: R$ 4.794,22
        val saldoInter = 4794.22
        // Mercado Pago: R$ 173,92
        val saldoMp = 173.92
        val totalContas = saldoInter + saldoMp // R$ 4.968,14

        // Compromisso pendente com cofrinho / reserva: R$ 2.000,00
        val pendingCofrinho = 2000.0

        val saldoLivreBancos = (totalContas - pendingCofrinho).coerceAtLeast(0.0)

        assertEquals(4968.14, totalContas, 0.001)
        assertEquals(2968.14, saldoLivreBancos, 0.001)
    }
}
