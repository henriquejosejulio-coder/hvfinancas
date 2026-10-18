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
    hideValues: Boolean,
    onAdjustBalance: (() -> Unit)? = null
) {
    var showOnlyCurrentMonth by remember { mutableStateOf(true) }

    // Renda considerada: do mês limpo (padrão) ou somada com o mês anterior
    val effectiveIncome = if (showOnlyCurrentMonth && previousMonthBalance != 0.0) currentMonthIncome else totalIncome

    // Livre para Gastar Comigo = Renda Efetiva - Total de Despesas do Mês - Tetos Reservados Não Gastos (ex: R$ 700 Mercado)
    val freeToSpend = effectiveIncome - totalExpense - unspentBudgets

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
            // SEÇÃO EM DESTAQUE: LIVRE PARA MIM (DISPONÍVEL REAL)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MobillsGreenPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
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
                                    text = "Livre para Mim (Disponível Real)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (unspentBudgets > 0) {
                                    "Já descontando R$ ${Formatters.formatCurrencyPlain(unspentBudgets)} do teto de mercado e contas fixas"
                                } else {
                                    "Dinheiro livre para gastar à vontade"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = Formatters.formatMoney(freeToSpend, hideValues),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (freeToSpend >= 0) MobillsGreenPrimary else ExpenseRed
                        )
                    }

                    if (previousMonthBalance != 0.0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showOnlyCurrentMonth) "Mostrando: Receita do mês (sem anterior)" else "Mostrando: Com saldo anterior somado",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MobillsGreenPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { showOnlyCurrentMonth = !showOnlyCurrentMonth }
                            ) {
                                Text(
                                    text = if (showOnlyCurrentMonth) {
                                        "Ver com anterior (+${Formatters.formatCurrencyPlain(previousMonthBalance)})"
                                    } else {
                                        "Ver só mês limpo"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Saldo / Sobrou do Mês
            Row(

                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable(enabled = onAdjustBalance != null) { onAdjustBalance?.invoke() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Saldo Final (Sobrou)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onAdjustBalance != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("✏️", fontSize = 12.sp)
                        }
                    }
                    Text(
                        text = Formatters.formatMoney(remainingBalance, hideValues),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (remainingBalance >= 0) MobillsGreenPrimary else ExpenseRed
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onAdjustBalance != null) {
                        FilledTonalButton(
                            onClick = onAdjustBalance,
                            shape = MaterialTheme.shapes.small,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("⚖️ Acertar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (remainingBalance >= 0) IncomeGreen.copy(alpha = 0.15f) else ExpenseRed.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (remainingBalance >= 0) "POSITIVO" else "ATENÇÃO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            if (previousMonthBalance != 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💰 Saldo do mês anterior somado:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = (if (previousMonthBalance >= 0) "+ " else "- ") + Formatters.formatMoney(kotlin.math.abs(previousMonthBalance), hideValues),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (previousMonthBalance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra proporcional de Despesa vs Receita
            val total = (totalIncome + totalExpense).coerceAtLeast(1.0)
            val expenseRatio = (totalExpense / total).toFloat().coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(IncomeGreen.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(expenseRatio)
                        .fillMaxHeight()
                        .background(ExpenseRed)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(12.dp))

            // Linha com Receitas e Despesas lado a lado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Receitas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(IncomeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↓", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (previousMonthBalance != 0.0) "Total Disp." else "Receitas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatMoney(totalIncome, hideValues),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }
                }

                // Despesas
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(ExpenseRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↑", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Despesas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatMoney(totalExpense, hideValues),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}
