package com.z.financetracker.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.z.financetracker.api.CategorySummary
import com.z.financetracker.api.TrendPoint
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var selectedRange by remember { mutableStateOf("MONTH") }
    var incomeTrends by remember { mutableStateOf<List<TrendPoint>>(emptyList()) }
    var expenseTrends by remember { mutableStateOf<List<TrendPoint>>(emptyList()) }
    var incomeCategories by remember { mutableStateOf<List<CategorySummary>>(emptyList()) }
    var expenseCategories by remember { mutableStateOf<List<CategorySummary>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ── REMOVED: modelProducer and LaunchedEffect(trends) that referenced
    //             nonexistent 'trends' variable. Each TrendChartCard manages
    //             its own modelProducer internally. ──────────────────────────

    fun load() {
        scope.launch {
            isLoading = true
            try {
                val incTrends = NetworkClient.getTransactionApi(context)
                    .getTrends(selectedRange, TraType.INCOME)
                if (incTrends.isSuccessful) incomeTrends = incTrends.body() ?: emptyList()

                val expTrends = NetworkClient.getTransactionApi(context)
                    .getTrends(selectedRange, TraType.EXPENSE)
                if (expTrends.isSuccessful) expenseTrends = expTrends.body() ?: emptyList()

                val summary = NetworkClient.getTransactionApi(context).getSummary()
                if (summary.isSuccessful) {
                    incomeCategories = summary.body()?.incomeCategories ?: emptyList()
                    expenseCategories = summary.body()?.expenseCategories ?: emptyList()
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Reload whenever the range tab changes
    LaunchedEffect(selectedRange) { load() }

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
            .padding(16.dp)
    ) {
        Text("Analytics", fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(16.dp))

        // ── Range Selector ─────────────────────────────────────────
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                listOf(
                    "DAY" to "Daily",
                    "MONTH" to "Monthly",
                    "YEAR" to "Yearly"
                ).forEach { (key, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedRange == key) Color(0xFF2563EB) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedRange = key }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (selectedRange == key) Color.White else Color(0xFF6B7280),
                            fontSize = 13.sp,
                            fontWeight = if (selectedRange == key) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Income Trend ───────────────────────────────────────────
        TrendChartCard(
            title = "Income Trend",
            trends = incomeTrends,
            color = Color(0xFF10B981)
        )

        Spacer(Modifier.height(16.dp))

        // ── Expense Trend ──────────────────────────────────────────
        TrendChartCard(
            title = "Expense Trend",
            trends = expenseTrends,
            color = Color(0xFFEF4444)
        )

        Spacer(Modifier.height(16.dp))

        // ── Category Breakdown ─────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryBreakdownCard(
                title = "Income by Category",
                categories = incomeCategories,
                baseColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            CategoryBreakdownCard(
                title = "Expense by Category",
                categories = expenseCategories,
                baseColor = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TrendChartCard(title: String, trends: List<TrendPoint>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))

            if (trends.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data", color = Color(0xFF9CA3AF))
                }
                return@Column
            }

            val maxVal = trends.maxOfOrNull { it.total } ?: 0.0
            Text(
                "Peak: ${fmtAnalytics(maxVal)}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280)
            )
            Spacer(Modifier.height(8.dp))

            // modelProducer lives here, keyed on 'trends' so it resets
            // when the data changes (e.g. range tab switches)
            val modelProducer = remember(trends) { CartesianChartModelProducer() }

            LaunchedEffect(trends) {
                modelProducer.runTransaction {
                    lineSeries {
                        series(trends.map { it.total.toFloat() })
                    }
                }
            }

            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(
                            // ── FIXED: LineCartesianLayer.Line(...) not rememberLine(...)
                            // ── FIXED: LineFill.single(fill = fill(color)) named param
                            // ── FIXED: No AreaFill — Brush not accepted by fill()
                            LineCartesianLayer.Line(
                                fill = LineCartesianLayer.LineFill.single(
                                    fill = fill(color)
                                )
                            )
                        )
                    ),
                    // ── FIXED: VerticalAxis.rememberStart() not rememberStartAxis()
                    // ── FIXED: HorizontalAxis.rememberBottom() not rememberBottomAxis()
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom()
                ),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            // Date labels below chart (only when few enough points to fit)
            if (trends.size <= 14) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    trends.forEach { pt ->
                        Text(
                            pt.timeLabel.takeLast(5),
                            fontSize = 9.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    title: String,
    categories: List<CategorySummary>,
    baseColor: Color,
    modifier: Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Text("No data", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                return@Column
            }

            val total = categories.sumOf { it.value }
            val sorted = categories.sortedByDescending { it.value }.take(5)

            DonutChart(sorted, total, baseColor)
            Spacer(Modifier.height(8.dp))

            sorted.forEach { cat ->
                val pct = if (total > 0) (cat.value / total * 100).roundToInt() else 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        cat.name,
                        fontSize = 11.sp,
                        color = Color(0xFF374151),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$pct%",
                        fontSize = 11.sp,
                        color = baseColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(categories: List<CategorySummary>, total: Double, baseColor: Color) {
    if (total <= 0) return
    val colors = generateColorShades(baseColor, categories.size)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        categories.forEachIndexed { i, cat ->
            val pct = (cat.value / total).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pct)
                        .height(8.dp)
                        .background(colors[i], RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

private fun generateColorShades(base: Color, count: Int): List<Color> =
    (0 until count).map { i ->
        base.copy(alpha = 1f - (i * 0.15f).coerceAtMost(0.6f))
    }

private fun fmtAnalytics(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 0
    }.format(amount)