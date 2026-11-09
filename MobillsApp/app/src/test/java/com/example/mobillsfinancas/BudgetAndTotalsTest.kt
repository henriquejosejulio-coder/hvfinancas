package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
        var duplicateTransfer = if (salarySurplus > 0.0) minOf(transfersToOther, salarySurplus) else 0.0
        var netOther = (otherAccountsBalance - duplicateTransfer).coerceAtLeast(0.0)
        var freeToSpend = (totalIncome + netOther) - totalExpense
        assertEquals(380.51, freeToSpend, 0.001)

        // Cenário 2: Usuário transfere R$ 206,59 do Inter para o Mercado Pago (para fechar os 380 certinho)
        // Saldo do Mercado Pago sobe para R$ 380,51
        otherAccountsBalance = 173.92 + 206.59 // 380.51
        transfersToOther = 206.59
        duplicateTransfer = if (salarySurplus > 0.0) minOf(transfersToOther, salarySurplus) else 0.0
        assertEquals(206.59, duplicateTransfer, 0.001)
        netOther = (otherAccountsBalance - duplicateTransfer).coerceAtLeast(0.0)
        assertEquals(173.92, netOther, 0.001)
        freeToSpend = (totalIncome + netOther) - totalExpense
        // DEVE CONTINUAR EXATAMENTE R$ 380,51 (e NUNCA R$ 587,10)
        assertEquals(380.51, freeToSpend, 0.001)

        // Cenário 3: Usuário tem transferências acumuladas (256,59) e saldo no MP2 de R$ 370,51
        // O valor livre para gastar DEVE ser exatamente o saldo do Mercado Pago (R$ 370,51), NUNCA R$ 340,51!
        val currentIncome = 5132.22
        val currentCommitted = 4905.63
        val currentSalarySurplus = currentIncome - currentCommitted // 226.59
        val currentOtherBalance = 370.51
        val currentTransfers = 256.59
        val currentDuplicate = if (currentSalarySurplus > 0.0) minOf(currentTransfers, currentSalarySurplus) else 0.0
        assertEquals(226.59, currentDuplicate, 0.001)
        val currentNetOther = (currentOtherBalance - currentDuplicate).coerceAtLeast(0.0)
        assertEquals(143.92, currentNetOther, 0.001)
        val currentFreeToSpend = (currentIncome + currentNetOther) - currentCommitted
        assertEquals(370.51, currentFreeToSpend, 0.001)

        // Cenário 4: Usuário transfere R$ 170,00 do Inter para o Sofisa Direto!
        // Sofisa recebe 170,00, saldo do Sofisa vai para 170,00.
        // A transferência NÃO DEVE computar valor livre: o dinheiro livre para gastar DEVE continuar R$ 370,51 (e NUNCA R$ 540,51)!
        val sofisaBalance = 170.0
        val transfersToSofisa = 170.0
        val sofisaNetContribution = (sofisaBalance - transfersToSofisa).coerceAtLeast(0.0)
        assertEquals(0.0, sofisaNetContribution, 0.001)

        val totalOtherEffectiveBalance = currentOtherBalance + sofisaNetContribution // 370.51 + 0.0 = 370.51
        val finalDuplicate = if (currentSalarySurplus > 0.0) minOf(currentTransfers, currentSalarySurplus) else 0.0
        val finalNetOther = (totalOtherEffectiveBalance - finalDuplicate).coerceAtLeast(0.0)
        val finalFreeToSpend = (currentIncome + finalNetOther) - currentCommitted
        assertEquals(370.51, finalFreeToSpend, 0.001) // Continua intacto em R$ 370,51!
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

    @Test
    fun testFixedExpenseAndFixedIncomeFilters() {
        val luz = com.example.mobillsfinancas.data.TransactionItem(
            description = "Luz",
            amount = 200.0,
            type = com.example.mobillsfinancas.data.TransactionType.EXPENSE,
            monthYear = "2026-11",
            isRecurring = true
        )
        val chuveiro = com.example.mobillsfinancas.data.TransactionItem(
            description = "Chuveiro",
            amount = 100.0,
            type = com.example.mobillsfinancas.data.TransactionType.EXPENSE,
            monthYear = "2026-11",
            isRecurring = false
        )
        val salario = com.example.mobillsfinancas.data.TransactionItem(
            description = "Salário",
            amount = 3000.0,
            type = com.example.mobillsfinancas.data.TransactionType.INCOME,
            monthYear = "2026-11",
            isRecurring = true,
            isSalaryReplacement = true
        )
        val pixAvulso = com.example.mobillsfinancas.data.TransactionItem(
            description = "Pix Venda",
            amount = 50.0,
            type = com.example.mobillsfinancas.data.TransactionType.INCOME,
            monthYear = "2026-11",
            isRecurring = false,
            isSalaryReplacement = false
        )

        val list = listOf(luz, chuveiro, salario, pixAvulso)

        // Filtro Despesas Fixas (index 3)
        val despesasFixas = list.filter { it.type == com.example.mobillsfinancas.data.TransactionType.EXPENSE && it.isRecurring }
        assertEquals(1, despesasFixas.size)
        assertEquals("Luz", despesasFixas.first().description)
        assertEquals(200.0, despesasFixas.first().amount, 0.001)

        // Filtro Receitas Fixas (index 4)
        val receitasFixas = list.filter { it.type == com.example.mobillsfinancas.data.TransactionType.INCOME && (it.isRecurring || it.isSalaryReplacement) }
        assertEquals(1, receitasFixas.size)
        assertEquals("Salário", receitasFixas.first().description)
        assertEquals(3000.0, receitasFixas.first().amount, 0.001)
    }

    @Test
    fun testDefaultAccountMarkDoesNotDistortPrimarySalaryAccountAndReceitas() {
        // Cenário real do usuário:
        // Conta Inter recebe o salário de R$ 5.102,22 e tem saldo operacional de R$ 2.787,63.
        // Mercado Pago 2 é marcado pelo usuário como "⭐ Conta Padrão" para compras diárias e tem saldo de R$ 370,51.
        val inter = AccountItem(
            id = "acc_inter",
            name = "Conta Inter",
            balance = 2787.63,
            includeInOverallBalance = true,
            isDefault = false
        )
        val mp2 = AccountItem(
            id = "acc_mp",
            name = "Mercado Pago 2",
            balance = 370.51,
            includeInOverallBalance = true,
            isDefault = true // Usuário colocou como Conta Padrão!
        )
        val accounts = listOf(mp2, inter)

        // Receitas do mês que caíram na Conta Inter
        val salario = TransactionItem(
            id = "1",
            description = "Salário",
            amount = 3000.0,
            type = TransactionType.INCOME,
            accountOrCardName = "Conta Inter",
            monthYear = "NOVEMBRO 2026"
        )
        val ferias = TransactionItem(
            id = "2",
            description = "Férias",
            amount = 2102.22,
            type = TransactionType.INCOME,
            accountOrCardName = "Conta Inter",
            monthYear = "NOVEMBRO 2026"
        )
        val items = listOf(salario, ferias)

        // 1. Identificação dinâmica da conta operacional onde entra o salário
        val salaryAccountName = items
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.accountOrCardName }
            .maxByOrNull { entry -> entry.value.sumOf { it.amount } }?.key

        assertEquals("Conta Inter", salaryAccountName)

        val primaryAccount = accounts.find { it.name.trim().equals(salaryAccountName?.trim(), ignoreCase = true) }
            ?: accounts.filter { it.includeInOverallBalance }.maxByOrNull { it.balance }
            ?: accounts.find { it.isDefault }

        // A conta principal DEVE ser Conta Inter (onde entrou o salário), MESMO MP2 sendo isDefault
        assertEquals("acc_inter", primaryAccount?.id)
        assertEquals("Conta Inter", primaryAccount?.name)

        // As outras contas serão apenas as secundárias (MP2)
        val otherAccounts = accounts.filter { acc ->
            acc.includeInOverallBalance && acc.balance > 0.0 && acc.id != primaryAccount?.id
        }
        assertEquals(1, otherAccounts.size)
        assertEquals("acc_mp", otherAccounts.first().id)
        val otherAccountsBalance = otherAccounts.sumOf { it.balance }
        assertEquals(370.51, otherAccountsBalance, 0.001)

        // 2. Transferência de R$ 206,59 do Inter para o MP2
        val baseIncome = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalCommitted = 4895.63 // Contas fixas + Reserva Cofrinho + Mercado
        val salarySurplus = baseIncome - totalCommitted // 206.59
        val transfersToOtherAccounts = 206.59
        val duplicateTransferAmount = if (salarySurplus > 0.0) minOf(transfersToOtherAccounts, salarySurplus) else 0.0
        val netOtherAccountsBalance = maxOf(0.0, otherAccountsBalance - duplicateTransferAmount)
        assertEquals(163.92, netOtherAccountsBalance, 0.001)

        // 3. Receitas e Sobrou para Gastar Comigo
        val effectiveIncome = baseIncome + netOtherAccountsBalance
        val freeToSpend = effectiveIncome - totalCommitted // 370.51

        // Verificações essenciais:
        // Receitas do mês NUNCA deve somar saldo de contas bancárias (não pode ser 7.919,85)
        assertEquals(5102.22, baseIncome, 0.001)
        assertNotEquals(7919.85, baseIncome, 0.001)

        // Sobrou livre deve bater com o bolso real (R$ 370,51), NUNCA R$ 3.014,22
        assertEquals(370.51, freeToSpend, 0.001)
        assertNotEquals(3014.22, freeToSpend, 0.001)
    }

    @Test
    fun testInvoicePaymentDoesNotDeductFromSobrouParaGastarComigo() {
        val baseIncome = 5132.22
        // Despesas do mês já incluindo as compras individuais do cartão (R$ 507,51):
        val cartaoCompras = TransactionItem(
            id = "c1", description = "Chuveiro e compras no crédito", amount = 507.51,
            type = TransactionType.EXPENSE, paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = "Cartão Inter", monthYear = "NOVEMBRO 2026", includeInBudget = true
        )
        val contasFixas = TransactionItem(
            id = "c2", description = "Contas Fixas", amount = 2371.22,
            type = TransactionType.EXPENSE, paymentType = PaymentType.DEBIT_CARD,
            accountOrCardName = "Conta Inter", monthYear = "NOVEMBRO 2026", includeInBudget = true
        )
        val metaCofrinho = TransactionItem(
            id = "c3", description = "Reserva Cofrinho", amount = 2000.0,
            type = TransactionType.TRANSFER, monthYear = "NOVEMBRO 2026", includeInBudget = true
        )

        val itemsBeforePayment = listOf(cartaoCompras, contasFixas, metaCofrinho)
        val totalExpBefore = itemsBeforePayment.filter {
            (it.type == TransactionType.EXPENSE && it.includeInBudget && !com.example.mobillsfinancas.data.isInvoicePayment(it)) ||
            (it.type == TransactionType.TRANSFER && it.includeInBudget)
        }.sumOf { it.amount }
        assertEquals(4878.73, totalExpBefore, 0.001)

        val otherAccountsBalance = 370.51
        val transfersToOther = 253.49
        val surplusBefore = baseIncome - totalExpBefore // 253.49
        val dupTransferBefore = if (surplusBefore > 0) minOf(transfersToOther, surplusBefore) else 0.0
        val netOtherBefore = (otherAccountsBalance - dupTransferBefore).coerceAtLeast(0.0)
        val freeToSpendBefore = (baseIncome + netOtherBefore) - totalExpBefore
        assertEquals(370.51, freeToSpendBefore, 0.001)

        // Usuário paga a fatura de R$ 507,51!
        val paymentReceipt = TransactionItem(
            id = "pay_inv",
            description = "Pagamento de Fatura - Cartão Inter",
            amount = 507.51,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.DEBIT_CARD,
            accountOrCardName = "Conta Inter",
            monthYear = "NOVEMBRO 2026",
            status = PaymentStatus.PAID,
            includeInBudget = false // Pagamento de fatura não entra no orçamento!
        )

        val itemsAfterPayment = listOf(paymentReceipt) + itemsBeforePayment

        // Total despesas do mês NÃO DEVE mudar (não pode ir para 5.386,24)
        val totalExpAfter = itemsAfterPayment.filter {
            (it.type == TransactionType.EXPENSE && it.includeInBudget && !com.example.mobillsfinancas.data.isInvoicePayment(it)) ||
            (it.type == TransactionType.TRANSFER && it.includeInBudget)
        }.sumOf { it.amount }
        assertEquals(4878.73, totalExpAfter, 0.001)

        // Sobrou para Gastar Comigo DEVE continuar intacto em R$ 370,51 (e NUNCA cair para R$ 192,53!)
        val surplusAfter = baseIncome - totalExpAfter
        val dupTransferAfter = if (surplusAfter > 0) minOf(transfersToOther, surplusAfter) else 0.0
        val netOtherAfter = (otherAccountsBalance - dupTransferAfter).coerceAtLeast(0.0)
        val freeToSpendAfter = (baseIncome + netOtherAfter) - totalExpAfter
        assertEquals(370.51, freeToSpendAfter, 0.001)
        assertNotEquals(192.53, freeToSpendAfter, 0.001)
    }
}
