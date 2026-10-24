package com.example.mobillsfinancas

import com.example.mobillsfinancas.utils.BackupManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCountItems_emptyOrInvalid() {
        assertEquals(-1, BackupManager.countItems(null))
        assertEquals(-1, BackupManager.countItems(""))
        assertEquals(-1, BackupManager.countItems("   "))
        assertEquals(-1, BackupManager.countItems("abc"))
        assertEquals(-1, BackupManager.countItems("{ invalid json"))
    }

    @Test
    fun testCountItems_validJsonObject() {
        val json = """
            {
              "categories": [],
              "months": [
                {
                  "mes_ano": "OUTUBRO 2026",
                  "itens": [
                    {"id": "1", "description": "Mercado", "amount": 150.0},
                    {"id": "2", "description": "Farmácia", "amount": 50.0}
                  ]
                },
                {
                  "mes_ano": "NOVEMBRO 2026",
                  "itens": [
                    {"id": "3", "description": "Internet", "amount": 100.0}
                  ]
                }
              ]
            }
        """.trimIndent()

        val count = BackupManager.countItems(json)
        assertEquals(3, count)
    }

    @Test
    fun testCountItems_validJsonArray() {
        val json = """
            [
              {
                "mes_ano": "OUTUBRO 2026",
                "itens": [
                  {"id": "1", "description": "Mercado", "amount": 150.0}
                ]
              }
            ]
        """.trimIndent()

        val count = BackupManager.countItems(json)
        assertEquals(1, count)
    }

    @Test
    fun testCountItems_resilientToMissingItens() {
        val json = """
            {
              "months": [
                {
                  "mes_ano": "JANEIRO 2025"
                }
              ]
            }
        """.trimIndent()

        val count = BackupManager.countItems(json)
        assertEquals(0, count)
    }

    @Test
    fun testAtomicWrite_writesCorrectContent() {
        val file = File(tempFolder.root, "test_backup.json")
        val content = "{\"test\": \"ok\", \"months\": []}"

        BackupManager.atomicWrite(file, content)

        assertTrue(file.exists())
        assertEquals(content, file.readText())
    }

    @Test
    fun testAtomicWrite_overwritesExistingCleanly() {
        val file = File(tempFolder.root, "overwrite.json")
        BackupManager.atomicWrite(file, "initial content")
        assertEquals("initial content", file.readText())

        val newContent = "{\"updated\": true}"
        BackupManager.atomicWrite(file, newContent)
        assertEquals(newContent, file.readText())
    }
}
