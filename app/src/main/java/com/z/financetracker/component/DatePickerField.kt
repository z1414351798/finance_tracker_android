package com.z.financetracker.component

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,               // "yyyy-MM-dd" or ""
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = value
            .toEpochMillis()
            ?: System.currentTimeMillis()
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = "Pick date",
                tint = Color(0xFF2563EB)
            )
        },
        modifier = modifier.clickable { showPicker = true },
        enabled = false,          // disables keyboard, clickable handles tap
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledBorderColor  = Color(0xFFE5E7EB),
            disabledLabelColor   = Color(0xFF6B7280),
            disabledTextColor    = Color(0xFF111827),
            disabledTrailingIconColor = Color(0xFF2563EB),
            focusedBorderColor   = Color(0xFF2563EB),
            unfocusedBorderColor = Color(0xFFE5E7EB)
        )
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis
                        ?.toDateString()
                        ?.let { onDateSelected(it) }
                    showPicker = false
                }) { Text("OK", color = Color(0xFF2563EB)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel", color = Color(0xFF6B7280))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = Color(0xFF2563EB),
                    todayDateBorderColor      = Color(0xFF2563EB),
                    todayContentColor         = Color(0xFF2563EB)
                )
            )
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────

private fun String.toEpochMillis(): Long? {
    if (isBlank()) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(this)?.time
    }.getOrNull()
}

private fun Long.toDateString(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        .format(Date(this))