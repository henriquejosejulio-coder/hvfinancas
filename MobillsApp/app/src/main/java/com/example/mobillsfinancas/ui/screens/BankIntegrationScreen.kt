package com.example.mobillsfinancas.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.PermissionHelper
import com.example.mobillsfinancas.services.BankNotificationService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankIntegrationScreen(
    currentMonth: String,
    onImportOfx: (content: String, target: String) -> Int,
    onImportCsv: (content: String, target: String) -> Int,
    onExportBackup: () -> String,
    onImportBackup: (String) -> Boolean,
    onClearCurrentMonth: () -> Unit,
    onClearAllTransactions: () -> Unit,
    onStartProfessorMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTarget by remember { mutableStateOf("Cartão Inter") }
    var fileContent by remember { mutableStateOf("") }
    var importStatusMessage by remember { mutableStateOf("") }
    var isNotificationListenerEnabled by remember {
        mutableStateOf(PermissionHelper.isNotificationListenerEnabled(context))
    }
    var showDevTestDialog by remember { mutableStateOf(false) }
    var debugLogs by remember {
        mutableStateOf(BankNotificationService.getDebugLogs(context))
    }

    LaunchedEffect(Unit) {
        isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
        debugLogs = BankNotificationService.getDebugLogs(context)
    }

    val targets = listOf("Cartão Inter", "Cartão Will", "Cartão Sofisa", "Cartão Mercado Pago", "Conta Principal (Pix)")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "Integração Bancária",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MobillsGreenPrimary
        )
        Text(
            text = "Conecte seus bancos e automatize seus gastos no celular",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Card 1: Leitor Automático de Notificações
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Leitor de Notificações de Bancos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nubank, Inter, Mercado Pago, C6, Itaú, Bradesco, Santander, Caixa, BB, PicPay e outros",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Badge da Permissão
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isNotificationListenerEnabled) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isNotificationListenerEnabled) Color(0xFF86EFAC) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isNotificationListenerEnabled) "✅" else "⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isNotificationListenerEnabled) "Sincronizador Ativo no Android" else "Acesso a Notificações Necessário",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isNotificationListenerEnabled) Color(0xFF166534) else Color(0xFF92400E)
                            )
                            Text(
                                text = if (isNotificationListenerEnabled) "O HV Finanças lê e cadastra seus Pix e compras automaticamente." else "Toque no botão abaixo para autorizar a leitura no sistema.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isNotificationListenerEnabled) Color(0xFF166534) else Color(0xFF78350F)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isNotificationListenerEnabled) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ativar Permissão no Android", color = Color.White)
                    }

                    if (Build.VERSION.SDK_INT >= 33) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "🔒 Se a chave estiver cinza ('Configuração restrita'):",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "O Android 13/14 exige liberar uma única vez nas configurações do app:\n" +
                                           "1. Toque em 'Tirar Restrições do App' abaixo.\n" +
                                           "2. Toque nos 3 pontinhos (⋮) no topo direito da tela.\n" +
                                           "3. Selecione 'Permitir configurações restritas'.\n" +
                                           "4. Volte e ative a permissão normalmente!",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFF7F1D1D)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { PermissionHelper.openAppDetails(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp)
                                ) {
                                    Text("⚙️ Tirar Restrições do App (Info do App)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 2: Importação de Extrato / Fatura (OFX / CSV)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📁", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Importar Fatura / Extrato (OFX ou CSV)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Baixe o extrato no app do seu banco e cole aqui",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Destino dos lançamentos:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                ScrollableTabRow(
                    selectedTabIndex = targets.indexOf(selectedTarget).coerceAtLeast(0),
                    edgePadding = 0.dp,
                    divider = {}
                ) {
                    targets.forEach { target ->
                        Tab(
                            selected = selectedTarget == target,
                            onClick = { selectedTarget = target },
                            text = { Text(target, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fileContent,
                    onValueChange = { fileContent = it },
                    label = { Text("Conteúdo do arquivo OFX ou CSV") },
                    placeholder = { Text("Cole o conteúdo do arquivo OFX ou CSV exportado do banco...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (fileContent.isNotBlank()) {
                                val count = if (fileContent.contains("<OFX>", ignoreCase = true) || fileContent.contains("<STMTTRN>", ignoreCase = true)) {
                                    onImportOfx(fileContent, selectedTarget)
                                } else {
                                    onImportCsv(fileContent, selectedTarget)
                                }
                                importStatusMessage = "Sucesso: $count transações importadas para $selectedTarget!"
                                fileContent = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Processar e Importar", color = Color.White)
                    }
                }

                if (importStatusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 3: Backup & Restauração dos Dados
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💾", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Backup & Restauração",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Salve seus dados com segurança ou restaure em outro celular",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                var showBackupDialog by remember { mutableStateOf(false) }
                var showImportDialog by remember { mutableStateOf(false) }
                var showConfirmRestoreDialog by remember { mutableStateOf(false) }
                var jsonToRestore by remember { mutableStateOf("") }
                var backupJson by remember { mutableStateOf("") }

                // Launcher para escolher arquivo JSON no celular (Downloads, Drive, etc.)
                val filePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            if (!content.isNullOrBlank()) {
                                jsonToRestore = content
                                showConfirmRestoreDialog = true
                            } else {
                                Toast.makeText(context, "O arquivo selecionado está vazio.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erro ao ler arquivo: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            backupJson = onExportBackup()
                            showBackupDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📦", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Exportar Backup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("📥", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Importar Backup",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // DIALOG DE EXPORTAÇÃO
                if (showBackupDialog) {
                    AlertDialog(
                        onDismissRequest = { showBackupDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📦", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Exportar Backup (JSON)", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Backup gerado com sucesso (${backupJson.length} caracteres). Escolha como deseja salvar ou enviar:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Ação 1: Salvar no Google Drive ou Compartilhar Arquivo via FileProvider
                                Button(
                                    onClick = {
                                        try {
                                            val cacheFile = File(context.cacheDir, "hvfinancas_backup.json")
                                            cacheFile.writeText(backupJson)
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                cacheFile
                                            )
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, "hvfinancas_backup.json")
                                                putExtra(Intent.EXTRA_TITLE, "hvfinancas_backup.json")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Salvar no Drive ou Compartilhar via..."))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao abrir compartilhamento: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("☁️ Salvar no Google Drive / Compartilhar", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Ação 2: Salvar direto na pasta Downloads
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            val target = File(downloads, "hvfinancas_backup.json")
                                            target.writeText(backupJson)
                                            Toast.makeText(context, "💾 Salvo em: Downloads/hvfinancas_backup.json", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao salvar em Downloads: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("💾 Salvar na Pasta Downloads")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Ação 3: Copiar JSON Completo
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("HVFinancas Backup", backupJson)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "✅ JSON completo copiado para a área de transferência!", Toast.LENGTH_LONG).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("📋 Copiar Texto JSON")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Prévia do Conteúdo:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = backupJson.take(500) + if (backupJson.length > 500) "\n... [${backupJson.length} caracteres no total]" else "",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showBackupDialog = false }) {
                                Text("Fechar")
                            }
                        }
                    )
                }

                // DIALOG DE IMPORTAÇÃO / RESTAURAÇÃO
                if (showImportDialog) {
                    var pasteJsonInput by remember { mutableStateOf("") }
                    val downloadsBackupFile = remember {
                        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "hvfinancas_backup.json")
                    }

                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📥", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restaurar Backup (JSON)", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Escolha como deseja carregar seu arquivo de backup:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Opção 1: Selecionar Arquivo no Celular (Downloads, Drive, etc.)
                                Button(
                                    onClick = {
                                        try {
                                            filePickerLauncher.launch("*/*")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Erro ao abrir seletor de arquivos: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("📂 Selecionar Arquivo do Celular (.json)", fontWeight = FontWeight.Bold)
                                }

                                // Opção 2: Se o arquivo já existir em Downloads, botão rápido de 1 clique!
                                if (downloadsBackupFile.exists()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val content = downloadsBackupFile.readText()
                                                if (content.isNotBlank()) {
                                                    jsonToRestore = content
                                                    showConfirmRestoreDialog = true
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Erro ao ler Downloads: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("💾 Carregar de Downloads/hvfinancas_backup.json", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Ou cole o texto JSON do backup:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = pasteJsonInput,
                                    onValueChange = { pasteJsonInput = it },
                                    placeholder = { Text("Cole o JSON do backup aqui...") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                )

                                if (pasteJsonInput.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            jsonToRestore = pasteJsonInput
                                            showConfirmRestoreDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Restaurar Este JSON", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                // DIALOG DE CONFIRMAÇÃO DE RESTAURAÇÃO
                if (showConfirmRestoreDialog) {
                    AlertDialog(
                        onDismissRequest = { showConfirmRestoreDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚠️", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Confirmar Restauração", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Text(
                                text = "Atenção: Ao restaurar este backup (${jsonToRestore.length} caracteres), todos os seus dados atuais (contas, cartões, faturas, categorias e lançamentos) serão substituídos pelos dados contidos no arquivo.\n\nDeseja prosseguir com a restauração?",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val success = onImportBackup(jsonToRestore)
                                    if (success) {
                                        Toast.makeText(context, "✅ Backup restaurado com sucesso!", Toast.LENGTH_LONG).show()
                                        showConfirmRestoreDialog = false
                                        showImportDialog = false
                                        jsonToRestore = ""
                                    } else {
                                        Toast.makeText(context, "❌ Erro: Formato de backup inválido.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                            ) {
                                Text("Sim, Restaurar Dados", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showConfirmRestoreDialog = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 4: Limpar / Zerar Movimentações
        var showConfirmClearMonth by remember { mutableStateOf(false) }
        var showConfirmClearAll by remember { mutableStateOf(false) }
        var clearSuccessMsg by remember { mutableStateOf("") }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🧹", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Limpar / Zerar Movimentações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.mobillsfinancas.theme.ExpenseRed
                        )
                        Text(
                            text = "Apague lançamentos para cadastrar tudo manualmente",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Se você prefere lançar tudo manualmente na mão, pode apagar as movimentações de uma só vez. Suas contas bancárias, cartões de crédito e categorias continuarão salvos e intactos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (clearSuccessMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = clearSuccessMsg,
                        color = MobillsGreenPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = { showConfirmClearMonth = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🗑️ Limpar Movimentações de $currentMonth")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showConfirmClearAll = true },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.ExpenseRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚠️ Zerar TODOS os Lançamentos (Zerar Tudo)", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (showConfirmClearMonth) {
                    AlertDialog(
                        onDismissRequest = { showConfirmClearMonth = false },
                        title = { Text("Limpar Mês Atual?") },
                        text = {
                            Text("Deseja apagar todos os lançamentos do mês de $currentMonth? Seus bancos e cartões não serão afetados.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmClearMonth = false
                                    onClearCurrentMonth()
                                    clearSuccessMsg = "Lançamentos de $currentMonth apagados com sucesso!"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.ExpenseRed)
                            ) {
                                Text("Sim, Limpar Mês", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmClearMonth = false }) { Text("Cancelar") }
                        }
                    )
                }

                if (showConfirmClearAll) {
                    AlertDialog(
                        onDismissRequest = { showConfirmClearAll = false },
                        title = { Text("⚠️ Zerar TODOS os Lançamentos?") },
                        text = {
                            Text("Atenção: Todos os lançamentos de todos os meses serão apagados para você começar do zero na mão. Suas contas, saldos e cartões de crédito serão mantidos. Deseja continuar?")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showConfirmClearAll = false
                                    onClearAllTransactions()
                                    clearSuccessMsg = "Todas as movimentações foram apagadas com sucesso! Extrato limpo."
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.ExpenseRed)
                            ) {
                                Text("Sim, Zerar Tudo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showConfirmClearAll = false }) { Text("Cancelar") }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card: Modo Professor (Tutorial Interativo)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFF6366F1).copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎓", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Modo Professor (Tutorial Guiado)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6366F1)
                        )
                        Text(
                            text = "Aprenda passo a passo a usar todas as telas e recursos do app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "O Modo Professor te guia por todo o aplicativo: saldo consolidado, resumo de receitas e despesas, contas e cartões, leitor automático de bancos, formulário completo de lançamentos, extrato detalhado, categorias e backup seguro.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { onStartProfessorMode?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("👨‍🏫 Iniciar Tutorial Interativo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card 4: Informações do Aplicativo / Versão
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.mobillsfinancas.R.drawable.app_logo),
                    contentDescription = "Logo HV Finanças",
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "HV Finanças",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MobillsGreenPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "Versão ${com.example.mobillsfinancas.BuildConfig.VERSION_NAME} • Build ${com.example.mobillsfinancas.BuildConfig.VERSION_CODE}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Controle Financeiro Pessoal & Familiar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Armazenamento 100% Local, Seguro e Privado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showDevTestDialog = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🔒 Área de Testes (Acesso Restrito)", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showDevTestDialog) {
        com.example.mobillsfinancas.ui.components.DevTestDialog(
            onDismiss = { showDevTestDialog = false }
        )
    }
}
