package com.example.mobillsfinancas

import com.example.mobillsfinancas.utils.TransactionDescriptionCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionDescriptionCleanerTest {

    @Test
    fun testCleanMercadoPagoUserHandle() {
        val raw = "Você pagou Josemigueldepaula03807"
        val clean = TransactionDescriptionCleaner.cleanDescription(raw, "Outros", "Mercado Pago 2")
        assertEquals("Jose Miguel de Paula", clean)
    }

    @Test
    fun testCleanStoreWithPrefix() {
        val raw1 = "Você pagou Auto posto veredas exp"
        val clean1 = TransactionDescriptionCleaner.cleanDescription(raw1, "Transporte", "Mercado Pago 2")
        assertEquals("Auto Posto Veredas Exp", clean1)

        val raw2 = "Você pagou Disk cerveja ferreira"
        val clean2 = TransactionDescriptionCleaner.cleanDescription(raw2, "Outros", "Mercado Pago 2")
        assertEquals("Disk Cerveja Ferreira", clean2)
    }

    @Test
    fun testCleanGenericBankTitle() {
        val raw1 = "PIX realizado com sucesso"
        val clean1 = TransactionDescriptionCleaner.cleanDescription(raw1, "Outros", "Sofisa Direto")
        assertEquals("Pix Enviado", clean1)

        val raw2 = "Conta Inter"
        val clean2 = TransactionDescriptionCleaner.cleanDescription(raw2, "Mercado", "Conta Inter")
        assertEquals("Mercado", clean2)
    }

    @Test
    fun testCleanTransferPrefix() {
        val raw = "Transferência: Conta Inter ➔ Mercado Pago 2"
        val clean = TransactionDescriptionCleaner.cleanDescription(raw, "Transferência", "Conta Inter")
        assertEquals("Conta Inter ➔ Mercado Pago 2", clean)
    }

    @Test
    fun testPreserveKnownBrands() {
        assertEquals("iFood", TransactionDescriptionCleaner.cleanDescription("iFood"))
        assertEquals("Uber", TransactionDescriptionCleaner.cleanDescription("Uber"))
        assertEquals("Netflix", TransactionDescriptionCleaner.cleanDescription("netflix"))
    }
}
