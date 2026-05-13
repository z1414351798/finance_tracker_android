package com.z.financetracker.entity

import com.z.financetracker.enums.TraType

data class RecurringTransaction(
    val id:           Int?     = null,
    val text:         String   = "",
    val amount:       Double   = 0.0,    // always positive in DB
    val type:         TraType  = TraType.EXPENSE,
    val categoryId:   Long     = 0L,
    val categoryName: String?  = null,
    val frequency:    String   = "MONTHLY",   // WEEKLY BIWEEKLY MONTHLY QUARTERLY YEARLY
    val nextDueDate:  String   = "",           // yyyy-MM-dd
    val isActive:     Boolean  = true,
    val note:         String?  = null
)
