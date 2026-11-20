package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun YieldDialog(
    accounts: List<AccountItem>,
    isAutoYieldEnabled: Boolean,
    onToggleAutoYield: (Boolean) -> Unit,
    onApplyYields: (Map<String, Double>) -> Unit,
    onDirectBalanceUpdate: ((AccountItem, Double) -> Unit)? = null,
    cdiData: com.example.mobillsfinancas.services.CdiRateData? = null,
    onRefreshCdi: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentCdiData = cdiData ?: remember { com.example.mobillsfinancas.services.BcbCdiService.getCachedCdiRate(context) }

    val yieldAccounts = remember(accounts) {
        accounts.filter { it.cdiRatePercentage > 0.0 && it.balance > 0.0 }
    }

    // Mapa de valores a aplicar por conta (inicia com o rendimento diário calculado)
    val yieldInputs = remember(yieldAccounts, currentCdiData) {
        mutableStateMapOf<String, String>().apply {
            yieldAccounts.forEach { acc ->
                val calculatedDaily = acc.calculateDailyYield(dailyCdiRatePercent = currentCdiData.dailyRatePercent)
                this[acc.id] = String.format(java.util.Locale.US, "%.2f", calculatedDaily)
            }
        }
    }

    var autoYieldChecked by remember { mutableStateOf(isAutoYieldEnabled) }
    var accountToAdjust by remember { mutableStateOf<AccountItem?>(null) }
    var directBalanceInput by remember { mutableStateOf("") }

    if (accountToAdjust != null) {
        val targetAcc = accountToAdjust!!
        AlertDialog(
            onDismissRequest = { accountToAdjust = null },
            title = {
                Text("Ajustar Saldo Real: ${targetAcc.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Informe o saldo exato que aparece no aplicativo do seu banco para sincronizar imediatamente:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = directBalanceInput,
                        onValueChange = { directBalanceInput = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                        label = { Text("Saldo Real no Banco (R$)") },
                        placeholder = { Text(String.format(java.util.Locale.US, "%.2f", targetAcc.balance)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = directBalanceInput.replace(',', '.').toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onDirectBalanceUpdate?.invoke(targetAcc, parsed)
                        }
                        accountToAdjust = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                ) {
                    Text("Salvar Saldo")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToAdjust = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📈", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Rendimento Diário (CDI)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MobillsGreenPrimary
                            )
                            Text(
                                text = "Mercado Pago & Contas Remuneradas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "📊 CDI BCB: ${String.format(java.util.Locale.US, "%.4f", currentCdiData.dailyRatePercent)}% a.d. (~${String.format(java.util.Locale.US, "%.2f", currentCdiData.annualRatePercent)}% a.a.) • Líquido (-22,5% IR)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MobillsGreenPrimary
                                )
                                if (onRefreshCdi != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "🔄",
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { onRefreshCdi() }
                                    )
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Card de Automação
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MobillsGreenPrimary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MobillsGreenPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Rendimento Automático",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MobillsGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Aplica o rendimento diário líquido oficial em dias úteis ao abrir o app para bater exatamente com o banco.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = autoYieldChecked,
                            onCheckedChange = {
                                autoYieldChecked = it
                                onToggleAutoYield(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MobillsGreenPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (yieldAccounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏦", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Nenhuma conta com rendimento configurado",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Edite uma conta (ex: Mercado Pago) e informe a % do CDI para acompanhar o rendimento.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(yieldAccounts, key = { it.id }) { acc ->
                            val currentInput = yieldInputs[acc.id] ?: "0.00"
                            val parsedYield = currentInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val newBalance = acc.balance + parsedYield

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Linha do Banco e Taxa
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(acc.colorHex).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(acc.bankIcon, fontSize = 16.sp)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val estMonthly = acc.calculateMonthlyYield(dailyCdiRatePercent = currentCdiData.dailyRatePercent)
                                                Text(
                                                    text = "${if (acc.cdiRatePercentage % 1.0 == 0.0) acc.cdiRatePercentage.toInt() else acc.cdiRatePercentage}% CDI • ~${Formatters.formatMoney(estMonthly)}/mês",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Saldo Atual (garante largura e nunca quebra linha)
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Saldo atual",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = Formatters.formatMoney(acc.balance),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            if (onDirectBalanceUpdate != null) {
                                                Text(
                                                    text = "✏️ Ajustar saldo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MobillsGreenPrimary,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            directBalanceInput = String.format(java.util.Locale.US, "%.2f", acc.balance)
                                                            accountToAdjust = acc
                                                        }
                                                        .padding(top = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Campo de Rendimento do Dia e Novo Saldo
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = currentInput,
                                            onValueChange = { val clean = it.filter { c -> c.isDigit() || c == '.' || c == ',' }; yieldInputs[acc.id] = clean },
                                            label = { Text("Rendimento Hoje", fontSize = 11.sp) },
                                            placeholder = { Text("0.05") },
                                            leadingIcon = { Text("+ R$", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IncomeGreen) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1.1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedBorderColor = MobillsGreenPrimary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                focusedLabelColor = MobillsGreenPrimary,
                                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )

                                        Column(
                                            modifier = Modifier.weight(0.9f),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "Novo Saldo:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = Formatters.formatMoney(newBalance),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = MobillsGreenPrimary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }


                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Total a Creditar
                    val totalYieldToApply = yieldAccounts.sumOf { acc ->
                        val input = yieldInputs[acc.id] ?: "0.00"
                        input.replace(',', '.').toDoubleOrNull() ?: 0.0
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total de rendimentos hoje:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "+${Formatters.formatMoney(totalYieldToApply)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val map = mutableMapOf<String, Double>()
                            yieldAccounts.forEach { acc ->
                                val input = yieldInputs[acc.id] ?: "0.00"
                                val amt = input.replace(',', '.').toDoubleOrNull() ?: 0.0
                                if (amt > 0.0) {
                                    map[acc.id] = amt
                                }
                            }
                            onApplyYields(map)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                    ) {
                        Text(
                            text = "✓ Aplicar Rendimentos nas Contas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
