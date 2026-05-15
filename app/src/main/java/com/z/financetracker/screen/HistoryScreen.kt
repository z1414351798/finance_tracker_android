package com.z.financetracker.screen

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.z.financetracker.R
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.DatePickerField
import com.z.financetracker.component.ToastHost
import com.z.financetracker.component.rememberToastState
import com.z.financetracker.entity.Category
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ── Filter state ───────────────────────────────────────────────
    var searchText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TraType?>(null) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var showFilters by remember { mutableStateOf(false) }

    // ── Pagination state ───────────────────────────────────────────
    var page by remember { mutableStateOf(0) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var totalElements by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }

    // ── Edit/Delete state ──────────────────────────────────────────
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var deletingTransaction by remember { mutableStateOf<Transaction?>(null) }
    val toast = rememberToastState()

    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isExportingCsv by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    val detailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val pageSize = 15
    val hasMore = transactions.size < totalElements

    // Count active filters for badge
    val activeFilterCount = listOf(
        selectedType != null,
        startDate.isNotBlank(),
        endDate.isNotBlank(),
        minAmount.isNotBlank(),
        maxAmount.isNotBlank(),
        selectedCategory != null,
        noteText.isNotBlank()
    ).count { it }

    // Load all categories for filter dropdown
    LaunchedEffect(Unit) {
        try {
            categories = NetworkClient.getCategoryApi(context).getCategories(TraType.EXPENSE) +
                    NetworkClient.getCategoryApi(context).getCategories(TraType.INCOME)
        } catch (e: Exception) { }
    }

    fun loadPage(reset: Boolean) {
        scope.launch {
            if (reset) { isLoading = true; page = 0 } else isLoadingMore = true
            try {
                val resp = NetworkClient.getTransactionApi(context).getHistory(
                    page = if (reset) 0 else page,
                    size = pageSize,
                    text = searchText.ifBlank { null },
                    categoryId = selectedCategory?.id,
                    type = selectedType,
                    startDate = startDate.ifBlank { null },
                    endDate = endDate.ifBlank { null },
                    minAmount = minAmount.toDoubleOrNull(),
                    maxAmount = maxAmount.toDoubleOrNull(),
                    note = noteText.ifBlank { null }
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

    fun clearAllFilters() {
        searchText = ""
        noteText = ""
        selectedType = null
        startDate = ""
        endDate = ""
        minAmount = ""
        maxAmount = ""
        selectedCategory = null
        loadPage(true)
    }

    LaunchedEffect(Unit) { loadPage(true) }

    Scaffold { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
            ) {
            // ── Search + Filter Bar ────────────────────────────────
            // Wrap in verticalScroll so filter panel can expand
            // without being clipped by bottom nav
            val filterScrollState = rememberScrollState()

            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(filterScrollState)  // ← makes filter panel scrollable
                        .padding(16.dp)
                ) {
                    // Search row
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text(stringResource(R.string.search_by_name), color = Color(0xFF9CA3AF)) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9CA3AF)) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = ""; loadPage(true) }) {
                                        Icon(Icons.Default.Close, null, tint = Color(0xFF9CA3AF))
                                    }
                                }
                                Box {
                                    IconButton(onClick = { showFilters = !showFilters }) {
                                        Icon(
                                            Icons.Default.FilterList, null,
                                            tint = if (activeFilterCount > 0 || showFilters)
                                                Color(0xFF2563EB) else Color(0xFF9CA3AF)
                                        )
                                    }
                                    if (activeFilterCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(Color(0xFFEF4444), RoundedCornerShape(8.dp))
                                                .align(Alignment.TopEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "$activeFilterCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
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

                    // ── Filter Panel ───────────────────────────────
                    AnimatedVisibility(visible = showFilters) {
                        Column(Modifier.padding(top = 12.dp)) {
                            Text(stringResource(R.string.type_label), fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TypeChip(stringResource(R.string.all), selectedType == null) { selectedType = null }
                                TypeChip(stringResource(R.string.income), selectedType == TraType.INCOME) {
                                    selectedType = if (selectedType == TraType.INCOME) null else TraType.INCOME
                                }
                                TypeChip(stringResource(R.string.expense), selectedType == TraType.EXPENSE) {
                                    selectedType = if (selectedType == TraType.EXPENSE) null else TraType.EXPENSE
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.date_range), fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DatePickerField(stringResource(R.string.from_date), startDate, { startDate = it }, Modifier.weight(1f))
                                DatePickerField(stringResource(R.string.to_date), endDate, { endDate = it }, Modifier.weight(1f))
                            }
                            if (startDate.isNotBlank() || endDate.isNotBlank()) {
                                TextButton(onClick = { startDate = ""; endDate = "" }, contentPadding = PaddingValues(0.dp)) {
                                    Text(stringResource(R.string.clear_dates), color = Color(0xFF6B7280), fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.amount_range), fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = minAmount, onValueChange = { minAmount = it },
                                    label = { Text(stringResource(R.string.min_amount)) }, modifier = Modifier.weight(1f),
                                    singleLine = true, shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2563EB),
                                        unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                                )
                                OutlinedTextField(
                                    value = maxAmount, onValueChange = { maxAmount = it },
                                    label = { Text(stringResource(R.string.max_amount)) }, modifier = Modifier.weight(1f),
                                    singleLine = true, shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF2563EB),
                                        unfocusedBorderColor = Color(0xFFE5E7EB)
                                    )
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.note_label), fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(
                                value = noteText, onValueChange = { noteText = it },
                                placeholder = { Text(stringResource(R.string.search_in_notes), color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFE5E7EB)
                                )
                            )

                            if (categories.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(stringResource(R.string.category_label), fontSize = 12.sp, color = Color(0xFF6B7280), fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = selectedCategory == null,
                                        onClick = { selectedCategory = null },
                                        label = { Text(stringResource(R.string.all), fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF2563EB),
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                    categories.forEach { cat ->
                                        FilterChip(
                                            selected = selectedCategory == cat,
                                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                            label = { Text(cat.name, fontSize = 12.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFF2563EB),
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (activeFilterCount > 0) {
                                    OutlinedButton(
                                        onClick = { clearAllFilters() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text(stringResource(R.string.clear_all)) }
                                }
                                Button(
                                    onClick = { loadPage(true) },
                                    modifier = Modifier.weight(if (activeFilterCount > 0) 1f else 2f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text(stringResource(R.string.apply_filters)) }
                            }

                            // ← Extra padding at bottom so Apply button
                            //   clears the bottom nav
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }

            // ── Active filter chips ────────────────────────────────
            if (activeFilterCount > 0 && !showFilters) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (selectedType != null) {
                        item {
                            ActiveFilterChip(selectedType!!.name.lowercase().replaceFirstChar { it.uppercase() }) {
                                selectedType = null; loadPage(true)
                            }
                        }
                    }
                    if (startDate.isNotBlank() || endDate.isNotBlank()) {
                        item {
                            val anyLabel = stringResource(R.string.any_date)
                            ActiveFilterChip("${startDate.ifBlank { anyLabel }} → ${endDate.ifBlank { anyLabel }}") {
                                startDate = ""; endDate = ""; loadPage(true)
                            }
                        }
                    }
                    if (minAmount.isNotBlank() || maxAmount.isNotBlank()) {
                        item {
                            ActiveFilterChip("${minAmount.ifBlank { "0" }} ~ ${maxAmount.ifBlank { "∞" }}") {
                                minAmount = ""; maxAmount = ""; loadPage(true)
                            }
                        }
                    }
                    if (selectedCategory != null) {
                        item {
                            ActiveFilterChip(selectedCategory!!.name) {
                                selectedCategory = null; loadPage(true)
                            }
                        }
                    }
                    if (noteText.isNotBlank()) {
                        item {
                            val noteChipLabel = stringResource(R.string.note_chip, noteText)
                            ActiveFilterChip(noteChipLabel) {
                                noteText = ""; loadPage(true)
                            }
                        }
                    }
                }
            }

            // ── Count + Export ─────────────────────────────────────
            if (!isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.transactions_count, totalElements),
                        fontSize = 12.sp, color = Color(0xFF6B7280)
                    )
                    // Export CSV button
                    TextButton(
                        onClick = {
                            scope.launch {
                                isExportingCsv = true
                                try {
                                    val response = NetworkClient.getTransactionApi(context).exportCsv()
                                    if (response.isSuccessful) {
                                        val bytes = response.body()?.bytes() ?: return@launch
                                        val fileName = "transactions_${System.currentTimeMillis()}.csv"
                                        val file = java.io.File(context.cacheDir, fileName)
                                        file.writeBytes(bytes)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share CSV"))
                                    }
                                } catch (e: Exception) {
                                    toast.showError("Export failed: ${e.message}")
                                } finally {
                                    isExportingCsv = false
                                }
                            }
                        },
                        enabled = !isExportingCsv,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        if (isExportingCsv) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF059669)
                            )
                        } else {
                            Icon(Icons.Default.Share, null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(R.string.export_csv),
                            fontSize = 12.sp,
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ── List ───────────────────────────────────────────────
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (transactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, tint = Color(0xFFD1D5DB), modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.no_transactions), color = Color(0xFF9CA3AF))
                        if (activeFilterCount > 0) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { clearAllFilters() }) {
                                Text(stringResource(R.string.clear_filters), color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                        bottom = 80.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id ?: it.hashCode() }) { tx ->
                        TappableTransactionCard(
                            tx = tx,
                            onClick = {
                                selectedTransaction = tx
                                showDetailSheet = true
                            }
                        )
                    }

                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF2563EB))
                                } else {
                                    TextButton(onClick = { loadPage(false) }) {
                                        Text(stringResource(R.string.load_more), color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
            ToastHost(toast)
        }
    }

    // ── Edit Sheet ─────────────────────────────────────────────────
    val msgUpdated    = stringResource(R.string.transaction_updated)
    val msgUpdateFail = stringResource(R.string.update_failed)
    if (showEditSheet && editingTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false; editingTransaction = null },
            sheetState = editSheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = Color.White
        ) {
            EditTransactionSheet(
                transaction = editingTransaction!!,
                onSave = { updated ->
                    scope.launch {
                        try {
                            val resp = NetworkClient.getTransactionApi(context)
                                .updateTransaction(updated.id!!, updated)
                            if (resp.isSuccessful) {
                                showEditSheet = false
                                editingTransaction = null
                                loadPage(true)
                                scope.launch { toast.showSuccess(msgUpdated) }
                            }
                        } catch (e: Exception) {
                            scope.launch { toast.showError("$msgUpdateFail: ${e.message}") }
                        }
                    }
                },
                onDismiss = { showEditSheet = false; editingTransaction = null }
            )
        }
    }

    // ── Delete Confirm ─────────────────────────────────────────────
    deletingTransaction?.let { tx ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            shape = RoundedCornerShape(16.dp),
            title = { Text(stringResource(R.string.delete_transaction), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "\"${tx.text}\" will be permanently deleted.",
                    color = Color(0xFF6B7280)
                )
            },
            confirmButton = {
                val msgDeleted = stringResource(R.string.transaction_deleted)
                val msgDeleteFail = stringResource(R.string.delete_failed)
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val resp = NetworkClient.getTransactionApi(context)
                                    .deleteTransaction(tx.id!!)
                                if (resp.isSuccessful) {
                                    deletingTransaction = null
                                    loadPage(true)
                                    scope.launch { toast.showDelete(msgDeleted) }
                                }
                            } catch (e: Exception) {
                                scope.launch { toast.showError(msgDeleteFail) }
                                deletingTransaction = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // ── Detail Sheet ── ADD THIS ───────────────────────────────────────
    if (showDetailSheet && selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showDetailSheet = false
                selectedTransaction = null
            },
            sheetState = detailSheetState,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
            containerColor = Color.White,
            dragHandle = null
        ) {
            TransactionDetailScreen(
                transaction = selectedTransaction!!,
                onDismiss = {
                    showDetailSheet = false
                    selectedTransaction = null
                },
                onEdit = { tx ->
                    showDetailSheet = false
                    selectedTransaction = null
                    editingTransaction = tx
                    showEditSheet = true
                },
                onDelete = { tx ->
                    showDetailSheet = false
                    selectedTransaction = null
                    deletingTransaction = tx
                },
                onImageUpdated = { updated ->
                    transactions = transactions.map {
                        if (it.id == updated.id) updated else it
                    }
                    selectedTransaction = updated
                }
            )
        }
    }
}



// ── Active filter chip ─────────────────────────────────────────────

@Composable
private fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .background(Color(0xFFEFF6FF), RoundedCornerShape(20.dp))
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(2.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(18.dp)) {
                Icon(Icons.Default.Close, null, tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Swipeable card ─────────────────────────────────────────────────

@Composable
private fun TappableTransactionCard(tx: Transaction, onClick: () -> Unit) {
    val isIncome = tx.type.name == "INCOME"
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "cardFlip"
    )
    val showBack = rotation > 90f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationY = rotation; cameraDistance = 14f * density }
            .clickable { flipped = !flipped }
    ) {
        if (!showBack) {
            // ── FRONT ───────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(
                                if (isIncome) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            null,
                            tint = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tx.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(3.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (!tx.categoryName.isNullOrBlank()) {
                                Box(
                                    Modifier.background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(tx.categoryName, fontSize = 10.sp, color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.Medium)
                                }
                            }
                            Text(tx.date, fontSize = 11.sp, color = Color(0xFF9CA3AF))
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${if (isIncome) "+" else "-"}${fmtAmt(tx.amount.absoluteValue)}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text("tap to flip", fontSize = 9.sp, color = Color(0xFFD1D5DB))
                    }
                }
            }
        } else {
            // ── BACK ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Receipt thumbnail or placeholder
                        val thumbUrl = tx.imagePresignedUrl
                        if (!thumbUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(thumbUrl).crossfade(true).build(),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(Modifier.size(56.dp).background(Color(0xFF1E293B), RoundedCornerShape(10.dp)))
                                }
                            )
                        } else {
                            Box(
                                Modifier.size(56.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Receipt, null,
                                    tint = Color(0xFF334155), modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("NOTE", fontSize = 9.sp, color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (!tx.note.isNullOrBlank()) tx.note else "No note added",
                                fontSize = 13.sp,
                                color = if (!tx.note.isNullOrBlank()) Color.White else Color(0xFF475569),
                                fontStyle = if (tx.note.isNullOrBlank())
                                    androidx.compose.ui.text.font.FontStyle.Italic
                                else androidx.compose.ui.text.font.FontStyle.Normal,
                                lineHeight = 18.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Tap detail
                        Column(horizontalAlignment = Alignment.End) {
                            Icon(Icons.Default.OpenInBrowser, null,
                                tint = Color(0xFF334155), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("details", fontSize = 9.sp, color = Color(0xFF334155))
                        }
                    }
                }
            }
        }
    }
}

// ── History card ───────────────────────────────────────────────────

@Composable
private fun HistoryTransactionCard(tx: Transaction, modifier: Modifier = Modifier) {
    val isIncome = tx.type.name == "INCOME"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                    tx.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!tx.categoryName.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(tx.categoryName, fontSize = 10.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                        }
                    }
                    Text(tx.date, fontSize = 11.sp, color = Color(0xFF9CA3AF))
                }
                if (!tx.note.isNullOrBlank()) {
                    Text(
                        tx.note, fontSize = 11.sp, color = Color(0xFF6B7280),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "${if (isIncome) "+" else "-"}${fmtAmt(tx.amount.absoluteValue)}",
                fontWeight = FontWeight.Bold, fontSize = 14.sp,
                color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }
    }
}

// ── Edit sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditTransactionSheet(
    transaction: Transaction,
    onSave: (Transaction) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf(transaction.text) }
    var amount by remember { mutableStateOf(transaction.amount.absoluteValue.toString()) }
    var note by remember { mutableStateOf(transaction.note ?: "") }
    var date by remember { mutableStateOf(transaction.date) }
    var selectedType by remember { mutableStateOf(transaction.type) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(selectedType) {
        try {
            val list = NetworkClient.getCategoryApi(context).getCategories(selectedType)
            categories = list
            selectedCategory = list.find { it.name == transaction.categoryName }
        } catch (e: Exception) { }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 40.dp, height = 4.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp)))
        }

        Text(stringResource(R.string.edit_transaction), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(20.dp))

        // Type toggle
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedType = TraType.EXPENSE },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TraType.EXPENSE) Color(0xFFEF4444) else Color(0xFFF1F5F9)
                )
            ) {
                Text(stringResource(R.string.expense), color = if (selectedType == TraType.EXPENSE) Color.White else Color(0xFF6B7280))
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { selectedType = TraType.INCOME },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TraType.INCOME) Color(0xFF10B981) else Color(0xFFF1F5F9)
                )
            ) {
                Text(stringResource(R.string.income), color = if (selectedType == TraType.INCOME) Color.White else Color(0xFF6B7280))
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = text, onValueChange = { text = it },
            label = { Text(stringResource(R.string.transaction_name)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = amount, onValueChange = { amount = it },
            label = { Text(stringResource(R.string.amount_label)) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        Spacer(Modifier.height(12.dp))

        DatePickerField(label = stringResource(R.string.date_label), value = date, onDateSelected = { date = it }, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = note, onValueChange = { note = it },
            label = { Text(stringResource(R.string.note_optional_label)) },
            modifier = Modifier.fillMaxWidth(), maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE5E7EB)
            )
        )

        Spacer(Modifier.height(16.dp))

        Text(stringResource(R.string.category_label), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.name, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text(stringResource(R.string.cancel)) }

            Button(
                onClick = {
                    val rawAmount = amount.toDoubleOrNull() ?: return@Button
                    val finalAmount = if (selectedType == TraType.EXPENSE)
                        -rawAmount.absoluteValue else rawAmount.absoluteValue
                    onSave(
                        transaction.copy(
                            text = text.ifBlank { transaction.text },
                            amount = finalAmount,
                            note = note,
                            date = date,
                            type = selectedType,
                            categoryId = selectedCategory?.id ?: transaction.categoryId
                        )
                    )
                },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) { Text(stringResource(R.string.save_changes), fontWeight = FontWeight.SemiBold) }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── Helpers ────────────────────────────────────────────────────────

@Composable
private fun TypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(label, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF2563EB),
            selectedLabelColor = Color.White
        )
    )
}

private fun fmtAmt(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 0
    }.format(amount)