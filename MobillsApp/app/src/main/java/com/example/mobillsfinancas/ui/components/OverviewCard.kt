package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun OverviewCard(
    totalIncome: Double,
    totalExpense: Double,
    remainingBalance: Double,
    previousMonthBalance: Double = 0.0,
    currentMonthIncome: Double = 0.0,
    unspentBudgets: Double = 0.0,
    items: List<TransactionItem> = emptyList(),
    hideValues: Boolean,
    onAdjustBalance: (() -> Unit)? = null
) {
    var showBreakdown by remember { mutableStateOf(false) }

    // Renda considerada: se o mês anterior já virou e somou, usa totalIncome; senão usa currentMonthIncome limpa
    val effectiveIncome = if (previousMonthBalance > 0.0) totalIncome else currentMonthIncome

    // Total comprometido = Despesas já lançadas no mês + Tetos reservados ainda não gastos (ex: R$ 700 do Mercado)
    val totalCommitted = totalExpense + unspentBudgets

    // Disponível Real para Gastar Comigo = Receita - Total Comprometido
    val freeToSpend = effectiveIncome - totalCommitted

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
                .padding(20.dp)
        ) {
            // TÍTULO E VALOR PRINCIPAL ÚNICO: DISPONÍVEL PARA GASTAR COMIGO
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎯", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sobrou para Gastar Comigo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (unspentBudgets > 0) {
                            "Já garantindo o teto de R$ ${Formatters.formatCurrencyPlain(unspentBudgets)} do mercado e contas fixas"
                        } else {
                            "Dinheiro livre para você gastar como quiser"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.formatMoney(freeToSpend, hideValues),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (freeToSpend >= 0) MobillsGreenPrimary else ExpenseRed
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (freeToSpend >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (freeToSpend >= 0) "POSITIVO" else "ATENÇÃO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (freeToSpend >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            if (previousMonthBalance > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💰 Saldo do mês anterior virado e somado:",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "+ " + Formatters.formatMoney(previousMonthBalance, hideValues),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra proporcional de Despesas vs Receitas
            val totalBase = maxOf(effectiveIncome, totalCommitted, 1.0)
            val expenseRatio = (totalCommitted / totalBase).toFloat().coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(IncomeGreen.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(expenseRatio)
                        .fillMaxHeight()
                        .background(ExpenseRed)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Linha com Receitas e Total Comprometido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Receitas (clicável para ajustar o Salário Base/Renda)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = onAdjustBalance != null) { onAdjustBalance?.invoke() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↓", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Receitas", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✏️", fontSize = 10.sp)
                        }
                        Text(
                            text = Formatters.formatMoney(effectiveIncome, hideValues),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Total Comprometido
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(ExpenseRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↑", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Total Despesas", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = Formatters.formatMoney(totalCommitted, hideValues),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }

                // Botão de Detalhes
                OutlinedButton(
                    onClick = { showBreakdown = !showBreakdown },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (showBreakdown) "Fechar ▲" else "Detalhes ▼",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary
                    )
                }
            }

            // GAVETA DE DETALHAMENTO TRANSPARENTE
            if (showBreakdown) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Como o seu dinheiro está planejado:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("💵 Receitas do mês", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+ " + Formatters.formatMoney(effectiveIncome, hideValues), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }

                val cofrinhoAmt = items.filter { it.type == TransactionType.TRANSFER && it.includeInBudget }.sumOf { it.amount }
                val fixasECartoes = totalExpense - cofrinhoAmt

                Spacer(modifier = Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📋 Contas fixas e faturas de cartão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("- " + Formatters.formatMoney(fixasECartoes, hideValues), fontSize = 11.sp, color = ExpenseRed)
                }

                if (cofrinhoAmt > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🐖 Cofrinho / Reserva (Guardado)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- " + Formatters.formatMoney(cofrinhoAmt, hideValues), fontSize = 11.sp, color = Color(0xFF0284C7))
                    }
                }

                if (unspentBudgets > 0) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🛒 Mercado / Tetos Reservados", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("- " + Formatters.formatMoney(unspentBudgets, hideValues), fontSize = 11.sp, color = Color(0xFFFF9800))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("📋 Total de Despesas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                    Text("- " + Formatters.formatMoney(totalCommitted, hideValues), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                }

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🎯 SOBROU LIVRE", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(Formatters.formatMoney(freeToSpend, hideValues), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (freeToSpend >= 0) MobillsGreenPrimary else ExpenseRed)
                }
            }
        }
    }
}

