package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.CategoryItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun CategoryBreakdown(
    items: List<TransactionItem>,
    categories: List<CategoryItem> = emptyList(),
    totalExpense: Double,
    hideValues: Boolean,
    onSetBudgetLimit: ((categoryId: String, limit: Double, reserveInBudget: Boolean) -> Unit)? = null,
    onEditTransaction: ((TransactionItem) -> Unit)? = null
) {
    var selectedCategoryForBudget by remember { mutableStateOf<CategoryItem?>(null) }
    var viewingCategoryDetails by remember { mutableStateOf<CategoryItem?>(null) }
    var budgetInputTextFieldValue by remember { mutableStateOf(TextFieldValue("", TextRange(0))) }
    val budgetInputText = budgetInputTextFieldValue.text

    var filterMode by remember { mutableIntStateOf(0) } // 0 = No Teto de Gastos, 1 = Total Geral (Tudo)

    // Agrupamento dos gastos
    val expenseInBudgetByCategory = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE && it.includeInBudget }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val expenseExcludedByCategory = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE && !it.includeInBudget }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
    val expenseTotalByCategory = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    val totalSpentInBudget = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE && it.includeInBudget }.sumOf { it.amount }
    }
    val totalSpentExcluded = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE && !it.includeInBudget }.sumOf { it.amount }
    }
    val totalSpentAll = remember(items) {
        items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    // Categorias relevantes conforme o filtro ativo
    val displayCategories = remember(categories, expenseTotalByCategory, expenseInBudgetByCategory, filterMode) {
        categories.filter { cat ->
            cat.isExpense && (cat.budgetLimit > 0 || (expenseTotalByCategory[cat.name] ?: 0.0) > 0)
        }.sortedWith(
            if (filterMode == 0) {
                compareByDescending<CategoryItem> { it.budgetLimit > 0 }
                    .thenByDescending { expenseInBudgetByCategory[it.name] ?: 0.0 }
            } else {
                compareByDescending<CategoryItem> { expenseTotalByCategory[it.name] ?: 0.0 }
            }
        ).ifEmpty {
            expenseTotalByCategory.keys.map { name ->
                CategoryItem(id = name, name = name, iconEmoji = "🏷️", colorHex = 0xFF757575)
            }
        }
    }

    if (displayCategories.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (filterMode == 0) "🎯 Tetos & Metas de Gastos" else "📊 Relatório Geral de Gastos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (filterMode == 0) {
                            "Despesas computadas no teto mensal"
                        } else {
                            "100% dos gastos reais do mês"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filterMode == 0 && totalSpentExcluded > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF64748B).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "+ ${Formatters.formatMoney(totalSpentExcluded, hideValues)} fora",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ideia 1: Alternador com 1 toque entre "No Teto" e "Total Geral"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterMode = 0 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (filterMode == 0) MobillsGreenPrimary else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎯 No Teto (${Formatters.formatMoney(totalSpentInBudget, hideValues)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (filterMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { filterMode = 1 },
                    shape = RoundedCornerShape(8.dp),
                    color = if (filterMode == 1) MobillsGreenPrimary else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📊 Total Geral (${Formatters.formatMoney(totalSpentAll, hideValues)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (filterMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Gráfico Multi-Colorido de Distribuição Geral de Gastos (quando no Total Geral)
            if (filterMode == 1 && totalSpentAll > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Distribuição dos Gastos",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${displayCategories.count { (expenseTotalByCategory[it.name] ?: 0.0) > 0 }} categorias",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        displayCategories.forEach { c ->
                            val cAmt = expenseTotalByCategory[c.name] ?: 0.0
                            if (cAmt > 0) {
                                val weight = (cAmt / totalSpentAll).toFloat()
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .fillMaxHeight()
                                        .background(Color(c.colorHex))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            displayCategories.forEach { cat ->
                val spentInBudget = expenseInBudgetByCategory[cat.name] ?: 0.0
                val spentExcluded = expenseExcludedByCategory[cat.name] ?: 0.0
                val spentTotal = expenseTotalByCategory[cat.name] ?: 0.0

                val spent = if (filterMode == 0) spentInBudget else spentTotal
                val hasBudget = cat.budgetLimit > 0
                val limit = cat.budgetLimit
                val remaining = limit - spent

                val ratio = if (filterMode == 0) {
                    if (hasBudget) (spent / limit).toFloat()
                    else if (totalSpentInBudget > 0) (spent / totalSpentInBudget).toFloat()
                    else 0f
                } else {
                    if (totalSpentAll > 0) (spent / totalSpentAll).toFloat()
                    else 0f
                }
                val percent = (ratio * 100).toInt()

                val barColor = when {
                    filterMode == 1 -> Color(cat.colorHex)
                    !hasBudget -> Color(cat.colorHex)
                    percent < 75 -> MobillsGreenPrimary
                    percent < 100 -> Color(0xFFEAB308) // Alerta amarelo
                    else -> ExpenseRed // Estourou o teto
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            // Ao clicar no card, abre a tela de detalhes com todos os gastos da categoria
                            viewingCategoryDetails = cat
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(cat.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasBudget) {
                                        Text(
                                            text = "Teto mensal: ${Formatters.formatMoney(limit, hideValues)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Sem teto definido",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatMoney(spent, hideValues),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (hasBudget && spentInBudget > limit && filterMode == 0) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (filterMode == 0) {
                                        if (hasBudget) "$percent% do teto" else "${(ratio * 100).toInt()}% das despesas"
                                    } else {
                                        "$percent% do total gasto"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = barColor
                                )

                                // Ideia 2: Aviso de valor fora do teto dentro da categoria
                                if (filterMode == 0 && spentExcluded > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF64748B).copy(alpha = 0.15f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = "+ ${Formatters.formatMoney(spentExcluded, hideValues)} fora do teto",
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                } else if (filterMode == 1 && spentExcluded > 0) {
                                    Text(
                                        text = "(${Formatters.formatMoney(spentInBudget, hideValues)} teto + ${Formatters.formatMoney(spentExcluded, hideValues)} fora)",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Barra de Progresso visual
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(barColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (filterMode == 0) {
                                if (hasBudget) {
                                    if (remaining >= 0) {
                                        Text(
                                            text = "Resta: ${Formatters.formatMoney(remaining, hideValues)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = IncomeGreen
                                        )
                                    } else {
                                        Text(
                                            text = "⚠️ Estourou em ${Formatters.formatMoney(-remaining, hideValues)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Toque para ver gastos ou definir teto",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MobillsGreenPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                Text(
                                    text = if (hasBudget) "Teto definido: ${Formatters.formatMoney(limit, hideValues)}" else "Toque para ver detalhes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (onSetBudgetLimit != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MobillsGreenPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable {
                                        selectedCategoryForBudget = cat
                                        budgetInputTextFieldValue = Formatters.toCurrencyTextFieldValue(cat.budgetLimit)
                                    }
                                ) {
                                    Text(
                                        text = "✏️ Ajustar teto",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MobillsGreenPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE DETALHES DOS GASTOS DA CATEGORIA ---
    viewingCategoryDetails?.let { cat ->
        val catItems = items.filter {
            it.type == TransactionType.EXPENSE && it.category.equals(cat.name, ignoreCase = true)
        }.sortedByDescending { it.date }
        val catSpent = catItems.filter { it.includeInBudget }.sumOf { it.amount }
        val catSpentExcluded = catItems.filter { !it.includeInBudget }.sumOf { it.amount }
        val hasBudget = cat.budgetLimit > 0
        val limit = cat.budgetLimit
        val remaining = limit - catSpent
        val ratio = if (hasBudget) (catSpent / limit).toFloat() else 0f
        val percent = (ratio * 100).toInt()

        val barColor = when {
            !hasBudget -> Color(cat.colorHex)
            percent < 75 -> MobillsGreenPrimary
            percent < 100 -> Color(0xFFEAB308)
            else -> ExpenseRed
        }

        Dialog(onDismissRequest = { viewingCategoryDetails = null }) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Cabeçalho da Categoria
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(cat.colorHex).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat.iconEmoji, fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (hasBudget) "Teto: ${Formatters.formatMoney(limit, hideValues)}" else "Sem teto definido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { viewingCategoryDetails = null }) {
                            Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Card Resumo do Teto
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Gasto no Teto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = Formatters.formatMoney(catSpent, hideValues),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (hasBudget && catSpent > limit) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (catSpentExcluded > 0) {
                                        Text(
                                            text = "+ ${Formatters.formatMoney(catSpentExcluded, hideValues)} fora do teto",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "= ${Formatters.formatMoney(catSpent + catSpentExcluded, hideValues)} total geral",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MobillsGreenPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (hasBudget) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (remaining >= 0) "Restante do Teto" else "Estourou em",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (remaining >= 0) Formatters.formatMoney(remaining, hideValues) else Formatters.formatMoney(-remaining, hideValues),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (remaining >= 0) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }
                            }

                            if (hasBudget) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(ratio.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .background(barColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$percent% do teto mensal utilizado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = barColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            if (onSetBudgetLimit != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        val targetCat = cat
                                        viewingCategoryDetails = null
                                        selectedCategoryForBudget = targetCat
                                        budgetInputTextFieldValue = Formatters.toCurrencyTextFieldValue(targetCat.budgetLimit)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = if (hasBudget) "✏️ Alterar Teto (${Formatters.formatMoney(limit, hideValues)})" else "🎯 Definir Teto de Gastos",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Título da Lista
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lançamentos (${catItems.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Toque no item para editar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Lista de Gastos
                    if (catItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🛍️", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhum gasto nesta categoria",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Gastos com '${cat.name}' aparecerão aqui.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(catItems) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewingCategoryDetails = null
                                            onEditTransaction?.invoke(item)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (item.date.isNotBlank()) {
                                                    Text(
                                                        text = item.date,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = " • ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = item.accountOrCardName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "- ${Formatters.formatMoney(item.amount, hideValues)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (item.includeInBudget) ExpenseRed else Color(0xFF64748B)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                if (!item.includeInBudget) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFF64748B).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "FORA DO TETO",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF64748B),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (item.status == PaymentStatus.PAID) IncomeGreen.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (item.status == PaymentStatus.PAID) "PAGO" else "PENDENTE",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.status == PaymentStatus.PAID) IncomeGreen else Color(0xFFD97706),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { viewingCategoryDetails = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Fechar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // --- DIÁLOGO PARA DEFINIR / EDITAR O TETO DA CATEGORIA ---
    selectedCategoryForBudget?.let { cat ->
        var reserveInBudgetState by remember(cat) { mutableStateOf(cat.reserveInBudget) }

        Dialog(onDismissRequest = { selectedCategoryForBudget = null }) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${cat.iconEmoji} Definir Teto de Gastos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Defina quanto você planeja gastar por mês em '${cat.name}'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = budgetInputTextFieldValue,
                        onValueChange = { budgetInputTextFieldValue = Formatters.updateCurrencyInput(it, budgetInputTextFieldValue) },
                        label = { Text("Teto Mensal") },
                        prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = MobillsGreenPrimary) },
                        placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Atalhos rápidos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickAmounts = listOf(150.0, 250.0, 500.0, 700.0)
                        quickAmounts.forEach { amount ->
                            FilledTonalButton(
                                onClick = { budgetInputTextFieldValue = Formatters.toCurrencyTextFieldValue(amount) },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("${amount.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Opção de Reservar no Total de Despesas do Mês
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reserveInBudgetState = !reserveInBudgetState }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Reservar no Total de Despesas",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (reserveInBudgetState) {
                                        "O teto não gasto entra na soma das despesas e abate do saldo livre."
                                    } else {
                                        "Apenas visual/alerta. Não deduz do dinheiro livre para você."
                                    },
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = reserveInBudgetState,
                                onCheckedChange = { reserveInBudgetState = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Botão Principal: Salvar Teto
                    Button(
                        onClick = {
                            val limit = Formatters.parseCurrency(budgetInputText)
                            onSetBudgetLimit?.invoke(cat.id, limit, reserveInBudgetState)
                            selectedCategoryForBudget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Salvar Teto", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Linha inferior com opções secundárias (Remover / Cancelar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (cat.budgetLimit > 0) Arrangement.SpaceBetween else Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (cat.budgetLimit > 0) {
                            TextButton(
                                onClick = {
                                    onSetBudgetLimit?.invoke(cat.id, 0.0, false)
                                    selectedCategoryForBudget = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                            ) {
                                Text("🗑️ Remover Teto", fontSize = 13.sp)
                            }
                        }

                        TextButton(
                            onClick = { selectedCategoryForBudget = null }
                        ) {
                            Text("Cancelar", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
