package com.example.mobillsfinancas.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.data.AuditLogEntry
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditScreen(
    auditLogs: List<AuditLogEntry>,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }
    val dateFilterOptions = listOf(
        "📅 Hoje",
        "Ontem",
        "7 Dias",
        "Este Mês",
        "📆 Personalizado",
        "Todos os Dias"
    )
    var selectedDateFilter by remember { mutableStateOf("📅 Hoje") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val todayDateStr = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
    val firstDayOfMonthStr = remember {
        val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)
    }
    var customStartDateStr by remember { mutableStateOf(firstDayOfMonthStr) }
    var customEndDateStr by remember { mutableStateOf(todayDateStr) }

    val filterOptions = listOf(
        "Todos",
        "✏️ Edições",
        "⚡ Capturas",
        "⇄ Transferências",
        "🗑️ Exclusões",
        "⚖️ Ajuste Saldo",
        "💳 Faturas",
        "➕ Lançamentos",
        "📥 Backups"
    )

    val filteredLogs = remember(auditLogs, searchQuery, selectedFilter, selectedDateFilter, customStartDateStr, customEndDateStr) {
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfSevenDays = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -6)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val yesterdayDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L))

        auditLogs.filter { log ->
            // Filtro por Data
            val logTime = if (log.timestamp > 0L) log.timestamp else {
                try {
                    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(log.dateTimeStr)?.time ?: 0L
                } catch (e: Exception) { 0L }
            }

            val matchesDate = when (selectedDateFilter) {
                "📅 Hoje" -> if (logTime > 0L) logTime >= startOfToday else log.dateTimeStr.startsWith(todayDateStr)
                "Ontem" -> if (logTime > 0L) logTime in startOfYesterday until startOfToday else log.dateTimeStr.startsWith(yesterdayDateStr)
                "7 Dias" -> if (logTime > 0L) logTime >= startOfSevenDays else true
                "Este Mês" -> if (logTime > 0L) logTime >= startOfMonth else true
                "📆 Personalizado" -> {
                    val startMs = parseDateToStartOfDayMs(customStartDateStr)
                    val endMs = parseDateToEndOfDayMs(customEndDateStr)
                    if (startMs != null && endMs != null) {
                        logTime in startMs..endMs
                    } else if (startMs != null) {
                        logTime >= startMs
                    } else if (endMs != null) {
                        logTime <= endMs
                    } else {
                        true
                    }
                }
                else -> true
            }

            // Filtro por tipo de ação
            val matchesFilter = when (selectedFilter) {
                "✏️ Edições" -> log.action.contains("EDITOU", ignoreCase = true)
                "⚡ Capturas" -> log.action.contains("CAPTURA", ignoreCase = true)
                "⇄ Transferências" -> log.action.contains("TRANSFERENCIA", ignoreCase = true)
                "🗑️ Exclusões" -> log.action.contains("EXCLUIU", ignoreCase = true)
                "⚖️ Ajuste Saldo" -> log.action.contains("ACERTOU", ignoreCase = true)
                "💳 Faturas" -> log.action.contains("FATURA", ignoreCase = true)
                "➕ Lançamentos" -> log.action.contains("CRIOU", ignoreCase = true) || log.action.contains("COMPRA", ignoreCase = true) || log.action.contains("RECEITA", ignoreCase = true)
                "📥 Backups" -> log.action.contains("BACKUP", ignoreCase = true)
                else -> true
            }

            // Filtro por busca de texto
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            val matchesSearch = q.isEmpty() ||
                    log.summary.lowercase(Locale.ROOT).contains(q) ||
                    log.action.lowercase(Locale.ROOT).contains(q) ||
                    log.details.lowercase(Locale.ROOT).contains(q) ||
                    log.dateTimeStr.lowercase(Locale.ROOT).contains(q)

            matchesDate && matchesFilter && matchesSearch
        }
    }

    // Tela inteira rolável de forma unificada (Resolve tela travada/pequena)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Item 1: Header com Copiar, Compartilhar e Limpar
        item(key = "header_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📜", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Histórico de Auditoria",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Linha do tempo de todas as ações e alterações",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botões de Ação Rápida (Layout robusto sem quebra de texto)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (filteredLogs.isEmpty()) {
                                    Toast.makeText(context, "Nenhum registro para copiar.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val sb = StringBuilder()
                                sb.append("=== HISTÓRICO DE AUDITORIA HV FINANÇAS ===\n")
                                sb.append("Total de registros: ${filteredLogs.size}\n\n")
                                for (log in filteredLogs) {
                                    sb.append("[${log.dateTimeStr}] [${log.action}]\n")
                                    sb.append("Resumo: ${log.summary}\n")
                                    if (log.details.isNotBlank()) {
                                        sb.append("Detalhes: ${log.details}\n")
                                    }
                                    sb.append("----------------------------------------\n")
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Auditoria HV Finanças", sb.toString())
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "✅ ${filteredLogs.size} registros copiados com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📋", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Copiar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (filteredLogs.isEmpty()) {
                                    Toast.makeText(context, "Nenhum registro para exportar.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                try {
                                    val sb = StringBuilder()
                                    sb.append("=== HISTÓRICO DE AUDITORIA HV FINANÇAS ===\n")
                                    sb.append("Total de registros: ${filteredLogs.size}\n")
                                    sb.append("Gerado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
                                    for (log in filteredLogs) {
                                        sb.append("[${log.dateTimeStr}] [${log.action}]\n")
                                        sb.append("Resumo: ${log.summary}\n")
                                        if (log.details.isNotBlank()) {
                                            sb.append("Detalhes: ${log.details}\n")
                                        }
                                        sb.append("----------------------------------------\n")
                                    }
                                    val cacheFile = File(context.cacheDir, "auditoria_hvfinancas.txt")
                                    cacheFile.writeText(sb.toString())
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        cacheFile
                                    )
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "auditoria_hvfinancas.txt")
                                        putExtra(Intent.EXTRA_TITLE, "auditoria_hvfinancas.txt")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Exportar Histórico de Auditoria via..."))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Erro ao exportar arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.35f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📤", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Compartilhar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = Color.White
                                )
                            }
                        }

                        if (auditLogs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showClearConfirmDialog = true },
                                modifier = Modifier.weight(0.95f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🗑️", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Limpar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = ExpenseRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Item 2: Filtros e Barra de Busca
        item(key = "filters_section") {
            Column {
                // Campo de Busca
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pesquisar por ação, valor, conta ou data...", fontSize = 13.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. Carrossel de Chips de Filtro por Data
                Text(
                    text = "Filtrar por data:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dateFilterOptions) { dateOption ->
                        val isSelected = selectedDateFilter == dateOption
                        val chipText = if (dateOption == "📆 Personalizado" && isSelected && customStartDateStr.isNotBlank() && customEndDateStr.isNotBlank()) {
                            "📆 ${customStartDateStr.take(5)} a ${customEndDateStr.take(5)}"
                        } else {
                            dateOption
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDateFilter = dateOption
                                if (dateOption == "📆 Personalizado" && customStartDateStr.isBlank()) {
                                    customStartDateStr = firstDayOfMonthStr
                                    customEndDateStr = todayDateStr
                                }
                            },
                            label = {
                                Text(
                                    text = chipText,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (dateOption == "📅 Hoje") MobillsGreenPrimary.copy(alpha = 0.25f) else Color(0xFF818CF8).copy(alpha = 0.25f),
                                selectedLabelColor = if (dateOption == "📅 Hoje") MobillsGreenPrimary else Color(0xFF818CF8)
                            )
                        )
                    }
                }

                // Seletor de Período Personalizado (De / Até) com Calendário Nativo
                if (selectedDateFilter == "📆 Personalizado") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Data Inicial
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showDatePicker(context, customStartDateStr) { newDate ->
                                            customStartDateStr = newDate
                                        }
                                    }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("De: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = customStartDateStr.ifBlank { "Selecionar" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📅", fontSize = 12.sp)
                            }

                            Text("➔", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // Data Final
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        showDatePicker(context, customEndDateStr) { newDate ->
                                            customEndDateStr = newDate
                                        }
                                    }
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Até: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = customEndDateStr.ifBlank { "Selecionar" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("📅", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2. Carrossel de Chips de Filtro por Ação
                Text(
                    text = "Filtrar por ação:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MobillsGreenPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = MobillsGreenPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contagem de Registros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateLabel = if (selectedDateFilter == "📆 Personalizado") {
                        "$customStartDateStr a $customEndDateStr"
                    } else {
                        selectedDateFilter
                    }
                    Text(
                        text = if (filteredLogs.size == auditLogs.size) {
                            "${auditLogs.size} ações no histórico"
                        } else {
                            "Exibindo ${filteredLogs.size} de ${auditLogs.size} ações ($dateLabel)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Itens de Auditoria ou Estado Vazio
        if (filteredLogs.isEmpty()) {
            item(key = "empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📜", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        val emptyMsg = when {
                            selectedDateFilter == "📅 Hoje" -> "Nenhuma ação registrada hoje."
                            selectedDateFilter == "Ontem" -> "Nenhuma ação registrada ontem."
                            selectedDateFilter == "📆 Personalizado" -> "Nenhuma ação registrada no período de $customStartDateStr a $customEndDateStr."
                            searchQuery.isNotEmpty() -> "Nenhum registro para '$searchQuery'."
                            else -> "Nenhum registro para estes filtros."
                        }
                        Text(
                            text = emptyMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedDateFilter != "Todos os Dias" && auditLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedDateFilter = "Todos os Dias"
                                    selectedFilter = "Todos"
                                    searchQuery = ""
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Ver Todos os ${auditLogs.size} Registros", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                val (icon, badgeColor, badgeLabel) = when {
                    log.action.contains("CAPTURA") -> Triple("⚡", Color(0xFF00E676), "CAPTURA AUTOMÁTICA")
                    log.action.contains("EDITOU") -> Triple("✏️", Color(0xFF38BDF8), "EDIÇÃO")
                    log.action.contains("EXCLUIU") -> Triple("🗑️", Color(0xFFEF4444), "EXCLUSÃO")
                    log.action.contains("TRANSFERENCIA") -> Triple("⇄", Color(0xFF818CF8), "TRANSFERÊNCIA")
                    log.action.contains("FATURA") -> Triple("💳", Color(0xFFA855F7), "FATURA PAGA")
                    log.action.contains("ACERTOU") -> Triple("⚖️", Color(0xFFF59E0B), "AJUSTE DE SALDO")
                    log.action.contains("BACKUP") -> Triple("📥", Color(0xFF06B6D4), "BACKUP")
                    else -> Triple("➕", IncomeGreen, log.action)
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val itemText = "[${log.dateTimeStr}] ${badgeLabel}: ${log.summary}\n${log.details}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Registro de Auditoria", itemText))
                            Toast.makeText(context, "Ação copiada!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(icon, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badgeLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = log.dateTimeStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = log.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (log.details.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = log.details,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Limpar Histórico de Auditoria?") },
            text = { Text("Tem certeza que deseja apagar todos os registros de auditoria gravados? Essa ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearLogs()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Histórico de auditoria limpo.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Sim, Limpar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

private fun showDatePicker(
    context: Context,
    currentDateStr: String,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    val parts = currentDateStr.split("/", "-")
    val y = if (parts.size == 3) parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR)
    val m = if (parts.size == 3) ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11) else cal.get(Calendar.MONTH)
    val d = if (parts.size == 3) (parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31) else cal.get(Calendar.DAY_OF_MONTH)

    DatePickerDialog(context, { _, year, month, dayOfMonth ->
        val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
        onDateSelected(formatted)
    }, y, m, d).show()
}

fun parseDateToStartOfDayMs(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    return try {
        val parts = dateStr.trim().split("/", "-")
        if (parts.size == 3) {
            val d = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val y = parts[2].toInt()
            Calendar.getInstance().apply {
                set(y, m, d, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } else null
    } catch (e: Exception) {
        null
    }
}

fun parseDateToEndOfDayMs(dateStr: String): Long? {
    if (dateStr.isBlank()) return null
    return try {
        val parts = dateStr.trim().split("/", "-")
        if (parts.size == 3) {
            val d = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val y = parts[2].toInt()
            Calendar.getInstance().apply {
                set(y, m, d, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        } else null
    } catch (e: Exception) {
        null
    }
}
