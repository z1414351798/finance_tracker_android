package com.z.financetracker.entity

data class SavingsGoal(
    val id: Long = 0,
    val name: String = "",
    val icon: String = "🎯",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: String? = null,    // "yyyy-MM-dd"
    val createdAt: String? = null
)