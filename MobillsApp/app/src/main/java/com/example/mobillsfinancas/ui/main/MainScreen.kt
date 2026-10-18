package com.example.mobillsfinancas.ui.main

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.example.mobillsfinancas.data.*
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.ui.components.*
import com.example.mobillsfinancas.ui.screens.BankIntegrationScreen
import com.example.mobillsfinancas.ui.screens.CategoriesTagsScreen
import com.example.mobillsfinancas.utils.Formatters
import com.example.mobillsfinancas.utils.PermissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentMonthData by viewModel.currentMonthData.collectAsStateWithLifecycle()
    val months by viewModel.months.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val recentCapturedList by viewModel.recentCapturedTransactions.collectAsStateWithLifecycle()

    val sharedPrefs = remember { context.getSharedPreferences("hvfinancas_prefs", android.content.Context.MODE_PRIVATE) }
    var isNotificationListenerEnabled by remember {
        mutableStateOf(PermissionHelper.isNotificationListenerEnabled(context))
    }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var isXiaomiAutostartConfirmed by remember {
        mutableStateOf(sharedPrefs.getBoolean("xiaomi_autostart_confirmed", false))
    }
    var showNotificationHelpDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
                isBatteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMobillsTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionItem?>(null) }

    var isProfessorMode by remember {
        mutableStateOf(!sharedPrefs.getBoolean("modo_professor_concluido", false))
    }
    var professorStep by remember {
        mutableIntStateOf(if (isProfessorMode) 1 else 0)
    }

    val goToProfessorStep: (Int) -> Unit = { targetStep ->
        if (targetStep in 1..professorSteps.size) {
            val stepDef = professorSteps.find { it.step == targetStep }
            if (stepDef != null) {
                selectedTab = stepDef.targetTab
            }
            professorStep = targetStep
            isProfessorMode = true
        } else if (targetStep > professorSteps.size) {
            isProfessorMode = false
            professorStep = 0
            sharedPrefs.edit().putBoolean("modo_professor_concluido", true).apply()
        }
    }

    // Diálogos de edição
    var editingCard by remember { mutableStateOf<CreditCardItem?>(null) }
    var showAddCardDialog by remember { mutableStateOf(false) }
    var payingCard by remember { mutableStateOf<CreditCardItem?>(null) }

    var editingAccount by remember { mutableStateOf<AccountItem?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }
    var adjustBalanceInitialTab by remember { mutableIntStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDevTestDialog by remember { mutableStateOf(false) }

    var updateInfo by remember { mutableStateOf<com.example.mobillsfinancas.utils.UpdateInfo?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(-1f) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val info = com.example.mobillsfinancas.utils.AppUpdateManager.checkForUpdate()
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showAboutDialog = true }
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.mobillsfinancas.R.drawable.app_logo),
                            contentDescription = "Logo HV Finanças",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "HV Finanças",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MobillsGreenPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "v${com.example.mobillsfinancas.BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MobillsGreenPrimary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            val totalBalance = accounts.sumOf { it.balance }
                            Text(
                                text = "Saldo: ${Formatters.formatMoney(totalBalance, viewModel.hideValues)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            goToProfessorStep(1)
                        }
                    ) {
                        Text(
                            text = "🎓",
                            fontSize = 20.sp
                        )
                    }
                    IconButton(onClick = { showAboutDialog = true }) {
                        Text(
                            text = "ℹ️",
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = { viewModel.toggleHideValues() }) {
                        Text(
                            text = if (viewModel.hideValues) "👁️" else "🙈",
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Text("🏠", fontSize = 18.sp) },
                    label = { Text("Geral", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Text("📋", fontSize = 18.sp) },
                    label = { Text("Extrato", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Text("💳", fontSize = 18.sp) },
                    label = { Text("Cartões", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Text("🏷️", fontSize = 18.sp) },
                    label = { Text("Tags/Cat", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Text("🏦", fontSize = 18.sp) },
                    label = { Text("Bancos", fontSize = 10.sp) }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMobillsTransactionSheet = true },
                containerColor = MobillsGreenPrimary,
                contentColor = Color.White,
                shape = MaterialTheme.shapes.large
            ) {
                Text("+", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> DashboardTab(
                    currentMonthData = currentMonthData,
                    months = months,
                    selectedMonth = selectedMonth,
                    cards = cards,
                    accounts = accounts,
                    recentCapturedList = recentCapturedList,
                    isNotificationListenerEnabled = isNotificationListenerEnabled,
                    isBatteryOptimizationIgnored = isBatteryOptimizationIgnored,
                    hideValues = viewModel.hideValues,
                    onSelectMonth = { viewModel.selectMonth(it) },
                    getCardInvoice = { viewModel.getCardInvoice(it) },
                    onEditTransaction = { editingTransaction = it },
                    onToggleStatus = { viewModel.toggleStatus(it) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onEditCard = { editingCard = it },
                    onAddCard = { showAddCardDialog = true },
                    onEditAccount = { editingAccount = it },
                    onAddAccount = { showAddAccountDialog = true },
                    onTransfer = { showTransferDialog = true },
                    onReorderAccounts = { viewModel.reorderAccounts(it) },
                    onReorderCards = { viewModel.reorderCards(it) },
                    onAdjustBalance = { tab ->
                        adjustBalanceInitialTab = tab
                        showAdjustBalanceDialog = true
                    },
                    onApplyQuickInstallments = { id, inst ->
                        viewModel.applyQuickInstallments(id, inst)
                    },
                    onDismissQuickInstallments = { id ->
                        viewModel.dismissQuickInstallments(id)
                    },
                    onDismissAllQuickInstallments = {
                        viewModel.dismissQuickInstallments(null)
                    },
                    categories = categories,
                    onSetBudgetLimit = { catId, limit ->
                        viewModel.updateCategoryBudget(catId, limit)
                    },
                    onRequestNotificationPermission = {
                        showNotificationHelpDialog = true
                    }
                )
                1 -> TransactionsTab(
                    currentMonthData = currentMonthData,
                    months = months,
                    selectedMonth = selectedMonth,
                    hideValues = viewModel.hideValues,
                    onSelectMonth = { viewModel.selectMonth(it) },
                    onEditTransaction = { editingTransaction = it },
                    onToggleStatus = { viewModel.toggleStatus(it) },
                    onDeleteTransaction = { viewModel.deleteTransaction(it) },
                    onClearCurrentMonth = { viewModel.clearCurrentMonthTransactions() },
                    onMarkMultipleStatus = { ids, status -> viewModel.markTransactionsStatus(ids, status) },
                    onDeleteMultipleTransactions = { ids -> viewModel.deleteMultipleTransactions(ids) }
                )
                2 -> CardsTab(
                    cards = cards,
                    months = months,
                    currentMonth = selectedMonth,
                    currentMonthData = currentMonthData,
                    accounts = accounts,
                    hideValues = viewModel.hideValues,
                    onSelectMonth = { viewModel.selectMonth(it) },
                    onEditCard = { editingCard = it },
                    onAddCard = { showAddCardDialog = true },
                    onPayInvoice = { payingCard = it },
                    onEditTransaction = { editingTransaction = it },
                    onReorderCards = { viewModel.reorderCards(it) }
                )
                3 -> CategoriesTagsScreen(
                    categories = categories,
                    tags = tags,
                    onAddCategory = { viewModel.addCategory(it) },
                    onDeleteCategory = { viewModel.deleteCategory(it) },
                    onAddTag = { viewModel.addTag(it) },
                    onDeleteTag = { viewModel.deleteTag(it) }
                )
                4 -> BankIntegrationScreen(
                    currentMonth = selectedMonth,
                    onImportOfx = { content, target -> viewModel.importOfx(content, target) },
                    onImportCsv = { content, target -> viewModel.importCsv(content, target) },
                    onExportBackup = { viewModel.exportBackup() },
                    onImportBackup = { viewModel.importBackup(it) },
                    onClearCurrentMonth = { viewModel.clearCurrentMonthTransactions() },
                    onClearAllTransactions = { viewModel.clearAllTransactions() },
                    onStartProfessorMode = {
                        goToProfessorStep(1)
                    }
                )
            }

            // Overlay do Modo Professor (Tutorial Guiado Completo do App)
            if (isProfessorMode && professorStep in 1..professorSteps.size) {
                ProfessorTourOverlay(
                    currentStep = professorStep,
                    totalSteps = professorSteps.size,
                    onNextStep = { goToProfessorStep(professorStep + 1) },
                    onPrevStep = { goToProfessorStep(professorStep - 1) },
                    onOpenForm = { showMobillsTransactionSheet = true },
                    onSkipTour = {
                        isProfessorMode = false
                        professorStep = 0
                        sharedPrefs.edit().putBoolean("modo_professor_concluido", true).apply()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (selectedTab == 0) 80.dp else 16.dp)
                )
            }

            // Formulário de Nova Transação (Estilo Mobills)
            if (showMobillsTransactionSheet) {
                MobillsTransactionSheet(
                    existingItem = null,
                    currentMonth = selectedMonth,
                    categories = categories,
                    tags = tags,
                    cards = cards,
                    accounts = accounts,
                    isProfessorMode = isProfessorMode && (professorStep == 6 || professorStep == 1),
                    onDismissProfessor = {
                        showMobillsTransactionSheet = false
                        if (isProfessorMode) {
                            goToProfessorStep(7)
                        }
                    },
                    onDismiss = { showMobillsTransactionSheet = false },
                    onConfirm = { item, _, includePast, isAmountPerInst ->
                        viewModel.addTransaction(item, includePast, isAmountPerInst)
                        showMobillsTransactionSheet = false
                    },
                    onAddNewCategory = { showAddCategoryDialog = true },
                    onAddNewTag = { showAddTagDialog = true },
                    onOpenTransfer = { showTransferDialog = true },
                    onAddNewAccount = { showAddAccountDialog = true },
                    onAddNewCard = { showAddCardDialog = true }
                )
            }

            // Formulário de Editar Transação (Estilo Mobills)
            if (editingTransaction != null) {
                MobillsTransactionSheet(
                    existingItem = editingTransaction,
                    currentMonth = selectedMonth,
                    categories = categories,
                    tags = tags,
                    cards = cards,
                    accounts = accounts,
                    onDismiss = { editingTransaction = null },
                    onConfirm = { updatedItem, updateFuture, _, _ ->
                        viewModel.updateTransaction(updatedItem, editingTransaction?.monthYear, updateFuture)
                        editingTransaction = null
                    },
                    onDelete = { itemToDelete ->
                        viewModel.deleteTransaction(itemToDelete.id, itemToDelete.monthYear, true)
                        editingTransaction = null
                    },
                    onAddNewCategory = { showAddCategoryDialog = true },
                    onAddNewTag = { showAddTagDialog = true },
                    onAddNewAccount = { showAddAccountDialog = true },
                    onAddNewCard = { showAddCardDialog = true }
                )
            }

            // Diálogo de Pagamento de Fatura do Cartão
            if (payingCard != null) {
                val invoice = viewModel.getCardInvoice(payingCard!!)
                PayInvoiceDialog(
                    card = payingCard!!,
                    invoiceAmount = invoice,
                    accounts = accounts,
                    onDismiss = { payingCard = null },
                    onConfirmPay = { accountId, amount ->
                        viewModel.payCardInvoice(payingCard!!.name, accountId, amount)
                        payingCard = null
                    }
                )
            }

            // Diálogo de Editar Cartão
            if (editingCard != null) {
                EditCardDialog(
                    card = editingCard,
                    accounts = accounts,
                    existingCards = cards,
                    onDismiss = { editingCard = null },
                    onSave = { updated ->
                        viewModel.updateCard(updated)
                        editingCard = null
                    },
                    onDelete = { cardId ->
                        viewModel.deleteCard(cardId)
                        editingCard = null
                    }
                )
            }

            // Diálogo de Adicionar Cartão
            if (showAddCardDialog) {
                EditCardDialog(
                    card = null,
                    accounts = accounts,
                    existingCards = cards,
                    onDismiss = { showAddCardDialog = false },
                    onSave = { newCard ->
                        viewModel.addCard(newCard)
                        showAddCardDialog = false
                    }
                )
            }

            // Diálogo de Editar Conta
            if (editingAccount != null) {
                EditAccountDialog(
                    account = editingAccount,
                    onDismiss = { editingAccount = null },
                    onSave = { updated ->
                        viewModel.updateAccount(updated)
                        editingAccount = null
                    },
                    onDelete = { accId ->
                        viewModel.deleteAccount(accId)
                        editingAccount = null
                    }
                )
            }

            // Diálogo de Adicionar Conta
            if (showAddAccountDialog) {
                EditAccountDialog(
                    account = null,
                    onDismiss = { showAddAccountDialog = false },
                    onSave = { newAcc ->
                        viewModel.addAccount(newAcc)
                        showAddAccountDialog = false
                    }
                )
            }

            // Diálogo de Transferência
            if (showTransferDialog) {
                TransferDialog(
                    accounts = accounts,
                    onDismiss = { showTransferDialog = false },
                    onTransfer = { fromId, toId, amt ->
                        viewModel.transfer(fromId, toId, amt)
                        showTransferDialog = false
                    }
                )
            }

            // Diálogos auxiliares de Categoria e Tag
            if (showAddCategoryDialog) {
                com.example.mobillsfinancas.ui.screens.AddCategoryDialog(
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { cat ->
                        viewModel.addCategory(cat)
                        showAddCategoryDialog = false
                    }
                )
            }

            if (showAddTagDialog) {
                com.example.mobillsfinancas.ui.screens.AddTagDialog(
                    onDismiss = { showAddTagDialog = false },
                    onConfirm = { tag ->
                        viewModel.addTag(tag)
                        showAddTagDialog = false
                    }
                )
            }

            if (showAdjustBalanceDialog) {
                AdjustBalanceDialog(
                    selectedMonth = selectedMonth,
                    currentMonthData = currentMonthData,
                    accounts = accounts,
                    initialTab = adjustBalanceInitialTab,
                    onDismiss = { showAdjustBalanceDialog = false },
                    onAdjustMonthBalance = { target ->
                        viewModel.adjustMonthBalance(selectedMonth, target)
                    },
                    onSetBaseIncome = { base, applyFuture ->
                        viewModel.setMonthBaseIncome(selectedMonth, base, applyFuture)
                    },
                    onUpdateAccount = { acc ->
                        viewModel.updateAccount(acc)
                    }
                )
            }

            // Diálogo Explicativo e Ação para Notificações e Segundo Plano
            if (showNotificationHelpDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showNotificationHelpDialog = false
                        isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
                        isBatteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Captura Automática & Segundo Plano", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Para registrar automaticamente compras de cartão e Pix (Inter, Mercado Pago, Will, etc.) mesmo em segundo plano ou com a tela apagada:",
                                style = MaterialTheme.typography.bodySmall
                            )

                            // 1. Leitor de Notificações
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNotificationListenerEnabled) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isNotificationListenerEnabled) Color(0xFF86EFAC) else Color(0xFFFDE68A))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isNotificationListenerEnabled) "✅" else "🔔", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "1. Leitor de Notificações",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isNotificationListenerEnabled) Color(0xFF166534) else Color(0xFF92400E)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isNotificationListenerEnabled)
                                            "Permissão ativa! Notificações de bancos são lidas pelo app."
                                        else
                                            "Necessário para ler o valor, banco e descrição da compra/Pix.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isNotificationListenerEnabled) Color(0xFF166534) else Color(0xFF78350F)
                                    )
                                    if (!isNotificationListenerEnabled) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Autorizar Acesso a Notificações", fontSize = 12.sp)
                                        }

                                        if (Build.VERSION.SDK_INT >= 33) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFFEF2F2),
                                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(
                                                        text = "🔒 Se a chave estiver cinza ('Configuração restrita'):",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF991B1B)
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "O Android 13/14 exige liberar uma única vez:\n" +
                                                               "1. Toque em 'Tirar Restrições' abaixo.\n" +
                                                               "2. Toque nos 3 pontinhos (⋮) no topo direito da tela.\n" +
                                                               "3. Escolha 'Permitir configurações restritas'.\n" +
                                                               "4. Volte e ative a notificação normalmente!",
                                                        fontSize = 10.sp,
                                                        lineHeight = 14.sp,
                                                        color = Color(0xFF7F1D1D)
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    OutlinedButton(
                                                        onClick = { PermissionHelper.openAppDetails(context) },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 8.dp)
                                                    ) {
                                                        Text("⚙️ Tirar Restrições (Info do App)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Bateria sem restrições (Executar em segundo plano)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isBatteryOptimizationIgnored) Color(0xFFDCFCE7) else Color(0xFFEFF6FF)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isBatteryOptimizationIgnored) Color(0xFF86EFAC) else Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(if (isBatteryOptimizationIgnored) "✅" else "🔋", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "2. Executar em Segundo Plano",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isBatteryOptimizationIgnored) Color(0xFF166534) else Color(0xFF1E40AF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isBatteryOptimizationIgnored)
                                            "Bateria configurada com sucesso! A restrição foi removida e o Android não pausará o leitor em segundo plano."
                                        else
                                            "O que fazer: Toque no botão abaixo e, na tela do Android que se abrir, selecione a opção 'Sem restrições' (ou 'Não otimizar'). Isso impede que o sistema feche o leitor de gastos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isBatteryOptimizationIgnored) Color(0xFF166534) else Color(0xFF1E3A8A)
                                    )
                                    if (!isBatteryOptimizationIgnored) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Selecione a opção 'Sem restrições' para o HV Finanças",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                                PermissionHelper.requestIgnoreBatteryOptimizations(context)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Abrir Tela e Selecionar 'Sem Restrições'", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            // 3. Xiaomi / HyperOS / MIUI (Início Automático)
                            if (PermissionHelper.isXiaomiDevice()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isXiaomiAutostartConfirmed) Color(0xFFDCFCE7) else Color(0xFFFFF7ED)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (isXiaomiAutostartConfirmed) Color(0xFF86EFAC) else Color(0xFFFED7AA))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(if (isXiaomiAutostartConfirmed) "✅" else "🚀", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "3. Início Automático (Xiaomi)",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isXiaomiAutostartConfirmed) Color(0xFF166534) else Color(0xFF9A3412)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isXiaomiAutostartConfirmed)
                                                "Início automático configurado! O HV Finanças iniciará em segundo plano mesmo após reiniciar o celular."
                                            else
                                                "No HyperOS/MIUI da Xiaomi, ative 'Início Automático' para o HV Finanças capturar transações mesmo após reiniciar.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isXiaomiAutostartConfirmed) Color(0xFF166534) else Color(0xFF7C2D12)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val newState = !isXiaomiAutostartConfirmed
                                                    sharedPrefs.edit().putBoolean("xiaomi_autostart_confirmed", newState).apply()
                                                    isXiaomiAutostartConfirmed = newState
                                                }
                                        ) {
                                            Checkbox(
                                                checked = isXiaomiAutostartConfirmed,
                                                onCheckedChange = { checked ->
                                                    sharedPrefs.edit().putBoolean("xiaomi_autostart_confirmed", checked).apply()
                                                    isXiaomiAutostartConfirmed = checked
                                                }
                                            )
                                            Text(
                                                text = "Já ativei nas configurações da Xiaomi",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isXiaomiAutostartConfirmed) Color(0xFF166534) else Color(0xFF9A3412)
                                            )
                                        }
                                        if (!isXiaomiAutostartConfirmed) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            OutlinedButton(
                                                onClick = {
                                                    PermissionHelper.openXiaomiAutostart(context)
                                                    sharedPrefs.edit().putBoolean("xiaomi_autostart_confirmed", true).apply()
                                                    isXiaomiAutostartConfirmed = true
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Abrir Tela de Início Automático", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // Botão Configurações do App
                            OutlinedButton(
                                onClick = {
                                    PermissionHelper.openAppDetails(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⚙️ Informações do App (Configurações)", fontSize = 12.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showNotificationHelpDialog = false
                                isNotificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
                                isBatteryOptimizationIgnored = PermissionHelper.isIgnoringBatteryOptimizations(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("Concluído")
                        }
                    }
                )
            }

            // Diálogo Sobre o Aplicativo / Versão
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.mobillsfinancas.R.drawable.app_logo),
                                contentDescription = "Logo HV Finanças",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "HV Finanças",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "Controle Financeiro Pessoal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MobillsGreenPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Versão Atual",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "v${com.example.mobillsfinancas.BuildConfig.VERSION_NAME}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = MobillsGreenPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MobillsGreenPrimary
                                    ) {
                                        Text(
                                            text = "Build ${com.example.mobillsfinancas.BuildConfig.VERSION_CODE}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("👤", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Perfil: Finanças Pessoais",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔔", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Leitor de Bancos: Inter, MP, Nu, Sofisa, Caixa, Will",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🔒", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Dados: 100% Offline e Privados no Aparelho",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    showAboutDialog = false
                                    goToProfessorStep(1)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎓 Ativar Modo Professor (Tutorial Guiado)", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedButton(
                                onClick = {
                                    showAboutDialog = false
                                    showNotificationHelpDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("⚡ Status de Notificações & Segundo Plano", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isCheckingUpdate = true
                                        val info = com.example.mobillsfinancas.utils.AppUpdateManager.checkForUpdate()
                                        isCheckingUpdate = false
                                        if (info != null) {
                                            updateInfo = info
                                            showAboutDialog = false
                                            showUpdateDialog = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Você já está na versão mais recente (v${com.example.mobillsfinancas.BuildConfig.VERSION_NAME})!",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isCheckingUpdate) "Buscando atualizações..." else "🔄 Verificar Atualizações no GitHub",
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }

                            TextButton(
                                onClick = {
                                    showAboutDialog = false
                                    showDevTestDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔒 Área de Testes (Acesso Restrito)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("Fechar")
                        }
                    }
                )
            }

            if (showDevTestDialog) {
                com.example.mobillsfinancas.ui.components.DevTestDialog(
                    onDismiss = { showDevTestDialog = false }
                )
            }

            // Diálogo de Atualização Automática
            if (showUpdateDialog && updateInfo != null) {
                val info = updateInfo!!
                AlertDialog(
                    onDismissRequest = {
                        if (downloadProgress < 0f) {
                            showUpdateDialog = false
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚀", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Nova Versão Disponível!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Uma nova versão do HV Finanças está disponível no GitHub:",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MobillsGreenPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Versão: v${info.latestVersionName}",
                                        fontWeight = FontWeight.Bold,
                                        color = MobillsGreenPrimary
                                    )
                                    Text(
                                        text = "Atual: v${com.example.mobillsfinancas.BuildConfig.VERSION_NAME}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (info.changelog.isNotBlank()) {
                                Text(
                                    text = "Novidades:",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = info.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (downloadProgress >= 0f) {
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MobillsGreenPrimary
                                )
                                Text(
                                    text = "Baixando atualização: ${downloadProgress.toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MobillsGreenPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (updateError != null) {
                                Text(
                                    text = "Erro: $updateError",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (downloadProgress < 0f) {
                                    downloadProgress = 0f
                                    updateError = null
                                    coroutineScope.launch {
                                        com.example.mobillsfinancas.utils.AppUpdateManager.downloadAndInstallApk(
                                            context = context,
                                            downloadUrl = info.downloadUrl,
                                            onProgress = { p -> downloadProgress = p.toFloat() },
                                            onError = { err ->
                                                downloadProgress = -1f
                                                updateError = err
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = downloadProgress < 0f,
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text(if (downloadProgress >= 0f) "Baixando..." else "Atualizar Agora")
                        }
                    },
                    dismissButton = {
                        if (downloadProgress < 0f) {
                            TextButton(onClick = { showUpdateDialog = false }) {
                                Text("Lembrar Mais Tarde")
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardTab(
    currentMonthData: com.example.mobillsfinancas.data.MonthData?,
    months: List<String>,
    selectedMonth: String,
    cards: List<CreditCardItem>,
    accounts: List<AccountItem>,
    recentCapturedList: List<TransactionItem>,
    isNotificationListenerEnabled: Boolean,
    isBatteryOptimizationIgnored: Boolean,
    hideValues: Boolean,
    onSelectMonth: (String) -> Unit,
    getCardInvoice: (String) -> Double,
    onEditTransaction: (TransactionItem) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onEditCard: (CreditCardItem) -> Unit,
    onAddCard: () -> Unit,
    onEditAccount: (AccountItem) -> Unit,
    onAddAccount: () -> Unit,
    onTransfer: () -> Unit,
    onReorderAccounts: ((List<AccountItem>) -> Unit)? = null,
    onReorderCards: ((List<CreditCardItem>) -> Unit)? = null,
    onAdjustBalance: (Int) -> Unit,
    onApplyQuickInstallments: (String, Int) -> Unit,
    onDismissQuickInstallments: (String) -> Unit,
    onDismissAllQuickInstallments: () -> Unit = {},
    categories: List<com.example.mobillsfinancas.data.CategoryItem> = emptyList(),
    onSetBudgetLimit: (String, Double) -> Unit = { _, _ -> },
    onRequestNotificationPermission: () -> Unit
) {
    val items = currentMonthData?.items ?: emptyList()
    val totalExp = currentMonthData?.totalExpense ?: 0.0
    val totalInc = currentMonthData?.totalIncome ?: 3500.0
    val remaining = currentMonthData?.remainingBalance ?: 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            MonthSelector(
                months = months,
                currentMonth = selectedMonth,
                onSelectMonth = onSelectMonth
            )
        }

        // Banner de ativação de notificações e segundo plano (se faltar alguma permissão)
        if (!isNotificationListenerEnabled || !isBatteryOptimizationIgnored) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onRequestNotificationPermission() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (!isNotificationListenerEnabled) "🔔" else "⚡", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!isNotificationListenerEnabled)
                                    "Ativar Leitor de Notificações de Bancos"
                                else
                                    "Permitir Execução em Segundo Plano",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = if (!isNotificationListenerEnabled)
                                    "Toque para autorizar a captura automática de Pix e compras de cartão."
                                else
                                    "Toque para liberar o app a capturar gastos com a tela desligada.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                        Text("👉", fontSize = 16.sp)
                    }
                }
            }
        }

        // Cards de Confirmação Rápida de Parcelas (com 1 clique) para compras capturadas
        if (recentCapturedList.isNotEmpty()) {
            if (recentCapturedList.size > 1) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ ${recentCapturedList.size} Lançamentos Capturados",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MobillsGreenPrimary
                        )
                        TextButton(
                            onClick = onDismissAllQuickInstallments,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Dispensar Todos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            items(recentCapturedList, key = { it.id }) { capturedItem ->
                QuickInstallmentCard(
                    item = capturedItem,
                    onSelectInstallments = { inst ->
                        onApplyQuickInstallments(capturedItem.id, inst)
                    },
                    onDismiss = {
                        onDismissQuickInstallments(capturedItem.id)
                    },
                    onOpenDetails = {
                        onEditTransaction(capturedItem)
                        onDismissQuickInstallments(capturedItem.id)
                    }
                )
            }
        }

        item {
            OverviewCard(
                totalIncome = totalInc,
                totalExpense = totalExp,
                remainingBalance = remaining,
                previousMonthBalance = currentMonthData?.previousMonthBalance ?: 0.0,
                currentMonthIncome = currentMonthData?.currentMonthIncome ?: 0.0,
                hideValues = hideValues,
                onAdjustBalance = { onAdjustBalance(0) }
            )
        }

        // Seção Bancos & Contas (Controle de Saldo estilo Mobills)
        item {
            AccountsSection(
                accounts = accounts,
                hideValues = hideValues,
                onEditAccount = onEditAccount,
                onAddAccount = onAddAccount,
                onTransfer = onTransfer,
                onAdjustBalance = { onAdjustBalance(1) },
                onReorderAccounts = onReorderAccounts
            )
        }

        // Carrossel de Cartões de Crédito com botão de editar
        item {
            CardsCarousel(
                cards = cards,
                currentMonth = selectedMonth,
                currentMonthData = currentMonthData,
                accounts = accounts,
                hideValues = hideValues,
                onEditCard = onEditCard,
                onAddCard = onAddCard,
                onReorderCards = onReorderCards
            )
        }

        item {
            CategoryBreakdown(
                items = items,
                categories = categories,
                totalExpense = totalExp,
                hideValues = hideValues,
                onSetBudgetLimit = onSetBudgetLimit,
                onEditTransaction = onEditTransaction
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lançamentos de $selectedMonth (${items.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(items, key = { "${it.id}_${it.accountOrCardName}_${it.amount}_${it.status}_${it.date}" }) { item ->
            TransactionItemView(
                item = item,
                hideValues = hideValues,
                onEdit = onEditTransaction,
                onToggleStatus = onToggleStatus,
                onDelete = onDeleteTransaction
            )
        }
    }
}

@Composable
fun TransactionsTab(
    currentMonthData: com.example.mobillsfinancas.data.MonthData?,
    months: List<String>,
    selectedMonth: String,
    hideValues: Boolean,
    onSelectMonth: (String) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onClearCurrentMonth: () -> Unit,
    onMarkMultipleStatus: (Set<String>, PaymentStatus) -> Unit,
    onDeleteMultipleTransactions: (Set<String>) -> Unit
) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Todos", "Despesas", "Receitas", "Pendentes", "Pagos")
    var showClearMonthDialog by remember { mutableStateOf(false) }
    var isBulkMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }

    val allItems = currentMonthData?.items ?: emptyList()

    val filteredItems = remember(allItems, filterIndex) {
        when (filterIndex) {
            1 -> allItems.filter { it.type == TransactionType.EXPENSE }
            2 -> allItems.filter { it.type == TransactionType.INCOME }
            3 -> allItems.filter { it.status == PaymentStatus.PENDING }
            4 -> allItems.filter { it.status == PaymentStatus.PAID }
            else -> allItems
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (isBulkMode && selectedIds.isNotEmpty()) 140.dp else 80.dp)
        ) {
            item {
                MonthSelector(
                    months = months,
                    currentMonth = selectedMonth,
                    onSelectMonth = onSelectMonth
                )
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = filterIndex,
                    edgePadding = 16.dp,
                    divider = {},
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    filters.forEachIndexed { index, title ->
                        Tab(
                            selected = filterIndex == index,
                            onClick = {
                                filterIndex = index
                                selectedIds = emptySet()
                            },
                            text = { Text(title, fontWeight = if (filterIndex == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lançamentos (${filteredItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (allItems.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    isBulkMode = !isBulkMode
                                    selectedIds = emptySet()
                                }
                            ) {
                                Text(
                                    text = if (isBulkMode) "✕ Fechar" else "☑️ Alteração em Massa",
                                    color = if (isBulkMode) com.example.mobillsfinancas.theme.ExpenseRed else MobillsGreenPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!isBulkMode) {
                                TextButton(onClick = { showClearMonthDialog = true }) {
                                    Text(
                                        text = "🗑️ Limpar Mês",
                                        color = com.example.mobillsfinancas.theme.ExpenseRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Toolbar secundária no modo em massa
            if (isBulkMode && filteredItems.isNotEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selecionados: ${selectedIds.size} de ${filteredItems.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row {
                                if (selectedIds.size < filteredItems.size) {
                                    TextButton(onClick = { selectedIds = filteredItems.map { it.id }.toSet() }) {
                                        Text("Selecionar Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)
                                    }
                                } else {
                                    TextButton(onClick = { selectedIds = emptySet() }) {
                                        Text("Desmarcar Todos", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum lançamento encontrado para este filtro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredItems, key = { "${it.id}_${it.accountOrCardName}_${it.amount}_${it.status}_${it.date}" }) { item ->
                    TransactionItemView(
                        item = item,
                        hideValues = hideValues,
                        onEdit = onEditTransaction,
                        onToggleStatus = onToggleStatus,
                        onDelete = onDeleteTransaction,
                        isSelectionMode = isBulkMode,
                        isSelected = selectedIds.contains(item.id),
                        onToggleSelect = { id ->
                            selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                        }
                    )
                }
            }
        }

        // Barra inferior flutuante de ações em massa quando há itens selecionados
        if (isBulkMode && selectedIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onMarkMultipleStatus(selectedIds, PaymentStatus.PAID)
                            selectedIds = emptySet()
                            isBulkMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.IncomeGreen),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("✓ Pago / Recebido (${selectedIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                    }

                    FilledTonalButton(
                        onClick = {
                            onMarkMultipleStatus(selectedIds, PaymentStatus.PENDING)
                            selectedIds = emptySet()
                            isBulkMode = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⏳ Pendente / A Receber", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showBulkDeleteConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = com.example.mobillsfinancas.theme.ExpenseRed)
                    ) {
                        Text("🗑️ Excluir", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Diálogo Limpar Mês
        if (showClearMonthDialog) {
            AlertDialog(
                onDismissRequest = { showClearMonthDialog = false },
                title = { Text("Limpar Lançamentos do Mês?") },
                text = {
                    Text("Deseja apagar todos os lançamentos do mês de $selectedMonth para lançar manualmente? Suas contas e cartões continuarão salvos.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearMonthDialog = false
                            onClearCurrentMonth()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.ExpenseRed)
                    ) {
                        Text("Sim, Limpar Mês", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearMonthDialog = false }) { Text("Cancelar") }
                }
            )
        }

        // Diálogo Excluir Selecionados em Massa
        if (showBulkDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteConfirmDialog = false },
                title = { Text("Excluir ${selectedIds.size} Lançamentos?") },
                text = {
                    Text("Tem certeza que deseja apagar os ${selectedIds.size} lançamentos selecionados?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBulkDeleteConfirmDialog = false
                            onDeleteMultipleTransactions(selectedIds)
                            selectedIds = emptySet()
                            isBulkMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = com.example.mobillsfinancas.theme.ExpenseRed)
                    ) {
                        Text("Excluir Selecionados", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteConfirmDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun CardsTab(
    cards: List<CreditCardItem>,
    months: List<String>,
    currentMonth: String,
    currentMonthData: com.example.mobillsfinancas.data.MonthData?,
    accounts: List<AccountItem>,
    hideValues: Boolean,
    onSelectMonth: (String) -> Unit,
    onEditCard: (CreditCardItem) -> Unit,
    onAddCard: () -> Unit,
    onPayInvoice: (CreditCardItem) -> Unit,
    onEditTransaction: (TransactionItem) -> Unit,
    onReorderCards: ((List<CreditCardItem>) -> Unit)? = null
) {
    var showReorderCardsDialog by remember { mutableStateOf(false) }
    val items = currentMonthData?.items ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gestão de Cartões",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MobillsGreenPrimary
                    )
                    Text(
                        text = "Faturas, limites, vencimentos e parcelamentos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (cards.size > 1 && onReorderCards != null) {
                        OutlinedButton(
                            onClick = { showReorderCardsDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("⇅ Ordenar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = onAddCard,
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                    ) {
                        Text("+ Cartão", color = Color.White)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        item {
            MonthSelector(
                months = months,
                currentMonth = currentMonth,
                onSelectMonth = onSelectMonth
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(cards, key = { "${it.id}_${it.name}_${it.colorHex}_${it.limit}_${it.cardType}_$currentMonth" }) { card ->
            val cardItems = items.filter { isTransactionForCard(it, card, accounts) }
            val invoice = cardItems.sumOf { it.amount }
            val availableLimit = (card.limit - invoice).coerceAtLeast(0.0)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header do Cartão
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = card.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(card.colorHex),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(card.colorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${card.cardType.icon} ${card.cardType.badgeText}",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(card.colorHex)
                                    )
                                }
                            }
                            val linkedAcc = accounts.find { it.id == card.linkedAccountId }
                            if (linkedAcc != null) {
                                Text(
                                    text = "🔗 Conta: ${linkedAcc.bankIcon} ${linkedAcc.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (card.cardType.allowsCredit) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(card.colorHex).copy(alpha = 0.12f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "Fech: dia ${card.closingDay}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(card.colorHex)
                                    )
                                    Text(
                                        text = "Venc: dia ${card.dueDay}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(card.colorHex)
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(card.colorHex).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Débito em Conta",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(card.colorHex)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Linha com Fatura e Limite
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = if (card.cardType.allowsCredit) "Fatura de $currentMonth" else "Gastos de $currentMonth no Débito",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = Formatters.formatMoney(invoice, hideValues),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (invoice > 0) Color(card.colorHex) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (card.cardType.allowsCredit) {
                                Text("Limite Disponível", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(availableLimit, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                                Text(
                                    text = "Total: ${Formatters.formatMoney(card.limit, hideValues)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                val linkedAcc = accounts.find { it.id == card.linkedAccountId }
                                Text("Saldo Disponível", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatMoney(linkedAcc?.balance ?: 0.0, hideValues),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary
                                )
                                Text(
                                    text = linkedAcc?.name ?: "Conta Vinculada",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Barra de Consumo do Limite (quando cartão de crédito com limite > 0)
                    if (card.cardType.allowsCredit && card.limit > 0) {
                        val limitUsage = (invoice / card.limit).toFloat().coerceIn(0f, 1f)
                        val usagePercent = ((invoice / card.limit) * 100).toInt()
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Consumo do limite: $usagePercent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (usagePercent > 85) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (usagePercent > 85) {
                                    Text(
                                        text = "⚠️ Próximo ao limite",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ExpenseRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { limitUsage },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (usagePercent > 85) ExpenseRed else Color(card.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botões de Ação do Cartão (Estilo Mobills)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (card.cardType.allowsCredit && invoice > 0) {
                            Button(
                                onClick = { onPayInvoice(card) },
                                colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💳 Pagar Fatura", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { onEditCard(card) },
                            modifier = if (card.cardType.allowsCredit && invoice > 0) Modifier.wrapContentWidth() else Modifier.fillMaxWidth()
                        ) {
                            Text("✏️ Editar Cartão")
                        }
                    }

                    if (cardItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Compras deste cartão em $currentMonth (${cardItems.size})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque para editar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        cardItems.forEach { item ->
                            val cleanDesc = item.description.replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()
                            val displayDate = if (item.date.isNotBlank()) {
                                item.date.replace("Vencimento: ", "").replace("Venc: ", "").trim()
                            } else {
                                item.monthYear
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onEditTransaction(item) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Ícone + Descrição + Categoria/Data
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color(item.categoryColorHex).copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(item.categoryIcon, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = cleanDesc,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (item.totalInstallments > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text(
                                                            text = "${item.currentInstallment}/${item.totalInstallments}",
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (displayDate.isNotBlank()) {
                                                    Text(
                                                        text = "📅 $displayDate",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = " • ",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = item.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Valor + Badge de Status em Português
                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = Formatters.formatMoney(item.amount, hideValues),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.type == TransactionType.INCOME) IncomeGreen else ExpenseRed
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (item.status) {
                                                PaymentStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                                                PaymentStatus.PENDING -> ExpenseRed.copy(alpha = 0.15f)
                                                PaymentStatus.TO_RECEIVE -> Color(0xFF0288D1).copy(alpha = 0.15f)
                                            }
                                        ) {
                                            Text(
                                                text = when (item.status) {
                                                    PaymentStatus.PAID -> "PAGO"
                                                    PaymentStatus.PENDING -> "PENDENTE"
                                                    PaymentStatus.TO_RECEIVE -> "A RECEBER"
                                                },
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                color = when (item.status) {
                                                    PaymentStatus.PAID -> IncomeGreen
                                                    PaymentStatus.PENDING -> ExpenseRed
                                                    PaymentStatus.TO_RECEIVE -> Color(0xFF0288D1)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showReorderCardsDialog) {
        ReorderCardsDialog(
            cards = cards,
            onDismiss = { showReorderCardsDialog = false },
            onSaveOrder = { updated ->
                onReorderCards?.invoke(updated)
                showReorderCardsDialog = false
            }
        )
    }
}
