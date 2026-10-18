package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.data.PaymentType
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.data.TransactionType
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun QuickInstallmentCard(
    item: TransactionItem,
    onSelectInstallments: (Int) -> Unit,
    onDismiss: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Rich slate dark
        border = BorderStroke(1.5.dp, MobillsGreenPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Badge + Botão fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isIncome = (item.type == TransactionType.INCOME)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(if (isIncome) Color(0xFF00E676).copy(alpha = 0.2f) else MobillsGreenPrimary.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isIncome) "💵" else "⚡", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isIncome) "RECEITA CAPTURADA AUTOMATICAMENTE" else "COMPRA CAPTURADA AUTOMATICAMENTE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isIncome) Color(0xFF00E676) else MobillsGreenPrimary,
                        letterSpacing = 0.8.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Text(
                        text = "✕",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Detalhes da Compra / Receita
            val isIncome = (item.type == TransactionType.INCOME)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${item.accountOrCardName} • ${item.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                val totalAmount = if (item.totalInstallments > 1) item.amount * item.totalInstallments else item.amount
                Text(
                    text = (if (isIncome) "+ " else "") + Formatters.formatMoney(totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isIncome) Color(0xFF00E676) else Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val isCredit = (item.paymentType == PaymentType.CREDIT_CARD)
            Text(
                text = when {
                    isCredit -> "Deseja parcelar em quantas vezes? (1 clique)"
                    isIncome -> "Tipo de Recebimento no Orçamento:"
                    else -> "Forma confirmada no extrato:"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isCredit) {
                val totalAmount = if (item.totalInstallments > 1) item.amount * item.totalInstallments else item.amount
                val options = listOf(1, 2, 3, 4, 5, 6, 10, 12)

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(options) { inst ->
                        val isSelected = (item.totalInstallments == inst)
                        val parcelAmount = totalAmount / inst
                        val subtitle = if (inst > 1) Formatters.formatMoney(parcelAmount) else "À vista"

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectInstallments(inst) },
                            label = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${inst}x",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = subtitle,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else Color(0xFF94A3B8)
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MobillsGreenPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MobillsGreenPrimary else Color(0xFF334155),
                                selectedBorderColor = MobillsGreenPrimary
                            )
                        )
                    }

                    // Botão Outro / Mais detalhes
                    item {
                        FilterChip(
                            selected = false,
                            onClick = onOpenDetails,
                            label = {
                                Text(
                                    text = "✏️ Outro...",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFF38BDF8)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = Color(0xFF0284C7)
                            )
                        )
                    }
                }
            } else if (isIncome) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (item.isSalaryReplacement)
                                "💼 Salário do Mês (substitui a média de R$ 3.300)"
                            else
                                "➕ Receita Extra Avulsa",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = "Toque em 'Editar' para alternar ou ajustar o valor",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Row {
                        TextButton(onClick = onOpenDetails) {
                            Text("Editar ✏️", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("Confirmar ✓", color = Color(0xFF00E676), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓ Registrado como Pago no débito/saldo da conta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MobillsGreenPrimary
                    )
                    TextButton(onClick = onDismiss) {
                        Text("OK", color = MobillsGreenPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
