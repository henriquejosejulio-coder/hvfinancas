package com.example.mobillsfinancas

import com.example.mobillsfinancas.services.BankNotificationService
import com.example.mobillsfinancas.utils.BankRegistry
import com.example.mobillsfinancas.utils.TransactionDescriptionCleaner
import org.junit.Assert.*
import org.junit.Test

class BankNotificationParsingTest {

    private val service = BankNotificationService()

    @Test
    fun testBancoInterNotificationFromScreenshot() {
        val title = "Inter"
        val text = "Olá Henrique, você acaba de comprar no débito no MP *JOSEMIGUELDEPAULA o valor de R$ 13,00."
        val fullContent = "$title $text"

        // 1. Validação de filtros
        assertFalse("Não deve ser non-transaction", service.isNonTransactionNotification(fullContent))
        assertTrue("Deve ser uma transação confirmada válida", service.isValidTransactionConfirmation(fullContent))

        // 2. Detecção de Banco Inter por pacote oficial
        val detectedBank = BankRegistry.findBankByPackageOrContent("br.com.intermedium", title, fullContent)
        assertNotNull("Deve detectar Banco Inter", detectedBank)
        assertEquals("inter", detectedBank?.key)

        // 3. Extração correta do valor
        val amount = service.extractAmount(fullContent)
        assertEquals(13.00, amount, 0.001)

        // 4. Extração do estabelecimento com novo regex inteligente e limpeza de nome
        val store = service.extractStoreOrBeneficiary(fullContent)
        assertEquals("Jose Miguel de Paula", store)
    }

    @Test
    fun testBancoInterCreditAndPix() {
        // Crédito
        val interCredito = "Olá Henrique, você acaba de comprar no crédito no Mercado Livre o valor de R$ 76,77."
        assertFalse(service.isNonTransactionNotification(interCredito))
        assertTrue(service.isValidTransactionConfirmation(interCredito))
        assertEquals(76.77, service.extractAmount(interCredito), 0.001)
        val storeCred = service.extractStoreOrBeneficiary(interCredito)
        assertTrue("Deve extrair Mercado Livre: '$storeCred'", storeCred.contains("Mercado Livre", ignoreCase = true))

        // Compra no crédito com DL Google
        val interGoogle = "Você acaba de comprar R$ 26,90 em DL GOOGLE YouTub"
        assertFalse(service.isNonTransactionNotification(interGoogle))
        assertTrue(service.isValidTransactionConfirmation(interGoogle))
        assertEquals(26.90, service.extractAmount(interGoogle), 0.001)
        val storeGoogle = service.extractStoreOrBeneficiary(interGoogle)
        assertTrue("Deve extrair YouTube: '$storeGoogle'", storeGoogle.contains("YouTube", ignoreCase = true))

        // Pix enviado
        val interPix = "Pix enviado: você transferiu R$ 11,00 para Mercado Pago com sucesso."
        assertFalse(service.isNonTransactionNotification(interPix))
        assertTrue(service.isValidTransactionConfirmation(interPix))
        assertEquals(11.00, service.extractAmount(interPix), 0.001)
        val storePix = service.extractStoreOrBeneficiary(interPix)
        assertEquals("Mercado Pago", storePix)

        // Pix recebido
        val interPixRec = "Você recebeu um Pix de Maria Silva no valor de R$ 150,00."
        assertFalse(service.isNonTransactionNotification(interPixRec))
        assertTrue(service.isValidTransactionConfirmation(interPixRec))
        assertEquals(150.00, service.extractAmount(interPixRec), 0.001)
        val storePixRec = service.extractStoreOrBeneficiary(interPixRec)
        assertEquals("Maria Silva", storePixRec)
    }

    @Test
    fun testAll19BanksPurchasesAndDetection() {
        // 1. Nubank
        val nuTxt = "Compra de R$ 45,00 aprovada em Padaria Estrela"
        val nuBank = BankRegistry.findBankByPackageOrContent("com.nu.production", "Nubank", nuTxt)
        assertEquals("nubank", nuBank?.key)
        assertEquals(45.0, service.extractAmount(nuTxt), 0.001)
        assertEquals("Padaria Estrela", service.cleanStoreName(service.extractStoreOrBeneficiary(nuTxt)))

        // 2. Mercado Pago
        val mpTxt = "Você pagou R$ 30,00 em Auto posto veredas exp com saldo"
        val mpBank = BankRegistry.findBankByPackageOrContent("com.mercadopago.wallet", "Mercado Pago", mpTxt)
        assertEquals("mercadopago", mpBank?.key)
        assertEquals(30.0, service.extractAmount(mpTxt), 0.001)
        val mpClean = TransactionDescriptionCleaner.cleanDescription(service.extractStoreOrBeneficiary(mpTxt), "Transporte", "Mercado Pago 2")
        assertEquals("Auto Posto Veredas Exp", mpClean)

        // 3. Banco Inter
        val interTxt = "Olá Henrique, você acaba de comprar no débito na Padaria Pão Quente o valor de R$ 15,50."
        val interBank = BankRegistry.findBankByPackageOrContent("br.com.intermedium", "Inter", interTxt)
        assertEquals("inter", interBank?.key)
        assertEquals(15.50, service.extractAmount(interTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(interTxt).contains("Padaria Pão Quente", ignoreCase = true))

        // 4. C6 Bank
        val c6Txt = "Compra aprovada no seu C6 Bank: R$ 45,00 em Restaurante Fogão"
        val c6Bank = BankRegistry.findBankByPackageOrContent("com.c6bank.app", "C6 Bank", c6Txt)
        assertEquals("c6", c6Bank?.key)
        assertEquals(45.0, service.extractAmount(c6Txt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(c6Txt).contains("Restaurante Fogão", ignoreCase = true))

        // 5. Itaú
        val itauTxt = "Compra aprovada no seu cartão Itaú: R$ 75,00 em Drogaria SP"
        val itauBank = BankRegistry.findBankByPackageOrContent("com.itau", "Itaú", itauTxt)
        assertEquals("itau", itauBank?.key)
        assertEquals(75.0, service.extractAmount(itauTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(itauTxt).contains("Drogaria SP", ignoreCase = true))

        // 6. Bradesco
        val bradescoTxt = "Bradesco Cartões: Compra de R$ 89,90 aprovada em Lojas Americanas no cartão final 5678"
        val bradescoBank = BankRegistry.findBankByPackageOrContent("com.bradesco.cartoes", "Bradesco", bradescoTxt)
        assertEquals("bradesco", bradescoBank?.key)
        assertEquals(89.90, service.extractAmount(bradescoTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(bradescoTxt).contains("Lojas Americanas", ignoreCase = true))

        // 7. Santander
        val santanderTxt = "Santander: Compra aprovada no cartão SX final 5678: R$ 65,00 em Outback"
        val santanderBank = BankRegistry.findBankByPackageOrContent("com.santander.app", "Santander", santanderTxt)
        assertEquals("santander", santanderBank?.key)
        assertEquals(65.0, service.extractAmount(santanderTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(santanderTxt).contains("Outback", ignoreCase = true))

        // 8. Caixa Econômica
        val caixaTxt = "Caixa: Compra com cartão de débito aprovada: R$ 45,00 em Mercado Local"
        val caixaBank = BankRegistry.findBankByPackageOrContent("br.com.gabba.Caixa", "Caixa", caixaTxt)
        assertEquals("caixa", caixaBank?.key)
        assertEquals(45.0, service.extractAmount(caixaTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(caixaTxt).contains("Mercado Local", ignoreCase = true))

        // 9. Banco do Brasil
        val bbTxt = "BB: Compra com Ourocard aprovada no valor de R$ 95,00 em Carrefour"
        val bbBank = BankRegistry.findBankByPackageOrContent("br.com.bb.android", "Banco do Brasil", bbTxt)
        assertEquals("bb", bbBank?.key)
        assertEquals(95.0, service.extractAmount(bbTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(bbTxt).contains("Carrefour", ignoreCase = true))

        // 10. PicPay
        val picPayTxt = "PicPay: Pagamento de R$ 45,00 aprovado no PicPay Card em Uber"
        val picPay = BankRegistry.findBankByPackageOrContent("com.picpay", "PicPay", picPayTxt)
        assertEquals("picpay", picPay?.key)
        assertEquals(45.0, service.extractAmount(picPayTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(picPayTxt).contains("Uber", ignoreCase = true))

        // 11. Will Bank
        val willTxt = "Compra aprovada no seu Cartão Will de R$ 85,00 em Drogasil"
        val willBank = BankRegistry.findBankByPackageOrContent("com.willbank.app", "Will Bank", willTxt)
        assertEquals("will", willBank?.key)
        assertEquals(85.0, service.extractAmount(willTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(willTxt).contains("Drogasil", ignoreCase = true))

        // 12. Sofisa Direto
        val sofisaTxt = "Sofisa Direto: Cartão de crédito aprovado: R$ 50,00 em Livraria Cultura"
        val sofisaBank = BankRegistry.findBankByPackageOrContent("br.com.sofisadireto", "Sofisa Direto", sofisaTxt)
        assertEquals("sofisa", sofisaBank?.key)
        assertEquals(50.0, service.extractAmount(sofisaTxt), 0.001)

        // 13. PagBank
        val pagBankTxt = "PagBank: Compra aprovada no valor de R$ 38,00 no cartão da conta em Padaria"
        val pagBank = BankRegistry.findBankByPackageOrContent("br.com.uol.ps.myaccount", "PagBank", pagBankTxt)
        assertEquals("pagbank", pagBank?.key)
        assertEquals(38.0, service.extractAmount(pagBankTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(pagBankTxt).contains("Padaria", ignoreCase = true))

        // 14. Neon
        val neonTxt = "Neon: Compra aprovada de R$ 29,90 em Uber"
        val neonBank = BankRegistry.findBankByPackageOrContent("br.com.neon", "Neon", neonTxt)
        assertEquals("neon", neonBank?.key)
        assertEquals(29.90, service.extractAmount(neonTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(neonTxt).contains("Uber", ignoreCase = true))

        // 15. Next
        val nextTxt = "next: Compra aprovada no cartão de crédito de R$ 65,00 em Subway"
        val nextBank = BankRegistry.findBankByPackageOrContent("br.com.bradesco.next", "Next", nextTxt)
        assertEquals("next", nextBank?.key)
        assertEquals(65.0, service.extractAmount(nextTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(nextTxt).contains("Subway", ignoreCase = true))

        // 16. Sicoob
        val sicoobTxt = "Sicoob: Compra aprovada de R$ 75,00 no cartão final 7890 em Supermercado"
        val sicoobBank = BankRegistry.findBankByPackageOrContent("br.com.sicoob.netcorp", "Sicoob", sicoobTxt)
        assertEquals("sicoob", sicoobBank?.key)
        assertEquals(75.0, service.extractAmount(sicoobTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(sicoobTxt).contains("Supermercado", ignoreCase = true))

        // 17. Sicredi
        val sicrediTxt = "Sicredi: Compra aprovada no valor de R$ 110,00 em Posto de Combustível"
        val sicrediBank = BankRegistry.findBankByPackageOrContent("br.com.sicredi.mobile", "Sicredi", sicrediTxt)
        assertEquals("sicredi", sicrediBank?.key)
        assertEquals(110.0, service.extractAmount(sicrediTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(sicrediTxt).contains("Posto de Combustível", ignoreCase = true))

        // 18. BTG Pactual
        val btgTxt = "BTG Pactual: Compra aprovada no valor de R$ 250,00 em Hotel Fazenda"
        val btgBank = BankRegistry.findBankByPackageOrContent("br.com.btg.pactual.banking", "BTG Pactual", btgTxt)
        assertEquals("btg", btgBank?.key)
        assertEquals(250.0, service.extractAmount(btgTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(btgTxt).contains("Hotel Fazenda", ignoreCase = true))

        // 19. RecargaPay
        val recargaTxt = "RecargaPay: Pagamento de R$ 30,00 aprovado no cartão em Mercado"
        val recargaBank = BankRegistry.findBankByPackageOrContent("com.recargapay", "RecargaPay", recargaTxt)
        assertEquals("recargapay", recargaBank?.key)
        assertEquals(30.0, service.extractAmount(recargaTxt), 0.001)
        assertTrue(service.extractStoreOrBeneficiary(recargaTxt).contains("Mercado", ignoreCase = true))
    }

    @Test
    fun testPixSentAndReceivedPatterns() {
        // Pix Enviado - Sofisa
        val sofisaPix = "PIX realizado com sucesso: R$ 170,00 para Conta Inter"
        assertFalse(service.isNonTransactionNotification(sofisaPix))
        assertTrue(service.isValidTransactionConfirmation(sofisaPix))
        assertEquals(170.0, service.extractAmount(sofisaPix), 0.001)
        assertEquals("Conta Inter", service.extractStoreOrBeneficiary(sofisaPix))

        // Pix Enviado - Nubank
        val nuPix = "Você transferiu R$ 25,00 para Carlos Silva via Pix"
        assertFalse(service.isNonTransactionNotification(nuPix))
        assertTrue(service.isValidTransactionConfirmation(nuPix))
        assertEquals(25.0, service.extractAmount(nuPix), 0.001)
        assertEquals("Carlos Silva", service.extractStoreOrBeneficiary(nuPix))

        // Pix Enviado - Mercado Pago
        val mpPix = "Seu Pix de R$ 80,00 para Beltrano foi enviado"
        assertFalse(service.isNonTransactionNotification(mpPix))
        assertTrue(service.isValidTransactionConfirmation(mpPix))
        assertEquals(80.0, service.extractAmount(mpPix), 0.001)
        assertEquals("Beltrano", service.extractStoreOrBeneficiary(mpPix))

        // Pix Recebido - Nubank
        val nuRec = "Você recebeu uma transferência de R$ 100,00 de Beltrano"
        assertFalse(service.isNonTransactionNotification(nuRec))
        assertTrue(service.isValidTransactionConfirmation(nuRec))
        assertEquals(100.0, service.extractAmount(nuRec), 0.001)
        assertEquals("Beltrano", service.extractStoreOrBeneficiary(nuRec))

        // Pix Recebido - "Fulano te enviou um Pix"
        val teEnviou = "Mariana te enviou um Pix de R$ 50,00"
        assertFalse(service.isNonTransactionNotification(teEnviou))
        assertTrue(service.isValidTransactionConfirmation(teEnviou))
        assertEquals(50.0, service.extractAmount(teEnviou), 0.001)
        assertEquals("Mariana", service.extractStoreOrBeneficiary(teEnviou))
    }

    @Test
    fun testCurrencyVariations() {
        assertEquals(13.0, service.extractAmount("Compra de R$ 13,00 aprovada"), 0.001)
        assertEquals(13.0, service.extractAmount("Compra de R$ 13.00 aprovada"), 0.001)
        assertEquals(13.0, service.extractAmount("Compra de R$13 aprovada"), 0.001)
        assertEquals(13.0, service.extractAmount("Você pagou 13 reais em Café"), 0.001)
        assertEquals(1250.50, service.extractAmount("Pix de R$ 1.250,50 enviado"), 0.001)
        assertEquals(1250.0, service.extractAmount("Pix de R$ 1.250 enviado"), 0.001)
    }

    @Test
    fun testGatewayPrefixRemoval() {
        assertEquals("Padaria Central", TransactionDescriptionCleaner.cleanDescription("PAG*PADARIA CENTRAL"))
        assertEquals("Burger King", TransactionDescriptionCleaner.cleanDescription("IFD*BURGER KING"))
        assertEquals("Posto Ipiranga", TransactionDescriptionCleaner.cleanDescription("STONE *POSTO IPIRANGA"))
        assertEquals("Jose Miguel de Paula", TransactionDescriptionCleaner.cleanDescription("MP *JOSEMIGUELDEPAULA"))
    }

    @Test
    fun testAntiMarketingAndNonTransactionsIgnored() {
        // Oferta de empréstimo
        assertTrue(service.isNonTransactionNotification("Você tem um empréstimo pré-aprovado de R$ 5.000 no Inter"))

        // Limite aumentado
        assertTrue(service.isNonTransactionNotification("Liberamos um limite aprovado para você de R$ 2.000"))

        // Fatura fechada
        assertTrue(service.isNonTransactionNotification("Sua fatura fechou no valor de R$ 1.500,00"))

        // Rendimento CDI
        assertTrue(service.isNonTransactionNotification("Seu dinheiro está rendendo a 115% do CDI na conta"))

        // Boleto DDA
        assertTrue(service.isNonTransactionNotification("Novo boleto disponível no DDA no valor de R$ 250,00"))

        // Segurança / Token
        assertTrue(service.isNonTransactionNotification("Seu código de segurança para autorizar o acesso é 123456"))

        // Compra negada por saldo / limite
        assertTrue(service.isNonTransactionNotification("Compra negada: saldo insuficiente"))
        assertFalse(service.isValidTransactionConfirmation("Compra negada no valor de R$ 150,00"))
    }
}
