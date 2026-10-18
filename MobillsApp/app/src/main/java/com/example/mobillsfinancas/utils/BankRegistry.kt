package com.example.mobillsfinancas.utils

data class SupportedBank(
    val key: String,
    val name: String,
    val cardName: String,
    val colorHex: Long,
    val icon: String,
    val packageNames: List<String>,
    val keywords: List<String>
)

object BankRegistry {
    val ALL_BANKS = listOf(
        SupportedBank(
            key = "nubank",
            name = "Nubank",
            cardName = "Cartão Nubank",
            colorHex = 0xFF820AD1L, // Roxo Nubank oficial
            icon = "🟣",
            packageNames = listOf("com.nu.production", "br.com.nubank"),
            keywords = listOf("nubank", "nu pagamentos", "nu financeira", "conta do nubank", "cartão nubank", "cartao nubank")
        ),
        SupportedBank(
            key = "mercadopago",
            name = "Mercado Pago",
            cardName = "Cartão Mercado Pago",
            colorHex = 0xFF009EE3L, // Azul Mercado Pago
            icon = "📱",
            packageNames = listOf("com.mercadopago.wallet", "com.mercadolivre", "com.mercadolibre"),
            keywords = listOf("mercado pago", "mercadopago", "mercadolivre", "mercado livre")
        ),
        SupportedBank(
            key = "inter",
            name = "Banco Inter",
            cardName = "Cartão Inter",
            colorHex = 0xFFFF7A00L, // Laranja Inter
            icon = "🟠",
            packageNames = listOf("br.com.intermedium", "br.com.bancointer"),
            keywords = listOf("banco inter", "intermedium", "conta inter", "cartão inter", "cartao inter")
        ),
        SupportedBank(
            key = "c6",
            name = "C6 Bank",
            cardName = "Cartão C6",
            colorHex = 0xFF242424L, // Carbon C6
            icon = "⬛",
            packageNames = listOf("com.c6bank.app"),
            keywords = listOf("c6 bank", "c6bank", "c6", "carbon")
        ),
        SupportedBank(
            key = "itau",
            name = "Banco Itaú",
            cardName = "Cartão Itaú",
            colorHex = 0xFFEC7000L, // Laranja Itaú
            icon = "🟧",
            packageNames = listOf("com.itau", "com.itau.personnalite", "com.itau.cartoes", "com.iti"),
            keywords = listOf("banco itaú", "banco itau", "itaú", "itau", "iti itau", "iti")
        ),
        SupportedBank(
            key = "bradesco",
            name = "Bradesco",
            cardName = "Cartão Bradesco",
            colorHex = 0xFFCC092FL, // Vermelho Bradesco
            icon = "🔴",
            packageNames = listOf("com.bradesco", "com.bradesco.cartoes", "com.bradesco.prime"),
            keywords = listOf("bradesco", "banco bradesco", "bradesco prime")
        ),
        SupportedBank(
            key = "santander",
            name = "Santander",
            cardName = "Cartão Santander",
            colorHex = 0xFFEC0000L, // Vermelho Santander
            icon = "🟥",
            packageNames = listOf("com.santander.app", "com.santander.way"),
            keywords = listOf("santander", "banco santander", "way santander")
        ),
        SupportedBank(
            key = "caixa",
            name = "Caixa Econômica",
            cardName = "Cartão Caixa",
            colorHex = 0xFF005CA9L, // Azul Caixa
            icon = "🏦",
            packageNames = listOf("br.com.gabba.Caixa", "br.gov.caixa.tem", "br.com.caixa"),
            keywords = listOf("caixa econômica", "caixa economica", "caixa tem", "habitação caixa")
        ),
        SupportedBank(
            key = "bb",
            name = "Banco do Brasil",
            cardName = "Cartão Banco do Brasil",
            colorHex = 0xFF003882L, // Azul BB
            icon = "🟡",
            packageNames = listOf("br.com.bb.android"),
            keywords = listOf("banco do brasil", "ourocard", "bb s.a.", "bb sa")
        ),
        SupportedBank(
            key = "picpay",
            name = "PicPay",
            cardName = "Cartão PicPay",
            colorHex = 0xFF11C76FL, // Verde PicPay
            icon = "🟢",
            packageNames = listOf("com.picpay"),
            keywords = listOf("picpay", "pic pay")
        ),
        SupportedBank(
            key = "will",
            name = "Will Bank",
            cardName = "Cartão Will",
            colorHex = 0xFFFFDE59L, // Amarelo Will
            icon = "🟨",
            packageNames = listOf("com.willbank.app"),
            keywords = listOf("will bank", "willbank", "meu will")
        ),
        SupportedBank(
            key = "sofisa",
            name = "Sofisa Direto",
            cardName = "Cartão Sofisa",
            colorHex = 0xFF005C8AL, // Azul Sofisa
            icon = "🔷",
            packageNames = listOf("br.com.sofisa.direto"),
            keywords = listOf("sofisa direto", "sofisa", "banco sofisa")
        ),
        SupportedBank(
            key = "pagbank",
            name = "PagBank",
            cardName = "Cartão PagBank",
            colorHex = 0xFF00A868L, // Verde PagBank
            icon = "💳",
            packageNames = listOf("br.com.uol.ps.myaccount", "br.com.uol.ps"),
            keywords = listOf("pagbank", "pagseguro", "uol pagseguro")
        ),
        SupportedBank(
            key = "neon",
            name = "Neon",
            cardName = "Cartão Neon",
            colorHex = 0xFF00E5FFL, // Azul Neon
            icon = "💠",
            packageNames = listOf("br.com.neon"),
            keywords = listOf("neon", "banco neon")
        ),
        SupportedBank(
            key = "next",
            name = "Next",
            cardName = "Cartão Next",
            colorHex = 0xFF00FF5FL, // Verde Next
            icon = "🟢",
            packageNames = listOf("br.com.bradesco.next"),
            keywords = listOf("banco next", "next")
        ),
        SupportedBank(
            key = "sicoob",
            name = "Sicoob",
            cardName = "Cartão Sicoob",
            colorHex = 0xFF003641L, // Verde Sicoob
            icon = "🌿",
            packageNames = listOf("br.com.sicoob.netcorp", "br.com.sicoobmobile"),
            keywords = listOf("sicoob", "bancoob")
        ),
        SupportedBank(
            key = "sicredi",
            name = "Sicredi",
            cardName = "Cartão Sicredi",
            colorHex = 0xFF007F3DL, // Verde Sicredi
            icon = "🟢",
            packageNames = listOf("br.com.sicredi.mobile"),
            keywords = listOf("sicredi")
        ),
        SupportedBank(
            key = "btg",
            name = "BTG Pactual",
            cardName = "Cartão BTG",
            colorHex = 0xFF001E62L, // Azul Escuro BTG
            icon = "🏦",
            packageNames = listOf("br.com.btg.pactual.banking"),
            keywords = listOf("btg pactual", "banco btg")
        ),
        SupportedBank(
            key = "recargapay",
            name = "RecargaPay",
            cardName = "Cartão RecargaPay",
            colorHex = 0xFFFF4B00L, // Laranja RecargaPay
            icon = "⚡",
            packageNames = listOf("com.recargapay"),
            keywords = listOf("recargapay", "recarga pay")
        )
    )

    fun findBankByPackageOrContent(packageName: String, title: String, content: String): SupportedBank? {
        val pkg = packageName.lowercase()
        val text = "$title $content".lowercase()

        // 1. Match por pacote Android exato
        val byPackage = ALL_BANKS.find { bank ->
            bank.packageNames.any { p -> pkg.contains(p) || p.contains(pkg) }
        }
        if (byPackage != null) return byPackage

        // 2. Match especial para Banco Inter quando o título é "Inter"
        if (title.equals("Inter", ignoreCase = true) || title.startsWith("Inter ", ignoreCase = true)) {
            return ALL_BANKS.find { it.key == "inter" }
        }

        // 3. Match por palavras-chave
        return ALL_BANKS.find { bank ->
            bank.keywords.any { kw -> text.contains(kw) }
        }
    }

    fun findBankByName(name: String): SupportedBank? {
        val clean = name.lowercase().trim()
        return ALL_BANKS.find { bank ->
            bank.name.lowercase().contains(clean) ||
            clean.contains(bank.name.lowercase()) ||
            bank.cardName.lowercase().contains(clean) ||
            clean.contains(bank.cardName.lowercase()) ||
            bank.key == clean ||
            bank.keywords.any { kw -> clean.contains(kw) || kw.contains(clean) }
        }
    }
}
