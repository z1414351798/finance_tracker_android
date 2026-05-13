package com.z.financetracker.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.ToastHost
import com.z.financetracker.component.ToastType
import com.z.financetracker.component.rememberToastState
import com.z.financetracker.entity.Category
import com.z.financetracker.entity.RecurringTransaction
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    val toast   = rememberToastState()

    var items       by remember { mutableStateOf<List<RecurringTransaction>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var showAddSheet       by remember { mutableStateOf(false) }
    var editItem           by remember { mutableStateOf<RecurringTransaction?>(null) }
    var showDetectSheet    by remember { mutableStateOf(false) }
    var detectResults      by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isDetecting        by remember { mutableStateOf(false) }

    fun load() {
        scope.launch {
            try {
                val resp = NetworkClient.getRecurringApi(context).getAll()
                if (resp.isSuccessful) items = resp.body() ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    // Partition: overdue/due-soon vs rest
    val today = LocalDate.now()
    val upcoming = items.filter {
        runCatching { !LocalDate.parse(it.nextDueDate).isAfter(today.plusDays(7)) }
            .getOrDefault(false)
    }
    val later = items.filter { it !in upcoming }

    // Monthly total
    val monthlyTotal = items.sumOf { r ->
        val multiplier = when (r.frequency) {
            "WEEKLY"    -> 4.33
            "BIWEEKLY"  -> 2.17
            "QUARTERLY" -> 0.33
            "YEARLY"    -> 0.083
            else        -> 1.0   // MONTHLY
        }
        if (r.type == TraType.EXPENSE) r.amount * multiplier else 0.0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {

            // ── Header ────────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF1E3A8A))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recurring", color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp, letterSpacing = 1.sp)
                            Text("Bills & Subscriptions", color = Color.White,
                                fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        // Detect button
                        IconButton(
                            onClick = {
                                isDetecting = true
                                showDetectSheet = true
                                scope.launch {
                                    try {
                                        val r = NetworkClient.getRecurringApi(context).detect()
                                        if (r.isSuccessful) detectResults = r.body() ?: emptyList()
                                    } catch (_: Exception) { }
                                    isDetecting = false
                                }
                            },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = Color.White)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Monthly spend summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeaderStat(
                            label = "Monthly cost",
                            value = "$${"%.2f".format(monthlyTotal)}",
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStat(
                            label = "Active",
                            value = "${items.size}",
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStat(
                            label = "Due soon",
                            value = "${upcoming.size}",
                            highlight = upcoming.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
                return@Column
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                // ── Due soon ──────────────────────────────────────────
                if (upcoming.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("⏰  Due Soon", Color(0xFFDC2626))
                    Spacer(Modifier.height(8.dp))
                    upcoming.forEach { item ->
                        RecurringCard(
                            item      = item,
                            urgent    = true,
                            onPay     = {
                                scope.launch {
                                    try {
                                        val r = NetworkClient.getRecurringApi(context)
                                            .pay(item.id!!)
                                        if (r.isSuccessful) {
                                            toast.show("Paid ${item.text}!", ToastType.SUCCESS)
                                            load()
                                        }
                                    } catch (_: Exception) {
                                        toast.show("Failed", ToastType.ERROR)
                                    }
                                }
                            },
                            onEdit    = { editItem = item },
                            onDelete  = {
                                scope.launch {
                                    NetworkClient.getRecurringApi(context).delete(item.id!!)
                                    toast.show("Deleted", ToastType.DELETE)
                                    load()
                                }
                            }
                        )
                    }
                }

                // ── All others ────────────────────────────────────────
                if (later.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionLabel("📅  Upcoming", Color(0xFF374151))
                    Spacer(Modifier.height(8.dp))
                    later.forEach { item ->
                        RecurringCard(
                            item     = item,
                            urgent   = false,
                            onPay    = {
                                scope.launch {
                                    try {
                                        val r = NetworkClient.getRecurringApi(context)
                                            .pay(item.id!!)
                                        if (r.isSuccessful) {
                                            toast.show("Paid ${item.text}!", ToastType.SUCCESS)
                                            load()
                                        }
                                    } catch (_: Exception) { }
                                }
                            },
                            onEdit   = { editItem = item },
                            onDelete = {
                                scope.launch {
                                    NetworkClient.getRecurringApi(context).delete(item.id!!)
                                    toast.show("Deleted", ToastType.DELETE)
                                    load()
                                }
                            }
                        )
                    }
                }

                if (items.isEmpty()) {
                    EmptyState()
                }
            }
        }

        // ── FAB ───────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            containerColor = Color(0xFF2563EB)
        ) {
            Icon(Icons.Default.Add, null, tint = Color.White)
        }

        ToastHost(toast)
    }

    // ── Add / Edit sheet ──────────────────────────────────────────────────────
    if (showAddSheet || editItem != null) {
        RecurringFormSheet(
            initial   = editItem,
            onDismiss = { showAddSheet = false; editItem = null },
            onSave    = { r ->
                scope.launch {
                    try {
                        if (r.id == null) {
                            NetworkClient.getRecurringApi(context).add(r)
                            toast.show("Added!", ToastType.SUCCESS)
                        } else {
                            NetworkClient.getRecurringApi(context).update(r.id, r)
                            toast.show("Updated!", ToastType.SUCCESS)
                        }
                        showAddSheet = false; editItem = null
                        load()
                    } catch (_: Exception) {
                        toast.show("Failed to save", ToastType.ERROR)
                    }
                }
            }
        )
    }

    // ── Detect sheet ──────────────────────────────────────────────────────────
    if (showDetectSheet) {
        DetectSheet(
            isLoading = isDetecting,
            results   = detectResults,
            onDismiss = { showDetectSheet = false },
            onAdd     = { suggestion ->
                scope.launch {
                    try {
                        NetworkClient.getRecurringApi(context).add(suggestion)
                        toast.show("Added ${suggestion.text}!", ToastType.SUCCESS)
                        load()
                    } catch (_: Exception) { }
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Recurring card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecurringCard(
    item:     RecurringTransaction,
    urgent:   Boolean,
    onPay:    () -> Unit,
    onEdit:   () -> Unit,
    onDelete: () -> Unit
) {
    val today     = LocalDate.now()
    val dueDate   = runCatching { LocalDate.parse(item.nextDueDate) }.getOrNull()
    val daysUntil = dueDate?.let { ChronoUnit.DAYS.between(today, it) } ?: 0L
    val isOverdue = daysUntil < 0

    val borderColor = when {
        isOverdue -> Color(0xFFEF4444)
        daysUntil <= 3 -> Color(0xFFF59E0B)
        urgent -> Color(0xFFFCA5A5)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .then(if (urgent) Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                  else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdue) Color(0xFFFEF2F2) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Frequency icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(frequencyColor(item.frequency).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(frequencyEmoji(item.frequency), fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(item.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category
                    item.categoryName?.let {
                        Text(it, fontSize = 11.sp, color = Color(0xFF6B7280))
                        Text("·", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    }
                    // Frequency badge
                    Text(
                        item.frequency.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = frequencyColor(item.frequency),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Due date
                Text(
                    when {
                        isOverdue -> "Overdue by ${abs(daysUntil)}d"
                        daysUntil == 0L -> "Due today"
                        daysUntil == 1L -> "Due tomorrow"
                        else -> "Due in ${daysUntil}d  (${item.nextDueDate})"
                    },
                    fontSize = 11.sp,
                    color = when {
                        isOverdue   -> Color(0xFFDC2626)
                        daysUntil <= 3 -> Color(0xFFF59E0B)
                        else        -> Color(0xFF6B7280)
                    },
                    fontWeight = if (daysUntil <= 3) FontWeight.Medium else FontWeight.Normal
                )
            }

            Column(horizontalAlignment = Alignment.End,
                   verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${if (item.type == TraType.EXPENSE) "-" else "+"}$${"%.2f".format(item.amount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.type == TraType.EXPENSE) Color(0xFFDC2626)
                            else Color(0xFF059669)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Pay button
                    SmallButton(
                        text   = if (isOverdue) "Pay!" else "Pay",
                        color  = if (isOverdue) Color(0xFFDC2626) else Color(0xFF2563EB),
                        onClick = onPay
                    )
                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, null,
                            tint = Color(0xFF9CA3AF), modifier = Modifier.size(16.dp))
                    }
                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, null,
                            tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add / Edit bottom sheet form
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFormSheet(
    initial:   RecurringTransaction?,
    onDismiss: () -> Unit,
    onSave:    (RecurringTransaction) -> Unit
) {
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    var text       by remember { mutableStateOf(initial?.text ?: "") }
    var amount     by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    var type       by remember { mutableStateOf(initial?.type ?: TraType.EXPENSE) }
    var frequency  by remember { mutableStateOf(initial?.frequency ?: "MONTHLY") }
    var nextDue    by remember { mutableStateOf(initial?.nextDueDate ?: "") }
    var note       by remember { mutableStateOf(initial?.note ?: "") }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCat by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(type) {
        try {
            val list = NetworkClient.getCategoryApi(context).getCategories(type)
            categories = list
            selectedCat = list.firstOrNull { it.id == initial?.categoryId }
        } catch (_: Exception) {}
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                if (initial == null) "Add Recurring" else "Edit Recurring",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827)
            )
            Spacer(Modifier.height(16.dp))

            // Type toggle
            Row(Modifier.fillMaxWidth()) {
                Button(
                    onClick = { type = TraType.EXPENSE },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TraType.EXPENSE) Color(0xFFEF4444)
                                         else Color(0xFFE5E7EB)
                    )
                ) { Text("Expense", color = if (type == TraType.EXPENSE) Color.White else Color(0xFF374151)) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { type = TraType.INCOME },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == TraType.INCOME) Color(0xFF10B981)
                                         else Color(0xFFE5E7EB)
                    )
                ) { Text("Income", color = if (type == TraType.INCOME) Color.White else Color(0xFF374151)) }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = text, onValueChange = { text = it },
                label = { Text("Name (e.g. Netflix)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = nextDue, onValueChange = { nextDue = it },
                label = { Text("Next due date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Frequency picker
            Text("Frequency", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = Color(0xFF374151))
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("WEEKLY","BIWEEKLY","MONTHLY","QUARTERLY","YEARLY").forEach { f ->
                    val selected = frequency == f
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { frequency = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            f.lowercase().replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            color = if (selected) Color.White else Color(0xFF6B7280),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Category chips
            if (categories.isNotEmpty()) {
                Text("Category", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color(0xFF374151))
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val sel = selectedCat == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    if (sel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCat = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(cat.name, fontSize = 12.sp,
                                color = if (sel) Color.White else Color(0xFF6B7280))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    onSave(
                        RecurringTransaction(
                            id          = initial?.id,
                            text        = text.trim(),
                            amount      = abs(amt),
                            type        = type,
                            categoryId  = selectedCat?.id ?: 0L,
                            frequency   = frequency,
                            nextDueDate = nextDue.trim(),
                            isActive    = true,
                            note        = note.trim().ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text(if (initial == null) "Add" else "Save", fontSize = 16.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Auto-detect bottom sheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetectSheet(
    isLoading: Boolean,
    results:   List<Map<String, Any>>,
    onDismiss: () -> Unit,
    onAdd:     (RecurringTransaction) -> Unit
) {
    val added = remember { mutableStateListOf<String>() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp))
                Text("Detected Patterns", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text("Based on your last 6 months of transactions",
                fontSize = 12.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                           verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                        Text("Scanning history…", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }
            } else if (results.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No recurring patterns found yet.\nAdd more transactions over time!",
                        fontSize = 13.sp, color = Color(0xFF9CA3AF))
                }
            } else {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    results.forEach { p ->
                        val name  = p["text"] as? String ?: ""
                        val amt   = (p["avgAmount"] as? Number)?.toDouble() ?: 0.0
                        val freq  = p["suggestedFrequency"] as? String ?: "MONTHLY"
                        val due   = p["suggestedNextDueDate"] as? String ?: ""
                        val occ   = (p["occurrences"] as? Number)?.toInt() ?: 0
                        val type  = p["type"] as? String ?: "EXPENSE"
                        val isAdded = name in added

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(frequencyEmoji(freq), fontSize = 22.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$occ times · $freq · next $due",
                                    fontSize = 11.sp, color = Color(0xFF6B7280))
                            }
                            Text("$${"%.2f".format(amt)}",
                                fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                color = if (type == "EXPENSE") Color(0xFFDC2626)
                                        else Color(0xFF059669))
                            if (isAdded) {
                                Icon(Icons.Default.CheckCircle, null,
                                    tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                            } else {
                                IconButton(
                                    onClick = {
                                        onAdd(RecurringTransaction(
                                            text        = name,
                                            amount      = amt,
                                            type        = if (type == "INCOME") TraType.INCOME
                                                          else TraType.EXPENSE,
                                            frequency   = freq,
                                            nextDueDate = due,
                                            isActive    = true
                                        ))
                                        added.add(name)
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color(0xFF2563EB).copy(alpha = 0.1f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, null,
                                        tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Empty state
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("📅", fontSize = 48.sp)
        Text("No recurring transactions yet", fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp, color = Color(0xFF374151))
        Text("Tap + to add subscriptions or bills,\nor tap ✨ to auto-detect from history",
            fontSize = 13.sp, color = Color(0xFF9CA3AF),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Small helpers
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeaderStat(
    label: String, value: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = if (highlight) 0.25f else 0.12f),
                RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = if (highlight) Color(0xFFFCA5A5) else Color.White,
            fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
private fun SectionLabel(text: String, color: Color) {
    Text(text, modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
}

@Composable
private fun SmallButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun frequencyColor(freq: String) = when (freq) {
    "WEEKLY"    -> Color(0xFF8B5CF6)
    "BIWEEKLY"  -> Color(0xFF0EA5E9)
    "MONTHLY"   -> Color(0xFF2563EB)
    "QUARTERLY" -> Color(0xFFF59E0B)
    "YEARLY"    -> Color(0xFF10B981)
    else        -> Color(0xFF6B7280)
}

private fun frequencyEmoji(freq: String) = when (freq) {
    "WEEKLY"    -> "🔁"
    "BIWEEKLY"  -> "🔄"
    "MONTHLY"   -> "📅"
    "QUARTERLY" -> "🗓️"
    "YEARLY"    -> "📆"
    else        -> "📅"
}
