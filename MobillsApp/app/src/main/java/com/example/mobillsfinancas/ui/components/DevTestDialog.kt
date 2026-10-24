package com.example.mobillsfinancas.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.services.BankNotificationService
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary

private const val DEV_PASSWORD = "290517Hv!"

@Composable
fun DevTestDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isUnlocked by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    var debugLogs by remember { mutableStateOf(BankNotificationService.getDebugLogs(context)) }

    // Campos de simulação personalizada
    var customBank by remember { mutableStateOf("Inter") }
    var customTitle by remember { mutableStateOf("Inter") }
    var customText by remember { mutableStateOf("Compra aprovada no seu Cartão Inter: R$ 50,00 em Posto de Combustível") }

    fun refreshLogs() {
        debugLogs = BankNotificationService.getDebugLogs(context)
    }

    if (!isUnlocked) {
        // --- DIÁLOGO DE SENHA DE ACESSO ---
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 34.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Acesso Restrito",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Área exclusiva para testes e simulações de desenvolvedor. Digite sua senha de acesso:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            passwordError = false
                        },
                        label = { Text("Senha de Desenvolvedor") },
                        singleLine = true,
                        isError = passwordError,
                        supportingText = {
                            if (passwordError) {
                                Text("Senha incorreta. Acesso negado.", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        keyboardActions = KeyboardActions(onDone = {
                            if (passwordInput == DEV_PASSWORD) {
                                isUnlocked = true
                                passwordError = false
                            } else {
                                passwordError = true
                            }
                        }),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "👁️" else "🙈", fontSize = 16.sp)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancelar")
                        }

                        Button(
                            onClick = {
                                if (passwordInput == DEV_PASSWORD) {
                                    isUnlocked = true
                                    passwordError = false
                                } else {
                                    passwordError = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Acessar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        // --- CENTRAL DE TESTES DESBLOQUEADA ---
        Dialog(onDismissRequest = onDismiss) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(vertical = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧪", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Central de Testes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                                Text(
                                    text = "Simulador de Notificações de Bancos",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. SEÇÃO CARTÃO DE CRÉDITO
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("💳", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Testar Compras no Cartão de Crédito",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "Simula a notificação de compra aprovada no cartão:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Botão 1: Inter Crédito
                                OutlinedButton(
                                    onClick = {
                                        val ok = BankNotificationService.simulateNotification(
                                            context,
                                            "br.com.intermedium",
                                            "Inter",
                                            "Compra aprovada no seu Cartão Inter: R$ 85,50 em Supermercado BH."
                                        )
                                        if (ok) {
                                            Toast.makeText(context, "✅ Compra Cartão Inter (R$ 85,50) capturada!", Toast.LENGTH_SHORT).show()
                                            refreshLogs()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💳 Inter: R$ 85,50 (Supermercado BH)", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Botão 2: Mercado Pago Crédito
                                OutlinedButton(
                                    onClick = {
                                        val ok = BankNotificationService.simulateNotification(
                                            context,
                                            "com.mercadopago.wallet",
                                            "Mercado Pago",
                                            "Você comprou R$ 42,90 com seu cartão Visa em Posto Ipiranga."
                                        )
                                        if (ok) {
                                            Toast.makeText(context, "✅ Compra Cartão Mercado Pago (R$ 42,90) capturada!", Toast.LENGTH_SHORT).show()
                                            refreshLogs()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💳 Mercado Pago: R$ 42,90 (Posto Ipiranga)", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Botão: Gmail Mercado Pago Pix (Uber R$ 6,94)
                                OutlinedButton(
                                    onClick = {
                                        val ok = BankNotificationService.simulateNotification(
                                            context,
                                            "com.google.android.gm",
                                            "Mercado Pago",
                                            "Seu Pix de R$ 6,94 foi enviado Sexta-feira, 23 de outubro de 2026, às 21:39:57. UBER DO BRASIL TECNOLOGIA LTDA. CPF/CNPJ: xx.x95.646/0001-xx ADYEN DO BRASIL INSTITUICAO DE PAGAMENTO LTDA. Agência: 0001"
                                        )
                                        if (ok) {
                                            Toast.makeText(context, "✅ Pix Gmail Mercado Pago (R$ 6,94 Uber) capturado!", Toast.LENGTH_SHORT).show()
                                            refreshLogs()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("📧 Gmail / MP: Pix R$ 6,94 (Uber)", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Botão 3: Nubank Crédito
                                OutlinedButton(
                                    onClick = {
                                        val ok = BankNotificationService.simulateNotification(
                                            context,
                                            "com.nu.production",
                                            "Nubank",
                                            "Compra de R$ 60,00 aprovada no cartão em Padaria Central."
                                        )
                                        if (ok) {
                                            Toast.makeText(context, "✅ Compra Cartão Nubank (R$ 60,00) capturada!", Toast.LENGTH_SHORT).show()
                                            refreshLogs()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💳 Nubank: R$ 60,00 (Padaria Central)", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Botão 4: Will Bank Crédito
                                OutlinedButton(
                                    onClick = {
                                        val ok = BankNotificationService.simulateNotification(
                                            context,
                                            "com.willbank.app",
                                            "Will Bank",
                                            "Compra aprovada de R$ 35,00 no seu cartão Will Bank."
                                        )
                                        if (ok) {
                                            Toast.makeText(context, "✅ Compra Cartão Will (R$ 35,00) capturada!", Toast.LENGTH_SHORT).show()
                                            refreshLogs()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("💳 Will Bank: R$ 35,00 (Farmácia)", fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. SEÇÃO PIX & CONTAS
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⚡", fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Testar Pix & Transferências",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = "Simula recebimento ou envio de Pix:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val ok = BankNotificationService.simulateNotification(
                                                context,
                                                "com.mercadopago.wallet",
                                                "Seu dinheiro já está disponível",
                                                "Você depositou R$ 11 via Pix e o valor já está rendendo na sua conta."
                                            )
                                            if (ok) {
                                                Toast.makeText(context, "✅ Pix MP (R$ 11) capturado!", Toast.LENGTH_SHORT).show()
                                                refreshLogs()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("MP Pix (R$ 11)", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val ok = BankNotificationService.simulateNotification(
                                                context,
                                                "br.com.intermedium",
                                                "Inter",
                                                "Pix enviado: você transferiu R$ 11 para Mercado Pago com sucesso."
                                            )
                                            if (ok) {
                                                Toast.makeText(context, "✅ Pix Inter (R$ 11) capturado!", Toast.LENGTH_SHORT).show()
                                                refreshLogs()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Inter Pix (R$ 11)", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // 3. REGISTRO DE LOGS DE NOTIFICAÇÃO
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("📋", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Logs de Captura", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { refreshLogs() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("🔄 Atualizar", fontSize = 11.sp, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                BankNotificationService.clearDebugLogs(context)
                                                refreshLogs()
                                                Toast.makeText(context, "Logs limpos!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("🗑️ Limpar", fontSize = 11.sp, color = ExpenseRed, maxLines = 1)
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 180.dp)
                                ) {
                                    Text(
                                        text = debugLogs,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .verticalScroll(rememberScrollState())
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Rodapé com botão de Bloquear e Fechar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isUnlocked = false
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔒 Bloquear e Sair", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("Concluído", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
