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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.z.financetracker.api.CashFlow
import com.z.financetracker.api.SummaryResponse
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.DailyTrend
import com.z.financetracker.entity.Transaction
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun SkylineScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var summary by remember { mutableStateOf<SummaryResponse?>(null) }
    var dailyTrends by remember { mutableStateOf<List<DailyTrend>>(emptyList()) }
    var recentTransactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun load() {
        scope.launch {
            try {
                val summaryResp = NetworkClient.getTransactionApi(context).getSummary()
                if (summaryResp.isSuccessful) summary = summaryResp.body()

                val trendsResp = NetworkClient.getTransactionApi(context).getDailyTrends(30)
                if (trendsResp.isSuccessful) dailyTrends = trendsResp.body() ?: emptyList()

                val historyResp = NetworkClient.getTransactionApi(context).getHistory(0, 5)
                if (historyResp.isSuccessful) recentTransactions = historyResp.body()?.content ?: emptyList()
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
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
    ) {
        // ── Balance Hero Card ──────────────────────────────────────
        val totalIncome = summary?.cashFlow?.totalIncome ?: 0.0
        val totalExpense = summary?.cashFlow?.totalExpense ?: 0.0
        val balance = totalIncome - totalExpense

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1E40AF), Color(0xFF2563EB), Color(0xFF3B82F6))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Balance",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatCurrency(balance),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth()) {
                    SummaryChip(
                        label = "Income",
                        amount = totalIncome,
                        color = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(16.dp))
                    SummaryChip(
                        label = "Expense",
                        amount = totalExpense,
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── 30-Day Trend Chart ─────────────────────────────────────
        if (dailyTrends.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("30-Day Overview", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    DailyTrendChart(trends = dailyTrends)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Expense Breakdown ──────────────────────────────────────
        val expenseCategories = summary?.expenseCategories ?: emptyList()
        if (expenseCategories.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    val total = expenseCategories.sumOf { it.value }
                    expenseCategories.sortedByDescending { it.value }.take(5).forEach { cat ->
                        CategoryBar(
                            name = cat.name,
                            amount = cat.value,
                            total = total,
                            color = categoryColor(cat.name)
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Recent Transactions ────────────────────────────────────
        if (recentTransactions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    recentTransactions.forEach { tx ->
                        TransactionRow(tx)
                        if (tx != recentTransactions.last()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryChip(label: String, amount: Double, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            Text(
                formatCurrency(amount),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyTrendChart(trends: List<DailyTrend>) {
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(trends) {
        modelProducer.runTransaction {
            lineSeries {
                series(trends.map { it.income.toFloat() })
                series(trends.map { it.expense.toFloat() })
            }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill = fill(Color(0xFF10B981)))
                    ),
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill = fill(Color(0xFFEF4444)))
                    )
                )
            ),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom()
        ),
        modelProducer = modelProducer,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    )
}

@Composable
private fun CategoryBar(name: String, amount: Double, total: Double, color: Color) {
    val pct = if (total > 0) (amount / total).toFloat() else 0f
    val animPct by animateFloatAsState(targetValue = pct, animationSpec = tween(800))

    Column {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 13.sp, color = Color(0xFF374151))
            Text(
                formatCurrency(amount),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF111827)
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFF1F5F9), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animPct)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val isIncome = tx.type.name == "INCOME"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tx.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                tx.categoryName ?: tx.date,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        Text(
            "${if (isIncome) "+" else "-"}${formatCurrency(Math.abs(tx.amount))}",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
        )
    }
}

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 0
    }.format(amount)
}

private fun categoryColor(name: String): Color {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFFEF4444), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFF84CC16)
    )
    return colors[name.hashCode().and(0x7FFFFFFF) % colors.size]
}