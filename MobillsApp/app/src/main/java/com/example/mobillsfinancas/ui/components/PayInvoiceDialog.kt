package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.data.CreditCardItem
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayInvoiceDialog(
    card: CreditCardItem,
    invoiceAmount: Double,
    accounts: List<AccountItem>,
    onDismiss: () -> Unit,
    onConfirmPay: (accountId: String, amount: Double) -> Unit
) {
    var selectedAccount by remember {
        mutableStateOf(
            accounts.find { it.id == card.linkedAccountId } ?: accounts.firstOrNull()
        )
    }
    var amountText by remember { mutableStateOf(Formatters.formatCurrencyPlain(invoiceAmount)) }
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pagar Fatura",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Valor Total da Fatura", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = Formatters.formatMoney(invoiceAmount),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MobillsGreenPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Seletor de Conta que fará o pagamento
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = "${selectedAccount?.bankIcon ?: "🏦"} ${selectedAccount?.name ?: "Selecione uma conta"}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pagar com qual conta?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.bankIcon} ${acc.name} (${Formatters.formatMoney(acc.balance)})") },
                                onClick = {
                                    selectedAccount = acc
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Valor a Pagar
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = Formatters.formatCurrencyInput(it) },
                    label = { Text("Valor a Pagar (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanVal = Formatters.parseCurrency(amountText)
                            if (cleanVal > 0 && selectedAccount != null) {
                                onConfirmPay(selectedAccount!!.id, cleanVal)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                    ) {
                        Text("Confirmar Pagamento", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
