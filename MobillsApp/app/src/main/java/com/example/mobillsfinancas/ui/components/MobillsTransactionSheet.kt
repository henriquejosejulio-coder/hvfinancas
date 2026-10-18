package com.example.mobillsfinancas.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import com.example.mobillsfinancas.data.*
import com.example.mobillsfinancas.theme.ExpenseRed
import com.example.mobillsfinancas.theme.IncomeGreen
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters
import com.example.mobillsfinancas.utils.MonthHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobillsTransactionSheet(
    existingItem: TransactionItem? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    currentMonth: String,
    categories: List<CategoryItem>,
    tags: List<TagItem>,
    cards: List<CreditCardItem>,
    accounts: List<AccountItem>,
    isProfessorMode: Boolean = false,
    onDismissProfessor: () -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: (TransactionItem, Boolean, Boolean, Boolean) -> Unit, // item, updateFutureInstallments, includePreviousInstallments, isAmountPerInstallment
    onDelete: ((TransactionItem) -> Unit)? = null,
    onAddNewCategory: () -> Unit,
    onAddNewTag: () -> Unit,
    onOpenTransfer: (() -> Unit)? = null,
    onAddNewAccount: (() -> Unit)? = null,
    onAddNewCard: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(existingItem?.type ?: initialType) }
    var amountText by remember {
        mutableStateOf(
            if (existingItem != null && existingItem.amount > 0) {
                Formatters.formatCurrencyPlain(existingItem.amount)
            } else ""
        )
    }
    var dateText by remember {
        mutableStateOf(
            if (!existingItem?.date.isNullOrBlank()) {
                existingItem!!.date
            } else {
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            }
        )
    }
    var description by remember { mutableStateOf(existingItem?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            if (existingItem != null) {
                categories.find { it.name.equals(existingItem.category, ignoreCase = true) }
                    ?: categories.first()
            } else {
                categories.firstOrNull { if (type == TransactionType.EXPENSE) it.isExpense else !it.isExpense }
                    ?: categories.first()
            }
        )
    }
    var selectedPaymentType by remember {
        mutableStateOf(
            existingItem?.paymentType ?: if (type == TransactionType.EXPENSE) PaymentType.CREDIT_CARD else PaymentType.PIX
        )
    }
    val defaultAccName = accounts.firstOrNull { it.isDefault }?.name ?: accounts.firstOrNull()?.name ?: "Conta Principal"
    var selectedAccountOrCard by remember {
        mutableStateOf(
            existingItem?.accountOrCardName ?: if (selectedPaymentType == PaymentType.CREDIT_CARD && cards.isNotEmpty()) {
                cards.first().name
            } else {
                defaultAccName
            }
        )
    }
    var isRecurring by remember {
        mutableStateOf(existingItem?.isRecurring ?: false)
    }
    var selectedTags by remember { mutableStateOf(existingItem?.tags?.toSet() ?: setOf<String>()) }
    var isPaid by remember { mutableStateOf(existingItem?.status == PaymentStatus.PAID) }
    var totalInstallmentsCount by remember {
        mutableIntStateOf(if ((existingItem?.totalInstallments ?: 1) > 0) existingItem?.totalInstallments ?: 1 else 1)
    }
    var startInstallment by remember {
        mutableIntStateOf(if ((existingItem?.currentInstallment ?: 1) > 0) existingItem?.currentInstallment ?: 1 else 1)
    }
    var isAmountPerInstallment by remember { mutableStateOf(true) }
    var includePreviousInstallments by remember { mutableStateOf(false) }
    var updateFutureInstallments by remember { mutableStateOf(if (existingItem != null) false else true) }
    var note by remember { mutableStateOf(existingItem?.note ?: "") }
    var includeInBudget by remember { mutableStateOf(existingItem?.includeInBudget ?: true) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    var professorSheetStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(professorSheetStep) {
        if (isProfessorMode) {
            val targetScroll = when (professorSheetStep) {
                1 -> 0
                2 -> 0
                3 -> 350
                4 -> 680
                5 -> 1080
                6 -> 1600
                else -> 0
            }
            scrollState.animateScrollTo(targetScroll)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 10.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Banner do Modo Professor no Topo do Formulário
                if (isProfessorMode) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                        border = BorderStroke(2.dp, MobillsGreenPrimary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("👨‍🏫", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Modo Professor • Passo $professorSheetStep de 6",
                                            fontWeight = FontWeight.Bold,
                                            color = MobillsGreenPrimary,
                                            fontSize = 12.sp
                                        )
                                        val stepTitle = when (professorSheetStep) {
                                            1 -> "1. Valor da Transação (R$)"
                                            2 -> "2. Tipo (Despesa, Receita ou Transferir)"
                                            3 -> "3. Categoria & Descrição"
                                            4 -> "4. Forma de Pagamento & Conta/Cartão"
                                            5 -> "5. Fixa Todo Mês ou Parcelamento"
                                            6 -> "6. Conferir & Salvar Lançamento"
                                            else -> ""
                                        }
                                        Text(
                                            text = stepTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onDismissProfessor,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val stepExplanation = when (professorSheetStep) {
                                1 -> "Digite o valor em reais (ex: 50,00). O valor é o centro do seu controle e atualiza seus saldos e faturas instantaneamente."
                                2 -> "Defina se o dinheiro está SAINDO (Despesa), ENTRANDO (Receita) ou se é uma TRANSFERÊNCIA entre duas contas suas."
                                3 -> "Dê um nome (ex: Supermercado) e toque na categoria correspondente. As categorias organizam seus gráficos e tetos orçamentários!"
                                4 -> "Escolha como pagou: Cartão de Crédito (vai para a fatura), Débito/Conta (desconta do saldo), Pix ou Dinheiro. Suporta cartões físicos, virtuais e contas!"
                                5 -> "Se for um valor mensal (salário, aluguel, internet), marque 'Fixa Todo Mês'. Se for compra parcelada no cartão, defina a quantidade de parcelas (ex: 10x) e a parcela inicial!"
                                6 -> "Confira se a transação já está paga ou pendente, adicione observações ou tags (#) e toque no botão verde 'Salvar Lançamento' para concluir! Parabéns, você dominou o formulário!"
                                else -> ""
                            }

                            Text(
                                text = stepExplanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { professorSheetStep / 6f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MobillsGreenPrimary,
                                trackColor = MobillsGreenPrimary.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (professorSheetStep > 1) {
                                    OutlinedButton(
                                        onClick = { professorSheetStep-- },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("◀ Anterior", fontSize = 12.sp)
                                    }
                                } else {
                                    TextButton(onClick = onDismissProfessor) {
                                        Text("Pular Tutorial", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (professorSheetStep < 6) {
                                    Button(
                                        onClick = { professorSheetStep++ },
                                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Próximo ▶", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = onDismissProfessor,
                                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Continuar Tour pelo App ➔", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Header com valor grande estilo Mobills
                val headerBg = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBg)
                        .padding(18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (existingItem == null) {
                                if (type == TransactionType.EXPENSE) "Nova Despesa" else "Nova Receita"
                            } else {
                                if (type == TransactionType.EXPENSE) "Editar Despesa" else "Editar Receita"
                            },
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Passo 2 do Modo Professor
                        if (isProfessorMode && professorSheetStep == 2) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF08A),
                                border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "👈 PASSO 2: Escolha Despesa, Receita ou Transferir",
                                    color = Color(0xFF854D0E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Switch Tipo (Despesa / Receita / Transferir)
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Despesa",
                                color = Color.White,
                                fontWeight = if (type == TransactionType.EXPENSE) FontWeight.ExtraBold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (type == TransactionType.EXPENSE) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        type = TransactionType.EXPENSE
                                        selectedPaymentType = PaymentType.CREDIT_CARD
                                        if (cards.isNotEmpty()) selectedAccountOrCard = cards.first().name
                                        if (!selectedCategory.isExpense) {
                                            selectedCategory = categories.firstOrNull { it.isExpense } ?: selectedCategory
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            Text(
                                text = "Receita",
                                color = Color.White,
                                fontWeight = if (type == TransactionType.INCOME) FontWeight.ExtraBold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (type == TransactionType.INCOME) Color.White.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        type = TransactionType.INCOME
                                        selectedPaymentType = PaymentType.PIX
                                        selectedAccountOrCard = defaultAccName
                                        if (selectedCategory.isExpense) {
                                            selectedCategory = categories.firstOrNull { !it.isExpense } ?: selectedCategory
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                            if (existingItem == null && onOpenTransfer != null) {
                                Text(
                                    text = "⇄ Transferir",
                                    color = Color.White,
                                    fontWeight = FontWeight.Normal,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .background(Color.Transparent)
                                        .clickable {
                                            onDismiss()
                                            onOpenTransfer()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Passo 1 do Modo Professor
                        if (isProfessorMode && professorSheetStep == 1) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF08A),
                                border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                                modifier = Modifier.padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "👈 PASSO 1: Digite o valor aqui",
                                    color = Color(0xFF854D0E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text("Valor", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)

                        // Input Grande de Valor com Máscara de Moeda (Pontos no milhar e vírgula nos centavos)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("R$ ", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = { input ->
                                    amountText = Formatters.formatCurrencyInput(input)
                                },
                                placeholder = { Text("0,00", color = Color.White.copy(alpha = 0.6f), fontSize = 32.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Start),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                        }

                        // Atalhos rápidos para valores
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.clickable {
                                    val digits = amountText.filter { it.isDigit() }
                                    if (digits.isNotBlank()) {
                                        amountText = Formatters.formatCurrencyInput(digits + "00")
                                    }
                                }
                            ) {
                                Text(
                                    text = "+,00",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.clickable {
                                    val currentVal = Formatters.parseCurrency(amountText)
                                    amountText = Formatters.formatCurrencyPlain(currentVal + 10.0)
                                }
                            ) {
                                Text(
                                    text = "+10",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.clickable {
                                    val currentVal = Formatters.parseCurrency(amountText)
                                    amountText = Formatters.formatCurrencyPlain(currentVal + 50.0)
                                }
                            ) {
                                Text(
                                    text = "+50",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.22f),
                                modifier = Modifier.clickable {
                                    val currentVal = Formatters.parseCurrency(amountText)
                                    amountText = Formatters.formatCurrencyPlain(currentVal + 100.0)
                                }
                            ) {
                                Text(
                                    text = "+100",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (amountText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.22f),
                                    modifier = Modifier.clickable { amountText = "" }
                                ) {
                                    Text(
                                        text = "Limpar ⌫",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Corpo do formulário
                Column(modifier = Modifier.padding(18.dp)) {

                    // Passo 3 do Modo Professor
                    if (isProfessorMode && professorSheetStep == 3) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF08A),
                            border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "👈 PASSO 3: Digite a Descrição e selecione a Categoria",
                                color = Color(0xFF854D0E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Campo Descrição
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        placeholder = { Text("Ex: Supermercado, Aluguel, Posto") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Campo Data do Lançamento
                    val sheetContext = LocalContext.current
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                val parts = dateText.split("/")
                                val y = if (parts.size == 3) parts[2].toIntOrNull() ?: cal.get(Calendar.YEAR) else cal.get(Calendar.YEAR)
                                val m = if (parts.size == 3) ((parts[1].toIntOrNull() ?: 1) - 1).coerceIn(0, 11) else cal.get(Calendar.MONTH)
                                val d = if (parts.size == 3) (parts[0].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)).coerceIn(1, 31) else cal.get(Calendar.DAY_OF_MONTH)
                                DatePickerDialog(sheetContext, { _, year, month, dayOfMonth ->
                                    dateText = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                                }, y, m, d).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📅", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Data do Lançamento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = dateText.ifBlank { "Selecionar data" },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MobillsGreenPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Alterar 🗓️",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobillsGreenPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seção de Categoria (Estilo Mobills com ícone redondo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Categoria", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onAddNewCategory) {
                            Text("+ Nova Categoria", color = MobillsGreenPrimary, fontSize = 11.sp)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(categories.filter { if (type == TransactionType.EXPENSE) it.isExpense else true }) { cat ->
                            val isSelected = selectedCategory.id == cat.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { selectedCategory = cat }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex).copy(alpha = if (isSelected) 1f else 0.2f))
                                        .then(
                                            if (isSelected) Modifier.border(2.dp, Color.Black, CircleShape) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.iconEmoji, fontSize = 20.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(cat.colorHex) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Passo 4 do Modo Professor
                    if (isProfessorMode && professorSheetStep == 4) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF08A),
                            border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "👈 PASSO 4: Escolha a Forma de Pagamento e Conta/Cartão",
                                color = Color(0xFF854D0E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Seleção de Pagamento: Cartão de Crédito, Débito, Pix, Dinheiro
                    if (type == TransactionType.EXPENSE) {
                        Text("Forma de Pagamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.CREDIT_CARD,
                                    onClick = {
                                        selectedPaymentType = PaymentType.CREDIT_CARD
                                        if (cards.isNotEmpty() && !cards.any { it.name == selectedAccountOrCard }) {
                                            selectedAccountOrCard = cards.first().name
                                        }
                                    },
                                    label = { Text("💳 Cartão de Crédito") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.DEBIT_CARD,
                                    onClick = {
                                        selectedPaymentType = PaymentType.DEBIT_CARD
                                        if (accounts.isNotEmpty() && !accounts.any { it.name == selectedAccountOrCard }) {
                                            selectedAccountOrCard = accounts.first().name
                                        }
                                    },
                                    label = { Text("🏦 Débito / Conta") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.PIX,
                                    onClick = {
                                        selectedPaymentType = PaymentType.PIX
                                        if (accounts.isNotEmpty() && !accounts.any { it.name == selectedAccountOrCard }) {
                                            selectedAccountOrCard = accounts.first().name
                                        }
                                    },
                                    label = { Text("⚡ Pix") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.CASH,
                                    onClick = {
                                        selectedPaymentType = PaymentType.CASH
                                        val wallet = accounts.find { it.type == AccountType.WALLET } ?: accounts.firstOrNull()
                                        selectedAccountOrCard = wallet?.name ?: "Dinheiro / Carteira"
                                    },
                                    label = { Text("💵 Dinheiro") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Seletor do Cartão ou Conta dependendo da forma de pagamento
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (selectedPaymentType == PaymentType.CREDIT_CARD) {
                                val creditCards = cards.filter { it.cardType.allowsCredit }.ifEmpty { cards }
                                items(creditCards) { card ->
                                    val isSelected = selectedAccountOrCard == card.name
                                    val linkedAcc = accounts.find { it.id == card.linkedAccountId }
                                    val duplicateCount = cards.count { it.name.trim().equals(card.name.trim(), ignoreCase = true) }
                                    val chipLabel = if (duplicateCount > 1 && linkedAcc != null) {
                                        "${card.name} (${linkedAcc.name})"
                                    } else if (card.cardType != CardType.CREDIT) {
                                        "${card.name} (${card.cardType.label})"
                                    } else {
                                        card.name
                                    }
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAccountOrCard = card.name },
                                        leadingIcon = {
                                            Text(if (card.cardType.isVirtual) "🌐" else "💳", fontSize = 12.sp)
                                        },
                                        label = {
                                            Text(text = chipLabel)
                                        }
                                    )
                                }
                                if (onAddNewCard != null) {
                                    item {
                                        FilterChip(
                                            selected = false,
                                            onClick = onAddNewCard,
                                            label = { Text("+ Novo Cartão") }
                                        )
                                    }
                                }
                            } else {
                                items(accounts) { acc ->
                                    val isSelected = selectedAccountOrCard == acc.name
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedAccountOrCard = acc.name },
                                        label = { Text("${acc.bankIcon} ${acc.name}") }
                                    )
                                }
                                if (selectedPaymentType == PaymentType.DEBIT_CARD) {
                                    val debitCards = cards.filter { it.cardType.allowsDebit }
                                    items(debitCards) { card ->
                                        val isSelected = selectedAccountOrCard == card.name
                                        val linkedAcc = accounts.find { it.id == card.linkedAccountId }
                                        val duplicateCount = cards.count { it.name.trim().equals(card.name.trim(), ignoreCase = true) }
                                        val chipLabel = if (duplicateCount > 1 && linkedAcc != null) {
                                            "${card.name} (${linkedAcc.name})"
                                        } else {
                                            "${card.name} (${card.cardType.label})"
                                        }
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedAccountOrCard = card.name },
                                            leadingIcon = {
                                                Text(if (card.cardType.isVirtual) "🌐" else "💳", fontSize = 12.sp)
                                            },
                                            label = { Text(chipLabel) }
                                        )
                                    }
                                }
                                if (onAddNewAccount != null) {
                                    item {
                                        FilterChip(
                                            selected = false,
                                            onClick = onAddNewAccount,
                                            label = { Text("+ Nova Conta") }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Receita: selecionar forma (Pix, Transferência/Conta, Dinheiro) e conta que recebe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Conta de Destino / Recebimento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (onAddNewAccount != null) {
                                TextButton(onClick = onAddNewAccount) {
                                    Text("+ Nova Conta", color = MobillsGreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.PIX,
                                    onClick = { selectedPaymentType = PaymentType.PIX },
                                    label = { Text("⚡ Pix") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.DEBIT_CARD,
                                    onClick = { selectedPaymentType = PaymentType.DEBIT_CARD },
                                    label = { Text("🏦 Conta Corrente") }
                                )
                            }
                            item {
                                FilterChip(
                                    selected = selectedPaymentType == PaymentType.CASH,
                                    onClick = { selectedPaymentType = PaymentType.CASH },
                                    label = { Text("💵 Dinheiro") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(accounts) { acc ->
                                val isSelected = selectedAccountOrCard == acc.name
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedAccountOrCard = acc.name },
                                    label = { Text("${acc.bankIcon} ${acc.name}") }
                                )
                            }
                            if (onAddNewAccount != null) {
                                item {
                                    FilterChip(
                                        selected = false,
                                        onClick = onAddNewAccount,
                                        label = { Text("+ Nova Conta") }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Seção de TAGS (Identico ao Mobills)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tags (#)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onAddNewTag) {
                            Text("+ Nova Tag", color = MobillsGreenPrimary, fontSize = 11.sp)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(tags) { tag ->
                            val isTagSelected = selectedTags.contains(tag.name)
                            FilterChip(
                                selected = isTagSelected,
                                onClick = {
                                    selectedTags = if (isTagSelected) {
                                        selectedTags - tag.name
                                    } else {
                                        selectedTags + tag.name
                                    }
                                },
                                label = { Text("#${tag.name}") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Status Já Pago / Recebido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(if (type == TransactionType.EXPENSE) "Despesa Paga?" else "Receita Já Recebida?", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (isPaid) {
                                    if (type == TransactionType.EXPENSE) "✓ Pago / Liquidado" else "✓ Já recebido na conta"
                                } else {
                                    if (type == TransactionType.EXPENSE) "⏳ Pendente para vencimento" else "⏳ A receber"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPaid) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Switch(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MobillsGreenPrimary)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Passo 5 do Modo Professor
                    if (isProfessorMode && professorSheetStep == 5) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF08A),
                            border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "👈 PASSO 5: Defina se é Fixa Todo Mês ou Parcelada",
                                color = Color(0xFF854D0E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Despesa / Receita Fixa & Parcelamento nos Meses Seguintes (Estilo Mobills)
                    val isExpense = (type == TransactionType.EXPENSE)

                    // 1. Toggle de Transação Fixa (Repetir todo mês)
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isExpense) "📌 Despesa Fixa" else "📌 Receita Fixa",
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isRecurring) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = MaterialTheme.shapes.extraSmall,
                                            color = MobillsGreenPrimary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "TODO MÊS",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MobillsGreenPrimary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isRecurring) {
                                        if (isExpense) {
                                            "Repete automaticamente em todos os meses futuros"
                                        } else {
                                            "Recebimento fixo todo mês (ex: Gladstoni, salário, aluguel)"
                                        }
                                    } else {
                                        if (isExpense) {
                                            "Lançamento pontual apenas para este mês"
                                        } else {
                                            "Recebimento pontual apenas para este mês"
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isRecurring) MobillsGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isRecurring,
                                onCheckedChange = {
                                    isRecurring = it
                                    if (it) {
                                        totalInstallmentsCount = 1
                                        startInstallment = 1
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = MobillsGreenPrimary)
                            )
                        }
                    }

                    // Opção de propagação de alterações para Lançamentos Fixos ou Parcelados existentes
                    if (existingItem != null && (isRecurring || existingItem.isRecurring || existingItem.installmentGroupId.isNotBlank() || totalInstallmentsCount > 1)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MobillsGreenPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎯", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isRecurring || existingItem.isRecurring) {
                                            "Aplicação no Lançamento Fixo"
                                        } else {
                                            "Aplicação na Compra Parcelada"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MobillsGreenPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Escolha onde o novo valor e informações devem ser aplicados:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !updateFutureInstallments,
                                        onClick = { updateFutureInstallments = false },
                                        label = {
                                            Text(
                                                text = "Somente $currentMonth",
                                                fontSize = 11.sp,
                                                fontWeight = if (!updateFutureInstallments) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            if (!updateFutureInstallments) {
                                                Text("✓", fontSize = 12.sp, color = MobillsGreenPrimary)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = updateFutureInstallments,
                                        onClick = { updateFutureInstallments = true },
                                        label = {
                                            Text(
                                                text = "Alterar os demais também",
                                                fontSize = 11.sp,
                                                fontWeight = if (updateFutureInstallments) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            if (updateFutureInstallments) {
                                                Text("🔄", fontSize = 12.sp)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (!updateFutureInstallments) {
                                        "💡 O novo valor será alterado SOMENTE no mês de $currentMonth. Os outros meses permanecem com os valores anteriores (ideal para água, luz ou receitas variáveis)."
                                    } else {
                                        "💡 O novo valor e alterações serão aplicados neste mês e replicados para os meses seguintes."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Parcelamento / Repetição por parcelas (Aparece apenas se NÃO for fixa)
                    if (!isRecurring) {
                        val cleanVal = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                        val parcelVal = if (isAmountPerInstallment) cleanVal else (if (totalInstallmentsCount > 0) cleanVal / totalInstallmentsCount else cleanVal)

                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Total de Parcelas (ex: 11x)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isExpense) "Compra Parcelada?" else "Receber Parcelado / Repetir?",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (totalInstallmentsCount > 1) {
                                                if (startInstallment > 1) {
                                                    if (isExpense) "Começa na parcela ${startInstallment} de $totalInstallmentsCount" else "Começa no recebimento ${startInstallment} de $totalInstallmentsCount"
                                                } else {
                                                    "${totalInstallmentsCount}x de ${Formatters.formatMoney(parcelVal)} (projeta nos meses seguintes)"
                                                }
                                            } else {
                                                if (isExpense) "Lançamento único neste mês" else "Recebimento único neste mês"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (totalInstallmentsCount > 1) MobillsGreenPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (totalInstallmentsCount > 1) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (totalInstallmentsCount > 1) {
                                            IconButton(onClick = {
                                                totalInstallmentsCount--
                                                if (startInstallment > totalInstallmentsCount) startInstallment = totalInstallmentsCount
                                            }) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                                        }
                                        Text("${totalInstallmentsCount}x", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                        IconButton(onClick = { if (totalInstallmentsCount < 48) totalInstallmentsCount++ }) {
                                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Seletor da Parcela Deste Mês e Opções Avançadas
                                if (totalInstallmentsCount > 1) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Parcela Deste Mês (ex: começa na 6 de 11)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (isExpense) "Parcela deste mês:" else "Recebimento deste mês:",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "No extrato deste mês: (${startInstallment}/$totalInstallmentsCount)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MobillsGreenPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (startInstallment > 1) {
                                                IconButton(onClick = { startInstallment-- }) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                            }
                                            Surface(
                                                shape = MaterialTheme.shapes.small,
                                                color = MobillsGreenPrimary.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            ) {
                                                Text(
                                                    text = "$startInstallment de $totalInstallmentsCount",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MobillsGreenPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (startInstallment < totalInstallmentsCount) {
                                                IconButton(onClick = { startInstallment++ }) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Opção de como interpretar o valor digitado
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = isAmountPerInstallment,
                                            onClick = { isAmountPerInstallment = true },
                                            label = { Text(if (isExpense) "Valor de CADA parcela" else "Valor de CADA recebimento", fontSize = 11.sp) }
                                        )
                                        FilterChip(
                                            selected = !isAmountPerInstallment,
                                            onClick = { isAmountPerInstallment = false },
                                            label = { Text(if (isExpense) "Valor TOTAL da compra" else "Valor TOTAL a receber", fontSize = 11.sp) }
                                        )
                                    }

                                    // Checkbox para lançar histórico nos meses anteriores (1 até startInstallment - 1)
                                    if (startInstallment > 1 && existingItem == null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = includePreviousInstallments,
                                                onCheckedChange = { includePreviousInstallments = it },
                                                colors = CheckboxDefaults.colors(checkedColor = MobillsGreenPrimary)
                                            )
                                            Column {
                                                Text(
                                                    text = "Lançar histórico nos meses anteriores (1 a ${startInstallment - 1})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = if (isExpense) "Registra as parcelas passadas como PAGAS para ter o extrato completo" else "Registra os recebimentos passados como RECEBIDOS",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Contabilizar no Teto de Gastos (apenas se for DESPESA)
                    if (type == TransactionType.EXPENSE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🎯 Incluir no Teto de Gastos",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (includeInBudget) {
                                            if (selectedPaymentType == PaymentType.CREDIT_CARD) {
                                                "Desconta do teto de '${selectedCategory.name}' no cartão"
                                            } else {
                                                "Desconta da meta mensal de '${selectedCategory.name}'"
                                            }
                                        } else {
                                            "NÃO desconta do teto de '${selectedCategory.name}' (gasto isolado no cartão/extrato)"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (includeInBudget) MobillsGreenPrimary else Color(0xFFD97706),
                                        fontWeight = if (!includeInBudget) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = includeInBudget,
                                    onCheckedChange = { includeInBudget = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MobillsGreenPrimary)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Passo 6 do Modo Professor
                    if (isProfessorMode && professorSheetStep == 6) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF08A),
                            border = BorderStroke(1.dp, Color(0xFFCA8A04)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "👈 PASSO 6: Toque no botão 'Salvar' para finalizar o lançamento!",
                                color = Color(0xFF854D0E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Botões de Ação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (existingItem != null && onDelete != null) {
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "🗑️ Excluir",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Cancelar",
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val cleanVal = Formatters.parseCurrency(amountText)
                                if (cleanVal > 0 && description.isNotBlank()) {
                                    val finalAmount = if (totalInstallmentsCount > 1 && !isAmountPerInstallment) {
                                        cleanVal / totalInstallmentsCount
                                    } else {
                                        cleanVal
                                    }

                                    val cleanDesc = description.trim().replace(Regex("\\s*\\(\\d+/\\d+\\)"), "").trim()
                                    val finalDesc = if (totalInstallmentsCount > 1 && !isRecurring) {
                                        "$cleanDesc ($startInstallment/$totalInstallmentsCount)"
                                    } else cleanDesc

                                    val targetMonth = MonthHelper.getMonthYearFromDate(dateText) ?: currentMonth
                                    val itemToSave = if (existingItem != null) {
                                        existingItem.copy(
                                            description = finalDesc,
                                            amount = finalAmount,
                                            type = type,
                                            category = selectedCategory.name,
                                            categoryIcon = selectedCategory.iconEmoji,
                                            categoryColorHex = selectedCategory.colorHex,
                                            accountOrCardName = selectedAccountOrCard,
                                            paymentType = selectedPaymentType,
                                            tags = selectedTags.toList(),
                                            monthYear = targetMonth,
                                            date = dateText,
                                            status = if (isPaid) PaymentStatus.PAID else PaymentStatus.PENDING,
                                            isRecurring = isRecurring,
                                            currentInstallment = if (isRecurring) 1 else startInstallment,
                                            totalInstallments = if (isRecurring) 1 else totalInstallmentsCount,
                                            note = note,
                                            includeInBudget = includeInBudget
                                        )
                                    } else {
                                        TransactionItem(
                                            description = finalDesc,
                                            amount = finalAmount,
                                            type = type,
                                            category = selectedCategory.name,
                                            categoryIcon = selectedCategory.iconEmoji,
                                            categoryColorHex = selectedCategory.colorHex,
                                            accountOrCardName = selectedAccountOrCard,
                                            paymentType = selectedPaymentType,
                                            tags = selectedTags.toList(),
                                            monthYear = targetMonth,
                                            date = dateText,
                                            status = if (isPaid) PaymentStatus.PAID else PaymentStatus.PENDING,
                                            isRecurring = isRecurring,
                                            currentInstallment = if (isRecurring) 1 else startInstallment,
                                            totalInstallments = if (isRecurring) 1 else totalInstallmentsCount,
                                            note = note,
                                            includeInBudget = includeInBudget
                                        )
                                    }

                                    if (isProfessorMode) {
                                        onDismissProfessor()
                                    }
                                    onConfirm(itemToSave, updateFutureInstallments, includePreviousInstallments, isAmountPerInstallment)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(if (existingItem != null) 1.5f else 1.3f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = if (existingItem != null) "Salvar Alterações" else "Salvar",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog && existingItem != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Excluir Lançamento") },
            text = {
                Text(
                    if (existingItem.installmentGroupId.isNotBlank()) {
                        "Deseja excluir este lançamento? Você pode remover este item do extrato."
                    } else {
                        "Tem certeza que deseja excluir '${existingItem.description}'?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete(existingItem)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Excluir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
