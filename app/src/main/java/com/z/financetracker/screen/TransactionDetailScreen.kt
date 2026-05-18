package com.z.financetracker.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.component.ToastHost
import com.z.financetracker.util.ImageCompressor
import com.z.financetracker.component.rememberToastState
import com.z.financetracker.entity.Transaction
import com.z.financetracker.ui.theme.AppColors
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit,
    onImageUpdated: (Transaction) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toast = rememberToastState()

    var currentTx by remember { mutableStateOf(transaction) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var showDeleteImageConfirm by remember { mutableStateOf(false) }
    var showFullImage by remember { mutableStateOf(false) }

    val isIncome = currentTx.type.name == "INCOME"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingImage = true
            try {
                val bytes = ImageCompressor.compress(context, uri)
                val part = MultipartBody.Part.createFormData(
                    "image", "receipt.jpg",
                    bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val resp = NetworkClient.getTransactionApi(context)
                    .uploadTransactionImage(currentTx.id!!, part)
                if (resp.isSuccessful) {
                    val newUrl = resp.body()?.get("imageUrl")
                    currentTx = currentTx.copy(imageUrl = newUrl)
                    onImageUpdated(currentTx)
                    scope.launch { toast.showSuccess("Photo updated!") }
                }
            } catch (e: Exception) {
                scope.launch { toast.showError("Upload failed") }
            } finally {
                isUploadingImage = false
            }
        }
    }

    // ── Full screen image viewer — rendered at root level ──────────
    // Must be OUTSIDE Scaffold so it covers everything including top bar
    // Presigned URL for full-screen; fall back to proxy URL if not available
    val imageLoadUrl = currentTx.imagePresignedUrl

    if (showFullImage && imageLoadUrl != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .zIndex(10f)     // float above everything
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageLoadUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Full image",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )

            // Close button — tap anywhere or use X button
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        }
                    ) { showFullImage = false }
            )

            // X button top left
            IconButton(
                onClick = { showFullImage = false },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }

            // Hint text at bottom
            Text(
                "Tap anywhere to close",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
        return  // ← don't render anything else while fullscreen is showing
    }

    // ── Normal detail view ─────────────────────────────────────────
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
            // ── Image section ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (currentTx.imageUrl != null) 240.dp else 140.dp)
            ) {
                if (imageLoadUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageLoadUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Receipt",
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { showFullImage = true },
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    )

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )

                    // Tap to view hint
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ZoomIn, null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Tap to view", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    // Image action buttons top right
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White,
                                modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { showDeleteImageConfirm = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Delete, null, tint = Color.White,
                                modifier = Modifier.size(18.dp))
                        }
                    }

                    if (isUploadingImage) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                } else {
                    // No image placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(color = Color(0xFF2563EB))
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Add receipt photo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Back button top left
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            if (currentTx.imageUrl != null) Color.Black.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack, null,
                        tint = if (currentTx.imageUrl != null) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Transaction details ────────────────────────────────
            Column(Modifier.padding(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${if (isIncome) "+" else "-"}${fmtDetail(currentTx.amount.absoluteValue)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isIncome) AppColors.incomeText else AppColors.expenseText
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (isIncome) AppColors.incomeBackground else AppColors.expensBackground,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            currentTx.type.name,
                            color = if (isIncome) AppColors.incomeText else AppColors.expenseText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(currentTx.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.height(20.dp))

                DetailRow(Icons.Default.CalendarMonth, "Date", currentTx.date)

                if (!currentTx.categoryName.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    DetailRow(Icons.Default.Category, "Category", currentTx.categoryName!!)
                }

                if (!currentTx.note.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    DetailRow(Icons.Default.Notes, "Note", currentTx.note!!)
                }

                Spacer(Modifier.height(32.dp))

                // Edit / Delete buttons
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEdit(currentTx) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onDelete(currentTx) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.expenseText)
                    ) {
                        Icon(Icons.Default.Delete, null, tint = Color.White,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
            }
            ToastHost(toast)
        }
    }

    // ── Delete image confirm ───────────────────────────────────────
    if (showDeleteImageConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteImageConfirm = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Remove photo?", fontWeight = FontWeight.Bold) },
            text = { Text("The receipt photo will be permanently deleted.",
                color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                val resp = NetworkClient.getTransactionApi(context)
                                    .deleteTransactionImage(currentTx.id!!)
                                if (resp.isSuccessful) {
                                    currentTx = currentTx.copy(imageUrl = null)
                                    onImageUpdated(currentTx)
                                    showDeleteImageConfirm = false
                                    scope.launch { toast.showDelete("Photo removed") }
                                }
                            } catch (e: Exception) {
                                scope.launch { toast.showError("Failed to remove photo") }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.expenseText)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteImageConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Medium)
        }
    }
}

private fun fmtDetail(amount: Double): String =
    NumberFormat.getNumberInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 0
    }.format(amount)