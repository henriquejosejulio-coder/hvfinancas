package com.example.mobillsfinancas.data

import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class PaymentType {
    CREDIT_CARD, // Cartão de Crédito
    DEBIT_CARD,  // Débito / Conta Corrente
    PIX,         // Pix
    CASH,        // Dinheiro
    INVESTMENT   // Investimento / Cofrinho
}

enum class PaymentStatus {
    PAID,       // PAGO
    PENDING,    // PENDENTE
    TO_RECEIVE  // A RECEBER
}

enum class AccountType {
    CHECKING,   // Conta Corrente
    SAVINGS,    // Poupança / Cofrinho
    WALLET,     // Carteira (Dinheiro)
    INVESTMENT  // Fundo de Emergência / Investimentos
}

data class CategoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconEmoji: String,
    val colorHex: Long,
    val isExpense: Boolean = true,
    val budgetLimit: Double = 0.0, // Teto mensal de gastos (0.0 = sem limite definido)
    val reserveInBudget: Boolean = false // Se true, o teto restante entra na reserva do Total de Despesas do mês
)

data class TagItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: Long = 0xFF00A868
)

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val amount: Double,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: String = "Outros",
    val categoryIcon: String = "🏷️",
    val categoryColorHex: Long = 0xFF757575,
    val accountOrCardName: String = "Conta Inter",
    val paymentType: PaymentType = PaymentType.CREDIT_CARD,
    val tags: List<String> = emptyList(),
    val monthYear: String,
    val date: String = "",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val isRecurring: Boolean = false,
    val installmentGroupId: String = "",
    val currentInstallment: Int = 1,
    val totalInstallments: Int = 1,
    val note: String = "",
    val includeInBudget: Boolean = true,
    val isSalaryReplacement: Boolean = false
)

enum class CardType(val label: String, val icon: String, val badgeText: String) {
    CREDIT("Crédito", "💳", "CRÉDITO"),
    DEBIT("Somente Débito", "💳", "DÉBITO"),
    MULTIPLE("Crédito & Débito", "🔄", "MÚLTIPLO"),
    VIRTUAL_CREDIT("Crédito Virtual", "🌐", "VIRTUAL CRÉDITO"),
    VIRTUAL_DEBIT("Débito Virtual", "🌐", "VIRTUAL DÉBITO");

    val isVirtual: Boolean
        get() = this == VIRTUAL_CREDIT || this == VIRTUAL_DEBIT

    val allowsCredit: Boolean
        get() = this == CREDIT || this == MULTIPLE || this == VIRTUAL_CREDIT

    val allowsDebit: Boolean
        get() = this == DEBIT || this == MULTIPLE || this == VIRTUAL_DEBIT
}

data class CreditCardItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val limit: Double,
    val closingDay: Int = 6,
    val dueDay: Int = 15,
    val colorHex: Long = 0xFFFF7A00,
    val bankName: String = "Banco",
    val brand: String = "Mastercard",
    val linkedAccountId: String = "",
    val cardType: CardType = CardType.CREDIT
)

data class AccountItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val balance: Double = 0.0,
    val colorHex: Long = 0xFF00A868,
    val bankIcon: String = "🏦",
    val isDefault: Boolean = false,
    val includeInOverallBalance: Boolean = true, // false para Cofrinhos, Reservas e Investimentos (não somam no saldo livre)
    val cdiRatePercentage: Double = 0.0 // Ex: 115.0 para 115% do CDI (Mercado Pago), 100.0 para 100% do CDI
) {
    fun calculateMonthlyYield(annualCdiRate: Double = 10.65, businessDaysCount: Int = 21): Double {
        if (balance <= 0 || cdiRatePercentage <= 0) return 0.0
        val effectiveAnnualRate = (annualCdiRate * (cdiRatePercentage / 100.0)) / 100.0
        val dailyRate = Math.pow(1.0 + effectiveAnnualRate, 1.0 / 252.0) - 1.0
        return balance * (Math.pow(1.0 + dailyRate, businessDaysCount.toDouble()) - 1.0)
    }

    fun calculateDailyYield(annualCdiRate: Double = 10.65, isBusinessDay: Boolean = true): Double {
        if (balance <= 0 || cdiRatePercentage <= 0 || !isBusinessDay) return 0.0
        val effectiveAnnualRate = (annualCdiRate * (cdiRatePercentage / 100.0)) / 100.0
        val dailyRate = Math.pow(1.0 + effectiveAnnualRate, 1.0 / 252.0) - 1.0
        return balance * dailyRate
    }
}

data class MonthData(
    val monthYear: String,
    val items: List<TransactionItem> = emptyList(),
    val totalExpense: Double,
    val totalIncome: Double,
    val remainingBalance: Double,
    val previousMonthBalance: Double = 0.0,
    val currentMonthIncome: Double = 0.0,
    val baseIncome: Double = 0.0,
    val lastModified: Long = System.currentTimeMillis()
)

fun isTransactionForCard(
    item: TransactionItem,
    card: CreditCardItem,
    accounts: List<AccountItem> = emptyList()
): Boolean {
    if (item.type != TransactionType.EXPENSE) return false

    // 1. Verificação de Tipo de Pagamento vs Modalidade do Cartão:
    if (!card.cardType.allowsCredit) {
        // Cartão Somente Débito: NUNCA aceita parcelas ou compras no crédito! Apenas débito em conta.
        if (item.paymentType != PaymentType.DEBIT_CARD) return false
    } else if (card.cardType == CardType.CREDIT || card.cardType == CardType.VIRTUAL_CREDIT) {
        // Cartão Somente Crédito: NUNCA aceita débito. Apenas faturas/compras no crédito!
        if (item.paymentType != PaymentType.CREDIT_CARD) return false
    } else if (card.cardType == CardType.MULTIPLE) {
        // Cartão Múltiplo: Na fatura do cartão conta Crédito.
        if (item.paymentType != PaymentType.CREDIT_CARD) return false
    }

    // 2. Identificação do Cartão / Conta:
    val itemTarget = item.accountOrCardName.trim()
    val cardName = card.name.trim()

    // Bate exatamente com o nome do cartão (case-insensitive)
    if (itemTarget.equals(cardName, ignoreCase = true)) {
        return true
    }

    // Se o cartão de débito estiver vinculado a uma conta, e a despesa de débito foi lançada com o nome dessa conta
    if (!card.cardType.allowsCredit && card.linkedAccountId.isNotEmpty()) {
        val linkedAcc = accounts.find { it.id == card.linkedAccountId }
        if (linkedAcc != null && itemTarget.equals(linkedAcc.name.trim(), ignoreCase = true)) {
            return true
        }
    }

    return false
}

