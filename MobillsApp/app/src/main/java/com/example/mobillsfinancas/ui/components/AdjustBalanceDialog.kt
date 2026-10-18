package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.mobillsfinancas.data.MonthData
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBalanceDialog(
    selectedMonth: String,
    currentMonthData: MonthData?,
    accounts: List<AccountItem>,
    initialTab: Int = 0,
    onDismiss: () -> Unit,
    onAdjustMonthBalance: (Double) -> Unit,
    onSetBaseIncome: (Double, Boolean) -> Unit,
    onUpdateAccount: (AccountItem) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    val remaining = currentMonthData?.remainingBalance ?: 0.0
    val baseIncome = currentMonthData?.baseIncome ?: 0.0

    var realBalanceInput by remember {
        mutableStateOf(Formatters.formatCurrencyPlain(remaining))
    }
    var baseIncomeInput by remember {
        mutableStateOf(Formatters.formatCurrencyPlain(baseIncome))
    }
    var applyToFutureMonths by remember { mutableStateOf(true) }

    // Edição rápida de conta
    var editingAccount by remember { mutableStateOf<AccountItem?>(null) }
    var accountBalanceInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Cabeçalho
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚖️ Acertar Saldo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MobillsGreenPrimary
                        )
                        Text(
                            text = "Ajuste para bater com sua realidade",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Abas
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MobillsGreenPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Salário / Renda", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Saldo do Mês", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Bancos / Contas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                if (selectedTab == 0) {
                    // Aba 0: Ajustar Salário / Receita Base
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "💵 Salário e Receita Base: $selectedMonth",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Defina sua renda base mensal (ex: seu salário de R$ 3.300,00). O HV Finanças usa esse valor como base para abater suas despesas fixas, mercado e calcular quanto sobra livre para você.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Salário Base Atual
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Salário / Receita atual:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatMoney(baseIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Novo Salário / Receita Mensal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Digite o valor da sua receita base fixa:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = baseIncomeInput,
                            onValueChange = { baseIncomeInput = Formatters.formatCurrencyInput(it) },
                            label = { Text("Salário / Receita Base (R$)") },
                            placeholder = { Text("0,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { applyToFutureMonths = !applyToFutureMonths }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = applyToFutureMonths,
                                onCheckedChange = { applyToFutureMonths = it },
                                colors = CheckboxDefaults.colors(checkedColor = MobillsGreenPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Aplicar também para os meses seguintes",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val clean = Formatters.parseCurrency(baseIncomeInput)
                                onSetBaseIncome(clean, applyToFutureMonths)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✓ Salvar Salário Base", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else if (selectedTab == 1) {
                    // Aba 1: Acerto do Saldo do Mês
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "📌 Mês selecionado: $selectedMonth",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Você pode definir quanto sobrou no final do mês. O app ajusta a diferença e transfere esse saldo para o próximo mês ao virar a folha.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Saldo Atual Calculado
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saldo atual calculado:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatMoney(remaining),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remaining >= 0) IncomeGreen else ExpenseRed
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Definir Saldo Real Final",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Digite quanto você quer que reste no final de $selectedMonth:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = realBalanceInput,
                            onValueChange = { realBalanceInput = Formatters.formatCurrencyInput(it) },
                            label = { Text("Novo Saldo Real (R$)") },
                            placeholder = { Text("0,00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val clean = Formatters.parseCurrency(realBalanceInput)
                                onAdjustMonthBalance(clean)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("✓ Confirmar Acerto de Saldo", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } else {
                    // Aba 1: Bancos e Contas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        val totalInAccounts = accounts.sumOf { it.balance }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Saldo Total em Contas:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = Formatters.formatMoney(totalInAccounts),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MobillsGreenPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Toque em uma conta para acertar o saldo:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accounts, key = { it.id }) { acc ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingAccount = acc
                                            accountBalanceInput = String.format(Locale.US, "%.2f", acc.balance)
                                        },
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(acc.bankIcon, fontSize = 22.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(acc.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text(acc.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = Formatters.formatMoney(acc.balance),
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (acc.balance >= 0) IncomeGreen else ExpenseRed
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("✏️", fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo inline para editar saldo da conta
    if (editingAccount != null) {
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text("Acertar Saldo de ${editingAccount!!.name}") },
            text = {
                Column {
                    Text(
                        text = "Informe o saldo real desta conta no momento:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = accountBalanceInput,
                        onValueChange = { accountBalanceInput = Formatters.formatCurrencyInput(it) },
                        label = { Text("Saldo Real (R$)") },
                        placeholder = { Text("0,00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bal = Formatters.parseCurrency(accountBalanceInput)
                        onUpdateAccount(editingAccount!!.copy(balance = bal))
                        editingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                ) {
                    Text("Salvar Saldo", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
