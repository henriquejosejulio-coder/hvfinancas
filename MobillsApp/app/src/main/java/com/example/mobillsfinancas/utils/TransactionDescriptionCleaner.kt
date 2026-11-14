package com.example.mobillsfinancas.utils

import java.util.Locale

object TransactionDescriptionCleaner {

    private val KNOWN_BRANDS = mapOf(
        "ifood" to "iFood",
        "uber" to "Uber",
        "google" to "Google",
        "youtube" to "YouTube",
        "youtub" to "YouTube",
        "netflix" to "Netflix",
        "spotify" to "Spotify",
        "amazon" to "Amazon",
        "mercado livre" to "Mercado Livre",
        "mercadolivre" to "Mercado Livre",
        "shopee" to "Shopee",
        "playstation" to "PlayStation"
    )

    private val PORTUGUESE_NAME_TOKENS = listOf(
        "jose", "miguel", "paula", "silva", "santos", "oliveira", "souza", "sousa",
        "rodrigues", "ferreira", "alves", "pereira", "lima", "gomes", "costa", "ribeiro",
        "martins", "carvalho", "almeida", "lopes", "soares", "fernandes", "vieira", "barbosa",
        "rocha", "dias", "nascimento", "andrade", "moreira", "nunes", "marques", "machado",
        "mendes", "freitas", "cardoso", "ramos", "goncalves", "santana", "teixeira", "henrique",
        "vital", "junior", "neto", "filho", "carlos", "eduardo", "lucas", "gabriel", "matheus",
        "felipe", "guilherme", "pedro", "joao", "marcos", "vinicius", "rafael", "bruno",
        "ana", "maria", "julia", "beatriz", "larissa", "camila", "fernanda", "amanda", "leticia",
        "posto", "cerveja", "distribuidora", "auto", "veredas", "exp",
        "de", "da", "do", "dos", "das"
    )

    fun cleanDescription(rawDesc: String, category: String = "", accountName: String = ""): String {
        var s = rawDesc.trim()
        if (s.isEmpty()) return s

        // 1. Limpa prefixo redundante de "Transferência: " se já tiver seta indicativa
        val transferPrefixRegex = Regex("^transfer[eê]ncia:\\s*", RegexOption.IGNORE_CASE)
        if (transferPrefixRegex.containsMatchIn(s)) {
            val after = s.replace(transferPrefixRegex, "").trim()
            if (after.contains("➔") || after.contains("->")) {
                s = after
            }
        }

        // 2. Remove frases conversacionais comuns de notificações de bancos
        s = s.replace(Regex("^(?:você|voce)\\s+pagou\\s+(?:a\\s+|em\\s+|para\\s+)?", RegexOption.IGNORE_CASE), "")
             .replace(Regex("^(?:você|voce)\\s+transferiu\\s+(?:para\\s+|a\\s+)?", RegexOption.IGNORE_CASE), "")
             .replace(Regex("^(?:você|voce)\\s+recebeu\\s+(?:de\\s+|um\\s+pix\\s+de\\s+)?", RegexOption.IGNORE_CASE), "")
             .replace(Regex("^mp\\s*[*_\\s]\\s*", RegexOption.IGNORE_CASE), "")
             .trim()

        // 3. Trata títulos genéricos que foram capturados em vez do estabelecimento real
        val lower = s.lowercase(Locale.ROOT)
        if (lower == "pix realizado com sucesso" || lower == "pix enviado com sucesso" ||
            lower == "transferência realizada" || lower == "transferencia realizada" ||
            lower == "pix efetuado") {
            return "Pix Enviado"
        }

        val isGenericAccountName = (accountName.isNotBlank() && lower == accountName.trim().lowercase(Locale.ROOT)) ||
                lower == "conta inter" || lower == "mercado pago" || lower == "mercado pago 2" || lower == "sofisa direto"

        if (isGenericAccountName) {
            val catTrimmed = category.trim()
            if (catTrimmed.isNotBlank() && !catTrimmed.equals("outros", ignoreCase = true) && !catTrimmed.equals("geral", ignoreCase = true)) {
                return catTrimmed
            }
            return "Compra no Débito"
        }

        // 4. Trata nomes aglutinados com dígitos (ex: "josemigueldepaula03807")
        val handleMatch = Regex("^([a-zA-ZÀ-ÖØ-öø-ÿ]+)(\\d{2,})$").matchEntire(s)
        if (handleMatch != null) {
            val letters = handleMatch.groupValues[1]
            val split = splitConcatenatedName(letters)
            if (split.isNotBlank()) {
                s = split
            }
        } else if (Regex("^[a-z]{8,}$").matches(s)) {
            // Palavra longa totalmente minúscula sem espaço
            val split = splitConcatenatedName(s)
            if (split.isNotBlank()) {
                s = split
            }
        }

        // 5. Remove sufixos jurídicos empresariais que poluem
        s = s.replace(Regex("(?i)\\b(TECNOLOGIA LTDA|INSTITUICAO DE PAGAMENTO|PAGAMENTOS LTDA|PAGAMENTOS|LTDA|S\\.A\\.|ME|EPP)\\b"), "")
             .replace(Regex("\\s+"), " ")
             .trim()

        // 6. Formata com Title Case elegante
        s = titleCaseWords(s)

        return s.ifEmpty { rawDesc }
    }

    private fun splitConcatenatedName(letters: String): String {
        var rem = letters.lowercase(Locale.ROOT)
        val parts = mutableListOf<String>()
        val sortedTokens = PORTUGUESE_NAME_TOKENS.sortedByDescending { it.length }

        while (rem.isNotEmpty()) {
            var matched = false
            for (token in sortedTokens) {
                if (rem.startsWith(token)) {
                    parts.add(token)
                    rem = rem.substring(token.length)
                    matched = true
                    break
                }
            }
            if (!matched) {
                parts.add(rem)
                break
            }
        }

        return if (parts.size > 1) {
            parts.joinToString(" ") { p ->
                if (p in listOf("de", "da", "do", "dos", "das")) p
                else p.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        } else {
            letters.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    private fun titleCaseWords(s: String): String {
        val words = s.split(" ")
        if (words.isEmpty()) return s

        val out = mutableListOf<String>()
        for ((idx, w) in words.withIndex()) {
            val wl = w.lowercase(Locale.ROOT)
            val brand = KNOWN_BRANDS[wl]
            when {
                brand != null -> out.add(brand)
                wl in listOf("de", "da", "do", "dos", "das", "e") && idx > 0 -> out.add(wl)
                wl in listOf("sa", "me", "epp", "ltda", "cdi") -> out.add(wl.uppercase(Locale.ROOT))
                wl == "pix" -> out.add("Pix")
                w.all { it.isUpperCase() } && w.length <= 3 -> out.add(w)
                w.all { it.isLowerCase() } || w.all { it.isUpperCase() } -> {
                    out.add(w.lowercase(Locale.ROOT).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    })
                }
                else -> out.add(w) // mantém misto preservado
            }
        }
        return out.joinToString(" ")
    }
}
