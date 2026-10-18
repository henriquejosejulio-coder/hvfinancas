package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun TransferDialog(
    accounts: List<AccountItem>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Double, isScheduled: Boolean, date: String) -> Unit
) {
    if (accounts.size < 2) return

    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember {
        val targetIdx = accounts.indexOfFirst { !it.includeInOverallBalance }
        mutableIntStateOf(if (targetIdx != -1) targetIdx else if (accounts.size > 1) 1 else 0)
    }
    var amountText by remember { mutableStateOf("") }
    var isScheduled by remember { mutableStateOf(false) }
    var dateText by remember { mutableStateOf<String>(Formatters.todayDateString()) }

    val fromAcc = accounts.getOrNull(fromIndex) ?: accounts.first()
    val toAcc = accounts.getOrNull(toIndex) ?: accounts.last()
    val isToCofrinho = !toAcc.includeInOverallBalance

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⇄ Transferência entre Contas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Modalidade: Agora vs Agendado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !isScheduled,
                        onClick = { isScheduled = false },
                        label = { Text("⚡ Efetivar Já", fontSize = 11.sp, fontWeight = if (!isScheduled) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MobillsGreenPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = MobillsGreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isScheduled,
                        onClick = { isScheduled = true },
                        label = { Text("🕒 Agendar / Reservar", fontSize = 11.sp, fontWeight = if (isScheduled) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f),
                            selectedLabelColor = Color(0xFFE65100)
                        ),
                        modifier = Modifier.weight(1.2f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "De (Conta de Origem):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEachIndexed { idx, acc ->
                        val isSel = fromIndex == idx
                        FilterChip(
                            selected = isSel,
                            onClick = { fromIndex = idx },
                            label = {
                                Text(
                                    text = "${acc.name} (${Formatters.formatCurrencyPlain(acc.balance)})",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(acc.colorHex).copy(alpha = 0.2f),
                                selectedLabelColor = Color(acc.colorHex)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Para (Conta de Destino / Cofrinho):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accounts.forEachIndexed { idx, acc ->
                        val isSel = toIndex == idx
                        val isCofrinho = !acc.includeInOverallBalance
                        FilterChip(
                            selected = isSel,
                            onClick = { toIndex = idx },
                            label = {
                                Text(
                                    text = if (isCofrinho) "🐖 ${acc.name}" else acc.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isCofrinho) Color(0xFF0284C7).copy(alpha = 0.2f) else Color(acc.colorHex).copy(alpha = 0.2f),
                                selectedLabelColor = if (isCofrinho) Color(0xFF0369A1) else Color(acc.colorHex)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Formatters.formatCurrencyInput(it) },
                    label = { Text("Valor a Transferir (R$)") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (isScheduled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Data Prevista (DD/MM/AAAA)") },
                        placeholder = { Text("Ex: 20/10/2026") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (isToCofrinho) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE0F2FE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🐖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScheduled) {
                                    "Dinheiro Carimbado: Este valor será reservado do seu saldo livre e descontará do total para gastar no mês. Ficará no extrato como pendente até você tocar para concluir."
                                } else {
                                    "Aporte no Cofrinho: O valor sairá do seu saldo livre e passará a render no cofrinho."
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = Formatters.parseCurrency(amountText)
                            if (amt > 0 && fromIndex != toIndex) {
                                onTransfer(fromAcc.id, toAcc.id, amt, isScheduled, dateText.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScheduled) Color(0xFFE65100) else MobillsGreenPrimary
                        )
                    ) {
                        Text(if (isScheduled) "Agendar" else "Transferir", color = Color.White)
                    }
                }
            }
        }
    }
}
