package com.example.mobillsfinancas

import com.example.mobillsfinancas.utils.BankRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BankRegistryTest {

    @Test
    fun testBancoInterDetection() {
        val bankByPkg = BankRegistry.findBankByPackageOrContent("br.com.intermedium", "Compra no crédito", "Você acaba de comprar R$ 26,90")
        assertNotNull(bankByPkg)
        assertEquals("inter", bankByPkg?.key)
        assertEquals("Banco Inter", bankByPkg?.name)
        assertEquals("Cartão Inter", bankByPkg?.cardName)

        val bankByContent = BankRegistry.findBankByPackageOrContent("some.launcher.app", "Inter", "Você comprou R$ 10")
        assertNotNull(bankByContent)
        assertEquals("inter", bankByContent?.key)
    }

    @Test
    fun testMercadoPagoDetection() {
        val bankByPkg = BankRegistry.findBankByPackageOrContent("com.mercadopago.wallet", "Mercado Pago", "Você recebeu um Pix de R$ 50,00")
        assertNotNull(bankByPkg)
        assertEquals("mercadopago", bankByPkg?.key)
        assertEquals("Mercado Pago", bankByPkg?.name)

        val bankByKeywords = BankRegistry.findBankByPackageOrContent("com.android.systemui", "Notificação", "Transferência enviada no Mercado Pago")
        assertNotNull(bankByKeywords)
        assertEquals("mercadopago", bankByKeywords?.key)
    }

    @Test
    fun testNubankDetection() {
        val bank = BankRegistry.findBankByPackageOrContent("com.nu.production", "Nubank", "Compra de R$ 45,00 aprovada no cartão Nubank")
        assertNotNull(bank)
        assertEquals("nubank", bank?.key)
        assertEquals("Nubank", bank?.name)
    }

    @Test
    fun testC6BankDetection() {
        val bank = BankRegistry.findBankByPackageOrContent("com.c6bank.app", "C6 Bank", "Compra aprovada no seu C6")
        assertNotNull(bank)
        assertEquals("c6", bank?.key)
    }

    @Test
    fun testFindBankByName() {
        assertEquals("Banco Inter", BankRegistry.findBankByName("Cartão Inter")?.name)
        assertEquals("Mercado Pago", BankRegistry.findBankByName("Mercado Pago 2")?.name)
        assertEquals("Nubank", BankRegistry.findBankByName("Cartão Nubank")?.name)
        assertEquals("Sofisa Direto", BankRegistry.findBankByName("Sofisa")?.name)
    }
}
