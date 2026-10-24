package com.example.mobillsfinancas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mobillsfinancas.data.CategoryItem
import com.example.mobillsfinancas.data.TagItem
import com.example.mobillsfinancas.theme.MobillsGreenPrimary
import com.example.mobillsfinancas.utils.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesTagsScreen(
    categories: List<CategoryItem>,
    tags: List<TagItem>,
    onAddCategory: (CategoryItem) -> Unit,
    onUpdateCategory: (CategoryItem) -> Unit = {},
    onDeleteCategory: (String) -> Unit,
    onAddTag: (TagItem) -> Unit,
    onUpdateTag: (TagItem) -> Unit = {},
    onDeleteTag: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Categorias, 1 = Tags
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var editingTag by remember { mutableStateOf<TagItem?>(null) }
    var deletingCategory by remember { mutableStateOf<CategoryItem?>(null) }
    var deletingTag by remember { mutableStateOf<TagItem?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("🏷️ Categorias (${categories.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("#️⃣ Tags (${tags.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            // Lista de Categorias
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Categorias de Gastos e Receitas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque em uma categoria ou no ✏️ para alterar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("+ Categoria", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                items(categories, key = { it.id }) { cat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { editingCategory = cat },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(cat.colorHex).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.iconEmoji, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (cat.isExpense) "Despesa" else "Receita",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (cat.isExpense && cat.budgetLimit > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MobillsGreenPrimary.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "🎯 Teto: ${Formatters.formatMoney(cat.budgetLimit, false)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MobillsGreenPrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingCategory = cat }) {
                                    Text("✏️", fontSize = 17.sp)
                                }
                                IconButton(onClick = { deletingCategory = cat }) {
                                    Text("🗑️", fontSize = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Lista de Tags
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Tags de Agrupamento (#)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Toque em uma tag ou no ✏️ para alterar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showAddTagDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary)
                        ) {
                            Text("+ Nova Tag", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                items(tags, key = { it.id }) { tag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { editingTag = tag },
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = Color(tag.colorHex).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "#${tag.name}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(tag.colorHex),
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { editingTag = tag }) {
                                    Text("✏️", fontSize = 17.sp)
                                }
                                IconButton(onClick = { deletingTag = tag }) {
                                    Text("🗑️", fontSize = 17.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo Adicionar Categoria
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { cat ->
                    onAddCategory(cat)
                    showAddCategoryDialog = false
                }
            )
        }

        // Diálogo Editar Categoria
        editingCategory?.let { catToEdit ->
            EditCategoryDialog(
                category = catToEdit,
                onDismiss = { editingCategory = null },
                onConfirm = { updated ->
                    onUpdateCategory(updated)
                    editingCategory = null
                },
                onDelete = {
                    val toDelete = catToEdit
                    editingCategory = null
                    deletingCategory = toDelete
                }
            )
        }

        // Diálogo Confirmar Exclusão de Categoria
        deletingCategory?.let { catToDelete ->
            AlertDialog(
                onDismissRequest = { deletingCategory = null },
                title = { Text("Excluir Categoria") },
                text = { Text("Tem certeza que deseja excluir a categoria '${catToDelete.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteCategory(catToDelete.id)
                            deletingCategory = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingCategory = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Diálogo Adicionar Tag
        if (showAddTagDialog) {
            AddTagDialog(
                onDismiss = { showAddTagDialog = false },
                onConfirm = { tag ->
                    onAddTag(tag)
                    showAddTagDialog = false
                }
            )
        }

        // Diálogo Editar Tag
        editingTag?.let { tagToEdit ->
            EditTagDialog(
                tag = tagToEdit,
                onDismiss = { editingTag = null },
                onConfirm = { updated ->
                    onUpdateTag(updated)
                    editingTag = null
                },
                onDelete = {
                    val toDelete = tagToEdit
                    editingTag = null
                    deletingTag = toDelete
                }
            )
        }

        // Diálogo Confirmar Exclusão de Tag
        deletingTag?.let { tagToDelete ->
            AlertDialog(
                onDismissRequest = { deletingTag = null },
                title = { Text("Excluir Tag") },
                text = { Text("Tem certeza que deseja excluir a tag '#${tagToDelete.name}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteTag(tagToDelete.id)
                            deletingTag = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Excluir", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingTag = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (CategoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🏷️") }
    var isExpense by remember { mutableStateOf(true) }

    val sampleEmojis = listOf(
        "🏷️", "🍔", "🛒", "🏠", "🚗", "💳", "🏋️", "💈", "📱", 
        "🍿", "💊", "🎓", "🐾", "💰", "✈️", "🏦", "📈", "🎮", 
        "📚", "🩺", "⚡", "🎁", "🍻", "☕", "🔧", "👗", "⚽"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Nova Categoria", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Categoria") },
                    placeholder = { Text("Ex: Mercado, Passeios, Pet") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Escolha um Ícone:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleEmojis.forEach { e ->
                        val isSelected = (emoji == e)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MobillsGreenPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MobillsGreenPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(e, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Tipo da Categoria:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = {
                            Text(
                                "📉 Despesa",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEBEE),
                            selectedLabelColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = {
                            Text(
                                "📈 Receita",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancelar", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    CategoryItem(
                                        name = name.trim(),
                                        iconEmoji = emoji,
                                        colorHex = if (isExpense) 0xFFE91E63 else 0xFF00A868,
                                        isExpense = isExpense
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (TagItem) -> Unit
) {
    var name by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Nova Tag (#)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("#", "").replace(" ", "") },
                    label = { Text("Nome da Tag (sem espaços)") },
                    placeholder = { Text("Ex: viagem, alex, reforma") },
                    prefix = { Text("#") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancelar", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(TagItem(name = name.trim().lowercase()))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Salvar Tag", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun EditCategoryDialog(
    category: CategoryItem,
    onDismiss: () -> Unit,
    onConfirm: (CategoryItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(category) { mutableStateOf(category.name) }
    var emoji by remember(category) { mutableStateOf(category.iconEmoji) }
    var colorHex by remember(category) { mutableLongStateOf(category.colorHex) }
    var isExpense by remember(category) { mutableStateOf(category.isExpense) }
    var budgetInputTextFieldValue by remember(category) {
        mutableStateOf(
            if (category.budgetLimit > 0) Formatters.toCurrencyTextFieldValue(category.budgetLimit)
            else TextFieldValue("", TextRange(0))
        )
    }
    var reserveInBudget by remember(category) { mutableStateOf(category.reserveInBudget) }

    val sampleEmojis = listOf(
        "🏷️", "🍔", "🛒", "🏠", "🚗", "💳", "🏋️", "💈", "📱", 
        "🍿", "💊", "🎓", "🐾", "💰", "✈️", "🏦", "📈", "🎮", 
        "📚", "🩺", "⚡", "🎁", "🍻", "☕", "🔧", "👗", "⚽",
        "💻", "🍕", "🍼", "🎬", "🏖️", "🧼", "🧾", "🪙", "💎"
    )

    val sampleColors = listOf(
        0xFF00A868, // Verde Mobills
        0xFFE91E63, // Rosa
        0xFF2196F3, // Azul
        0xFFFF9800, // Laranja
        0xFF9C27B0, // Roxo
        0xFF00BCD4, // Ciano
        0xFFF44336, // Vermelho
        0xFF795548, // Marrom
        0xFF607D8B  // Cinza ardósia
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "✏️ Alterar Categoria",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MobillsGreenPrimary
                        )
                        IconButton(onClick = onDismiss) {
                            Text("✕", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da Categoria") },
                        placeholder = { Text("Ex: Supermercado, Passeios...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Ícone da Categoria:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val emojiList = if (sampleEmojis.contains(emoji)) sampleEmojis else listOf(emoji) + sampleEmojis
                        emojiList.forEach { e ->
                            val isSelected = (emoji == e)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MobillsGreenPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MobillsGreenPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { emoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Cor de Destaque:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleColors.forEach { cHex ->
                            val isSelected = (colorHex == cHex)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(cHex))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { colorHex = cHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Tipo da Categoria:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = isExpense,
                            onClick = { isExpense = true },
                            label = {
                                Text(
                                    "📉 Despesa",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (isExpense) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFFFEBEE),
                                selectedLabelColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isExpense,
                            onClick = { isExpense = false },
                            label = {
                                Text(
                                    "📈 Receita",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontWeight = if (!isExpense) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isExpense) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Teto de Gastos Mensal (Opcional):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = budgetInputTextFieldValue,
                            onValueChange = { budgetInputTextFieldValue = Formatters.updateCurrencyInput(it, budgetInputTextFieldValue) },
                            label = { Text("Teto Mensal") },
                            prefix = { Text("R$ ", fontWeight = FontWeight.Bold, color = MobillsGreenPrimary) },
                            placeholder = { Text("0,00 (sem teto)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reserveInBudget = !reserveInBudget }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                    Text("Reservar no Total de Despesas", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Deduz o saldo disponível mesmo que não tenha gasto", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = reserveInBudget, onCheckedChange = { reserveInBudget = it })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancelar", fontSize = 14.sp)
                        }
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    val parsedBudget = if (isExpense) Formatters.parseCurrency(budgetInputTextFieldValue.text) else 0.0
                                    onConfirm(
                                        category.copy(
                                            name = name.trim(),
                                            iconEmoji = emoji,
                                            colorHex = colorHex,
                                            isExpense = isExpense,
                                            budgetLimit = parsedBudget,
                                            reserveInBudget = reserveInBudget
                                        )
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    if (onDelete != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                        ) {
                            Text("🗑️ Excluir esta Categoria", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditTagDialog(
    tag: TagItem,
    onDismiss: () -> Unit,
    onConfirm: (TagItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember(tag) { mutableStateOf(tag.name) }
    var colorHex by remember(tag) { mutableLongStateOf(tag.colorHex) }

    val sampleColors = listOf(
        0xFF00A868, 0xFFE91E63, 0xFF2196F3, 0xFFFF9800, 
        0xFF9C27B0, 0xFF00BCD4, 0xFFF44336, 0xFF795548, 0xFF607D8B
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("✏️ Alterar Tag (#)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MobillsGreenPrimary)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.replace("#", "").replace(" ", "") },
                    label = { Text("Nome da Tag (sem espaços)") },
                    prefix = { Text("#") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Cor da Tag:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sampleColors.forEach { cHex ->
                        val isSelected = (colorHex == cHex)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(cHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = cHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Cancelar", fontSize = 14.sp)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(tag.copy(name = name.trim().lowercase(), colorHex = colorHex))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MobillsGreenPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("🗑️ Excluir esta Tag", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
