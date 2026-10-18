package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountsSection(
    accounts: List<AccountItem>,
    hideValues: Boolean,
    onEditAccount: (AccountItem) -> Unit,
    onAddAccount: () -> Unit,
    onTransfer: () -> Unit,
    onAdjustBalance: (() -> Unit)? = null,
    onReorderAccounts: ((List<AccountItem>) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var quickReorderAccount by remember { mutableStateOf<AccountItem?>(null) }
    var showFullReorderDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bancos & Contas (Controle de Saldo)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                val availableTotal = accounts.filter { it.includeInOverallBalance }.sumOf { it.balance }
                val reservesTotal = accounts.filter { !it.includeInOverallBalance }.sumOf { it.balance }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Saldo Livre: ${Formatters.formatMoney(availableTotal, hideValues)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MobillsGreenPrimary
                    )
                    if (reservesTotal > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• 🐖 Cofrinho: ${Formatters.formatMoney(reservesTotal, hideValues)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            if (accounts.size > 1 && onReorderAccounts != null) {
                TextButton(
                    onClick = { showFullReorderDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "⇅ Ordenar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary
                    )
                }
            }
        }

        // Barra de Ações Rápidas de Bancos (Transferência e Ajuste)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTransfer,
                modifier = Modifier.weight(1.2f),
                colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
            ) {
                Text("⇄ Transferência entre Contas", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }

            if (onAdjustBalance != null) {
                OutlinedButton(
                    onClick = onAdjustBalance,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⚖️ Acertar Saldo", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(accounts, key = { it.id }) { acc ->
                Card(
                    modifier = Modifier
                        .width(190.dp)
                        .combinedClickable(
                            onClick = { onEditAccount(acc) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                quickReorderAccount = acc
                            }
                        ),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(acc.colorHex),
                                    maxLines = 1
                                )
                                if (!acc.includeInOverallBalance) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "🐖 Reserva / Cofrinho",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0369A1)
                                        )
                                    }
                                } else if (acc.isDefault) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "⭐ Conta Padrão",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                            }
                            Text("✏️", fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Saldo atual:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatMoney(acc.balance, hideValues),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (acc.balance >= 0) MaterialTheme.colorScheme.onSurface else Color.Red
                        )
                    }
                }
            }

            item {
                OutlinedCard(
                    onClick = onAddAccount,
                    modifier = Modifier.width(130.dp).height(80.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("+", fontSize = 22.sp, color = MobillsGreenPrimary, fontWeight = FontWeight.Bold)
                        Text("Novo Banco", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal Rápido de Ordenação ao "Segurar" uma Conta
    quickReorderAccount?.let { targetAcc ->
        val currentIndex = accounts.indexOfFirst { it.id == targetAcc.id }
        if (currentIndex != -1) {
            Dialog(onDismissRequest = { quickReorderAccount = null }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(targetAcc.colorHex))
                            )
                            Text(
                                text = targetAcc.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Posição atual: ${currentIndex + 1}º de ${accounts.size} contas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botão Principal: PASSAR PRA FRENTE (1º Lugar)
                        Button(
                            onClick = {
                                if (currentIndex > 0) {
                                    val list = accounts.toMutableList()
                                    val item = list.removeAt(currentIndex)
                                    list.add(0, item)
                                    onReorderAccounts?.invoke(list)
                                }
                                quickReorderAccount = null
                            },
                            enabled = currentIndex > 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text(
                                text = if (currentIndex == 0) "⭐ Já está em 1º lugar (Na frente)" else "🚀 Passar para a Frente (1º Lugar)",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botões Mover Esquerda / Direita
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        val list = accounts.toMutableList()
                                        val item = list.removeAt(currentIndex)
                                        list.add(currentIndex - 1, item)
                                        onReorderAccounts?.invoke(list)
                                    }
                                    quickReorderAccount = null
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("◀ Esquerda")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (currentIndex < accounts.size - 1) {
                                        val list = accounts.toMutableList()
                                        val item = list.removeAt(currentIndex)
                                        list.add(currentIndex + 1, item)
                                        onReorderAccounts?.invoke(list)
                                    }
                                    quickReorderAccount = null
                                },
                                enabled = currentIndex < accounts.size - 1,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Direita ▶")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mover para o final
                        OutlinedButton(
                            onClick = {
                                if (currentIndex < accounts.size - 1) {
                                    val list = accounts.toMutableList()
                                    val item = list.removeAt(currentIndex)
                                    list.add(item)
                                    onReorderAccounts?.invoke(list)
                                }
                                quickReorderAccount = null
                            },
                            enabled = currentIndex < accounts.size - 1,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🏁 Mover para o Final")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                quickReorderAccount = null
                                showFullReorderDialog = true
                            }) {
                                Text("⇅ Ver Todas", color = MobillsGreenPrimary)
                            }

                            TextButton(onClick = { quickReorderAccount = null }) {
                                Text("Fechar")
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo Completo de Reordenação de Contas
    if (showFullReorderDialog) {
        ReorderAccountsDialog(
            accounts = accounts,
            onDismiss = { showFullReorderDialog = false },
            onSaveOrder = { updated ->
                onReorderAccounts?.invoke(updated)
                showFullReorderDialog = false
            }
        )
    }
}

@Composable
fun ReorderAccountsDialog(
    accounts: List<AccountItem>,
    onDismiss: () -> Unit,
    onSaveOrder: (List<AccountItem>) -> Unit
) {
    var list by remember { mutableStateOf(accounts) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⇅ Organizar Ordem das Contas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Defina a ordem em que aparecem no carrossel:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(list, key = { _, acc -> acc.id }) { index, acc ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MobillsGreenPrimary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}º",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MobillsGreenPrimary
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(acc.colorHex))
                                    )
                                    Text(
                                        text = acc.name,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (index > 0) {
                                        IconButton(
                                            onClick = {
                                                val mutable = list.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(0, item)
                                                list = mutable
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("🚀", fontSize = 14.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                val mutable = list.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index - 1, item)
                                                list = mutable
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (index < list.size - 1) {
                                        IconButton(
                                            onClick = {
                                                val mutable = list.toMutableList()
                                                val item = mutable.removeAt(index)
                                                mutable.add(index + 1, item)
                                                list = mutable
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveOrder(list) },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                    ) {
                        Text("Salvar Ordem", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
