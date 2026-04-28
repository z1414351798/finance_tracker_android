package com.z.financetracker.entity

data class BudgetProgress(
    val id: Long,
    val categoryName: String,
    val budgetLimit: Double,
    val spent: Double,
    val month: String
) {
    val remaining: Double get() = budgetLimit - spent
    val percentage: Float get() = if (budgetLimit > 0) (spent / budgetLimit).toFloat() else 0f
    val isOver: Boolean get() = spent > budgetLimit
}