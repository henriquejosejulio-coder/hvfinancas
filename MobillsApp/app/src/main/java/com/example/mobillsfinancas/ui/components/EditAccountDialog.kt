package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.BankRegistry
import com.example.mobillsfinancas.utils.Formatters

private val PRESET_ACCOUNT_COLORS = listOf(
    0xFFFF7A00L, // Laranja Inter / PagBank
    0xFF8A05BEL, // Roxo Nubank
    0xFF009EE3L, // Azul Mercado Pago
    0xFFFFD500L, // Amarelo Will / BB
    0xFF0047BAL, // Azul Sofisa / Caixa
    0xFF00A868L, // Verde Mobills / PicPay / Sicredi
    0xFFCC0000L, // Vermelho Santander / Bradesco
    0xFF1F2937L, // Preto / Grafite Black
    0xFF0038A8L, // Azul Escuro Itaú
    0xFFE91E63L, // Rosa Neon / Pan
    0xFF7C3AEDL, // Violeta
    0xFF059669L, // Verde Petróleo
    0xFF0284C7L, // Azul Turquesa / C6
    0xFFB45309L  // Dourado / Cobre
)

@Composable
fun EditAccountDialog(
    account: AccountItem?, // null if adding new account
    onDismiss: () -> Unit,
    onSave: (AccountItem) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var balanceText by remember { mutableStateOf(if (account != null && account.balance != 0.0) Formatters.formatCurrencyPlain(Math.abs(account.balance)) else "") }
    var selectedColorHex by remember { mutableStateOf(account?.colorHex ?: 0xFF00A868L) }
    var isDefault by remember { mutableStateOf(account?.isDefault ?: false) }
    var includeInOverallBalance by remember { mutableStateOf(account?.includeInOverallBalance ?: true) }

    val accountPalette = remember(account?.colorHex) {
        val current = account?.colorHex
        if (current != null && current !in PRESET_ACCOUNT_COLORS) {
            listOf(current) + PRESET_ACCOUNT_COLORS
        } else {
            PRESET_ACCOUNT_COLORS
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
                    text = if (account == null) "Cadastrar Banco / Conta" else "Editar Conta Bancária",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Sugestões de Bancos Populares (Preenchimento Rápido com 1 Toque)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🏦 Sugestão de Bancos (Toque para preencher):",
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
                            val isSelected = name.equals(b.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    name = b.name
                                    selectedColorHex = b.colorHex
                                },
                                label = { Text("${b.icon} ${b.name}", fontSize = 12.sp) },
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
                    label = { Text("Nome da Conta ou Banco") },
                    placeholder = { Text("Ex: Banco Inter, Nubank, Cofrinho") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = Formatters.formatCurrencyInput(it) },
                    label = { Text("Saldo Atual (R$)") },
                    placeholder = { Text("0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Seletor de Cor da Conta
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎨 Cor da Conta / Banco",
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
                        text = "Escolha a cor para identificar este banco no app:",
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
                        accountPalette.forEach { hex ->
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

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDefault) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⭐ Conta Principal / Padrão",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDefault) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Usada como padrão para novas receitas, despesas e capturas automáticas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDefault) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDefault,
                            onCheckedChange = { isDefault = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MobillsGreenPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (!includeInOverallBalance) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🐖 Cofrinho / Reserva (Guardado)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (!includeInOverallBalance) Color(0xFF0369A1) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Não soma no saldo livre do dia a dia. Fica separado como dinheiro guardado rendendo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (!includeInOverallBalance) Color(0xFF0284C7) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !includeInOverallBalance,
                            onCheckedChange = { isReserve -> includeInOverallBalance = !isReserve },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0284C7))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null && onDelete != null) {
                        TextButton(
                            onClick = { onDelete(account.id) },
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
                                val bal = Formatters.parseCurrency(balanceText)
                                if (name.isNotBlank()) {
                                    val detectedBank = BankRegistry.findBankByName(name.trim())
                                    val bankIcon = detectedBank?.icon ?: (account?.bankIcon ?: "🏦")
                                    val item = account?.copy(
                                        name = name.trim(),
                                        balance = bal,
                                        colorHex = selectedColorHex,
                                        bankIcon = bankIcon,
                                        isDefault = isDefault,
                                        includeInOverallBalance = includeInOverallBalance
                                    ) ?: AccountItem(
                                        id = "acc_" + System.currentTimeMillis(),
                                        name = name.trim(),
                                        balance = bal,
                                        colorHex = selectedColorHex,
                                        bankIcon = bankIcon,
                                        isDefault = isDefault,
                                        includeInOverallBalance = includeInOverallBalance
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
