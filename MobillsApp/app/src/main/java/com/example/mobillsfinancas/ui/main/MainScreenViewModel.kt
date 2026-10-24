package com.example.mobillsfinancas.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.mobillsfinancas.data.*

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FinanceRepository.getInstance(application.applicationContext)

    val months = repository.months
    val selectedMonth = repository.selectedMonth
    val currentMonthData = repository.currentMonthData
    val cards = repository.cards
    val accounts = repository.accounts
    val categories = repository.categories
    val tags = repository.tags
    val recentCapturedTransactions = repository.recentCapturedTransactions
    val recentCapturedTransaction = repository.recentCapturedTransaction

    var hideValues by mutableStateOf(false)
        private set

    fun applyQuickInstallments(transactionId: String, totalInstallments: Int) {
        repository.updateInstallmentsForTransaction(transactionId, totalInstallments)
    }

    fun dismissQuickInstallments(id: String? = null) {
        repository.dismissRecentCapture(id)
    }

    fun toggleHideValues() {
        hideValues = !hideValues
    }

    fun selectMonth(month: String) {
        repository.selectMonth(month)
    }

    fun addTransaction(
        item: TransactionItem,
        includePreviousInstallments: Boolean = false,
        isAmountPerInstallment: Boolean = true
    ) {
        repository.addTransaction(item, includePreviousInstallments, isAmountPerInstallment)
    }

    fun toggleStatus(transactionId: String) {
        repository.toggleStatus(transactionId)
    }

    fun updateTransaction(
        updated: TransactionItem,
        previousMonthYear: String? = null,
        updateFutureInstallments: Boolean = false
    ) {
        repository.updateTransaction(updated, previousMonthYear, updateFutureInstallments)
    }

    fun deleteTransaction(
        transactionId: String,
        monthYear: String? = null,
        deleteEntireGroup: Boolean = false
    ) {
        repository.deleteTransaction(transactionId, monthYear, deleteEntireGroup)
    }

    fun markTransactionsStatus(ids: Set<String>, status: PaymentStatus) {
        repository.markTransactionsStatus(ids, status, selectedMonth.value)
    }

    fun deleteMultipleTransactions(ids: Set<String>) {
        repository.deleteMultipleTransactions(ids, selectedMonth.value)
    }

    fun payCardInvoice(cardName: String, accountId: String, amountPaid: Double) {
        repository.payCardInvoice(cardName, selectedMonth.value, accountId, amountPaid)
    }

    fun clearCurrentMonthTransactions() {
        repository.clearCurrentMonthTransactions()
    }

    fun clearAllTransactions() {
        repository.clearAllTransactions()
    }

    fun adjustMonthBalance(monthYear: String, targetBalance: Double) {
        repository.adjustMonthBalance(monthYear, targetBalance)
    }

    fun setMonthBaseIncome(monthYear: String, newBaseIncome: Double, applyToFutureMonths: Boolean = true) {
        repository.setMonthBaseIncome(monthYear, newBaseIncome, applyToFutureMonths)
    }

    // Gestão de Cartões
    fun addCard(card: CreditCardItem) {
        repository.addCard(card)
    }

    fun updateCard(card: CreditCardItem) {
        repository.updateCard(card)
    }

    fun deleteCard(cardId: String) {
        repository.deleteCard(cardId)
    }

    // Gestão de Bancos / Contas
    fun addAccount(account: AccountItem) {
        repository.addAccount(account)
    }

    fun updateAccount(account: AccountItem) {
        repository.updateAccount(account)
    }

    fun deleteAccount(accountId: String) {
        repository.deleteAccount(accountId)
    }

    fun applyDailyYields(yields: Map<String, Double>) {
        repository.applyDailyYields(yields)
    }

    fun setAutoApplyYield(enabled: Boolean) {
        repository.setAutoApplyYield(enabled)
    }

    fun isAutoApplyYieldEnabled(): Boolean {
        return repository.isAutoApplyYieldEnabled()
    }

    fun getLastYieldDate(): String {
        return repository.getLastYieldDate()
    }

    fun reorderAccounts(newAccounts: List<AccountItem>) {
        repository.reorderAccounts(newAccounts)
    }

    fun reorderCards(newCards: List<CreditCardItem>) {
        repository.reorderCards(newCards)
    }

    fun transfer(
        fromId: String,
        toId: String,
        amount: Double,
        isScheduled: Boolean = false,
        date: String = "",
        isRecurring: Boolean = false
    ) {
        repository.transfer(fromId, toId, amount, isScheduled, date, isRecurring)
    }

    // Categorias e Tags
    fun addCategory(category: CategoryItem) {
        repository.addCategory(category)
    }

    fun updateCategory(category: CategoryItem) {
        repository.updateCategory(category)
    }

    fun updateCategoryBudget(categoryId: String, budget: Double, reserveInBudget: Boolean = true) {
        repository.updateCategoryBudget(categoryId, budget, reserveInBudget)
    }

    fun deleteCategory(id: String) {
        repository.deleteCategory(id)
    }

    fun addTag(tag: TagItem) {
        repository.addTag(tag)
    }

    fun updateTag(tag: TagItem) {
        repository.updateTag(tag)
    }

    fun deleteTag(id: String) {
        repository.deleteTag(id)
    }

    fun getCardInvoice(card: CreditCardItem): Double {
        return repository.getInvoiceForCard(card, selectedMonth.value)
    }

    fun getCardInvoice(cardName: String): Double {
        return repository.getInvoiceForCard(cardName, selectedMonth.value)
    }

    fun getPurchasesForCard(card: CreditCardItem): List<TransactionItem> {
        return repository.getPurchasesForCard(card, selectedMonth.value)
    }

    fun getPurchasesForCard(cardName: String): List<TransactionItem> {
        return repository.getPurchasesForCard(cardName, selectedMonth.value)
    }

    fun importOfx(content: String, target: String): Int {
        return repository.importOfx(content, target, selectedMonth.value)
    }

    fun importCsv(content: String, target: String): Int {
        return repository.importCsv(content, target, selectedMonth.value)
    }

    fun exportBackup(): String {
        return repository.exportBackupJson()
    }

    fun importBackup(jsonString: String): Boolean {
        return repository.importBackupJson(jsonString)
    }

    fun scanAndRestoreBestData(): Pair<Boolean, Int> {
        return repository.restoreFromBestAvailableSource()
    }
}
