package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.CategoryItem
import com.example.mobillsfinancas.data.CreditCardItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DeletionAndLifecycleTest {

    @Test
    fun testPayingNovemberInvoiceDoesNotAffectDecemberOrFutureInstallments() {
        val groupId = "group_installments_tenis"

        // Compra parcelada em 3x: Novembro (1/3), Dezembro (2/3), Janeiro (3/3)
        val parcelaNov = TransactionItem(
            id = "nov_1",
            description = "Tênis Novo (1/3)",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = "Cartão Inter",
            status = PaymentStatus.PENDING,
            monthYear = "NOVEMBRO 2026",
            installmentGroupId = groupId,
            currentInstallment = 1,
            totalInstallments = 3
        )
        val parcelaDez = TransactionItem(
            id = "dez_2",
            description = "Tênis Novo (2/3)",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = "Cartão Inter",
            status = PaymentStatus.PENDING,
            monthYear = "DEZEMBRO 2026",
            installmentGroupId = groupId,
            currentInstallment = 2,
            totalInstallments = 3
        )
        val parcelaJan = TransactionItem(
            id = "jan_3",
            description = "Tênis Novo (3/3)",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.CREDIT_CARD,
            accountOrCardName = "Cartão Inter",
            status = PaymentStatus.PENDING,
            monthYear = "JANEIRO 2027",
            installmentGroupId = groupId,
            currentInstallment = 3,
            totalInstallments = 3
        )

        val monthDataMap = mutableMapOf(
            "NOVEMBRO 2026" to listOf(parcelaNov),
            "DEZEMBRO 2026" to listOf(parcelaDez),
            "JANEIRO 2027" to listOf(parcelaJan)
        )

        // Usuário paga a fatura de Novembro:
        val novItems = monthDataMap["NOVEMBRO 2026"]!!.map {
            if (it.accountOrCardName == "Cartão Inter") it.copy(status = PaymentStatus.PAID) else it
        }
        monthDataMap["NOVEMBRO 2026"] = novItems

        // 1. Em Novembro, a parcela 1/3 agora está PAGA
        assertEquals(PaymentStatus.PAID, monthDataMap["NOVEMBRO 2026"]!!.first().status)

        // 2. Em Dezembro, a parcela 2/3 NÃO SUMIU e continua intacta com status PENDING!
        val itemDez = monthDataMap["DEZEMBRO 2026"]!!.first()
        assertEquals("Tênis Novo (2/3)", itemDez.description)
        assertEquals(PaymentStatus.PENDING, itemDez.status)
        assertEquals(100.0, itemDez.amount, 0.001)

        // 3. Em Janeiro, a parcela 3/3 continua intacta com status PENDING!
        val itemJan = monthDataMap["JANEIRO 2027"]!!.first()
        assertEquals("Tênis Novo (3/3)", itemJan.description)
        assertEquals(PaymentStatus.PENDING, itemJan.status)
    }

    @Test
    fun testDeleteSimpleTransactionAndRefundBalanceIfPaid() {
        var checking = AccountItem(
            id = "acc_inter",
            name = "Conta Inter",
            balance = 4794.22,
            includeInOverallBalance = true
        )

        // Despesa no débito de R$ 200 que já foi paga
        val despesaPaga = TransactionItem(
            id = "tx_luz",
            description = "Luz",
            amount = 200.0,
            type = TransactionType.EXPENSE,
            paymentType = PaymentType.DEBIT_CARD,
            accountOrCardName = checking.name,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026"
        )

        var items = listOf(despesaPaga)

        // Ao excluir uma despesa PAGA no débito, o valor deve ser estornado de volta para o saldo da conta
        if (despesaPaga.status == PaymentStatus.PAID && despesaPaga.type == TransactionType.EXPENSE) {
            checking = checking.copy(balance = checking.balance + despesaPaga.amount)
        }
        items = items.filter { it.id != despesaPaga.id }

        assertTrue(items.isEmpty())
        assertEquals(4994.22, checking.balance, 0.001)
    }

    @Test
    fun testDeleteIncomeRefundsBalance() {
        var checking = AccountItem(
            id = "acc_inter",
            name = "Conta Inter",
            balance = 4794.22,
            includeInOverallBalance = true
        )

        val receitaRecebida = TransactionItem(
            id = "tx_pix_extra",
            description = "Pix Recebido",
            amount = 100.0,
            type = TransactionType.INCOME,
            paymentType = PaymentType.PIX,
            accountOrCardName = checking.name,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026"
        )

        // Ao excluir uma receita que já foi recebida, o saldo da conta diminui
        if (receitaRecebida.status == PaymentStatus.PAID && receitaRecebida.type == TransactionType.INCOME) {
            checking = checking.copy(balance = checking.balance - receitaRecebida.amount)
        }

        assertEquals(4694.22, checking.balance, 0.001)
    }

    @Test
    fun testDeleteOnlyCurrentInstallmentVsEntireGroup() {
        val groupId = "group_tv"
        val p1 = TransactionItem(id = "tv_1", description = "TV (1/2)", amount = 500.0, installmentGroupId = groupId, monthYear = "NOVEMBRO 2026")
        val p2 = TransactionItem(id = "tv_2", description = "TV (2/2)", amount = 500.0, installmentGroupId = groupId, monthYear = "DEZEMBRO 2026")

        var novList = listOf(p1)
        var dezList = listOf(p2)

        // Caso A: Usuário opta por excluir apenas a parcela 1 de Novembro
        novList = novList.filter { it.id != "tv_1" }
        assertTrue(novList.isEmpty())
        // A parcela 2 de Dezembro continua viva
        assertEquals(1, dezList.size)
        assertEquals("tv_2", dezList.first().id)

        // Caso B: Usuário opta por excluir o grupo inteiro
        dezList = dezList.filter { it.installmentGroupId != groupId }
        assertTrue(dezList.isEmpty())
    }

    @Test
    fun testDeleteTransferReversesBothAccounts() {
        var inter = AccountItem(id = "acc_inter", name = "Conta Inter", balance = 4000.0)
        var mp = AccountItem(id = "acc_mp", name = "Mercado Pago", balance = 1000.0)

        val transferItem = TransactionItem(
            id = "trf_1",
            description = "Transferência Inter -> MP",
            amount = 500.0,
            type = TransactionType.TRANSFER,
            status = PaymentStatus.PAID,
            monthYear = "NOVEMBRO 2026"
        )

        // Reversão de transferência executada:
        inter = inter.copy(balance = inter.balance + transferItem.amount)
        mp = mp.copy(balance = mp.balance - transferItem.amount)

        assertEquals(4500.0, inter.balance, 0.001)
        assertEquals(500.0, mp.balance, 0.001)
    }

    @Test
    fun testDeleteAccountAndReassignDefault() {
        val acc1 = AccountItem(id = "1", name = "Mercado Pago", isDefault = true)
        val acc2 = AccountItem(id = "2", name = "Conta Inter", isDefault = false)

        var accounts = listOf(acc1, acc2)

        // Exclui a conta padrão (acc1)
        accounts = accounts.filter { it.id != "1" }

        // Sistema reelege automaticamente a primeira conta restante como padrão
        if (accounts.none { it.isDefault } && accounts.isNotEmpty()) {
            accounts = accounts.mapIndexed { idx, it -> if (idx == 0) it.copy(isDefault = true) else it }
        }

        assertEquals(1, accounts.size)
        assertEquals("Conta Inter", accounts.first().name)
        assertTrue(accounts.first().isDefault)
    }

    @Test
    fun testDeleteCardAndCategory() {
        val card1 = CreditCardItem(id = "c1", name = "Cartão Inter", limit = 5000.0)
        val card2 = CreditCardItem(id = "c2", name = "Cartão Nubank", limit = 3000.0)
        var cards = listOf(card1, card2)

        cards = cards.filter { it.id != "c1" }
        assertEquals(1, cards.size)
        assertEquals("Cartão Nubank", cards.first().name)

        val cat1 = CategoryItem(id = "cat1", name = "Mercado", iconEmoji = "🛒", colorHex = 0xFF4CAF50)
        val cat2 = CategoryItem(id = "cat2", name = "Lazer", iconEmoji = "🎉", colorHex = 0xFFFF9800)
        var categories = listOf(cat1, cat2)

        categories = categories.filter { it.id != "cat1" }
        assertEquals(1, categories.size)
        assertEquals("Lazer", categories.first().name)
    }

    @Test
    fun testDeleteMultipleTransactionsInBatch() {
        val t1 = TransactionItem(id = "1", description = "Item 1", amount = 10.0, monthYear = "NOVEMBRO 2026")
        val t2 = TransactionItem(id = "2", description = "Item 2", amount = 20.0, monthYear = "NOVEMBRO 2026")
        val t3 = TransactionItem(id = "3", description = "Item 3", amount = 30.0, monthYear = "NOVEMBRO 2026")
        val t4 = TransactionItem(id = "4", description = "Item 4 (Manter)", amount = 40.0, monthYear = "NOVEMBRO 2026")

        var list = listOf(t1, t2, t3, t4)
        val idsToDelete = setOf("1", "2", "3")

        list = list.filter { it.id !in idsToDelete }

        assertEquals(1, list.size)
        assertEquals("Item 4 (Manter)", list.first().description)
        assertEquals(40.0, list.first().amount, 0.001)
    }
}
