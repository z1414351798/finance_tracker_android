package com.z.financetracker.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.DatePickerField
import com.z.financetracker.component.ToastHost
import com.z.financetracker.component.rememberToastState
import com.z.financetracker.entity.SavingsGoal
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.min

@Composable
fun GoalsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var goals by remember { mutableStateOf<List<SavingsGoal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var contributeTarget by remember { mutableStateOf<SavingsGoal?>(null) }
    var editTarget by remember { mutableStateOf<SavingsGoal?>(null) }
    val toast = rememberToastState()

    fun load() {
        scope.launch {
            isLoading = true
            try {
                val resp = NetworkClient.getGoalApi(context).getGoals()
                if (resp.isSuccessful) goals = resp.body() ?: emptyList()
            } catch (_: Exception) {
                // server unreachable or network error — stay on screen with empty data
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF2563EB),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ── Header ─────────────────────────────────────
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Savings Goals", fontWeight = FontWeight.Black, fontSize = 22.sp)
                                Text(
                                    "${goals.count { it.currentAmount >= it.targetAmount }} of ${goals.size} completed",
                                    fontSize = 13.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            FloatingActionButton(
                                onClick = { showAddDialog = true },
                                containerColor = Color(0xFF2563EB),
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // ── Overall savings summary card ───────────────
                    if (goals.isNotEmpty()) {
                        item {
                            val totalTarget = goals.sumOf { it.targetAmount }
                            val totalSaved = goals.sumOf { it.currentAmount }
                            val overallPct = if (totalTarget > 0) (totalSaved / totalTarget).toFloat() else 0f

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E40AF)),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("Total saved", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        fmtGoal(totalSaved),
                                        color = Color.White,
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        "of ${fmtGoal(totalTarget)} across ${goals.size} goal${if (goals.size != 1) "s" else ""}",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    GoalProgressBar(pct = overallPct, color = Color(0xFF34D399))
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${(overallPct * 100).toInt()}% of total goals reached",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── Empty state ────────────────────────────────
                    if (goals.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(top = 60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🎯", fontSize = 64.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("No savings goals yet", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Set a goal and track your progress",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { showAddDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) { Text("Create first goal") }
                                }
                            }
                        }
                    }

                    // ── Goal cards ─────────────────────────────────
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            onContribute = { contributeTarget = goal },
                            onEdit = { editTarget = goal },
                            onDelete = {
                                scope.launch {
                                    NetworkClient.getGoalApi(context).deleteGoal(goal.id)
                                    load()
                                    scope.launch { toast.showDelete("Goal deleted") }
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            ToastHost(toast)
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────
    if (showAddDialog) {
        GoalFormDialog(
            existing = null,
            onDismiss = { showAddDialog = false },
            onSave = { goal ->
                scope.launch {
                    val resp = NetworkClient.getGoalApi(context).createGoal(goal)
                    if (resp.isSuccessful) {
                        showAddDialog = false
                        load()
                        scope.launch { toast.showSuccess("Goal created!") }
                    }
                }
            }
        )
    }

    editTarget?.let { goal ->
        GoalFormDialog(
            existing = goal,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                scope.launch {
                    val resp = NetworkClient.getGoalApi(context).updateGoal(goal.id, updated)
                    if (resp.isSuccessful) {
                        editTarget = null
                        load()
                        scope.launch { toast.showSuccess("Goal updated!") }
                    }
                }
            }
        )
    }

    contributeTarget?.let { goal ->
        ContributeDialog(
            goal = goal,
            onDismiss = { contributeTarget = null },
            onContribute = { amount ->
                scope.launch {
                    val resp = NetworkClient.getGoalApi(context)
                        .contribute(goal.id, mapOf("amount" to amount))
                    if (resp.isSuccessful) {
                        contributeTarget = null
                        load()
                        scope.launch { toast.showSuccess("Added ${fmtGoal(amount)} to ${goal.name}!") }
                    }
                }
            }
        )
    }
}

// ── Goal Card ──────────────────────────────────────────────────────

@Composable
private fun GoalCard(
    goal: SavingsGoal,
    onContribute: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = if (goal.targetAmount > 0)
        (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val isComplete = pct >= 1f
    val remaining = goal.targetAmount - goal.currentAmount

    // Days left calculation
    val daysLeft = goal.deadline?.let {
        runCatching {
            ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(it))
        }.getOrNull()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) Color(0xFFF0FDF4) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            // ── Top row ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (isComplete) Color(0xFFD1FAE5) else Color(0xFFEFF6FF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(goal.icon, fontSize = 22.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        goal.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isComplete) {
                        Text("🎉 Goal reached!", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Medium)
                    } else {
                        daysLeft?.let {
                            Text(
                                when {
                                    it < 0  -> "Overdue by ${-it}d"
                                    it == 0L -> "Due today"
                                    else    -> "$it days left"
                                },
                                fontSize = 12.sp,
                                color = when {
                                    it < 0  -> Color(0xFFDC2626)
                                    it <= 7 -> Color(0xFFF59E0B)
                                    else    -> Color(0xFF6B7280)
                                }
                            )
                        }
                    }
                }

                // Menu
                var menuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, null, tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) },
                            onClick = { menuExpanded = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFDC2626)) },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Amount display ─────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Saved", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    Text(fmtGoal(goal.currentAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF059669))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    Text(fmtGoal(goal.targetAmount), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Progress bar ───────────────────────────────────────
            GoalProgressBar(
                pct = pct,
                color = if (isComplete) Color(0xFF059669) else Color(0xFF2563EB)
            )

            Spacer(Modifier.height(6.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${(pct * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isComplete) Color(0xFF059669) else Color(0xFF2563EB)
                )
                if (!isComplete) {
                    Text(
                        "${fmtGoal(remaining)} to go",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            // ── Add money button ───────────────────────────────────
            if (!isComplete) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onContribute,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add money", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}

// ── Progress bar ───────────────────────────────────────────────────

@Composable
private fun GoalProgressBar(pct: Float, color: Color) {
    val animPct by animateFloatAsState(
        targetValue = min(pct, 1f),
        animationSpec = tween(900, easing = EaseOutCubic)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFE5E7EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animPct)
                .height(10.dp)
                .background(color, RoundedCornerShape(5.dp))
        )
    }
}

// ── Add / Edit dialog ──────────────────────────────────────────────

@Composable
private fun GoalFormDialog(
    existing: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (SavingsGoal) -> Unit
) {
    val isEdit = existing != null
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetAmount by remember { mutableStateOf(existing?.targetAmount?.toString() ?: "") }
    var deadline by remember { mutableStateOf(existing?.deadline ?: "") }
    var selectedIcon by remember { mutableStateOf(existing?.icon ?: "🎯") }

    val icons = listOf("🎯","🏠","✈️","🚗","📱","💻","🎓","💍","🏖️","🛍️","🏋️","🎸","🐶","👶","💊","🌍")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(if (isEdit) "Edit goal" else "New savings goal", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Icon picker
                Text("Pick an icon", fontSize = 12.sp, color = Color(0xFF6B7280))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { icon ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selectedIcon == icon) Color(0xFFEFF6FF) else Color(0xFFF9FAFB),
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    width = if (selectedIcon == icon) 2.dp else 0.dp,
                                    color = if (selectedIcon == icon) Color(0xFF2563EB) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(icon, fontSize = 20.sp)
                        }
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal name") },
                    placeholder = { Text("e.g. New laptop") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                // Target amount
                OutlinedTextField(
                    value = targetAmount,
                    onValueChange = { targetAmount = it },
                    label = { Text("Target amount") },
                    placeholder = { Text("e.g. 50000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                // Deadline
                DatePickerField(
                    label = "Deadline (optional)",
                    value = deadline,
                    onDateSelected = { deadline = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Add clear button next to it if a date is set:
                if (deadline.isNotBlank()) {
                    TextButton(
                        onClick = { deadline = "" },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear deadline", color = Color(0xFF6B7280), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = targetAmount.toDoubleOrNull() ?: return@Button
                    if (name.isBlank()) return@Button
                    onSave(
                        SavingsGoal(
                            id = existing?.id ?: 0,
                            name = name.trim(),
                            icon = selectedIcon,
                            targetAmount = amt,
                            currentAmount = existing?.currentAmount ?: 0.0,
                            deadline = deadline.ifBlank { null }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) { Text(if (isEdit) "Save changes" else "Create goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ── Contribute dialog ──────────────────────────────────────────────

@Composable
private fun ContributeDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    val remaining = goal.targetAmount - goal.currentAmount

    // Quick add chips
    val quickAmounts = listOf(1000.0, 5000.0, 10000.0, 50000.0)
        .filter { it <= remaining }
        .take(3)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(goal.icon, fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Add to ${goal.name}", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Progress reminder
                val pct = (goal.currentAmount / goal.targetAmount * 100).toInt()
                Text(
                    "${fmtGoal(goal.currentAmount)} saved of ${fmtGoal(goal.targetAmount)} · $pct%",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )

                // Quick amount chips
                if (quickAmounts.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        quickAmounts.forEach { quick ->
                            SuggestionChip(
                                onClick = { amount = quick.toInt().toString() },
                                label = { Text("+${fmtGoal(quick)}", fontSize = 12.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to add") },
                    placeholder = { Text("e.g. 10000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                // Max out helper
                TextButton(
                    onClick = { amount = remaining.toInt().toString() },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Add remaining ${fmtGoal(remaining)}",
                        fontSize = 12.sp,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    if (amt > 0) onContribute(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add money")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun fmtGoal(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }.format(amount)