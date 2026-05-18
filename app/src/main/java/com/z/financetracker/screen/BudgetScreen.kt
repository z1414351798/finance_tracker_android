package com.z.financetracker.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.z.financetracker.api.CategorySummary
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.Category
import com.z.financetracker.enums.TraType
import com.z.financetracker.util.Budget
import com.z.financetracker.ui.theme.AppColors
import com.z.financetracker.util.BudgetManager
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.min

@Composable
fun BudgetScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val budgetManager = remember { BudgetManager(context) }
    val scrollState = rememberScrollState()

    var budgets by remember { mutableStateOf(budgetManager.getBudgets()) }
    var spentMap by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var expenseCategories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            try {
                // Load actual spending per category
                val summary = NetworkClient.getTransactionApi(context).getSummary()
                if (summary.isSuccessful) {
                    spentMap = summary.body()?.expenseCategories
                        ?.associate { it.name to it.value } ?: emptyMap()
                }

                // Load categories for dropdown in add dialog
                val cats = NetworkClient.getCategoryApi(context).getCategories(TraType.EXPENSE)
                expenseCategories = cats
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // ── Header ─────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Budgets", fontWeight = FontWeight.Black, fontSize = 22.sp)
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF2563EB),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Monthly Overview Card ──────────────────────────────────
        val totalBudgeted = budgets.sumOf { it.limit }
        val totalSpent = budgets.sumOf { spentMap[it.categoryName] ?: 0.0 }
        val overallPct = if (totalBudgeted > 0) (totalSpent / totalBudgeted).toFloat() else 0f

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    overallPct >= 1f -> AppColors.errorBackground
                    overallPct >= 0.8f -> AppColors.warningBackground
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Monthly Budget Overview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BudgetStat("Budgeted", totalBudgeted, Color(0xFF2563EB))
                    BudgetStat("Spent", totalSpent,
                        if (overallPct >= 1f) Color(0xFFDC2626) else Color(0xFF374151)
                    )
                    BudgetStat("Remaining", (totalBudgeted - totalSpent).coerceAtLeast(0.0),
                        Color(0xFF059669)
                    )
                }
                Spacer(Modifier.height(12.dp))
                BudgetProgressBar(pct = overallPct)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${(overallPct * 100).toInt()}% of total budget used",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Per-Category Budget Cards ──────────────────────────────
        if (budgets.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No budgets set yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("Add your first budget", color = Color(0xFF2563EB))
                    }
                }
            }
        } else {
            budgets.forEach { budget ->
                BudgetCategoryCard(
                    budget = budget,
                    spent = spentMap[budget.categoryName] ?: 0.0,
                    onDelete = {
                        budgets = budgets.filter { it != budget }
                        budgetManager.saveBudgets(budgets)
                    }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ── Add Budget Dialog ──────────────────────────────────────────
    if (showAddDialog) {
        AddBudgetDialog(
            availableCategories = expenseCategories.map { it.name }
                .filter { name -> budgets.none { it.categoryName == name } },
            onDismiss = { showAddDialog = false },
            onAdd = { newBudget ->
                budgets = budgets + newBudget
                budgetManager.saveBudgets(budgets)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun BudgetStat(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(
            fmtBudget(amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun BudgetProgressBar(pct: Float) {
    val animPct by animateFloatAsState(
        targetValue = min(pct, 1f),
        animationSpec = tween(900)
    )
    val barColor = when {
        pct >= 1f -> Color(0xFFDC2626)
        pct >= 0.8f -> Color(0xFFF59E0B)
        else -> Color(0xFF2563EB)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.outline)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animPct)
                .height(8.dp)
                .background(barColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun BudgetCategoryCard(budget: Budget, spent: Double, onDelete: () -> Unit) {
    val pct = if (budget.limit > 0) (spent / budget.limit).toFloat() else 0f
    val remaining = budget.limit - spent
    val isOver = pct >= 1f
    val isWarning = pct >= 0.8f && !isOver

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOver) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        budget.categoryName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isOver) Color(0xFFDC2626) else MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete, null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            BudgetProgressBar(pct)
            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${fmtBudget(spent)} spent",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isOver) "${fmtBudget(-remaining)} over"
                    else "${fmtBudget(remaining)} left",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isOver -> Color(0xFFDC2626)
                        isWarning -> Color(0xFFF59E0B)
                        else -> Color(0xFF059669)
                    }
                )
            }
            Text(
                "of ${fmtBudget(budget.limit)} budget",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (Budget) -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(availableCategories.firstOrNull() ?: "")
    }
    var limitAmount by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (availableCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("All categories have budgets", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = { expanded = false }
                            )
                        } else {
                            availableCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { selectedCategory = cat; expanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = limitAmount,
                    onValueChange = { limitAmount = it },
                    label = { Text("Monthly Limit") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitAmount.toDoubleOrNull()
                    if (selectedCategory.isNotEmpty() && limit != null && limit > 0) {
                        onAdd(Budget(categoryName = selectedCategory, limit = limit))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

private fun fmtBudget(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }.format(amount)