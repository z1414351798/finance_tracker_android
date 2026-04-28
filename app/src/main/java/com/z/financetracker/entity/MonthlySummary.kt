package com.z.financetracker.entity

data class MonthlySummary(
    val income: Double,
    val expense: Double,
    val net: Double
)

data class DashboardSummary(
    val totalBalance: Double,
    val monthlyBalance: MonthlySummary?,
    val expenseCategories: List<CategorySummary>,
    val incomeCategories: List<CategorySummary>
)

data class CategorySummary(
    val name: String,
    val value: Double
)