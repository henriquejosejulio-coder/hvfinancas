package com.example.mobillsfinancas.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.Calendar
import com.example.mobillsfinancas.utils.MonthHelper
import com.example.mobillsfinancas.utils.BankRegistry
import com.example.mobillsfinancas.utils.SupportedBank
import com.example.mobillsfinancas.utils.Formatters

class FinanceRepository(private val context: Context) {

    companion object {
        @Volatile
        private var instance: FinanceRepository? = null

        fun getInstance(context: Context): FinanceRepository {
            return instance ?: synchronized(this) {
                instance ?: FinanceRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dataFile = File(context.filesDir, "hvfinancas_v6.json")

    private val _months = MutableStateFlow<List<String>>(emptyList())
    val months: StateFlow<List<String>> = _months.asStateFlow()

    private val _selectedMonth = MutableStateFlow("")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _monthDataMap = mutableMapOf<String, MonthData>()

    private val _currentMonthData = MutableStateFlow<MonthData?>(null)
    val currentMonthData: StateFlow<MonthData?> = _currentMonthData.asStateFlow()

    private val _cards = MutableStateFlow<List<CreditCardItem>>(emptyList())
    val cards: StateFlow<List<CreditCardItem>> = _cards.asStateFlow()

    private val _accounts = MutableStateFlow<List<AccountItem>>(emptyList())
    val accounts: StateFlow<List<AccountItem>> = _accounts.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()

    private val _tags = MutableStateFlow<List<TagItem>>(emptyList())
    val tags: StateFlow<List<TagItem>> = _tags.asStateFlow()

    private val _recentCapturedTransactions = MutableStateFlow<List<TransactionItem>>(emptyList())
    val recentCapturedTransactions: StateFlow<List<TransactionItem>> = _recentCapturedTransactions.asStateFlow()

    val recentCapturedTransaction: StateFlow<TransactionItem?> get() = MutableStateFlow(_recentCapturedTransactions.value.firstOrNull())

    fun addRecentCapturedTransaction(item: TransactionItem) {
        val list = _recentCapturedTransactions.value.toMutableList()
        list.removeAll { it.id == item.id }
        list.add(0, item) // mais recente primeiro
        _recentCapturedTransactions.value = list
    }

    fun setRecentCapturedTransaction(item: TransactionItem?) {
        if (item == null) {
            _recentCapturedTransactions.value = emptyList()
        } else {
            addRecentCapturedTransaction(item)
        }
    }

    fun dismissRecentCapture(transactionId: String? = null) {
        if (transactionId == null) {
            _recentCapturedTransactions.value = emptyList()
        } else {
            _recentCapturedTransactions.value = _recentCapturedTransactions.value.filter { it.id != transactionId }
        }
    }

    init {
        loadData()
    }

    private fun loadData() {
        initDefaultCategoriesAndTags()
        initDefaultCardsAndAccounts()

        if (dataFile.exists()) {
            try {
                val jsonString = dataFile.readText()
                parseFullSavedJson(jsonString)
                cleanupBogusCapturedTransactions()
                reconcileCardAndAccountNames()
                checkAndApplyAutoYield()
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Se for primeira vez, carrega o JSON parseado da planilha
        try {
            val assetJson = context.assets.open("dados_iniciais.json").bufferedReader().use { it.readText() }
            parseExcelExtractedJson(assetJson)
            applyOctoberBackwardsPaidStatus()
            reconcileCardAndAccountNames()
            saveToDisk()
        } catch (e: Exception) {
            e.printStackTrace()
            createFallbackData()
        }
    }

    private fun initDefaultCategoriesAndTags() {
        _categories.value = listOf(
            CategoryItem(id = "cat_merc", name = "Mercado", iconEmoji = "🛒", colorHex = 0xFFFF9800, budgetLimit = 700.0, reserveInBudget = true),
            CategoryItem(id = "cat_alim", name = "Alimentação", iconEmoji = "🍔", colorHex = 0xFFFF9800, budgetLimit = 0.0, reserveInBudget = false),
            CategoryItem(id = "cat_mora", name = "Moradia", iconEmoji = "🏠", colorHex = 0xFF3F51B5),
            CategoryItem(id = "cat_tran", name = "Transporte", iconEmoji = "🚗", colorHex = 0xFF00BCD4, budgetLimit = 0.0, reserveInBudget = false),
            CategoryItem(id = "cat_card", name = "Cartão de Crédito", iconEmoji = "💳", colorHex = 0xFFE91E63),
            CategoryItem(id = "cat_saud", name = "Saúde & Esporte", iconEmoji = "🏋️", colorHex = 0xFF4CAF50),
            CategoryItem(id = "cat_cuid", name = "Cuidados Pessoais", iconEmoji = "💈", colorHex = 0xFF9C27B0),
            CategoryItem(id = "cat_serv", name = "Serviços & Assinaturas", iconEmoji = "📱", colorHex = 0xFF009688),
            CategoryItem(id = "cat_empr", name = "Empréstimos & Dívidas", iconEmoji = "🏦", colorHex = 0xFFF44336),
            CategoryItem(id = "cat_inve", name = "Investimentos & Metas", iconEmoji = "📈", colorHex = 0xFF00A868),
            CategoryItem(id = "cat_laze", name = "Lazer", iconEmoji = "🍿", colorHex = 0xFFFF5722),
            CategoryItem(id = "cat_rece", name = "Salário / Receita", iconEmoji = "💰", colorHex = 0xFF2E7D32, isExpense = false),
            CategoryItem(id = "cat_outr", name = "Outros", iconEmoji = "🏷️", colorHex = 0xFF757575)
        )

        _tags.value = listOf(
            TagItem(id = "tag_pess", name = "pessoal", colorHex = 0xFF00A868),
            TagItem(id = "tag_trab", name = "trabalho", colorHex = 0xFF0288D1),
            TagItem(id = "tag_casa", name = "casa", colorHex = 0xFF3F51B5),
            TagItem(id = "tag_carr", name = "carro", colorHex = 0xFFFF9800),
            TagItem(id = "tag_alex", name = "alex", colorHex = 0xFF9C27B0),
            TagItem(id = "tag_viag", name = "viagem", colorHex = 0xFFE91E63)
        )
    }

    private fun initDefaultCardsAndAccounts() {
        _cards.value = listOf(
            CreditCardItem("card_inter", "Cartão Inter", 3000.0, 6, 15, 0xFFFF7A00, "Banco Inter", "Mastercard", linkedAccountId = "acc_inter"),
            CreditCardItem("card_will", "Cartão Will", 1500.0, 5, 12, 0xFFFFD500, "Will Bank", "Mastercard", linkedAccountId = "acc_will"),
            CreditCardItem("card_sofisa", "Cartão Sofisa", 2500.0, 8, 15, 0xFF0047BA, "Sofisa Direto", "Mastercard", linkedAccountId = "acc_sofisa"),
            CreditCardItem("card_mp", "Cartão Mercado Pago", 1200.0, 9, 15, 0xFF009EE3, "Mercado Pago", "Visa", linkedAccountId = "acc_mp"),
            CreditCardItem("card_nu", "Cartão Nubank", 2000.0, 10, 17, 0xFF8A05BE, "Nubank", "Mastercard", linkedAccountId = "acc_nubank"),
            CreditCardItem("card_caixa", "Cartão Caixa", 2000.0, 8, 15, 0xFF005CA9, "Caixa Econômica", "Elo", linkedAccountId = "acc_caixa")
        )

        _accounts.value = listOf(
            AccountItem("acc_inter", "Conta Inter (Principal)", AccountType.CHECKING, 0.0, 0xFFFF7A00, "🏦", isDefault = true),
            AccountItem("acc_mp", "Mercado Pago", AccountType.WALLET, 0.0, 0xFF009EE3, "📱"),
            AccountItem("acc_will", "Conta Will Bank", AccountType.CHECKING, 0.0, 0xFFFFD500, "💳"),
            AccountItem("acc_sofisa", "Sofisa Direto", AccountType.INVESTMENT, 0.0, 0xFF0047BA, "📈"),
            AccountItem("acc_nubank", "Nubank", AccountType.CHECKING, 0.0, 0xFF8A05BE, "💜"),
            AccountItem("acc_caixa", "Caixa Econômica", AccountType.CHECKING, 0.0, 0xFF005CA9, "🏦"),
            AccountItem("acc_cofrinho", "Cofrinho", AccountType.SAVINGS, 0.0, 0xFF00A868, "🐷", includeInOverallBalance = false),
            AccountItem("acc_emergencia", "Fundo de Emergência", AccountType.INVESTMENT, 0.0, 0xFF0288D1, "🛡️", includeInOverallBalance = false)
        )
    }

    private fun parseExcelExtractedJson(jsonString: String) {
        val rootArray = JSONArray(jsonString)
        val monthList = mutableListOf<String>()

        for (i in 0 until rootArray.length()) {
            val monthObj = rootArray.getJSONObject(i)
            val monthYear = monthObj.getString("mes_ano").trim()
            monthList.add(monthYear)

            val itemsArray = monthObj.optJSONArray("itens") ?: JSONArray()
            val items = mutableListOf<TransactionItem>()
            var totalExp = monthObj.optDouble("total_despesas", 0.0)
            var renda = monthObj.optDouble("renda", 0.0)
            var sobrou = monthObj.optDouble("sobrou", 0.0)

            var calcExp = 0.0

            for (j in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(j)
                val desc = itemObj.optString("descricao", "").trim()
                val upperDesc = desc.uppercase()
                if ((upperDesc.contains("FUNDO") && upperDesc.contains("EMERG")) ||
                    (upperDesc.contains("GASTO") && upperDesc.contains("ADICIONA")) ||
                    (upperDesc == "MERCADO" && itemObj.optDouble("valor", 0.0) == 700.0)
                ) {
                    continue
                }
                val valor = itemObj.optDouble("valor", 0.0)
                val statusStr = itemObj.optString("status", "PENDENTE").uppercase()
                val catName = itemObj.optString("categoria", "Outros")
                val payMethod = itemObj.optString("forma_pagamento", "Conta Inter")
                val venc = itemObj.optString("vencimento", "")
                val isIncome = itemObj.optBoolean("is_income", false)

                val matchedCat = _categories.value.find { it.name.equals(catName, ignoreCase = true) }
                    ?: _categories.value.find { it.name == "Outros" }!!

                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
                val paymentType = if (payMethod.contains("Cartão", ignoreCase = true)) PaymentType.CREDIT_CARD else PaymentType.DEBIT_CARD

                val status = when {
                    statusStr.contains("PAGO") -> PaymentStatus.PAID
                    statusStr.contains("RECEBER") -> PaymentStatus.TO_RECEIVE
                    else -> PaymentStatus.PENDING
                }

                if (type == TransactionType.EXPENSE) {
                    calcExp += valor
                }

                // Extrai tag simples se aplicável
                val itemTags = mutableListOf<String>()
                if (desc.contains("ALEX", ignoreCase = true)) itemTags.add("alex")
                if (catName.contains("Moradia", ignoreCase = true)) itemTags.add("casa")
                if (catName.contains("Transporte", ignoreCase = true)) itemTags.add("carro")

                val currentInst = itemObj.optInt("current_installment", itemObj.optInt("currentInstallment", 1))
                val totalInst = itemObj.optInt("total_installments", itemObj.optInt("totalInstallments", 1))

                items.add(
                    TransactionItem(
                        id = UUID.randomUUID().toString(),
                        description = desc,
                        amount = valor,
                        type = type,
                        category = matchedCat.name,
                        categoryIcon = matchedCat.iconEmoji,
                        categoryColorHex = matchedCat.colorHex,
                        accountOrCardName = payMethod,
                        paymentType = paymentType,
                        tags = itemTags,
                        monthYear = monthYear,
                        date = if (venc.isNotEmpty()) "Vencimento: $venc" else "Mês de $monthYear",
                        status = status,
                        currentInstallment = currentInst,
                        totalInstallments = totalInst
                    )
                )
            }

            if (totalExp <= 0.0) totalExp = calcExp
            if (sobrou == 0.0) sobrou = renda - totalExp

            _monthDataMap[monthYear] = MonthData(
                monthYear = monthYear,
                items = items,
                totalExpense = totalExp,
                totalIncome = renda,
                remainingBalance = sobrou,
                baseIncome = renda
            )
        }

        _months.value = monthList
        recalculateAllMonths()

        val defaultMonth = monthList.find { it.contains("OUTUBRO 2026", ignoreCase = true) }
            ?: monthList.find { it.contains("OUTUBRO 2025", ignoreCase = true) }
            ?: monthList.firstOrNull() ?: ""

        selectMonth(defaultMonth)
    }

    private fun cleanupBogusCapturedTransactions() {
        var modified = false
        for ((mKey, mData) in _monthDataMap) {
            val invalidItems = mData.items.filter { item ->
                (item.description.equals("novembro", ignoreCase = true) && item.amount in 447.0..450.0) ||
                item.description.contains("negada", ignoreCase = true) ||
                item.description.contains("recusada", ignoreCase = true) ||
                item.description.contains("revisada", ignoreCase = true) ||
                item.description.contains("cancelada", ignoreCase = true)
            }
            if (invalidItems.isNotEmpty()) {
                val newItems = mData.items.filterNot { it in invalidItems }
                _monthDataMap[mKey] = mData.copy(items = newItems, lastModified = System.currentTimeMillis())
                modified = true
            }
        }
        if (modified) {
            recalculateAllMonths()
            saveToDisk()
        }
        _recentCapturedTransactions.value = _recentCapturedTransactions.value.filterNot {
            (it.description.equals("novembro", ignoreCase = true) && it.amount in 447.0..450.0) ||
            it.description.contains("negada", ignoreCase = true) ||
            it.description.contains("recusada", ignoreCase = true) ||
            it.description.contains("revisada", ignoreCase = true) ||
            it.description.contains("cancelada", ignoreCase = true)
        }
    }

    private fun createFallbackData() {
        val month = "OUTUBRO 2026"
        _months.value = listOf(month)
        _monthDataMap[month] = MonthData(
            monthYear = month,
            items = emptyList(),
            totalExpense = 0.0,
            totalIncome = 0.0,
            remainingBalance = 0.0,
            baseIncome = 0.0,
            lastModified = System.currentTimeMillis()
        )
        selectMonth(month)
    }

    fun selectMonth(month: String) {
        if (month.isEmpty()) return
        val exactKey = _monthDataMap.keys.find { it.equals(month.trim(), ignoreCase = true) } ?: month
        if (!_monthDataMap.containsKey(exactKey)) {
            ensureMonthExists(exactKey)
        }
        _selectedMonth.value = exactKey
        val cur = _monthDataMap[exactKey]!!
        _currentMonthData.value = cur.copy(items = cur.items.toList(), lastModified = System.currentTimeMillis())
    }

    // --- Categorias CRUD ---
    fun addCategory(category: CategoryItem) {
        _categories.value = _categories.value + category
        saveToDisk()
    }

    fun updateCategory(updated: CategoryItem) {
        val oldCat = _categories.value.find { it.id == updated.id }
        _categories.value = _categories.value.map { if (it.id == updated.id) updated else it }
        if (oldCat != null && (oldCat.name != updated.name || oldCat.iconEmoji != updated.iconEmoji || oldCat.colorHex != updated.colorHex)) {
            for ((mYear, mData) in _monthDataMap) {
                var changed = false
                val updatedItems = mData.items.map { item ->
                    if (item.category.equals(oldCat.name, ignoreCase = true)) {
                        changed = true
                        item.copy(
                            category = updated.name,
                            categoryIcon = updated.iconEmoji,
                            categoryColorHex = updated.colorHex
                        )
                    } else {
                        item
                    }
                }
                if (changed) {
                    _monthDataMap[mYear] = mData.copy(items = updatedItems, lastModified = System.currentTimeMillis())
                }
            }
        }
        recalculateAllMonths()
        saveToDisk()
    }

    fun updateCategoryBudget(categoryId: String, budget: Double, reserveInBudget: Boolean = true) {
        _categories.value = _categories.value.map {
            if (it.id == categoryId) it.copy(budgetLimit = budget, reserveInBudget = reserveInBudget) else it
        }
        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteCategory(id: String) {
        _categories.value = _categories.value.filter { it.id != id }
        saveToDisk()
    }

    // --- Tags CRUD ---
    fun addTag(tag: TagItem) {
        _tags.value = _tags.value + tag
        saveToDisk()
    }

    fun updateTag(updated: TagItem) {
        val oldTag = _tags.value.find { it.id == updated.id }
        _tags.value = _tags.value.map { if (it.id == updated.id) updated else it }
        if (oldTag != null && oldTag.name != updated.name) {
            for ((mYear, mData) in _monthDataMap) {
                var changed = false
                val updatedItems = mData.items.map { item ->
                    if (item.tags.any { it.equals(oldTag.name, ignoreCase = true) }) {
                        changed = true
                        item.copy(tags = item.tags.map { if (it.equals(oldTag.name, ignoreCase = true)) updated.name else it })
                    } else {
                        item
                    }
                }
                if (changed) {
                    _monthDataMap[mYear] = mData.copy(items = updatedItems, lastModified = System.currentTimeMillis())
                }
            }
        }
        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteTag(id: String) {
        _tags.value = _tags.value.filter { it.id != id }
        saveToDisk()
    }

    // --- Contas CRUD ---
    fun getDefaultAccount(): AccountItem? {
        return _accounts.value.find { it.isDefault } ?: _accounts.value.firstOrNull()
    }

    fun setDefaultAccount(accountId: String) {
        _accounts.value = _accounts.value.map {
            it.copy(isDefault = (it.id == accountId))
        }
        saveToDisk()
    }

    fun addAccount(account: AccountItem) {
        val willBeDefault = account.isDefault || _accounts.value.isEmpty()
        val toAdd = account.copy(isDefault = willBeDefault)
        val current = if (willBeDefault) {
            _accounts.value.map { it.copy(isDefault = false) }
        } else {
            _accounts.value
        }
        _accounts.value = current + toAdd
        saveToDisk()
    }

    fun updateAccount(updated: AccountItem) {
        val oldAcc = _accounts.value.find { it.id == updated.id }
        val oldName = oldAcc?.name?.trim() ?: ""
        val newName = updated.name.trim()

        _accounts.value = _accounts.value.map {
            if (it.id == updated.id) {
                updated
            } else {
                if (updated.isDefault) it.copy(isDefault = false) else it
            }
        }

        // Se o nome da conta mudou, sincroniza todos os lançamentos em todos os meses
        if (oldName.isNotBlank() && !oldName.equals(newName, ignoreCase = true)) {
            for ((mKey, mData) in _monthDataMap) {
                var modified = false
                val updatedItems = mData.items.map { item ->
                    if (item.accountOrCardName.trim().equals(oldName, ignoreCase = true)) {
                        modified = true
                        item.copy(accountOrCardName = newName)
                    } else {
                        item
                    }
                }
                if (modified) {
                    _monthDataMap[mKey] = mData.copy(
                        items = updatedItems,
                        lastModified = System.currentTimeMillis()
                    )
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteAccount(accountId: String) {
        val filtered = _accounts.value.filter { it.id != accountId }
        val updated = if (filtered.none { it.isDefault } && filtered.isNotEmpty()) {
            filtered.mapIndexed { idx, it -> if (idx == 0) it.copy(isDefault = true) else it }
        } else {
            filtered
        }
        _accounts.value = updated
        saveToDisk()
    }

    private val prefs by lazy {
        context.getSharedPreferences("hvfinancas_prefs", Context.MODE_PRIVATE)
    }

    fun isAutoApplyYieldEnabled(): Boolean {
        return prefs.getBoolean("auto_apply_yield_enabled", true)
    }

    fun setAutoApplyYield(enabled: Boolean) {
        prefs.edit().putBoolean("auto_apply_yield_enabled", enabled).apply()
    }

    fun getLastYieldDate(): String {
        return prefs.getString("last_yield_date", "") ?: ""
    }

    fun applyDailyYields(yields: Map<String, Double>) {
        if (yields.isEmpty()) return
        val currentAccs = _accounts.value
        _accounts.value = currentAccs.map { acc ->
            val yield = yields[acc.id] ?: 0.0
            if (yield > 0.0) {
                val newBal = Math.round((acc.balance + yield) * 100.0) / 100.0
                acc.copy(balance = newBal)
            } else {
                acc
            }
        }
        val cal = Calendar.getInstance()
        val todayStr = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        prefs.edit().putString("last_yield_date", todayStr).apply()
        saveToDisk()
    }

    fun checkAndApplyAutoYield(): Map<String, Double> {
        if (!isAutoApplyYieldEnabled()) return emptyMap()
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Apenas dias úteis (Segunda a Sexta)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return emptyMap()
        }
        val todayStr = String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        val lastDate = prefs.getString("last_yield_date", "") ?: ""
        if (lastDate == todayStr) {
            return emptyMap()
        }

        val yields = mutableMapOf<String, Double>()
        for (acc in _accounts.value) {
            if (acc.cdiRatePercentage > 0.0 && acc.balance > 0.0) {
                val daily = acc.calculateDailyYield()
                if (daily > 0.0) {
                    yields[acc.id] = daily
                }
            }
        }

        if (yields.isNotEmpty()) {
            applyDailyYields(yields)
        }
        return yields
    }

    fun findMatchingAccount(nameOrMethod: String, fallbackToDefault: Boolean = false): AccountItem? {
        val clean = nameOrMethod.lowercase().trim()
        val bankInfo = BankRegistry.findBankByName(nameOrMethod)

        val matching = _accounts.value.filter { acc ->
            val accName = acc.name.lowercase().trim()
            accName == clean ||
            accName.contains(clean) ||
            clean.contains(accName) ||
            (bankInfo != null && (
                accName.contains(bankInfo.key, ignoreCase = true) ||
                accName.contains(bankInfo.name, ignoreCase = true) ||
                bankInfo.keywords.any { kw -> accName.contains(kw, ignoreCase = true) }
            ))
        }

        // Prioriza a conta principal/padrão que faça parte do saldo livre (evita debitar de cofrinhos ou reservas)
        val found = matching.find { it.isDefault && it.includeInOverallBalance }
            ?: matching.find { it.includeInOverallBalance }
            ?: matching.firstOrNull()

        return if (fallbackToDefault) {
            found ?: getDefaultAccount() ?: _accounts.value.firstOrNull()
        } else {
            found
        }
    }

    fun getOrCreateAccountForBank(bank: SupportedBank): Pair<AccountItem, Boolean> {
        val existing = findMatchingAccount(bank.name, fallbackToDefault = false)
        if (existing != null) {
            return Pair(existing, false)
        }
        val newAcc = AccountItem(
            id = UUID.randomUUID().toString(),
            name = bank.name,
            balance = 0.0,
            colorHex = bank.colorHex,
            bankIcon = bank.icon,
            isDefault = false,
            includeInOverallBalance = true
        )
        addAccount(newAcc)
        return Pair(newAcc, true)
    }

    fun getOrCreateCardForBank(bank: SupportedBank, linkedAccountId: String = ""): Pair<CreditCardItem, Boolean> {
        val existing = _cards.value.find { card ->
            val cName = card.name.lowercase().trim()
            cName.contains(bank.key, ignoreCase = true) ||
            cName.contains(bank.name, ignoreCase = true) ||
            cName.contains(bank.cardName, ignoreCase = true)
        }
        if (existing != null) {
            return Pair(existing, false)
        }
        val newCard = CreditCardItem(
            id = UUID.randomUUID().toString(),
            name = bank.cardName,
            limit = 1000.0,
            closingDay = 1,
            dueDay = 10,
            colorHex = bank.colorHex,
            bankName = bank.name,
            brand = "Mastercard",
            linkedAccountId = linkedAccountId
        )
        addCard(newCard)
        return Pair(newCard, true)
    }

    private fun extractNoteParam(note: String, key: String): String? {
        val parts = note.split("|")
        for (p in parts) {
            val kv = p.split(":")
            if (kv.size == 2 && kv[0].trim() == key) {
                return kv[1].trim()
            }
        }
        return null
    }

    fun transfer(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        isScheduled: Boolean = false,
        date: String = "",
        isRecurring: Boolean = false
    ) {
        val fromAcc = _accounts.value.find { it.id == fromAccountId } ?: return
        val toAcc = _accounts.value.find { it.id == toAccountId } ?: return
        val txDate = if (date.isNotBlank()) date else Formatters.todayDateString()
        val targetMonth = MonthHelper.getMonthYearFromDate(txDate) ?: _selectedMonth.value
        val targetMonthData = ensureMonthExists(targetMonth)

        if (!isScheduled) {
            val currentList = _accounts.value.map {
                when (it.id) {
                    fromAccountId -> it.copy(balance = it.balance - amount)
                    toAccountId -> it.copy(balance = it.balance + amount)
                    else -> it
                }
            }
            _accounts.value = currentList
        }

        val isToReserve = !toAcc.includeInOverallBalance
        val groupId = if (isRecurring) "trf_rec_${UUID.randomUUID()}" else ""
        val txItem = TransactionItem(
            id = "trf_" + UUID.randomUUID().toString(),
            description = if (isScheduled) "Agendado: ${fromAcc.name} ➔ ${toAcc.name}" else "Transferência: ${fromAcc.name} ➔ ${toAcc.name}",
            amount = amount,
            type = TransactionType.TRANSFER,
            category = if (isToReserve) "Cofrinho & Reserva" else "Transferência",
            categoryIcon = if (isToReserve) "🐖" else "⇄",
            categoryColorHex = if (isToReserve) 0xFF0284C7 else 0xFF00A868,
            accountOrCardName = fromAcc.name,
            paymentType = PaymentType.PIX,
            monthYear = targetMonth,
            date = txDate,
            status = if (isScheduled) PaymentStatus.PENDING else PaymentStatus.PAID,
            note = "fromAccountId:${fromAcc.id}|toAccountId:${toAcc.id}",
            includeInBudget = isToReserve,
            isRecurring = isRecurring,
            installmentGroupId = groupId
        )

        if (isRecurring) {
            addRecurringTransaction(txItem)
        } else {
            val updatedItems = listOf(txItem) + targetMonthData.items
            _monthDataMap[targetMonth] = targetMonthData.copy(
                items = updatedItems,
                lastModified = System.currentTimeMillis()
            )
            recalculateAllMonths()
            saveToDisk()
        }
    }

    fun reorderAccounts(newAccounts: List<AccountItem>) {
        _accounts.value = newAccounts
        saveToDisk()
    }

    fun moveAccount(fromIndex: Int, toIndex: Int) {
        val list = _accounts.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _accounts.value = list
            saveToDisk()
        }
    }

    // --- Cartões CRUD ---
    fun addCard(card: CreditCardItem) {
        _cards.value = _cards.value + card
        saveToDisk()
    }

    fun updateCard(updated: CreditCardItem) {
        val oldCard = _cards.value.find { it.id == updated.id }
        val oldName = oldCard?.name?.trim() ?: ""
        val newName = updated.name.trim()

        _cards.value = _cards.value.map { if (it.id == updated.id) updated else it }

        // Se o nome do cartão mudou, sincroniza todos os lançamentos em todos os meses
        if (oldName.isNotBlank() && !oldName.equals(newName, ignoreCase = true)) {
            for ((mKey, mData) in _monthDataMap) {
                var modified = false
                val updatedItems = mData.items.map { item ->
                    if (item.accountOrCardName.trim().equals(oldName, ignoreCase = true)) {
                        modified = true
                        item.copy(accountOrCardName = newName)
                    } else {
                        item
                    }
                }
                if (modified) {
                    _monthDataMap[mKey] = mData.copy(
                        items = updatedItems,
                        lastModified = System.currentTimeMillis()
                    )
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteCard(cardId: String) {
        _cards.value = _cards.value.filter { it.id != cardId }
        recalculateAllMonths()
        saveToDisk()
    }

    fun reorderCards(newCards: List<CreditCardItem>) {
        _cards.value = newCards
        recalculateAllMonths()
        saveToDisk()
    }

    /**
     * Reconcilia lançamentos antigos com cartões ou contas cujos nomes foram alterados
     * anteriormente, garantindo que faturas, extrato e gráficos fiquem 100% atualizados.
     */
    private fun reconcileCardAndAccountNames() {
        // Mapeamento de nomes originais conhecidos para o ID do cartão
        val defaultCardIds = mapOf(
            "cartao inter" to "card_inter",
            "cartão inter" to "card_inter",
            "inter" to "card_inter",
            "cartao will" to "card_will",
            "cartão will" to "card_will",
            "will" to "card_will",
            "will bank" to "card_will",
            "cartao sofisa" to "card_sofisa",
            "cartão sofisa" to "card_sofisa",
            "sofisa" to "card_sofisa",
            "sofisa direto" to "card_sofisa",
            "cartao mercado pago" to "card_mp",
            "cartão mercado pago" to "card_mp",
            "mercado pago" to "card_mp",
            "cartao nubank" to "card_nu",
            "cartão nubank" to "card_nu",
            "nubank" to "card_nu",
            "cartao caixa" to "card_caixa",
            "cartão caixa" to "card_caixa",
            "caixa" to "card_caixa",
            "caixa economica" to "card_caixa",
            "caixa econômica" to "card_caixa"
        )

        var anyModified = false
        val currentCardNames = _cards.value.map { it.name.trim().lowercase() }.toSet()

        for ((mKey, mData) in _monthDataMap) {
            var monthModified = false
            val updatedItems = mData.items.map { item ->
                val cleanTarget = item.accountOrCardName.trim().lowercase()

                // Se o lançamento for de cartão de crédito e o nome atual dele não bate com nenhum cartão ativo:
                if (item.paymentType == PaymentType.CREDIT_CARD && !currentCardNames.contains(cleanTarget)) {
                    val matchedId = defaultCardIds[cleanTarget]
                    val targetCard = if (matchedId != null) {
                        _cards.value.find { it.id == matchedId }
                    } else {
                        _cards.value.find { card ->
                            card.cardType.allowsCredit && (
                                cleanTarget.contains(card.bankName.trim().lowercase()) ||
                                card.name.trim().lowercase().contains(cleanTarget)
                            )
                        }
                    }

                    if (targetCard != null && !item.accountOrCardName.equals(targetCard.name, ignoreCase = true)) {
                        monthModified = true
                        anyModified = true
                        item.copy(accountOrCardName = targetCard.name)
                    } else {
                        item
                    }
                } else {
                    item
                }
            }

            if (monthModified) {
                _monthDataMap[mKey] = mData.copy(
                    items = updatedItems,
                    lastModified = System.currentTimeMillis()
                )
            }
        }

        if (anyModified) {
            recalculateAllMonths()
            saveToDisk()
        }
        reconcileNovemberBudget()
    }

    /**
     * Ajusta os lançamentos de Novembro 2026 para sincronizar 100% com a planilha de controle:
     * - Internet: R$ 79,90
     * - Tim: R$ 55,00
     * - Desodorante: R$ 26,48
     */
    private fun reconcileNovemberBudget() {
        // Ajusta categorias para garantir a existência de Mercado e configurar tetos padrão
        val hasMercadoCat = _categories.value.any { it.id == "cat_merc" || it.name.equals("Mercado", ignoreCase = true) }
        var catList = _categories.value.toMutableList()
        if (!hasMercadoCat) {
            catList.add(0, CategoryItem(id = "cat_merc", name = "Mercado", iconEmoji = "🛒", colorHex = 0xFFFF9800, budgetLimit = 700.0, reserveInBudget = true))
        }
        catList = catList.map {
            if (it.name.equals("Mercado", ignoreCase = true)) {
                it.copy(budgetLimit = 700.0, reserveInBudget = true)
            } else if (it.name.equals("Alimentação", ignoreCase = true) && it.budgetLimit == 700.0) {
                it.copy(budgetLimit = 0.0, reserveInBudget = false)
            } else if (it.name.equals("Transporte", ignoreCase = true) && it.budgetLimit == 250.0) {
                it.copy(budgetLimit = 0.0, reserveInBudget = false)
            } else {
                it
            }
        }.toMutableList()
        if (catList != _categories.value) {
            _categories.value = catList
        }

        val novData = _monthDataMap["NOVEMBRO 2026"] ?: return
        var changed = false
        var novBaseIncome = novData.baseIncome
        if (novBaseIncome <= 0.0) {
            novBaseIncome = 3300.0
            changed = true
        }

        // Remove itens que não existem ou estão em branco na planilha atualizada:
        // - Desodorante: em branco (R$ 0,00)
        // - YouTube Premium avulso: em branco ("lembrar vem inter")
        // - Parcela Growth de 17,65 no Cartão Inter (para a fatura fechar em R$ 362,96)
        val filteredList = novData.items.filterNot {
            it.description.contains("Desodorante", ignoreCase = true) ||
            it.description.contains("GROWTHSUPPLEMENTS", ignoreCase = true) ||
            (it.description.contains("YouTube", ignoreCase = true) && it.paymentType != PaymentType.CREDIT_CARD)
        }
        if (filteredList.size != novData.items.size) {
            changed = true
        }

        val updatedItems = filteredList.map { item ->
            var updated = item

            // 1. Internet: R$ 79,90
            if (updated.description.contains("Internet", ignoreCase = true) && updated.amount != 79.90) {
                updated = updated.copy(amount = 79.90)
                changed = true
            }

            // 2. Tim: R$ 55,00
            if (updated.description.equals("Tim", ignoreCase = true) && updated.amount != 55.00) {
                updated = updated.copy(amount = 55.00)
                changed = true
            }

            // 3. Aluguel: R$ 500,00
            if (updated.description.contains("Aluguel", ignoreCase = true) && updated.amount != 500.00) {
                updated = updated.copy(amount = 500.00)
                changed = true
            }

            // 4. Água: R$ 95,00
            if ((updated.description.contains("Água", ignoreCase = true) || updated.description.contains("Agua", ignoreCase = true)) && updated.amount != 95.00) {
                updated = updated.copy(amount = 95.00)
                changed = true
            }

            // 5. Luz: R$ 100,00
            if (updated.description.contains("Luz", ignoreCase = true) && updated.amount != 100.00) {
                updated = updated.copy(amount = 100.00)
                changed = true
            }

            // 6. Capoeira: R$ 80,00
            if (updated.description.contains("Capoeira", ignoreCase = true) && updated.amount != 80.00) {
                updated = updated.copy(amount = 80.00)
                changed = true
            }

            // 7. Dentista: R$ 109,90
            if (updated.description.contains("Dentista", ignoreCase = true) && updated.amount != 109.90) {
                updated = updated.copy(amount = 109.90)
                changed = true
            }

            // 8. Alex: R$ 25,00
            if (updated.description.contains("Alex", ignoreCase = true) && updated.amount != 25.00) {
                updated = updated.copy(amount = 25.00)
                changed = true
            }

            // 9. Barbeiro: R$ 50,00
            if (updated.description.contains("Barbeiro", ignoreCase = true) && updated.amount != 50.00) {
                updated = updated.copy(amount = 50.00)
                changed = true
            }

            // 10. Compras parceladas antigas de supermercado no Cartão Inter:
            // Pertencem à fatura do cartão (R$ 362,96), não ao teto novo de mercado
            if (updated.paymentType == PaymentType.CREDIT_CARD &&
                (updated.description.contains("ARMACAO", ignoreCase = true) || updated.description.contains("SUPERMERCADOS BH", ignoreCase = true))) {
                if (updated.category != "Cartão de Crédito" || !updated.includeInBudget) {
                    updated = updated.copy(
                        category = "Cartão de Crédito",
                        categoryIcon = "💳",
                        categoryColorHex = 0xFFE91E63,
                        includeInBudget = true
                    )
                    changed = true
                }
            }

            // 11. Cofrinho se já existir como transferência
            if (updated.type == TransactionType.TRANSFER && updated.description.contains("Cofrinho", ignoreCase = true)) {
                if (updated.amount != 1000.0 || !updated.includeInBudget) {
                    updated = updated.copy(amount = 1000.0, includeInBudget = true)
                    changed = true
                }
            }

            // 12. Gladstoni se já existir
            if (updated.description.contains("Gladstoni", ignoreCase = true)) {
                if (updated.amount != 400.0 || updated.type != TransactionType.INCOME) {
                    updated = updated.copy(amount = 400.0, type = TransactionType.INCOME)
                    changed = true
                }
            }

            updated
        }.toMutableList()

        // Gladstoni: R$ 400,00 (Receber)
        val hasGladstoni = updatedItems.any { it.description.contains("Gladstoni", ignoreCase = true) }
        if (!hasGladstoni) {
            val gladstoniItem = TransactionItem(
                id = "gladstoni_nov_2026",
                description = "GLADSTONI",
                amount = 400.0,
                type = TransactionType.INCOME,
                category = "Salário / Receita",
                categoryIcon = "💰",
                categoryColorHex = 0xFF2E7D32,
                accountOrCardName = "Conta Inter",
                paymentType = PaymentType.DEBIT_CARD,
                tags = listOf("pessoal"),
                monthYear = "NOVEMBRO 2026",
                date = "",
                status = PaymentStatus.TO_RECEIVE,
                isRecurring = false,
                includeInBudget = true
            )
            updatedItems.add(gladstoniItem)
            changed = true
        }

        // Cofrinho: R$ 1.000,00 (Aporte)
        val hasCofrinho = updatedItems.any { it.type == TransactionType.TRANSFER && it.description.contains("Cofrinho", ignoreCase = true) }
        if (!hasCofrinho) {
            val cofrinhoItem = TransactionItem(
                id = "cofrinho_nov_2026",
                description = "COFRINHO (Aporte)",
                amount = 1000.0,
                type = TransactionType.TRANSFER,
                category = "Cofrinho & Reserva",
                categoryIcon = "🐖",
                categoryColorHex = 0xFF0284C7,
                accountOrCardName = "Conta Inter",
                paymentType = PaymentType.PIX,
                tags = listOf("pessoal"),
                monthYear = "NOVEMBRO 2026",
                date = "",
                status = PaymentStatus.PENDING,
                isRecurring = false,
                note = "Cofrinho Inter",
                includeInBudget = true
            )
            updatedItems.add(cofrinhoItem)
            changed = true
        }

        if (changed) {
            _monthDataMap["NOVEMBRO 2026"] = novData.copy(
                baseIncome = novBaseIncome,
                items = updatedItems,
                lastModified = System.currentTimeMillis()
            )
            recalculateAllMonths()
            saveToDisk()
        }
    }


    fun moveCard(fromIndex: Int, toIndex: Int) {
        val list = _cards.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _cards.value = list
            saveToDisk()
        }
    }

    fun ensureMonthExists(monthYear: String): MonthData {
        var data = _monthDataMap[monthYear]
        if (data == null) {
            val recurringItems = mutableListOf<TransactionItem>()
            val processedRecGroups = mutableSetOf<String>()

            // Procura lançamentos recorrentes (Despesas Fixas) em meses existentes para projetar no novo mês
            for (mData in _monthDataMap.values) {
                for (itm in mData.items) {
                    if (itm.isRecurring && itm.installmentGroupId.isNotBlank() && !processedRecGroups.contains(itm.installmentGroupId)) {
                        processedRecGroups.add(itm.installmentGroupId)
                        recurringItems.add(
                            itm.copy(
                                id = UUID.randomUUID().toString(),
                                monthYear = monthYear,
                                status = PaymentStatus.PENDING
                            )
                        )
                    }
                }
            }

            // Herda o baseIncome do mês cronologicamente anterior mais próximo (se zerado, continua zerado!)
            val sorted = MonthHelper.sortMonths(_monthDataMap.keys.toList())
            val targetParsed = MonthHelper.parseMonthYear(monthYear)
            var inheritedBaseIncome = 0.0
            if (sorted.isNotEmpty()) {
                if (targetParsed != null) {
                    val priorMonth = sorted.lastOrNull { m ->
                        val p = MonthHelper.parseMonthYear(m)
                        p != null && (p.second < targetParsed.second || (p.second == targetParsed.second && p.first <= targetParsed.first))
                    } ?: sorted.lastOrNull()
                    if (priorMonth != null && _monthDataMap.containsKey(priorMonth)) {
                        inheritedBaseIncome = _monthDataMap[priorMonth]!!.baseIncome
                    }
                } else {
                    inheritedBaseIncome = _monthDataMap[sorted.last()]?.baseIncome ?: 0.0
                }
            }

            data = MonthData(
                monthYear = monthYear,
                items = recurringItems,
                totalExpense = recurringItems.filter { it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.includeInBudget) }.sumOf { it.amount },
                totalIncome = inheritedBaseIncome,
                remainingBalance = inheritedBaseIncome,
                baseIncome = inheritedBaseIncome,
                lastModified = System.currentTimeMillis()
            )
            _monthDataMap[monthYear] = data
            _months.value = MonthHelper.sortMonths(_months.value + monthYear)
            recalculateAllMonths()
        }
        return data
    }

    fun addTransaction(
        item: TransactionItem,
        includePreviousInstallments: Boolean = false,
        isAmountPerInstallment: Boolean = true
    ) {
        val txDate = if (item.date.isBlank()) {
            java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        } else item.date

        val targetMonth = if (item.paymentType == PaymentType.CREDIT_CARD && item.monthYear.isNotBlank()) {
            item.monthYear
        } else if (item.paymentType == PaymentType.CREDIT_CARD) {
            val card = _cards.value.find { it.name.equals(item.accountOrCardName, ignoreCase = true) }
            if (card != null && card.closingDay > 0) {
                MonthHelper.getTargetInvoiceMonth(txDate, card.closingDay) ?: MonthHelper.getMonthYearFromDate(txDate) ?: item.monthYear.ifEmpty { _selectedMonth.value }
            } else {
                MonthHelper.getMonthYearFromDate(txDate) ?: item.monthYear.ifEmpty { _selectedMonth.value }
            }
        } else {
            MonthHelper.getMonthYearFromDate(txDate) ?: item.monthYear.ifEmpty { _selectedMonth.value }
        }
        val finalItem = item.copy(date = txDate, monthYear = targetMonth)

        if (finalItem.isRecurring) {
            addRecurringTransaction(finalItem)
            return
        }

        if (finalItem.totalInstallments > 1) {
            addTransactionWithInstallments(
                baseItem = finalItem,
                totalInstallments = finalItem.totalInstallments,
                startInstallment = finalItem.currentInstallment,
                isAmountPerInstallment = isAmountPerInstallment,
                includePreviousInstallments = includePreviousInstallments
            )
            return
        }

        val month = targetMonth
        val monthData = ensureMonthExists(month)


        // Se for despesa no débito/pix/dinheiro e já paga, debita da conta selecionada
        if (finalItem.type == TransactionType.EXPENSE &&
            (finalItem.paymentType == PaymentType.DEBIT_CARD || finalItem.paymentType == PaymentType.PIX || finalItem.paymentType == PaymentType.CASH) &&
            finalItem.status == PaymentStatus.PAID) {
            val account = findMatchingAccount(finalItem.accountOrCardName, fallbackToDefault = false)
            if (account != null) {
                updateAccount(account.copy(balance = account.balance - finalItem.amount))
            }
        } else if (finalItem.type == TransactionType.INCOME && finalItem.status == PaymentStatus.PAID) {
            val account = findMatchingAccount(finalItem.accountOrCardName, fallbackToDefault = false)
            if (account != null) {
                updateAccount(account.copy(balance = account.balance + finalItem.amount))
            }
        }

        val updatedItems = listOf(finalItem) + monthData.items
        _monthDataMap[month] = monthData.copy(
            items = updatedItems,
            lastModified = System.currentTimeMillis()
        )
        recalculateAllMonths()
        saveToDisk()
    }

    fun addRecurringTransaction(baseItem: TransactionItem, futureMonthsCount: Int = 24) {
        val groupId = if (baseItem.installmentGroupId.isNotBlank()) baseItem.installmentGroupId else "rec_${UUID.randomUUID()}"
        val baseMonth = baseItem.monthYear.ifEmpty { _selectedMonth.value }

        for (i in 0 until futureMonthsCount) {
            val targetMonth = MonthHelper.addMonths(baseMonth, i)
            val monthData = ensureMonthExists(targetMonth)
            val isFirst = (i == 0)

            val itemForMonth = baseItem.copy(
                id = if (isFirst) baseItem.id else UUID.randomUUID().toString(),
                installmentGroupId = groupId,
                isRecurring = true,
                monthYear = targetMonth,
                status = if (isFirst) baseItem.status else PaymentStatus.PENDING,
                description = if (!isFirst && baseItem.type == TransactionType.TRANSFER) {
                    baseItem.description.replace("Transferência:", "Agendado:")
                } else baseItem.description
            )

            val existingIdx = monthData.items.indexOfFirst { it.installmentGroupId == groupId }
            val updatedItems = if (existingIdx != -1) {
                monthData.items.map { if (it.installmentGroupId == groupId) itemForMonth else it }
            } else {
                listOf(itemForMonth) + monthData.items
            }

            _monthDataMap[targetMonth] = monthData.copy(
                items = updatedItems,
                lastModified = System.currentTimeMillis()
            )
        }

        // Sincroniza saldo da conta se já estiver paga no mês inicial
        if (baseItem.status == PaymentStatus.PAID) {
            val account = findMatchingAccount(baseItem.accountOrCardName, fallbackToDefault = false)
            if (account != null) {
                if (baseItem.type == TransactionType.EXPENSE &&
                    (baseItem.paymentType == PaymentType.DEBIT_CARD || baseItem.paymentType == PaymentType.PIX || baseItem.paymentType == PaymentType.CASH)) {
                    updateAccount(account.copy(balance = account.balance - baseItem.amount))
                } else if (baseItem.type == TransactionType.INCOME) {
                    updateAccount(account.copy(balance = account.balance + baseItem.amount))
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    fun addTransactionWithInstallments(
        baseItem: TransactionItem,
        totalInstallments: Int,
        startInstallment: Int = 1,
        isAmountPerInstallment: Boolean = true,
        includePreviousInstallments: Boolean = false
    ) {
        val groupId = if (baseItem.installmentGroupId.isNotBlank()) baseItem.installmentGroupId else UUID.randomUUID().toString()
        val installmentAmount = if (isAmountPerInstallment) {
            baseItem.amount
        } else {
            Math.round((baseItem.amount / totalInstallments) * 100.0) / 100.0
        }

        val baseMonth = baseItem.monthYear.ifEmpty { _selectedMonth.value }
        val cleanDesc = baseItem.description.replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()

        // 1. Parcelas anteriores (lançar nos meses anteriores como PAGAS se solicitado)
        if (includePreviousInstallments && startInstallment > 1) {
            for (p in 1 until startInstallment) {
                val pastMonthYear = MonthHelper.addMonths(baseMonth, -(startInstallment - p))
                val pastData = ensureMonthExists(pastMonthYear)
                val pastItem = baseItem.copy(
                    id = UUID.randomUUID().toString(),
                    installmentGroupId = groupId,
                    currentInstallment = p,
                    totalInstallments = totalInstallments,
                    description = "$cleanDesc ($p/$totalInstallments)",
                    amount = installmentAmount,
                    monthYear = pastMonthYear,
                    status = PaymentStatus.PAID
                )
                val updatedList = listOf(pastItem) + pastData.items.filter { it.installmentGroupId != groupId || it.currentInstallment != p }
                _monthDataMap[pastMonthYear] = pastData.copy(
                    items = updatedList,
                    lastModified = System.currentTimeMillis()
                )
            }
        }

        // 2. Parcela deste mês e meses futuros (de startInstallment até totalInstallments)
        val remainingCount = (totalInstallments - startInstallment) + 1
        for (i in 0 until remainingCount) {
            val installmentNum = startInstallment + i
            val currentMonthYear = MonthHelper.addMonths(baseMonth, i)
            val monthData = ensureMonthExists(currentMonthYear)
            val isCurrent = (i == 0)

            val installmentItem = baseItem.copy(
                id = if (isCurrent) baseItem.id else UUID.randomUUID().toString(),
                installmentGroupId = groupId,
                currentInstallment = installmentNum,
                totalInstallments = totalInstallments,
                description = "$cleanDesc ($installmentNum/$totalInstallments)",
                amount = installmentAmount,
                monthYear = currentMonthYear,
                status = if (isCurrent) baseItem.status else PaymentStatus.PENDING
            )

            val updatedList = listOf(installmentItem) + monthData.items.filter { it.id != installmentItem.id }
            _monthDataMap[currentMonthYear] = monthData.copy(
                items = updatedList,
                lastModified = System.currentTimeMillis()
            )
        }
 
        // Sincroniza saldo da conta se a primeira parcela já estiver paga
        if (baseItem.status == PaymentStatus.PAID) {
            val account = findMatchingAccount(baseItem.accountOrCardName, fallbackToDefault = false)
            if (account != null) {
                if (baseItem.type == TransactionType.EXPENSE &&
                    (baseItem.paymentType == PaymentType.DEBIT_CARD || baseItem.paymentType == PaymentType.PIX || baseItem.paymentType == PaymentType.CASH)) {
                    updateAccount(account.copy(balance = account.balance - installmentAmount))
                } else if (baseItem.type == TransactionType.INCOME) {
                    updateAccount(account.copy(balance = account.balance + installmentAmount))
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    fun updateTransaction(
        updatedItem: TransactionItem,
        previousMonthYear: String? = null,
        updateFutureInstallments: Boolean = false
    ) {
        val targetMonth = if (updatedItem.paymentType == PaymentType.CREDIT_CARD && updatedItem.monthYear.isNotBlank()) {
            updatedItem.monthYear
        } else {
            MonthHelper.getMonthYearFromDate(updatedItem.date) ?: updatedItem.monthYear
        }
        val finalItem = updatedItem.copy(monthYear = targetMonth)
        val oldMonth = previousMonthYear ?: updatedItem.monthYear
        val newMonth = targetMonth

        // Sincroniza saldo das contas bancárias caso o usuário altere a conta, valor ou status
        val oldData = _monthDataMap[oldMonth]
        val oldItem = oldData?.items?.find { it.id == finalItem.id }

        if (oldItem != null) {
            // 1. Reverte o impacto financeiro da transação antiga
            if (oldItem.type == TransactionType.INCOME && oldItem.status == PaymentStatus.PAID) {
                val oldAcc = findMatchingAccount(oldItem.accountOrCardName, fallbackToDefault = false)
                if (oldAcc != null) {
                    updateAccount(oldAcc.copy(balance = oldAcc.balance - oldItem.amount))
                }
            } else if (oldItem.type == TransactionType.EXPENSE &&
                (oldItem.paymentType == PaymentType.DEBIT_CARD || oldItem.paymentType == PaymentType.PIX || oldItem.paymentType == PaymentType.CASH) &&
                oldItem.status == PaymentStatus.PAID) {
                val oldAcc = findMatchingAccount(oldItem.accountOrCardName, fallbackToDefault = false)
                if (oldAcc != null) {
                    updateAccount(oldAcc.copy(balance = oldAcc.balance + oldItem.amount))
                }
            }

            // 2. Aplica o impacto financeiro da transação atualizada
            if (finalItem.type == TransactionType.INCOME && finalItem.status == PaymentStatus.PAID) {
                val newAcc = findMatchingAccount(finalItem.accountOrCardName, fallbackToDefault = false)
                if (newAcc != null) {
                    updateAccount(newAcc.copy(balance = newAcc.balance + finalItem.amount))
                }
            } else if (finalItem.type == TransactionType.EXPENSE &&
                (finalItem.paymentType == PaymentType.DEBIT_CARD || finalItem.paymentType == PaymentType.PIX || finalItem.paymentType == PaymentType.CASH) &&
                finalItem.status == PaymentStatus.PAID) {
                val newAcc = findMatchingAccount(finalItem.accountOrCardName, fallbackToDefault = false)
                if (newAcc != null) {
                    updateAccount(newAcc.copy(balance = newAcc.balance - finalItem.amount))
                }
            }
        }

        if (oldMonth != newMonth) {
            if (oldData != null) {
                val updatedOldItems = oldData.items.filter { it.id != finalItem.id }
                _monthDataMap[oldMonth] = oldData.copy(items = updatedOldItems, lastModified = System.currentTimeMillis())
            }
            val newData = ensureMonthExists(newMonth)
            val updatedNewItems = listOf(finalItem) + newData.items.filter { it.id != finalItem.id }
            _monthDataMap[newMonth] = newData.copy(items = updatedNewItems, lastModified = System.currentTimeMillis())
        } else {
            val data = _monthDataMap[newMonth] ?: ensureMonthExists(newMonth)
            val index = data.items.indexOfFirst { it.id == finalItem.id }
            val updatedItems = if (index != -1) {
                data.items.map { if (it.id == finalItem.id) finalItem else it }
            } else {
                listOf(finalItem) + data.items
            }
            _monthDataMap[newMonth] = data.copy(items = updatedItems, lastModified = System.currentTimeMillis())
        }


        // Se o usuário aumentou parcelas de uma compra que não tinha parcelas
        if (updatedItem.totalInstallments > 1 && updatedItem.installmentGroupId.isBlank()) {
            val groupId = UUID.randomUUID().toString()
            val totalInstallments = updatedItem.totalInstallments
            val totalAmount = updatedItem.amount
            val installmentAmount = Math.round((totalAmount / totalInstallments) * 100.0) / 100.0
            val remainder = Math.round((totalAmount - (installmentAmount * totalInstallments)) * 100.0) / 100.0
            val cleanDesc = updatedItem.description.replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()

            val data = _monthDataMap[newMonth]!!
            val firstItem = updatedItem.copy(
                installmentGroupId = groupId,
                currentInstallment = 1,
                totalInstallments = totalInstallments,
                description = "$cleanDesc (1/$totalInstallments)",
                amount = installmentAmount + remainder
            )
            val updatedList = data.items.map { if (it.id == updatedItem.id) firstItem else it }
            _monthDataMap[newMonth] = data.copy(items = updatedList, lastModified = System.currentTimeMillis())

            for (i in 1 until totalInstallments) {
                val nextMonth = MonthHelper.addMonths(newMonth, i)
                val nextData = ensureMonthExists(nextMonth)
                val itemFuture = updatedItem.copy(
                    id = UUID.randomUUID().toString(),
                    installmentGroupId = groupId,
                    currentInstallment = i + 1,
                    totalInstallments = totalInstallments,
                    description = "$cleanDesc (${i + 1}/$totalInstallments)",
                    amount = installmentAmount,
                    monthYear = nextMonth,
                    status = PaymentStatus.PENDING
                )
                val nextItems = listOf(itemFuture) + nextData.items.filter { it.id != itemFuture.id }
                _monthDataMap[nextMonth] = nextData.copy(items = nextItems, lastModified = System.currentTimeMillis())
            }
        } else if (updateFutureInstallments && updatedItem.installmentGroupId.isNotBlank()) {
            val cleanDesc = updatedItem.description.replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()
            _monthDataMap.forEach { (mKey, mData) ->
                // Aplica alterações apenas do mês atual em diante, preservando o histórico de meses anteriores
                val isCurrentOrFuture = MonthHelper.compareMonths(mKey, newMonth) >= 0
                if (isCurrentOrFuture) {
                    var modified = false
                    val updatedItems = mData.items.map { item ->
                        if (item.installmentGroupId == updatedItem.installmentGroupId && item.id != updatedItem.id) {
                            modified = true
                            item.copy(
                                description = if (item.isRecurring) cleanDesc else "$cleanDesc (${item.currentInstallment}/${item.totalInstallments})",
                                amount = if (item.isRecurring) updatedItem.amount else item.amount,
                                category = updatedItem.category,
                                categoryIcon = updatedItem.categoryIcon,
                                categoryColorHex = updatedItem.categoryColorHex,
                                accountOrCardName = updatedItem.accountOrCardName,
                                paymentType = updatedItem.paymentType,
                                tags = updatedItem.tags
                            )
                        } else item
                    }
                    if (modified) {
                        _monthDataMap[mKey] = mData.copy(items = updatedItems, lastModified = System.currentTimeMillis())
                    }
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    fun updateInstallmentsForTransaction(transactionId: String, totalInstallments: Int) {
        var targetMonth: String? = null
        var targetItem: TransactionItem? = null

        for ((mKey, mData) in _monthDataMap) {
            val found = mData.items.find { it.id == transactionId }
            if (found != null) {
                targetMonth = mKey
                targetItem = found
                break
            }
        }

        if (targetItem == null || targetMonth == null) return

        val baseMonth = targetItem.monthYear.ifEmpty { targetMonth }
        val groupId = if (targetItem.installmentGroupId.isNotBlank()) targetItem.installmentGroupId else "grp_${UUID.randomUUID()}"

        // Se já pertencia a um grupo, limpa outras parcelas futuras/passadas do grupo
        if (targetItem.installmentGroupId.isNotBlank()) {
            for ((mKey, mData) in _monthDataMap) {
                val cleaned = mData.items.filter { it.installmentGroupId != targetItem.installmentGroupId || it.id == targetItem.id }
                if (cleaned.size != mData.items.size) {
                    _monthDataMap[mKey] = mData.copy(items = cleaned, lastModified = System.currentTimeMillis())
                }
            }
        }

        val cleanDesc = targetItem.description.replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()

        val originalTotalAmount = if (targetItem.totalInstallments > 1) {
            targetItem.amount * targetItem.totalInstallments
        } else {
            targetItem.amount
        }

        if (totalInstallments <= 1) {
            val updatedSingle = targetItem.copy(
                description = cleanDesc,
                amount = originalTotalAmount,
                currentInstallment = 1,
                totalInstallments = 1,
                installmentGroupId = ""
            )
            val mData = _monthDataMap[baseMonth] ?: ensureMonthExists(baseMonth)
            _monthDataMap[baseMonth] = mData.copy(
                items = mData.items.map { if (it.id == targetItem.id) updatedSingle else it },
                lastModified = System.currentTimeMillis()
            )
        } else {
            val installmentAmount = Math.round((originalTotalAmount / totalInstallments) * 100.0) / 100.0
            val remainder = Math.round((originalTotalAmount - (installmentAmount * totalInstallments)) * 100.0) / 100.0

            val updatedItem1 = targetItem.copy(
                description = "$cleanDesc (1/$totalInstallments)",
                amount = installmentAmount + remainder,
                currentInstallment = 1,
                totalInstallments = totalInstallments,
                installmentGroupId = groupId
            )
            val mData = _monthDataMap[baseMonth] ?: ensureMonthExists(baseMonth)
            _monthDataMap[baseMonth] = mData.copy(
                items = mData.items.map { if (it.id == targetItem.id) updatedItem1 else it },
                lastModified = System.currentTimeMillis()
            )

            for (i in 1 until totalInstallments) {
                val futureMonth = MonthHelper.addMonths(baseMonth, i)
                val futureData = ensureMonthExists(futureMonth)
                val futureItem = targetItem.copy(
                    id = UUID.randomUUID().toString(),
                    description = "$cleanDesc (${i + 1}/$totalInstallments)",
                    amount = installmentAmount,
                    currentInstallment = i + 1,
                    totalInstallments = totalInstallments,
                    monthYear = futureMonth,
                    status = PaymentStatus.PENDING,
                    installmentGroupId = groupId
                )
                _monthDataMap[futureMonth] = futureData.copy(
                    items = listOf(futureItem) + futureData.items,
                    lastModified = System.currentTimeMillis()
                )
            }
        }

        recalculateAllMonths()
        saveToDisk()

        dismissRecentCapture(transactionId)
    }

    fun toggleStatus(transactionId: String) {
        val currentMonth = _selectedMonth.value
        val current = _monthDataMap[currentMonth] ?: return
        val item = current.items.find { it.id == transactionId } ?: return

        val newStatus = when (item.status) {
            PaymentStatus.PAID -> PaymentStatus.PENDING
            PaymentStatus.PENDING -> PaymentStatus.PAID
            PaymentStatus.TO_RECEIVE -> PaymentStatus.PAID
        }

        val wasPaid = (item.status == PaymentStatus.PAID)
        val isNowPaid = (newStatus == PaymentStatus.PAID)
        if (wasPaid != isNowPaid) {
            if (item.type == TransactionType.TRANSFER) {
                val fromId = extractNoteParam(item.note, "fromAccountId")
                val toId = extractNoteParam(item.note, "toAccountId")
                val fromAcc = _accounts.value.find { it.id == fromId }
                val toAcc = _accounts.value.find { it.id == toId }
                if (fromAcc != null && toAcc != null) {
                    val delta = if (isNowPaid) item.amount else -item.amount
                    val updatedFrom = fromAcc.copy(balance = fromAcc.balance - delta)
                    val updatedTo = toAcc.copy(balance = toAcc.balance + delta)
                    _accounts.value = _accounts.value.map {
                        when (it.id) {
                            updatedFrom.id -> updatedFrom
                            updatedTo.id -> updatedTo
                            else -> it
                        }
                    }
                }
            } else {
                val account = findMatchingAccount(item.accountOrCardName, fallbackToDefault = false)
                if (account != null) {
                    if (item.type == TransactionType.INCOME) {
                        val delta = if (isNowPaid) item.amount else -item.amount
                        updateAccount(account.copy(balance = account.balance + delta))
                    } else if (item.type == TransactionType.EXPENSE &&
                        (item.paymentType == PaymentType.DEBIT_CARD || item.paymentType == PaymentType.PIX || item.paymentType == PaymentType.CASH)) {
                        val delta = if (isNowPaid) -item.amount else item.amount
                        updateAccount(account.copy(balance = account.balance + delta))
                    }
                }
            }
        }

        val updatedList = current.items.map {
            if (it.id == transactionId) {
                val newDesc = if (it.type == TransactionType.TRANSFER) {
                    if (isNowPaid) it.description.replace("Agendado:", "Transferência:") else it.description.replace("Transferência:", "Agendado:")
                } else it.description
                it.copy(status = newStatus, description = newDesc)
            } else it
        }

        val updatedMonth = current.copy(
            items = updatedList,
            lastModified = System.currentTimeMillis()
        )
        _monthDataMap[currentMonth] = updatedMonth
        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteTransaction(
        transactionId: String,
        monthYear: String? = null,
        deleteEntireGroup: Boolean = false
    ) {
        val targetMonth = monthYear ?: _selectedMonth.value

        val itemToDelete = _monthDataMap[targetMonth]?.items?.find { it.id == transactionId }
        if (itemToDelete != null && itemToDelete.status == PaymentStatus.PAID) {
            if (itemToDelete.type == TransactionType.TRANSFER) {
                val fromId = extractNoteParam(itemToDelete.note, "fromAccountId")
                val toId = extractNoteParam(itemToDelete.note, "toAccountId")
                val fromAcc = _accounts.value.find { it.id == fromId }
                val toAcc = _accounts.value.find { it.id == toId }
                if (fromAcc != null && toAcc != null) {
                    val updatedFrom = fromAcc.copy(balance = fromAcc.balance + itemToDelete.amount)
                    val updatedTo = toAcc.copy(balance = toAcc.balance - itemToDelete.amount)
                    _accounts.value = _accounts.value.map {
                        when (it.id) {
                            updatedFrom.id -> updatedFrom
                            updatedTo.id -> updatedTo
                            else -> it
                        }
                    }
                }
            } else {
                val account = findMatchingAccount(itemToDelete.accountOrCardName, fallbackToDefault = false)
                if (account != null) {
                    if (itemToDelete.type == TransactionType.INCOME) {
                        updateAccount(account.copy(balance = account.balance - itemToDelete.amount))
                    } else if (itemToDelete.type == TransactionType.EXPENSE &&
                        (itemToDelete.paymentType == PaymentType.DEBIT_CARD || itemToDelete.paymentType == PaymentType.PIX || itemToDelete.paymentType == PaymentType.CASH)) {
                        updateAccount(account.copy(balance = account.balance + itemToDelete.amount))
                    }
                }
            }
        }

        if (deleteEntireGroup) {
            var targetGroupId = ""
            for (mData in _monthDataMap.values) {
                val itm = mData.items.find { it.id == transactionId }
                if (itm != null) {
                    targetGroupId = itm.installmentGroupId
                    break
                }
            }

            if (targetGroupId.isNotBlank()) {
                _monthDataMap.forEach { (mKey, mData) ->
                    val filtered = mData.items.filter { it.installmentGroupId != targetGroupId }
                    if (filtered.size != mData.items.size) {
                        _monthDataMap[mKey] = mData.copy(items = filtered, lastModified = System.currentTimeMillis())
                    }
                }
            } else {
                val data = _monthDataMap[targetMonth]
                if (data != null) {
                    _monthDataMap[targetMonth] = data.copy(
                        items = data.items.filter { it.id != transactionId },
                        lastModified = System.currentTimeMillis()
                    )
                }
            }
        } else {
            val data = _monthDataMap[targetMonth]
            if (data != null) {
                _monthDataMap[targetMonth] = data.copy(
                    items = data.items.filter { it.id != transactionId },
                    lastModified = System.currentTimeMillis()
                )
            }
        }

        recalculateAllMonths()
        saveToDisk()
        dismissRecentCapture(transactionId)
    }

    fun markTransactionsStatus(ids: Set<String>, status: PaymentStatus, monthYear: String? = null) {
        if (ids.isEmpty()) return
        val targetMonth = monthYear ?: _selectedMonth.value
        val data = _monthDataMap[targetMonth] ?: return

        val updatedItems = data.items.map { item ->
            if (ids.contains(item.id)) {
                if (item.status != PaymentStatus.PAID && status == PaymentStatus.PAID) {
                    val targetAcc = findMatchingAccount(item.accountOrCardName, fallbackToDefault = false) ?: getDefaultAccount()
                    if (targetAcc != null) {
                        if (item.type == TransactionType.EXPENSE && (item.paymentType == PaymentType.DEBIT_CARD || item.paymentType == PaymentType.PIX || item.paymentType == PaymentType.CASH)) {
                            updateAccount(targetAcc.copy(balance = targetAcc.balance - item.amount))
                        } else if (item.type == TransactionType.INCOME) {
                            updateAccount(targetAcc.copy(balance = targetAcc.balance + item.amount))
                        }
                    }
                }
                item.copy(status = status)
            } else item
        }

        _monthDataMap[targetMonth] = data.copy(
            items = updatedItems,
            lastModified = System.currentTimeMillis()
        )
        recalculateAllMonths()
        saveToDisk()
    }

    fun deleteMultipleTransactions(ids: Set<String>, monthYear: String? = null) {
        if (ids.isEmpty()) return
        val targetMonth = monthYear ?: _selectedMonth.value
        val data = _monthDataMap[targetMonth] ?: return

        val updatedItems = data.items.filter { !ids.contains(it.id) }
        _monthDataMap[targetMonth] = data.copy(
            items = updatedItems,
            lastModified = System.currentTimeMillis()
        )
        recalculateAllMonths()
        saveToDisk()
    }

    fun payCardInvoice(cardName: String, monthYear: String, accountId: String, amountPaid: Double) {
        val monthData = _monthDataMap[monthYear] ?: return
        val account = _accounts.value.find { it.id == accountId } ?: getDefaultAccount() ?: _accounts.value.firstOrNull()
        val card = _cards.value.find { it.name.equals(cardName, ignoreCase = true) }

        val updatedItems = monthData.items.map { item ->
            val matches = if (card != null) {
                isTransactionForCard(item, card, _accounts.value)
            } else {
                item.accountOrCardName.equals(cardName, ignoreCase = true) && item.type == TransactionType.EXPENSE && item.paymentType == PaymentType.CREDIT_CARD
            }
            if (matches) {
                item.copy(status = PaymentStatus.PAID)
            } else item
        }.toMutableList()

        if (account != null) {
            val updatedAccount = account.copy(balance = account.balance - amountPaid)
            updateAccount(updatedAccount)

            val paymentItem = TransactionItem(
                id = UUID.randomUUID().toString(),
                description = "Pagamento de Fatura - $cardName",
                amount = amountPaid,
                type = TransactionType.EXPENSE,
                category = "Cartão de Crédito",
                categoryIcon = "💳",
                categoryColorHex = 0xFFFF7A00,
                accountOrCardName = account.name,
                paymentType = PaymentType.DEBIT_CARD,
                monthYear = monthYear,
                status = PaymentStatus.PAID,
                note = "Pagamento da fatura de $monthYear"
            )
            updatedItems.add(0, paymentItem)
        }

        _monthDataMap[monthYear] = monthData.copy(
            items = updatedItems,
            lastModified = System.currentTimeMillis()
        )
        recalculateAllMonths()
        saveToDisk()
    }

    fun clearCurrentMonthTransactions() {
        val current = _currentMonthData.value ?: return
        val updated = current.copy(
            items = emptyList(),
            totalExpense = 0.0,
            remainingBalance = current.totalIncome,
            lastModified = System.currentTimeMillis()
        )
        _monthDataMap[current.monthYear] = updated
        recalculateAllMonths()
        saveToDisk()
    }

    fun clearAllTransactions() {
        _monthDataMap.forEach { (mKey, mData) ->
            _monthDataMap[mKey] = mData.copy(
                items = emptyList(),
                lastModified = System.currentTimeMillis()
            )
        }
        recalculateAllMonths()
        saveToDisk()
    }

    // --- Acerto e Ajuste de Saldo ---
    fun adjustMonthBalance(monthYear: String, targetBalance: Double) {
        val monthData = _monthDataMap[monthYear] ?: ensureMonthExists(monthYear)

        var totalExp = 0.0
        var extraInc = 0.0
        for (item in monthData.items) {
            if (item.type == TransactionType.EXPENSE) {
                totalExp += item.amount
            } else if (item.type == TransactionType.INCOME) {
                extraInc += item.amount
            }
        }

        // targetBalance = baseIncome + extraInc + prevBal - totalExp
        // baseIncome = targetBalance - prevBal - extraInc + totalExp
        val prevBal = monthData.previousMonthBalance
        val calculatedBaseIncome = (targetBalance - prevBal - extraInc + totalExp).coerceAtLeast(0.0)

        val updated = monthData.copy(
            baseIncome = Math.round(calculatedBaseIncome * 100.0) / 100.0,
            lastModified = System.currentTimeMillis()
        )
        _monthDataMap[monthYear] = updated
        recalculateAllMonths()
        saveToDisk()
    }

    fun setMonthBaseIncome(monthYear: String, newBaseIncome: Double, applyToFutureMonths: Boolean = true) {
        val rounded = Math.round(newBaseIncome * 100.0) / 100.0
        val monthData = _monthDataMap[monthYear] ?: ensureMonthExists(monthYear)
        val updated = monthData.copy(
            baseIncome = rounded,
            lastModified = System.currentTimeMillis()
        )
        _monthDataMap[monthYear] = updated

        if (applyToFutureMonths) {
            val sortedMonths = MonthHelper.sortMonths(_monthDataMap.keys.toList())
            val targetParsed = MonthHelper.parseMonthYear(monthYear)
            if (targetParsed != null) {
                for (mKey in sortedMonths) {
                    val p = MonthHelper.parseMonthYear(mKey)
                    if (p != null && (p.second > targetParsed.second || (p.second == targetParsed.second && p.first > targetParsed.first))) {
                        val mData = _monthDataMap[mKey]
                        if (mData != null) {
                            _monthDataMap[mKey] = mData.copy(
                                baseIncome = rounded,
                                lastModified = System.currentTimeMillis()
                            )
                        }
                    }
                }
            } else {
                val currentIndex = sortedMonths.indexOf(monthYear)
                if (currentIndex != -1) {
                    for (i in (currentIndex + 1) until sortedMonths.size) {
                        val nextMonthKey = sortedMonths[i]
                        val nextData = _monthDataMap[nextMonthKey]
                        if (nextData != null) {
                            _monthDataMap[nextMonthKey] = nextData.copy(
                                baseIncome = rounded,
                                lastModified = System.currentTimeMillis()
                            )
                        }
                    }
                }
            }
        }

        recalculateAllMonths()
        saveToDisk()
    }

    private fun isMonthConcluded(monthYear: String): Boolean {
        val pair: Pair<Int, Int> = MonthHelper.parseMonthYear(monthYear) ?: return false
        val monthIdx: Int = pair.first
        val yearVal: Int = pair.second
        val cal = Calendar.getInstance()
        val currentYear: Int = cal.get(Calendar.YEAR)
        val currentMonthIdx: Int = cal.get(Calendar.MONTH) // 0-indexed: Janeiro = 0, Outubro = 9, Novembro = 10
        if (yearVal < currentYear) return true
        if (yearVal == currentYear && monthIdx < currentMonthIdx) return true
        return false
    }

    fun recalculateAllMonths() {
        val monthList = _months.value
        var carriedBalance = 0.0
        var isCascadingStarted = false

        for (monthKey in monthList) {
            val monthData = _monthDataMap[monthKey] ?: continue

            if (monthKey.contains("OUTUBRO 2026", ignoreCase = true)) {
                isCascadingStarted = true
            }

            val salaryItem = monthData.items.find { it.isSalaryReplacement && it.type == TransactionType.INCOME }
            val effectiveBaseInc = if (salaryItem != null && salaryItem.amount > 0) salaryItem.amount else monthData.baseIncome

            var totalExp = 0.0
            var extraInc = 0.0
            for (item in monthData.items) {
                if (item.type == TransactionType.EXPENSE) {
                    totalExp += item.amount
                } else if (item.type == TransactionType.INCOME) {
                    if (!item.isSalaryReplacement) {
                        extraInc += item.amount
                    }
                } else if (item.type == TransactionType.TRANSFER && item.includeInBudget) {
                    totalExp += item.amount
                }
            }

            val baseInc = effectiveBaseInc
            val currentInc = baseInc + extraInc

            val prevMonthKey = MonthHelper.addMonths(monthKey, -1)
            val isPrevConcluded = isMonthConcluded(prevMonthKey)

            val prevBal = if (isCascadingStarted && isPrevConcluded && !monthKey.contains("OUTUBRO 2026", ignoreCase = true)) carriedBalance else 0.0
            val totalInc = currentInc + prevBal
            val remaining = totalInc - totalExp

            val updated = monthData.copy(
                totalExpense = totalExp,
                totalIncome = totalInc,
                remainingBalance = remaining,
                previousMonthBalance = prevBal,
                currentMonthIncome = currentInc,
                baseIncome = effectiveBaseInc,
                items = monthData.items.toList(),
                lastModified = System.currentTimeMillis()
            )
            _monthDataMap[monthKey] = updated

            if (isCascadingStarted) {
                carriedBalance = remaining
            }
        }

        val currentMonth = _selectedMonth.value
        if (_monthDataMap.containsKey(currentMonth)) {
            val cur = _monthDataMap[currentMonth]!!
            _currentMonthData.value = cur.copy(
                items = cur.items.toList(),
                lastModified = System.currentTimeMillis()
            )
        }
    }

    private fun recalculateTotals(monthData: MonthData) {
        recalculateAllMonths()
    }

    fun getInvoiceForCard(card: CreditCardItem, month: String): Double {
        val data = _monthDataMap[month] ?: return 0.0
        return data.items.filter { isTransactionForCard(it, card, _accounts.value) }.sumOf { it.amount }
    }

    fun getInvoiceForCard(cardName: String, month: String): Double {
        val card = _cards.value.find { it.name.equals(cardName, ignoreCase = true) }
        if (card != null) {
            return getInvoiceForCard(card, month)
        }
        val data = _monthDataMap[month] ?: return 0.0
        return data.items.filter {
            it.type == TransactionType.EXPENSE &&
            it.paymentType == PaymentType.CREDIT_CARD &&
            it.accountOrCardName.equals(cardName, ignoreCase = true)
        }.sumOf { it.amount }
    }

    fun getPurchasesForCard(card: CreditCardItem, month: String): List<TransactionItem> {
        val data = _monthDataMap[month] ?: return emptyList()
        return data.items.filter { isTransactionForCard(it, card, _accounts.value) }
    }

    fun getPurchasesForCard(cardName: String, month: String): List<TransactionItem> {
        val card = _cards.value.find { it.name.equals(cardName, ignoreCase = true) }
        if (card != null) {
            return getPurchasesForCard(card, month)
        }
        val data = _monthDataMap[month] ?: return emptyList()
        return data.items.filter {
            it.accountOrCardName.equals(cardName, ignoreCase = true)
        }
    }

    // --- Importador OFX ---
    fun importOfx(content: String, targetMethod: String, monthYear: String): Int {
        var importedCount = 0
        val trnRegex = Regex("<STMTTRN>[\\s\\S]*?</STMTTRN>", RegexOption.IGNORE_CASE)
        val trnamtRegex = Regex("<TRNAMT>([+-]?[0-9.,]+)", RegexOption.IGNORE_CASE)
        val memoRegex = Regex("<MEMO>(.*?)(?:<|$)", RegexOption.IGNORE_CASE)
        val nameRegex = Regex("<NAME>(.*?)(?:<|$)", RegexOption.IGNORE_CASE)
        val dateRegex = Regex("<DTPOSTED>(\\d{4})(\\d{2})(\\d{2})", RegexOption.IGNORE_CASE)

        val matches = trnRegex.findAll(content)
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

                val item = TransactionItem(
                    description = desc,
                    amount = absAmount,
                    type = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME,
                    category = "Cartão de Crédito",
                    accountOrCardName = targetMethod,
                    paymentType = PaymentType.CREDIT_CARD,
                    monthYear = monthYear,
                    date = dateStr,
                    status = PaymentStatus.PAID
                )
                addTransaction(item)
                importedCount++
            }
        }
        return importedCount
    }

    // --- Importador CSV ---
    fun importCsv(content: String, targetMethod: String, monthYear: String): Int {
        var count = 0
        val lines = content.lines()
        for (line in lines) {
            val parts = line.split(";", ",")
            if (parts.size >= 3) {
                val dateStr = parts[0].trim()
                val desc = parts[1].trim()
                val amtStr = parts[2].trim().replace("R$", "").replace(" ", "").replace(",", ".")
                val amt = amtStr.toDoubleOrNull()
                if (amt != null && !desc.equals("descrição", ignoreCase = true)) {
                    val absAmt = Math.abs(amt)
                    val isExp = amt < 0 || !desc.contains("Receb", ignoreCase = true)
                    addTransaction(
                        TransactionItem(
                            description = desc,
                            amount = absAmt,
                            type = if (isExp) TransactionType.EXPENSE else TransactionType.INCOME,
                            category = "Outros",
                            accountOrCardName = targetMethod,
                            paymentType = PaymentType.CREDIT_CARD,
                            monthYear = monthYear,
                            date = dateStr,
                            status = PaymentStatus.PAID
                        )
                    )
                    count++
                }
            }
        }
        return count
    }

    private fun saveToDisk() {
        try {
            val rootObj = JSONObject()

            // Salva Categorias
            val catArr = JSONArray()
            for (cat in _categories.value) {
                val o = JSONObject()
                o.put("id", cat.id)
                o.put("name", cat.name)
                o.put("iconEmoji", cat.iconEmoji)
                o.put("colorHex", cat.colorHex)
                o.put("isExpense", cat.isExpense)
                o.put("budgetLimit", cat.budgetLimit)
                o.put("reserveInBudget", cat.reserveInBudget)
                catArr.put(o)
            }
            rootObj.put("categories", catArr)

            // Salva Tags
            val tagArr = JSONArray()
            for (tag in _tags.value) {
                val o = JSONObject()
                o.put("id", tag.id)
                o.put("name", tag.name)
                o.put("colorHex", tag.colorHex)
                tagArr.put(o)
            }
            rootObj.put("tags", tagArr)

            // Salva Contas
            val accArr = JSONArray()
            for (acc in _accounts.value) {
                val o = JSONObject()
                o.put("id", acc.id)
                o.put("name", acc.name)
                o.put("type", acc.type.name)
                o.put("balance", acc.balance)
                o.put("colorHex", acc.colorHex)
                o.put("bankIcon", acc.bankIcon)
                o.put("isDefault", acc.isDefault)
                o.put("includeInOverallBalance", acc.includeInOverallBalance)
                o.put("cdiRatePercentage", acc.cdiRatePercentage)
                accArr.put(o)
            }
            rootObj.put("accounts", accArr)

            // Salva Cartões
            val cardArr = JSONArray()
            for (card in _cards.value) {
                val o = JSONObject()
                o.put("id", card.id)
                o.put("name", card.name)
                o.put("limit", card.limit)
                o.put("closingDay", card.closingDay)
                o.put("dueDay", card.dueDay)
                o.put("colorHex", card.colorHex)
                o.put("bankName", card.bankName)
                o.put("brand", card.brand)
                o.put("linkedAccountId", card.linkedAccountId)
                o.put("cardType", card.cardType.name)
                cardArr.put(o)
            }
            rootObj.put("cards", cardArr)

            // Salva Meses e Itens
            val monthsArr = JSONArray()
            for (month in _months.value) {
                val data = _monthDataMap[month] ?: continue
                val mObj = JSONObject()
                mObj.put("mes_ano", data.monthYear)
                mObj.put("totalExpense", data.totalExpense)
                mObj.put("totalIncome", data.totalIncome)
                mObj.put("remainingBalance", data.remainingBalance)
                mObj.put("previousMonthBalance", data.previousMonthBalance)
                mObj.put("currentMonthIncome", data.currentMonthIncome)
                mObj.put("baseIncome", data.baseIncome)

                val itemsArr = JSONArray()
                for (item in data.items) {
                    val iObj = JSONObject()
                    iObj.put("id", item.id)
                    iObj.put("description", item.description)
                    iObj.put("amount", item.amount)
                    iObj.put("type", item.type.name)
                    iObj.put("category", item.category)
                    iObj.put("categoryIcon", item.categoryIcon)
                    iObj.put("categoryColorHex", item.categoryColorHex)
                    iObj.put("accountOrCardName", item.accountOrCardName)
                    iObj.put("paymentType", item.paymentType.name)
                    iObj.put("tags", JSONArray(item.tags))
                    iObj.put("date", item.date)
                    iObj.put("status", item.status.name)
                    iObj.put("isRecurring", item.isRecurring)
                    iObj.put("installmentGroupId", item.installmentGroupId)
                    iObj.put("currentInstallment", item.currentInstallment)
                    iObj.put("totalInstallments", item.totalInstallments)
                    iObj.put("note", item.note)
                    iObj.put("includeInBudget", item.includeInBudget)
                    iObj.put("isSalaryReplacement", item.isSalaryReplacement)
                    itemsArr.put(iObj)
                }
                mObj.put("itens", itemsArr)
                monthsArr.put(mObj)
            }
            rootObj.put("months", monthsArr)

            dataFile.writeText(rootObj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseFullSavedJson(jsonString: String) {
        val trimmed = jsonString.trim()
        val rootObj = if (trimmed.startsWith("[")) {
            JSONObject().apply { put("months", JSONArray(trimmed)) }
        } else {
            JSONObject(trimmed)
        }

        // Categorias
        if (rootObj.has("categories")) {
            val arr = rootObj.optJSONArray("categories") ?: JSONArray()
            val list = mutableListOf<CategoryItem>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val catId = o.optString("id", UUID.randomUUID().toString())
                val catName = o.optString("name", "Geral")
                val isMercadoOrAlim = catId == "cat_merc" || catId == "cat_alim" || catName.equals("Mercado", ignoreCase = true)
                val defaultLimit = if (isMercadoOrAlim) 700.0 else 0.0
                val defaultReserve = isMercadoOrAlim
                list.add(
                    CategoryItem(
                        id = catId,
                        name = catName,
                        iconEmoji = o.optString("iconEmoji", "🏷️"),
                        colorHex = o.optLong("colorHex", 0xFF00A868L),
                        isExpense = o.optBoolean("isExpense", true),
                        budgetLimit = o.optDouble("budgetLimit", defaultLimit),
                        reserveInBudget = o.optBoolean("reserveInBudget", defaultReserve)
                    )
                )
            }
            if (list.isNotEmpty()) {
                _categories.value = list
            }
        }

        // Tags
        if (rootObj.has("tags")) {
            val arr = rootObj.optJSONArray("tags") ?: JSONArray()
            val list = mutableListOf<TagItem>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    TagItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", "Tag"),
                        colorHex = o.optLong("colorHex", 0xFF00A868L)
                    )
                )
            }
            if (list.isNotEmpty()) {
                _tags.value = list
            }
        }

        // Contas
        if (rootObj.has("accounts")) {
            val accArr = rootObj.optJSONArray("accounts") ?: JSONArray()
            val accList = mutableListOf<AccountItem>()
            for (i in 0 until accArr.length()) {
                val o = accArr.optJSONObject(i) ?: continue
                val accId = o.optString("id", UUID.randomUUID().toString())
                val rawType = o.optString("type", "CHECKING").uppercase()
                val accType = when {
                    rawType.contains("SAVING") || rawType.contains("POUPAN") -> AccountType.SAVINGS
                    rawType.contains("WALLET") || rawType.contains("CARTEIRA") -> AccountType.WALLET
                    rawType.contains("INVEST") -> AccountType.INVESTMENT
                    else -> AccountType.CHECKING
                }
                val defaultInclude = !(accId.contains("cofrinho", ignoreCase = true) || accId.contains("emergencia", ignoreCase = true) || accType == AccountType.SAVINGS || accType == AccountType.INVESTMENT)
                val accName = o.optString("name", "Conta")
                val defaultCdi = if (accName.contains("Mercado Pago", ignoreCase = true) || accId.contains("mp", ignoreCase = true)) 115.0 else 0.0
                accList.add(
                    AccountItem(
                        id = accId,
                        name = accName,
                        type = accType,
                        balance = o.optDouble("balance", 0.0),
                        colorHex = o.optLong("colorHex", 0xFF00A86BL),
                        bankIcon = o.optString("bankIcon", "🏦"),
                        isDefault = o.optBoolean("isDefault", i == 0),
                        includeInOverallBalance = o.optBoolean("includeInOverallBalance", defaultInclude),
                        cdiRatePercentage = o.optDouble("cdiRatePercentage", defaultCdi)
                    )
                )
            }
            if (accList.isNotEmpty()) {
                if (accList.none { it.isDefault }) {
                    accList[0] = accList[0].copy(isDefault = true)
                }
                _accounts.value = accList
            }
        }

        // Cartões
        if (rootObj.has("cards")) {
            val cardArr = rootObj.optJSONArray("cards") ?: JSONArray()
            val cardList = mutableListOf<CreditCardItem>()
            for (i in 0 until cardArr.length()) {
                val o = cardArr.optJSONObject(i) ?: continue
                val rawCardType = o.optString("cardType", "CREDIT").uppercase()
                val cType = when {
                    rawCardType.contains("VIRTUAL_DEBIT") || (rawCardType.contains("VIRTUAL") && rawCardType.contains("DEBIT")) -> CardType.VIRTUAL_DEBIT
                    rawCardType.contains("VIRTUAL_CREDIT") || (rawCardType.contains("VIRTUAL") && rawCardType.contains("CREDIT")) -> CardType.VIRTUAL_CREDIT
                    rawCardType.contains("MULTIPLE") || rawCardType.contains("MULTIPLO") -> CardType.MULTIPLE
                    rawCardType.contains("DEBIT") || rawCardType.contains("DEBITO") -> CardType.DEBIT
                    else -> CardType.CREDIT
                }
                cardList.add(
                    CreditCardItem(
                        id = o.optString("id", UUID.randomUUID().toString()),
                        name = o.optString("name", "Cartão"),
                        limit = o.optDouble("limit", 1000.0),
                        closingDay = o.optInt("closingDay", 6),
                        dueDay = o.optInt("dueDay", 15),
                        colorHex = o.optLong("colorHex", 0xFFFF7A00L),
                        bankName = o.optString("bankName", "Banco"),
                        brand = o.optString("brand", "Mastercard"),
                        linkedAccountId = o.optString("linkedAccountId", ""),
                        cardType = cType
                    )
                )
            }
            if (cardList.isNotEmpty()) {
                _cards.value = cardList
            }
        }

        // Meses
        val monthsArr = rootObj.optJSONArray("months") ?: JSONArray()
        val monthList = mutableListOf<String>()
        for (i in 0 until monthsArr.length()) {
            val mObj = monthsArr.optJSONObject(i) ?: continue
            val rawMonth = mObj.optString("mes_ano", mObj.optString("monthYear", ""))
            val month = rawMonth.replace("\uFFFD", "Ç").trim()
            if (month.isBlank()) continue
            monthList.add(month)

            val itemsArr = mObj.optJSONArray("itens") ?: mObj.optJSONArray("items") ?: JSONArray()
            val items = mutableListOf<TransactionItem>()
            for (j in 0 until itemsArr.length()) {
                val iObj = itemsArr.optJSONObject(j) ?: continue
                val tArr = iObj.optJSONArray("tags") ?: JSONArray()
                val tagList = mutableListOf<String>()
                for (k in 0 until tArr.length()) tagList.add(tArr.getString(k))

                val rawType = iObj.optString("type", "EXPENSE").uppercase()
                val transType = when {
                    rawType.contains("INCOME") || rawType.contains("RECEITA") -> TransactionType.INCOME
                    rawType.contains("TRANSFER") -> TransactionType.TRANSFER
                    else -> TransactionType.EXPENSE
                }

                val rawPayType = iObj.optString("paymentType", "CREDIT_CARD").uppercase()
                val payType = when {
                    rawPayType.contains("DEBIT") || rawPayType.contains("DEBITO") -> PaymentType.DEBIT_CARD
                    rawPayType.contains("PIX") -> PaymentType.PIX
                    rawPayType.contains("CASH") || rawPayType.contains("DINHEIRO") -> PaymentType.CASH
                    rawPayType.contains("INVEST") -> PaymentType.INVESTMENT
                    else -> PaymentType.CREDIT_CARD
                }

                val rawStatus = iObj.optString("status", "PENDING").uppercase()
                val payStatus = when {
                    rawStatus.contains("PAGO") || rawStatus.contains("PAID") -> PaymentStatus.PAID
                    rawStatus.contains("RECEBER") || rawStatus.contains("TO_RECEIVE") -> PaymentStatus.TO_RECEIVE
                    else -> PaymentStatus.PENDING
                }

                val desc = iObj.optString("description", iObj.optString("descricao", "Lançamento"))
                val amt = if (iObj.has("amount")) iObj.optDouble("amount", 0.0) else iObj.optDouble("valor", 0.0)
                val catName = iObj.optString("category", iObj.optString("categoria", "Geral"))
                val accCardName = iObj.optString("accountOrCardName", iObj.optString("forma_pagamento", "Conta Principal"))
                val dt = iObj.optString("date", iObj.optString("vencimento", ""))

                items.add(
                    TransactionItem(
                        id = iObj.optString("id", UUID.randomUUID().toString()),
                        description = desc,
                        amount = amt,
                        type = transType,
                        category = catName,
                        categoryIcon = iObj.optString("categoryIcon", "🏷️"),
                        categoryColorHex = iObj.optLong("categoryColorHex", 0xFF757575L),
                        accountOrCardName = accCardName,
                        paymentType = payType,
                        tags = tagList,
                        monthYear = month,
                        date = dt,
                        status = payStatus,
                        isRecurring = iObj.optBoolean("isRecurring", false),
                        installmentGroupId = iObj.optString("installmentGroupId", ""),
                        currentInstallment = iObj.optInt("currentInstallment", 1),
                        totalInstallments = iObj.optInt("totalInstallments", 1),
                        note = iObj.optString("note", ""),
                        includeInBudget = iObj.optBoolean("includeInBudget", true),
                        isSalaryReplacement = iObj.optBoolean("isSalaryReplacement", false)
                    )
                )
            }

            _monthDataMap[month] = MonthData(
                monthYear = month,
                items = items,
                totalExpense = mObj.optDouble("totalExpense", 0.0),
                totalIncome = mObj.optDouble("totalIncome", 0.0),
                remainingBalance = mObj.optDouble("remainingBalance", 0.0),
                previousMonthBalance = mObj.optDouble("previousMonthBalance", 0.0),
                currentMonthIncome = mObj.optDouble("currentMonthIncome", 0.0),
                baseIncome = mObj.optDouble("baseIncome", 0.0)
            )
        }

        if (monthList.isNotEmpty()) {
            _months.value = monthList
            applyOctoberBackwardsPaidStatus()
            purgeSpuriousNotificationEchoes()
            purgeFundoEmergenciaAndGastosAdicionais()
            purgeMercadoPlaceholder()
            recalculateAllMonths()
            val defaultMonth = monthList.find { it.contains("OUTUBRO 2026") }
                ?: monthList.firstOrNull() ?: ""
            selectMonth(defaultMonth)
        }
    }

    fun isMonthOctober2026OrEarlier(monthYear: String): Boolean {
        val monthOrder = listOf("JANEIRO", "FEVEREIRO", "MARÇO", "MARCO", "ABRIL", "MAIO", "JUNHO", "JULHO", "AGOSTO", "SETEMBRO", "OUTUBRO", "NOVEMBRO", "NOVEMBO", "DEZEMBRO")
        val parts = monthYear.trim().split(" ")
        if (parts.size >= 2) {
            val mName = parts[0].uppercase()
            val year = parts[1].toIntOrNull() ?: 2026
            if (year < 2026) return true
            if (year == 2026) {
                val mIdx = monthOrder.indexOfFirst { mName.contains(it) }
                val octIdx = monthOrder.indexOfFirst { it.contains("OUTUBRO") }
                return mIdx in 0..octIdx
            }
            return false
        }
        return false
    }

    fun applyOctoberBackwardsPaidStatus() {
        var changed = false
        for ((mYear, mData) in _monthDataMap) {
            if (isMonthOctober2026OrEarlier(mYear)) {
                val updatedItems = mData.items.map {
                    if (it.status != PaymentStatus.PAID) {
                        changed = true
                        it.copy(status = PaymentStatus.PAID)
                    } else it
                }
                if (changed) {
                    _monthDataMap[mYear] = mData.copy(items = updatedItems)
                }
            }
        }
        if (changed) {
            recalculateAllMonths()
            saveToDisk()
        }
    }

    fun purgeSpuriousNotificationEchoes() {
        var changed = false
        for ((mYear, mData) in _monthDataMap) {
            val filtered = mData.items.filterNot {
                it.description.contains("Compra Registrada!", ignoreCase = true) ||
                it.description.contains("Receita Registrada!", ignoreCase = true)
            }
            if (filtered.size != mData.items.size) {
                changed = true
                _monthDataMap[mYear] = mData.copy(items = filtered)
            }
        }
        _recentCapturedTransactions.value = _recentCapturedTransactions.value.filterNot {
            it.description.contains("Registrada!", ignoreCase = true)
        }
        if (changed) {
            recalculateAllMonths()
            saveToDisk()
        }
    }

    fun purgeFundoEmergenciaAndGastosAdicionais() {
        var changed = false
        for ((mYear, mData) in _monthDataMap) {
            val filtered = mData.items.filterNot {
                val desc = it.description.uppercase()
                (desc.contains("FUNDO") && desc.contains("EMERG")) ||
                (desc.contains("GASTO") && desc.contains("ADICIONA"))
            }
            if (filtered.size != mData.items.size) {
                changed = true
                _monthDataMap[mYear] = mData.copy(items = filtered)
            }
        }
        if (changed) {
            recalculateAllMonths()
            saveToDisk()
        }
    }

    fun purgeMercadoPlaceholder() {
        var changed = false
        for ((mYear, mData) in _monthDataMap) {
            val filtered = mData.items.filterNot {
                it.description.trim().equals("MERCADO", ignoreCase = true) && it.amount == 700.0
            }
            if (filtered.size != mData.items.size) {
                changed = true
                _monthDataMap[mYear] = mData.copy(items = filtered)
            }
        }
        if (changed) {
            recalculateAllMonths()
            saveToDisk()
        }
    }

    fun exportBackupJson(): String {
        saveToDisk()
        return if (dataFile.exists()) dataFile.readText() else "{}"
    }

    fun importBackupJson(jsonString: String): Boolean {
        return try {
            val trimmed = jsonString.trim()
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                android.util.Log.e("FinanceRepository", "Formato inválido: não inicia com { ou [")
                return false
            }
            parseFullSavedJson(trimmed)
            saveToDisk()
            true
        } catch (e: Exception) {
            android.util.Log.e("FinanceRepository", "Erro ao importar backup JSON", e)
            e.printStackTrace()
            false
        }
    }
}
