package com.z.financetracker.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun AnalyticsWithBudgetAndGoalScreen() {
    var selectedTab by remember { mutableStateOf("analytics") }

    Column(modifier = Modifier.fillMaxSize()) {

        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "analytics" to "Analytics",
                    "budget"    to "Budget",
                    "goals"     to "Goals",
                    "calendar"  to "Calendar",
                    "report"    to "Report",
                    "recurring" to "Recurring"
                ).forEach { (key, label) ->
                    InnerTab(
                        label = label,
                        selected = selectedTab == key,
                        modifier = Modifier.width(100.dp),
                        onClick = { selectedTab = key }
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                "analytics" -> AnalyticsScreen()
                "budget"    -> BudgetScreen()
                "goals"     -> GoalsScreen()
                "calendar"  -> SpendingCalendarScreen()
                "report"    -> MonthlyReportScreen()
                "recurring" -> RecurringScreen()
            }
        }
    }
}

@Composable
private fun InnerTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .background(
                color = if (selected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF6B7280)
        )
    }
}