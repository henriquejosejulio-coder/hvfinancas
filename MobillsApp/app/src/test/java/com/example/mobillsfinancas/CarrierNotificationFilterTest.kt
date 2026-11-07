package com.example.mobillsfinancas

import com.example.mobillsfinancas.services.BankNotificationService
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarrierNotificationFilterTest {

    private val service = BankNotificationService()

    @Test
    fun testTimCarrierPromosAreBlocked() {
        val timPromo1 = "Ganhe R$ 75 de desconto na atualização do seu plano TIM pagando com Pix"
        val timPromo2 = "Você recebeu R$ 75 de desconto na atualização do seu plano TIM"
        val timPromo3 = "Pix de desconto na atualização do seu plano TIM: R$ 75 de desconto"
        val timPromo4 = "Recarga premiada TIM: Recarregue R$ 20 e ganhe bônus de 10GB de internet"

        assertTrue("Promo TIM 1 deve ser marcada como non-transaction", service.isNonTransactionNotification(timPromo1))
        assertTrue("Promo TIM 2 deve ser marcada como non-transaction", service.isNonTransactionNotification(timPromo2))
        assertTrue("Promo TIM 3 deve ser marcada como non-transaction", service.isNonTransactionNotification(timPromo3))
        assertTrue("Promo TIM 4 deve ser marcada como non-transaction", service.isNonTransactionNotification(timPromo4))

        assertFalse("Promo TIM 2 NÃO pode ser confirmada como transação real", service.isValidTransactionConfirmation(timPromo2))
    }

    @Test
    fun testOtherCarriersAreBlocked() {
        val claro = "Claro Flex: Ganhe R$ 10 de desconto na atualização do seu plano Claro"
        val vivo = "Vivo Total: Aproveite R$ 50 de desconto no seu plano Vivo"
        val recarga = "Faça uma recarga de R$ 15 e ganhe bônus em dobro"

        assertTrue("Claro deve ser bloqueado", service.isNonTransactionNotification(claro))
        assertTrue("Vivo deve ser bloqueado", service.isNonTransactionNotification(vivo))
        assertTrue("Recarga deve ser bloqueada", service.isNonTransactionNotification(recarga))
    }

    @Test
    fun testStoreCandidateRejectsDiscountPhrases() {
        assertFalse(service.isValidStoreCandidate("desconto na atualização do seu plano TIM"))
        assertFalse(service.isValidStoreCandidate("plano tim"))
        assertFalse(service.isValidStoreCandidate("desconto"))
        assertFalse(service.isValidStoreCandidate("bônus de recarga"))
        
        // Nomes reais de lojas devem ser válidos
        assertTrue(service.isValidStoreCandidate("Mercado Central"))
        assertTrue(service.isValidStoreCandidate("Padaria Estrela"))
        assertTrue(service.isValidStoreCandidate("Uber"))
        assertTrue(service.isValidStoreCandidate("Henrique Jose"))
    }

    @Test
    fun testRealBankTransactionsAreStillAccepted() {
        val pixRecebido = "Você recebeu um Pix de R$ 150,00 de João da Silva"
        val compraCartao = "Compra de R$ 49,90 aprovada em Supermercado Carrefour"
        val pixEnviado = "Pix enviado: R$ 30,00 para Padaria Estrela"
        val salario = "Salário creditado no valor de R$ 3.200,00 na sua conta"
        val ferias = "Férias recebidas no valor de R$ 2.500,00"

        assertFalse("Pix real não deve ser marcado como non-transaction", service.isNonTransactionNotification(pixRecebido))
        assertFalse("Compra real não deve ser marcada como non-transaction", service.isNonTransactionNotification(compraCartao))
        assertFalse("Pix enviado não deve ser marcado como non-transaction", service.isNonTransactionNotification(pixEnviado))
        assertFalse("Salário não deve ser marcado como non-transaction", service.isNonTransactionNotification(salario))
        assertFalse("Férias não devem ser marcadas como non-transaction", service.isNonTransactionNotification(ferias))

        assertTrue("Pix real deve ser confirmado", service.isValidTransactionConfirmation(pixRecebido))
        assertTrue("Compra real deve ser confirmada", service.isValidTransactionConfirmation(compraCartao))
        assertTrue("Pix enviado deve ser confirmado", service.isValidTransactionConfirmation(pixEnviado))
        assertTrue("Salário deve ser confirmado", service.isValidTransactionConfirmation(salario))
        assertTrue("Férias devem ser confirmadas", service.isValidTransactionConfirmation(ferias))
    }

    @Test
    fun testBancoInterCreditPurchaseIsAccepted() {
        val interNotification = "Compra no crédito Olá, Henrique. Você acaba de comprar R$ 26,90 em DL*GOOGLE YouTub. A compra foi no crédito nacional, com o cartão final 4456. Inter"
        assertFalse("Não deve ser marcado como non-transaction", service.isNonTransactionNotification(interNotification))
        assertTrue("Deve ser confirmado como transação válida", service.isValidTransactionConfirmation(interNotification))
    }
}
