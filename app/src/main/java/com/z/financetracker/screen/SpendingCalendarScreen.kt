package com.z.financetracker.screen

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.Transaction
import com.z.financetracker.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingCalendarScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var currentMonth      by remember { mutableStateOf(YearMonth.now()) }
    var monthTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var dailyExpenses     by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isLoading         by remember { mutableStateOf(true) }

    // Day detail sheet
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    fun loadMonth() {
        scope.launch {
            isLoading = true
            try {
                val start = currentMonth.atDay(1).toString()
                val end   = currentMonth.atEndOfMonth().toString()
                val resp  = NetworkClient.getTransactionApi(context)
                    .getHistory(page = 0, size = 300, startDate = start, endDate = end)
                if (resp.isSuccessful) {
                    val txs = resp.body()?.content ?: emptyList()
                    monthTransactions = txs
                    dailyExpenses = txs
                        .filter { it.type.name == "EXPENSE" }
                        .groupBy { it.date }
                        .mapValues { (_, list) -> list.sumOf { abs(it.amount) } }
                }
            } catch (_: Exception) {}
            finally { isLoading = false }
        }
    }

    LaunchedEffect(currentMonth) { loadMonth() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Month navigator ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { currentMonth = currentMonth.minusMonths(1) },
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            ) {
                Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    currentMonth.year.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = {
                    if (currentMonth < YearMonth.now())
                        currentMonth = currentMonth.plusMonths(1)
                },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (currentMonth < YearMonth.now()) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = if (currentMonth < YearMonth.now()) MaterialTheme.colorScheme.onSurface
                           else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Calendar card ──────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        DayOfWeekHeaders()
                        Spacer(Modifier.height(8.dp))
                        CalendarGrid(
                            month        = currentMonth,
                            dailyExpenses = dailyExpenses,
                            onDayClick   = { selectedDate = it }
                        )
                        Spacer(Modifier.height(16.dp))
                        ColorLegend(maxExpense = dailyExpenses.values.maxOrNull() ?: 0.0)
                    }
                }

                // ── Monthly summary ────────────────────────────────────
                MonthSummaryCard(
                    transactions  = monthTransactions,
                    dailyExpenses = dailyExpenses
                )

                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ── Day detail bottom sheet ────────────────────────────────────────
    selectedDate?.let { date ->
        val dayTxs = monthTransactions.filter { it.date == date.toString() }
        ModalBottomSheet(
            onDismissRequest = { selectedDate = null },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor   = MaterialTheme.colorScheme.surface
        ) {
            DayDetailSheet(date = date, transactions = dayTxs)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Day-of-week header row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayOfWeekHeaders() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(Modifier.fillMaxWidth()) {
        days.forEach { d ->
            Text(
                d,
                modifier    = Modifier.weight(1f),
                textAlign   = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize    = 11.sp,
                fontWeight  = FontWeight.Medium,
                color       = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calendar grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    month: YearMonth,
    dailyExpenses: Map<String, Double>,
    onDayClick: (LocalDate) -> Unit
) {
    val daysInMonth    = month.lengthOfMonth()
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1   // Mon=0 … Sun=6
    val maxExpense     = dailyExpenses.values.maxOrNull() ?: 1.0
    val today          = LocalDate.now()
    val totalCells     = firstDayOffset + daysInMonth
    val rows           = ceil(totalCells / 7.0).toInt()

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                for (col in 0 until 7) {
                    val day = row * 7 + col - firstDayOffset + 1
                    if (day < 1 || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date     = month.atDay(day)
                        val expense  = dailyExpenses[date.toString()] ?: 0.0
                        val isFuture = date > today
                        DayCell(
                            day        = day,
                            expense    = expense,
                            maxExpense = maxExpense,
                            isToday    = date == today,
                            isFuture   = isFuture,
                            modifier   = Modifier.weight(1f),
                            onClick    = { if (!isFuture) onDayClick(date) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Individual day cell with animated heat color
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayCell(
    day: Int,
    expense: Double,
    maxExpense: Double,
    isToday: Boolean,
    isFuture: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val rawIntensity = when {
        isFuture || expense <= 0 -> 0f
        else -> (expense / maxExpense).toFloat().coerceIn(0.05f, 1f)
    }

    // Animate fill when month loads
    val animIntensity by animateFloatAsState(
        targetValue    = rawIntensity,
        animationSpec  = tween(500, delayMillis = (day % 14) * 20, easing = EaseOutCubic),
        label          = "heatIntensity"
    )

    val bgColor = when {
        isFuture        -> MaterialTheme.colorScheme.background
        animIntensity <= 0f -> MaterialTheme.colorScheme.surfaceVariant
        else -> lerp(Color(0xFFDBEAFE), Color(0xFF1E3A8A), animIntensity)
    }

    val textColor = when {
        isFuture            -> Color(0xFFD1D5DB)
        animIntensity > 0.55f -> Color.White
        else                -> Color(0xFF374151)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .then(
                if (isToday) Modifier.border(2.dp, Color(0xFF2563EB), RoundedCornerShape(9.dp))
                else Modifier
            )
            .clickable(enabled = !isFuture) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = day.toString(),
            fontSize   = 12.sp,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
            color      = textColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gradient legend
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ColorLegend(maxExpense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Less", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val steps = listOf(0f, 0.2f, 0.4f, 0.65f, 1f)
            steps.forEach { t ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (t == 0f) Color(0xFFF1F5F9)
                            else lerp(Color(0xFFDBEAFE), Color(0xFF1E3A8A), t)
                        )
                )
            }
        }
        Text(
            if (maxExpense > 0) fmtCal(maxExpense) else "More",
            fontSize = 10.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Monthly summary card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun MonthSummaryCard(
    transactions: List<Transaction>,
    dailyExpenses: Map<String, Double>
) {
    val totalExpense  = dailyExpenses.values.sum()
    val totalIncome   = transactions.filter { it.type.name == "INCOME" }.sumOf { it.amount }
    val activeDays    = dailyExpenses.size
    val avgPerDay     = if (activeDays > 0) totalExpense / activeDays else 0.0
    val busiestEntry  = dailyExpenses.maxByOrNull { it.value }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Month Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryStatBox("Spent",   fmtCal(totalExpense), Color(0xFFEF4444), Modifier.weight(1f))
                SummaryStatBox("Income",  fmtCal(totalIncome),  Color(0xFF10B981), Modifier.weight(1f))
                SummaryStatBox("Avg/Day", fmtCal(avgPerDay),    Color(0xFF3B82F6), Modifier.weight(1f))
            }

            if (busiestEntry != null && totalExpense > 0) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.TrendingUp, null,
                        tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                    Text(
                        "Busiest day  •  ${fmtDate(busiestEntry.key)}  •  ${fmtCal(busiestEntry.value)}",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Day detail bottom sheet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayDetailSheet(date: LocalDate, transactions: List<Transaction>) {
    val fmt          = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())
    val totalExpense = transactions.filter { it.type.name == "EXPENSE" }.sumOf { abs(it.amount) }
    val totalIncome  = transactions.filter { it.type.name == "INCOME" }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 40.dp)
    ) {
        // Handle
        Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(40.dp, 4.dp).background(MaterialTheme.colorScheme.outline, CircleShape))
        }

        Text(date.format(fmt), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))

        // Quick totals
        if (totalExpense > 0 || totalIncome > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (totalExpense > 0) {
                    AmountPill("↓ ${fmtCal(totalExpense)}", Color(0xFFEF4444))
                }
                if (totalIncome > 0) {
                    AmountPill("↑ ${fmtCal(totalIncome)}", Color(0xFF10B981))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No activity on this day",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                transactions.forEach { tx ->
                    DayTransactionRow(tx)
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }
            }
        }
    }
}

@Composable
private fun AmountPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DayTransactionRow(tx: Transaction) {
    val isIncome = tx.type.name == "INCOME"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isIncome) AppColors.incomeBackground else AppColors.expensBackground,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                null,
                tint = if (isIncome) AppColors.incomeText else AppColors.expenseText,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.text, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!tx.categoryName.isNullOrBlank()) {
                Text(tx.categoryName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            "${if (isIncome) "+" else "-"}${fmtCal(abs(tx.amount))}",
            fontWeight = FontWeight.Bold,
            fontSize   = 14.sp,
            color      = if (isIncome) AppColors.incomeText else AppColors.expenseText
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────
private fun fmtCal(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0
    }.format(amount)

private fun fmtDate(dateStr: String): String = runCatching {
    LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))
}.getOrDefault(dateStr)
