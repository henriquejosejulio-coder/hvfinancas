package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.utils.Formatters

import com.example.mobillsfinancas.theme.MobillsGreenPrimary

@Composable
fun TransactionItemView(
    item: TransactionItem,
    hideValues: Boolean,
    onEdit: (TransactionItem) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDelete: (String) -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: ((String) -> Unit)? = null
) {
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable {
                if (isSelectionMode) {
                    onToggleSelect?.invoke(item.id)
                } else {
                    onEdit(item)
                }
            },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MobillsGreenPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox quando em modo de seleção em massa
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect?.invoke(item.id) },
                        colors = CheckboxDefaults.colors(checkedColor = MobillsGreenPrimary),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }

                // Ícone redondo colorido da categoria estilo Mobills
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(item.categoryColorHex).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.categoryIcon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Descrição, Categoria e Conta/Cartão
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isRecurring) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MobillsGreenPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "📌 FIXA",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                            }
                        } else if (item.totalInstallments > 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${item.currentInstallment}/${item.totalInstallments}",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        if (!item.includeInBudget) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = Color(0xFF64748B).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "FORA DO TETO",
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                    val payTypeText = when (item.paymentType) {
                        PaymentType.CREDIT_CARD -> "💳 Crédito"
                        PaymentType.DEBIT_CARD -> "🏦 Débito"
                        PaymentType.PIX -> "⚡ Pix"
                        PaymentType.CASH -> "💵 Dinheiro"
                        PaymentType.INVESTMENT -> "📈 Aplicação"
                    }
                    val displayDate = if (item.date.isNotBlank()) {
                        item.date.replace("Vencimento: ", "Venc: ").trim()
                    } else {
                        item.monthYear
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (displayDate.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    text = "📅 $displayDate",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = "${item.category} • ${item.accountOrCardName} ($payTypeText)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Valor e Status
                Column(horizontalAlignment = Alignment.End) {
                    val prefix = if (item.type == TransactionType.INCOME) "+ " else "- "
                    val valueColor = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed

                    Text(
                        text = prefix + Formatters.formatMoney(item.amount, hideValues),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = valueColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val (statusText, statusBg, statusColor) = when (item.status) {
                        PaymentStatus.PAID -> if (item.type == TransactionType.INCOME) {
                            Triple("RECEBIDO", IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
                        } else {
                            Triple("PAGO", IncomeGreen.copy(alpha = 0.15f), IncomeGreen)
                        }
                        PaymentStatus.PENDING -> if (item.type == TransactionType.INCOME) {
                            Triple("A RECEBER", Color(0xFF0288D1).copy(alpha = 0.15f), Color(0xFF0288D1))
                        } else {
                            Triple("PENDENTE", ExpenseRed.copy(alpha = 0.15f), ExpenseRed)
                        }
                        PaymentStatus.TO_RECEIVE -> Triple("A RECEBER", Color(0xFF0288D1).copy(alpha = 0.15f), Color(0xFF0288D1))
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusBg,
                        modifier = Modifier.clickable { onToggleStatus(item.id) }
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                if (!isSelectionMode) {
                    IconButton(
                        onClick = { onEdit(item) },
                        modifier = Modifier.size(28.dp).padding(start = 4.dp)
                    ) {
                        Text("✏️", fontSize = 14.sp)
                    }

                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(28.dp).padding(start = 2.dp)
                    ) {
                        Text("×", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }

            // Tags (#) do item, se houver
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 54.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.tags.forEach { tag ->
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Deseja excluir este lançamento?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tem certeza de que deseja excluir o lançamento abaixo?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.description,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val prefix = if (item.type == TransactionType.INCOME) "+ " else "- "
                            val valueColor = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                            val confirmDate = if (item.date.isNotBlank()) item.date.replace("Vencimento: ", "Venc: ").trim() else item.monthYear
                            Text(
                                text = "$prefix${Formatters.formatMoney(item.amount, hideValues)} • ${item.category} • 📅 $confirmDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = valueColor,
                                fontWeight = FontWeight.Bold
                            )
                            if (item.totalInstallments > 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Parcela ${item.currentInstallment} de ${item.totalInstallments}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Esta ação não pode ser desfeita.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(item.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Sim, Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Não, Cancelar")
                }
            }
        )
    }
}
