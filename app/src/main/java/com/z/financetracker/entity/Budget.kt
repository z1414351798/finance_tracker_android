package com.z.financetracker.entity

import com.z.financetracker.enums.TraType

data class Budget(
    val id: Long? = null,
    val categoryId: Long,
    val limitAmount: Double,
    val month: String,
    val categoryName: String? = null
)