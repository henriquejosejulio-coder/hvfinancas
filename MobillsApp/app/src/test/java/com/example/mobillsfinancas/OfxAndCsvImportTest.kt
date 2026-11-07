package com.example.mobillsfinancas

import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfxAndCsvImportTest {

    @Test
    fun testParseOfxContent() {
        val ofxSample = """
            OFXHEADER:100
            DATA:OFXSGML
            <OFX>
            <BANKMSGSRSV1>
            <STMTTRNRS>
            <STMTRS>
            <BANKTRANLIST>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20261103120000[-03:EST]
            <TRNAMT>-26.90
            <MEMO>DL*GOOGLE YouTub
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>DEBIT
            <DTPOSTED>20261104100000[-03:EST]
            <TRNAMT>-93.56
            <MEMO>SUPERMERCADO ARMACAO
            </STMTTRN>
            <STMTTRN>
            <TRNTYPE>CREDIT
            <DTPOSTED>20261105080000[-03:EST]
            <TRNAMT>400.00
            <NAME>Pix de Arrighi C D Ltda
            </STMTTRN>
            </BANKTRANLIST>
            </STMTRS>
            </STMTTRNRS>
            </BANKMSGSRSV1>
            </OFX>
        """.trimIndent()

        val trnRegex = Regex("<STMTTRN>[\\s\\S]*?</STMTTRN>", RegexOption.IGNORE_CASE)
        val trnamtRegex = Regex("<TRNAMT>([+-]?[0-9.,]+)", RegexOption.IGNORE_CASE)
        val memoRegex = Regex("<MEMO>([^<\\r\\n]+)", RegexOption.IGNORE_CASE)
        val nameRegex = Regex("<NAME>([^<\\r\\n]+)", RegexOption.IGNORE_CASE)
        val dateRegex = Regex("<DTPOSTED>(\\d{4})(\\d{2})(\\d{2})", RegexOption.IGNORE_CASE)

        val matches = trnRegex.findAll(ofxSample).toList()
        assertEquals(3, matches.size)

        val parsedItems = mutableListOf<TransactionItem>()
        for (m in matches) {
            val block = m.value
            val amtMatch = trnamtRegex.find(block)
            val memoMatch = memoRegex.find(block) ?: nameRegex.find(block)
            val dateMatch = dateRegex.find(block)

            if (amtMatch != null) {
                val amtVal = amtMatch.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 0.0
                val desc = memoMatch?.groupValues?.get(1)?.trim() ?: "Compra OFX"
                val dateStr = if (dateMatch != null) {
                    "${dateMatch.groupValues[3]}/${dateMatch.groupValues[2]}/${dateMatch.groupValues[1]}"
                } else "Hoje"

                val isExpense = amtVal < 0
                val absAmount = Math.abs(amtVal)

                parsedItems.add(
                    TransactionItem(
                        id = "ofx_${parsedItems.size}",
                        description = desc,
                        amount = absAmount,
                        type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                        accountOrCardName = "Cartão Inter",
                        paymentType = PaymentType.CREDIT_CARD,
                        monthYear = "NOVEMBRO 2026",
                        date = dateStr,
                        status = PaymentStatus.PAID
                    )
                )
            }
        }

        assertEquals(3, parsedItems.size)

        // Item 1: YouTube
        assertEquals("DL*GOOGLE YouTub", parsedItems[0].description)
        assertEquals(26.90, parsedItems[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsedItems[0].type)
        assertEquals("03/11/2026", parsedItems[0].date)
        assertEquals(PaymentStatus.PAID, parsedItems[0].status)

        // Item 2: Armação
        assertEquals("SUPERMERCADO ARMACAO", parsedItems[1].description)
        assertEquals(93.56, parsedItems[1].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsedItems[1].type)
        assertEquals("04/11/2026", parsedItems[1].date)

        // Item 3: Pix Crédito
        assertEquals("Pix de Arrighi C D Ltda", parsedItems[2].description)
        assertEquals(400.00, parsedItems[2].amount, 0.001)
        assertEquals(TransactionType.INCOME, parsedItems[2].type)
        assertEquals("05/11/2026", parsedItems[2].date)
    }

    @Test
    fun testParseCsvContent() {
        val csvSample = """
            Data;Descrição;Valor
            03/11/2026;Uber; -7,98
            04/11/2026;Luz; -200,00
            05/11/2026;Pix Recebido; +100,00
        """.trimIndent()

        val parsedItems = mutableListOf<TransactionItem>()
        for (line in csvSample.lines()) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            val separator = if (trimmedLine.contains(";")) ";" else ","
            val parts = trimmedLine.split(separator)
            if (parts.size >= 3) {
                val dateStr = parts[0].trim()
                val desc = parts[1].trim()
                val amtStr = parts[2].trim().replace("R$", "").replace(" ", "").replace(",", ".").replace("+", "")
                val amt = amtStr.toDoubleOrNull()
                if (amt != null && !desc.equals("descrição", ignoreCase = true)) {
                    val absAmt = Math.abs(amt)
                    val isExp = amt < 0 || !desc.contains("Receb", ignoreCase = true)
                    parsedItems.add(
                        TransactionItem(
                            id = "csv_${parsedItems.size}",
                            description = desc,
                            amount = absAmt,
                            type = if (isExp) TransactionType.EXPENSE else TransactionType.INCOME,
                            monthYear = "NOVEMBRO 2026",
                            date = dateStr,
                            status = PaymentStatus.PAID
                        )
                    )
                }
            }
        }

        assertEquals(3, parsedItems.size)
        assertEquals("Uber", parsedItems[0].description)
        assertEquals(7.98, parsedItems[0].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsedItems[0].type)

        assertEquals("Luz", parsedItems[1].description)
        assertEquals(200.00, parsedItems[1].amount, 0.001)
        assertEquals(TransactionType.EXPENSE, parsedItems[1].type)

        assertEquals("Pix Recebido", parsedItems[2].description)
        assertEquals(100.00, parsedItems[2].amount, 0.001)
        assertEquals(TransactionType.INCOME, parsedItems[2].type)
    }
}
