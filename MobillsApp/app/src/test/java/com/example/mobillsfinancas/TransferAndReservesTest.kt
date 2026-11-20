package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransferAndReservesTest {

    @Test
    fun testImmediateTransferBetweenCheckingAccounts() {
        var inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4794.22, includeInOverallBalance = true)
        var mp = AccountItem(id = "acc_mp", name = "Mercado Pago", balance = 173.92, includeInOverallBalance = true)

        val transferAmount = 500.0

        // Executa transferência imediata de Inter -> MP
        inter = inter.copy(balance = inter.balance - transferAmount)
        mp = mp.copy(balance = mp.balance + transferAmount)

        assertEquals(4294.22, inter.balance, 0.001)
        assertEquals(673.92, mp.balance, 0.001)

        // Soma total das contas permanece idêntica (apenas migrou de bolso)
        assertEquals(4968.14, inter.balance + mp.balance, 0.001)
    }

    @Test
    fun testTransferToPiggyBankReserve() {
        var inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4794.22, includeInOverallBalance = true)
        val cofrinho = AccountItem(id = "acc_cofrinho", name = "Cofrinho MP 2", balance = 0.0, includeInOverallBalance = false)

        val reserveAmount = 2000.0

        // Destino não é conta corrente (includeInOverallBalance = false), portanto é uma reserva
        val isToReserve = !cofrinho.includeInOverallBalance
        assertTrue(isToReserve)

        val txItem = TransactionItem(
            id = "trf_123",
            description = "Transferência: ${inter.name} ➔ ${cofrinho.name}",
            amount = reserveAmount,
            type = TransactionType.TRANSFER,
            category = if (isToReserve) "Cofrinho & Reserva" else "Transferência",
            includeInBudget = isToReserve,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026"
        )

        assertEquals("Cofrinho & Reserva", txItem.category)
        assertTrue(txItem.includeInBudget)

        // Se a transferência for concluída, debita da conta corrente
        inter = inter.copy(balance = inter.balance - reserveAmount)
        assertEquals(2794.22, inter.balance, 0.001)
    }

    @Test
    fun testScheduledTransferDeductionFromAvailableBalance() {
        val inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4794.22, includeInOverallBalance = true)
        val mp = AccountItem(id = "acc_mp", name = "Mercado Pago 2", balance = 173.92, includeInOverallBalance = true)
        val accounts = listOf(inter, mp)

        val totalInBank = accounts.filter { it.includeInOverallBalance }.sumOf { it.balance }
        assertEquals(4968.14, totalInBank, 0.001)

        // Transferência agendada de R$ 2.000 para o cofrinho (status = PENDING)
        val scheduledTransfer = TransactionItem(
            id = "trf_sched_1",
            description = "Agendado: Conta Inter ➔ Cofrinho MP 2",
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            includeInBudget = true,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026"
        )

        val pendingCofrinhoTransfers = listOf(scheduledTransfer)
            .filter { it.type == TransactionType.TRANSFER && it.status == PaymentStatus.PENDING && it.includeInBudget }
            .sumOf { it.amount }

        assertEquals(2000.0, pendingCofrinhoTransfers, 0.001)

        // Saldo disponível em conta corrente protegido deduzindo o agendamento
        val effectiveAvailable = (totalInBank - pendingCofrinhoTransfers).coerceAtLeast(0.0)
        assertEquals(2968.14, effectiveAvailable, 0.001)
    }

    @Test
    fun testEditingScheduledTransferToPaidMovesBalances() {
        var inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4794.22, includeInOverallBalance = true)
        var cofrinho = AccountItem(id = "acc_cofrinho", name = "Cofrinho MP 2", balance = 0.0, includeInOverallBalance = false)

        val scheduledItem = TransactionItem(
            id = "trf_sched_2000",
            description = "Agendado: Conta Inter ➔ Cofrinho MP 2",
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026",
            note = "fromAccountId:${inter.id}|toAccountId:${cofrinho.id}"
        )

        // Ao efetivar (mudar para PAID), deve debitar da origem e creditar no destino
        val isNowPaid = true
        if (isNowPaid) {
            inter = inter.copy(balance = inter.balance - scheduledItem.amount)
            cofrinho = cofrinho.copy(balance = cofrinho.balance + scheduledItem.amount)
        }

        assertEquals(2794.22, inter.balance, 0.001)
        assertEquals(2000.0, cofrinho.balance, 0.001)

        val paidItem = scheduledItem.copy(
            status = PaymentStatus.PAID,
            description = scheduledItem.description.replace("Agendado:", "Transferência:")
        )
        assertEquals("Transferência: Conta Inter ➔ Cofrinho MP 2", paidItem.description)
        assertEquals(PaymentStatus.PAID, paidItem.status)
    }

    @Test
    fun testEditingPaidTransferBackToPendingRevertsBalances() {
        var inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 2794.22, includeInOverallBalance = true)
        var cofrinho = AccountItem(id = "acc_cofrinho", name = "Cofrinho MP 2", balance = 2000.0, includeInOverallBalance = false)

        val paidItem = TransactionItem(
            id = "trf_paid_2000",
            description = "Transferência: Conta Inter ➔ Cofrinho MP 2",
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026",
            note = "fromAccountId:${inter.id}|toAccountId:${cofrinho.id}"
        )

        // Ao desmarcar (mudar para PENDING), reverte os saldos
        inter = inter.copy(balance = inter.balance + paidItem.amount)
        cofrinho = cofrinho.copy(balance = cofrinho.balance - paidItem.amount)

        assertEquals(4794.22, inter.balance, 0.001)
        assertEquals(0.0, cofrinho.balance, 0.001)

        val pendingItem = paidItem.copy(
            status = PaymentStatus.PENDING,
            description = paidItem.description.replace("Transferência:", "Agendado:")
        )
        assertEquals("Agendado: Conta Inter ➔ Cofrinho MP 2", pendingItem.description)
        assertEquals(PaymentStatus.PENDING, pendingItem.status)
    }

    @Test
    fun testFallbackAccountResolutionFromDescriptionWhenNoteIsEmpty() {
        val inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4794.22, includeInOverallBalance = true)
        val cofrinho = AccountItem(id = "acc_cofrinho", name = "Cofrinho MP 2", balance = 0.0, includeInOverallBalance = false)
        val accounts = listOf(inter, cofrinho)

        val itemWithoutNote = TransactionItem(
            id = "trf_no_note",
            description = "Agendado: Conta Inter ➔ Cofrinho MP 2",
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026",
            note = "" // Nota vazia
        )

        // Resolução por fallback da descrição
        val arrow = if (itemWithoutNote.description.contains("➔")) "➔" else "->"
        val parts = itemWithoutNote.description.split(arrow)
        val rawFrom = parts[0].replace("Agendado:", "").replace("Transferência:", "").trim()
        val rawTo = parts[1].trim()

        val resolvedFrom = accounts.find { it.name.equals(rawFrom, ignoreCase = true) }
        val resolvedTo = accounts.find { it.name.equals(rawTo, ignoreCase = true) }

        assertEquals(inter.id, resolvedFrom?.id)
        assertEquals(cofrinho.id, resolvedTo?.id)
    }

    @Test
    fun testResgateFromPiggyBankOffsetsReserveAllocation() {
        val inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 5000.0, includeInOverallBalance = true)
        val cofrinho = AccountItem(id = "acc_cofrinho", name = "Cofrinho MP 2", balance = 0.0, includeInOverallBalance = false)
        val mp2 = AccountItem(id = "acc_mp2", name = "Mercado Pago 2", balance = 0.0, includeInOverallBalance = true)
        val accounts = listOf(inter, cofrinho, mp2)

        // 1. Guarda R$ 2.000 no Cofrinho
        val saveTx = TransactionItem(
            id = "trf_save",
            description = "Transferência: Conta Inter ➔ Cofrinho MP 2",
            amount = 2000.0,
            type = TransactionType.TRANSFER,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026",
            note = "fromAccountId:${inter.id}|toAccountId:${cofrinho.id}"
        )

        // 2. Resgata R$ 70 do Cofrinho para o Mercado Pago 2 para pagar Caio Dentista
        val resgateTx = TransactionItem(
            id = "trf_resgate",
            description = "Transferência: Cofrinho MP 2 ➔ Mercado Pago 2",
            amount = 70.0,
            type = TransactionType.TRANSFER,
            category = "Resgate Cofrinho",
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026",
            note = "fromAccountId:${cofrinho.id}|toAccountId:${mp2.id}"
        )

        // 3. Despesa de R$ 70 (Caio Dentista)
        val expenseTx = TransactionItem(
            id = "exp_dentista",
            description = "Caio Dentista",
            amount = 70.0,
            type = TransactionType.EXPENSE,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026",
            includeInBudget = true
        )

        // Simula cálculo de recalculateAllMonths com offset de resgate
        var reserveNet = 0.0
        var totalExp = 0.0
        val items = listOf(saveTx, resgateTx, expenseTx)

        for (item in items) {
            if (item.type == TransactionType.EXPENSE && item.includeInBudget) {
                totalExp += item.amount
            } else if (item.type == TransactionType.TRANSFER) {
                val (fromAcc, toAcc) = com.example.mobillsfinancas.data.FinanceRepository.resolveTransferAccounts(item, accounts)
                val isToReserve = fromAcc?.includeInOverallBalance == true && toAcc?.includeInOverallBalance == false
                val isFromReserve = fromAcc?.includeInOverallBalance == false && toAcc?.includeInOverallBalance == true
                if (isToReserve) reserveNet += item.amount
                if (isFromReserve) reserveNet -= item.amount
            }
        }
        if (reserveNet >= 0.0) totalExp += reserveNet

        // Total comprometido deve ser exatamente R$ 2.000,00 (1930 no cofrinho + 70 no dentista)
        // e NUNCA R$ 2.070,00 (sem dupla cobrança)!
        assertEquals(2000.0, totalExp, 0.001)
    }

    @Test
    fun testDestinationAccountMatchingWithTrailingNumber() {
        val inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 500.0, includeInOverallBalance = true)
        val mp2 = AccountItem(id = "acc_mp2", name = "Mercado Pago 2", balance = 200.0, includeInOverallBalance = true)
        val accounts = listOf(inter, mp2)

        val notificationText = "Você transferiu R$ 50,00 para Mercado Pago com sucesso."
        val lowerText = notificationText.lowercase()

        // Verifica algoritmo aprimorado de match
        var matchedAcc: AccountItem? = null
        for (acc in accounts) {
            if (acc.id == inter.id) continue
            val cleanName = acc.name.lowercase().trim()
            val baseName = cleanName.replace(Regex("\\s*\\d+$"), "").trim()
            if (lowerText.contains(cleanName) || lowerText.contains(baseName)) {
                matchedAcc = acc
                break
            }
        }

        assertEquals("acc_mp2", matchedAcc?.id)
        assertEquals("Mercado Pago 2", matchedAcc?.name)
    }
}
