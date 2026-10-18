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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.*
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun CardsCarousel(
    cards: List<CreditCardItem>,
    currentMonth: String,
    currentMonthData: com.example.mobillsfinancas.data.MonthData?,
    accounts: List<AccountItem> = emptyList(),
    hideValues: Boolean,
    onEditCard: (CreditCardItem) -> Unit,
    onAddCard: () -> Unit,
    onReorderCards: ((List<CreditCardItem>) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    var quickReorderCard by remember { mutableStateOf<CreditCardItem?>(null) }
    var showFullReorderDialog by remember { mutableStateOf(false) }

    val items = currentMonthData?.items ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Meus Cartões de Crédito",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (currentMonth.isNotEmpty()) {
                    Text(
                        text = "Faturas de $currentMonth",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (cards.size > 1 && onReorderCards != null) {
                    TextButton(
                        onClick = { showFullReorderDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "⇅ Ordenar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MobillsGreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TextButton(
                    onClick = onAddCard,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+ Adicionar Cartão",
                        style = MaterialTheme.typography.bodySmall,
                        color = MobillsGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards, key = { "${it.id}_$currentMonth" }) { card ->
                val invoice = items.filter { isTransactionForCard(it, card, accounts) }.sumOf { it.amount }

                CreditCardView(
                    card = card,
                    invoiceAmount = invoice,
                    currentMonth = currentMonth,
                    accounts = accounts,
                    hideValues = hideValues,
                    onEdit = { onEditCard(card) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        quickReorderCard = card
                    }
                )
            }

            item {
                // Card pontilhado para adicionar novo cartão
                OutlinedCard(
                    onClick = onAddCard,
                    modifier = Modifier
                        .width(140.dp)
                        .height(150.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("+", fontSize = 28.sp, color = MobillsGreenPrimary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Novo Cartão", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Modal Rápido de Ordenação ao "Segurar" um Cartão
    quickReorderCard?.let { targetCard ->
        val currentIndex = cards.indexOfFirst { it.id == targetCard.id }
        if (currentIndex != -1) {
            Dialog(onDismissRequest = { quickReorderCard = null }) {
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
                                    .background(Color(targetCard.colorHex))
                            )
                            Text(
                                text = targetCard.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(targetCard.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${targetCard.cardType.icon} ${targetCard.cardType.label}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(targetCard.colorHex)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Posição atual: ${currentIndex + 1}º de ${cards.size} cartões",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Botão Principal: PASSAR PRA FRENTE (1º Lugar)
                        Button(
                            onClick = {
                                if (currentIndex > 0) {
                                    val list = cards.toMutableList()
                                    val item = list.removeAt(currentIndex)
                                    list.add(0, item)
                                    onReorderCards?.invoke(list)
                                }
                                quickReorderCard = null
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
                                        val list = cards.toMutableList()
                                        val item = list.removeAt(currentIndex)
                                        list.add(currentIndex - 1, item)
                                        onReorderCards?.invoke(list)
                                    }
                                    quickReorderCard = null
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("◀ Esquerda")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (currentIndex < cards.size - 1) {
                                        val list = cards.toMutableList()
                                        val item = list.removeAt(currentIndex)
                                        list.add(currentIndex + 1, item)
                                        onReorderCards?.invoke(list)
                                    }
                                    quickReorderCard = null
                                },
                                enabled = currentIndex < cards.size - 1,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Direita ▶")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mover para o final
                        OutlinedButton(
                            onClick = {
                                if (currentIndex < cards.size - 1) {
                                    val list = cards.toMutableList()
                                    val item = list.removeAt(currentIndex)
                                    list.add(item)
                                    onReorderCards?.invoke(list)
                                }
                                quickReorderCard = null
                            },
                            enabled = currentIndex < cards.size - 1,
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
                                quickReorderCard = null
                                showFullReorderDialog = true
                            }) {
                                Text("⇅ Ver Todos", color = MobillsGreenPrimary)
                            }

                            TextButton(onClick = { quickReorderCard = null }) {
                                Text("Fechar")
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo Completo de Reordenação de Cartões
    if (showFullReorderDialog) {
        ReorderCardsDialog(
            cards = cards,
            onDismiss = { showFullReorderDialog = false },
            onSaveOrder = { updated ->
                onReorderCards?.invoke(updated)
                showFullReorderDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CreditCardView(
    card: CreditCardItem,
    invoiceAmount: Double,
    currentMonth: String = "",
    accounts: List<AccountItem> = emptyList(),
    hideValues: Boolean,
    onEdit: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val cardColor = Color(card.colorHex)
    val gradient = Brush.linearGradient(
        colors = listOf(
            cardColor,
            cardColor.copy(alpha = 0.8f)
        )
    )

    Card(
        modifier = Modifier
            .width(260.dp)
            .height(150.dp)
            .combinedClickable(
                onClick = onEdit,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header do Cartão com badge de tipo e botão de editar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        val linked = accounts.find { it.id == card.linkedAccountId }
                        if (linked != null) {
                            Text(
                                text = "🔗 ${linked.bankIcon} ${linked.name}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.32f)
                        ) {
                            Text(
                                text = "${card.cardType.icon} ${card.cardType.badgeText}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = Color.Black.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "✏️",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Fatura Atual ou Gastos no Débito
                Column {
                    Text(
                        text = if (!card.cardType.allowsCredit) {
                            if (currentMonth.isNotEmpty()) "Gastos no Débito • $currentMonth" else "Gastos no Débito"
                        } else {
                            if (currentMonth.isNotEmpty()) "Fatura • $currentMonth" else "Fatura Aberta"
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = Formatters.formatMoney(invoiceAmount, hideValues),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }

                // Limite e Datas (Fechamento / Vencimento) ou Débito em Conta
                Column {
                    if (card.cardType.allowsCredit) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val availableLimit = (card.limit - invoiceAmount).coerceAtLeast(0.0)
                            Text(
                                text = "Limite: ${Formatters.formatMoney(card.limit, hideValues)}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Disp: ${Formatters.formatMoney(availableLimit, hideValues)}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fechamento: dia ${card.closingDay} • Vencimento: dia ${card.dueDay}",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp
                        )
                    } else {
                        val linked = accounts.find { it.id == card.linkedAccountId }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (linked != null) "Conta: ${linked.name}" else "Débito direto",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                            if (linked != null) {
                                Text(
                                    text = "Saldo: ${Formatters.formatMoney(linked.balance, hideValues)}",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Débito em conta corrente",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderCardsDialog(
    cards: List<CreditCardItem>,
    onDismiss: () -> Unit,
    onSaveOrder: (List<CreditCardItem>) -> Unit
) {
    var list by remember { mutableStateOf(cards) }

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
                    text = "⇅ Organizar Ordem dos Cartões",
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
                    itemsIndexed(list, key = { _, card -> card.id }) { index, card ->
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
                                            .background(Color(card.colorHex))
                                    )
                                    Column {
                                        Text(
                                            text = card.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${card.cardType.icon} ${card.cardType.label}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
