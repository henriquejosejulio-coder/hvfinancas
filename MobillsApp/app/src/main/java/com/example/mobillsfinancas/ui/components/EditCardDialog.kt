package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.CardType
import com.example.mobillsfinancas.data.CreditCardItem
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.BankRegistry
import com.example.mobillsfinancas.utils.Formatters

private val PRESET_CARD_COLORS = listOf(
    0xFFFF7A00L, // Laranja Inter / PagBank
    0xFF8A05BEL, // Roxo Nubank
    0xFF009EE3L, // Azul Mercado Pago
    0xFFFFD500L, // Amarelo Will / BB
    0xFF0047BAL, // Azul Sofisa / Caixa
    0xFF00A868L, // Verde Mobills / PicPay / Sicredi
    0xFFCC0000L, // Vermelho Santander / Bradesco
    0xFF1F2937L, // Preto / Grafite Black / Ultravioleta
    0xFF0038A8L, // Azul Escuro Itaú
    0xFFE91E63L, // Rosa Neon / Pan
    0xFF7C3AEDL, // Violeta
    0xFF059669L, // Verde Petróleo
    0xFF0284C7L, // Azul Turquesa / C6
    0xFFB45309L  // Dourado / Cobre
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardDialog(
    card: CreditCardItem?, // null if adding new card
    accounts: List<AccountItem> = emptyList(),
    existingCards: List<CreditCardItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CreditCardItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(card?.name ?: "") }
    var selectedCardType by remember { mutableStateOf(card?.cardType ?: CardType.CREDIT) }
    var limitText by remember { mutableStateOf(if (card != null && card.limit > 0) Formatters.formatCurrencyPlain(card.limit) else "") }
    var closingDayText by remember { mutableStateOf(card?.closingDay?.toString() ?: "10") }
    var dueDayText by remember { mutableStateOf(card?.dueDay?.toString() ?: "17") }
    var selectedAccountId by remember { mutableStateOf(card?.linkedAccountId ?: "") }
    var selectedColorHex by remember { mutableStateOf(card?.colorHex ?: 0xFFFF7A00L) }

    val hasDuplicateName = remember(name, card?.id, existingCards) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) false
        else existingCards.any { it.id != card?.id && it.name.trim().equals(trimmed, ignoreCase = true) }
    }

    val cardPalette = remember(card?.colorHex) {
        val current = card?.colorHex
        if (current != null && current !in PRESET_CARD_COLORS) {
            listOf(current) + PRESET_CARD_COLORS
        } else {
            PRESET_CARD_COLORS
        }
    }

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
                    text = if (card == null) "Adicionar Novo Cartão" else "Editar Cartão",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Prévia Visual do Cartão em Tempo Real
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(selectedColorHex),
                                        Color(selectedColorHex).copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name.ifBlank { "Nome do Cartão" },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = "${selectedCardType.icon} ${selectedCardType.badgeText}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = if (selectedCardType == CardType.DEBIT || selectedCardType == CardType.VIRTUAL_DEBIT) {
                                        "Débito em Conta"
                                    } else {
                                        "Limite: R$ ${limitText.ifBlank { "0,00" }}"
                                    },
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (selectedCardType.isVirtual) "🌐" else "💳",
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seletor de Tipo / Modalidade do Cartão
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🏷️ Modalidade do Cartão",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CardType.values().forEach { type ->
                            val isSelected = selectedCardType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCardType = type },
                                label = {
                                    Text(
                                        text = "${type.icon} ${type.label}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MobillsGreenPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = MobillsGreenPrimary
                                ),
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, MobillsGreenPrimary)
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sugestões de Cartões Populares (Preenchimento Rápido com 1 Toque)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "💳 Sugestão de Cartões (Toque para preencher):",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BankRegistry.ALL_BANKS.forEach { b ->
                            val isSelected = name.equals(b.cardName, ignoreCase = true) || name.equals(b.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    name = b.cardName
                                    selectedColorHex = b.colorHex
                                    val matchedAcc = accounts.find {
                                        it.name.contains(b.name, ignoreCase = true) || it.name.contains(b.key, ignoreCase = true)
                                    }
                                    if (matchedAcc != null) {
                                        selectedAccountId = matchedAcc.id
                                    }
                                },
                                label = { Text("${b.icon} ${b.cardName}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(b.colorHex).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(b.colorHex)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Cartão") },
                    placeholder = { Text("Ex: Cartão Inter, Cartão Will") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (hasDuplicateName) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ExpenseRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚠️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Já existe outro cartão com este nome. Recomendamos diferenciar (ex: '${name.trim()} 1' e '${name.trim()} 2' ou indicar Débito/Crédito) para não misturar faturas.",
                                style = MaterialTheme.typography.labelSmall,
                                color = ExpenseRed
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seletor de Conta Vinculada com Chips e Destaque
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔗 Vincular à Conta Bancária",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val currentAcc = accounts.find { it.id == selectedAccountId }
                        if (currentAcc != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(currentAcc.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${currentAcc.bankIcon} ${currentAcc.name}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(currentAcc.colorHex)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Selecione o banco que paga a fatura deste cartão:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (accounts.isEmpty()) {
                        Text(
                            text = "Nenhuma conta bancária cadastrada ainda.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = selectedAccountId.isEmpty(),
                                onClick = { selectedAccountId = "" },
                                label = { Text("❌ Sem Vínculo", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            accounts.forEach { acc ->
                                val isSelected = selectedAccountId == acc.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAccountId = acc.id },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(acc.colorHex))
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = "${acc.bankIcon} ${acc.name}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(acc.colorHex).copy(alpha = 0.2f),
                                        selectedLabelColor = Color(acc.colorHex)
                                    ),
                                    border = if (isSelected) {
                                        BorderStroke(1.5.dp, Color(acc.colorHex))
                                    } else null
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = Formatters.formatCurrencyInput(it) },
                    label = {
                        Text(
                            if (selectedCardType.allowsCredit) "Limite Total do Cartão (R$)"
                            else "Limite Diário / Transação (R$) (Opcional)"
                        )
                    },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedCardType.allowsCredit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = closingDayText,
                            onValueChange = { closingDayText = it },
                            label = { Text("Dia Fechamento") },
                            placeholder = { Text("Ex: 6") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dueDayText,
                            onValueChange = { dueDayText = it },
                            label = { Text("Dia Vencimento") },
                            placeholder = { Text("Ex: 15") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("ℹ️", fontSize = 16.sp)
                            Text(
                                text = "Cartões de débito debitam diretamente da conta bancária vinculada, sem fatura mensal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seletor de Cor do Cartão
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎨 Cor do Cartão",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color(selectedColorHex))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Escolha a cor do gradiente do seu cartão:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        cardPalette.forEach { hex ->
                            val isSelected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                        }
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = if (hex == 0xFFFFD500L) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (card != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(card.id) },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                        ) {
                            Text("Excluir")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsedLim = Formatters.parseCurrency(limitText)
                                val lim = if (parsedLim > 0) parsedLim else 2000.0
                                val cDay = closingDayText.toIntOrNull()?.coerceIn(1, 31) ?: 10
                                val dDay = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 17
                                if (name.isNotBlank()) {
                                    val item = card?.copy(
                                        name = name.trim(),
                                        limit = lim,
                                        closingDay = cDay,
                                        dueDay = dDay,
                                        colorHex = selectedColorHex,
                                        linkedAccountId = selectedAccountId,
                                        cardType = selectedCardType
                                    ) ?: CreditCardItem(
                                        id = "card_" + System.currentTimeMillis(),
                                        name = name.trim(),
                                        limit = lim,
                                        closingDay = cDay,
                                        dueDay = dDay,
                                        colorHex = selectedColorHex,
                                        bankName = name.trim(),
                                        linkedAccountId = selectedAccountId,
                                        cardType = selectedCardType
                                    )
                                    onSave(item)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("Salvar", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
