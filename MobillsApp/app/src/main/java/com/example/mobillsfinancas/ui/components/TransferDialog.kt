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
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.AccountItem
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@Composable
fun TransferDialog(
    accounts: List<AccountItem>,
    onDismiss: () -> Unit,
    onTransfer: (fromId: String, toId: String, amount: Double) -> Unit
) {
    if (accounts.size < 2) return

    var fromIndex by remember { mutableIntStateOf(0) }
    var toIndex by remember { mutableIntStateOf(if (accounts.size > 1) 1 else 0) }
    var amountText by remember { mutableStateOf("") }

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
                    text = "Transferência entre Contas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MobillsGreenPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("De (Origem):", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start))
                ScrollableTabRow(
                    selectedTabIndex = fromIndex,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    accounts.forEachIndexed { idx, acc ->
                        Tab(
                            selected = fromIndex == idx,
                            onClick = { fromIndex = idx },
                            text = { Text(acc.name, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Para (Destino):", style = MaterialTheme.typography.bodySmall, modifier = Modifier.align(Alignment.Start))
                ScrollableTabRow(
                    selectedTabIndex = toIndex,
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    accounts.forEachIndexed { idx, acc ->
                        Tab(
                            selected = toIndex == idx,
                            onClick = { toIndex = idx },
                            text = { Text(acc.name, style = MaterialTheme.typography.bodySmall) }
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
                            val amt = Formatters.parseCurrency(amountText)
                            if (amt > 0 && fromIndex != toIndex) {
                                onTransfer(accounts[fromIndex].id, accounts[toIndex].id, amt)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                    ) {
                        Text("Confirmar", color = Color.White)
                    }
                }
            }
        }
    }
}
