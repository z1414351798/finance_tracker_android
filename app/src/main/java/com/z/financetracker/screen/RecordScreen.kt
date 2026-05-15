package com.z.financetracker.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.z.financetracker.R
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.util.ImageCompressor
import com.z.financetracker.component.FinanceInput
import com.z.financetracker.entity.Category
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var text             by remember { mutableStateOf("") }
    var amount           by remember { mutableStateOf("") }
    var note             by remember { mutableStateOf("") }
    var selectedType     by remember { mutableStateOf(TraType.EXPENSE) }
    var categories       by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName  by remember { mutableStateOf("") }

    var selectedImageUri     by remember { mutableStateOf<Uri?>(null) }
    var isUploading          by remember { mutableStateOf(false) }
    var showFullImagePreview by remember { mutableStateOf(false) }

    // Date picker
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter    = SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault())
    val displayFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    fun loadCategories() {
        scope.launch {
            try {
                categories = NetworkClient.getCategoryApi(context).getCategories(selectedType)
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(selectedType) { loadCategories() }

    // ── Full image preview ────────────────────────────────────────────────────
    if (showFullImagePreview && selectedImageUri != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10f)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(selectedImageUri).crossfade(true).build(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { showFullImagePreview = false }
            )
            IconButton(
                onClick = { showFullImagePreview = false },
                modifier = Modifier
                    .align(Alignment.TopStart).padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) { Icon(Icons.Default.Close, null, tint = Color.White) }
            Text(stringResource(R.string.tap_anywhere_close),
                color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.add_record), style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(16.dp))

        // ── Type toggle ───────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedType = TraType.EXPENSE },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TraType.EXPENSE) Color(0xFFEF4444) else Color.LightGray)
            ) { Text(stringResource(R.string.expense)) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { selectedType = TraType.INCOME },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == TraType.INCOME) Color(0xFF10B981) else Color.LightGray)
            ) { Text(stringResource(R.string.income)) }
        }

        Spacer(Modifier.height(16.dp))

        FinanceInput(value = text,   onValueChange = { text   = it }, label = stringResource(R.string.transaction_name_hint))
        Spacer(Modifier.height(8.dp))
        FinanceInput(value = amount, onValueChange = { amount = it }, label = stringResource(R.string.amount_label))
        Spacer(Modifier.height(8.dp))
        FinanceInput(value = note,   onValueChange = { note   = it }, label = stringResource(R.string.note_optional))

        Spacer(Modifier.height(8.dp))

        // ── Date picker row ───────────────────────────────────────────────────
        OutlinedCard(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = displayFormatter.format(selectedDate.time),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF374151)
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Category ──────────────────────────────────────────────────────────
        Text(stringResource(R.string.select_category), fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick  = { selectedCategory = category },
                    label    = { Text(category.name) }
                )
            }
            FilterChip(
                selected = false,
                onClick  = { showAddCategoryDialog = true },
                label    = { Text(stringResource(R.string.add_new_category)) },
                colors   = FilterChipDefaults.filterChipColors(labelColor = Color.Blue)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Receipt photo ─────────────────────────────────────────────────────
        Text(stringResource(R.string.receipt_photo_optional), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        if (selectedImageUri != null) {
            Box(modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(selectedImageUri).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showFullImagePreview = true },
                    contentScale = ContentScale.Crop
                )
                // Tap to view hint
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter).padding(bottom = 8.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ZoomIn, null, tint = Color.White,
                            modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.tap_to_view), color = Color.White, fontSize = 11.sp)
                    }
                }
                // Remove button
                IconButton(
                    onClick = { selectedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd).padding(4.dp).size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White,
                        modifier = Modifier.size(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, null,
                        tint = Color(0xFF9CA3AF), modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.tap_attach_receipt), color = Color(0xFF9CA3AF), fontSize = 13.sp)
                    Text(stringResource(R.string.stored_privately), color = Color(0xFFBBBFCA), fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Save ──────────────────────────────────────────────────────────────
        Button(
            onClick = {
                scope.launch {
                    isUploading = true
                    try {
                        val rawAmount   = amount.toDoubleOrNull() ?: 0.0
                        val finalAmount = if (selectedType == TraType.EXPENSE)
                            -Math.abs(rawAmount) else Math.abs(rawAmount)

                        val tx = Transaction(
                            text       = if (text.isEmpty()) selectedCategory?.name ?: "Record" else text,
                            amount     = finalAmount,
                            categoryId = selectedCategory?.id ?: 0L,
                            type       = selectedType,
                            date       = dateFormatter.format(selectedDate.time),
                            note       = note
                        )
                        val resp = NetworkClient.getTransactionApi(context).addTransaction(tx)

                        if (resp.isSuccessful) {
                            val savedTx = resp.body()
                            if (selectedImageUri != null && savedTx?.id != null) {
                                val bytes = ImageCompressor.compress(context, selectedImageUri!!)
                                val part  = MultipartBody.Part.createFormData(
                                    "image", "receipt.jpg",
                                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                                )
                                NetworkClient.getTransactionApi(context).uploadTransactionImage(savedTx.id, part)
                            }
                            onSuccess()
                        }
                    } finally {
                        isUploading = false
                    }
                }
            },
            enabled  = !isUploading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isUploading)
                CircularProgressIndicator(color = Color.White,
                    modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else
                Text(stringResource(R.string.save))
        }
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Calendar.getInstance().apply { timeInMillis = millis }
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Add category dialog ───────────────────────────────────────────────────
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title   = { Text(stringResource(R.string.new_category)) },
            text    = {
                FinanceInput(value = newCategoryName,
                    onValueChange = { newCategoryName = it }, label = stringResource(R.string.category_name))
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val response = NetworkClient.getCategoryApi(context).addCategory(
                            Category(id = 0L, name = newCategoryName, type = selectedType))
                        if (response.isSuccessful) {
                            loadCategories()
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                }) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
