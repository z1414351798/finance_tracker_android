package com.z.financetracker.screen

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.z.financetracker.component.FinanceInput
import androidx.compose.ui.text.TextStyle
import com.z.financetracker.client.NetworkClient
import com.z.financetracker.entity.Category
import com.z.financetracker.entity.Transaction
import com.z.financetracker.enums.TraType
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Form States
    var text by remember { mutableStateOf("") } // The "Title" of the transaction
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TraType.EXPENSE) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }

    // Dialog State for adding a new category
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // 1. Loading categories
    fun loadCategories() {
        scope.launch {
            try {
                val list = NetworkClient.getCategoryApi(context).getCategories(selectedType)
                categories = list
            } catch (e: Exception) {
                Log.e("API", "Failed to fetch categories", e)
            }
        }
    }

    LaunchedEffect(selectedType) {
        loadCategories()
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Add Record", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))

        Spacer(modifier = Modifier.height(16.dp))

        // Toggle (Expense/Income)
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { selectedType = TraType.EXPENSE },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(selectedType == TraType.EXPENSE) Color(0xFFEF4444) else Color.LightGray
                )
            ) { Text("Expense") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { selectedType = TraType.INCOME },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if(selectedType == TraType.INCOME) Color(0xFF10B981) else Color.LightGray
                )
            ) { Text("Income") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Inputs
        FinanceInput(value = text, onValueChange = { text = it }, label = "Transaction Name (e.g. Lunch)")
        Spacer(modifier = Modifier.height(8.dp))
        FinanceInput(value = amount, onValueChange = { amount = it }, label = "Amount")
        Spacer(modifier = Modifier.height(8.dp))
        FinanceInput(value = note, onValueChange = { note = it }, label = "Note (Optional)")

        Spacer(modifier = Modifier.height(16.dp))

        // Category Section
        Text("Select Category:", fontWeight = FontWeight.Bold)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name) }
                )
            }

            // The "Add Category" Button at the end
            FilterChip(
                selected = false,
                onClick = { showAddCategoryDialog = true },
                label = { Text("+ Add New") },
                colors = FilterChipDefaults.filterChipColors(labelColor = Color.Blue)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Save Transaction
        Button(
            onClick = {
                scope.launch {
                    val transaction = Transaction(
                        text = if (text.isEmpty()) selectedCategory?.name ?: "Record" else text,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        categoryId = selectedCategory?.id ?: 0L,
                        type = selectedType,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        note = note
                    )
                    val response = NetworkClient.getTransactionApi(context).addTransaction(transaction)
                    if (response.isSuccessful) onSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("SAVE")
        }
    }

    // --- Add Category Dialog ---
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                FinanceInput(value = newCategoryName, onValueChange = { newCategoryName = it }, label = "Category Name")
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        val response = NetworkClient.getCategoryApi(context).addCategory(
                            Category(id = 0L, name = newCategoryName, type = selectedType)
                        )
                        if (response.isSuccessful) {
                            loadCategories() // Refresh list
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }
}