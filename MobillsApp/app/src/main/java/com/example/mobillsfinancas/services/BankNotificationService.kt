package com.example.mobillsfinancas.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobillsfinancas.MainActivity
import com.example.mobillsfinancas.data.FinanceRepository
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import com.example.mobillsfinancas.utils.BankRegistry
import com.example.mobillsfinancas.utils.MonthHelper
import com.example.mobillsfinancas.utils.SupportedBank

class BankNotificationService : NotificationListenerService() {

    private lateinit var repository: FinanceRepository
    private val CHANNEL_ID = "mobills_auto_capture"

    // Cache simples para evitar registrar a mesma compra duplicada em menos de 2 minutos
    private val recentCaptures = mutableMapOf<String, Long>()

    companion object {
        var isConnected: Boolean = false
            private set

        fun saveDebugLog(context: Context, logEntry: String) {
            try {
                val prefs = context.getSharedPreferences("bank_debug_logs", Context.MODE_PRIVATE)
                val existing = prefs.getString("logs", "") ?: ""
                val timestamp = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date())
                val newLogs = "[$timestamp] $logEntry\n$existing".take(1500)
                prefs.edit().putString("logs", newLogs).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getDebugLogs(context: Context): String {
            return try {
                val prefs = context.getSharedPreferences("bank_debug_logs", Context.MODE_PRIVATE)
                prefs.getString("logs", "Nenhuma notificação capturada recentemente.") ?: "Nenhuma notificação capturada recentemente."
            } catch (e: Exception) {
                "Sem registros disponíveis."
            }
        }

        fun clearDebugLogs(context: Context) {
            try {
                val prefs = context.getSharedPreferences("bank_debug_logs", Context.MODE_PRIVATE)
                prefs.edit().remove("logs").apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun simulateNotification(context: Context, packageName: String, title: String, text: String): Boolean {
            val service = BankNotificationService()
            service.repository = FinanceRepository.getInstance(context.applicationContext)
            return service.processNotificationInternal(context.applicationContext, packageName, title, text, "$title $text", isSimulation = true)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            repository = FinanceRepository.getInstance(applicationContext)
            createNotificationChannel()
            Log.d("BankNotificationService", "BankNotificationService iniciado com sucesso")
        } catch (t: Throwable) {
            Log.e("BankNotificationService", "Erro durante onCreate do serviço", t)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        saveDebugLog(applicationContext, "✅ Sincronizador CONECTADO ao Android com sucesso.")
        Log.d("BankNotificationService", "✅ Sincronizador de Notificações CONECTADO com sucesso ao Android!")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        saveDebugLog(applicationContext, "⚠️ Sincronizador DESCONECTADO pelo Android. Tentando reconectar...")
        Log.d("BankNotificationService", "⚠️ Sincronizador de Notificações DESCONECTADO pelo Android. Solicitando reconexão...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(android.content.ComponentName(this, BankNotificationService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lançamentos Automáticos"
            val descriptionText = "Avisos quando uma compra ou Pix do banco é registrado"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName?.lowercase() ?: ""
        val myPackageName = applicationContext.packageName.lowercase()

        // 1. NUNCA processar notificações do nosso próprio aplicativo!
        if (packageName == myPackageName ||
            packageName == "com.henriquevital.hvfinancas" ||
            packageName == "com.example.mobillsfinancas" ||
            packageName.contains("hvfinancas") ||
            packageName.contains("mobillsfinancas")) {
            return
        }

        val extras = sbn.notification.extras ?: return

        // Extrai com segurança todos os possíveis campos de texto da notificação
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""
        val infoText = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString() ?: ""

        val rawContent = "$title $text $bigText $subText $summaryText $infoText"
        processNotificationInternal(applicationContext, packageName, title, text, rawContent, isSimulation = false)
    }

    fun processNotificationInternal(
        context: Context,
        packageName: String,
        title: String,
        text: String,
        rawContent: String,
        isSimulation: Boolean = false
    ): Boolean {
        if (!::repository.isInitialized) {
            repository = FinanceRepository.getInstance(context)
        }

        // Normaliza espaços comuns e não-quebráveis (como \u00A0 e \u202F que bancos costumam usar)
        val fullContent = rawContent.replace('\u00A0', ' ').replace('\u202F', ' ').trim()

        if (fullContent.isBlank()) return false

        // Descarta se for notificação com nossas próprias mensagens
        if (fullContent.contains("Receita Registrada!", ignoreCase = true) ||
            fullContent.contains("Compra Registrada!", ignoreCase = true) ||
            fullContent.contains("HV Finanças", ignoreCase = true)) {
            return false
        }

        // 1. FILTRO ANTI-MARKETING / ANTI-PROMOÇÃO / INVESTIMENTOS / LEMBRETES
        // Descarta mensagens que NÃO são transações financeiras reais realizadas
        if (isNonTransactionNotification(fullContent)) {
            saveDebugLog(context, "🚫 Ignorado (promocional/marketing/informativo): $fullContent")
            Log.d("BankNotificationService", "Notificação ignorada (promocional/marketing): $fullContent")
            return false
        }

        // 2. VALIDAÇÃO POSITIVA: Só prossegue se for uma confirmação clara de operação financeira já realizada
        if (!isValidTransactionConfirmation(fullContent)) {
            saveDebugLog(context, "🚫 Ignorado (não é transação financeira realizada): $fullContent")
            Log.d("BankNotificationService", "Notificação ignorada (não é transação): $fullContent")
            return false
        }

        // Identificação inteligente dos bancos usando o BankRegistry oficial
        val detectedBank = BankRegistry.findBankByPackageOrContent(packageName, title, fullContent)
        val isKnownBank = detectedBank != null

        // Se não for um pacote conhecido de banco, verifica se é uma notificação com palavras-chave financeiras
        val hasFinancialKeywords = fullContent.contains("compra", ignoreCase = true) ||
                fullContent.contains("pagamento", ignoreCase = true) ||
                fullContent.contains("pagou", ignoreCase = true) ||
                fullContent.contains("aprovad", ignoreCase = true) ||
                fullContent.contains("pix", ignoreCase = true) ||
                fullContent.contains("transferência", ignoreCase = true) ||
                fullContent.contains("transferencia", ignoreCase = true) ||
                fullContent.contains("cartão", ignoreCase = true) ||
                fullContent.contains("cartao", ignoreCase = true) ||
                fullContent.contains("débito", ignoreCase = true) ||
                fullContent.contains("debito", ignoreCase = true) ||
                fullContent.contains("depositou", ignoreCase = true) ||
                fullContent.contains("recebeu", ignoreCase = true) ||
                fullContent.contains("enviou", ignoreCase = true) ||
                fullContent.contains("transferiu", ignoreCase = true) ||
                fullContent.contains("gasto", ignoreCase = true) ||
                fullContent.contains("gastou", ignoreCase = true)

        if (!isKnownBank && !hasFinancialKeywords) {
            return false
        }

        // 1. Extração robusta do valor em Real (com ou sem centavos, ex: R$ 11 ou R$ 11,00 ou 11 reais)
        val cleanAmount = extractAmount(fullContent)

        if (cleanAmount <= 0.0) {
            saveDebugLog(context, "Ignorado (valor zero): $fullContent")
            return false
        }

        // Evita duplicatas se o banco mandar 2 notificações da mesma compra em menos de 2 minutos
        val captureKey = "${cleanAmount}_$fullContent"
        val now = System.currentTimeMillis()
        if (!isSimulation && recentCaptures.containsKey(captureKey) && (now - (recentCaptures[captureKey] ?: 0)) < 120_000) {
            return false
        }
        recentCaptures[captureKey] = now

        // 2. Identificação precisa do Tipo de Operação:
        // A) É Receita / Depósito / Pix Recebido?
        val isIncome = fullContent.contains("Pix recebido", ignoreCase = true) ||
                fullContent.contains("recebeu um Pix", ignoreCase = true) ||
                fullContent.contains("recebeu via Pix", ignoreCase = true) ||
                fullContent.contains("recebeu uma transferência", ignoreCase = true) ||
                fullContent.contains("transferência recebida", ignoreCase = true) ||
                fullContent.contains("transferencia recebida", ignoreCase = true) ||
                fullContent.contains("te enviou um Pix", ignoreCase = true) ||
                fullContent.contains("crédito recebido", ignoreCase = true) ||
                fullContent.contains("credito recebido", ignoreCase = true) ||
                fullContent.contains("depositou", ignoreCase = true) ||
                fullContent.contains("depósito", ignoreCase = true) ||
                fullContent.contains("deposito", ignoreCase = true) ||
                fullContent.contains("dinheiro já está disponível", ignoreCase = true) ||
                fullContent.contains("dinheiro ja esta disponivel", ignoreCase = true) ||
                fullContent.contains("está rendendo na sua conta", ignoreCase = true) ||
                fullContent.contains("esta rendendo na sua conta", ignoreCase = true) ||
                fullContent.contains("creditado em sua conta", ignoreCase = true) ||
                fullContent.contains("creditado na sua conta", ignoreCase = true) ||
                fullContent.contains("reembolso", ignoreCase = true) ||
                fullContent.contains("estorno", ignoreCase = true) ||
                (fullContent.contains("você recebeu", ignoreCase = true) && !fullContent.contains("fatura", ignoreCase = true))

        // B) É Pix Enviado / Transferência Enviada? (Bancos, Gmail e e-mails de confirmação)
        val isPixEnviado = fullContent.contains("Pix enviado", ignoreCase = true) ||
                fullContent.contains("Pix foi enviado", ignoreCase = true) ||
                fullContent.contains("Seu Pix foi enviado", ignoreCase = true) ||
                fullContent.contains("Seu Pix de", ignoreCase = true) ||
                fullContent.contains("Pix de R$", ignoreCase = true) ||
                fullContent.contains("comprovante de Pix", ignoreCase = true) ||
                fullContent.contains("comprovante Pix", ignoreCase = true) ||
                fullContent.contains("enviou um Pix", ignoreCase = true) ||
                fullContent.contains("você transferiu", ignoreCase = true) ||
                fullContent.contains("voce transferiu", ignoreCase = true) ||
                fullContent.contains("transferência enviada", ignoreCase = true) ||
                fullContent.contains("transferencia enviada", ignoreCase = true) ||
                fullContent.contains("transferência realizada", ignoreCase = true) ||
                fullContent.contains("transferencia realizada", ignoreCase = true) ||
                fullContent.contains("transferência concluída", ignoreCase = true) ||
                fullContent.contains("transferencia concluida", ignoreCase = true) ||
                fullContent.contains("pagou com Pix", ignoreCase = true) ||
                fullContent.contains("Pix realizado", ignoreCase = true) ||
                fullContent.contains("Pix feito", ignoreCase = true) ||
                fullContent.contains("pagamento via Pix", ignoreCase = true) ||
                fullContent.contains("pix no valor de", ignoreCase = true) ||
                fullContent.contains("transferência no valor de", ignoreCase = true)

        // C) É Débito em Conta?
        val isDebito = fullContent.contains("débito", ignoreCase = true) ||
                fullContent.contains("debito", ignoreCase = true) ||
                fullContent.contains("saldo da conta", ignoreCase = true) ||
                fullContent.contains("usou seu saldo", ignoreCase = true) ||
                fullContent.contains("com seu saldo", ignoreCase = true) ||
                fullContent.contains("da sua conta", ignoreCase = true)

        val isCartaoCredito = fullContent.contains("crédito", ignoreCase = true) ||
                fullContent.contains("credito", ignoreCase = true) ||
                fullContent.contains("no seu cartão", ignoreCase = true) ||
                fullContent.contains("no cartão", ignoreCase = true) ||
                fullContent.contains("cartão de crédito", ignoreCase = true) ||
                fullContent.contains("cartao de credito", ignoreCase = true) ||
                fullContent.contains("fatura", ignoreCase = true)

        val isPix = isPixEnviado || isIncome || fullContent.contains("pix", ignoreCase = true)

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        // 3. Define a forma de pagamento e a conta/cartão exatos
        val defaultAccount = repository.getDefaultAccount() ?: repository.accounts.value.firstOrNull()
        val defaultAccountName = defaultAccount?.name ?: "Conta Principal"

        val paymentMethod: String
        val paymentType: PaymentType
        var autoCreatedNotice: String? = null

        if (detectedBank != null) {
            if (isDebito || isPix) {
                paymentType = if (isPix) PaymentType.PIX else PaymentType.DEBIT_CARD
                // Procura a conta do banco; se o usuário NÃO tiver a conta cadastrada, CRIA AUTOMATICAMENTE para que o dinheiro caia na conta correta e NÃO em outro banco!
                val (account, wasCreated) = repository.getOrCreateAccountForBank(detectedBank)
                paymentMethod = account.name
                if (wasCreated) {
                    autoCreatedNotice = "Conta '${account.name}' cadastrada automaticamente!"
                    saveDebugLog(context, "✨ Conta '${account.name}' criada automaticamente para o Pix/Débito.")
                }
            } else {
                paymentType = PaymentType.CREDIT_CARD
                // Procura o cartão do banco; se não existir, cria o cartão vinculado à conta
                val existingAcc = repository.findMatchingAccount(detectedBank.name, fallbackToDefault = false)
                val (card, wasCardCreated) = repository.getOrCreateCardForBank(detectedBank, existingAcc?.id ?: "")
                paymentMethod = card.name
                if (wasCardCreated) {
                    autoCreatedNotice = "Cartão '${card.name}' cadastrado automaticamente!"
                    saveDebugLog(context, "✨ Cartão '${card.name}' criado automaticamente para o crédito.")
                }
            }
        } else {
            if (isDebito) {
                paymentMethod = defaultAccountName
                paymentType = PaymentType.DEBIT_CARD
            } else if (isPix) {
                paymentMethod = defaultAccountName
                paymentType = PaymentType.PIX
            } else {
                paymentMethod = if (isCartaoCredito || !isIncome) "Cartão de Crédito" else defaultAccountName
                paymentType = if (isCartaoCredito || !isIncome) PaymentType.CREDIT_CARD else PaymentType.PIX
            }
        }

        // 4. Identifica o estabelecimento ou beneficiário
        val storeName = extractStoreOrBeneficiary(fullContent)
        val isVacationOr13th = isIncome && (
            fullContent.contains("férias", ignoreCase = true) ||
            fullContent.contains("ferias", ignoreCase = true) ||
            fullContent.contains("13º", ignoreCase = true) ||
            fullContent.contains("13o", ignoreCase = true) ||
            fullContent.contains("décimo terceiro", ignoreCase = true) ||
            fullContent.contains("decimo terceiro", ignoreCase = true)
        )
        val isSalary = isIncome && !isVacationOr13th && (
            fullContent.contains("salário", ignoreCase = true) ||
            fullContent.contains("salario", ignoreCase = true) ||
            fullContent.contains("folha de pagamento", ignoreCase = true) ||
            fullContent.contains("pro labore", ignoreCase = true) ||
            fullContent.contains("remuneração", ignoreCase = true) ||
            fullContent.contains("remuneracao", ignoreCase = true)
        )
        val category = if (isVacationOr13th) "Benefícios / Férias / 13º"
                       else if (isSalary) "Salário"
                       else if (isIncome) "Salário / Receita"
                       else inferCategory(storeName, paymentMethod)

        val currentMonth = repository.selectedMonth.value.ifEmpty { "OUTUBRO 2026" }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val today = dateFormat.format(Date())

        val targetMonth = if (paymentType == PaymentType.CREDIT_CARD) {
            val card = repository.cards.value.find { it.name.equals(paymentMethod, ignoreCase = true) }
            if (card != null && card.closingDay > 0) {
                MonthHelper.getTargetInvoiceMonth(today, card.closingDay) ?: currentMonth
            } else currentMonth
        } else {
            currentMonth
        }

        val description = if (storeName.isNotBlank()) {
            if (isIncome) {
                if (isVacationOr13th) "Férias/13º: $storeName"
                else if (isSalary) "Salário: $storeName"
                else if (storeName.contains("Depósito", ignoreCase = true) || storeName.contains("Pix", ignoreCase = true)) storeName
                else "Pix de $storeName"
            } else {
                storeName
            }
        } else {
            if (isIncome) {
                if (isVacationOr13th) "Férias / 13º Recebido"
                else if (isSalary) "Salário Recebido"
                else "Pix Recebido"
            } else (if (title.isNotBlank()) title else paymentMethod)
        }

        // 5. Leitor inteligente de parcelas (para compras parceladas)
        var totalInstallments = 1
        var currentInstallment = 1
        var isAmountPerInstallment = false
        var calculatedAmount = cleanAmount

        val xDePattern = Pattern.compile("(\\d{1,2})\\s*x\\s+de\\s+R?\\$?\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE)
        val xDeMatcher = xDePattern.matcher(fullContent)
        if (xDeMatcher.find()) {
            val inst = xDeMatcher.group(1)?.toIntOrNull() ?: 1
            if (inst in 2..48) {
                totalInstallments = inst
                val parcelStr = xDeMatcher.group(2) ?: "0"
                val pVal = parcelStr.replace(".", "").replace(",", ".").toDoubleOrNull() ?: 0.0
                if (pVal > 0) {
                    calculatedAmount = pVal
                    isAmountPerInstallment = true
                }
            }
        } else {
            val slashPattern = Pattern.compile("(\\d{1,2})\\s*(?:/|de)\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE)
            val slashMatcher = slashPattern.matcher(fullContent)
            if (slashMatcher.find()) {
                val g1 = slashMatcher.group(1)?.toIntOrNull() ?: 1
                val g2 = slashMatcher.group(2)?.toIntOrNull() ?: 1
                if (g2 in 2..48 && g1 in 1..g2) {
                    currentInstallment = g1
                    totalInstallments = g2
                }
            } else {
                val instPattern = Pattern.compile("(?:em|de|parc(?:ela)?(?:s)?\\s+em|dividido\\s+em)?\\s*(\\d{1,2})\\s*(?:x|vezes|parcelas)", Pattern.CASE_INSENSITIVE)
                val instMatcher = instPattern.matcher(fullContent)
                if (instMatcher.find()) {
                    val inst = instMatcher.group(1)?.toIntOrNull() ?: 1
                    if (inst in 2..48) {
                        totalInstallments = inst
                    }
                }
            }
        }

        val finalDesc = if (totalInstallments > 1) "$description ($currentInstallment/$totalInstallments)" else description

        val item = TransactionItem(
            description = finalDesc,
            amount = calculatedAmount,
            type = type,
            category = category,
            accountOrCardName = paymentMethod,
            paymentType = paymentType,
            monthYear = targetMonth,
            date = today,
            status = if (isIncome || isDebito || isPix) PaymentStatus.PAID else PaymentStatus.PENDING,
            currentInstallment = currentInstallment,
            totalInstallments = totalInstallments,
            isSalaryReplacement = isSalary
        )

        // Adiciona a transação no repositório central
        repository.addTransaction(item, includePreviousInstallments = false, isAmountPerInstallment = isAmountPerInstallment)

        // Define como a compra recente capturada para exibir o card de confirmação rápida na home do app
        repository.addRecentCapturedTransaction(item)

        saveDebugLog(context, "✅ Capturado: $finalDesc - R$ $calculatedAmount ($paymentMethod)")
        Log.d("BankNotificationService", "Lançamento capturado e salvo: $finalDesc | R$ $calculatedAmount no $paymentMethod (${totalInstallments}x)")

        // Dispara notificação com botões interativos de parcelamento no Android
        showInteractiveConfirmationNotification(context, item, calculatedAmount, finalDesc, paymentMethod, isIncome, autoCreatedNotice)
        return true
    }

    private fun extractAmount(content: String): Double {
        // 1. Padrão R$: Suporta "R$ 11", "R$ 11,00", "R$ 11.00", "R$11", "R$ 1.250,50", "R$ 1.250"
        val rDollarPattern = Pattern.compile(
            "R\\$\\s*([0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:,[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val m1 = rDollarPattern.matcher(content)
        if (m1.find()) {
            val raw = m1.group(1) ?: ""
            val normalized = raw.replace(".", "").replace(",", ".")
            val value = normalized.toDoubleOrNull() ?: 0.0
            if (value > 0.0) return value
        }

        // 2. Padrão com palavras-chave precedendo o número:
        // Ex: "depositou 11", "recebeu 11 via pix", "transferiu 11,00", "valor de 11", "pagou 11"
        val wordPattern = Pattern.compile(
            "(?:valor\\s+(?:de\\s+)?|pagou|pago|compra\\s+de|transferiu|recebeu|depositou|depósito\\s+de|deposito\\s+de)\\s*(?:R\\$\\s*)?([0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:,[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
        )
        val m2 = wordPattern.matcher(content)
        if (m2.find()) {
            val raw = m2.group(1) ?: ""
            val normalized = raw.replace(".", "").replace(",", ".")
            val value = normalized.toDoubleOrNull() ?: 0.0
            if (value > 0.0) return value
        }

        // 3. Padrão com 'reais': Ex: "11 reais", "50,00 reais"
        val reaisPattern = Pattern.compile(
            "([0-9]{1,3}(?:\\.[0-9]{3})*(?:,[0-9]{1,2})?|[0-9]+(?:,[0-9]{1,2})?)\\s+reais",
            Pattern.CASE_INSENSITIVE
        )
        val m3 = reaisPattern.matcher(content)
        if (m3.find()) {
            val raw = m3.group(1) ?: ""
            val normalized = raw.replace(".", "").replace(",", ".")
            val value = normalized.toDoubleOrNull() ?: 0.0
            if (value > 0.0) return value
        }

        return 0.0
    }

    private fun isValidStoreCandidate(candidate: String): Boolean {
        val clean = candidate.trim().lowercase()
        if (clean.length < 3 || clean.length > 45) return false

        val invalidNames = setOf(
            "janeiro", "fevereiro", "março", "marco", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
            "hoje", "ontem", "amanhã", "amanha", "cdi", "cdb", "rendimento",
            "conta", "cartão", "cartao", "débito", "debito", "crédito", "credito",
            "banco", "saldo", "fatura", "dinheiro", "pix", "transferência", "transferencia",
            "valor", "reais", "este mês", "este mes", "rendendo", "investimento",
            "parcela", "parcelas", "vezes"
        )
        if (clean in invalidNames) return false
        if (clean.startsWith("cartão") || clean.startsWith("cartao") || clean.startsWith("conta")) return false
        return true
    }

    private fun extractStoreOrBeneficiary(text: String): String {
        // 0. Depósito em conta (ex: Mercado Pago "Você depositou R$ 11 via Pix")
        if (text.contains("depositou", ignoreCase = true) ||
            text.contains("depósito", ignoreCase = true) ||
            text.contains("deposito", ignoreCase = true) ||
            text.contains("rendendo na sua conta", ignoreCase = true)) {
            return "Depósito via Pix"
        }

        // 1. "Henrique Jose Julio te enviou um Pix" (Inter, Mercado Pago, Nubank, Caixa)
        val enviouPixPattern = Pattern.compile("([A-Za-zÀ-ÖØ-öø-ÿ\\s]+?)\\s+te\\s+enviou\\s+um\\s+Pix", Pattern.CASE_INSENSITIVE)
        val mEnviou = enviouPixPattern.matcher(text)
        if (mEnviou.find()) {
            val cand = mEnviou.group(1)?.replace("Pix recebido", "", ignoreCase = true)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cand
        }

        // 2. "Você recebeu uma transferência de R$ 50,00 de Fulano", "Pix recebido de Fulano", "Você recebeu um Pix de Fulano"
        val recebidoDePattern = Pattern.compile("(?:transferência de|transferência recebida:?|pix recebido de|você recebeu um pix de|você recebeu de).*?de\\s+([A-Za-zÀ-ÖØ-öø-ÿ\\s\\.]+?)(?:\\s+no\\s+valor|\\.|\\$|$)", Pattern.CASE_INSENSITIVE)
        val mRecebido = recebidoDePattern.matcher(text)
        if (mRecebido.find()) {
            val cand = mRecebido.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cand
        }

        // 3. "Compra de R$ XX,XX aprovada em [Estabelecimento]" (Nubank / Inter)
        val compraAprovadaPattern = Pattern.compile("(?:compra\\s+de\\s+R?\\$?[0-9.,]+\\s+aprovada\\s+em|compra\\s+aprovada\\s+(?:no\\s+seu\\s+cartão.*?\\s+)?em)\\s+([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\.|\\$|\\sat|$)", Pattern.CASE_INSENSITIVE)
        val mCompra = compraAprovadaPattern.matcher(text)
        if (mCompra.find()) {
            val cand = mCompra.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cand
        }

        // 4. "Você gastou R$ XX em [Estabelecimento]" (Nubank)
        val gastouPattern = Pattern.compile("gastou\\s+R?\\$?[0-9.,]+\\s+em\\s+([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\s+com|\\.|\\$|$)", Pattern.CASE_INSENSITIVE)
        val mGastou = gastouPattern.matcher(text)
        if (mGastou.find()) {
            val cand = mGastou.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cand
        }

        // 5. "Você pagou a Fulano", "Você pagou R$ XX em [Estabelecimento]" (Mercado Pago)
        val pagouPattern = Pattern.compile("(?:você\\s+pagou\\s+a|você\\s+pagou\\s+(?:R?\\$?[0-9.,]+\\s+)?em)\\s+([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\.|\\$|$)", Pattern.CASE_INSENSITIVE)
        val mPagou = pagouPattern.matcher(text)
        if (mPagou.find()) {
            val cand = mPagou.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cand
        }

        // 6. "Você transferiu para Fulano", "Pix para Fulano", "Pix enviado para Fulano"
        val paraPattern = Pattern.compile("(?:para|de)\\s+([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\s+no\\s+valor|\\s+creditado|\\.|$)")
        val mPara = paraPattern.matcher(text)
        if (mPara.find()) {
            val cand = mPara.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cleanStoreName(cand)
        }

        // 7. Padrão de e-mail / notificação do Gmail do Mercado Pago e outros bancos:
        // "Seu Pix de R$ 6,94 foi enviado Sexta-feira, 23 de outubro de 2026, às 21:39:57. UBER DO BRASIL TECNOLOGIA LTDA. CPF/CNPJ: ..."
        val mpEmailPattern = Pattern.compile(
            "(?:às|as)\\s+\\d{1,2}:\\d{2}(?::\\d{2})?\\.\\s*([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\.\\s*CPF|\\s+CPF|CPF/CNPJ|\\.|\\$)",
            Pattern.CASE_INSENSITIVE
        )
        val mMpEmail = mpEmailPattern.matcher(text)
        if (mMpEmail.find()) {
            val cand = mMpEmail.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cleanStoreName(cand)
        }

        // 8. Padrão alternativo de e-mail Pix enviado: "... foi enviado. [Estabelecimento]. CPF/CNPJ"
        val pixEmailPattern = Pattern.compile(
            "(?:foi enviado|enviado)\\.?\\s+([A-Za-zÀ-ÖØ-öø-ÿ0-9\\s\\.\\*\\-]+?)(?:\\.\\s*CPF|\\s+CPF|CPF/CNPJ|\\.|\\$)",
            Pattern.CASE_INSENSITIVE
        )
        val mPixEmail = pixEmailPattern.matcher(text)
        if (mPixEmail.find()) {
            val cand = mPixEmail.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(cand)) return cleanStoreName(cand)
        }

        val storePattern = Pattern.compile("(?:em|no|na|para|de)\\s+([A-Za-z0-9\\s\\.\\*\\-]+?)(?:\\s+no\\s+seu|\\s+com|\\s+às|\\s+as|\\s+em|\\.|$)")
        val matcher = storePattern.matcher(text)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim() ?: ""
            if (isValidStoreCandidate(candidate)) return cleanStoreName(candidate)
        }
        return ""
    }

    private fun cleanStoreName(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.contains("UBER", ignoreCase = true)) {
            return "Uber"
        }
        if (trimmed.contains("99APP", ignoreCase = true) || trimmed.contains("99 TECNOLOGIA", ignoreCase = true)) {
            return "99"
        }
        if (trimmed.contains("IFOOD", ignoreCase = true)) {
            return "iFood"
        }
        // Remove termos empresariais que costumam poluir a descrição de comprovantes bancários
        val cleaned = trimmed
            .replace(Regex("(?i)\\b(TECNOLOGIA LTDA|INSTITUICAO DE PAGAMENTO|PAGAMENTOS|TECNOLOGIA|LTDA|S\\.A\\.|SA|ME|EPP)\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.ifEmpty { trimmed }
    }

    private fun isNonTransactionNotification(content: String): Boolean {
        val lower = content.lowercase()

        // 1. Promoções de CDI, Rendimento de Conta, Investimentos, CDB, etc.
        val promoKeywords = listOf(
            "cdi", "cdb", "lci", "lca", "selic", "tesouro direto",
            "% do cdi", "% cdi", "% do cdb", "% a.a.", "% ao ano",
            "continue com até", "continue com ate", "mantenha o rendimento",
            "melhor rendimento", "rendendo mais", "seu dinheiro rende", "rendimento da conta",
            "deposite mais", "deposite a partir", "deposite para", "faça um depósito para", "faca um deposito para",
            "se você depositar", "se voce depositar", "deposite até", "deposite ate",
            "comece a investir", "invista a partir", "invista em", "invista já", "invista ja",
            "aplique seu dinheiro", "aplique a partir", "aproveite o rendimento", "rentabilidade",
            "cofrinho", "guardar dinheiro", "guarde seu dinheiro"
        )
        if (promoKeywords.any { lower.contains(it) }) return true

        // 2. Empréstimos, Crédito e Limite pré-aprovado
        val creditPromoKeywords = listOf(
            "empréstimo pré-aprovado", "emprestimo pre-aprovado", "crédito pré-aprovado", "credito pre-aprovado",
            "empréstimo disponível", "emprestimo disponivel", "você tem empréstimo", "voce tem emprestimo",
            "simule seu empréstimo", "simule seu emprestimo", "contrate seu empréstimo", "contrate agora",
            "aumente seu limite", "novo limite disponível", "novo limite disponivel", "limite pré-aprovado",
            "limite pre-aprovado", "liberamos um limite", "limite aprovado para você", "limite aprovado pra você",
            "pedir empréstimo", "solicite seu empréstimo", "precisando de dinheiro", "antecipe seu fgts", "saque aniversário"
        )
        if (creditPromoKeywords.any { lower.contains(it) }) return true

        // 3. Fatura fechada / Lembrete de vencimento (Fatura NÃO é uma compra individual)
        val invoiceKeywords = listOf(
            "sua fatura fechou", "fatura fechada", "fatura disponível", "fatura disponivel",
            "lembrete de fatura", "sua fatura vence", "pague sua fatura", "vencimento da fatura",
            "fatura do cartão fechou", "fatura do cartao fechou", "fechamento da sua fatura"
        )
        if (invoiceKeywords.any { lower.contains(it) }) return true

        // 4. DDA e Boletos a vencer (aviso de cobrança futura)
        val ddaKeywords = listOf(
            "boleto disponível no dda", "boleto disponivel no dda", "novo boleto no dda",
            "boleto dda", "dda disponível", "dda disponivel", "novo dda", "boleto a vencer",
            "novo boleto cadastrado", "boleto emitido"
        )
        if (ddaKeywords.any { lower.contains(it) }) return true

        // 5. Cupons, Cashback, Ofertas de Shopping / Parcerias
        val shoppingKeywords = listOf(
            "ganhe até", "ganhe ate", "ganhe cashback", "cashback de até", "cashback de ate",
            "use o cupom", "cupom de desconto", "desconto de até", "desconto de ate",
            "desconto exclusivo", "ofertas imperdíveis", "ofertas imperdiveis", "compre com desconto",
            "aproveite as ofertas", "compre e ganhe", "desconto na loja"
        )
        if (shoppingKeywords.any { lower.contains(it) }) return true

        // 6. Segurança, Senhas, Tokens e Verificação
        val securityKeywords = listOf(
            "código de segurança", "codigo de seguranca", "código de verificação", "codigo de verificacao",
            "código de confirmação", "codigo de confirmacao", "código para autorizar", "codigo para autorizar",
            "seu token", "código itoken", "codigo itoken", "não compartilhe", "nao compartilhe",
            "novo acesso", "dispositivo não reconhecido", "dispositivo nao reconhecido", "troca de senha",
            "reconhecimento facial"
        )
        if (securityKeywords.any { lower.contains(it) }) return true

        // 7. Compras negadas, recusadas, canceladas, em revisão/análise ou limite/saldo insuficiente
        val deniedOrReviewKeywords = listOf(
            "negada", "negado", "recusada", "recusado", "recusamos", "não autorizada", "nao autorizada",
            "não aprovada", "nao aprovada", "não foi aprovada", "nao foi aprovada", "não autorizamos", "nao autorizamos",
            "compra não autorizada", "compra nao autorizada", "transação não autorizada", "transacao nao autorizada",
            "compra negada", "crédito negado", "credito negado", "débito negado", "debito negado",
            "compra no crédito negada", "compra no credito negada", "compra no débito negada", "compra no debito negada",
            "compra cancelada", "pagamento cancelado", "transação cancelada", "transacao cancelada",
            "compra foi cancelada", "compra estornada", "estorno de compra", "compra estorno",
            "compra revisada", "transação revisada", "transacao revisada", "revisada", "revisado",
            "em revisão", "em revisao", "em análise", "em analise", "sob análise", "sob analise",
            "limite não é suficiente", "limite nao e suficiente", "limite insuficiente", "limite não suficiente", "limite nao suficiente",
            "pois o limite disponível não é suficiente", "pois o limite disponivel nao e suficiente",
            "saldo insuficiente", "saldo não é suficiente", "saldo nao e suficiente",
            "cartão bloqueado", "cartao bloqueado", "cartão está bloqueado", "cartao esta bloqueado",
            "bloqueio temporário", "bloqueio temporario", "bloqueamos seu cartão", "bloqueamos seu cartao",
            "suspeita de fraude", "compra suspeita", "transação suspeita", "transacao suspeita",
            "você reconhece", "voce reconhece", "não reconhece", "nao reconhece", "foi você?", "foi voce?"
        )
        if (deniedOrReviewKeywords.any { lower.contains(it) }) return true

        return false
    }

    private fun isValidTransactionConfirmation(content: String): Boolean {
        val lower = content.lowercase()

        // Barreira primária: Compras negadas, recusadas, canceladas ou em revisão JAMAIS são confirmadas
        val deniedOrCancelKeywords = listOf(
            "negada", "negado", "recusada", "recusado", "recusamos",
            "não autorizada", "nao autorizada", "não aprovada", "nao aprovada",
            "não foi aprovada", "nao foi aprovada", "cancelada", "cancelado",
            "compra cancelada", "compra estornada", "revisada", "revisado", "em revisão", "em revisao",
            "em análise", "em analise", "limite insuficiente", "limite disponível não é suficiente",
            "limite disponivel nao e suficiente", "saldo insuficiente", "cartão bloqueado", "cartao bloqueado"
        )
        if (deniedOrCancelKeywords.any { lower.contains(it) }) return false

        // 1. Compras no cartão ou pagamentos efetuados
        val isPurchaseOrPayment = lower.contains("compra aprovada") ||
                lower.contains("compra realizada") ||
                lower.contains("compra confirmada") ||
                lower.contains("compra no cartão") ||
                lower.contains("compra no cartao") ||
                lower.contains("compra no débito") ||
                lower.contains("compra no debito") ||
                lower.contains("compra no valor de") ||
                lower.contains("compra de r$") ||
                lower.contains("você comprou") ||
                lower.contains("voce comprou") ||
                lower.contains("pagamento aprovado") ||
                lower.contains("pagamento realizado") ||
                lower.contains("pagamento confirmado") ||
                lower.contains("pagamento de r$") ||
                lower.contains("você pagou") ||
                lower.contains("voce pagou") ||
                lower.contains("transação aprovada") ||
                lower.contains("transacao aprovada") ||
                lower.contains("autorização de compra") ||
                lower.contains("autorizacao de compra") ||
                lower.contains("usou seu cartão") ||
                lower.contains("usou seu cartao") ||
                lower.contains("gasto aprovado") ||
                (lower.contains("gastou") && (lower.contains("r$") || lower.contains("reais")))

        if (isPurchaseOrPayment) return true

        // 2. Pix enviado ou transferência enviada (bancos, Gmail, e-mails de comprovante)
        val isTransferSent = lower.contains("pix enviado") ||
                lower.contains("pix foi enviado") ||
                lower.contains("seu pix foi enviado") ||
                lower.contains("seu pix de r$") ||
                lower.contains("seu pix de") ||
                lower.contains("pix de r$") ||
                lower.contains("comprovante de pix") ||
                lower.contains("comprovante pix") ||
                lower.contains("transferência concluída") ||
                lower.contains("transferencia concluida") ||
                lower.contains("enviou um pix") ||
                lower.contains("você transferiu") ||
                lower.contains("voce transferiu") ||
                lower.contains("transferência enviada") ||
                lower.contains("transferencia enviada") ||
                lower.contains("transferência realizada") ||
                lower.contains("transferencia realizada") ||
                lower.contains("pagou com pix") ||
                lower.contains("pix realizado") ||
                lower.contains("pix feito") ||
                lower.contains("pagamento via pix") ||
                lower.contains("pix no valor de") ||
                lower.contains("débito realizado") ||
                lower.contains("debito realizado") ||
                lower.contains("débito de r$") ||
                lower.contains("debito de r$")

        if (isTransferSent) return true

        // 3. Receitas: Pix recebido, transferência recebida ou depósito creditado
        val isIncomeOrDeposit = lower.contains("pix recebido") ||
                lower.contains("recebeu um pix") ||
                lower.contains("recebeu via pix") ||
                lower.contains("te enviou um pix") ||
                lower.contains("você recebeu um pix") ||
                lower.contains("voce recebeu um pix") ||
                lower.contains("transferência recebida") ||
                lower.contains("transferencia recebida") ||
                lower.contains("você recebeu uma transferência") ||
                lower.contains("voce recebeu uma transferencia") ||
                lower.contains("você recebeu r$") ||
                lower.contains("voce recebeu r$") ||
                lower.contains("depositou r$") ||
                lower.contains("você depositou") ||
                lower.contains("voce depositou") ||
                lower.contains("depósito recebido") ||
                lower.contains("deposito recebido") ||
                lower.contains("depósito no valor de") ||
                lower.contains("deposito no valor de") ||
                lower.contains("depósito de r$") ||
                lower.contains("deposito de r$") ||
                lower.contains("creditado em sua conta") ||
                lower.contains("creditado na sua conta") ||
                lower.contains("crédito recebido") ||
                lower.contains("credito recebido") ||
                lower.contains("reembolso recebido") ||
                lower.contains("reembolso de r$") ||
                lower.contains("estorno realizado") ||
                lower.contains("estorno de r$")

        if (isIncomeOrDeposit) return true

        return false
    }

    private fun inferCategory(store: String, paymentMethod: String): String {
        val upper = store.uppercase()
        return when {
            upper.contains("POSTO") || upper.contains("SHELL") || upper.contains("IPIRANGA") || upper.contains("UBER") || upper.contains("99") -> "Transporte"
            upper.contains("MERCADO") || upper.contains("SUPER") || upper.contains("PADARIA") || upper.contains("IFOOD") || upper.contains("RESTAURANTE") || upper.contains("BURGER") -> "Alimentação"
            upper.contains("FARMACIA") || upper.contains("DROGA") || upper.contains("SAUDE") || upper.contains("MEDIC") -> "Saúde & Esporte"
            upper.contains("BARBEARIA") || upper.contains("CABEL") || upper.contains("SALÃO") -> "Cuidados Pessoais"
            upper.contains("LUZ") || upper.contains("ENERGIA") || upper.contains("AGUA") || upper.contains("CONDOMINIO") -> "Moradia"
            upper.contains("NETFLIX") || upper.contains("SPOTIFY") || upper.contains("AMAZON") || upper.contains("YOUTUBE") -> "Serviços & Assinaturas"
            paymentMethod.contains("Cartão", ignoreCase = true) -> "Cartão de Crédito"
            else -> "Outros"
        }
    }

    private fun showInteractiveConfirmationNotification(
        context: Context,
        item: TransactionItem,
        amount: Double,
        description: String,
        method: String,
        isIncome: Boolean,
        extraNotice: String? = null
    ) {
        try {
            val notifId = (System.currentTimeMillis() % 100000).toInt()
            val formattedAmount = String.format(Locale.getDefault(), "R$ %.2f", amount)
            val title = if (isIncome) "💰 Receita Registrada!" else "💳 Compra Registrada!"
            val noticeSuffix = if (!extraNotice.isNullOrBlank()) " ($extraNotice)" else ""

            val body = if (item.paymentType == PaymentType.CREDIT_CARD && item.totalInstallments == 1) {
                "$formattedAmount em '$description' ($method).$noticeSuffix Foi parcelado? Escolha abaixo:"
            } else {
                "$formattedAmount em '$description' ($method) adicionado com sucesso.$noticeSuffix"
            }

            // Ação ao tocar no corpo da notificação: Abre o app na tela inicial
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openAppIntent,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(openAppPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // Se for compra no cartão e ainda não foi parcelada automaticamente, adiciona botões rápidos na barra de notificações
            if (item.paymentType == PaymentType.CREDIT_CARD && item.totalInstallments == 1) {
                val quickOptions = listOf(2, 3, 5, 10, 12)
                for (inst in quickOptions) {
                    val pIntent = createInstallmentPendingIntent(context, item.id, inst, amount, description, notifId)
                    builder.addAction(0, "${inst}x", pIntent)
                }
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(notifId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createInstallmentPendingIntent(
        context: Context,
        transactionId: String,
        installments: Int,
        amount: Double,
        desc: String,
        notifId: Int
    ): PendingIntent {
        val intent = Intent(context, InstallmentActionReceiver::class.java).apply {
            action = "com.example.mobillsfinancas.ACTION_SET_INSTALLMENTS"
            putExtra("EXTRA_TRANSACTION_ID", transactionId)
            putExtra("EXTRA_INSTALLMENTS", installments)
            putExtra("EXTRA_BASE_AMOUNT", amount)
            putExtra("EXTRA_DESC", desc)
            putExtra("EXTRA_NOTIFICATION_ID", notifId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(context, installments * 1000 + (notifId % 1000), intent, flags)
    }
}
