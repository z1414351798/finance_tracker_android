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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.api.BigTransaction
import com.z.financetracker.api.MonthlyBreakdown
import com.z.financetracker.client.NetworkClient
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

// ─────────────────────────────────────────────────────────────────────────────
// Monthly Report Screen
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun MonthlyReportScreen() {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var breakdown   by remember { mutableStateOf<List<MonthlyBreakdown>>(emptyList()) }
    var bigTx       by remember { mutableStateOf<List<BigTransaction>>(emptyList()) }
    var isLoading   by remember { mutableStateOf(true) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val b = NetworkClient.getTransactionApi(context).getMonthlyBreakdown(6)
                val t = NetworkClient.getTransactionApi(context).getBiggestTransactions(5)
                if (b.isSuccessful) breakdown = b.body() ?: emptyList()
                if (t.isSuccessful) bigTx      = t.body() ?: emptyList()
            } catch (_: Exception) { }
            isLoading = false
        }
    }

    // Current month data
    val fmt = DateTimeFormatter.ofPattern("yyyy-MM")
    val currentMonthKey = selectedMonth.format(fmt)
    val currentData  = breakdown.firstOrNull { it.month == currentMonthKey }
    val previousData = breakdown.drop(1).firstOrNull()

    val totalSpent   = abs(currentData?.expense ?: 0.0)
    val totalIncome  = currentData?.income ?: 0.0
    val prevSpent    = abs(previousData?.expense ?: 0.0)
    val prevIncome   = previousData?.income ?: 0.0

    val spentChange  = if (prevSpent  > 0) ((totalSpent  - prevSpent)  / prevSpent  * 100) else 0.0
    val incomeChange = if (prevIncome > 0) ((totalIncome - prevIncome) / prevIncome * 100) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero header ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column {
                Text(
                    "Monthly Report",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    selectedMonth.month.getDisplayName(JTextStyle.FULL, Locale.getDefault()) +
                            " ${selectedMonth.year}",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                // Month navigation row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.minusMonths(1) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }

                    breakdown.take(6).forEach { mb ->
                        val key = mb.month
                        val isSelected = key == currentMonthKey
                        val label = try {
                            YearMonth.parse(key, fmt).month
                                .getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                        } catch (_: Exception) { key }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    selectedMonth = try { YearMonth.parse(key, fmt) }
                                    catch (_: Exception) { selectedMonth }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF1E3A8A) else Color.White
                            )
                        }
                    }

                    if (selectedMonth < YearMonth.now()) {
                        IconButton(
                            onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White,
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
            return@Column
        }

        Spacer(Modifier.height(16.dp))

        // ── Spent vs Income cards ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label       = "Total Spent",
                amount      = totalSpent,
                change      = spentChange,
                isExpense   = true,
                modifier    = Modifier.weight(1f)
            )
            StatCard(
                label       = "Total Income",
                amount      = totalIncome,
                change      = incomeChange,
                isExpense   = false,
                modifier    = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Net balance card
        val net = totalIncome - totalSpent
        NetBalanceCard(net = net, modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp))

        Spacer(Modifier.height(20.dp))

        // ── 6-month bar chart ─────────────────────────────────────────
        SectionHeader("Last 6 Months")
        Spacer(Modifier.height(10.dp))
        SixMonthBarChart(
            breakdown = breakdown.reversed(),          // oldest → newest
            selectedKey = currentMonthKey,
            onSelect = { key ->
                selectedMonth = try { YearMonth.parse(key, fmt) }
                catch (_: Exception) { selectedMonth }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(20.dp))

        // ── Quick stats row ───────────────────────────────────────────
        currentData?.let { data ->
            SectionHeader("Quick Stats")
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val daysInMonth = selectedMonth.lengthOfMonth().toDouble()
                QuickStat(
                    icon  = Icons.Default.Receipt,
                    color = Color(0xFF8B5CF6),
                    label = "Transactions",
                    value = "${data.transactionCount}",
                    modifier = Modifier.weight(1f)
                )
                QuickStat(
                    icon  = Icons.Default.CalendarMonth,
                    color = Color(0xFF0EA5E9),
                    label = "Avg / Day",
                    value = "$${"%.0f".format(totalSpent / daysInMonth)}",
                    modifier = Modifier.weight(1f)
                )
                QuickStat(
                    icon  = Icons.Default.Savings,
                    color = Color(0xFF10B981),
                    label = "Saved",
                    value = if (net >= 0) "+$${"%.0f".format(net)}" else "-$${"%.0f".format(abs(net))}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Biggest transactions ──────────────────────────────────────
        if (bigTx.isNotEmpty()) {
            SectionHeader("Biggest Expenses (3 months)")
            Spacer(Modifier.height(10.dp))
            bigTx.forEachIndexed { i, tx ->
                BigTransactionRow(rank = i + 1, tx = tx)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stat Card (spent / income)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String,
    amount: Double,
    change: Double,
    isExpense: Boolean,
    modifier: Modifier = Modifier
) {
    // Animated counter
    val animAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "amount"
    )

    val accentColor = if (isExpense) Color(0xFFEF4444) else Color(0xFF10B981)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isExpense) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                        null, tint = accentColor, modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "$${"%.2f".format(animAmount)}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
            Spacer(Modifier.height(6.dp))
            if (change != 0.0) {
                val up = change > 0
                // For expense: going up is bad (red). For income: going up is good (green)
                val goodUp   = !isExpense
                val arrowColor = when {
                    up && goodUp   -> Color(0xFF10B981)
                    up && !goodUp  -> Color(0xFFEF4444)
                    !up && goodUp  -> Color(0xFFEF4444)
                    else           -> Color(0xFF10B981)
                }
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(
                        if (up) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        null, tint = arrowColor, modifier = Modifier.size(12.dp)
                    )
                    Text(
                        "${"%.1f".format(abs(change))}% vs last month",
                        fontSize = 10.sp, color = arrowColor
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Net balance card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NetBalanceCard(net: Double, modifier: Modifier = Modifier) {
    val isPositive = net >= 0
    val bgColor    = if (isPositive) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
    val textColor  = if (isPositive) Color(0xFF059669) else Color(0xFFDC2626)

    val animNet by animateFloatAsState(
        targetValue = net.toFloat(),
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "net"
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (isPositive) Icons.Default.SentimentVerySatisfied
                    else Icons.Default.SentimentVeryDissatisfied,
                    null, tint = textColor, modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        if (isPositive) "You're saving money!" else "Spending exceeds income",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor
                    )
                    Text(
                        "Net balance this month",
                        fontSize = 11.sp, color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                "${if (isPositive) "+" else ""}$${"%.2f".format(animNet)}",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6-Month bar chart
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SixMonthBarChart(
    breakdown: List<MonthlyBreakdown>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) return

    val maxVal = breakdown.maxOf { max(it.income, abs(it.expense)) }.coerceAtLeast(1.0)
    val fmt    = DateTimeFormatter.ofPattern("yyyy-MM")

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(color = Color(0xFF2563EB), label = "Income")
                LegendDot(color = Color(0xFFEF4444), label = "Expense")
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                breakdown.forEach { mb ->
                    val isSelected = mb.month == selectedKey
                    val incomeH = (mb.income   / maxVal).toFloat()
                    val expenseH = (abs(mb.expense) / maxVal).toFloat()
                    val shortLabel = try {
                        YearMonth.parse(mb.month, fmt).month
                            .getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                    } catch (_: Exception) { mb.month }

                    // Animate bar heights
                    val animIncome  by animateFloatAsState(incomeH,  tween(700), label = "inc")
                    val animExpense by animateFloatAsState(expenseH, tween(700), label = "exp")

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(mb.month) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Income bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(animIncome)
                                        .background(
                                            if (isSelected) Color(0xFF2563EB) else Color(0xFFBFDBFE),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                                // Expense bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(animExpense)
                                        .background(
                                            if (isSelected) Color(0xFFEF4444) else Color(0xFFFECACA),
                                            RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            shortLabel,
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7280))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick Stat chip
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827),
                textAlign = TextAlign.Center)
            Text(label, fontSize = 9.sp, color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Biggest transaction row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun BigTransactionRow(rank: Int, tx: BigTransaction) {
    val rankColor = when (rank) {
        1 -> Color(0xFFF59E0B)
        2 -> Color(0xFF9CA3AF)
        3 -> Color(0xFFCD7C2F)
        else -> Color(0xFFD1D5DB)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(rankColor.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "#$rank",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = rankColor
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(tx.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    tx.categoryName?.let { append(it) }
                    if (tx.categoryName != null) append(" · ")
                    append(tx.date)
                },
                fontSize = 11.sp, color = Color(0xFF9CA3AF)
            )
        }

        Text(
            "$${"%.2f".format(abs(tx.amount))}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF4444)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        modifier = Modifier.padding(horizontal = 16.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1F2937)
    )
}
