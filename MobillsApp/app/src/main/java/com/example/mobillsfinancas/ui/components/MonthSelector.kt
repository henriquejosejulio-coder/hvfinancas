package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.theme.MobillsGreenPrimary

@Composable
fun MonthSelector(
    months: List<String>,
    currentMonth: String,
    onSelectMonth: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val currentIndex = months.indexOf(currentMonth)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botão Mês Anterior (<)
            IconButton(
                onClick = {
                    if (currentIndex > 0) {
                        onSelectMonth(months[currentIndex - 1])
                    }
                },
                enabled = currentIndex > 0
            ) {
                Text("❮", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (currentIndex > 0) MobillsGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            }

            // Título Mês / Ano (clicável para abrir lista)
            Box {
                Row(
                    modifier = Modifier
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentMonth.ifEmpty { "Selecionar Mês" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("▾", fontSize = 14.sp, color = MobillsGreenPrimary)
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = month,
                                    fontWeight = if (month == currentMonth) FontWeight.Bold else FontWeight.Normal,
                                    color = if (month == currentMonth) MobillsGreenPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onSelectMonth(month)
                                expanded = false
                            }
                        )
                    }
                    Divider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "➕ Próximo Mês (${com.example.mobillsfinancas.utils.MonthHelper.addMonths(months.lastOrNull() ?: currentMonth, 1)})",
                                fontWeight = FontWeight.Bold,
                                color = MobillsGreenPrimary
                            )
                        },
                        onClick = {
                            val nextMonth = com.example.mobillsfinancas.utils.MonthHelper.addMonths(months.lastOrNull() ?: currentMonth, 1)
                            onSelectMonth(nextMonth)
                            expanded = false
                        }
                    )
                }
            }

            // Botão Próximo Mês (>) - navegação infinita para frente
            IconButton(
                onClick = {
                    if (currentIndex < months.size - 1 && currentIndex >= 0) {
                        onSelectMonth(months[currentIndex + 1])
                    } else if (currentMonth.isNotEmpty()) {
                        val nextMonth = com.example.mobillsfinancas.utils.MonthHelper.addMonths(currentMonth, 1)
                        onSelectMonth(nextMonth)
                    }
                }
            ) {
                Text("❯", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)
            }
        }
    }
}
