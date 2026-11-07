package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.PaymentStatus
import com.example.mobillsfinancas.data.TransactionItem
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters


@Composable
fun TransferDialog(
    accounts: List<AccountItem>,
    existingTransfer: TransactionItem? = null,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Double, isScheduled: Boolean, date: String, isRecurring: Boolean) -> Unit,
    onUpdateTransfer: ((existing: TransactionItem, fromId: String, toId: String, amount: Double, isScheduled: Boolean, date: String, isRecurring: Boolean) -> Unit)? = null,
    onDeleteTransfer: ((TransactionItem) -> Unit)? = null
) {
    if (accounts.size < 2) return

    val initialFromIndex = remember(existingTransfer, accounts) {
        if (existingTransfer != null) {
            val fromId = extractNoteParam(existingTransfer.note, "fromAccountId")
            val byId = accounts.indexOfFirst { it.id == fromId }
            if (byId != -1) return@remember byId

            val arrow = if (existingTransfer.description.contains("➔")) "➔" else if (existingTransfer.description.contains("->")) "->" else null
            if (arrow != null) {
                val rawFrom = existingTransfer.description.split(arrow)[0]
                    .replace("Agendado:", "")
                    .replace("Transferência:", "")
                    .replace("Transferencia:", "")
                    .trim()
                val byName = accounts.indexOfFirst { it.name.equals(rawFrom, ignoreCase = true) || rawFrom.contains(it.name, ignoreCase = true) }
                if (byName != -1) return@remember byName
            }
            val byAccOrCard = accounts.indexOfFirst { it.name.equals(existingTransfer.accountOrCardName, ignoreCase = true) }
            if (byAccOrCard != -1) return@remember byAccOrCard
        }
        0
    }

    val initialToIndex = remember(existingTransfer, accounts) {
        if (existingTransfer != null) {
            val toId = extractNoteParam(existingTransfer.note, "toAccountId")
            val byId = accounts.indexOfFirst { it.id == toId }
            if (byId != -1) return@remember byId

            val arrow = if (existingTransfer.description.contains("➔")) "➔" else if (existingTransfer.description.contains("->")) "->" else null
            if (arrow != null) {
                val parts = existingTransfer.description.split(arrow)
                if (parts.size >= 2) {
                    val rawTo = parts[1].trim()
                    val byName = accounts.indexOfFirst { it.name.equals(rawTo, ignoreCase = true) || rawTo.contains(it.name, ignoreCase = true) }
                    if (byName != -1) return@remember byName
                }
            }
        }
        val targetIdx = accounts.indexOfFirst { !it.includeInOverallBalance }
        if (targetIdx != -1) targetIdx else if (accounts.size > 1) 1 else 0
    }

    var fromIndex by remember(existingTransfer?.id) { mutableIntStateOf(initialFromIndex) }
    var toIndex by remember(existingTransfer?.id) { mutableIntStateOf(initialToIndex) }
    var amountTextFieldValue by remember(existingTransfer?.id) {
        mutableStateOf(
            if (existingTransfer != null && existingTransfer.amount > 0)
                Formatters.toCurrencyTextFieldValue(existingTransfer.amount)
            else
                TextFieldValue("", TextRange(0))
        )
    }
    val amountText = amountTextFieldValue.text
    var isScheduled by remember(existingTransfer?.id) {
        mutableStateOf(
            if (existingTransfer != null) existingTransfer.status == PaymentStatus.PENDING else false
        )
    }
    var isRecurring by remember(existingTransfer?.id) {
        mutableStateOf(existingTransfer?.isRecurring ?: false)
    }
    var dateText by remember(existingTransfer?.id) {
        mutableStateOf(existingTransfer?.date?.ifBlank { Formatters.todayDateString() } ?: Formatters.todayDateString())
    }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val fromAcc = accounts.getOrNull(fromIndex) ?: accounts.first()
    val toAcc = accounts.getOrNull(toIndex) ?: accounts.last()
    val isToCofrinho = !toAcc.includeInOverallBalance

    if (showDeleteConfirmDialog && existingTransfer != null && onDeleteTransfer != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Transferência", fontWeight = FontWeight.Bold) },
            text = { Text("Deseja realmente excluir esta transferência de ${Formatters.formatCurrencyPlain(existingTransfer.amount)}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteTransfer(existingTransfer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
                    text = if (existingTransfer != null) "⇄ Editar Transferência" else "⇄ Transferência entre Contas",
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
                        label = { Text("⚡ Efetivada / Paga", fontSize = 11.sp, fontWeight = if (!isScheduled) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MobillsGreenPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = MobillsGreenPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isScheduled,
                        onClick = { isScheduled = true },
                        label = { Text("🕒 Agendada / Reservar", fontSize = 11.sp, fontWeight = if (isScheduled) FontWeight.Bold else FontWeight.Normal) },
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
                    value = amountTextFieldValue,
                    onValueChange = { amountTextFieldValue = Formatters.updateCurrencyInput(it, amountTextFieldValue) },
                    label = { Text("Valor da Transferência (R$)") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo Data com Seletor de Calendário Visual (DatePickerDialog)
                val dialogContext = LocalContext.current
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance()
                            val parts = dateText.split("/")
                            val y = if (parts.size == 3) parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR)
                            val m = if (parts.size == 3) ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11) else cal.get(Calendar.MONTH)
                            val d = if (parts.size == 3) (parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31) else cal.get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(dialogContext, { _, year, month, dayOfMonth ->
                                dateText = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                            }, y, m, d).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📅", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isScheduled) "Data Prevista / Agendada" else "Data da Transferência",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateText.ifBlank { "Selecionar data" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = (if (isScheduled) Color(0xFFFF9800) else MobillsGreenPrimary).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Alterar 🗓️",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isScheduled) Color(0xFFE65100) else MobillsGreenPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
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

                // Switch: Repetir todo mês (Transferência Fixa Recorrente)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, if (isRecurring) MobillsGreenPrimary.copy(alpha = 0.5f) else Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isRecurring = !isRecurring }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("🔁", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Repetir todo mês",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isRecurring) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MobillsGreenPrimary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "FIXO",
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MobillsGreenPrimary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isRecurring)
                                        "Lança e projeta automaticamente em todos os meses seguintes"
                                    else
                                        "Apenas neste mês específico",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = if (isRecurring) MobillsGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MobillsGreenPrimary,
                                checkedTrackColor = MobillsGreenPrimary.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (existingTransfer != null && onDeleteTransfer != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = ExpenseRed.copy(alpha = 0.08f),
                                contentColor = ExpenseRed
                            ),
                            border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("🗑️", fontSize = 15.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Excluir",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ExpenseRed,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Cancelar",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val amt = Formatters.parseCurrency(amountText)
                            if (amt > 0 && fromIndex != toIndex) {
                                if (existingTransfer != null && onUpdateTransfer != null) {
                                    onUpdateTransfer(
                                        existingTransfer,
                                        fromAcc.id,
                                        toAcc.id,
                                        amt,
                                        isScheduled,
                                        dateText.trim(),
                                        isRecurring
                                    )
                                } else {
                                    onTransfer(fromAcc.id, toAcc.id, amt, isScheduled, dateText.trim(), isRecurring)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScheduled) Color(0xFFFF9800) else MobillsGreenPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = if (existingTransfer != null) "Salvar" else (if (isScheduled) "Agendar" else "Transferir"),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun extractNoteParam(note: String, key: String): String? {
    val parts = note.split("|")
    for (p in parts) {
        val kv = p.split(":")
        if (kv.size == 2 && kv[0].trim() == key) {
            return kv[1].trim()
        }
    }
    return null
}

