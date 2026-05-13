package com.z.financetracker.entity

import com.z.financetracker.enums.TraType

data class Transaction(
    val id: Int? = null,
    val text: String,
    val note: String? = null,
    val amount: Double,
    val type: TraType,
    val categoryId: Long,
    val categoryName: String? = null, // ← ADD THIS (backend JOIN returns it)
    val date: String,
    val imageUrl: String? = null,      // ← add this
    val imagePresignedUrl: String? = null
)