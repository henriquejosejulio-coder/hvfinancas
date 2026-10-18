package com.example.mobillsfinancas.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object Formatters {
    private val ptBrLocale = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(ptBrLocale)
    private val brDecimalSymbols = DecimalFormatSymbols(ptBrLocale).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val decimalFormat = DecimalFormat("#,##0.00", brDecimalSymbols)

    fun formatMoney(amount: Double, hideValues: Boolean = false): String {
        if (hideValues) return "R$ ••••••"
        return currencyFormat.format(amount)
    }

    /**
     * Formata um valor double como string monetária sem o prefixo R$ (ex: 1250.5 -> "1.250,50")
     */
    fun formatCurrencyPlain(amount: Double): String {
        if (amount <= 0.0) return ""
        return decimalFormat.format(amount)
    }

    /**
     * Aplica máscara de moeda em tempo real baseada em centavos (estilo Nubank/Mobills).
     * Exemplo de digitação sequencial:
     * "1" -> "0,01"
     * "16" -> "0,16"
     * "163" -> "1,63"
     * "1630" -> "16,30"
     * "16300" -> "163,00"
     * "163000" -> "1.630,00"
     * "1630000" -> "16.300,00"
     */
    fun formatCurrencyInput(input: String): String {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val cleanDigits = if (digits.length > 12) digits.take(12) else digits
        val cents = cleanDigits.toLongOrNull() ?: 0L
        if (cents == 0L) return ""
        val doubleVal = cents / 100.0
        return decimalFormat.format(doubleVal)
    }

    /**
     * Converte de forma segura qualquer valor monetário (formatado com . ou , ou padrão BR) em Double.
     */
    fun parseCurrency(text: String): Double {
        if (text.isBlank()) return 0.0
        val clean = text.trim().replace("R$", "").replace(" ", "")
        return if (clean.contains(",")) {
            val normalized = clean.replace(".", "").replace(",", ".")
            normalized.toDoubleOrNull() ?: 0.0
        } else {
            clean.toDoubleOrNull() ?: 0.0
        }
    }

    fun todayDateString(): String {
        return java.text.SimpleDateFormat("dd/MM/yyyy", ptBrLocale).format(java.util.Date())
    }
}

