package com.z.financetracker.screen

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.api.HistoryResponse
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.DatePickerField
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Filter state
    var searchText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TraType?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }

    // Pagination state
    var page by remember { mutableStateOf(0) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var totalElements by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    val pageSize = 15
    val hasMore = transactions.size < totalElements

    fun loadPage(reset: Boolean) {
        scope.launch {
            if (reset) {
                isLoading = true
                page = 0
            } else {
                isLoadingMore = true
            }
            try {
                val resp = NetworkClient.getTransactionApi(context).getHistory(
                    page = if (reset) 0 else page,
                    size = pageSize,
                    text = searchText.ifBlank { null },
                    type = selectedType,
                    startDate = startDate.ifBlank { null },
                    endDate = endDate.ifBlank { null },
                    minAmount = minAmount.toDoubleOrNull(),
                    maxAmount = maxAmount.toDoubleOrNull()
                )
                if (resp.isSuccessful) {
                    val body = resp.body()!!
                    transactions = if (reset) body.content else transactions + body.content
                    totalElements = body.totalElements
                    page = if (reset) 1 else page + 1
                }
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPage(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        // ── Search Bar ─────────────────────────────────────────────
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search transactions…", color = Color(0xFF9CA3AF)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF))
                    },
                    trailingIcon = {
                        Row {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { searchText = ""; loadPage(true) }) {
                                    Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF))
                                }
                            }
                            IconButton(onClick = { showFilters = !showFilters }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    null,
                                    tint = if (showFilters) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE5E7EB)
                    )
                )

                // ── Filter Panel ───────────────────────────────────
                AnimatedVisibility(visible = showFilters) {
                    Column(Modifier.padding(top = 12.dp)) {

                        // Type chips
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TypeChip("All", selectedType == null) { selectedType = null }
                            TypeChip("Income", selectedType == TraType.INCOME) {
                                selectedType = if (selectedType == TraType.INCOME) null else TraType.INCOME
                            }
                            TypeChip("Expense", selectedType == TraType.EXPENSE) {
                                selectedType = if (selectedType == TraType.EXPENSE) null else TraType.EXPENSE
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Date range
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DatePickerField(
                                label = "From",
                                value = startDate,
                                onDateSelected = { startDate = it },
                                modifier = Modifier.weight(1f)
                            )
                            DatePickerField(
                                label = "To",
                                value = endDate,
                                onDateSelected = { endDate = it },
                                modifier = Modifier.weight(1f)
                            )

                            if (startDate.isNotBlank() || endDate.isNotBlank()) {
                                TextButton(
                                    onClick = { startDate = ""; endDate = "" },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Clear dates", color = Color(0xFF6B7280), fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Amount range
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = minAmount,
                                onValueChange = { minAmount = it },
                                label = { Text("Min Amount") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )
                            OutlinedTextField(
                                value = maxAmount,
                                onValueChange = { maxAmount = it },
                                label = { Text("Max Amount") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { loadPage(true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) { Text("Apply Filters") }
                    }
                }
            }
        }

        // ── Count ──────────────────────────────────────────────────
        if (!isLoading) {
            Text(
                "$totalElements transaction${if (totalElements != 1L) "s" else ""}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }

        // ── List ───────────────────────────────────────────────────
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else if (transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        null,
                        tint = Color(0xFFD1D5DB),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("No transactions found", color = Color(0xFF9CA3AF))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions, key = { it.id ?: it.hashCode() }) { tx ->
                    HistoryTransactionCard(tx)
                }

                if (hasMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingMore) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF2563EB)
                                )
                            } else {
                                TextButton(onClick = { loadPage(false) }) {
                                    Text("Load more", color = Color(0xFF2563EB))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF2563EB),
            selectedLabelColor = Color.White
        )
    )
}

@Composable
private fun HistoryTransactionCard(tx: Transaction) {
    val isIncome = tx.type.name == "INCOME"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    null,
                    tint = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    tx.text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!tx.categoryName.isNullOrBlank()) {
                        CategoryTag(tx.categoryName)
                    }
                    Text(tx.date, fontSize = 11.sp, color = Color(0xFF9CA3AF))
                }
                if (!tx.note.isNullOrBlank()) {
                    Text(
                        tx.note,
                        fontSize = 11.sp,
                        color = Color(0xFF6B7280),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                "${if (isIncome) "+" else "-"}${fmtAmt(Math.abs(tx.amount))}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }
    }
}

@Composable
private fun CategoryTag(name: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(name, fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
    }
}

private fun fmtAmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 0
    }.format(amount)